package mods.eln.utility.insulator

import mods.eln.client.gui.ElnScreen
import mods.eln.gui.ElnNodeMenu
import mods.eln.i18n.I18N.tr
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.client.gui.GuiGraphicsExtractor

class LineInsulatorScreen(menu: ElnNodeMenu,inventory: Inventory,title: Component,render: Any): ElnScreen(menu,inventory,title,244,208) {
    private val device=render as LineInsulatorRender
    private var circuit: net.minecraft.client.gui.components.Button?=null
    override fun init() {
        super.init()
        circuit=button(8,34,228,{circuitLabel(device.terminal)},{
            val choices=listOf(32,40,41,42)
            device.clientSendInt(80.toByte(),choices[(choices.indexOf(device.terminal)+1)%choices.size])
        })
        update()
    }
    override fun update() { circuit?.active=!device.connected }
    override fun extractLabels(g: GuiGraphicsExtractor,mx: Int,my: Int) {
        g.text(font,title,8,8,LABEL,false)
        g.text(font,mods.eln.misc.Utils.plotUIP(device.volts,device.amps),8,66,LABEL,false)
        g.text(font,tr("Disconnect spans before changing circuit"),8,86,LABEL,false)
    }
}
