package mods.eln.transparentnode.cord

import mods.eln.node.transparent.*
import java.io.DataInputStream
import java.util.UUID

class OutletRender(tile: TransparentNodeEntity,d: TransparentNodeDescriptor): TransparentNodeElementRender(tile,d) {
    val desc=d as OutletDescriptor
    var id=UUID(0,0);private set
    var wireless=false;private set
    var channel="";private set
    var supplied=false;private set
    var horizontal=false;private set
    var sockets=0;private set
    var volts=0.0;private set
    var amps=0.0;private set
    override fun networkUnserialize(s: DataInputStream) { super.networkUnserialize(s);id=UUID(s.readLong(),s.readLong());wireless=s.readBoolean();channel=s.readUTF();supplied=s.readBoolean();horizontal=s.readBoolean();sockets=s.readInt();volts=s.readDouble();amps=s.readDouble() }
    override fun draw()=desc.draw(front,horizontal,sockets)
    fun action(id: Int)=clientSendId(id.toByte())
    fun unplug(slot: Int)=clientSendInt(85.toByte(),slot)
    fun channel(value: String)=clientSendString(83.toByte(),value)
}
