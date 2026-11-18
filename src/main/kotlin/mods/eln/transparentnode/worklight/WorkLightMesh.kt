package mods.eln.transparentnode.worklight

import mods.eln.misc.*
import mods.eln.gl.GL11
import mods.eln.sixnode.lampsocket.LightAim

object WorkLightMesh {
    fun draw(model: Obj3D,light: Int,count: Int,technology: String,aim: LightAim) {
        GL11.glPushAttrib(0xFFFFF)
        try {
            GL11.glDisable(GL11.GL_TEXTURE_2D)
            GL11.glColor3f(.13f,.15f,.17f);model.getPart("Base")?.drawNoBind()
            GL11.glColor3f(.93f,.62f,.08f);model.getPart("Metal")?.drawNoBind()
            GL11.glColor3f(.39f,.43f,.46f);model.getPart("Trim")?.drawNoBind()
            GL11.glColor3f(.04f,.045f,.05f);model.getPart("Rubber")?.drawNoBind()
            for(i in 0..1) {
                GL11.glPushMatrix()
                val p=WorkLightOptics.pivot(i)
                GL11.glTranslated(p.x,p.y,p.z);GL11.glRotated(-aim.pan(i),0.0,1.0,0.0);GL11.glRotated(-aim.tilt(i),1.0,0.0,0.0);GL11.glTranslated(-p.x,-p.y,-p.z)
                GL11.glColor3f(.93f,.62f,.08f);model.getPart("Head${i}Metal")?.drawNoBind()
                GL11.glColor3f(.39f,.43f,.46f);model.getPart("Head${i}Trim")?.drawNoBind()
                val lit=i<count && light>=8
                if(lit)UtilsClient.disableLight()
                if(!lit)GL11.glColor3f(.17f,.20f,.22f)
                else if(technology=="farming")GL11.glColor3f(1f,.78f,.92f)
                else if(technology=="led" || technology=="fluorescent")GL11.glColor3f(.90f,.96f,1f)
                else GL11.glColor3f(1f,.9f,.65f)
                model.getPart("Light$i")?.drawNoBind()
                if(lit)UtilsClient.enableLight()
                GL11.glPopMatrix()
            }
        } finally { GL11.glPopAttrib() }
    }
}
