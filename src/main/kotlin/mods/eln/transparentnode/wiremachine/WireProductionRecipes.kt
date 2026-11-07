package mods.eln.transparentnode.wiremachine

import mods.eln.Eln
import mods.eln.misc.OreDictTags
import mods.eln.sixnode.electricalcable.*
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.ItemStack
import kotlin.math.ceil

/** Source 32 m display examples derived from the same catalog and predicates as real production. */
object WireProductionRecipes {
    const val EXAMPLE_METRES=32.0
    data class Step(val kind:WireMachineKind,val inputs:List<ItemStack>,val output:ItemStack,
                    val catalysts:List<ItemStack> = emptyList(),val metalKg:Double=0.0) {
        val machine:ItemStack get()=Eln.findItemStack(kind.title,1)
        val energyJoules:Double get()=EXAMPLE_METRES/kind.metresPerSecond*kind.watts*
            if(kind==WireMachineKind.INSULATOR)WireProduction.cable(output)?.let(WireProduction::insulationCostFactor) ?: 1.0 else 1.0
    }
    fun spool(d:UtilityCableDescriptor)=d.newItemStack().also { d.setRemainingLengthMeters(it,EXAMPLE_METRES) }
    fun ingot(material:UtilityCableMaterial):ItemStack {
        val tag=OreDictTags.tag(if(material==UtilityCableMaterial.COPPER)"ingotCopper" else "ingotAluminum")
        return BuiltInRegistries.ITEM.getTagOrEmpty(tag).firstOrNull()?.value()?.let { ItemStack(it) } ?: ItemStack.EMPTY
    }
    @JvmStatic fun steps():List<Step> = buildList {
        for(d in UtilityCables.all.filterNot { it.melted }) {
            if(!d.insulated && d.conductorCount==1) {
                val kg=WirePhysics.massKg(d.material,d.totalConductorAreaMm2,EXAMPLE_METRES)
                val metal=ingot(d.material);if(metal.isEmpty)continue
                var remaining=ceil(kg).toInt()
                val batches=buildList { while(remaining>0) { val count=minOf(remaining,metal.maxStackSize);add(metal.copyWithCount(count));remaining-=count } }
                add(Step(WireMachineKind.ROLLER,batches,spool(d),List(2) { Eln.findItemStack("Iron Roller Wheel",1) },kg))
            }else if(d.insulated && d.conductorCount==1) {
                val bare=WireProduction.singleFor(d,false) ?: continue
                add(Step(WireMachineKind.INSULATOR,listOf(spool(bare),Eln.findItemStack("Rubber",ceil(WireProduction.insulationCostFactor(d)).toInt())),spool(d)))
            }else if(d.insulated) {
                val single=WireProduction.singleFor(d,true) ?: continue
                val bundle=WireProduction.bundle.createBundleStack(d.sizeLabel,d.conductorAreaMm2.toString(),d.material,d.conductorCount,d.conductorAreaMm2,EXAMPLE_METRES)
                add(Step(WireMachineKind.COMBINER,List(d.conductorCount) { spool(single) },bundle))
                add(Step(WireMachineKind.INSULATOR,listOf(bundle.copy(),Eln.findItemStack("Rubber",ceil(WireProduction.insulationCostFactor(d)).toInt())),spool(d)))
            }
        }
    }
}
