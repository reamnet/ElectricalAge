package mods.eln.transparentnode.kitchen

import mods.eln.client.gui.ElnScreen
import mods.eln.gui.ElnNodeMenu
import mods.eln.i18n.I18N.tr
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory

class KitchenScreen(menu:ElnNodeMenu,inventory:Inventory,title:Component,render:Any):ElnScreen(menu,inventory,title,176,menu.spec.height()) {
    private val r=render as KitchenRender
    override fun init(){super.init();button(126,21,42,{if(r.display.enabled)tr("On")else tr("Off")},{r.clientSendId(80.toByte())})}
    override fun extractDevice(g:GuiGraphicsExtractor,mx:Int,my:Int,seconds:Float){
        val s=r.display
        label(g,8,26,tr("%1$ V / %2$ W",r.type.volts.toInt(),r.type.watts.toInt()))
        label(g,8,42,font.plainSubstrByWidth(kitchenStatus(s.status),160))
        label(g,8,54,tr("Work: %1$% / %2$ W",(s.progress*100).toInt(),s.watts.toInt()))
        label(g,16,68,tr("Input"));label(g,132,68,tr("Output"))
        if(r.type.refrigerator)label(g,8,99,tr("Temperature: %1$ C",String.format(java.util.Locale.ROOT,"%.1f",s.temperature)))
        else {
            if(r.cordDisplay.endpoint!=null)label(g,8,99,font.plainSubstrByWidth(mods.eln.transparentnode.cord.cordState(r.cordDisplay.state),160))
            label(g,8,110,font.plainSubstrByWidth(when(r.type){
            KitchenType.KETTLE->tr("Water bottle makes hot water")
            KitchenType.BLENDER->tr("Fruit + water makes a smoothie")
            KitchenType.RICE->tr("Wheat or rice + water bottle")
            KitchenType.COFFEE->tr("Cocoa beans + water bottle")
            KitchenType.TOASTER->tr("Bread makes toasted bread")
            else->tr("Food recipes; no fuel needed")
        },160))
        }
    }
}
