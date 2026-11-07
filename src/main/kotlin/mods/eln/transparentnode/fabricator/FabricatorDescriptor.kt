package mods.eln.transparentnode.fabricator

import mods.eln.Eln
import mods.eln.gl.GL11
import mods.eln.i18n.I18N.tr
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import mods.eln.node.transparent.*
import kotlin.math.sin

class FabricatorDescriptor(name: String) : TransparentNodeDescriptor(name, FabricatorElement::class.java, FabricatorRender::class.java) {
    private val model = requireNotNull(Eln.obj.getObj("fabricator"))
    private val body = requireNotNull(model.getPart("Cube.002_Cube.013"))
    private val etcherX = requireNotNull(model.getPart("Cube.003_Cube.012"))
    private val etcherZ = requireNotNull(model.getPart("Cube.001_Cube.010"))
    override fun addInformation(stack: ItemStack?, player: Player?, lines: MutableList<String>, advanced: Boolean) {
        super.addInformation(stack, player, lines, advanced)
        lines.add(tr("Creates chips from silicon wafers and copper plates"))
        lines.add(tr("Nominal input: %1$ V / %2$ W", 200, 1000))
        lines.add(tr("Each attempt consumes %1$ J, including failed yields", FabricationRules.ATTEMPT_JOULES))
    }
    override fun hasItemDraw() = true
    override fun drawItem(entity: Boolean) { draw(0.0) }
    fun draw(phase: Double) {
        GL11.glPushMatrix()
        try {
            GL11.glTranslated(-.5, -.5, .5)
            body.draw()
            GL11.glTranslated(.15 * sin(phase), -.05 * (1.0 - kotlin.math.cos(phase * .7)), 0.0)
            etcherX.draw()
            etcherZ.draw()
        } finally { GL11.glPopMatrix() }
    }
}
