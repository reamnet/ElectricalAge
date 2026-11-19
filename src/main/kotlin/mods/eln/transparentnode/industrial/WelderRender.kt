package mods.eln.transparentnode.industrial

import mods.eln.Eln
import mods.eln.misc.*
import mods.eln.node.transparent.*
import java.io.*

class WelderRender(e: TransparentNodeEntity,d: TransparentNodeDescriptor):TransparentNodeElementRender(e,d) {
    val display=WelderDisplay()
    override fun draw()=(transparentNodedescriptor as WelderDescriptor).draw(front,display)
    override fun cameraDrawOptimisation()=false
    override fun getCableRender(side: Direction,lrdu: LRDU)=when {
        side==front && (lrdu==LRDU.Down || lrdu==LRDU.Up)->mods.eln.registry.ElnDeviceRegistry.signalCableDescriptor.render
        (side==front.left() || side==Direction.YN) && lrdu==LRDU.Down->Eln.stdCableRender3200V
        else->null
    }
    override fun networkUnserialize(s: DataInputStream) { super.networkUnserialize(s);display.read(s) }
    fun settings(values: DoubleArray): Boolean {
        if(!IndustrialKind.WELDER.valid(values))return false
        val b=ByteArrayOutputStream();DataOutputStream(b).use { preparePacketForServer(it);it.writeByte(80);values.forEach(it::writeDouble) };sendPacketToServer(b);return true
    }
}
