package mods.eln.transparentnode.industrial
import mods.eln.client.gui.ElnScreen
import mods.eln.gui.ElnNodeMenu
import mods.eln.i18n.I18N.tr
import mods.eln.misc.Utils
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory

class PadTransformerScreen(menu: ElnNodeMenu,inventory: Inventory,title: Component,render: Any): ElnScreen(menu,inventory,title,368,326) {
    private val render=render as PadTransformerRender
    private var draftPrimary="12470.0";private var draftSecondary="240.0"
    private var resetButton: net.minecraft.client.gui.components.Button?=null
    override fun init() {
        super.init()
        draftPrimary=render.display.primary.toString();draftSecondary=render.display.secondary.toString()
        val primary=field(8,50,160,{draftPrimary},{draftPrimary=it},tr("Primary voltage: 12 to 230000 V"))
        val secondary=field(192,50,160,{draftSecondary},{draftSecondary=it},tr("Secondary voltage: 120 to 480 V; both service legs follow this winding"))
        button(8,80,112,{tr("Apply settings")},{
            val a=primary.value.toDoubleOrNull() ?: return@button
            val b=secondary.value.toDoubleOrNull() ?: return@button
            render.setSettings(a,b)
        })
        button(128,80,112,{if(render.display.enabled)tr("Isolate / open door") else tr("Enable transformer")},{render.clientSendBoolean(81.toByte(),!render.display.enabled)})
        resetButton=button(248,80,112,{tr("Reset protection")},{render.clientSendId(82.toByte())})
    }
    override fun extractDevice(g: GuiGraphicsExtractor,mx: Int,my: Int,delta: Float) {
        val d=render.display
        resetButton?.active=d.trip
        label(g,8,22,padStatusText(d.status));label(g,8,38,tr("Primary V"));label(g,192,38,tr("Secondary V"))
        label(g,8,112,tr("Primary: %1$; %2$",Utils.plotVolt("",d.volts).trim(),Utils.plotAmpere("",d.primaryAmps).trim()))
        label(g,8,128,tr("Secondary: %1$; %2$",Utils.plotVolt("",d.output).trim(),Utils.plotAmpere("",d.secondaryAmps).trim()))
        label(g,8,144,tr("Fuse: %1$ A primary; transformer: %2$ kW",d.fuseAmps.toInt(),d.ratedWatts/1000))
        label(g,8,160,tr("A core overload trips separately; a healthy primary fuse is retained."))
        label(g,40,184,tr("Installed transformer"));label(g,40,208,tr("Primary fuse cartridge"))
        label(g,8,232,tr("Bottom: Down primary; Up secondary; Left L1; Right L2"))
    }
}
