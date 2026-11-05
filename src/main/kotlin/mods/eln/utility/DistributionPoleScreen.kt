package mods.eln.utility
import mods.eln.client.gui.ElnScreen
import mods.eln.gui.ElnNodeMenu
import mods.eln.i18n.I18N.tr
import mods.eln.misc.Utils
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory

class DistributionPoleScreen(m: ElnNodeMenu,i: Inventory,t: Component,r: Any): ElnScreen(m,i,t,m.spec.width(),m.spec.height()) {
    private val render=r as DistributionPoleRender
    override fun init() {
        super.init();val k=render.desc.kind
        val terminals=buildList { if(!k.lvOnly) { repeat(k.phases) { add(32+it) };if(k.phases==3)add(52);if(k.protectedLine) { repeat(k.outputPhases) { add(36+it) };if(k.phases==3 && !k.singlePhaseTap)add(53) } };add(50);addAll(40..42) }
        if(!k.underground)button(8,30,352,{tr("Cable terminal: %1$",terminalLabel(render.display.terminal))},{render.clientSendInt(87.toByte(),terminals[(terminals.indexOf(render.display.terminal)+1)%terminals.size])})
        if(k.singlePhaseTap || k.transformer && k.phases==3)button(8,56,352,{tr("Tap phase: %1$",listOf("A","B","C")[render.display.phase.coerceIn(0,2)])},{render.clientSendInt(88.toByte(),(render.display.phase+1)%3)})
        if(k.cutout || k.regulator || k.singlePhaseTap || k.transformer)repeat(k.fuseCount) { n->button(8+n*116,82,112,{tr("Fuse %1$: %2$",n+1,if(render.display.fuseBlown.getOrNull(n)==true)tr("BLOWN") else if(render.display.fuseClosed.getOrNull(n)==true)tr("CLOSED") else tr("OPEN"))},{render.clientSendInt(89.toByte(),n)}) }
        if(k.transformer) {
            button(8,108,112,{tr("Winding: %1$ V",render.display.transformerVoltage)},{render.clientSendInt(80.toByte(),if(render.display.transformerVoltage==120)240 else 120)})
            button(128,108,112,{if(render.display.transformerEnabled)tr("Isolate") else tr("Enable")},{render.clientSendBoolean(81.toByte(),!render.display.transformerEnabled)})
            button(248,108,112,{tr("Reset core trip")},{render.clientSendId(82.toByte())})
        }
        if(!k.underground)run {
            val y=if(k.transformer || k.regulator)132 else 108
            button(8,y,170,{if(!render.display.lampInstalled)tr("Install light attachment") else tr("Lamp: %1$ (%2$)",if(render.display.lampOn)tr("ON") else tr("OFF"),Utils.plotPower("",render.display.lampWatts))},{render.clientSendBoolean(102.toByte(),!render.display.lampOn)})
            button(186,y,174,{if(render.display.lampAuto)tr("Dusk control: ON") else tr("Dusk control: OFF")},{render.clientSendBoolean(103.toByte(),!render.display.lampAuto)})
            button(8,y+26,112,{tr("Height: %1$ m",String.format(java.util.Locale.ROOT,"%.2f",5.2+mods.eln.transparentnode.utilitylight.StreetLightMount.offset(render.display.heightStep)))},{render.clientSendInt(104.toByte(),if(net.minecraft.client.Minecraft.getInstance().hasShiftDown())-1 else 1)},tr("Shift-click to lower; click to raise"))
            button(124,y+26,112,{tr("Yaw: %1$ deg",render.display.yaw)},{render.clientSendInt(105.toByte(),if(net.minecraft.client.Minecraft.getInstance().hasShiftDown())-5 else 5)},tr("Shift-click to turn the other way"))
            button(240,y+26,120,{tr("Pitch: %1$ deg",render.display.pitch)},{render.clientSendInt(106.toByte(),if(net.minecraft.client.Minecraft.getInstance().hasShiftDown())-5 else 5)},tr("Shift-click to tilt down; click to tilt up"))
        }
        if(!k.light && !k.underground) {
            button(if(k.transformer)124 else 8,if(k.transformer)82 else 304,112,{if(render.display.lampSupply==0)tr("Supply: AUTO") else tr("Supply: %1$ V",render.display.lampSupply)},{render.clientSendInt(107.toByte(),when(render.display.lampSupply) { 0->120;120->240;else->0 })})
            button(if(k.transformer)240 else 186,if(k.transformer)82 else 304,120,{if(render.display.lampInstalled)tr("Remove attachment") else tr("No light attachment")},{render.clientSendId(108.toByte())})
        }
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
        val d=render.display;val k=render.desc.kind
        if(k.underground)label(g,8,30,tr("Placed cable terminals; no overhead spans"))
        val lineY=if(k.transformer || k.regulator)184 else 160
        val serviceY=if(k.transformer || k.regulator)238 else 214
        for(n in d.line.indices)label(g,8,lineY+n*17,tr("Phase %1$: %2$ / %3$",n+1,Utils.plotVolt("",d.line[n]),Utils.plotVolt("",d.output.getOrElse(n){0.0})))
        for(n in d.service.indices)label(g,8,serviceY+n*17,tr("Service %1$: %2$",n+1,Utils.plotVolt("",d.service[n])))
        if(render.desc.kind.transformer) {
            label(g,8,292,tr("Transformer: %1$ kW; %2$",d.transformerRating/1000,Utils.plotCelsius("",d.transformerTemperature)))
            label(g,8,309,poleTransformerStatus(d.transformerStatus))

            label(g,40,336,tr("Installed transformer"))
        }
        if(!k.underground && !k.transformer && !k.regulator)label(g,8,274,tr("Light level: %1$/15; %2$",d.light,Utils.plotPower("",d.lampWatts)))
        if(render.desc.kind.recloser)label(g,8,287,if(d.locked)tr("Recloser locked out - replace the fault, then reset") else tr("Retry failures: %1$; wait %2$ s",d.failures,d.remaining.toInt()))
        if(render.desc.kind.regulator)label(g,8,287,tr("Tap ratios: %1$",d.ratios.joinToString { String.format(java.util.Locale.ROOT,"%.3f",it) }))
    }
}
