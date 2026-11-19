package mods.eln.transparentnode.industrial
import mods.eln.client.gui.ElnScreen
import mods.eln.gui.ElnNodeMenu
import mods.eln.i18n.I18N.tr
import mods.eln.misc.Utils
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory

class SurgeArresterScreen(m:ElnNodeMenu,i:Inventory,t:Component,r:Any):ElnScreen(m,i,t,368,224) {
    private val render=r as SurgeArresterRender;private var voltage=""
    override fun init() {
        super.init();voltage=render.display.nominal.toString()
        val v=field(144,42,72,{voltage},{voltage=it},tr("Nominal line V"))
        button(8,78,112,{tr("Apply settings")},{render.nominal(v.value.toDoubleOrNull() ?: return@button)})
        button(128,78,112,{if(render.display.enabled)tr("Disable") else tr("Enable")},{render.clientSendBoolean(81.toByte(),!render.display.enabled)})
    }
    override fun extractDevice(g:GuiGraphicsExtractor,mx:Int,my:Int,dt:Float) {
        val d=render.display
        label(g,8,22,if(d.remaining<=0)tr("Spent cartridge: replace arrester") else if(d.enabled)tr("Enabled") else tr("Disabled"))
        label(g,8,46,tr("Nominal line V"));label(g,8,112,tr("Input: %1$; %2$",Utils.plotVolt("",d.volts),Utils.plotPower("",d.watts)))
        label(g,8,140,tr("Surge capacity remaining: %1$ kJ",(d.remaining/1000).toInt()))
        label(g,8,172,tr("Consumed capacity survives removal and reload."))
        label(g,8,196,tr("Enabling cannot replenish a spent cartridge."))
    }
}
