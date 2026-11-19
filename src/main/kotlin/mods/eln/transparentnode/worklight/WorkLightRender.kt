package mods.eln.transparentnode.worklight

import mods.eln.misc.*
import mods.eln.node.transparent.*
import mods.eln.sound.LoopedSound
import mods.eln.sixnode.lampsocket.LightAim
import java.io.DataInputStream

class WorkLightRender(entity: TransparentNodeEntity,descriptor: TransparentNodeDescriptor) : TransparentNodeElementRender(entity,descriptor) {
    private val fixture=descriptor as WorkLightDescriptor
    private val connections=LRDUMask()
    var running=false;private set
    var fuel=0.0;private set
    var wireless=false;private set
    var channel="";private set
    var voltage=120.0;private set
    val aim=LightAim(2,-12.0)
    var light=0;var count=0;var technology="led"
    init { addLoopedSound(object : LoopedSound("eln:fuelgenerator",coordonate()) {
        override fun getVolume()=if(running).18f else 0f
        override fun getPitch()=.9f
    }) }
    override fun draw()=preserveMatrix{front.inverse.glRotateZnRef();fixture.drawFixture(light,count,technology,aim)}
    override fun getCableRender(side:Direction,lrdu:LRDU)=if(side.isNotY&&lrdu==LRDU.Down)mods.eln.registry.ElnDeviceRegistry.meduimVoltageCableDescriptor.render else null
    override fun renderBounds()=net.minecraft.world.phys.AABB(tileEntity.blockPos).inflate(3.5)
    override fun networkUnserialize(stream: DataInputStream) {
        super.networkUnserialize(stream)
        connections.deserialize(stream);running=stream.readBoolean();stream.readFloat()
        fuel=stream.readDouble();wireless=stream.readBoolean();channel=stream.readUTF()
        light=stream.readInt();count=stream.readInt();technology=stream.readUTF();voltage=stream.readDouble()
        aim.read(stream)
    }
}
