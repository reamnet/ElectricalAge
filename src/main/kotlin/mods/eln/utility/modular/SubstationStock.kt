package mods.eln.utility.modular

import mods.eln.Eln
import mods.eln.gl.GL11.*
import net.minecraft.world.phys.AABB

/** Model stock made from unit steel and conductor profiles, shaded by the native OBJ renderer. */
internal object SubstationStock {
    val model get() = requireNotNull(Eln.obj.getObj("substation_stock"))

    inline fun draw(body: SubstationStock.() -> Unit) {
        glPushAttrib(0xFFFFF)
        try { glDisable(GL_TEXTURE_2D); body() }
        finally { glPopAttrib() }
    }

    fun box(a: AABB, rgb: Int) = box(a.minX, a.minY, a.minZ, a.maxX, a.maxY, a.maxZ, rgb)

    fun box(x0: Double, y0: Double, z0: Double, x1: Double, y1: Double, z1: Double, rgb: Int) {
        glPushMatrix()
        try {
            glTranslated((x0+x1)/2, (y0+y1)/2, (z0+z1)/2)
            glScaled(x1-x0, y1-y0, z1-z0)
            glColor3f((rgb shr 16 and 255)/255f, (rgb shr 8 and 255)/255f, (rgb and 255)/255f)
            model.getPart("main").drawNoBind()
        } finally { glPopMatrix() }
    }
}
