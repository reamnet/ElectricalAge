package mods.eln.utility

import mods.eln.Eln
import mods.eln.gl.GL11
import mods.eln.gridnode.GridRender
import mods.eln.i18n.I18N
import mods.eln.i18n.I18N.tr
import mods.eln.misc.Direction
import mods.eln.node.transparent.*
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import java.io.DataInputStream
import java.io.DataOutputStream

class BreakerPanelDescriptor : ServiceWallDescriptor(I18N.TR_NAME(I18N.Type.NONE,"Twelve Circuit American Breaker Panel"),"twelvecircuitpanel",BreakerPanelElement::class.java,BreakerPanelRender::class.java) {
    private val model by lazy { requireNotNull(Eln.obj.getObj("twelvecircuitpanel")) }
    fun drawPanel(front: Direction, states: List<PanelBranchDisplay> = emptyList()) {
        GL11.glPushMatrix(); front.inverse.glRotateZnRef(); GL11.glPushAttrib(0xFFFFF)
        try {
            GL11.glDisable(GL11.GL_TEXTURE_2D)
            fun part(name: String, color: FloatArray) { GL11.glColor3f(color[0],color[1],color[2]);model.getPart(name)?.drawNoBind() }
            part("Case",floatArrayOf(.64f,.65f,.63f));part("Door",floatArrayOf(.64f,.65f,.63f));part("Black",floatArrayOf(.11f,.13f,.14f));part("Main",floatArrayOf(.11f,.13f,.14f))
            repeat(3) { part("Terminal$it",floatArrayOf(.72f,.51f,.26f));part("TopTerminal$it",floatArrayOf(.72f,.51f,.26f)) }
            repeat(12) { i->
                val s=states.getOrNull(i)
                if (s==null || s.rating>0) {
                    part("Breaker$i",floatArrayOf(.16f,.17f,.17f))
                    GL11.glPushMatrix();try { if(s?.closed!=true)GL11.glTranslated(if(i%2==0)-.035 else .035,0.0,0.0);val color=when{ s?.tripped==true->floatArrayOf(.9f,.2f,.035f);s?.closed==true->floatArrayOf(.3f,.47f,.34f);else->floatArrayOf(.4f,.42f,.42f) };part("Toggle$i",color) }finally { GL11.glPopMatrix() }
                }
            }
        } finally { GL11.glPopAttrib();GL11.glPopMatrix() }
    }
    override fun hasItemDraw()=true
    override fun drawItem(entity: Boolean)=drawPanel(Direction.ZP)
    override fun addInformation(item: ItemStack?, player: Player?, list: MutableList<String>, advanced: Boolean) {
        list.add(tr("Twelve circuits; install an Electrical Breaker in each occupied branch"))
        list.add(tr("Left and right banks are 120 V; center bus is the 240 V service input"))
        list.add(tr("Top or bottom accepts the service feed; the other vertical port is the selected branch"))
    }
}

class BreakerPanelRender(entity: TransparentNodeEntity, descriptor: TransparentNodeDescriptor) : GridRender(entity,descriptor) {
    private var selected=0; private var terminal=50; private var bottom=false;var range=32;private set
    val branches=List(12) { PanelBranchDisplay() }
    private val connections=mods.eln.misc.LRDUMask()
    private var cable:mods.eln.cable.CableRenderType?=null
    override fun draw() {
        drawCables();(transparentNodedescriptor as BreakerPanelDescriptor).drawPanel(front,branches)
        cable=drawCable(front.inverse,Eln.stdCableRender200V,connections,cable)
    }
    override fun networkUnserialize(stream: DataInputStream) {
        super.networkUnserialize(stream);selected=stream.readInt().coerceIn(0,11);terminal=stream.readInt();range=stream.readInt();bottom=stream.readBoolean()
        repeat(12) { i->val b=branches[i];b.rating=stream.readDouble();b.enabled=stream.readBoolean();b.tripped=stream.readBoolean();b.closed=stream.readBoolean();b.use240=stream.readBoolean();b.dusk=stream.readBoolean();b.power=stream.readUTF();b.control=stream.readUTF();b.aggregator=stream.readInt();b.volts=stream.readDouble();b.amps=stream.readDouble() };connections.deserialize(stream);cable=null
    }
    fun select(i:Int)=clientSendInt(80.toByte(),i)
    fun power(i:Int,v:String)=clientSendString(81.toByte(),"$i\n$v")
    fun control(i:Int,v:String)=clientSendString(82.toByte(),"$i\n$v")
    fun toggle(i:Int)=clientSendInt(83.toByte(),i)
    fun bus(i:Int)=clientSendInt(84.toByte(),i)
    fun aggregator(i:Int)=clientSendInt(85.toByte(),i)
    fun range(value:Int)=clientSendInt(86.toByte(),value)
    fun dusk(i:Int)=clientSendInt(88.toByte(),i)
    fun feeder(value:Boolean)=clientSendBoolean(89.toByte(),value)
    fun selected()=selected
    fun bottom()=bottom
    fun terminal()=terminal
    override fun getCableRender(side: Direction, lrdu: mods.eln.misc.LRDU): mods.eln.cable.CableRenderDescriptor? = if(side in listOf(Direction.YP,Direction.YN,front.left(),front.right())) Eln.stdCableRender200V else null
}

class PanelBranchDisplay { var rating=0.0;var enabled=true;var tripped=false;var closed=false;var use240=false;var dusk=false;var power="";var control="";var aggregator=0;var volts=0.0;var amps=0.0 }
