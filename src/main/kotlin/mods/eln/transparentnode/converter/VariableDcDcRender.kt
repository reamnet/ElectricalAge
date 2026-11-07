package mods.eln.transparentnode.converter

import mods.eln.cable.*
import mods.eln.gl.GL11
import mods.eln.generic.GenericItemUsingDamageDescriptor
import mods.eln.item.FerromagneticCoreDescriptor
import mods.eln.misc.*
import mods.eln.node.transparent.*
import mods.eln.registry.ElnDeviceRegistry
import mods.eln.sound.LoopedSound
import net.minecraft.world.item.ItemStack
import java.io.*
import kotlin.math.*

class VariableDcDcRender(tile:TransparentNodeEntity,d:TransparentNodeDescriptor):TransparentNodeElementRender(tile,d) {
    val descriptor=d as VariableDcDcDescriptor
    val settings=DcDcControl();val stacks=Array(4) { ItemStack.EMPTY }
    var ratio=1.0;private set
    var status="IDLE";private set
    var construction="";private set
    var primaryVolts=0.0;private set
    var secondaryVolts=0.0;private set
    var primaryAmps=0.0;private set
    var secondaryAmps=0.0;private set
    var primaryCelsius=20.0;private set
    var secondaryCelsius=20.0;private set
    private val door=PhysicalInterpolator(.4f,4f,.9f,.05f)
    private val load=SlewLimiter(.5f)
    private val mask=LRDUMask();private val pri=LRDUMask();private val sec=LRDUMask();private val ctl=LRDUMask()
    private var cable:CableRenderType?=null
    init {
        addLoopedSound(object:LoopedSound("eln:transformer",coordonate(),Attenuation.LINEAR) {
            override fun getVolume():Float=if(load.position>.5f)(.2f*(load.position-.5f)).coerceIn(0f,.1f) else 0f
        })
    }
    override fun networkUnserialize(s:DataInputStream) {
        super.networkUnserialize(s);settings.read(s)
        for(i in 0..3)stacks[i]=Utils.unserialiseItemStack(s)
        ratio=s.readDouble();status=s.readUTF();construction=s.readUTF()
        primaryVolts=s.readDouble();secondaryVolts=s.readDouble();primaryAmps=s.readDouble();secondaryAmps=s.readDouble();primaryCelsius=s.readDouble();secondaryCelsius=s.readDouble();mask.deserialize(s)
        pri.mask=0;sec.mask=0;ctl.mask=0
        for(lrdu in LRDU.values())if(mask.get(lrdu)) {
            val side=front.down().applyLRDU(lrdu)
            when(side) { front.left()->pri.set(lrdu,true);front.right()->sec.set(lrdu,true);else->ctl.set(lrdu,true) }
        }
        cable=null
    }
    override fun refresh(dt:Float) {
        super.refresh(dt)
        val w0=DcDcWinding.of(stacks[0]);val w1=DcDcWinding.of(stacks[1])
        val fraction=max(abs(primaryAmps)/(w0?.maximumCurrent ?: 1.0),abs(secondaryAmps)/(w1?.maximumCurrent ?: 1.0))
        load.setTarget(if(fraction.isFinite())fraction.toFloat().coerceIn(0f,1f) else 0f);load.step(dt)
        val p=tileEntity.blockPos;val near=!stacks[3].isEmpty && tileEntity.level?.getNearestPlayer(p.x+.5,p.y+.5,p.z+.5,3.0,false)!=null
        door.setTarget(if(near)1f else 0f);door.step(dt)
    }
    override fun notifyNeighborSpawn() { super.notifyNeighborSpawn();cable=null }
    override fun draw() {
        val core=GenericItemUsingDamageDescriptor.getDescriptor(stacks[2]) as? FerromagneticCoreDescriptor
        val a=DcDcWinding.of(stacks[0]);val b=DcDcWinding.of(stacks[1])
        GL11.glPushMatrix();try { front.glRotateXnRef();descriptor.draw(core?.feroPart,a?.renderedCount ?: 0,b?.renderedCount ?: 0,a?.renderedThickness ?: 1f,b?.renderedThickness ?: 1f,!stacks[3].isEmpty,door.get()) }finally { GL11.glPopMatrix() }
        cable=drawCable(front.down(),a?.descriptor?.render,pri,cable)
        cable=drawCable(front.down(),b?.descriptor?.render,sec,cable)
        cable=drawCable(front.down(),ElnDeviceRegistry.signalCableDescriptor.render,ctl,cable)
    }
    override fun getCableRender(side:Direction,lrdu:LRDU):CableRenderDescriptor?=if(lrdu!=LRDU.Down)null else when(side) {
        front.left()->DcDcWinding.of(stacks[0])?.descriptor?.render
        front.right()->DcDcWinding.of(stacks[1])?.descriptor?.render
        front,front.inverse->ElnDeviceRegistry.signalCableDescriptor.render
        else->null
    }
    fun setValue(value:Double) {
        val bytes=ByteArrayOutputStream();val output=DataOutputStream(bytes);preparePacketForServer(output);output.writeByte(41);output.writeDouble(value);sendPacketToServer(bytes)
    }
}
