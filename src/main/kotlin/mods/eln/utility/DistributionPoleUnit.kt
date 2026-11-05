package mods.eln.utility

import mods.eln.sim.IProcess
import mods.eln.sim.mna.component.*
import mods.eln.sim.mna.state.State
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.transparentnode.standby.TransferContact
import net.minecraft.nbt.CompoundTag
import kotlin.math.abs

/** Passive distribution paths. Protection observes accepted physical time, never converter probe retries. */
class DistributionPoleUnit(val kind: DistributionKind,states: MutableList<State>,parts: MutableList<Component>,processes: MutableList<IProcess>) {
    val lines=List(kind.phases) { NbtElectricalLoad("phase$it").apply { rs=.00001 } }
    val outs=if(kind.protectedLine)List(kind.outputPhases) { NbtElectricalLoad("phaseOut$it").apply { rs=.00001 } } else lines
    val transformer=if(kind.transformer)PoleTransformerUnit(states,parts) else null
    val services=transformer?.service?.loads ?: List(3) { NbtElectricalLoad("secondaryBus$it").apply { rs=.00001 } }
    val undergroundInput=if(kind.pedestal)List(3) { NbtElectricalLoad("undergroundServiceIn$it").apply { rs=.00001 } } else emptyList()
    val serviceSplit=when { kind.pedestal->WiredServiceSplit(states,parts,undergroundInput);kind.singlePrimaryRiser->WiredServiceSplit(states,parts,services);else->null }
    var wiredSplitAllowed: ()->Boolean={true}
    val lamp=when { kind.light->PoleLamp(services[0],parts,kind.lampWatts,kind.transformer);kind.underground->null;else->PoleLamp(services[0],parts,source240=services[2],removable=true) }
    val contacts=when {
        kind.pedestal->undergroundInput.indices.map { TransferContact(undergroundInput[it],services[it]) }
        kind.transformer->lines.map { TransferContact(it,transformer!!.primary) }
        kind.singlePhaseTap->lines.map { TransferContact(it,outs[0]) }
        kind.protectedLine->lines.indices.map { TransferContact(lines[it],outs[it]) }
        else->emptyList()
    }
    val fuses=List(kind.fuseCount) { CutoutFuse() }
    val recloser=RecloserControl()
    var phase=0;private set
    var gangClosed=true;private set
    var fuseAmps=100.0;private set
    var regulatorTarget=12470.0;private set
    var regulatorAutomatic=true;private set
    var changed: (String)->Unit={}
    private var regulatorClock=0.0
    private var pendingSample=false
    val regulatorTaps: List<ServiceTap>
    val capacitorPins=ArrayList<NbtElectricalLoad>()
    init {
        require(!kind.retiredGroundCabinet || kind==DistributionKind.UNDERGROUND_THREE)
        if(!kind.lvOnly)states.addAll(lines)
        if(outs!==lines && !kind.pedestal)states.addAll(outs)
        if(transformer==null)states.addAll(services)
        states.addAll(undergroundInput)
        if(kind.regulator) {
            // A fused line contact precedes the ratio tap. Keep both contacts and ratio stamps stable at idle.
            parts.addAll(contacts)
            regulatorTaps=lines.indices.map { i->
                val input=NbtElectricalLoad("regulatorInput$i").apply { rs=.00001 }
                val pin=NbtElectricalLoad("regulatorTap$i").apply { rs=.00001 }
                states.add(input);states.add(pin)
                contacts[i].connectTo(lines[i],input)
                parts.add(Resistor(pin,outs[i]).setR(.005))
                ServiceTap(input,pin,1.0).also(parts::add)
            }
        } else { regulatorTaps=emptyList();parts.addAll(contacts) }
        if(kind.capacitor)lines.forEachIndexed { i,line->
            val pin=NbtElectricalLoad("capacitor$i");capacitorPins.add(pin);states.add(pin)
            parts.add(Resistor(line,pin).setR(100.0));parts.add(Capacitor(pin,null).apply { setC(.0001) });parts.add(Resistor(pin,null).setR(1e9))
        }
        // Open terminals retain a deterministic finite reference, not a fictitious source.
        ((if(kind.lvOnly)services else lines+outs+services)+undergroundInput).distinct().forEach { parts.add(Resistor(it,null).setR(1e10)) }
        processes.add(IProcess(::step));applyContacts()
    }
    fun current(index: Int)=if(kind.pedestal)kotlin.math.abs(contacts[2].current)+.5*(kotlin.math.abs(contacts[0].current)+kotlin.math.abs(contacts[1].current)) else contacts.getOrNull(if(kind.singlePhaseTap || kind.transformer)phase else index)?.current ?: 0.0
    fun applyContacts() {
        for(i in contacts.indices) {
            val contact=contacts[i];val was=contact.closed
            contact.closed=when {
                kind.pedestal->fuses[0].closed
                kind.transformer->i==phase && fuses[0].closed && transformer!!.available
                kind.singlePhaseTap->i==phase && fuses[0].closed
                kind.cutout || kind.regulator->fuses[i].closed
                kind.recloser->recloser.closed
                else->gangClosed
            }
            if(contact.closed && !was)pendingSample=true
        }
        transformer?.let { it.tap.enabled=it.available && fuses[0].closed }
    }
    fun selectPhase(value: Int): Boolean { if(!(kind.singlePhaseTap || kind.transformer) || value !in lines.indices)return false;phase=value;applyContacts();return true }
    fun operateFuse(index: Int): Boolean { val changed=fuses.getOrNull(index)?.operate()==true;if(changed)applyContacts();return changed }
    fun configureRecloser(pickup: Double,delay: Double,failures: Int): Boolean {
        if(!pickup.isFinite() || pickup !in 1.0..2000.0 || !delay.isFinite() || delay !in 1.0..60.0 || failures !in 1..5)return false
        recloser.pickupAmps=pickup;recloser.retrySeconds=delay;recloser.maxFailures=failures;return true
    }
    fun configureRegulator(target: Double,automatic: Boolean): Boolean {
        if(!target.isFinite() || target !in 120.0..51200.0)return false
        regulatorTarget=target;regulatorAutomatic=automatic;return true
    }
    fun switch(closed: Boolean): Boolean {
        when { kind.recloser->{ if(closed && (recloser.locked || !recloser.closed && !recloser.manualOff))return false;if(recloser.closed!=closed)recloser.operate() }
            kind==DistributionKind.AIR_BREAK_THREE->gangClosed=closed;else->return false }
        applyContacts();return true
    }
    fun resetRecloser() { recloser.reset();applyContacts() }
    fun setFuseAmps(amps: Double): Boolean { if(!amps.isFinite() || amps !in 1.0..2000.0)return false;fuseAmps=amps;return true }
    fun step(dt: Double) {
        if(!dt.isFinite() || dt<=0)return
        val transformerWasPowered=transformer?.tap?.enabled==true
        serviceSplit?.update(wiredSplitAllowed())
        val sample=!pendingSample;pendingSample=false
        if(sample) {
            for(i in fuses.indices) {
                val amps=current(i)
                if(fuses[i].pickup(dt,amps))changed("utility_arc_close")
                if(fuses[i].step(dt,amps,transformer?.let { it.ratedWatts/12470.0 } ?: fuseAmps)) { applyContacts();changed("utility_fuse_blow") }
            }
            if(kind.recloser && recloser.step(dt,contacts.map { it.current })) { applyContacts();changed("utility_recloser_bump") }
        }
        if(transformer?.step(dt,transformerWasPowered,sample)==true) { applyContacts();changed("utility_arc_open") }
        transformer?.let { if(it.tap.enabled!=(it.available && fuses[0].closed))applyContacts() }
        if(kind.regulator && regulatorAutomatic) {
            regulatorClock+=dt
            if(regulatorClock>=1) {
                regulatorClock%=1.0
                regulatorTaps.forEach { tap->
                    val input=abs(tap.aPin.state)
                    if(input>1) { val desired=(regulatorTarget/input).coerceIn(.9,1.1);val next=tap.ratio+(desired-tap.ratio).coerceIn(-.01,.01);if(abs(next-tap.ratio)>.001)tap.ratio=next }
                }
            }
        }
    }
    fun installTransformer(watts: Int?) { transformer?.install(watts);applyContacts() }
    fun configureTransformer(voltage: Int,on: Boolean): Boolean { val ok=transformer?.configure(voltage,on)==true;if(ok)applyContacts();return ok }
    fun resetTransformer() { transformer?.reset();applyContacts() }
    fun write(t: CompoundTag) {
        transformer?.write(t);lamp?.write(t)
        t.putInt("utilityPhase",phase);t.putBoolean("utilityGangClosed",gangClosed);t.putDouble("utilityFuseAmps",fuseAmps)
        fuses.forEachIndexed { i,f->f.write(t,"utilityFuse$i") };recloser.write(t)
        t.putDouble("regulatorTarget",regulatorTarget);t.putBoolean("regulatorAuto",regulatorAutomatic);t.putDouble("nativeRegulatorClock",regulatorClock)
        regulatorTaps.forEachIndexed { i,p->t.putDouble("regulatorRatio$i",p.ratio) }
    }
    fun read(t: CompoundTag) {
        transformer?.read(t);lamp?.read(t)
        phase=t.getIntOr("utilityPhase",0).coerceIn(0,lines.lastIndex);gangClosed=t.getBooleanOr("utilityGangClosed",true);setFuseAmps(t.getDoubleOr("utilityFuseAmps",100.0))
        fuses.forEachIndexed { i,f->f.read(t,"utilityFuse$i") };recloser.read(t)
        configureRegulator(t.getDoubleOr("regulatorTarget",12470.0),t.getBooleanOr("regulatorAuto",true))
        regulatorClock=t.getDoubleOr("nativeRegulatorClock",0.0).takeIf { it.isFinite() && it>=0 }?.coerceAtMost(1.0) ?: 0.0
        regulatorTaps.forEachIndexed { i,p->p.ratio=t.getDoubleOr("regulatorRatio$i",1.0).takeIf { it.isFinite() && it in .9..1.1 } ?: 1.0 }
        applyContacts()
    }
}
