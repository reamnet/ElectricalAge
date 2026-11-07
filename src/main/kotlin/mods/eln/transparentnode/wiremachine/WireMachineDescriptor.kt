package mods.eln.transparentnode.wiremachine

import mods.eln.Eln
import mods.eln.gl.GL11
import mods.eln.misc.Obj3D
import mods.eln.node.transparent.*

class WireMachineDescriptor(name:String,val kind:WireMachineKind):TransparentNodeDescriptor(name,WireMachineElement::class.java,WireMachineRender::class.java) {
    val model:Obj3D=requireNotNull(Eln.obj.getObj(kind.model)) { "Wire machine model must be indexed: ${kind.model}" }
    private val main=requireNotNull(model.getPart(if(kind==WireMachineKind.INSULATOR)"Cube.002_Cube.013" else "main"))
    private val first=model.getPart("rot1") ?: model.getPart("rot")
    private val second=model.getPart("rot2")
    override fun hasItemDraw()=true
    override fun drawItem(entity:Boolean) { draw(0f,0.0) }
    fun draw(angle:Float,rubber:Double) {
        GL11.glPushMatrix()
        try {
            if(kind==WireMachineKind.INSULATOR)GL11.glTranslated(-.5,-.5,.5)
            main.draw();first?.draw(angle,0f,0f,1f);second?.draw()
            if(kind==WireMachineKind.INSULATOR && rubber>0) {
                GL11.glPushAttrib(GL11.GL_ENABLE_BIT or GL11.GL_CURRENT_BIT)
                try {
                    GL11.glDisable(GL11.GL_TEXTURE_2D);GL11.glDisable(GL11.GL_CULL_FACE);GL11.glDisable(GL11.GL_LIGHTING)
                    GL11.glColor4f(.12f,.12f,.12f,.95f);GL11.glBegin(GL11.GL_QUADS)
                    GL11.glVertex3d(.16,.4,-.16);GL11.glVertex3d(.84,.4,-.16);GL11.glVertex3d(.84,.4,-.84);GL11.glVertex3d(.16,.4,-.84);GL11.glEnd()
                }finally { GL11.glPopAttrib() }
            }
        }finally { GL11.glPopMatrix() }
    }
}
