package mods.eln.transparentnode.industrial
import mods.eln.node.transparent.*
import java.io.*

class IndustrialRender(entity: TransparentNodeEntity,descriptor: TransparentNodeDescriptor): TransparentNodeElementRender(entity,descriptor) {
    val kind=(descriptor as IndustrialDescriptor).kind
    val display=IndustrialDisplay(kind)
    override fun draw()=(transparentNodedescriptor as IndustrialDescriptor).draw(front,display)
    override fun cameraDrawOptimisation()=false
    override fun getCableRender(side: mods.eln.misc.Direction,lrdu: mods.eln.misc.LRDU): mods.eln.cable.CableRenderDescriptor? {
        if(side==front && (lrdu==mods.eln.misc.LRDU.Up || lrdu==mods.eln.misc.LRDU.Down))return mods.eln.registry.ElnDeviceRegistry.signalCableDescriptor.render
        val power=(side==front.left() && (lrdu==mods.eln.misc.LRDU.Down || kind==IndustrialKind.RELAY && lrdu==mods.eln.misc.LRDU.Right)) ||
            (side==mods.eln.misc.Direction.YN && lrdu==mods.eln.misc.LRDU.Down) ||
            (side==front.right() && (kind==IndustrialKind.SHEDDER || lrdu==mods.eln.misc.LRDU.Down || lrdu==mods.eln.misc.LRDU.Right))
        return if(power)mods.eln.Eln.stdCableRender3200V else null
    }
    override fun networkUnserialize(input: DataInputStream) { super.networkUnserialize(input);display.read(input) }
    fun setSettings(values: DoubleArray): Boolean {
        if(!kind.valid(values))return false
        val b=ByteArrayOutputStream();DataOutputStream(b).use { preparePacketForServer(it);it.writeByte(80);values.forEach(it::writeDouble) };sendPacketToServer(b);return true
    }
}
