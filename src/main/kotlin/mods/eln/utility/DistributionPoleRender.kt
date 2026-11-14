package mods.eln.utility
import mods.eln.node.transparent.*
import java.io.DataInputStream

class DistributionPoleRender(e: TransparentNodeEntity,d: TransparentNodeDescriptor): mods.eln.gridnode.GridRender(e,d) {
    val desc=d as DistributionPoleDescriptor
    val display=DistributionDisplay()
    override fun draw() { drawCables();desc.drawEquipment(front,display) }
    override fun cameraDrawOptimisation()=false
    override fun networkUnserialize(s: DataInputStream) { super.networkUnserialize(s);display.read(s) }
}
