package mods.eln.transparentnode.industrial

import mods.eln.sim.IProcess
import mods.eln.sim.mna.component.*
import mods.eln.sim.mna.state.State
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.utility.ServiceTap
import net.minecraft.nbt.CompoundTag
import java.io.*
import kotlin.math.*

/** Scalar V/Hz control with a power-conserving native tap and physical output impedance.
 * The current setting limits short-circuit current at nominal input, not an ideal current clamp.
 */
class MotorDriveUnit(states:MutableList<State>,parts:MutableList<Component>,processes:MutableList<IProcess>,
    private val inhibit:()->Double={0.0},private val tripOutput:(Boolean)->Unit={}) {
    var settings=IndustrialKind.DRIVE.defaults.copyOf();private set
    var enabled=true;private set
    var hertz=0.0;private set
    var temperature=20.0;private set
    var tripped=false;private set
    var lifetimeJoules=0.0;private set
    var inputWatts=0.0;private set
    var outputWatts=0.0;private set
    var lossWatts=0.0;private set
    val input=NbtElectricalLoad("industrialInput").apply { rs=.0001 }
    private val pin=NbtElectricalLoad("industrialTap").apply { rs=.0001 }
    val output=NbtElectricalLoad("industrialOutput0").apply { rs=.0001 }
    val tap=ServiceTap(input,pin,1.0).apply { enabled=false }
    val winding=Resistor(pin,output).setR(settings[1]/settings[4])
    val references=listOf(input,output).map { Resistor(it,null).setR(1e10) }
    val outputCurrent get()=abs(winding.current)
    init { states.addAll(listOf(input,pin,output));parts.addAll(listOf(tap,winding)+references);processes.add(IProcess(::step));configure() }
    private fun configure() {
        tap.ratio=max(.000001,settings[1]/settings[0]*hertz/60.0)
        winding.r=settings[1]/settings[4]
        tap.enabled=!tripped && hertz>.01
        tripOutput(tripped)
    }
    fun applySettings(values:DoubleArray):Boolean {
        if(!IndustrialKind.DRIVE.valid(values))return false
        settings=values.copyOf();configure();return true
    }
    fun setEnabled(value:Boolean) { enabled=value;configure() }
    fun resetTrip():Boolean {
        if(temperature>=80)return false
        tripped=false;configure();return true
    }
    fun step(dt:Double) {
        if(!dt.isFinite() || dt<=0)return
        val loss=winding.p
        if(loss.isFinite() && loss>=0) {
            lossWatts=loss;inputWatts=input.u*tap.current;outputWatts=output.u*winding.current
            // Winding heat includes the final solved interval before a trip opens the tap.
            lifetimeJoules=(lifetimeJoules+max(0.0,inputWatts)*dt).coerceAtMost(1e18)
            temperature+=(loss-(temperature-20)*12)*dt/15000
        }
        if(temperature>120)tripped=true
        hertz=IndustrialEnergy.ramp(hertz,if(enabled && !tripped && inhibit()<.6)settings[2] else 0.0,dt,settings[3])
        configure()
    }
    fun write(t:CompoundTag) {
        settings.forEachIndexed { i,v->t.putDouble("industrialSetting$i",v) }
        t.putBoolean("industrialEnabled",enabled);t.putBoolean("industrialTrip",tripped)
        t.putInt("industrialTripReason",if(tripped)6 else 0)
        t.putDouble("industrialTemperature",temperature);t.putDouble("industrialLifetime",lifetimeJoules)
        // Like the source, restart at zero frequency after loading; do not reapply a live motor command.
    }
    fun read(t:CompoundTag) {
        val p=DoubleArray(5) { t.getDoubleOr("industrialSetting$it",IndustrialKind.DRIVE.defaults[it]) }
        if(IndustrialKind.DRIVE.valid(p))settings=p
        enabled=t.getBooleanOr("industrialEnabled",true);tripped=t.getBooleanOr("industrialTrip",false)
        fun finite(k:String,default:Double,min:Double,max:Double)=t.getDoubleOr(k,default).takeIf { it.isFinite() }?.coerceIn(min,max) ?: default
        temperature=finite("industrialTemperature",20.0,-50.0,1000.0);lifetimeJoules=finite("industrialLifetime",0.0,0.0,1e18)
        hertz=0.0;configure()
    }
    fun packet(id:Byte,s:DataInputStream):Boolean=try {
        when(id.toInt()) { 80->applySettings(DoubleArray(5) { s.readDouble() });81->{setEnabled(s.readBoolean());true};82->resetTrip();else->false }
    }catch(_:IOException) { false }
}
