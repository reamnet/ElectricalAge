package mods.eln.utility

import net.minecraft.world.phys.Vec3

/** Common can geometry for items, installed models, bushings and cable clearance. */
internal data class PoleTransformerShape(val diameter: Double,val height: Double) {
    companion object {
        const val BACK=-.24
        const val TOP=5.535
        fun of(watts: Int)=when {
            watts<=10_000 -> PoleTransformerShape(.72,.72)
            watts<=25_000 -> PoleTransformerShape(.90,.88)
            watts<=50_000 -> PoleTransformerShape(1.10,1.10)
            watts<=75_000 -> PoleTransformerShape(1.28,1.25)
            else -> PoleTransformerShape(1.42,1.40)
        }
    }
    val bushing get()=Vec3(BACK-.315*diameter,5.715,0.0)
    fun point(original: Vec3)=Vec3(BACK+(original.x+.015)*diameter,TOP+(original.y-TOP)*height,original.z*diameter)
}
