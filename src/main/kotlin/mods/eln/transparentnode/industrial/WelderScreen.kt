package mods.eln.transparentnode.industrial
import mods.eln.client.gui.ElnScreen
import mods.eln.gui.ElnNodeMenu
import mods.eln.i18n.I18N.tr
import mods.eln.misc.Utils
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
class WelderScreen(m: ElnNodeMenu,i: Inventory,t: Component,r: Any):ElnScreen(m,i,t,368,350) {
    private val render=r as WelderRender
    private var voltage="";private var power=""
    override fun init() {
        super.init();voltage=render.display.nominal.toString();power=render.display.demand.toString()
        val v=field(122,42,52,{voltage},{voltage=it},tr("Nominal input V"))
        val p=field(306,42,52,{power},{power=it},tr("Welding power W"))
        button(8,78,112,{tr("Apply settings")},{render.settings(doubleArrayOf(v.value.toDoubleOrNull() ?: return@button,p.value.toDoubleOrNull() ?: return@button))})
        button(128,78,112,{if(render.display.enabled)tr("Disable") else tr("Enable")},{render.clientSendBoolean(81.toByte(),!render.display.enabled)})
    }
    override fun extractDevice(g: GuiGraphicsExtractor,mx: Int,my: Int,dt: Float) {
        val d=render.display
        label(g,8,22,if(d.working)tr("Welding") else tr("Disabled, inhibited or waiting for materials/output"))
        label(g,8,46,tr("Nominal input V"));label(g,192,46,tr("Welding power W"))
        label(g,8,112,tr("Input: %1$; %2$",Utils.plotVolt("",d.volts),Utils.plotPower("",d.watts)))
        label(g,8,138,tr("Work: %1$ kJ / 120 kJ; input: %2$ kJ",(d.work/1000).toInt(),(d.lifetime/1000).toLong()))
        label(g,8,190,tr("Workpiece + iron ingot -> output"))
        label(g,8,222,tr("Two iron ingots weld one machine frame."))
        label(g,8,242,tr("Repair restores 25% of maximum durability."))
    }
}
