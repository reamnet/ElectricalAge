package mods.eln.transparentnode.cord

import mods.eln.client.gui.ElnScreen
import mods.eln.gui.ElnNodeMenu
import mods.eln.i18n.I18N.tr
import mods.eln.misc.Utils
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory

class OutletScreen(menu: ElnNodeMenu,inv: Inventory,title: Component,render: Any): ElnScreen(menu,inv,title,368,294) {
    private val device=render as OutletRender
    override fun init() {
        super.init()
        button(8,56,170,{ if(device.horizontal)tr("Mount: Horizontal") else tr("Mount: Vertical") },{ device.action(84) })
        button(188,56,172,{ tr("Unplug all cords") },{ device.action(81) })
        button(8,82,170,{ if(device.wireless)tr("Wireless lamp supply") else tr("Cable power") },{ device.action(82) })
        field(188,86,172,{device.channel},{device.channel(it)},tr("Wireless lamp supply channel"))
        repeat(device.desc.sockets) { i->button(8+(i%3)*120,112+(i/3)*26,112,{tr("Unplug %1$",i+1)},{device.unplug(i)}) }
    }
    override fun extractDevice(g: GuiGraphicsExtractor,mx: Int,my: Int,dt: Float) {
        label(g,8,22,tr("%1$ V / %2$ A",Utils.plotValue(device.volts),Utils.plotValue(device.amps)))
        label(g,8,36,tr("%1$ / %2$ sockets occupied",Integer.bitCount(device.sockets),device.desc.sockets))
        label(g,8,174,if(device.wireless && !device.supplied)tr("No active supply in range") else tr("Protection belongs to the upstream branch"))
    }
}
