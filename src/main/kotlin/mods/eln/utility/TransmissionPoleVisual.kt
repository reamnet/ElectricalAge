package mods.eln.utility

import mods.eln.Eln
import mods.eln.gl.GL11
import mods.eln.gridnode.ServiceCableMount
import mods.eln.misc.UtilsClient
import mods.eln.transparentnode.utilitylight.StreetLightMount
import net.minecraft.world.phys.Vec3

/** The source shared suspension and removable fixture are separate from the steel OBJ. */
object TransmissionPoleVisual {
    private val model by lazy { requireNotNull(Eln.obj.getObj("distributionpole")) }
    fun draw(lamp: TransmissionLampDisplay?) {
        GL11.glPushMatrix()
        try {
            GL11.glRotatef(90f,0f,1f,0f)
            GL11.glPushMatrix()
            try {
                GL11.glTranslated(ServiceCableMount.MIN_REACH-.405,0.0,0.0)
                GL11.glColor3f(.54f,.65f,.67f); model.getPart("Secondary1")?.drawNoBind()
            } finally { GL11.glPopMatrix() }
            PoleLeadVisual.draw(Vec3(.14,4.97,0.0),Vec3(ServiceCableMount.MIN_REACH-.17,4.97,0.0))
            PoleLeadVisual.draw(Vec3(.14,5.23,0.0),Vec3(ServiceCableMount.MIN_REACH-.19,4.97,0.0))
        } finally { GL11.glPopMatrix() }
        if (lamp?.installed != true) return
        GL11.glPushMatrix()
        try {
            GL11.glTranslated(0.0,StreetLightMount.offset(lamp.height),0.0); GL11.glRotated(lamp.yaw.toDouble(),0.0,1.0,0.0)
            fun part(name: String,r: Float,g: Float,b: Float) { GL11.glColor3f(r,g,b); model.getPart(name)?.drawNoBind() }
            part("LampBracket",.48f,.50f,.51f)
            val pivot=StreetLightMount.pivot
            GL11.glTranslated(pivot.x,pivot.y,pivot.z); GL11.glRotated(lamp.pitch.toDouble(),0.0,0.0,1.0); GL11.glTranslated(-pivot.x,-pivot.y,-pivot.z)
            part("LampHousing",.26f,.29f,.31f)
            if (lamp.light>0) { UtilsClient.disableLight(); try { part("LampLens",1f,.94f,.77f) } finally { UtilsClient.enableLight() } }
            else part("LampLens",.14f,.17f,.19f)
        } finally { GL11.glPopMatrix() }
    }
}
