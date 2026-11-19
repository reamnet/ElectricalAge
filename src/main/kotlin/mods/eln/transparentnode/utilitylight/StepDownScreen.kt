package mods.eln.transparentnode.utilitylight
import mods.eln.client.gui.ElnScreen
import mods.eln.gui.ElnNodeMenu
import mods.eln.i18n.I18N.tr
import mods.eln.misc.Utils
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory

class StepDownScreen(m: ElnNodeMenu,i: Inventory,t: Component,r: Any):ElnScreen(m,i,t,368,224) {
    private val render=r as StepDownUnitRender
    override fun init() {
        super.init()
        button(8,42,112,{tr("Output: %1$ V",render.display.voltage)},{render.clientSendInt(80.toByte(),if(render.display.voltage==120)240 else 120)})
        button(128,42,112,{tr("Rating: %1$ kW",render.display.ratedWatts/1000)},{render.clientSendInt(86.toByte(),MainsStepDown.RATINGS.let { it[(it.indexOf(render.display.ratedWatts)+1)%it.size] })})
        button(248,42,112,{if(render.display.enabled)tr("Disable") else tr("Enable")},{render.clientSendBoolean(81.toByte(),!render.display.enabled)})
        button(248,78,112,{tr("Retry converter")},{render.clientSendId(87.toByte())})
    }
    override fun extractDevice(g: GuiGraphicsExtractor,mx: Int,my: Int,dt: Float) {
        val d=render.display
        label(g,8,22,stepDownStatus(d.status));label(g,8,86,tr("Input: %1$",Utils.plotVolt("",d.input)))
        label(g,8,112,tr("Output: %1$; %2$",Utils.plotVolt("",d.output),Utils.plotPower("",d.watts)))
        label(g,8,138,tr("Temperature: %1$",Utils.plotCelsius("",d.temperature)))
        label(g,8,172,tr("Left: input; right: output. Step-down only."))
        label(g,8,194,tr("Requires a live input at or above the selected voltage."))
    }
}
