package mods.eln.transparentnode.industrial

import mods.eln.sim.IProcess
import mods.eln.sim.mna.component.*
import mods.eln.sim.mna.state.State
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.utility.ServiceTap
import mods.eln.utility.ServiceSecondary
import net.minecraft.nbt.CompoundTag
import java.io.*
import kotlin.math.abs

/** One paid passive winding supplies every service output; fuse current is measured on its primary. */
class PadTransformerUnit(loads: MutableList<State>,components: MutableList<Component>,processes: MutableList<IProcess>,
    private val blowFuse: ()->Unit={},private val tripOutput: (Boolean)->Unit={}) {
    var primaryVolts=12470.0;private set
    var secondaryVolts=240.0;private set
    var enabled=true;private set
    var ratedWatts=0;private set
    var fuseAmps=0.0;private set
    val protection=Protection()
    val input=NbtElectricalLoad("industrialInput").apply { rs=.0001 }
    private val pin=NbtElectricalLoad("industrialTap").apply { rs=.0001 }
    val output=NbtElectricalLoad("industrialOutput0").apply { rs=.0001 }
    val tap=ServiceTap(input,pin,secondaryVolts/primaryVolts).apply { enabled=false }
    val winding=Resistor(pin,output).setR(.015)
    val service=ServiceSecondary(loads,components,output) { 240.0 }
    val references: List<Resistor>
    val inputWatts get()=input.u*tap.current
    val primaryAmps get()=abs(tap.current)
    val secondaryAmps get()=abs(winding.current)
    val status get()=when {
        protection.tripped->when(protection.reason) { 2->"PRIMARY_FUSE";7->"CORE_OVERLOAD";1->"OVERVOLTAGE";else->"INVALID" }
        !enabled->"DISABLED"
        ratedWatts<=0->"NEEDS_TRANSFORMER"
        fuseAmps<=0->"NEEDS_FUSE"
        abs(input.u)<1->"WAIT_POWER"
        else->"ONLINE"
    }
    init {
        loads.addAll(listOf(input,pin,output));components.addAll(listOf(tap,winding))
        references=(listOf(input,output)+service.loads).map { Resistor(it,null).setR(1e10) };components.addAll(references)
        processes.add(IProcess(::step))
    }
    fun install(watts: Int?,amps: Double?) {
        ratedWatts=watts?.coerceAtLeast(0) ?: 0
        fuseAmps=amps?.takeIf { it.isFinite() && it>0 } ?: 0.0
        configure()
    }
    fun configure() {
        tap.ratio=secondaryVolts/primaryVolts
        tap.enabled=enabled && !protection.tripped && ratedWatts>0 && fuseAmps>0
        tripOutput(protection.tripped)
    }
    fun applySettings(p: DoubleArray): Boolean {
        if(!IndustrialKind.PAD.valid(p))return false
        primaryVolts=p[0];secondaryVolts=p[1];configure();return true
    }
    fun setEnabled(value: Boolean) { enabled=value;configure() }
    fun resetTrip() { protection.reset();configure() }
    fun step(dt: Double) {
        if(!dt.isFinite() || dt<=0 || !tap.enabled)return
        if(protection.stepPad(dt,input.u,tap.current,primaryVolts,fuseAmps,ratedWatts.toDouble())) {
            if(protection.reason==2) { fuseAmps=0.0;blowFuse() }
            configure()
        }
    }
    fun write(t: CompoundTag) {
        t.putDouble("industrialSetting0",primaryVolts);t.putDouble("industrialSetting1",secondaryVolts);t.putBoolean("industrialEnabled",enabled)
        t.putBoolean("industrialTrip",protection.tripped);t.putInt("industrialTripReason",protection.reason)
        t.putDouble("industrialTripTime",protection.elapsed);t.putInt("industrialPendingReason",protection.pendingReason)
    }
    fun read(t: CompoundTag) {
        applySettings(doubleArrayOf(t.getDoubleOr("industrialSetting0",12470.0),t.getDoubleOr("industrialSetting1",240.0)))
        enabled=t.getBooleanOr("industrialEnabled",true);protection.tripped=t.getBooleanOr("industrialTrip",false);protection.reason=t.getIntOr("industrialTripReason",0).coerceIn(0,7)
        protection.elapsed=t.getDoubleOr("industrialTripTime",0.0).takeIf { it.isFinite() }?.coerceIn(0.0,2.0) ?: 0.0
        protection.pendingReason=t.getIntOr("industrialPendingReason",0).coerceIn(0,7);configure()
    }
    fun packet(id: Byte,s: DataInputStream): Boolean=try {
        when(id.toInt()) {
            80->applySettings(doubleArrayOf(s.readDouble(),s.readDouble()))
            81->{ setEnabled(s.readBoolean());true }
            82->{ resetTrip();true }
            else->false
        }
    } catch(_: IOException) { false }
}
