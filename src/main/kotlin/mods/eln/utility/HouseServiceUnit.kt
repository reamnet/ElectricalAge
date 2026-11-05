package mods.eln.utility

import mods.eln.sim.IProcess
import mods.eln.sim.ElectricalConnection
import mods.eln.sim.mna.component.*
import mods.eln.sim.mna.state.State
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.transparentnode.standby.TransferContact
import net.minecraft.nbt.CompoundTag

/** Passive service equivalents. Registers count accepted physical time, not solver retries. */
class HouseServiceUnit(val meter: Boolean,val serviceDisconnect: Boolean,states: MutableList<State>,parts: MutableList<Component>,processes: MutableList<IProcess>) {
    val line=List(3) { NbtElectricalLoad("serviceLine$it").apply { rs=.00001 } }
    val load=if(meter || serviceDisconnect)List(3) { NbtElectricalLoad("serviceLoad$it").apply { rs=.00001 } } else line
    val contacts=if(meter || serviceDisconnect)List(3) { TransferContact(line[it],load[it]) } else emptyList()
    val energy=ServiceEnergy()
    var meterType=0;private set
    var disconnected=false;private set
    var circuit=2;private set
    private val taps: List<ServiceTap>
    val references: List<Resistor>
    val watts get()=contacts.indices.sumOf { line[it].u*contacts[it].current }
    init {
        states.addAll(line);if(load!==line)states.addAll(load);parts.addAll(contacts)
        taps=(0..1).map { i->
            val pin=NbtElectricalLoad("wiredServiceTap$i");states.add(pin)
            parts.add(Resistor(pin,line[i]).setR(.0002))
            ServiceTap(line[2],pin,.5).apply { enabled=false }.also(parts::add)
        }
        references=(if(load===line)line else line+load).map { Resistor(it,null).setR(1e10) };parts.addAll(references)
        processes.add(IProcess { dt->
            if(!dt.isFinite() || dt<=0)return@IProcess
            if(meter)energy.step(dt,watts)
            setWired(line[2].connectedComponents.any { it is ElectricalConnection })
        })
        configure()
    }
    fun setWired(value: Boolean) { taps.forEach { it.enabled=value } }
    fun selectCircuit(value: Int): Boolean { if(value !in 0..2)return false;circuit=value;return true }
    fun installMeter(type: Int,data: CompoundTag?) {
        meterType=if(meter && type in 1..2)type else 0
        energy.read(if(meterType==0)CompoundTag() else data ?: CompoundTag())
        disconnected=meterType==2 && data?.getBooleanOr("meterDisconnected",false)==true
        configure()
    }
    fun setRemoteDisconnect(open: Boolean): Boolean {
        if(!serviceDisconnect && meterType!=2)return false
        disconnected=open;configure();return true
    }
    fun configure() { contacts.forEach { it.closed=if(serviceDisconnect)!disconnected else meterType!=0 && !(meterType==2 && disconnected) } }
    fun write(t: CompoundTag) { energy.write(t);t.putInt("serviceCircuit",circuit);t.putBoolean("meterDisconnected",disconnected);t.putInt("serviceTerminal",50) }
    fun read(t: CompoundTag) { selectCircuit(t.getIntOr("serviceCircuit",2));energy.read(t);disconnected=(serviceDisconnect || meterType==2) && t.getBooleanOr("meterDisconnected",false);configure() }
}
