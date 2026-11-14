package mods.eln.transparentnode.industrial
import mods.eln.client.gui.ElnScreen
import mods.eln.gui.ElnNodeMenu
import mods.eln.i18n.I18N.tr
import mods.eln.misc.Utils
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
class ChargerScreen(m: ElnNodeMenu,i: Inventory,t: Component,r: Any):ElnScreen(m,i,t,368,350) {
    private val render=r as ChargerRender
    private val draft=MutableList(4) { "" }
    override fun init() {
        super.init();val labels=IndustrialKind.CHARGER.labels()
        val fields=(0..3).map { n->draft[n]=render.display.settings[n].toString();field(122+184*(n%2),42+34*(n/2),52,{draft[n]},{draft[n]=it},labels[n]) }
        button(8,112,112,{tr("Apply settings")},{
            val p=fields.map { it.value.toDoubleOrNull() ?: return@button }.toDoubleArray();render.settings(p)
        })
        button(128,112,112,{if(render.display.enabled)tr("Disable") else tr("Enable")},{render.clientSendBoolean(81.toByte(),!render.display.enabled)})
    }
    override fun extractDevice(g: GuiGraphicsExtractor,mx: Int,my: Int,dt: Float) {
        val d=render.display
        label(g,8,22,if(d.working)tr("Charging") else tr("Disabled, full, absent, inhibited or outside schedule"))
        IndustrialKind.CHARGER.labels().forEachIndexed { n,s->label(g,8+184*(n%2),46+34*(n/2),s) }
        label(g,8,144,tr("Input: %1$; %2$",Utils.plotVolt("",d.volts),Utils.plotPower("",d.watts)))
        label(g,56,168,tr("Battery: remove to carry stored energy"))
        label(g,8,194,tr("Session: %1$ kJ; input: %2$ kJ",(d.session/1000).toLong(),(d.lifetime/1000).toLong()))
        label(g,8,222,tr("Hours use world time; equal hours: always on."))
        label(g,8,242,tr("Charging efficiency: 94%."))
    }
}
