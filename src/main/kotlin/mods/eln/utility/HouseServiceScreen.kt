package mods.eln.utility
import mods.eln.client.gui.ElnScreen
import mods.eln.gui.ElnNodeMenu
import mods.eln.i18n.I18N.tr
import mods.eln.misc.Utils
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import java.util.Locale

class HouseServiceScreen(menu: ElnNodeMenu,inventory: Inventory,title: Component,render: Any): ElnScreen(menu,inventory,title,320,284) {
    private val render=render as HouseServiceRender
    private var disconnect: net.minecraft.client.gui.components.Button?=null
    override fun init() {
        super.init()
        if(render.desc.overhead)button(8,56,304,{
            when(render.display.terminal) { 50->tr("Cable: service drop bundle");40->tr("Cable: L1 120 V");41->tr("Cable: L2 120 V");else->tr("Cable: 240 V") }
        },{ val ts=listOf(50,40,41,42);render.clientSendInt(87.toByte(),ts[(ts.indexOf(render.display.terminal)+1)%ts.size]) })
        button(8,82,304,{
            tr("Placed cable circuit: %1$",when(render.display.circuit) { 0->tr("L1 120 V");1->tr("L2 120 V");else->tr("240 V") })
        },{render.clientSendInt(88.toByte(),(render.display.circuit+1)%3)})
        if(render.desc.meter || render.desc.disconnect)disconnect=button(8,108,304,{if(render.display.disconnected)tr("Service: OFF") else tr("Service: ON")},{render.clientSendBoolean(89.toByte(),!render.display.disconnected)})
    }
    override fun extractDevice(g: GuiGraphicsExtractor,mx: Int,my: Int,delta: Float) {
        val d=render.display;disconnect?.active=render.desc.disconnect || d.type==2
        if(render.desc.meter) {
            label(g,32,44,when(d.type) { 1->tr("Analog meter installed");2->tr("Smart meter installed");else->tr("Insert a meter to connect service") })
            label(g,8,66,tr("Meter serial: %1$",d.serial))
            label(g,8,144,tr("Import: %1$ kWh",String.format(Locale.ROOT,"%.3f",d.imported)))
            label(g,8,160,tr("Export: %1$ kWh",String.format(Locale.ROOT,"%.3f",d.exported)))
            label(g,8,176,tr("Power: %1$",Utils.plotPower("",d.watts).trim()))
        } else for(i in 0..2)label(g,8,144+i*16,tr("%1$: %2$",when(i) { 0->tr("L1 120 V");1->tr("L2 120 V");else->tr("240 V") },Utils.plotVolt("",d.volts[i]).trim()))
    }
}
