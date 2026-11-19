package mods.eln.transparentnode.standby

import mods.eln.Eln
import mods.eln.cable.CableRenderType
import mods.eln.node.transparent.*
import mods.eln.sound.LoopedSound
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

class BackupRender(entity: TransparentNodeEntity,descriptor: TransparentNodeDescriptor): TransparentNodeElementRender(entity,descriptor) {
    val display=BackupDisplay()
    val cabinet get()=transparentNodedescriptor as BackupDescriptor
    private var cable: CableRenderType?=null
    init {
        if((descriptor as BackupDescriptor).generator)addLoopedSound(object: LoopedSound("eln:fuelgenerator",coordonate()) {
            override fun getVolume()=GeneratorEffects.volume(display.status,display.watts/descriptor.tier.factor)
            override fun getPitch()=(.84+.10*(display.watts/descriptor.tier.watts).coerceIn(0.0,1.0)).toFloat()
        })
    }
    override fun draw() {
        cable=drawCable(cabinet.connectionPlane(front),Eln.stdCableRender3200V,display.connections,cable)
        cabinet.draw(front,display)
    }
    override fun cameraDrawOptimisation()=false
    private var smokeClock=0f
    var emittedSmoke=0;private set
    override fun refresh(deltaT: Float) {
        super.refresh(deltaT)
        if(!cabinet.generator || !GeneratorEffects.running(display.status)) { smokeClock=0f;return }
        smokeClock+=deltaT.coerceIn(0f,.25f)
        if(smokeClock<.15f)return
        smokeClock%=.15f
        val world=tileEntity.level ?: return
        val pipe=GeneratorEffects.exhaust(front,cabinet.tier);val pos=tileEntity.blockPos
        world.addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE,pos.x+.5+pipe.x,pos.y+.5+pipe.y,pos.z+.5+pipe.z,0.0,.045,0.0)
        emittedSmoke++
    }
    override fun networkUnserialize(stream: DataInputStream) { super.networkUnserialize(stream);display.read(stream);cable=null }
    fun setVoltage(value: Double) {
        if(!TransferControl.validVoltage(value))return
        val buffer=ByteArrayOutputStream()
        DataOutputStream(buffer).use { preparePacketForServer(it);it.writeByte(80);it.writeDouble(value) };sendPacketToServer(buffer)
    }
    fun setChannel(value: String) {
        val buffer=ByteArrayOutputStream()
        DataOutputStream(buffer).use { preparePacketForServer(it);it.writeByte(81);it.writeUTF(StandbyLink.channel(value)) };sendPacketToServer(buffer)
    }
}
