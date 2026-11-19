package mods.eln.transparentnode.industrial

import mods.eln.sim.IProcess
import mods.eln.sim.mna.component.*
import mods.eln.sim.mna.state.State
import mods.eln.sim.nbt.NbtElectricalLoad
import net.minecraft.nbt.CompoundTag
import java.io.*
import kotlin.math.abs

interface WeldJob {
    /** Stable while the workpiece's item and components remain the same; count is irrelevant. */
    val identity: Any?
    fun ready():Boolean
    fun complete():Boolean
}

/** The source's 120 kJ cycle: a passive load, one output, no progress while the output is blocked. */
class WelderUnit(states: MutableList<State>,parts: MutableList<Component>,processes: MutableList<IProcess>,
    private val job: WeldJob,private val loaded: ()->Boolean={true},private val control: ()->Double={0.0}) {
    var nominal=240.0;private set
    var demand=6000.0;private set
    var enabled=true;private set
    var workJoules=0.0;private set
    var lifetimeJoules=0.0;private set
    var working=false;private set
    var watts=0.0;private set
    var volts=0.0;private set
    val input=NbtElectricalLoad("industrialInput").apply { rs=.0001 }
    val resistor=Resistor(input,null).setR(1e19)
    private var bound:Any?=null
    private var prepared:Any?=null
    init { states.add(input);parts.add(resistor);processes.add(IProcess(::step)) }
    fun applySettings(p:DoubleArray):Boolean {
        if(!IndustrialKind.WELDER.valid(p))return false
        nominal=p[0];demand=p[1];prepare();return true
    }
    fun setEnabled(on:Boolean) { enabled=on;prepare() }
    fun prepare() {
        val identity=job.identity
        if(identity!==bound) { workJoules=0.0;bound=identity }
        val signal=control()
        working=enabled && loaded() && signal.isFinite() && signal<.6 &&
            input.u.isFinite() && abs(input.u)<=nominal*1.3 && job.ready()
        prepared=if(working)identity else null
        resistor.r=IndustrialEnergy.resistance(nominal,if(working)demand else 0.0)
    }
    fun step(dt:Double) {
        if(!dt.isFinite() || dt<=0)return
        val power=resistor.p
        if(power.isFinite() && power>=0) {
            watts=power;volts=input.u;lifetimeJoules=(lifetimeJoules+power*dt).coerceAtMost(1e18)
            if(prepared!=null && job.identity===prepared && job.ready()) {
                workJoules=(workJoules+power*dt).coerceAtMost(120000.0)
                if(workJoules>=120000.0 && job.complete()) { workJoules=0.0;bound=null }
            }
        }
        prepare()
    }
    fun writeSettings(t:CompoundTag) { t.putDouble("industrialSetting0",nominal);t.putDouble("industrialSetting1",demand);t.putBoolean("industrialEnabled",enabled) }
    fun write(t:CompoundTag) { writeSettings(t);t.putDouble("industrialWork",workJoules);t.putDouble("industrialLifetime",lifetimeJoules) }
    fun readSettings(t:CompoundTag) {
        val p=doubleArrayOf(t.getDoubleOr("industrialSetting0",240.0),t.getDoubleOr("industrialSetting1",6000.0))
        if(IndustrialKind.WELDER.valid(p)) { nominal=p[0];demand=p[1] }
        enabled=t.getBooleanOr("industrialEnabled",true);prepare()
    }
    fun read(t:CompoundTag,resume:Boolean=true) {
        readSettings(t);bound=job.identity
        workJoules=if(resume)t.getDoubleOr("industrialWork",0.0).takeIf { it.isFinite() }?.coerceIn(0.0,120000.0) ?: 0.0 else 0.0
        lifetimeJoules=t.getDoubleOr("industrialLifetime",0.0).takeIf { it.isFinite() }?.coerceIn(0.0,1e18) ?: 0.0
        prepare()
    }
    fun packet(id:Byte,s:DataInputStream):Boolean=try {
        when(id.toInt()) { 80->applySettings(doubleArrayOf(s.readDouble(),s.readDouble()));81->{setEnabled(s.readBoolean());true};else->false }
    }catch(_:IOException) { false }
}
