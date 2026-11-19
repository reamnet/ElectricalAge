package mods.eln.transparentnode.firealarm

import mods.eln.client.gui.ElnScreen
import mods.eln.gui.ElnNodeMenu
import mods.eln.i18n.I18N.tr
import mods.eln.misc.Utils
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import java.util.Locale

class AlarmScreen(menu:ElnNodeMenu,inventory:Inventory,title:Component,render:Any):ElnScreen(menu,inventory,Component.empty(),300,382) {
    private val r=render as AlarmRender
    private var channel=r.channel;private var zone=r.label;private var threshold=r.threshold.toInt().toString()
    override fun init() {
        super.init()
        field(8,43,180,{channel},{channel=it},tr("Channel"))
        field(8,82,180,{zone},{zone=it},tr("Zone / device label"))
        button(198,39,94,{tr("Save names")},{r.text(80,channel);r.text(81,zone)})
        button(198,78,94,{if(r.enabled)tr("Enabled")else tr("Disabled")},{r.enable(!r.enabled)})
        if(r.kind==AlarmKind.PANEL) {
            button(8,112,138,{tr("Silence / resound")},{r.action(84)})
            button(154,112,138,{tr("Reset alarm")},{r.action(85)})
            button(8,138,138,{tr("Test for 10 s")},{r.action(86)})
            button(154,138,138,{tr("Forget missing")},{r.action(87)})
        }else if(r.kind==AlarmKind.PULL)button(8,112,284,{tr("Pull / reset station")},{r.action(83)})
        if(r.kind.sensor) {
            button(8,112,284,{tr("Detection radius: %1$ m",r.range)},{r.radius(r.range%8+1)})
            if(r.kind==AlarmKind.HEAT) {
                field(8,144,100,{threshold},{threshold=it},tr("Heat threshold"))
                button(118,140,174,{tr("Set heat threshold")},{threshold.toDoubleOrNull()?.let(r::heat)})
            }
        }
    }
    override fun extractLabels(g:GuiGraphicsExtractor,mx:Int,my:Int) {
        super.extractLabels(g,mx,my)
        label(g,8,8,font.plainSubstrByWidth(tr(r.kind.title),284))
        label(g,8,27,tr("Channel (blank for wired-only)"));label(g,8,66,tr("Zone / device label"))
    }
    override fun extractDevice(g:GuiGraphicsExtractor,mx:Int,my:Int,seconds:Float) {
        label(g,8,176,alarmStatus(r.powered,r.alarm,r.pulled||r.detected,r.trouble))
        label(g,8,192,tr("Supply: %1$; draw: %2$",Utils.plotVolt("",r.volts).trim(),Utils.plotPower("",r.watts).trim()))
        if(r.kind==AlarmKind.PANEL) {
            label(g,8,210,tr("Backup: %1$ Wh; linked devices: %2$",String.format(Locale.ROOT,"%.1f",r.energy/3600),r.members))
            label(g,8,228,font.plainSubstrByWidth(tr("Active: %1$",r.sources.ifBlank{tr("None")}),284))
            label(g,8,246,tr("Clear active detectors and pulled stations before reset"))
        }else if(r.kind==AlarmKind.HEAT)label(g,8,210,tr("Heat: %1$ C; threshold: %2$ C",r.temperature.toInt(),r.threshold.toInt()))
        else label(g,8,210,tr("Channel power from panel, or wired 24 V"))
    }
}
