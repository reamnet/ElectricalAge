package mods.eln.transparentnode.industrial

import mods.eln.client.gui.ElnScreen
import mods.eln.gui.ElnNodeMenu
import mods.eln.i18n.I18N.tr
import mods.eln.misc.Utils
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.client.gui.components.EditBox

class MotorDriveScreen(m:ElnNodeMenu,i:Inventory,t:Component,r:Any):ElnScreen(m,i,t,368,292) {
    private val render=r as MotorDriveRender
    private val fields=ArrayList<EditBox>();private val drafts=ArrayList<String>()
    override fun init() {
        super.init();fields.clear();drafts.clear()
        render.display.settings.forEachIndexed { i,v->
            drafts.add(v.toString());fields.add(field(122+(i%2)*184,42+(i/2)*24,52,{drafts[i]},{drafts[i]=it},IndustrialKind.DRIVE.labels()[i]))
        }
        button(8,118,112,{tr("Apply settings")},{render.settings(fields.map { it.value.toDoubleOrNull() ?: return@button }.toDoubleArray())})
        button(128,118,112,{if(render.display.enabled)tr("Stop / ramp down") else tr("Enable")},{render.clientSendBoolean(81.toByte(),!render.display.enabled)})
        button(248,118,112,{tr("Reset trip")},{render.clientSendId(82.toByte())})
    }
    override fun extractDevice(g:GuiGraphicsExtractor,mx:Int,my:Int,dt:Float) {
        val d=render.display
        label(g,8,22,if(d.tripped)tr("Thermal trip: cool below 80 C before reset") else if(d.enabled)tr("Enabled") else tr("Ramping down"))
        IndustrialKind.DRIVE.labels().forEachIndexed { i,s->label(g,8+(i%2)*184,46+(i/2)*24,font.plainSubstrByWidth(s,108)) }
        label(g,8,154,tr("Input: %1$; output: %2$",Utils.plotVolt("",d.input),Utils.plotVolt("",d.output)))
        label(g,8,180,tr("Output current: %1$; loss: %2$",Utils.plotAmpere("",d.amps),Utils.plotPower("",d.lossWatts)))
        label(g,8,206,tr("Actual frequency: %1$ Hz; case %2$ C",d.hertz.toInt(),d.temperature.toInt()))
        label(g,8,242,tr("Current rating applies at nominal input voltage"))
        label(g,8,266,tr("Disable and inhibit ramp down; thermal trip opens immediately."))
    }
}
