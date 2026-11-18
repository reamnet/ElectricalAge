package mods.eln.mechanical

import mods.eln.ElnConfig
import mods.eln.generic.GenericItemUsingDamageDescriptor
import mods.eln.item.GenericItemUsingDamageDescriptorUpgrade
import mods.eln.generic.ItemNbt
import mods.eln.i18n.I18N
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack

/**
 * Turbine blade (ForkPatch 1.21.1 item/TurbineBladeDescriptor.kt): the consumable a large turbine runs on,
 * whose condition (1.0 new, 0.0 broken) degrades with the fuel's temperature and corrosiveness. The condition
 * NBT lives in the CUSTOM_DATA component via [ItemNbt] (1.12 ItemStack.getTagCompound/setTagCompound).
 * Lifetime/infinite-life come from the per-tier config (ForkPatch items.turbineBlades.<tier>.*).
 * The alloy/tungsten tiers and the turbine consumers themselves are not ported yet.
 */
class TurbineBladeItem(
    name: String,
    val tierName: String,
    val temperatureResistance: Double,
    val corrosionResistance: Double,
    val tierDescription: String
) : GenericItemUsingDamageDescriptorUpgrade(name) {

    init {
        require(tierName in TURBINE_BLADE_TIERS) { "No turbine blade config tier '$tierName'" }
    }

    val nominalLifeInHours: Double get() = ElnConfig.turbineBladeLifeHours(tierName)
    val infiniteLifeEnabled: Boolean get() = ElnConfig.turbineBladeInfiniteLife(tierName)

    /** Condition 1.0 (new) to 0.0 (broken); 1.0 when absent (ForkPatch getCondition). */
    fun getCondition(stack: ItemStack): Double {
        val tag = ItemNbt.get(stack) ?: return 1.0
        return tag.getDoubleOr("condition", 1.0).coerceIn(0.0, 1.0)
    }

    fun setCondition(stack: ItemStack, condition: Double) {
        ItemNbt.edit(stack) { it.putDouble("condition", condition.coerceIn(0.0, 1.0)) }
    }

    override fun getDefaultNBT(): CompoundTag = CompoundTag().apply { putDouble("condition", 1.0) }

    private fun conditionLabel(stack: ItemStack): String {
        if (!ItemNbt.has(stack)) return I18N.tr("New")
        val c = getCondition(stack)
        return when {
            c >= 1.0 -> I18N.tr("New")
            c > 0.5 -> I18N.tr("Good")
            c > 0.15 -> I18N.tr("Used")
            c > 0.01 -> I18N.tr("Bad")
            else -> I18N.tr("End of life")
        }
    }

    override fun addInformation(itemStack: ItemStack?, entityPlayer: Player?, list: MutableList<Any?>, par4: Boolean) {
        super.addInformation(itemStack, entityPlayer, list, par4)
        list.add(I18N.tr("Nominal Lifetime: %1\$h", nominalLifeInHours.toInt()))
        if (itemStack != null) list.add(I18N.tr("Condition: %1\$", conditionLabel(itemStack)))
        if (tierDescription.isNotEmpty()) list.add(I18N.tr(tierDescription))
    }

    companion object {
        private val TURBINE_BLADE_TIERS = setOf("iron", "steel")

        fun getDescriptor(stack: ItemStack?): TurbineBladeItem? =
            stack?.let { GenericItemUsingDamageDescriptor.getDescriptor(it) as? TurbineBladeItem }
    }
}
