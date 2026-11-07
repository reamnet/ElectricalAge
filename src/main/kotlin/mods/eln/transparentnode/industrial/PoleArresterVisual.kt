package mods.eln.transparentnode.industrial
import mods.eln.Eln
import mods.eln.gl.GL11
import mods.eln.utility.PoleLeadVisual
import net.minecraft.world.phys.Vec3

/** Called within the native primary-rack rotation; secondary/catenary frames stay independent. */
object PoleArresterVisual {
    private val mesh by lazy {requireNotNull(Eln.obj.getObj("industrial_arrester"))}
    fun draw(phases:Int,spent:Boolean) {
        GL11.glPushAttrib(0xFFFFF)
        try {
            GL11.glDisable(GL11.GL_TEXTURE_2D)
            for(i in 0 until phases) {
                GL11.glPushMatrix()
                try {
                    val z=if(phases==1)0.0 else (i-1)*.95
                    PoleLeadVisual.draw(Vec3(0.0,if(phases==1 || i==1)7.15 else 6.63,z),Vec3(.46,6.44,z))
                    GL11.glTranslated(.46,6.25,z);GL11.glScaled(.45,.45,.45)
                    GL11.glColor3f(.30f,.32f,.34f);mesh.getPart("Base")?.drawNoBind();mesh.getPart("Terminal")?.drawNoBind()
                    GL11.glColor3f(if(spent).20f else .67f,if(spent).19f else .53f,if(spent).18f else .39f);mesh.getPart("Ceramic")?.drawNoBind()
                }finally {GL11.glPopMatrix()}
            }
        }finally {GL11.glPopAttrib()}
    }
}
