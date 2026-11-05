package mods.eln.sixnode.electricalcable
import mods.eln.node.six.*
import mods.eln.misc.*
import mods.eln.gl.GL11
import java.io.DataInputStream

class UtilityCableRender(e: SixNodeEntity,side: Direction,d: SixNodeDescriptor): ElectricalCableRender(e,side,d) {
    var temperature=20f;private set
    override fun publishUnserialize(s: DataInputStream) { super.publishUnserialize(s);temperature=s.readFloat() }
    override fun draw() {
        super.draw()
        if(temperature>550) {
            GL11.glPushAttrib(0xFFFFF)
            try { UtilsClient.disableLight();UtilsClient.enableBlend();GL11.glColor4f(1f,.55f,.12f,.8f);glListCall() }finally { GL11.glPopAttrib() }
        }
    }
}
