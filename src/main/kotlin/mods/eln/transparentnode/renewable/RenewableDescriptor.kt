package mods.eln.transparentnode.renewable

import mods.eln.Eln
import mods.eln.gl.GL11
import mods.eln.i18n.I18N.tr
import mods.eln.misc.Direction
import mods.eln.node.transparent.PowerCabinetDescriptor
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack

/** ForkPatch solar cabinets drawn through the native 26.3 recording facade. */
class RenewableDescriptor(name: String, val kind: RenewableKind, wallMounted: Boolean = false) :
    PowerCabinetDescriptor(name, SolarElement::class.java, RenewableRender::class.java, wallMounted) {
    private val model = requireNotNull(Eln.obj.getObj(when (kind) {
        RenewableKind.MPPT -> if (wallMounted) "wallmppt" else "solarmppt"
        RenewableKind.HYBRID -> if (wallMounted) "wallhybrid" else "solarhybrid"
    }))
    fun draw(front: Direction, display: SolarDisplay? = null) {
        GL11.glPushMatrix(); GL11.glPushAttrib(0xFFFFF)
        try {
            front.inverse.glRotateZnRef()
            GL11.glDisable(GL11.GL_TEXTURE_2D)
            fun part(name: String, r: Float, g: Float, b: Float) {
                GL11.glColor3f(r, g, b); model.getPart(name)?.drawNoBind()
            }
            part("Case", .77f, .80f, .82f); part("Trim", .17f, .20f, .23f)
            part("Solar", .12f, .55f, .92f); part("Grid", .08f, .24f, .55f)
            part("Output", .97f, .37f, .05f); part("Return", .1f, .12f, .14f)
            SolarFrontPanel.draw(model, display, kind == RenewableKind.HYBRID)
        } finally { GL11.glPopAttrib(); GL11.glPopMatrix() }
    }
    override fun hasItemDraw() = true
    override fun drawItem(entity: Boolean) = draw(Direction.ZP)
    override fun addInformation(stack: ItemStack?, player: Player?, lines: MutableList<String>, advanced: Boolean) {
        if (kind == RenewableKind.MPPT) {
            lines.add(tr("10000 W MPPT solar controller; programmable 12 to 3200 V output"))
            lines.add(tr("Optional grid feed on orange output; match nominal bus voltage first"))
            lines.add(tr("Grid feed requires a live grid that accepts reverse power"))
        } else {
            lines.add(tr("20000 W hybrid solar inverter; 120 or 240 V output"))
            lines.add(tr("Solar first with grid assist, grid first, solar only or grid only"))
        }
        lines.add(if (wallMounted) tr("Wall cables: blue left PV+; black bottom PV-; orange right output")
            else tr("Blue left: solar positive; black rear: solar return; orange right: output"))
        if (kind == RenewableKind.HYBRID) lines.add(if (wallMounted) tr("Dark blue top: optional grid input") else tr("Dark blue front: optional grid input"))
        lines.add(tr("Solar input: 6 to 51200 V; 200 A maximum; output never feeds back into inputs"))
        lines.add(tr("Internal 5000 J DC link; use a UPS or batteries for sustained storage"))
    }
}
