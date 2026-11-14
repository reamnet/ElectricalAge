package mods.eln.transparentnode.fabricator

import mods.eln.client.gui.ElnScreen
import mods.eln.gui.ElnNodeMenu
import mods.eln.i18n.I18N.tr
import mods.eln.misc.Utils
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory

class FabricatorScreen(menu: ElnNodeMenu, inventory: Inventory, title: Component, render: Any) : ElnScreen(menu, inventory, title, 368, 260) {
    private val device = render as FabricatorRender
    override fun init() {
        super.init()
        fun change(delta: Int) { device.select(FabricatorOperation.entries[Math.floorMod((device.operation?.ordinal ?: 0) + delta, FabricatorOperation.entries.size)]) }
        button(8, 54, 24, { "<" }, { change(-1) })
        button(336, 54, 24, { ">" }, { change(1) })
    }
    override fun extractDevice(graphics: GuiGraphicsExtractor, mx: Int, my: Int, seconds: Float) {
        label(graphics, 8, 22, tr("Input: %1$ V / %2$ W", Utils.plotValue(device.volts), Utils.plotValue(device.watts)))
        label(graphics, 8, 36, tr("Work: %1$ / %2$ J", Utils.plotValue(device.progressJoules), FabricationRules.ATTEMPT_JOULES))
        val selected = device.operation
        label(graphics, 40, 60, selected?.let { FabricationRecipes.output(it).hoverName.string } ?: tr("None"))
        if (selected != null) label(graphics, 8, 82, tr("Yield: %1$ percent; batch: %2$", selected.chancePercent, selected.outputCount))
        label(graphics, 8, 146, tr("Attempts: %1$; successful: %2$", device.attempts, device.successful))
    }
}
