package mods.eln.utility

import mods.eln.gridnode.GridRender
import mods.eln.node.transparent.*
import java.io.DataInputStream

class SubstationRender(entity:TransparentNodeEntity,descriptor:TransparentNodeDescriptor):GridRender(entity,descriptor) {
    val desc=descriptor as SubstationDescriptor
    var primaryClosed=true;var secondaryClosed=true;var selected=52
    val inputs=DoubleArray(3);val outputs=DoubleArray(3)
    var tripped=false;var heat=0.0
    override fun draw(){drawCables();desc.drawEquipment(TransmissionKind.facingAngle(front),primaryClosed,secondaryClosed)}
    override fun renderBounds():net.minecraft.world.phys.AABB {
        val p=tileEntity.blockPos;val r=desc.kind.range.toDouble()+4
        return net.minecraft.world.phys.AABB(p.x-r,p.y-r,p.z-r,p.x+r,p.y+desc.kind.height+r,p.z+r)
    }
    override fun cameraDrawOptimisation()=false
    override fun networkUnserialize(s:DataInputStream){
        super.networkUnserialize(s);primaryClosed=s.readBoolean();secondaryClosed=s.readBoolean();selected=s.readInt()
        repeat(3){inputs[it]=s.readDouble()};repeat(3){outputs[it]=s.readDouble()};tripped=s.readBoolean();heat=s.readDouble()
    }
}
