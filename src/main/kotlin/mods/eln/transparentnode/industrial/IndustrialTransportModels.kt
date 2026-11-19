package mods.eln.transparentnode.industrial
import mods.eln.Eln
import mods.eln.gl.GL11
private inline fun preserveMatrix(block: ()->Unit) {
    GL11.glPushMatrix()
    try { block() }finally { GL11.glPopMatrix() }
}

object IndustrialTransportModels {
    private val forklift by lazy { requireNotNull(Eln.obj.getObj("industrial_forklift")) }
    private val pallet by lazy { requireNotNull(Eln.obj.getObj("industrial_pallet")) }
    fun draw(isPallet: Boolean,lift: Double,cargo: Boolean): Unit=preserveMatrix {
        GL11.glPushAttrib(0xFFFFF)
        try {
            GL11.glDisable(GL11.GL_TEXTURE_2D)
            fun p(name: String,r: Float,g: Float,b: Float) { GL11.glColor3f(r,g,b);(if(isPallet)pallet else forklift).getPart(name)?.drawNoBind() }
            if(isPallet) {
                p("Wood",.52f,.34f,.16f);if(cargo)p("Cargo",.48f,.51f,.53f)
            } else {
                p("Body",.95f,.62f,.05f);p("Black",.08f,.09f,.10f);p("Wheel",.10f,.11f,.12f);p("Mast",.21f,.23f,.26f);p("Trim",.38f,.41f,.44f);p("Lamp",.95f,.38f,.04f)
                preserveMatrix {
                    GL11.glTranslated(0.0,lift,0.0);p("Fork",.36f,.38f,.41f)
                    if(cargo) { GL11.glTranslated(0.0,.24,1.35);draw(true,0.0,true) }
                }
            }
        } finally { GL11.glPopAttrib() }
    }
}
