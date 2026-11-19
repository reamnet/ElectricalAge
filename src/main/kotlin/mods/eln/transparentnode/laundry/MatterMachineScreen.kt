package mods.eln.transparentnode.laundry

import mods.eln.client.gui.ElnScreen
import mods.eln.gui.ElnNodeMenu
import mods.eln.i18n.I18N.tr
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory

class MatterMachineScreen(menu:ElnNodeMenu,inventory:Inventory,title:Component,render:Any):ElnScreen(menu,inventory,title,208,240) {
    private val r=render as MatterMachineRender
    override fun init(){super.init();button(154,23,46,{if(r.enabled)tr("On")else tr("Off")},{r.clientSendId(80.toByte())})}
    override fun extractDevice(g:GuiGraphicsExtractor,mx:Int,my:Int,seconds:Float){
        label(g,8,28,tr("Supply: %1$ V",r.volts.toInt()))
        label(g,8,44,font.plainSubstrByWidth(matterMachineStatus(r.status),192))
        label(g,8,58,tr("Power: %1$ / %2$ W",r.watts.toInt(),r.rated.toInt()))
        label(g,8,72,tr("Progress: %1$%",(100*r.joules/r.required.coerceAtLeast(1.0)).toInt()))
        label(g,25,89,if(r.kind==MatterMachineKind.FUSION)tr("Sample")else tr("Input"));label(g,130,89,tr("Output"))
        when(r.kind){
            MatterMachineKind.WASHER->label(g,8,127,tr("Water: %1$ / 10000 mB",r.water))
            MatterMachineKind.DRYER->label(g,8,127,tr("Wet equipment becomes usable"))
            MatterMachineKind.FUSION->{label(g,8,127,tr("Energy: %1$ / %2$ MJ",(r.joules/1e6).toInt(),(r.required/1e6).toInt()));label(g,8,141,tr("Nominal cycle: %1$ minutes",String.format(java.util.Locale.ROOT,"%.1f",r.required/r.rated/60)))}
        }
    }
}
