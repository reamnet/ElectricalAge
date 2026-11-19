package mods.eln.mechanical

import mods.eln.generic.GenericItemUsingDamageDescriptor
import mods.eln.generic.ItemNbt
import mods.eln.i18n.I18N
import mods.eln.misc.IFunction
import mods.eln.misc.LinearFunction
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack

/**
 * Clutch plate item (ForkPatch 1.21.1 mechanical/Clutch.kt ClutchPlateItem): the consumable side of a clutch,
 * with wear tracking and the energy/transfer/slip-wear ratings. On 26.x the wear NBT lives in the CUSTOM_DATA
 * component via [ItemNbt] (1.12 ItemStack.getTagCompound/setTagCompound).
 */
class ClutchPlateItem(
    name: String,
    val maxEF: Float, val minEF: Float,
    val maxDTF: Float, val minDTF: Float,
    val wearSpeed: Float, public val explodes: Boolean
) : GenericItemUsingDamageDescriptor(name) {
    companion object {
        var infiniteLifeEnabled: Boolean = false
    }

    override fun getDefaultNBT() = CompoundTag()

    fun setWear(stack: ItemStack, wear: Double) {
        ItemNbt.edit(stack) { it.putDouble("wear", wear) }
    }

    fun getWear(stack: ItemStack): Double {
        val tag = ItemNbt.get(stack) ?: return 0.0
        return tag.getDoubleOr("wear", 0.0)
    }

    fun maxStaticEnergyF(@Suppress("UNUSED_PARAMETER") stack: ItemStack): IFunction =
        LinearFunction(0f, maxEF, 1f, minEF)
    fun dynamicMaxTransferF(@Suppress("UNUSED_PARAMETER") stack: ItemStack): IFunction =
        LinearFunction(0f, maxDTF, 1f, minDTF)
    fun slipWearF(@Suppress("UNUSED_PARAMETER") stack: ItemStack): IFunction =
        LinearFunction(0f, 0f, 1000f, wearSpeed)

    override fun addInformation(itemStack: ItemStack?, entityPlayer: Player?, list: MutableList<Any?>, par4: Boolean) {
        super.addInformation(itemStack, entityPlayer, list, par4)
        if (itemStack != null) {
            val wear = getWear(itemStack)
            val condition = when {
                wear < 0.2 -> I18N.tr("New")
                wear < 0.5 -> I18N.tr("Good")
                wear < 0.8 -> I18N.tr("Used")
                wear < 0.9 -> I18N.tr("End of life")
                else -> I18N.tr("Bad")
            }
            list.add(I18N.tr("Condition:") + " " + condition)
        }
    }
}
