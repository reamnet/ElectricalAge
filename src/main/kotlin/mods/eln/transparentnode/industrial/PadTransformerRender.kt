package mods.eln.transparentnode.industrial
import mods.eln.misc.*
import mods.eln.node.transparent.*
import java.io.*

class PadTransformerRender(entity: TransparentNodeEntity,d: TransparentNodeDescriptor): TransparentNodeElementRender(entity,d) {
    val display=PadTransformerDisplay()
    override fun draw()=(transparentNodedescriptor as PadTransformerDescriptor).draw(front,display)
    override fun cameraDrawOptimisation()=false
    override fun networkUnserialize(input: DataInputStream) { super.networkUnserialize(input);display.read(input) }
    override fun getCableRender(side: Direction,lrdu: LRDU): mods.eln.cable.CableRenderDescriptor?=when {
        side==Direction.YN || (side==front.left() || side==front.right()) && lrdu==LRDU.Down->mods.eln.Eln.stdCableRender3200V
        side==front && lrdu==LRDU.Up->mods.eln.registry.ElnDeviceRegistry.signalCableDescriptor.render
        else->null
    }
    fun setSettings(primary: Double,secondary: Double) {
        if(!IndustrialKind.PAD.valid(doubleArrayOf(primary,secondary)))return
        val bytes=ByteArrayOutputStream();DataOutputStream(bytes).use { preparePacketForServer(it);it.writeByte(80);it.writeDouble(primary);it.writeDouble(secondary) };sendPacketToServer(bytes)
    }
}
