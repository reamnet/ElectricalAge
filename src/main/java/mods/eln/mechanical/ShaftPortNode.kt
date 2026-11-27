package mods.eln.mechanical

import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.node.GhostNode
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.ThermalLoad
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack

/** A raised coupling owned and recreated by a multiblock shaft machine. */
internal class ShaftPortNode(val owner: ShaftElement) : GhostNode() {
    override fun initializeFromThat(front: Direction?, entityLiving: LivingEntity?, itemStack: ItemStack?) {}
    override fun initializeFromNBT() {}
    override fun getSideConnectionMask(side: Direction?, lrdu: LRDU?) = 0
    override fun getThermalLoad(side: Direction?, lrdu: LRDU?): ThermalLoad? = null
    override fun getElectricalLoad(side: Direction?, lrdu: LRDU?): ElectricalLoad? = null
}
