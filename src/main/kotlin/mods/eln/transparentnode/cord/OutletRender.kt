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
    var wallState=net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();private set
    override fun networkUnserialize(s: DataInputStream) { super.networkUnserialize(s);id=UUID(s.readLong(),s.readLong());wireless=s.readBoolean();channel=s.readUTF();supplied=s.readBoolean();horizontal=s.readBoolean();sockets=s.readInt();volts=s.readDouble();amps=s.readDouble();wallState=net.minecraft.world.level.block.Block.stateById(s.readInt()) }
    override fun draw() {
        if(!wallState.isAir) {
            mods.eln.gl.GL11.glPushMatrix()
            try {
                mods.eln.gl.GL11.glTranslated(-.5,-.5,-.5)
                mods.eln.gl.GlFrame.current().extra(mods.eln.gl.GlFrame.WallBlock(wallState))
            }finally { mods.eln.gl.GL11.glPopMatrix() }
        }
        mods.eln.gl.GL11.glPushMatrix()
        try {
            if(!wallState.isAir) { val v=front.toEnumFacing();mods.eln.gl.GL11.glTranslated(v.stepX.toDouble(),0.0,v.stepZ.toDouble()) }
            desc.draw(front,horizontal,sockets)
        }finally { mods.eln.gl.GL11.glPopMatrix() }
    }
    fun action(id: Int)=clientSendId(id.toByte())
    fun unplug(slot: Int)=clientSendInt(85.toByte(),slot)
    fun channel(value: String)=clientSendString(83.toByte(),value)
}
