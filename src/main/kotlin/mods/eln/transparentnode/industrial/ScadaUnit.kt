package mods.eln.transparentnode.industrial

import mods.eln.i18n.I18N.tr
import mods.eln.sim.IProcess
import mods.eln.sim.mna.component.*
import mods.eln.sim.mna.state.State
import mods.eln.sim.nbt.NbtElectricalLoad
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import java.io.*
import kotlin.math.abs

/** Eight explicit world-local addresses; clocks and the 20W supply use physical steps only. */
class ScadaUnit(states:MutableList<State>,parts:MutableList<Component>,processes:MutableList<IProcess>,
    private val origin:()->BlockPos?,private val loaded:()->Boolean,private val inhibit:()->Double,
    private val readTarget:(Long,Int)->String,private val commandTarget:(Long,Int,Boolean)->Boolean) {
    var settings=IndustrialKind.SCADA.defaults.copyOf();private set
    var enabled=true;private set
    var selected=0;private set
    val targets=LongArray(8) { Long.MIN_VALUE };val circuits=IntArray(8)
    var telemetry="";private set
    var lifetimeJoules=0.0;private set
    val input=NbtElectricalLoad("industrialInput").apply { rs=.0001 }
    val resistor=Resistor(input,null).setR(1e19)
    private var pollClock=0.0
    val canCommand get()=enabled && loaded() && input.u.isFinite() && abs(input.u) in 90.0..156.0 && inhibit()<.6
    init { states.add(input);parts.add(resistor);processes.add(IProcess(::step));prepare() }
    private fun prepare() { resistor.r=if(enabled && loaded() && input.u.isFinite() && abs(input.u)<=156 && inhibit()<.6)720.0 else 1e19 }
    fun applySettings(values:DoubleArray):Boolean { if(!IndustrialKind.SCADA.valid(values))return false;settings=values.copyOf();return true }
    fun setEnabled(value:Boolean) { enabled=value;prepare() }
    fun bind(values:DoubleArray=settings):Boolean {
        if(!IndustrialKind.SCADA.valid(values))return false
        val here=origin() ?: return false
        val at=BlockPos(values[0].toInt(),values[1].toInt(),values[2].toInt())
        if(at.distSqr(here)>256.0*256)return false
        settings=values.copyOf();targets[selected]=at.asLong();circuits[selected]=values[3].toInt()-1;return true
    }
    fun select(index:Int):Boolean {
        if(index !in 0..7)return false
        selected=index
        if(targets[index]!=Long.MIN_VALUE) {
            val p=BlockPos.of(targets[index]);settings=doubleArrayOf(p.x.toDouble(),p.y.toDouble(),p.z.toDouble(),circuits[index]+1.0)
        }
        return true
    }
    fun poll() {
        telemetry=if(targets[selected]==Long.MIN_VALUE)tr("Target unavailable or chunk unloaded") else readTarget(targets[selected],circuits[selected]).take(2000)
    }
    fun command(closed:Boolean):Boolean {
        val accepted=canCommand && targets[selected]!=Long.MIN_VALUE && commandTarget(targets[selected],circuits[selected],closed)
        telemetry=if(accepted)tr("Command accepted") else tr("Command rejected: check console power, target type, chunk or trip latch")
        return accepted
    }
    fun step(dt:Double) {
        if(!dt.isFinite() || dt<=0)return
        val p=resistor.p;if(p.isFinite() && p>=0)lifetimeJoules=(lifetimeJoules+p*dt).coerceAtMost(1e18)
        if(loaded()) { pollClock+=dt;if(pollClock>=1.0) { pollClock%=1.0;poll() } }
        prepare()
    }
    fun write(t:CompoundTag) {
        settings.forEachIndexed { i,v->t.putDouble("industrialSetting$i",v) };t.putBoolean("industrialEnabled",enabled)
        t.putInt("industrialTarget",selected);t.putLongArray("industrialTargets",targets);t.putIntArray("industrialCircuits",circuits)
        t.putDouble("industrialLifetime",lifetimeJoules)
    }
    fun read(t:CompoundTag) {
        applySettings(DoubleArray(4) { t.getDoubleOr("industrialSetting$it",IndustrialKind.SCADA.defaults[it]) })
        enabled=t.getBooleanOr("industrialEnabled",true);selected=t.getIntOr("industrialTarget",0).coerceIn(0,7)
        t.getLongArray("industrialTargets").orElse(LongArray(0)).takeIf { it.size==8 }?.copyInto(targets)
        t.getIntArray("industrialCircuits").orElse(IntArray(0)).takeIf { it.size==8 }?.forEachIndexed { i,v->circuits[i]=v.coerceIn(0,11) }
        lifetimeJoules=t.getDoubleOr("industrialLifetime",0.0).takeIf { it.isFinite() }?.coerceIn(0.0,1e18) ?: 0.0
        pollClock=0.0;prepare()
    }
    /** True means a complete supported packet was handled, including rejected-command feedback. */
    fun packet(id:Byte,s:DataInputStream):Boolean=try {
        when(id.toInt()) {
            80->applySettings(DoubleArray(4) { s.readDouble() })
            81->{setEnabled(s.readBoolean());true}
            83->bind()
            84->select(s.readInt())
            85->{command(s.readBoolean());true}
            87->bind(DoubleArray(4) { s.readDouble() })
            else->false
        }
    }catch(_:IOException) { false }
}
