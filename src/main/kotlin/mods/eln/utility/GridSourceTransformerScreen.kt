package mods.eln.utility

import mods.eln.client.gui.ElnScreen
import mods.eln.gui.ElnNodeMenu
import mods.eln.i18n.I18N.tr
import mods.eln.misc.Utils
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory

/** Native access to the added latched protection; a reset never bypasses a continuing fault. */
class GridSourceTransformerScreen(menu:ElnNodeMenu,inventory:Inventory,title:Component,render:Any):ElnScreen(menu,inventory,title,240,166) {
    private val r=render as GridSourceTransformerRender
    override fun init(){super.init();button(8,56,224,{if(r.tripped)tr("Reset cooled protection")else tr("Protection ready")},{r.clientSendId(80.toByte())})}
    override fun extractDevice(g:GuiGraphicsExtractor,mx:Int,my:Int,seconds:Float){
        label(g,8,24,tr("Primary: %1$",Utils.plotVolt("",r.primary).trim()))
        label(g,8,40,tr("Secondary: %1$",Utils.plotVolt("",r.secondary).trim()))
    }
}
