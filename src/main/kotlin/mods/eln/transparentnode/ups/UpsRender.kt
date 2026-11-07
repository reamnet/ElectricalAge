package mods.eln.transparentnode.ups
import mods.eln.Eln
import mods.eln.cable.CableRenderType
import mods.eln.node.transparent.*
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

class UpsRender(entity: TransparentNodeEntity,descriptor: TransparentNodeDescriptor): TransparentNodeElementRender(entity,descriptor) {
    val display=UpsDisplay()
    val cabinet get()=transparentNodedescriptor as UpsDescriptor
    private val left=mods.eln.misc.LRDUMask()
    private val right=mods.eln.misc.LRDUMask()
    private var leftCable: CableRenderType?=null
    private var rightCable: CableRenderType?=null
    override fun draw() {
        cabinet.draw(front,display)
        val plane=cabinet.connectionPlane(front)
        left.set(0);right.set(0)
        val a=plane.getLRDUGoingTo(front.left())!!;val b=plane.getLRDUGoingTo(front.right())!!
        left[a]=display.connections[a];right[b]=display.connections[b]
        leftCable=drawCable(plane,Eln.stdCableRender3200V,left,leftCable)
        rightCable=drawCable(plane,Eln.stdCableRender3200V,right,rightCable)
    }
    override fun cameraDrawOptimisation()=false
    override fun networkUnserialize(stream: DataInputStream) { super.networkUnserialize(stream);display.read(stream);leftCable=null;rightCable=null }
    fun setVoltage(volts: Double) {
        if(!UpsPowerUnit.validVoltage(volts))return
        val b=ByteArrayOutputStream();DataOutputStream(b).use { preparePacketForServer(it);it.writeByte(80);it.writeDouble(volts) };sendPacketToServer(b)
    }
    fun setFlag(id: Int,value: Boolean) {
        if(id !in 81..82)return
        val b=ByteArrayOutputStream();DataOutputStream(b).use { preparePacketForServer(it);it.writeByte(id);it.writeBoolean(value) };sendPacketToServer(b)
    }
}
