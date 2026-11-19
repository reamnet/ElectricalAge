package mods.eln.transparentnode.switchgear

import mods.eln.sim.IProcess
import mods.eln.sim.mna.component.*
import mods.eln.sim.mna.state.State
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.sim.nbt.NbtElectricalGateInput
import net.minecraft.nbt.CompoundTag
import kotlin.math.abs

/** Physically independent poles, finite real contact/coil heat and accepted-time actuation. */
class SwitchgearUnit(val kind: SwitchgearKind,states: MutableList<State>,parts: MutableList<Component>,processes: MutableList<IProcess>) {
    val commons=List(kind.poles) { NbtElectricalLoad("contactCommon$it").apply { rs=.00001 } }
    val normallyOpen=List(kind.poles) { NbtElectricalLoad("contactNO$it").apply { rs=.00001 } }
    val normallyClosed=if(kind.changeover)List(kind.poles) { NbtElectricalLoad("contactNC$it").apply { rs=.00001 } } else emptyList()
    val noContacts=commons.indices.map { SwitchContact(commons[it],normallyOpen[it],kind.resistance) }
    val ncContacts=normallyClosed.indices.map { SwitchContact(commons[it],normallyClosed[it],kind.resistance) }
    val gate=NbtElectricalGateInput("controlSignal")
    val coilA=NbtElectricalLoad("coilA1").apply { rs=.00001 }
    val coilB=NbtElectricalLoad("coilA2").apply { rs=.00001 }
    val coil=Resistor(coilA,coilB).setR(1e19)
    var control=RelayControl.SIGNAL;private set
    var enabled=true;private set
    var energized=false;private set
    var transitioning=kind.changeover;private set
    var tripped=false;private set
    var temperature=20.0;private set
    private var target=false
    private var delay=if(kind.changeover).025 else 0.0
    private var overload=0.0
    // Changed and click are callbacks so native publication/audio stay outside the circuit model.
    var changed: ()->Unit={}
    var click: ()->Unit={}
    val contactLosses get()=noContacts.sumOf { it.power }+ncContacts.sumOf { it.power }
    init {
        val terminals=commons+normallyOpen+normallyClosed+listOf(gate,coilA,coilB)
        states.addAll(terminals);parts.addAll(noContacts+ncContacts);parts.add(coil)
        // Reference otherwise-open terminals without introducing fictitious supplies.
        terminals.forEach { parts.add(Resistor(it,null).setR(1e10)) }
        processes.add(IProcess(::step));contacts()
    }
    private fun configureCoil() { coil.r=if(control==RelayControl.SIGNAL)1e19 else control.nominal*control.nominal/kind.coilWatts }
    private fun contacts() {
        val ready=enabled && !tripped && !transitioning
        noContacts.forEach { it.closed=ready && energized };ncContacts.forEach { it.closed=ready && !energized }
    }
    fun setControl(value: RelayControl) {
        if(control==value)return
        control=value;energized=false;target=false;transitioning=true;delay=if(kind.solidState)0.0 else .025
        configureCoil();contacts();changed()
    }
    fun setEnabled(value: Boolean) {
        if(enabled==value)return
        enabled=value
        energized=false;target=false
        // UI changes must not close NC onto stale open-circuit voltages before the first solve.
        transitioning=value && kind.changeover;delay=if(transitioning).025 else 0.0
        contacts();changed()
    }
    fun resetTrip(): Boolean {
        if(temperature>=80)return false
        tripped=false;overload=0.0;energized=false;target=false;transitioning=enabled && kind.changeover;delay=if(transitioning).025 else 0.0;contacts();changed();return true
    }
    fun step(seconds: Double) {
        if(!seconds.isFinite() || seconds<=0)return
        val heat=contactLosses+coil.p
        var ratio=0.0
        for(contact in noContacts)ratio=maxOf(ratio,abs(contact.current)/kind.amps)
        for(contact in ncContacts)ratio=maxOf(ratio,abs(contact.current)/kind.amps)
        if(!heat.isFinite() || !ratio.isFinite()) { if(!tripped) { tripped=true;contacts();changed() };return }
        val coolingResistance=if(kind.solidState)1.8 else 3.0
        val heatCapacity=if(kind.solidState)180.0 else 250.0
        temperature+=(heat.coerceAtLeast(0.0)-(temperature-20.0)/coolingResistance)*seconds/heatCapacity
        overload=if(ratio>1.25)overload+seconds*(ratio*ratio-1) else (overload-seconds).coerceAtLeast(0.0)
        val overvoltage=control!=RelayControl.SIGNAL && abs(coil.u)>control.nominal*1.5
        if(!tripped && (overload>=5.0 || temperature>(if(kind.solidState)100 else 140) || overvoltage)) { tripped=true;contacts();changed() }
        val request=enabled && !tripped && when(control) {
            RelayControl.SIGNAL->if(energized || target)gate.normalized>.2 else gate.normalized>=.6
            else->abs(coil.u)>=control.nominal*if(energized || target).60 else .85
        }
        if(kind.solidState) {
            if(energized!=request || transitioning) { energized=request;target=request;transitioning=false;contacts();changed() }
        }else {
            if(request!=target) { target=request;delay=.025;transitioning=true;contacts() }
            if(transitioning) {
                delay-=seconds
                if(delay<=0) { val moved=energized!=target;energized=target;transitioning=false;contacts();if(moved)click();changed() }
            }
        }
    }
    fun write(t: CompoundTag) {
        t.putInt("switchgearControl",control.ordinal);t.putBoolean("switchgearEnabled",enabled);t.putBoolean("switchgearTrip",tripped);t.putDouble("switchgearTemperature",temperature);t.putDouble("switchgearOverload",overload)
    }
    fun read(t: CompoundTag) {
        control=RelayControl.entries.getOrNull(t.getIntOr("switchgearControl",0)) ?: RelayControl.SIGNAL
        enabled=t.getBooleanOr("switchgearEnabled",true);tripped=t.getBooleanOr("switchgearTrip",false)
        temperature=t.getDoubleOr("switchgearTemperature",20.0).takeIf { it.isFinite() && it>=0.0 } ?: 20.0
        overload=t.getDoubleOr("switchgearOverload",0.0).takeIf { it.isFinite() && it>=0 } ?: 0.0
        energized=false;target=false;transitioning=enabled && !tripped && kind.changeover;delay=if(transitioning).025 else 0.0;configureCoil();contacts()
    }
}
