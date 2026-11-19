package mods.eln.item.wiremachine

import mods.eln.generic.GenericItemUsingDamageDescriptor
import mods.eln.generic.ItemNbt
import mods.eln.i18n.I18N.tr
import mods.eln.sixnode.electricalcable.UtilityCableMaterial
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack

class RollerWheelDescriptor(name:String,icon:String):GenericItemUsingDamageDescriptor(name,icon)

/** A paid bundle is one physical object, with its constituent area and fractional length in the item. */
class WoundWireBundleDescriptor(name:String):GenericItemUsingDamageDescriptor(name,"coppercable") {
    override fun itemProperties(properties:Item.Properties)=properties.stacksTo(1)
    fun createBundleStack(label:String,metric:String,material:UtilityCableMaterial,conductors:Int,area:Double,metres:Double):ItemStack {
        require(conductors in 2..8 && area.isFinite() && area>0 && metres.isFinite() && metres>0)
        return newItemStack().also { ItemNbt.set(it,CompoundTag().apply {
            putString("wireBundleTargetLabel",label);putString("wireBundleTargetMetric",metric);putString("wireBundleMaterial",material.label)
            putInt("wireBundleConductors",conductors);putDouble("wireBundleArea",area);putDouble("wireBundleLengthMeters",metres)
        }) }
    }
    fun getTargetLabel(stack:ItemStack)=ItemNbt.get(stack)?.getStringOr("wireBundleTargetLabel","") ?: ""
    fun getTargetMetricLabel(stack:ItemStack)=ItemNbt.get(stack)?.getStringOr("wireBundleTargetMetric","") ?: ""
    fun getMaterial(stack:ItemStack):UtilityCableMaterial? {
        val label=ItemNbt.get(stack)?.getStringOr("wireBundleMaterial","") ?: return null
        return UtilityCableMaterial.entries.firstOrNull { it.label.equals(label,true) }
    }
    fun getConductorCount(stack:ItemStack)=ItemNbt.get(stack)?.getIntOr("wireBundleConductors",0) ?: 0
    fun getAreaMm2(stack:ItemStack)=ItemNbt.get(stack)?.getDoubleOr("wireBundleArea",0.0) ?: 0.0
    fun getLengthMeters(stack:ItemStack)=ItemNbt.get(stack)?.getDoubleOr("wireBundleLengthMeters",0.0) ?: 0.0
    fun setLengthMeters(stack:ItemStack,metres:Double) { require(metres.isFinite() && metres>=0);ItemNbt.edit(stack) { it.putDouble("wireBundleLengthMeters",metres) } }
    fun valid(stack:ItemStack)=checkSameItemStack(stack) && stack.count==1 && getMaterial(stack)!=null && getConductorCount(stack) in 2..8 && getAreaMm2(stack).let { it.isFinite() && it>0 } && getLengthMeters(stack).let { it.isFinite() && it>0 }
    override fun addInformation(stack:ItemStack?,player:Player?,lines:MutableList<Any?>,advanced:Boolean) {
        if(stack==null || !valid(stack))return
        lines.add(tr("Target: %1$",getTargetLabel(stack)));lines.add(tr("Material: %1$",getMaterial(stack)!!.label));lines.add(tr("Conductors: %1$",getConductorCount(stack)));lines.add(tr("Length: %1$ m",getLengthMeters(stack)))
    }
}
