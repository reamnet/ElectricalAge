package mods.eln.transparentnode.industrial

import mods.eln.sim.IProcess
import mods.eln.sim.mna.component.Component
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.mna.state.State
import mods.eln.sim.nbt.NbtElectricalLoad
import net.minecraft.nbt.CompoundTag
import java.io.DataInputStream
import java.io.IOException
import kotlin.math.*

/** ForkPatch's passive resistor and fan cooling, accounted once per accepted physical step.
 * No constant-power controller, probes or repeated LU invalidation on a steady load.
 */
class LoadBankUnit(states: MutableList<State>,parts: MutableList<Component>,processes: MutableList<IProcess>,
    private val loaded: ()->Boolean={true},private val control: ()->Double={0.0}) {
    var nominal=480.0;private set
    var demand=50000.0;private set
    var enabled=true;private set
    var temperature=20.0;private set
    var lifetimeJoules=0.0;private set
    var working=false;private set
    val input=NbtElectricalLoad("industrialInput").apply { rs=.0001 }
    val resistor=Resistor(input,null).setR(1e19)
    var volts=0.0;private set
    var watts=0.0;private set
    var amps=0.0;private set
    init { states.add(input);parts.add(resistor);processes.add(IProcess(::step));prepare() }
    fun applySettings(values: DoubleArray): Boolean {
        if(!IndustrialKind.LOAD_BANK.valid(values))return false
        nominal=values[0];demand=values[1];prepare();return true
    }
    fun setEnabled(on: Boolean) { enabled=on;prepare() }
    /** This can run repeatedly without advancing temperature or the energy meter. */
    fun prepare() {
        working=enabled && loaded() && control()<.6 && temperature<120 &&
            input.u.isFinite() && abs(input.u)<=nominal*1.3 && demand>0
        resistor.r=IndustrialEnergy.resistance(nominal,if(working)demand else 0.0)
    }
    fun step(dt: Double) {
        if(!dt.isFinite() || dt<=0)return
        val power=resistor.p
        if(power.isFinite() && power>=0) {
            // Capture before prepare changes conductance. Displays report the accepted interval,
            // including the last powered interval at a thermal/voltage/control boundary.
            volts=input.u;watts=power;amps=abs(resistor.current)
            lifetimeJoules=(lifetimeJoules+power*dt).coerceAtMost(1e18)
            temperature+=(power-(temperature-20)*max(100.0,demand/45))*dt/12000
        }
        prepare()
    }
    fun write(t: CompoundTag) {
        t.putDouble("industrialSetting0",nominal);t.putDouble("industrialSetting1",demand)
        t.putBoolean("industrialEnabled",enabled);t.putDouble("industrialTemperature",temperature)
        t.putDouble("industrialLifetime",lifetimeJoules)
    }
    fun read(t: CompoundTag) {
        val p=doubleArrayOf(t.getDoubleOr("industrialSetting0",480.0),t.getDoubleOr("industrialSetting1",50000.0))
        if(IndustrialKind.LOAD_BANK.valid(p)) { nominal=p[0];demand=p[1] }
        enabled=t.getBooleanOr("industrialEnabled",true)
        fun finite(key: String,default: Double,min: Double,max: Double)=t.getDoubleOr(key,default).takeIf { it.isFinite() }?.coerceIn(min,max) ?: default
        temperature=finite("industrialTemperature",20.0,-50.0,1000.0)
        lifetimeJoules=finite("industrialLifetime",0.0,0.0,1e18);prepare()
    }
    fun packet(id: Byte,s: DataInputStream): Boolean = try {
        when(id.toInt()) {
            80->applySettings(doubleArrayOf(s.readDouble(),s.readDouble()))
            81->{ setEnabled(s.readBoolean());true }
            else->false
        }
    }catch(_: IOException) { false }
}
