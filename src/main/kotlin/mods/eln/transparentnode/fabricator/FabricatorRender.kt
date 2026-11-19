package mods.eln.transparentnode.fabricator

import mods.eln.misc.*
import mods.eln.node.transparent.*
import net.minecraft.world.item.ItemStack
import java.io.DataInputStream

class FabricatorRender(tile: TransparentNodeEntity, descriptor: TransparentNodeDescriptor) : TransparentNodeElementRender(tile, descriptor) {
    var operation: FabricatorOperation? = FabricatorOperation.TRANSISTOR; private set
    var progressJoules = 0.0; private set
    var running = false; private set
    var attempts = 0L; private set
    var successful = 0L; private set
    var acceptedJoules = 0.0; private set
    var volts = 0.0; private set
    var watts = 0.0; private set
    var phase = 0.0; private set
    val stacks = Array(3) { ItemStack.EMPTY }
    override fun networkUnserialize(stream: DataInputStream) {
        super.networkUnserialize(stream)
        operation = FabricatorOperation.entries.getOrNull(stream.readInt())
        progressJoules = stream.readDouble()
        running = stream.readBoolean()
        attempts = stream.readLong()
        successful = stream.readLong()
        acceptedJoules = stream.readDouble()
        volts = stream.readDouble()
        watts = stream.readDouble()
        repeat(3) { stacks[it] = Utils.unserialiseItemStack(stream) }
    }
    override fun refresh(seconds: Float) {
        super.refresh(seconds)
        if (running && watts > 0 && seconds.isFinite() && seconds > 0)
            phase = (phase + seconds.coerceAtMost(.25f) * 8.0) % (2.0 * Math.PI)
    }
    override fun draw() { (transparentNodedescriptor as FabricatorDescriptor).draw(phase) }
    override fun getCableRender(side: Direction, lrdu: LRDU) =
        if (side == Direction.ZN && lrdu == LRDU.Down) mods.eln.registry.ElnDeviceRegistry.meduimVoltageCableDescriptor.render else null
    fun select(operation: FabricatorOperation) = clientSendInt(0.toByte(), operation.ordinal)
}
