package mods.eln.transparentnode.utilitylight
import mods.eln.Eln
import mods.eln.cable.CableRenderType
import mods.eln.misc.LRDU
import mods.eln.node.transparent.*
import java.io.DataInputStream

class StepDownUnitRender(e: TransparentNodeEntity,d: TransparentNodeDescriptor):TransparentNodeElementRender(e,d) {
    val display=StepDownDisplay();private var cable: CableRenderType?=null
    override fun draw() { cable=drawCable(front.down(),Eln.stdCableRender3200V,display.connections,cable);(transparentNodedescriptor as StepDownUnitDescriptor).draw(front) }
    override fun networkUnserialize(s: DataInputStream) { super.networkUnserialize(s);display.read(s);cable=null }
}
