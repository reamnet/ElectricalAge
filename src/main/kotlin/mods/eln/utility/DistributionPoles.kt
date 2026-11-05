package mods.eln.utility
import mods.eln.Eln
import net.minecraft.world.item.Items

object DistributionPoles {
    /** Explicit source offsets; hidden fixtures remain available only for legacy worlds. */
    val supported=listOf(DistributionKind.SINGLE,DistributionKind.THREE,DistributionKind.TRANSFORMER_SINGLE,DistributionKind.TRANSFORMER_THREE,DistributionKind.FEEDER_THREE,DistributionKind.CUTOUT_SINGLE,DistributionKind.CUTOUT_THREE,
        DistributionKind.RECLOSER_THREE,DistributionKind.SECONDARY,DistributionKind.CAPACITOR_THREE,DistributionKind.REGULATOR_SINGLE,DistributionKind.REGULATOR_THREE,
        DistributionKind.AIR_BREAK_THREE,DistributionKind.FUSED_TAP_THREE,DistributionKind.SECONDARY_LIGHT,DistributionKind.STREET_SINGLE,DistributionKind.STREET_THREE,DistributionKind.SECONDARY_FED_SINGLE,DistributionKind.SECONDARY_FED_THREE,DistributionKind.FEEDER_SINGLE,DistributionKind.SERVICE_PEDESTAL,DistributionKind.UNDERGROUND_SINGLE,DistributionKind.UNDERGROUND_SECONDARY_TRANSFORMER_SINGLE,DistributionKind.FUSED_TAP_LIGHT_THREE,DistributionKind.UNDERGROUND_THREE,DistributionKind.FUSED_TAP_TRANSFORMER_LIGHT_THREE,DistributionKind.UNDERGROUND_LIGHT_SINGLE,DistributionKind.UNDERGROUND_LIGHT_THREE,DistributionKind.FEEDER_LIGHT_SINGLE)
    val offsets=intArrayOf(34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,53,54,57,86,87,96,97,98,99,100,13,195,196,197)
    fun id(kind: DistributionKind)=7936+offsets[kind.ordinal]
    @JvmStatic fun register() { supported.forEach { if(it.light || it.retiredGroundCabinet)Eln.transparentNodeItem.addWithoutRegistry(id(it),DistributionPoleDescriptor(it)) else Eln.transparentNodeItem.addDescriptor(id(it),DistributionPoleDescriptor(it)) } }
    @JvmStatic fun recipes(shapeless: (mods.eln.datagen.ElnRecipesBase.RStack,Array<out Any>)->Unit) {
        fun find(n: String)=mods.eln.datagen.ElnRecipesBase.findItemStack(n,1)
        for(kind in supported) {
            if(kind.retiredGroundCabinet || kind.light)continue // Legacy fixtures have no new construction recipes.
            if(kind.buriedSecondary) { shapeless(find(kind.title),arrayOf(find(DistributionKind.TRANSFORMER_SINGLE.title),find("Underground Service Entrance Riser"),find("Iron Plate")));continue }
            if(kind.underground) {
                val base=if(kind.pedestal)"120/240V Service Weatherhead" else DistributionKind.CUTOUT_SINGLE.title
                // The destination callback is shapeless; preserve the source shaped bill of materials.
                shapeless(find(kind.title),arrayOf(find(base),find("Pole Fuse Link"),find("Copper Cable"),find("Copper Cable"),*Array(5) { find("Iron Plate") }));continue
            }
            if(kind.singlePrimaryRiser) { shapeless(find(kind.title),arrayOf(find(DistributionKind.CUTOUT_SINGLE.title),find("Copper Cable"),find("Iron Plate")));continue }
            val base=if(kind.lvOnly)"Utility Pole" else if(kind.phases==3 && kind!=DistributionKind.THREE)"Three Phase Utility Pole" else "Utility Pole"
            val detail: Any=when { kind.transformer->find("Pole Fuse Link");kind.cutout || kind.singlePhaseTap->find("Pole Fuse Link");kind.recloser->Items.COMPARATOR;kind.regulator->find("DC-DC Converter");kind.capacitor->find("Copper Cable");kind==DistributionKind.AIR_BREAK_THREE->Items.LEVER;kind==DistributionKind.FEEDER_THREE->Items.IRON_INGOT;kind.light->find("Simple Lamp Socket");else->if(kind.phases==1)Items.IRON_NUGGET else Items.COPPER_INGOT }
            val marker=if(kind in listOf(DistributionKind.STREET_SINGLE,DistributionKind.STREET_THREE,DistributionKind.SECONDARY_LIGHT))Items.GLOWSTONE_DUST else if(kind==DistributionKind.TRANSFORMER_THREE || kind==DistributionKind.CUTOUT_THREE || kind==DistributionKind.REGULATOR_THREE)Items.REDSTONE else Items.IRON_NUGGET
            val extra=if(kind.cutout)kind.phases-1 else if(kind.regulator)kind.phases else 0
            shapeless(find(kind.title),arrayOf(find(base),detail,marker,*Array(extra) { find("Pole Fuse Link") }))
        }
    }
}
