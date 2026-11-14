package mods.eln.transparentnode.switchgear

import mods.eln.client.gui.ElnScreen
import mods.eln.gui.ElnNodeMenu
import mods.eln.i18n.I18N.tr
import mods.eln.misc.Utils
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory

class SwitchgearScreen(m: ElnNodeMenu,i: Inventory,t: Component,r: Any):ElnScreen(m,i,t,368,278) {
    private val render=r as SwitchgearRender
    private var reset: net.minecraft.client.gui.components.Button?=null
    override fun init() {
        super.init()
        button(8,42,352,{tr("Control: %1$",render.display.control.label())},{render.clientSendInt(80.toByte(),(render.display.control.ordinal+1)%RelayControl.entries.size)})
        button(8,68,170,{if(render.display.enabled)tr("Isolate contacts") else tr("Restore contacts")},{render.clientSendBoolean(81.toByte(),!render.display.enabled)})
        reset=button(186,68,174,{tr("Reset trip")},{render.clientSendId(82.toByte())})
    }
    override fun extractDevice(g: GuiGraphicsExtractor,mx: Int,my: Int,dt: Float) {
        val d=render.display;reset?.active=d.fault && d.temperature<80
        label(g,8,22,switchgearStatus(d.enabled,d.on,d.moving,d.fault))
        label(g,8,102,tr("Coil: %1$ / %2$",Utils.plotVolt("",d.coilVolts),Utils.plotPower("",d.coilWatts)))
        label(g,8,120,tr("Loss: %1$; case: %2$ C",Utils.plotPower("",d.losses),String.format(java.util.Locale.ROOT,"%.1f",d.temperature)))
        d.volts.indices.forEach { n->label(g,8,146+n*18,tr("%1$: %2$; NO %3$; NC %4$",n+1,Utils.plotVolt("",d.volts[n]),Utils.plotAmpere("",d.noAmps[n]),Utils.plotAmpere("",d.ncAmps[n]))) }
        label(g,8,236,tr("Front: A1 left / A2 right / signal top"))
        label(g,8,256,if(render.kind.changeover)tr("Left common; right NO; rear NC") else tr("Left line; right load (normally open)"))
    }
}
