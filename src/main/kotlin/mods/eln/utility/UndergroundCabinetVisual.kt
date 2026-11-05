package mods.eln.utility

import mods.eln.Eln
import mods.eln.gl.GL11
import mods.eln.misc.*
import mods.eln.transparentnode.utilitylight.StreetLightMount

object UndergroundCabinetVisual {
    fun model(kind: DistributionKind)=if(kind.pedestal)"undergroundservicepedestal" else "undergroundfused${kind.phases}"+if(kind.light)"light" else ""
    fun draw(kind: DistributionKind,front: Direction,display: DistributionDisplay?) {
        GL11.glPushMatrix();try {
        front.glRotateXnRef();GL11.glPushAttrib(0xFFFFF)
        try {
            val mesh=requireNotNull(Eln.obj.getObj(model(kind)))
            GL11.glDisable(GL11.GL_TEXTURE_2D)
            fun part(name: String,r: Float,g: Float,b: Float) { GL11.glColor3f(r,g,b);mesh.getPart(name)?.drawNoBind() }
            part("Case",.22f,.34f,.28f);part("Trim",.45f,.48f,.46f);part("Services",.72f,.68f,.35f)
            repeat(kind.phases) { i->
                part("Fuse$i",.70f,.73f,.65f)
                GL11.glPushMatrix();try {
                    GL11.glTranslated(-.36,.35,(i-(kind.phases-1)/2.0)*.26)
                    if(display?.fuseClosed?.getOrNull(i)==false)GL11.glRotated(100.0,0.0,0.0,1.0)
                    GL11.glTranslated(.36,-.35,-(i-(kind.phases-1)/2.0)*.26)
                    part("Door$i",.52f,.36f,.22f)
                }finally { GL11.glPopMatrix() }
            }

        } finally { GL11.glPopAttrib() }
        } finally { GL11.glPopMatrix() }
    }
}
