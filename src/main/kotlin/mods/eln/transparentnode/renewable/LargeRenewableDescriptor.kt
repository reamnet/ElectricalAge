package mods.eln.transparentnode.renewable

import mods.eln.Eln
import mods.eln.gl.GL11
import mods.eln.ghost.GhostGroup
import mods.eln.i18n.I18N.tr
import mods.eln.misc.Direction
import mods.eln.node.transparent.*
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack

/** Separate native adapters leave the existing solar cabinet identities and packets intact. */
class LargeRenewableDescriptor(val water: Boolean):TransparentNodeDescriptor(
    if(water)"Large Watermill 3.2kV" else "Utility Wind Turbine 3.2kV",LargeRenewableElement::class.java,LargeRenewableRender::class.java) {
    private val model=requireNotNull(Eln.obj.getObj(if(water)"utilitywatermill" else "utilitywind"))
    init {
        ghostGroup=GhostGroup().apply {
            if(!water) { addRectangle(0,0,1,10,0,0);addRectangle(-1,-1,5,14,-5,5);addElement(1,9,0) }
            else { addRectangle(1,1,0,8,-4,4);addRectangle(0,0,1,5,-1,1) }
        }
    }
    fun draw(front: Direction,angle: Float=0f) {
        GL11.glPushMatrix();GL11.glPushAttrib(0xFFFFF)
        try {
            front.inverse.glRotateZnRef();GL11.glDisable(GL11.GL_TEXTURE_2D)
            fun part(name: String,r: Float,g: Float,b: Float) { GL11.glColor3f(r,g,b);model.getPart(name)?.drawNoBind() }
            part("Case",.77f,.80f,.82f);part("Trim",.17f,.20f,.23f);part("Output",.97f,.37f,.05f);part("Return",.10f,.12f,.14f)
            GL11.glPushMatrix()
            try { GL11.glTranslatef(0f,if(water)3.5f else 9.5f,if(water)-.65f else .8f);GL11.glRotatef(angle,0f,0f,1f);part("Rotor",if(water).37f else .88f,if(water).26f else .90f,if(water).14f else .91f) }finally { GL11.glPopMatrix() }
        }finally { GL11.glPopAttrib();GL11.glPopMatrix() }
    }
    override fun hasItemDraw()=true
    override fun drawItem(entity: Boolean) { GL11.glPushMatrix();try { val s=if(water).1 else .065;GL11.glScaled(s,s,s);GL11.glTranslated(0.0,-3.0,0.0);draw(Direction.ZP) }finally { GL11.glPopMatrix() } }
    override fun addInformation(stack: ItemStack?,player: Player?,lines: MutableList<String>,advanced: Boolean) {
        lines.add(if(water)tr("3200 V regulated watermill; up to 10000 W")else tr("3200 V regulated wind generator; up to 20000 W at 12 m/s"))
        lines.add(if(water)tr("Eight-metre wheel; nine-block clearance; moving water required")else tr("Nine-metre rotor; fifteen-block clearance; wind cut-in 3 m/s, cut-out 25 m/s"))
        lines.add(if(water)tr("Run flowing water behind the wheel at base level, across its width")else tr("Place in open sky; obstruction reduces available power"))
        lines.add(tr("Orange front terminal: output; reverse power is blocked"))
    }
    companion object {
        const val WIND_ID=7946
        // 7947 belongs to the established native floor UPS. Never replace its saved identity.
        const val WATER_ID=16383
        @JvmStatic fun destinationIdForSource(sourceId: Int)=when(sourceId) { 7946->WIND_ID;7947->WATER_ID;else->null }
    }
}
