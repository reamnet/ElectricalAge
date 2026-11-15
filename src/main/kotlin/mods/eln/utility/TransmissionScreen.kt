package mods.eln.utility

import mods.eln.client.gui.ElnScreen
import mods.eln.gui.ElnNodeMenu
import mods.eln.i18n.I18N.tr
import mods.eln.misc.Utils
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory

/** Source 240 x 230 pole layout, with the independent service measurements made explicit. */
class TransmissionScreen(menu: ElnNodeMenu,inventory: Inventory,title: Component,render: Any) : ElnScreen(menu,inventory,title,240,230) {
    private val device = render as TransmissionRender
    private var lampButton: net.minecraft.client.gui.components.Button? = null
    override fun init() {
        super.init()
        button(8,27,224,{tr("Cable terminal: %1$",TransmissionKind.terminalLabel(device.selected))},{
            val ports = device.desc.kind.terminals
            device.clientSendInt(82.toByte(),ports[(ports.indexOf(device.selected)+1)%ports.size])
        })
        lampButton=button(8,50,224,{if(device.lamp.installed)tr("Streetlight controls") else tr("No streetlight installed")},{
            onClose() // Return any carried inventory stack and close the server menu first.
            net.minecraft.client.Minecraft.getInstance().gui.setScreen(TransmissionLampScreen(device))
        })
        update()
    }
    override fun update() { lampButton?.active=device.lamp.installed }
    override fun extractLabels(g: GuiGraphicsExtractor,mx: Int,my: Int) {
        g.text(font,font.plainSubstrByWidth(title.string,224),8,8,LABEL,false)
    }
    override fun extractDevice(g: GuiGraphicsExtractor,mx: Int,my: Int,seconds: Float) {
        repeat(3) { i ->
            label(g,8,72+i*12,tr("Phase %1$: %2$",listOf("A","B","C")[i],Utils.plotVolt("",device.inputs[i]).trim()))
            label(g,8,108+i*12,tr("%1$: %2$",TransmissionKind.terminalLabel(40+i),Utils.plotVolt("",device.services[i]).trim()))
        }
    }
}
