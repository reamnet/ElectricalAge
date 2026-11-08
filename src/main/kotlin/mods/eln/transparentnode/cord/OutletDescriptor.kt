package mods.eln.transparentnode.cord

import mods.eln.Eln
import mods.eln.gl.GL11
import mods.eln.i18n.I18N.tr
import mods.eln.node.transparent.*
import mods.eln.misc.*
import net.minecraft.world.phys.AABB
import net.minecraft.world.item.ItemStack
import net.minecraft.world.entity.player.Player

class OutletDescriptor(val rating: CordRating,val strip: Boolean=false): PowerCabinetDescriptor(if(strip)mods.eln.i18n.I18N.TR_NAME(mods.eln.i18n.I18N.Type.NONE,"Six Socket Power Strip") else rating.outletName,OutletElement::class.java,OutletRender::class.java,true) {
    override fun newBlock(p: net.minecraft.world.level.block.state.BlockBehaviour.Properties)=OutletBlock(p,this)
    val sockets get()=if(strip)6 else 2
    private val model=requireNotNull(Eln.obj.getObj(when { strip->"sixsocketpowerstrip";rating==CordRating.V240_30->"dryeroutlet30";rating.industrial->"industrialmachineoutlet";rating==CordRating.V120->"appliancewalloutlet";else->"walloutlet240" }))
    private val body=requireNotNull(model.getPart("Case"));private val black=requireNotNull(model.getPart("Black"));private val trim=requireNotNull(model.getPart("Trim"));private val top=model.getPart("TopSocket")
    override fun wallBox(front: Direction)=OutletBlock.box(front,false,strip)
    override fun hasItemDraw()=true
    override fun drawItem(entity: Boolean)=draw(Direction.ZP,false,0)
    fun draw(front: Direction,horizontal: Boolean,occupied: Int) {
        GL11.glPushMatrix();front.inverse.glRotateZnRef();if(horizontal)GL11.glRotatef(90f,0f,0f,1f)
        GL11.glPushAttrib(0xFFFF)
        try {
            GL11.glDisable(GL11.GL_TEXTURE_2D);val c=if(rating.industrial)rating.color else floatArrayOf(.86f,.85f,.79f)
            GL11.glColor3f(c[0],c[1],c[2]);body.drawNoBind();GL11.glColor3f(.09f,.095f,.10f);black.drawNoBind();top?.drawNoBind();GL11.glColor3f(.5f,.51f,.53f);trim.drawNoBind()
            repeat(sockets) { i->if(occupied and (1 shl i)!=0) { val v=CordGeometry.offset(i,strip);GL11.glPushMatrix();GL11.glTranslated(v.x,v.y,v.z);CordGeometry.plug(rating);GL11.glPopMatrix() } }
        }finally { GL11.glPopAttrib();GL11.glPopMatrix() }
    }
    override fun addInformation(stack: ItemStack?,player: Player?,lines: MutableList<String>,advanced: Boolean) {
        lines.add(tr("%1$: %2$ V, %3$ A nominal; %4$ sockets",rating.socket,rating.volts.toInt(),rating.amps,sockets))
        lines.add(tr("Passive outlet; use an upstream circuit breaker"));lines.add(tr("Select this outlet with a matching cord, then connect a machine"))
    }
}
