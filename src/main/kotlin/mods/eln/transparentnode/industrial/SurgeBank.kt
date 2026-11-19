package mods.eln.transparentnode.industrial

import mods.eln.sim.IProcess
import mods.eln.sim.mna.component.Component
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.mna.state.State
import net.minecraft.nbt.CompoundTag
import kotlin.math.abs
import kotlin.math.max

/** Source finite-cartridge shunts. Each physical interval is real dissipation, not a trial debit.
 * Cartridge exhaustion opens on the following solve; the final interval is never erased.
 */
class SurgeBank(lines:List<State>,parts:MutableList<Component>,processes:MutableList<IProcess>,
    nominal:Double=12470.0,private val active:()->Boolean={true}) {
    companion object {const val CAPACITY=200000.0;const val LIGHTNING=40000.0}
    var nominal=nominal;private set
    var remaining=CAPACITY;private set
    var absorbedJoules=0.0;private set
    val shunts=lines.map { Resistor(it,null).setR(1e19) }
    init {require(nominal.isFinite() && nominal>0);parts.addAll(shunts);if(shunts.isNotEmpty())processes.add(IProcess(::step))}
    fun setNominal(value:Double):Boolean {if(!value.isFinite() || value !in 12.0..230000.0)return false;nominal=value;prepare();return true}
    fun prepare() {
        val on=active() && remaining>0 && shunts.any { it.u.isFinite() && abs(it.u)>nominal*1.3 }
        val r=if(on)nominal*nominal/(500000.0/shunts.size) else 1e19
        shunts.forEach { it.r=r }
    }
    fun step(dt:Double) {
        if(!dt.isFinite() || dt<=0)return
        val watts=shunts.sumOf { max(0.0,it.p) }
        if(watts.isFinite() && watts>=0) {
            val joules=watts*dt
            absorbedJoules=(absorbedJoules+joules).coerceAtMost(1e18)
            remaining=max(0.0,remaining-joules)
        }
        prepare()
    }
    fun lightning():Boolean {
        if(!active() || remaining<LIGHTNING)return false
        remaining-=LIGHTNING
        // Lightning is external energy. It must not activate an extra load on a healthy grid.
        prepare();return true
    }
    fun restore(joules:Double) {remaining=joules.takeIf { it.isFinite() }?.coerceIn(0.0,CAPACITY) ?: 0.0;prepare()}
    fun write(t:CompoundTag,key:String) {t.putDouble(key,remaining)}
    fun read(t:CompoundTag,key:String,default:Double=CAPACITY)=restore(t.getDoubleOr(key,default))
}
