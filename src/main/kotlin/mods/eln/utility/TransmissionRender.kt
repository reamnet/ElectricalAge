package mods.eln.utility

import mods.eln.node.transparent.*
import mods.eln.misc.*
import java.io.DataInputStream

class TransmissionRender(entity: TransparentNodeEntity,descriptor: TransparentNodeDescriptor) : mods.eln.gridnode.GridRender(entity,descriptor) {
    val desc = descriptor as TransmissionDescriptor
    var selected = 52; private set
    var primaryAngle = 0f; private set
    val lamp = TransmissionLampDisplay()
    val inputs = DoubleArray(3)
    val services = DoubleArray(3)
    override fun draw() { drawCables(); desc.drawEquipment(front,lamp,primaryAngle) }
    // Source poles reach far beyond the owner block. Include the complete paid
    // span envelope as well, so viewing its middle cannot cull its owner.
    private val bounds = tileEntity.blockPos.let { p ->
        val reach = desc.kind.range + desc.kind.spread + 1
        net.minecraft.world.phys.AABB(p.x-reach,p.y-reach,p.z-reach,
            p.x+reach+1,p.y+desc.kind.height+reach+1,p.z+reach+1)
    }
    /** Vanilla's 64-block owner test would hide the middle of a 128/192-block span. */
    fun visibleFrom(camera: net.minecraft.world.phys.Vec3): Boolean {
        val distance=(desc.kind.range+desc.kind.height).toDouble()
        return tileEntity.blockPos.distToCenterSqr(camera)<distance*distance
    }
    override fun renderBounds() = bounds
    override fun cameraDrawOptimisation() = false
    override fun networkUnserialize(stream: DataInputStream) {
        super.networkUnserialize(stream)
        selected = stream.readInt().takeIf { it in desc.kind.terminals } ?: 52
        primaryAngle=stream.readFloat().also { if(!it.isFinite()) throw java.io.IOException("Invalid transmission primary angle") }
        repeat(3) { inputs[it] = stream.readDouble() }
        repeat(3) { services[it] = stream.readDouble() }
        lamp.read(stream)
    }
    override fun getCableRender(side: Direction,lrdu: LRDU): mods.eln.cable.CableRenderDescriptor? {
        if (side == Direction.YN) return mods.eln.Eln.stdCableRender200V
        return if (desc.kind.riser && !side.isY && lrdu == LRDU.Down && side in listOf(front.left(),front.right(),front.inverse))
            mods.eln.registry.ElnDeviceRegistry.veryHighVoltageCableDescriptor.render else null
    }
}
