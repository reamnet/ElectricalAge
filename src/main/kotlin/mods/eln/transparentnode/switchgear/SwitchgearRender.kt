package mods.eln.transparentnode.switchgear

import mods.eln.Eln
import mods.eln.cable.CableRenderType
import mods.eln.node.transparent.*
import java.io.DataInputStream

class SwitchgearRender(e: TransparentNodeEntity,d: TransparentNodeDescriptor):TransparentNodeElementRender(e,d) {
    val kind=(d as SwitchgearDescriptor).kind
    val display=SwitchgearDisplay(kind)
    private val cables=arrayOfNulls<CableRenderType>(4)
    override fun draw() {
        val sides=listOf(front.left(),front.right(),front.inverse,front)
        sides.forEachIndexed { i,side->cables[i]=drawCable(side,Eln.stdCableRender3200V,display.connections[i],cables[i]) }
        (transparentNodedescriptor as SwitchgearDescriptor).draw(front,display.on,display.fault)
    }
    override fun cameraDrawOptimisation()=false
    override fun networkUnserialize(input: DataInputStream) { super.networkUnserialize(input);display.read(input);cables.fill(null) }
}
