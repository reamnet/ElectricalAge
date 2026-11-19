package mods.eln.transparentnode.converter

import mods.eln.misc.Obj3D
import mods.eln.node.transparent.*

/** Source130 has a distinct native name because native128 already owns dc_dc_converter. */
class FixedDcDcDescriptor(name:String,model:Obj3D?,core:Obj3D?,casing:Obj3D?):VariableDcDcDescriptor(name,model,core,casing) {
    override val variable=false
    override val fixedRatio=true
    init { ElementClass=FixedDcDcElement::class.java }
    companion object {
        const val ID=130
        const val NATIVE_NAME="winding_dc_dc_converter"
        @JvmStatic fun nativeRegistryNameForSource(sourceId:Int):String?=if(sourceId==ID)NATIVE_NAME else null
    }
}

class FixedDcDcElement(node:TransparentNode,d:TransparentNodeDescriptor):VariableDcDcElement(node,d)
