package mods.eln.transparentnode.wiremachine

import mods.eln.client.gui.ElnScreen
import mods.eln.gui.ElnNodeMenu
import mods.eln.i18n.I18N.tr
import mods.eln.misc.Utils
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory

class WireMachineScreen(menu:ElnNodeMenu,inventory:Inventory,title:Component,render:Any):ElnScreen(menu,inventory,title,368,294) {
    val device=render as WireMachineRender
    private var draft=device.targetLengthMeters.toString()
    override fun init() {
        super.init()
        fun change(delta:Int) { if(device.options.isNotEmpty())device.select(Math.floorMod(device.selectedOption+delta,device.options.size)) }
        button(8,60,24,{"<"},{change(-1)});button(336,60,24,{">"},{change(1)})
        if(device.machine.kind==WireMachineKind.ROLLER) {
            field(8,90,100,{draft},{draft=it},tr("Target length in meters"))
            button(116,90,80,{tr("Apply")},{draft.toIntOrNull()?.takeIf { it in 1..WireProduction.MAX_LENGTH }?.let(device::length)})
        }
    }
    override fun extractDevice(g:GuiGraphicsExtractor,mx:Int,my:Int,dt:Float) {
        label(g,8,22,tr("Input: %1$ V / %2$ W",Utils.plotValue(device.volts),Utils.plotValue(device.watts)))
        label(g,8,36,tr("Progress: %1$ / %2$ m",Utils.plotValue(device.progressMeters),Utils.plotValue(device.progressTargetMeters)))
        label(g,40,66,device.options.getOrNull(device.selectedOption)?.name ?: tr("No valid wire options"))
        label(g,8,116,tr("Buffers: %1$ kg %2$; %3$ insulation m",Utils.plotValue(device.metalKg),device.material?.label ?: "",Utils.plotValue(device.rubberMetres)))
        if(device.machine.kind!=WireMachineKind.ROLLER)label(g,8,94,if(device.machine.kind==WireMachineKind.COMBINER)tr("Uses the shortest participating wire length") else tr("Insulates the complete input length"))
    }
}
