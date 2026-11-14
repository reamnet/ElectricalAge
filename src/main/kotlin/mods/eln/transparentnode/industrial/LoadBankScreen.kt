package mods.eln.transparentnode.industrial

import mods.eln.client.gui.ElnScreen
import mods.eln.gui.ElnNodeMenu
import mods.eln.i18n.I18N.tr
import mods.eln.misc.Utils
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory

class LoadBankScreen(m: ElnNodeMenu,i: Inventory,t: Component,r: Any):ElnScreen(m,i,t,368,224) {
    private val render=r as LoadBankRender
    private var voltage="";private var power=""
    override fun init() {
        super.init();voltage=render.display.nominal.toString();power=render.display.demand.toString()
        val v=field(122,42,52,{voltage},{voltage=it},tr("Nominal input V"))
        val p=field(306,42,52,{power},{power=it},tr("Load watts"))
        button(8,78,112,{tr("Apply settings")},{
            val volts=v.value.toDoubleOrNull() ?: return@button;val watts=p.value.toDoubleOrNull() ?: return@button
            render.settings(doubleArrayOf(volts,watts))
        })
        button(128,78,112,{if(render.display.enabled)tr("Disable") else tr("Enable")},{render.clientSendBoolean(81.toByte(),!render.display.enabled)})
    }
    override fun extractDevice(g: GuiGraphicsExtractor,mx: Int,my: Int,dt: Float) {
        val d=render.display
        label(g,8,22,if(d.working)tr("Enabled") else if(d.temperature>=120)tr("Cooling: load disconnected") else tr("Disabled or inhibited"))
        label(g,8,46,tr("Nominal input V"));label(g,192,46,tr("Load watts"))
        label(g,8,112,tr("Input: %1$; %2$",Utils.plotVolt("",d.volts),Utils.plotPower("",d.watts)))
        label(g,8,138,tr("Case: %1$ C; consumed: %2$ kJ",d.temperature.toInt(),(d.joules/1000).toLong()))
        label(g,8,172,tr("Left or bottom: supply; front bottom: inhibit signal."))
        label(g,8,194,tr("Resistive power follows voltage squared; no constant-power draw."))
    }
}
