package mods.eln.sixnode.lampsocket

import mods.eln.misc.Direction
import mods.eln.node.six.SixNodeDescriptor
import mods.eln.node.six.SixNodeEntity
import java.io.DataInputStream

class DecorativeLampRender(entity: SixNodeEntity, side: Direction, descriptor: SixNodeDescriptor) : LampSocketRender(entity,side,descriptor) {
    val style get() = (descriptor as DecorativeLampDescriptor).style
    val aim = style.newAim()
    val suspension = WarehouseSuspension()
    var fanEnabled = true
        private set
    var fanPower = 0.0
        private set
    var motionMode = 0
        private set
    private val rotor = mods.eln.transparentnode.fan.FanVisual()
    val fanAngle get() = rotor.angle
    override fun refresh(deltaT: Float) {
        super.refresh(deltaT)
        if(style.fan)rotor.step(deltaT,fanPower)
    }
    init {
        if(style.fan)addLoopedSound(object: mods.eln.sound.LoopedSound("eln:appliance_ceiling_fan",mods.eln.misc.Coordonate(entity.blockPos,entity.level)) {
            override fun getVolume()=if(rotor.speed>.001f).16f*rotor.speed else 0f
            override fun getPitch()=.55f+.45f*rotor.speed
        })
    }
    var bulbCount = 0
        private set
    override fun publishUnserialize(stream: DataInputStream) {
        super.publishUnserialize(stream)
        bulbCount = stream.readUnsignedByte().coerceAtMost(style.bulbs)
        if(style==DecorativeLampStyle.MOTION)motionMode=stream.readUnsignedByte().coerceIn(0,3)
        aim.read(stream)
        if(style.hangingWarehouse)suspension.set(stream.readDouble())
        if(style.fan) {
            fanEnabled=stream.readBoolean()
            fanPower=stream.readDouble().takeIf { it.isFinite() }?.coerceIn(0.0,1.0) ?: 0.0
        }
    }
}
