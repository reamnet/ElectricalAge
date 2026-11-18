package mods.eln.item

import mods.eln.Eln
import mods.eln.misc.Obj3D
import mods.eln.misc.VoltageLevelColor
import mods.eln.registry.ElnDeviceRegistry
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor
import mods.eln.wiki.Data
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack

class ElectricalFuseDescriptor(
    name: String,
    val cableDescriptor: ElectricalCableDescriptor?,
    obj: Obj3D?,
    val currentLimit: Double? = null
) :
    GenericItemUsingDamageDescriptorUpgrade(name) {

    companion object {
        var BlownFuse: ElectricalFuseDescriptor? = null
    }

    constructor(name: String, cableDescriptor: ElectricalCableDescriptor?, obj: Obj3D?) : this(name, cableDescriptor, obj, null)

    private val fuseType = obj?.getPart("FuseType")
    private val fuseOk = obj?.getPart("FuseOk")
    private val fuse = obj?.getPart("Fuse")

    init {
        if (isUsable()) {
            setDefaultIcon("electricalfuse")
            voltageLevelColor = ElectricalCableDescriptor.voltageLevelColorOf(cableDescriptor)
        } else {
            setDefaultIcon("blownelectricalfuse")
            voltageLevelColor = VoltageLevelColor.Neutral
        }
    }

    // ForkPatch 1.21.1 lead fuses: current-limited fuses (5A/10A) with no cable descriptor.
    fun isUsable(): Boolean = cableDescriptor != null || currentLimit != null

    fun resistanceOhms(): Double = cableDescriptor?.electricalRs?.times(2.0) ?: ElnDeviceRegistry.getSmallRs() * 2.0

    fun warmLimitCelsius(): Double = cableDescriptor?.thermalWarmLimit ?: Eln.cableWarmLimit

    fun thermalRp(): Double {
        val limit = currentLimit ?: return cableDescriptor?.thermalRp ?: Double.POSITIVE_INFINITY
        return warmLimitCelsius() / (limit * limit * resistanceOhms())
    }

    fun thermalC(): Double {
        val limit = currentLimit ?: return cableDescriptor?.thermalC ?: Double.POSITIVE_INFINITY
        return limit * limit * resistanceOhms() * Eln.cableHeatingTime / warmLimitCelsius()
    }

    // 26.x port: shouldUseRenderHelper() removed (client rendering; redone in P3)

    // 26.x port: renderItem() removed (client rendering; redone in P3)


    override fun setParent(item: Item?, damage: Int) {
        super.setParent(item, damage)
        Data.addWiring(newItemStack())
    }
}
