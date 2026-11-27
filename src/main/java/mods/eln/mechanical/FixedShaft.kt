package mods.eln.mechanical

import mods.eln.misc.Obj3D
import mods.eln.node.transparent.TransparentNode
import mods.eln.node.transparent.TransparentNodeDescriptor

class FixedShaftDescriptor(name: String, override val obj: Obj3D) :
    SimpleShaftDescriptor(name, FixedShaftElement::class, null) {
    override val static = arrayOf(obj.getPart("Stand"), obj.getPart("Shaft")).requireNoNulls()
    override val rotating = emptyArray<Obj3D.Obj3DPart>()

    override fun draw(angle: Double) {
        static.forEach { it.draw() }
    }
}

/** A stationary shaft anchor. Adjacent shaft parts share its zero speed until separated. */
class FixedShaftElement(node: TransparentNode, desc: TransparentNodeDescriptor) : StraightJointElement(node, desc) {
    override val isFixed: Boolean get() = true
}
