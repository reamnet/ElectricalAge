package mods.eln.transparentnode.standby

import mods.eln.node.transparent.TransparentNodeElement
import mods.eln.sim.IProcess
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.mna.component.SwitchableVoltageSource
import mods.eln.sim.nbt.NbtElectricalLoad
import net.minecraft.nbt.CompoundTag

enum class StandbyMode { AUTO, MANUAL, OFF }

/** Fuel and engine timers advance only after an accepted physical electrical step. */
class StandbyGeneratorUnit(
    private val loads: MutableList<mods.eln.sim.mna.state.State>,
    private val components: MutableList<mods.eln.sim.mna.component.Component>,
    private val processes: MutableList<IProcess>,
    val fuel: StandbyFuelStore,
    val tier: GeneratorTier=GeneratorTier.SMALL
) {
    constructor(owner: TransparentNodeElement,tier: GeneratorTier=GeneratorTier.SMALL) :
        this(owner.electricalLoadList,owner.electricalComponentList,owner.electricalProcessList,StandbyTank(tier.tankMb) { owner.needPublish() },tier)
    val maxWatts get()=tier.watts
    val maxAmps get()=tier.amps
    companion object { const val MAX_WATTS=10_000.0;const val IDLE_WATTS=300.0 }
    val output=NbtElectricalLoad("standbyOutput").apply { rs=.000001 }
    private val internal=NbtElectricalLoad("standbyAlternator").apply { rs=.000001 }
    val source=SwitchableVoltageSource("standbyEngine").apply { connectTo(internal,null) }
    private val wire=Resistor(internal,output).apply { r=.005/tier.factor }
    private val leakage=Resistor(output,null).apply { r=1e10 }
    val controller=StandbyAlternator(source,this)
    private var attachedRoot: mods.eln.sim.mna.RootSystem?=null
    var mode=StandbyMode.AUTO
    var channel=""
    var nominal=240.0
        private set
    var status="STANDBY"
        private set
    var fault=""
        private set
    var running=false
        private set
    private var starting=0.0
    private var cooling=0.0
    private var overload=0.0
    var requested=false
    val watts get()=if(source.enabled)source.p.coerceAtLeast(0.0) else 0.0
    init {
        loads.addAll(listOf(output,internal))
        components.addAll(listOf(source,wire,leakage))
        processes.add(IProcess(::tick))
    }
    fun setVoltage(value: Double): Boolean {
        if(!TransferControl.validVoltage(value))return false
        nominal=value;stop();return true
    }
    fun attach(root: mods.eln.sim.mna.RootSystem) {
        if(attachedRoot===root)return
        attachedRoot?.removeProcess(controller);attachedRoot=root
        if(running)root.addProcess(controller)
    }
    fun detach() { stop();attachedRoot=null }
    fun stop() { attachedRoot?.removeProcess(controller);source.enabled=false;running=false;starting=0.0;cooling=0.0;overload=0.0 }
    fun reset() { fault="";stop();status=if(mode==StandbyMode.OFF)"OFF" else "STANDBY" }
    private fun trip(reason: String) { fault=reason;stop();status=reason }
    fun tick(seconds: Double) {
        if(!seconds.isFinite() || seconds<=0)return
        if(running) {
            // Account the last accepted electrical solve once, including idle fuel consumption.
            if(!fuel.consume(maxOf(IDLE_WATTS*tier.factor,source.p.coerceAtLeast(0.0))*seconds)) { trip("EMPTY");return }
            if(controller.problem.isNotEmpty()) { trip(controller.problem);return }
            if(output.u<nominal*.85) { trip("OVERLOAD");return }
            val bad=source.p>maxWatts*1.10 || source.current>maxAmps*1.000001 ||
                source.p>=minOf(maxWatts,nominal*maxAmps)*.995 && output.u<nominal*.98 || source.p < -50.0
            overload=if(bad)overload+seconds else 0.0
            if(overload>=.5) { trip(if(source.p < -50.0)"BACKFEED" else "OVERLOAD");return }
        }
        if(mode==StandbyMode.OFF) { stop();status="OFF";return }
        if(fault=="EMPTY" && fuel.availableJoules>=maxWatts*seconds)fault=""
        if(fault.isNotEmpty()) { status=fault;source.enabled=false;return }
        val demand=mode==StandbyMode.MANUAL || requested
        if(!demand && !running) { starting=0.0;status="STANDBY";return }
        if(fuel.availableJoules<maxWatts*seconds) { trip("EMPTY");return }
        if(demand)cooling=0.0
        if(!running) {
            starting+=seconds;status="STARTING"
            if(starting>=3.0) { running=true;source.u=nominal;source.enabled=true;attachedRoot?.addProcess(controller);status="RUNNING" }
        } else if(!demand) {
            cooling+=seconds;status="COOLDOWN"
            if(cooling>=5.0) { stop();status="STANDBY" }
        } else status="RUNNING"
    }
    fun read(nbt: CompoundTag) {
        stop();fault=nbt.getStringOr("standbyFault", "").takeIf { it in listOf("EMPTY","OVERLOAD","BACKFEED") } ?: ""
        nominal=nbt.getDoubleOr("standbyVoltage", 0.0).takeIf(TransferControl::validVoltage) ?: 240.0
        mode=StandbyMode.entries.getOrNull(nbt.getIntOr("standbyMode", 0)) ?: StandbyMode.AUTO
        channel=StandbyLink.channel(nbt.getStringOr("standbyChannel", ""));fuel.read(nbt)
    }
    fun write(nbt: CompoundTag) {
        nbt.putDouble("standbyVoltage",nominal);nbt.putInt("standbyMode",mode.ordinal);nbt.putString("standbyChannel",channel);nbt.putString("standbyFault",fault);fuel.write(nbt)
    }
}
