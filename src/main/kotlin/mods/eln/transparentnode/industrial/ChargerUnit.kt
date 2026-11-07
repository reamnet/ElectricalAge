package mods.eln.transparentnode.industrial

import mods.eln.sim.IProcess
import mods.eln.sim.mna.component.*
import mods.eln.sim.mna.state.State
import mods.eln.sim.nbt.NbtElectricalLoad
import net.minecraft.nbt.CompoundTag
import java.io.*
import kotlin.math.*

interface ChargeTarget {
    val identity: Any
    val capacity: Double
    val chargeWatts: Double
    var energy: Double
}

/** Only accepted resistor energy reaches the removable pack, at the source's 94% efficiency. */
class ChargerUnit(states: MutableList<State>,parts: MutableList<Component>,processes: MutableList<IProcess>,
    private val target: ()->ChargeTarget?,private val tick: ()->Long={0},
    private val loaded: ()->Boolean={true},private val control: ()->Double={0.0}) {
    var settings=IndustrialKind.CHARGER.defaults.copyOf();private set
    val nominal get()=settings[0]
    val demand get()=settings[1]
    var enabled=true;private set
    var sessionJoules=0.0;private set
    var lifetimeJoules=0.0;private set
    var working=false;private set
    var watts=0.0;private set
    var volts=0.0;private set
    val input=NbtElectricalLoad("industrialInput").apply { rs=.0001 }
    val resistor=Resistor(input,null).setR(1e19)
    private var bound: Any?=null
    private var firstBinding=true
    private var prepared: ChargeTarget?=null
    init { states.add(input);parts.add(resistor);processes.add(IProcess(::step)) }
    fun applySettings(p: DoubleArray): Boolean {
        if(!IndustrialKind.CHARGER.valid(p))return false
        settings=p.copyOf();prepare();return true
    }
    fun setEnabled(on: Boolean) { enabled=on;prepare() }
    fun prepare() {
        val pack=target()
        if(pack?.identity!==bound) { if(!firstBinding)sessionJoules=0.0;bound=pack?.identity }
        // Empty initialization must not discard a restored session before its inventory is read.
        if(pack!=null)firstBinding=false
        val signal=control()
        working=enabled && loaded() && signal.isFinite() && signal<.6 &&
            input.u.isFinite() && abs(input.u)<=nominal*1.3 &&
            IndustrialEnergy.schedule(tick(),settings[2].toInt(),settings[3].toInt()) &&
            pack!=null && pack.capacity.isFinite() && pack.capacity>0 && pack.chargeWatts.isFinite() && pack.chargeWatts>0 &&
            pack.energy.isFinite() && pack.energy>=0 && pack.energy<pack.capacity-.01
        prepared=if(working)pack else null
        resistor.r=IndustrialEnergy.resistance(nominal,if(working)min(demand,pack!!.chargeWatts) else 0.0)
    }
    fun step(dt: Double) {
        if(!dt.isFinite() || dt<=0)return
        val power=resistor.p
        if(power.isFinite() && power>=0) {
            watts=power;volts=input.u
            lifetimeJoules=(lifetimeJoules+power*dt).coerceAtMost(1e18)
            val pack=target()
            if(pack!=null && prepared?.identity===pack.identity) {
                val stored=minOf((pack.capacity-pack.energy).coerceAtLeast(0.0),power*dt*.94,pack.chargeWatts*dt)
                pack.energy=(pack.energy+stored).coerceIn(0.0,pack.capacity)
                sessionJoules=(sessionJoules+stored).coerceAtMost(1e18)
            }
        }
        prepare()
    }
    fun writeSettings(t: CompoundTag) { settings.forEachIndexed { i,v->t.putDouble("industrialSetting$i",v) };t.putBoolean("industrialEnabled",enabled) }
    fun write(t: CompoundTag) { writeSettings(t);t.putDouble("industrialSession",sessionJoules);t.putDouble("industrialLifetime",lifetimeJoules) }
    fun readSettings(t: CompoundTag) {
        val p=DoubleArray(4) { t.getDoubleOr("industrialSetting$it",IndustrialKind.CHARGER.defaults[it]) }
        if(IndustrialKind.CHARGER.valid(p))settings=p
        enabled=t.getBooleanOr("industrialEnabled",true);prepare()
    }
    fun read(t: CompoundTag) {
        readSettings(t)
        fun finite(key: String)=t.getDoubleOr(key,0.0).takeIf { it.isFinite() }?.coerceIn(0.0,1e18) ?: 0.0
        sessionJoules=finite("industrialSession");lifetimeJoules=finite("industrialLifetime")
        bound=null;firstBinding=true;prepare()
    }
    fun packet(id: Byte,s: DataInputStream):Boolean=try {
        when(id.toInt()) { 80->applySettings(DoubleArray(4) { s.readDouble() });81->{setEnabled(s.readBoolean());true};else->false }
    }catch(_:IOException) { false }
}
