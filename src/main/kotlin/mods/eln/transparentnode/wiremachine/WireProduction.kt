package mods.eln.transparentnode.wiremachine

import mods.eln.generic.GenericItemBlockUsingDamageDescriptor
import mods.eln.generic.GenericItemUsingDamageDescriptor
import mods.eln.generic.ItemNbt
import mods.eln.item.wiremachine.*
import mods.eln.misc.OreDictTags
import mods.eln.sixnode.electricalcable.*
import net.minecraft.world.item.ItemStack
import kotlin.math.abs

/** Shared native production options, inventory predicates and physical material accounting. */
object WireProduction {
    const val RUBBER_METERS=32.0
    const val MAX_LENGTH=65536
    val combinerInputs=intArrayOf(0,1,2,3,4,6,7,8)
    lateinit var bundle:WoundWireBundleDescriptor
    fun cable(stack:ItemStack):UtilityCableDescriptor? {
        if(stack.count!=1)return null
        val wire=GenericItemBlockUsingDamageDescriptor.getDescriptor(stack) as? UtilityCableDescriptor ?: return null
        val temperature=temperature(stack)
        val limit=if(wire.insulated)minOf(wire.material.meltingPointCelsius,wire.meltTemperatureCelsius) else wire.material.meltingPointCelsius
        return wire.takeIf { it.getRemainingLengthMeters(stack)>0 && temperature.isFinite() && temperature>=-273.15 && temperature<limit }
    }
    fun temperature(stack:ItemStack)=ItemNbt.get(stack)?.getDoubleOr("windingTemperatureCelsius",20.0) ?: 20.0
    fun material(stack:ItemStack):UtilityCableMaterial?=when {
        stack.isEmpty->null
        stack.`is`(OreDictTags.tag("ingotCopper"))->UtilityCableMaterial.COPPER
        stack.`is`(OreDictTags.tag("ingotAluminum"))->UtilityCableMaterial.ALUMINUM
        else->null
    }
    fun rubber(stack:ItemStack)=!stack.isEmpty && stack.`is`(OreDictTags.tag("itemRubber"))
    fun wheel(stack:ItemStack)=GenericItemUsingDamageDescriptor.getDescriptor(stack) is RollerWheelDescriptor
    fun rollerOptions(material:UtilityCableMaterial?)=UtilityCables.all.filter { !it.insulated && !it.melted && it.conductorCount==1 && it.material==(material ?: UtilityCableMaterial.COPPER) }.sortedBy { it.totalConductorAreaMm2 }
    fun singleFor(target:UtilityCableDescriptor,insulated:Boolean)=UtilityCables.all.firstOrNull { !it.melted && it.insulated==insulated && it.material==target.material && it.conductorCount==1 && abs(it.conductorAreaMm2-target.conductorAreaMm2)<.001 }
    fun insulationCostFactor(d:UtilityCableDescriptor)=when {
        d.insulationVoltageRating<=1000->1.0
        d.insulationVoltageRating<=5000->2.0
        d.insulationVoltageRating<=20000->4.0
        d.insulationVoltageRating<=40000->8.0
        else->16.0
    }
    fun insulatorOptions(input:ItemStack):List<UtilityCableDescriptor> {
        val wire=cable(input)
        val temperature=temperature(input)
        if(wire!=null) {
            if(wire.insulated || wire.melted || wire.conductorCount!=1)return emptyList()
            return UtilityCables.all.filter { it.insulated && !it.melted && temperature<it.meltTemperatureCelsius && it.conductorCount==1 && it.material==wire.material && abs(it.conductorAreaMm2-wire.conductorAreaMm2)<=.001 }.sortedBy { it.insulationVoltageRating }
        }
        if(!bundle.valid(input) || !temperature.isFinite() || temperature< -273.15)return emptyList()
        val material=bundle.getMaterial(input);val label=bundle.getTargetLabel(input)
        val count=bundle.getConductorCount(input);val area=bundle.getAreaMm2(input)
        return UtilityCables.all.filter { it.insulated && !it.melted && temperature<it.meltTemperatureCelsius && it.material==material && it.sizeLabel==label && it.conductorCount==count && abs(it.conductorAreaMm2-area)<=.001 }.sortedBy { it.insulationVoltageRating }
    }
    fun combinerOptions(inputs:List<ItemStack>):List<UtilityCableDescriptor> {
        if(inputs.size !in 2..8)return emptyList()
        val wires=inputs.map { cable(it) ?: return emptyList() };val first=wires.first()
        if(wires.any { !it.insulated || it.melted || it.conductorCount!=1 || it.material!=first.material || abs(it.conductorAreaMm2-first.conductorAreaMm2)>.001 })return emptyList()
        return UtilityCables.all.filter { it.insulated && !it.melted && it.conductorCount==wires.size && it.material==first.material && abs(it.conductorAreaMm2-first.conductorAreaMm2)<.001 }.sortedBy { it.name }
    }
    fun accepts(kind:WireMachineKind,slot:Int,stack:ItemStack):Boolean {
        if(slot==kind.outputSlot)return false
        return when(kind) {
            WireMachineKind.ROLLER->if(slot==0)material(stack)!=null else slot in 1..2 && wheel(stack)
            WireMachineKind.INSULATOR->if(slot==1)rubber(stack) else slot==0 && (insulatorOptions(stack).isNotEmpty())
            WireMachineKind.COMBINER->slot in combinerInputs && cable(stack)?.let { it.insulated && !it.melted && it.conductorCount==1 }==true
        }
    }
}

enum class WireMachineKind(val title:String,val watts:Double,val metresPerSecond:Double,val slotCount:Int,val outputSlot:Int,val model:String) {
    ROLLER("Wire Roller",300.0,6.0,4,3,"platemachinea"),
    INSULATOR("Wire Insulator",160.0,4.0,3,2,"fabricator"),
    COMBINER("Wire Combiner",120.0,5.0,9,5,"magnetizera");
    val id get()=4608+ordinal
}
