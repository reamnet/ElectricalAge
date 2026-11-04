package mods.eln.transparentnode.industrial

import mods.eln.client.gui.ElnScreen
import mods.eln.gui.ElnNodeMenu
import mods.eln.i18n.I18N.tr
import mods.eln.misc.Utils
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.client.gui.components.EditBox
import java.util.Locale

/** Explicit priorities, real branch readings and transactional settings on the native menu. */
class IndustrialScreen(menu: ElnNodeMenu,inventory: Inventory,title: Component,render: Any): ElnScreen(menu,inventory,title,368,292) {
    private val render=render as IndustrialRender
    private val fields=ArrayList<EditBox>()
    private val drafts=ArrayList<String>()
    override fun init() {
        super.init();fields.clear();drafts.clear()
        render.display.settings.forEachIndexed { i,v->
            val index=i
            drafts.add(v.toString())
            fields.add(field(122+(i%2)*184,42+(i/2)*24,52,{drafts[index]},{drafts[index]=it},render.kind.labels()[index]))
        }
        button(8,118,112,{tr("Apply settings")},{
            val p=fields.map { it.value.toDoubleOrNull() ?: return@button }.toDoubleArray();render.setSettings(p)
        })
        button(128,118,112,{if(render.display.enabled)tr("Disable / open") else tr("Enable / close")},{render.clientSendBoolean(81.toByte(),!render.display.enabled)})
        button(248,118,112,{if(render.kind==IndustrialKind.SHEDDER)tr("Retry shed loads") else tr("Reset trip")},{render.clientSendId((if(render.kind==IndustrialKind.SHEDDER)86 else 82).toByte())})
    }
    override fun extractDevice(g: GuiGraphicsExtractor,mx: Int,my: Int,delta: Float) {
        val d=render.display
        label(g,8,22,if(d.trip)tr("Tripped; inspect circuit before reset") else if(!d.enabled)tr("Disabled") else if(render.kind==IndustrialKind.SHEDDER)shedStatusText(d.shedStatus) else if(d.pendingReason!=0)tr("Fault detected; trip delay running") else tr("Protection ready"))
        render.kind.labels().forEachIndexed { i,s->label(g,8+(i%2)*184,46+(i/2)*24,font.plainSubstrByWidth(s,108)) }
        label(g,8,146,tr("Input: %1$; %2$",Utils.plotVolt("",d.volts).trim(),Utils.plotPower("",d.watts).trim()))
        if(render.kind==IndustrialKind.SHEDDER) {
            for(i in 0..3)label(g,8,164+i*18,tr("P%1$ (%2$): %3$; %4$",i+1,when(i) { 0->tr("left");1->tr("right");2->tr("down");else->tr("up") },if(d.closed[i])tr("On") else tr("Shed"),Utils.plotPower("",if(d.closed[i])d.branchWatts[i] else d.nominalDemand[i]).trim()))
            if(d.next>=0)label(g,8,242,tr("Next: P%1$; delay remaining: %2$ s",d.next+1,String.format(Locale.ROOT,"%.1f",d.remaining)))
            label(g,8,260,tr("P1 highest; P4 sheds first. Shed readings are last nominal demand."))
            label(g,8,278,tr("Shed below %1$ V; restore above %2$ V",(d.settings[0]*d.settings[4]).toInt(),(d.settings[0]*d.settings[5]).toInt()))
        } else {
            label(g,8,164,tr("Output: %1$; %2$",Utils.plotVolt("",d.output).trim(),Utils.plotAmpere("",d.amps).trim()))
            label(g,8,184,relayReasonText(d.reason))
            label(g,8,204,tr("Residual current: %1$; trip timer: %2$ s",Utils.plotAmpere("",d.differential).trim(),String.format(Locale.ROOT,"%.1f",d.tripElapsed)))
            label(g,8,224,tr("UV %1$ V / OV %2$ V / OC %3$ A",(d.settings[0]*d.settings[2]).toInt(),(d.settings[0]*d.settings[3]).toInt(),d.settings[1]))
            label(g,8,250,tr("Forward: left/right bottom. Return: left/right right."))
            label(g,8,268,tr("Use both conductors for differential protection."))
        }
    }
}
