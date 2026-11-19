package mods.eln.transparentnode.offgrid
import mods.eln.Eln
import mods.eln.cable.CableRenderType
import mods.eln.node.transparent.*
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

class OffGridRender(tile: TransparentNodeEntity,desc: TransparentNodeDescriptor): TransparentNodeElementRender(tile,desc) {
    val display=OffGridDisplay()
    val cabinet get()=transparentNodedescriptor as OffGridDescriptor
    private var cable: CableRenderType?=null
    override fun draw() {
        cable=drawCable(cabinet.connectionPlane(front),Eln.stdCableRender50V,display.connections,cable)
        cabinet.draw(front,display)
    }
    override fun cameraDrawOptimisation()=false
    override fun networkUnserialize(s: DataInputStream) { super.networkUnserialize(s);display.read(s);cable=null }
    fun sendValue(id: Int,value: Double) {
        if(!value.isFinite())return
        val buffer=ByteArrayOutputStream()
        DataOutputStream(buffer).use { preparePacketForServer(it);it.writeByte(id);it.writeDouble(value) }
        sendPacketToServer(buffer)
    }
}
