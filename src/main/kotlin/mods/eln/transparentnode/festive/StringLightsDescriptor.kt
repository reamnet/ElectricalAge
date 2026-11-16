package mods.eln.transparentnode.festive

import mods.eln.Eln
import mods.eln.gl.GL11
import mods.eln.misc.*
import mods.eln.node.transparent.TransparentNodeDescriptor

class StringLightsDescriptor:TransparentNodeDescriptor("String Lights",StringLightsElement::class.java,StringLightsRender::class.java) {
    private val model by lazy { requireNotNull(Eln.obj.getObj("christmas_lights")) }
    override fun mustHaveWall()=true
    override fun mustHaveFloor()=false
    override fun hasItemDraw()=true
    override fun drawItem(entity: Boolean)=draw(Direction.ZP,false)
    fun draw(front: Direction,powered: Boolean) {
        GL11.glPushMatrix()
        try {
            front.glRotateZnRef();GL11.glRotatef(180f,0f,1f,0f);GL11.glTranslatef(-.5f,-.5f,-.5f)
            model.getPart("Lights_Cube.009")?.draw()
            if(powered)UtilsClient.drawLight(model.getPart("LightOn_Cube.002"))
        } finally {GL11.glPopMatrix()}
    }
}
