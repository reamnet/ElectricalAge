package mods.eln.utility

import mods.eln.i18n.I18N.tr
import mods.eln.misc.Utils
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

/** Original shared attachment screen coordinates and colors, using native element packets. */
class TransmissionLampScreen(private val device: TransmissionRender) : Screen(Component.literal(tr("Pole streetlight attachment"))) {
    private val labels=ArrayList<Pair<Button,()->String>>()
    override fun isPauseScreen()=false
    override fun init() {
        labels.clear()
        val left=(width-308)/2; val top=(height-210)/2
        fun button(x: Int,y: Int,w: Int,text: ()->String,action: ()->Unit) {
            val b=addRenderableWidget(Button.builder(Component.literal(text())) { action() }.bounds(left+x,top+y,w,20).build())
            labels.add(b to text)
        }
        val d=device.lamp
        button(8,36,142,{if(d.enabled)tr("Light: ON") else tr("Light: OFF")},{device.clientSendBoolean(102.toByte(),!d.enabled)})
        button(158,36,142,{if(d.automatic)tr("Dusk / night") else tr("Always enabled")},{device.clientSendBoolean(103.toByte(),!d.automatic)})
        button(8,62,292,{if(d.supply==0)tr("Supply: automatic 120/240 V") else tr("Supply: %1$ V",d.supply)},{device.clientSendInt(107.toByte(),when(d.supply) { 0->120;120->240;else->0 })})
        button(8,90,142,{tr("Rotate left")},{device.clientSendInt(105.toByte(),-15)})
        button(158,90,142,{tr("Rotate right")},{device.clientSendInt(105.toByte(),15)})
        button(8,116,142,{tr("Lower")},{device.clientSendInt(104.toByte(),-1)})
        button(158,116,142,{tr("Raise")},{device.clientSendInt(104.toByte(),1)})
        button(8,142,142,{tr("Aim down")},{device.clientSendInt(106.toByte(),-5)})
        button(158,142,142,{tr("Aim up")},{device.clientSendInt(106.toByte(),5)})
        button(8,174,142,{tr("Remove attachment")},{device.clientSendId(108.toByte());onClose()})
        button(158,174,142,{tr("Done")},{onClose()})
    }
    override fun tick() {
        val p=device.tileEntity.blockPos
        val player=minecraft?.player
        if (player==null || device.tileEntity.isRemoved || player.distanceToSqr(p.x+.5,p.y+.5,p.z+.5)>64 || !device.lamp.installed) { onClose();return }
        for ((button,label) in labels) button.message=Component.literal(label())
    }
    override fun extractRenderState(g: GuiGraphicsExtractor,mx: Int,my: Int,partial: Float) {
        val left=(width-308)/2; val top=(height-210)/2
        g.fill(left,top,left+308,top+210,0xee23272b.toInt())
        g.text(font,tr("Pole streetlight attachment"),left+8,top+8,-1,false)
        g.text(font,tr("Secondary: %1$; %2$",Utils.plotVolt("",device.lamp.volts).trim(),Utils.plotPower("",device.lamp.watts).trim()),left+8,top+22,0xffcccccc.toInt(),false)
        super.extractRenderState(g,mx,my,partial)
    }
}
