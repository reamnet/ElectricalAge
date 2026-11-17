package mods.eln.utility

import mods.eln.client.gui.ElnScreen
import mods.eln.gui.ElnNodeMenu
import mods.eln.i18n.I18N.tr
import mods.eln.misc.Utils
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory

class BreakerPanelScreen(menu:ElnNodeMenu,inventory:Inventory,title:Component,render:Any):ElnScreen(menu,inventory,Component.empty(),312,386) {
    private val device=render as BreakerPanelRender
    private val selects=ArrayList<net.minecraft.client.gui.components.Button>()
    private val switches=ArrayList<net.minecraft.client.gui.components.Button>()
    private var powerText=""
    private var controlText=""
    private var displayed=-1
    override fun init() {
        super.init()
        repeat(12) { i->
            selects.add(button(30+(i%2)*152,32+(i/2)*20,65,{tr("%1$: %2$ A",i+1,device.branches[i].rating.toInt())},{device.select(i)}))
            switches.add(button(97+(i%2)*152,32+(i/2)*20,51,{val b=device.branches[i];if(b.tripped)tr("Reset") else if(b.enabled)tr("On") else tr("Off")},{device.toggle(i)}))
        }
        powerText=device.branches[device.selected()].power;controlText=device.branches[device.selected()].control
        field(8,178,212,{powerText},{powerText=it},tr("Power channel"))
        button(226,174,78,{tr("Power link")},{device.power(device.selected(),powerText)})
        field(8,202,212,{controlText},{controlText=it},tr("Switch channel"))
        button(226,198,78,{tr("Switch link")},{device.control(device.selected(),controlText)})
        button(8,222,138,{tr("Circuit %1$: %2$ V",device.selected()+1,if(device.branches[device.selected()].use240)240 else 120)},{device.bus(device.selected())})
        button(152,222,152,{when(device.branches[device.selected()].aggregator){0->tr("Switch: Any on");1->tr("Switch: All on");else->tr("Switch: Toggle")}},{device.aggregator(device.selected())})
        button(8,246,138,{tr("Wireless range: %1$",device.range)},{device.range(nextRange(device.range))})
        button(152,246,152,{if(device.bottom())tr("240 V feeder: Bottom") else tr("240 V feeder: Top")},{device.feeder(!device.bottom())})
        button(8,270,296,{if(device.branches[device.selected()].dusk)tr("Lighting control: Dusk / night") else tr("Lighting control: Manual / switch")},{device.dusk(device.selected())})
        update()
    }
    override fun update() {
        selects.forEachIndexed { i,b->b.active=i!=device.selected() }
        switches.forEachIndexed { i,b->b.active=device.branches[i].rating>0 }
        if(displayed!=device.selected()) { displayed=device.selected();powerText=device.branches[displayed].power;controlText=device.branches[displayed].control }
    }
    override fun extractLabels(g:GuiGraphicsExtractor,mx:Int,my:Int) {
        super.extractLabels(g,mx,my)
        label(g,8,8,tr("Twelve Circuit American Breaker Panel"))
        label(g,8,22,tr("Left bank: L1"));label(g,160,22,tr("Right bank: L2"))
    }
    override fun extractDevice(g:GuiGraphicsExtractor,mx:Int,my:Int,seconds:Float) {
        val i=device.selected().coerceIn(0,11);val b=device.branches[i]
        label(g,8,158,tr("Circuit %1$: %2$",i+1,Utils.plotUIP(b.volts,b.amps).trim()))
    }
    private fun nextRange(value:Int)=listOf(32,64,128,256,512).firstOrNull { it>value } ?: 32
}
