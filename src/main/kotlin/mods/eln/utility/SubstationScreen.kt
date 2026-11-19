package mods.eln.utility

import mods.eln.client.gui.ElnScreen
import mods.eln.gui.ElnNodeMenu
import mods.eln.i18n.I18N.tr
import mods.eln.misc.Utils
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory

class SubstationScreen(menu:ElnNodeMenu,inventory:Inventory,title:Component,render:Any):ElnScreen(menu,inventory,title,240,266){
    private val r=render as SubstationRender
    override fun init(){
        super.init()
        button(8,27,224,{tr("Cable terminal: %1$",r.selected)},{val ports=r.desc.kind.terminals;r.clientSendInt(82.toByte(),ports[(ports.indexOf(r.selected)+1)%ports.size])})
        button(8,53,224,{if(r.tripped)tr("Protection tripped: reset primary")else if(r.primaryClosed)tr("Open primary")else tr("Close primary")},{r.clientSendId(80.toByte())})
        if(r.desc.kind.transformer)button(8,79,224,{if(r.secondaryClosed)tr("Open secondary")else tr("Close secondary")},{r.clientSendId(81.toByte())})
    }
    override fun extractDevice(g:GuiGraphicsExtractor,mx:Int,my:Int,seconds:Float){
        repeat(3){i->label(g,8,106+i*12,tr("Input %1$: %2$",listOf("A","B","C")[i],Utils.plotVolt("",r.inputs[i]).trim()))}
        repeat(3){i->label(g,8,142+i*12,tr("Output %1$: %2$",listOf("A","B","C")[i],Utils.plotVolt("",r.outputs[i]).trim()))}
    }
}
