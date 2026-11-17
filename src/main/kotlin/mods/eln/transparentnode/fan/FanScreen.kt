package mods.eln.transparentnode.fan

import mods.eln.client.gui.ElnScreen
import mods.eln.gui.ElnNodeMenu
import mods.eln.i18n.I18N.tr
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory

class FanScreen(menu:ElnNodeMenu,inventory:Inventory,title:Component,render:Any):ElnScreen(menu,inventory,title,176,230) {
    private val fan=render as FanRender
    private var requestedSpeed=(fan.setting*100).toInt().toString()
    override fun init() {
        super.init()
        button(126,24,42,{if(fan.enabled)tr("On") else tr("Off")},{fan.clientSendId(80.toByte())})
        field(8,92,60,{requestedSpeed},{requestedSpeed=it})
        button(74,88,92,{tr("Set speed %")},{requestedSpeed.toDoubleOrNull()?.takeIf{it.isFinite()&&it in 25.0..100.0}?.let{
            val bytes=java.io.ByteArrayOutputStream();java.io.DataOutputStream(bytes).use{out->fan.preparePacketForServer(out);out.writeByte(81);out.writeDouble(it/100)};fan.sendPacketToServer(bytes)
        }})
    }
    override fun extractDevice(g:GuiGraphicsExtractor,mx:Int,my:Int,seconds:Float) {
        label(g,8,28,tr("Speed: %1$%",(fan.speed*100).toInt()))
        label(g,8,44,tr("%1$ V / %2$ W",fan.volts.toInt(),fan.watts.toInt()))
        label(g,8,60,font.plainSubstrByWidth(mods.eln.transparentnode.cord.cordState(fan.cordDisplay.state),160))
        label(g,8,116,if(fan.type.industrial)tr("Keep clear of rotating blades") else tr("Motor speed: 25 to 100%"))
    }
}
