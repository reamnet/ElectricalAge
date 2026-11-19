package mods.eln.transparentnode.industrial
import mods.eln.Eln
import mods.eln.gl.GL11
import mods.eln.misc.Direction
import mods.eln.node.transparent.*
import mods.eln.i18n.I18N.tr
import net.minecraft.world.item.ItemStack
import net.minecraft.world.entity.player.Player

class PadTransformerDescriptor: TransparentNodeDescriptor(IndustrialKind.PAD.title,PadTransformerElement::class.java,PadTransformerRender::class.java) {
    private val model=requireNotNull(Eln.obj.getObj("industrial_pad"))
    override fun mustHaveFloor()=false
    override fun hasItemDraw()=true
    override fun drawItem(entity: Boolean)=draw(Direction.ZN)
    fun draw(front: Direction,d: PadTransformerDisplay?=null) {
        GL11.glPushMatrix();front.inverse.glRotateZnRef();GL11.glPushAttrib(0xFFFFF)
        try {
            GL11.glDisable(GL11.GL_TEXTURE_2D)
            fun p(name: String,r: Float,g: Float,b: Float) { GL11.glColor3f(r,g,b);model.getPart(name)?.drawNoBind() }
            p("Base",.18f,.20f,.22f);p("Case",.18f,.37f,.23f);p("Pad",.42f,.44f,.41f);p("Lugs",.65f,.48f,.25f);p("Elbows",.10f,.12f,.13f);p("Ground",.35f,.49f,.32f)
            p("Trim",.12f,.14f,.16f);p("Screen",.025f,.10f,.12f);p("Input",.13f,.38f,.8f);p("Output",.96f,.48f,.07f);p("Terminal",.66f,.46f,.20f)
            GL11.glPushMatrix()
            try {
                if(d?.enabled==false) { GL11.glTranslated(-.40,0.0,.37);GL11.glRotated(-100.0,0.0,1.0,0.0);GL11.glTranslated(.40,0.0,-.37) }
                p("Door",.17f,.35f,.22f);p("DoorHardware",.12f,.14f,.16f);p("Nameplate",.75f,.78f,.71f);p("Warning",.95f,.65f,.08f);p("WarningMark",.05f,.06f,.04f)
            } finally { GL11.glPopMatrix() }
            mods.eln.client.power.PowerPanelDisplay.lamp(model,"Indicator",d?.trip==true || d?.status=="ONLINE",if(d?.trip==true)1f else .06f,if(d?.trip==true).08f else .8f,.06f)
        } finally { GL11.glPopAttrib();GL11.glPopMatrix() }
    }
    override fun addInformation(stack: ItemStack?,player: Player?,list: MutableList<String>,advanced: Boolean) {
        list.add(tr("Single-phase underground transformer; install a transformer and physical primary fuse"))
        list.add(tr("Fixed ratio follows primary sag; 120/240 V service shares one transformer rating"))
        list.add(tr("Fuse amperes are primary current; transformer overload trips separately"))
        list.add(tr("Bottom entries: Down primary, Up full secondary, Left L1, Right L2"))
        list.add(tr("Isolate before opening the front cable compartment"))
    }
}
