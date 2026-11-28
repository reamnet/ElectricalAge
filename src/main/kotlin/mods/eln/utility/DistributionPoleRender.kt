package mods.eln.utility
import mods.eln.node.transparent.*
import java.io.DataInputStream

class DistributionPoleRender(e: TransparentNodeEntity,d: TransparentNodeDescriptor): mods.eln.gridnode.GridRender(e,d) {
    val desc=d as DistributionPoleDescriptor
    val display=DistributionDisplay()
    var primaryAngle = 0f
        private set
    var secondaryAngle = 0f
        private set
    override fun draw() { drawCables();desc.drawEquipment(front,display,primaryAngle,secondaryAngle) }
    override fun cameraDrawOptimisation()=false
    override fun networkUnserialize(s: DataInputStream) { super.networkUnserialize(s);primaryAngle=s.readFloat();secondaryAngle=s.readFloat();display.read(s) }
}
