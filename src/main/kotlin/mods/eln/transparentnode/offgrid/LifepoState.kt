package mods.eln.transparentnode.offgrid

import net.minecraft.nbt.CompoundTag

/** Stored energy is measured in joules. The cabinet contains cells and a BMS, never an inverter. */
class LifepoState(val kwh: Int) {
    val capacity=kwh*3_600_000.0
    val maxAmps=kwh/16.0*100.0
    var energy=0.0;private set
    var enabled=true
    var fault=""
    val fraction get()=(energy/capacity).coerceIn(0.0,1.0)
    val cellVolts get()=50.0+4.0*fraction
    fun transfer(joules: Double): Double {
        if(!joules.isFinite())return 0.0
        val before=energy;energy=(energy+joules).coerceIn(0.0,capacity);return energy-before
    }
    fun read(tag: CompoundTag) {
        energy=tag.getDoubleOr("lfpJoules", 0.0).takeIf { it.isFinite() }?.coerceIn(0.0,capacity) ?: 0.0
        enabled=!tag.contains("lfpEnabled") || tag.getBooleanOr("lfpEnabled", true)
        fault=tag.getStringOr("lfpFault", "").takeIf { it in setOf("OVERVOLTAGE","REVERSE","TEMPERATURE") } ?: ""
    }
    fun write(tag: CompoundTag) { tag.putDouble("lfpJoules",energy);tag.putBoolean("lfpEnabled",enabled);tag.putString("lfpFault",fault) }
}
