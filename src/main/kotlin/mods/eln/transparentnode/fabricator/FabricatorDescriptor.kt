package mods.eln.transparentnode.fabricator

import mods.eln.Eln
import mods.eln.gl.GL11
import mods.eln.node.transparent.*
import kotlin.math.sin

class FabricatorDescriptor(name: String) : TransparentNodeDescriptor(name, FabricatorElement::class.java, FabricatorRender::class.java) {
    private val model = requireNotNull(Eln.obj.getObj("fabricator"))
    private val body = requireNotNull(model.getPart("Cube.002_Cube.013"))
    private val etcherX = requireNotNull(model.getPart("Cube.003_Cube.012"))
    private val etcherZ = requireNotNull(model.getPart("Cube.001_Cube.010"))
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
