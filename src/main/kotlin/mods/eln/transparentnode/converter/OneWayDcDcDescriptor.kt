package mods.eln.transparentnode.converter

import mods.eln.misc.Obj3D
import mods.eln.node.transparent.*
import mods.eln.sim.power.ConverterKind

enum class OneWayDcDcMode { FIXED,BOOST,BUCK,BOOST_BUCK }

/** Source one-way grounded converter variants; the development-only floating isolation unit is separate. */
class OneWayDcDcDescriptor(name:String,model:Obj3D?,core:Obj3D?,casing:Obj3D?,val mode:OneWayDcDcMode):VariableDcDcDescriptor(name,model,core,casing) {
    override val variable=mode!=OneWayDcDcMode.FIXED
    override val oneWay=true
    override val fixedRatio=mode==OneWayDcDcMode.FIXED
    override val kind=when(mode) {
        OneWayDcDcMode.BOOST->ConverterKind.BOOST
        OneWayDcDcMode.BUCK->ConverterKind.BUCK
        else->ConverterKind.BUCK_BOOST
    }
    init { ElementClass=OneWayDcDcElement::class.java }
}

class OneWayDcDcElement(node:TransparentNode,d:TransparentNodeDescriptor):VariableDcDcElement(node,d)
