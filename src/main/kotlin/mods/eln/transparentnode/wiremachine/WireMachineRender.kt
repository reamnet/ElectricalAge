package mods.eln.transparentnode.wiremachine

import mods.eln.gl.GL11
import mods.eln.misc.*
import mods.eln.node.transparent.*
import mods.eln.sixnode.electricalcable.*
import net.minecraft.world.item.ItemStack
import java.io.DataInputStream

class WireMachineRender(tile:TransparentNodeEntity,d:TransparentNodeDescriptor):TransparentNodeElementRender(tile,d) {
    val machine=d as WireMachineDescriptor
    val stacks=Array(machine.kind.slotCount) { ItemStack.EMPTY }
    var selectedOption=0;private set
    var targetLengthMeters=32;private set
    var progressMeters=0.0;private set
    var progressTargetMeters=0.0;private set
    var running=false;private set
    var material:UtilityCableMaterial?=null;private set
    var metalKg=0.0;private set
    var rubberMetres=0.0;private set
    var volts=0.0;private set
    var watts=0.0;private set
    var options=emptyList<UtilityCableDescriptor>();private set
    private var angle=0f
    private var smokeClock=0f
    override fun networkUnserialize(s:DataInputStream) {
        super.networkUnserialize(s);selectedOption=s.readInt();targetLengthMeters=s.readInt();progressMeters=s.readDouble();progressTargetMeters=s.readDouble();running=s.readBoolean()
        material=UtilityCableMaterial.entries.getOrNull(s.readInt());metalKg=s.readDouble();rubberMetres=s.readDouble();volts=s.readDouble();watts=s.readDouble()
        stacks.indices.forEach { stacks[it]=Utils.unserialiseItemStack(s) }
        options=when(machine.kind) {
            WireMachineKind.ROLLER->WireProduction.rollerOptions(material ?: WireProduction.material(stacks[0]))
            WireMachineKind.INSULATOR->WireProduction.insulatorOptions(stacks[0])
            WireMachineKind.COMBINER->WireProduction.combinerOptions(WireProduction.combinerInputs.map { stacks[it] }.filterNot { it.isEmpty })
        }
    }
    override fun refresh(dt:Float) {
        super.refresh(dt)
        if(!dt.isFinite() || dt<=0)return
        if(!running || watts<=0) { smokeClock=0f;return }
        angle=(angle+dt.coerceAtMost(.25f)*120f)%360f
        if(machine.kind!=WireMachineKind.INSULATOR)return
        // Effects advance with the client clock, never with mesh baking or extra draw passes.
        smokeClock+=dt.coerceAtMost(.25f);if(smokeClock<.25f)return
        smokeClock%=.25f
        val world=tileEntity.level as? net.minecraft.client.multiplayer.ClientLevel ?: return
        val p=tileEntity.blockPos;val random=world.random
        world.addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE,p.x+.5+(random.nextDouble()-.5)*.18,p.y+.68+random.nextDouble()*.05,p.z+.5+(random.nextDouble()-.5)*.18,0.0,.015,0.0)
    }
    override fun draw() { GL11.glPushMatrix();try { front.glRotateXnRef();machine.draw(angle,rubberMetres) }finally { GL11.glPopMatrix() } }
    override fun getCableRender(side:Direction,lrdu:LRDU)=if(lrdu==LRDU.Down)mods.eln.registry.ElnDeviceRegistry.meduimVoltageCableDescriptor.render else null
    fun select(index:Int)=clientSendInt(1.toByte(),index)
    fun length(value:Int)=clientSendInt(2.toByte(),value)
}
