package mods.eln.transparentnode.industrial

import mods.eln.client.gui.ElnScreen
import mods.eln.gui.ElnNodeMenu
import mods.eln.i18n.I18N.tr
import mods.eln.misc.Utils
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.client.gui.components.EditBox

class ScadaScreen(m:ElnNodeMenu,i:Inventory,t:Component,r:Any):ElnScreen(m,i,t,368,292) {
    private val render=r as ScadaRender
    private val fields=ArrayList<EditBox>();private val drafts=ArrayList<String>()
    private var displayed=-1
    private var open:net.minecraft.client.gui.components.Button?=null
    private var close:net.minecraft.client.gui.components.Button?=null
    override fun init() {
        super.init();fields.clear();drafts.clear();displayed=render.display.selected
        render.display.settings.forEachIndexed { i,v->drafts.add(v.toString());fields.add(field(122+(i%2)*184,42+(i/2)*24,52,{drafts[i]},{drafts[i]=it},IndustrialKind.SCADA.labels()[i])) }
        button(8,98,104,{tr("Target %1$",render.display.selected+1)},{render.clientSendInt(84.toByte(),(render.display.selected+1)%8)})
        button(120,98,112,{tr("Bind address")},{render.settings(fields.map { it.value.toDoubleOrNull() ?: return@button }.toDoubleArray(),true)})
        open=button(240,98,56,{tr("Open")},{render.clientSendBoolean(85.toByte(),false)})
        close=button(304,98,56,{tr("Close")},{render.clientSendBoolean(85.toByte(),true)})
        button(8,134,112,{tr("Apply settings")},{render.settings(fields.map { it.value.toDoubleOrNull() ?: return@button }.toDoubleArray())})
        button(128,134,112,{if(render.display.enabled)tr("Disable") else tr("Enable")},{render.clientSendBoolean(81.toByte(),!render.display.enabled)})
    }
    override fun extractDevice(g:GuiGraphicsExtractor,mx:Int,my:Int,dt:Float) {
        val d=render.display
        if(displayed!=d.selected) { displayed=d.selected;fields.forEachIndexed { i,f->f.value=d.settings[i].toString();drafts[i]=f.value } }
        open?.active=d.canCommand && d.bound;close?.active=d.canCommand && d.bound
        label(g,8,22,if(d.canCommand)tr("Remote control ready") else tr("Remote control unavailable"))
        IndustrialKind.SCADA.labels().forEachIndexed { i,s->label(g,8+(i%2)*184,46+(i/2)*24,font.plainSubstrByWidth(s,108)) }
        label(g,8,170,tr("Supply: %1$; %2$; target: %3$",Utils.plotVolt("",d.volts),Utils.plotPower("",d.watts),if(d.bound)tr("Bound") else tr("Unbound")))
        d.telemetry.lines().take(4).forEachIndexed { i,line->label(g,8,192+i*16,font.plainSubstrByWidth(line,352)) }
        label(g,8,274,tr("Loaded targets within 256 blocks; closing never resets a trip."))
    }
}
