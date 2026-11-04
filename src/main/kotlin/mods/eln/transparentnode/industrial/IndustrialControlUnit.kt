package mods.eln.transparentnode.industrial

import mods.eln.sim.IProcess
import mods.eln.sim.mna.component.Component
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.mna.state.State
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.transparentnode.standby.TransferContact
import net.minecraft.nbt.CompoundTag
import java.io.DataInputStream
import java.io.IOException
import kotlin.math.*

/** Passive protection contacts; clocks advance only after accepted physical solves. */
class IndustrialControlUnit(val kind: IndustrialKind,loads: MutableList<State>,components: MutableList<Component>,
    processes: MutableList<IProcess>,private val control: ()->Double={0.0},private val tripOutput: (Boolean)->Unit={}) {
    init { require(kind==IndustrialKind.RELAY || kind==IndustrialKind.SHEDDER) }
    var settings=kind.defaults.copyOf();private set
    var enabled=true;private set
    val protection=Protection()
    val shed=ShedControl()
    val input=NbtElectricalLoad("industrialInput").apply { rs=.0001 }
    val outputs=List(if(kind==IndustrialKind.RELAY)1 else 4) { NbtElectricalLoad("industrialOutput$it").apply { rs=.0001 } }
    val returnIn=NbtElectricalLoad("industrialReturnIn").apply { rs=.0001 }
    val returnOut=NbtElectricalLoad("industrialReturnOut").apply { rs=.0001 }
    val returnContact=TransferContact(returnIn,returnOut)
    val contacts=outputs.map { TransferContact(input,it) }
    val branchWatts=DoubleArray(4)
    val nominal get()=settings[0]
    val outputCurrent get()=contacts.sumOf { abs(it.current) }
    val inputWatts get()=max(0.0,input.u*contacts.sumOf { it.current })
    val differential get()=abs(contacts[0].current+returnContact.current)
    var lifetimeJoules=0.0;private set
    init {
        val states=listOf(input)+outputs+if(kind==IndustrialKind.RELAY)listOf(returnIn,returnOut)else emptyList()
        loads.addAll(states);components.addAll(contacts)
        if(kind==IndustrialKind.RELAY)components.add(returnContact)
        // The native LU model needs terminal references when disconnected. No path
        // crosses an open contact, and reference currents are not load measurements.
        states.forEach { components.add(Resistor(it,null).setR(1e10)) }
        processes.add(IProcess(::step));configure()
    }
    fun applySettings(values: DoubleArray): Boolean {
        if(kind==IndustrialKind.SHEDDER && values.size==2)return applySettings(kind.defaults.copyOf().also { values.copyInto(it) })
        if(!kind.valid(values))return false
        settings=values.copyOf();configure();return true
    }
    fun setEnabled(value: Boolean) {
        enabled=value
        if(kind==IndustrialKind.SHEDDER && !value)shed.step(.05,0.0,nominal,0.0,settings[1],false)
        configure()
    }
    fun resetTrip() { protection.reset();configure() }
    fun retryShed() { if(kind==IndustrialKind.SHEDDER)shed.retry() }
    fun configure() {
        // Assign only the desired state: opening then reclosing healthy contacts
        // would invalidate the LU factorization on every control step.
        if(kind==IndustrialKind.RELAY) {
            contacts[0].closed=enabled && !protection.tripped
            returnContact.closed=contacts[0].closed
        } else contacts.forEachIndexed { i,c->c.closed=enabled && shed.closed[i] }
        tripOutput(protection.tripped)
    }
    fun step(dt: Double) {
        if(!dt.isFinite() || dt<=0)return
        lifetimeJoules=(lifetimeJoules+inputWatts*dt).takeIf { it.isFinite() } ?: lifetimeJoules
        for(i in contacts.indices)branchWatts[i]=max(0.0,outputs[i].u*contacts[i].current)
        if(kind==IndustrialKind.RELAY) {
            if(control()>=.6) { protection.tripped=true;protection.reason=5 }
            if(enabled)protection.step(dt,input.u,contacts[0].current,returnContact.current,nominal,settings[1],settings[2],settings[3],settings[4],settings[5])
        } else shed.step(dt,input.u,nominal,inputWatts,settings[1],enabled && control()<.6,branchWatts,settings[2],settings[3],settings[4],settings[5])
        configure()
    }
    fun write(t: CompoundTag) {
        settings.forEachIndexed { i,v->t.putDouble("industrialSetting$i",v) }
        t.putBoolean("industrialEnabled",enabled);t.putBoolean("industrialTrip",protection.tripped);t.putInt("industrialTripReason",protection.reason)
        t.putBoolean("industrialArmed",protection.armed);t.putDouble("industrialTripTime",protection.elapsed);t.putInt("industrialPendingReason",protection.pendingReason);t.putDouble("industrialLifetime",lifetimeJoules)
        if(kind==IndustrialKind.SHEDDER)for(i in 0..3) { t.putBoolean("industrialShed$i",shed.closed[i]);t.putDouble("industrialShedDemand$i",shed.nominalDemand[i]) }
    }
    fun read(t: CompoundTag) {
        val p=DoubleArray(settings.size) { t.getDoubleOr("industrialSetting$it",kind.defaults[it]) };if(kind.valid(p))settings=p
        enabled=t.getBooleanOr("industrialEnabled",true);protection.tripped=t.getBooleanOr("industrialTrip",false);protection.reason=t.getIntOr("industrialTripReason",0).coerceIn(0,7)
        fun finite(k: String,max: Double)=t.getDoubleOr(k,0.0).takeIf { it.isFinite() }?.coerceIn(0.0,max) ?: 0.0
        protection.armed=t.getBooleanOr("industrialArmed",false);protection.elapsed=finite("industrialTripTime",60.0);protection.pendingReason=t.getIntOr("industrialPendingReason",0).coerceIn(0,7);lifetimeJoules=finite("industrialLifetime",1e18)
        if(kind==IndustrialKind.SHEDDER)for(i in 0..3) { shed.closed[i]=t.getBooleanOr("industrialShed$i",true);shed.nominalDemand[i]=finite("industrialShedDemand$i",1e12) }
        configure()
    }
    /** Parse a complete settings packet before changing any server state. */
    fun packet(id: Byte,s: DataInputStream): Boolean = try {
        when(id.toInt()) {
            80->applySettings(DoubleArray(settings.size) { s.readDouble() })
            81->{ setEnabled(s.readBoolean());true }
            82->{ if(kind==IndustrialKind.RELAY)resetTrip();true }
            86->{ retryShed();true }
            else->false
        }
    } catch(_: IOException) { false }
}
