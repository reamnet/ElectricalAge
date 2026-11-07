package mods.eln.sixnode.electricalcable

import mods.eln.sim.nbt.NbtThermalLoad
import net.minecraft.nbt.CompoundTag

/** Native Tc stays relative to the fixed 20 C reference. Biome changes never change stored heat. */
class WireThermalLoad(private val key: String,val physics: WireThermalPhysics): NbtThermalLoad(key) {
    private var initialized=false
    var phaseJoules=0.0;private set
    val absoluteCelsius get()=Tc+20.0
    val storedJoules get()=physics.sensibleEnthalpy(absoluteCelsius)+phaseJoules
    val failed get()=phaseJoules>=physics.fusionJoules
    fun updateProperties(air: Double,surrounding: Double,insulated: Boolean) {
        require(air.isFinite() && surrounding.isFinite())
        if(!initialized) { Tc=air-20.0;initialized=true }
        ambient=surrounding-20.0;C=physics.capacity(absoluteCelsius);Rs=physics.endpointThermalResistance
        Rp=1.0/physics.coolingConductance(absoluteCelsius,surrounding,insulated)
    }
    override fun integrateEnergy(joules: Double) {
        require(joules.isFinite());initialized=true
        val energy=storedJoules+joules;Tc=physics.temperature(energy)-20.0
        phaseJoules=(energy-physics.meltingEnthalpy).coerceAtLeast(0.0);C=physics.capacity(absoluteCelsius)
    }
    fun flushPendingEnergy() { val paid=EnergyTemp;EnergyTemp=0.0;if(paid!=0.0)integrateEnergy(paid) }
    fun inheritHeat(from: WireThermalLoad) { from.flushPendingEnergy();Tc=from.Tc;phaseJoules=from.phaseJoules;initialized=true }
    override fun readFromNBT(t: CompoundTag,p: String) {
        super.readFromNBT(t,p);initialized=true
        t.getDoubleOr(p+key+"NativeTc",Double.NaN).takeIf { it.isFinite() }?.let { Tc=it }
        // Source saves used a moving ambient reference; native saves explicitly use fixed Tc.
        if(!t.getBooleanOr(p+key+"NativeAbsolute",false)) {
            val air=t.getDoubleOr(p+key+"AmbientC",20.0);if(air.isFinite())Tc+=air-20.0
        }
        phaseJoules=t.getDoubleOr(p+key+"FusionJ",0.0).takeIf { it.isFinite() && it>=0 } ?: 0.0
        C=physics.capacity(absoluteCelsius)
    }
    override fun writeToNBT(t: CompoundTag,p: String) {
        flushPendingEnergy();super.writeToNBT(t,p);t.putBoolean(p+key+"NativeAbsolute",true);t.putDouble(p+key+"NativeTc",Tc);t.putDouble(p+key+"FusionJ",phaseJoules)
    }
}
