package mods.eln.transparentnode.festive

import mods.eln.i18n.I18N.tr
import mods.eln.client.gui.ElnScreen
import mods.eln.gui.ElnNodeMenu
import net.minecraft.world.entity.player.Inventory
import net.minecraft.network.chat.Component
import net.minecraft.client.gui.GuiGraphicsExtractor

/** Source coordinates and176x206 canvas, including the source channel and voltage controls. */
class StringLightsScreen(m: ElnNodeMenu,i: Inventory,t: Component,r: Any):ElnScreen(m,i,Component.empty(),176,206) {
    private val lamp=r as StringLightsRender
    private lateinit var channel: mods.eln.client.gui.EaField
    private var voltage: net.minecraft.client.gui.components.Button?=null
    override fun init() {
        super.init()
        channel=field(8,40,160,12,{lamp.channel},{lamp.clientSendString(81.toByte(),it)},tr("Wireless lamp supply channel")).apply {setMaxLength(128)}
        button(8,62,160,{tr("Set wireless channel")},{lamp.clientSendString(81.toByte(),channel.value)})
        voltage=button(8,86,160,{tr("Nominal: %1$ V",lamp.volts)},{lamp.clientSendInt(80.toByte(),if(lamp.volts==120)240 else 120)})
    }
    override fun extractDevice(g: GuiGraphicsExtractor,mx: Int,my: Int,dt: Float) {
        label(g,8,8,tr("Holiday lighting"));label(g,8,22,if(lamp.plugged)tr("Powered by cord") else tr("Wireless lamp supply"))
        voltage?.active=!lamp.plugged
    }
}
