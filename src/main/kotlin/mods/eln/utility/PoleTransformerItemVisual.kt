package mods.eln.utility
import mods.eln.Eln
import mods.eln.gl.GL11

/** Source can and bushing geometry; unrelated pole and overhead wiring stay pending. */
object PoleTransformerItemVisual {
    private val model by lazy { requireNotNull(Eln.obj.getObj("poletransformer")) }
    fun draw(watts: Int) {
        val size=when { watts<=10000->.72;watts<=25000->.9;watts<=50000->1.1;watts<=75000->1.28;else->1.42 }
        GL11.glPushMatrix();GL11.glPushAttrib(0xFFFFF)
        try {
            GL11.glDisable(GL11.GL_TEXTURE_2D);GL11.glScaled(size*.65,size*.65,size*.65)
            GL11.glColor3f(.53f,.55f,.54f);model.getPart("Transformer")?.drawNoBind()
            GL11.glColor3f(.32f,.43f,.44f);model.getPart("TransformerPin")?.drawNoBind()
        } finally { GL11.glPopAttrib();GL11.glPopMatrix() }
    }
}
