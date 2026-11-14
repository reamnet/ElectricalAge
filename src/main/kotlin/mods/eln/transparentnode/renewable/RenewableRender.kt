package mods.eln.transparentnode.renewable

import mods.eln.Eln
import mods.eln.cable.CableRenderType
import mods.eln.node.transparent.*
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

class RenewableRender(entity: TransparentNodeEntity, descriptor: TransparentNodeDescriptor) : TransparentNodeElementRender(entity, descriptor) {
    val display = SolarDisplay()
    private var cable: CableRenderType? = null
    val renewable get() = transparentNodedescriptor as RenewableDescriptor
    override fun draw() {
        cable = drawCable(renewable.connectionPlane(front), Eln.stdCableRender3200V, display.connections, cable)
        renewable.draw(front, display)
    }
    override fun cameraDrawOptimisation() = false
    override fun networkUnserialize(stream: DataInputStream) {
        super.networkUnserialize(stream); display.read(stream); cable = null
    }
    fun setVoltage(volts: Double) {
        if (!volts.isFinite()) return
        val buffer = ByteArrayOutputStream()
        DataOutputStream(buffer).use { stream -> preparePacketForServer(stream); stream.writeByte(80); stream.writeDouble(volts) }
        sendPacketToServer(buffer)
    }
}
