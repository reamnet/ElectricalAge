package mods.eln.utility
import mods.eln.client.gui.ElnScreen
import mods.eln.gui.ElnNodeMenu
import mods.eln.i18n.I18N.tr
import mods.eln.misc.Utils
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory

class DistributionPoleScreen(m: ElnNodeMenu,i: Inventory,t: Component,r: Any): ElnScreen(m,i,t,368,330) {
    private val render=r as DistributionPoleRender
    override fun init() {
        super.init();val k=render.desc.kind
        val terminals=buildList { if(!k.lvOnly) { repeat(k.phases) { add(32+it) };if(k.phases==3)add(52);if(k.protectedLine) { repeat(k.outputPhases) { add(36+it) };if(k.phases==3 && !k.singlePhaseTap)add(53) } };add(50);addAll(40..42) }
        button(8,30,352,{tr("Cable terminal: %1$",terminalLabel(render.display.terminal))},{render.clientSendInt(87.toByte(),terminals[(terminals.indexOf(render.display.terminal)+1)%terminals.size])})
        if(k.singlePhaseTap)button(8,56,352,{tr("Tap phase: %1$",listOf("A","B","C")[render.display.phase.coerceIn(0,2)])},{render.clientSendInt(88.toByte(),(render.display.phase+1)%3)})
        if(k.cutout || k.regulator || k.singlePhaseTap)repeat(k.fuseCount) { n->button(8+n*116,82,112,{tr("Fuse %1$: %2$",n+1,if(render.display.fuseBlown.getOrNull(n)==true)tr("BLOWN") else if(render.display.fuseClosed.getOrNull(n)==true)tr("CLOSED") else tr("OPEN"))},{render.clientSendInt(89.toByte(),n)}) }
        if(k.recloser) {
            button(8,56,170,{if(render.display.closed)tr("Open recloser") else tr("Close recloser")},{render.clientSendId(95.toByte())})
            button(186,56,174,{tr("Reset lockout")},{render.clientSendId(94.toByte())})
            button(8,82,112,{tr("Pickup: %1$ A",render.display.pickup.toInt())},{sendDouble(91,listOf(10.0,20.0,30.0,50.0,100.0,200.0,500.0,1000.0,2000.0).let { it[(it.indexOf(render.display.pickup)+1)%it.size] })})
            // Fixed presets use the native double packet helper below.
            button(124,82,112,{tr("Delay: %1$ s",render.display.delay.toInt())},{sendDouble(92,if(render.display.delay>=10)1.0 else render.display.delay+1)})
            button(240,82,120,{tr("Retries: %1$",render.display.maxFailures)},{render.clientSendInt(93.toByte(),render.display.maxFailures%5+1)})
        }
        if(k==DistributionKind.AIR_BREAK_THREE)button(8,56,352,{if(render.display.gang)tr("Open all phases") else tr("Close all phases")},{render.clientSendId(95.toByte())})
        if(k.regulator) {
            button(8,108,170,{if(render.display.automatic)tr("Automatic taps: ON") else tr("Automatic taps: OFF")},{render.clientSendBoolean(97.toByte(),!render.display.automatic)})
            button(186,108,174,{tr("Target: %1$ V",render.display.target.toInt())},{sendDouble(96,if(render.display.target>=15000)120.0 else if(render.display.target<1000)12470.0 else render.display.target+100)})
        }
    }
    private fun sendDouble(id: Int,value: Double) { val bytes=java.io.ByteArrayOutputStream();val out=java.io.DataOutputStream(bytes);render.preparePacketForServer(out);out.writeByte(id);out.writeDouble(value);render.sendPacketToServer(bytes) }
    private fun terminalLabel(t: Int)=when(t) { 50->tr("120/240 V service bundle");52->tr("Three-phase line");53->tr("Three-phase load");40->tr("Service L1");41->tr("Service L2");42->tr("Service 240 V");else->tr("Phase %1$ %2$",listOf("A","B","C")[((t-32)%4).coerceIn(0,2)],if(t>=36)tr("load") else tr("line")) }
    override fun extractDevice(g: GuiGraphicsExtractor,mx: Int,my: Int,dt: Float) {
        val d=render.display
        for(n in d.line.indices)label(g,8,146+n*17,tr("Phase %1$: %2$ / %3$",n+1,Utils.plotVolt("",d.line[n]),Utils.plotVolt("",d.output.getOrElse(n){0.0})))
        for(n in d.service.indices)label(g,8,204+n*17,tr("Service %1$: %2$",n+1,Utils.plotVolt("",d.service[n])))
        if(render.desc.kind.recloser)label(g,8,264,if(d.locked)tr("Recloser locked out - replace the fault, then reset") else tr("Retry failures: %1$; wait %2$ s",d.failures,d.remaining.toInt()))
        if(render.desc.kind.regulator)label(g,8,264,tr("Tap ratios: %1$",d.ratios.joinToString { String.format(java.util.Locale.ROOT,"%.3f",it) }))
    }
}
