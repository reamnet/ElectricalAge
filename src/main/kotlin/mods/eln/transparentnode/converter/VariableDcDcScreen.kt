package mods.eln.transparentnode.converter

import mods.eln.client.gui.ElnScreen
import mods.eln.gui.ElnNodeMenu
import mods.eln.i18n.I18N.tr
import mods.eln.misc.Utils
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory

class VariableDcDcScreen(m:ElnNodeMenu,i:Inventory,t:Component,r:Any):ElnScreen(m,i,t,368,294) {
    private val render=r as VariableDcDcRender
    private var draft=render.settings.value.toString()
    override fun init() {
        super.init()
        if(render.descriptor.variable) {
        button(8,76,112,{tr("Mode: %1$",render.settings.mode)},{
            val modes=if(render.settings.version==1)listOf("SIGNAL","RATIO") else listOf("SIGNAL","RATIO","VOLTAGE")
            render.clientSendInt(40.toByte(),(modes.indexOf(render.settings.mode)+1)%modes.size)
        })
        }
        if(render.descriptor.variable || render.descriptor.oneWay) {
        button(128,76,112,{if(render.settings.enabled)tr("Enabled") else tr("Disabled")},{render.clientSendBoolean(42.toByte(),!render.settings.enabled)})
        button(248,76,112,{tr("Use V2 (disabled)")},{if(render.settings.version==1)render.clientSendId(43.toByte())})
        }
        if(render.descriptor.variable) {
        field(8,106,170,{draft},{draft=it},tr("Ratio or target voltage"))
        button(186,106,174,{tr("Apply value")},{draft.toDoubleOrNull()?.let { render.setValue(it) }})
        }
    }
    override fun slotFrame(index:Int)=when(index) { 0->0xFF09B700.toInt();1->0xFFEDEA00.toInt();else->0 }
    override fun extractDevice(g:GuiGraphicsExtractor,mx:Int,my:Int,dt:Float) {
        label(g,8,22,tr("%1$ / %2$",render.construction,render.status))
        label(g,8,36,tr("Primary: %1$; secondary: %2$",Utils.plotUIP(render.primaryVolts,render.primaryAmps),Utils.plotUIP(render.secondaryVolts,render.secondaryAmps)))
        label(g,8,50,tr("Windings: %1$ C / %2$ C; ratio %3$",render.primaryCelsius.toInt(),render.secondaryCelsius.toInt(),Utils.plotValue(render.ratio)))
        label(g,8,140,if(render.descriptor.variable)tr("V1: matched windings. V2: independent wire lengths.") else tr("Ratio follows installed winding lengths."))
        label(g,8,180,tr("Primary / secondary / core / casing"))
    }
}
