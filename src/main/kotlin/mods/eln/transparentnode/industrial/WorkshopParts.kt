package mods.eln.transparentnode.industrial

import mods.eln.Eln
import mods.eln.generic.GenericItemUsingDamageDescriptor
import mods.eln.i18n.I18N
import mods.eln.i18n.I18N.tr
import mods.eln.item.electricalitem.BatteryItem
import net.minecraft.world.item.ItemStack

object WorkshopParts {
    val PACK=I18N.TR_NAME(I18N.Type.NONE,"48 V 20 kWh Traction Battery")
    val FRAME=I18N.TR_NAME(I18N.Type.NONE,"Welded Machine Frame")
    fun register() {
        Eln.sharedItemStackOne.addElement(117 shl 6,TractionBattery())
        Eln.sharedItem.addElement(1+(117 shl 6),GenericItemUsingDamageDescriptor(FRAME,"weldedmachineframe"))
    }
}
class TractionBattery:BatteryItem(WorkshopParts.PACK,72000000.0,11520.0,50000.0,3) {
    init { setDefaultIcon("upsindustrialbatterymodule") }
    override fun setEnergy(stack: ItemStack,value: Double) = super.setEnergy(stack,if(value.isFinite())value.coerceIn(0.0,72000000.0) else 0.0)
    override fun addInformation(stack: ItemStack?,player: net.minecraft.world.entity.player.Player?,list: MutableList<Any?>,advanced: Boolean) {
        super.addInformation(stack,player,list,advanced)
        list.add(tr("Removable vehicle pack; charge in the EV Pack Charger; crafted empty"))
    }
}
