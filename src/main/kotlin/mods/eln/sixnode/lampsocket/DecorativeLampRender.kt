package mods.eln.sixnode.lampsocket

import mods.eln.misc.Direction
import mods.eln.node.six.SixNodeDescriptor
import mods.eln.node.six.SixNodeEntity
import java.io.DataInputStream

class DecorativeLampRender(entity: SixNodeEntity, side: Direction, descriptor: SixNodeDescriptor) : LampSocketRender(entity,side,descriptor) {
    var bulbCount = 0
        private set
    override fun publishUnserialize(stream: DataInputStream) {
        super.publishUnserialize(stream)
        bulbCount = stream.readUnsignedByte().coerceAtMost((descriptor as DecorativeLampDescriptor).style.bulbs)
    }
}
