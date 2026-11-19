package mods.eln.utility
import net.minecraft.nbt.CompoundTag

/** Records both directions independently; reverse flow must not erase the import register. */
class ServiceEnergy {
    var importedKwh=0.0
        private set
    var exportedKwh=0.0
        private set
    fun step(seconds: Double,watts: Double) {
        if(!seconds.isFinite() || seconds<=0 || !watts.isFinite())return
        if(watts>=0)importedKwh+=watts*seconds/3_600_000 else exportedKwh-=watts*seconds/3_600_000
    }
    fun read(tag: CompoundTag) {
        importedKwh=tag.getDoubleOr("meterImportKwh",0.0).takeIf { it.isFinite() && it>=0 } ?: 0.0
        exportedKwh=tag.getDoubleOr("meterExportKwh",0.0).takeIf { it.isFinite() && it>=0 } ?: 0.0
    }
    fun write(tag: CompoundTag) { tag.putDouble("meterImportKwh",importedKwh);tag.putDouble("meterExportKwh",exportedKwh) }
}

