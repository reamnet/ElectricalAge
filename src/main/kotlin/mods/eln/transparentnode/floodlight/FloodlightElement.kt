package mods.eln.transparentnode.floodlight

import mods.eln.Eln
import mods.eln.i18n.I18N.tr
import mods.eln.item.LampDescriptor
import mods.eln.misc.*
import mods.eln.node.NodeBase
import mods.eln.node.NodePeriodicPublishProcess
import mods.eln.node.transparent.*
import mods.eln.sim.IProcess
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.nbt.*
import mods.eln.sim.process.destruct.*
import mods.eln.sixnode.lampsocket.SourceLampLife
import mods.eln.transparentnode.cord.*
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.Container
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.server.level.ServerLevel
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import java.io.*

class FloodlightElement(node:TransparentNode,d:TransparentNodeDescriptor):TransparentNodeElement(node,d),CordDevice {
    val fixture=d as FloodlightDescriptor
    val motorized=fixture.motorized
    private val items=object:TransparentNodeElementInventory(2,1,this){
        override fun getAccessibleSlotsFromSide(side:Int)=intArrayOf(0,1)
        override fun canInsertItem(slot:Int,stack:ItemStack,side:Int)=fixture.accepts(stack)
        override fun canExtractItem(slot:Int,stack:ItemStack,side:Int)=true
    }
    override fun getInventory()=items
    val electricalLoad=NbtElectricalLoad("electricalLoad").apply{rs=1e-6}
    val lamps=List(2){Resistor(electricalLoad,null).apply{r=1e19}}
    override val cord=ApplianceCord(this,this,electricalLoad)
    val cordVoltage:Double get(){val v=(0..1).mapNotNull{(Utils.getItemObject(inventory.getItem(it))as? LampDescriptor)?.nominalU}.distinct();return if(v.size>1)Double.NaN else v.firstOrNull()?:if(motorized)240.0 else 120.0}
    override fun acceptsCord(rating:CordRating)=mods.eln.sixnode.lampsocket.LampCordCompatibility.accepts(rating,cordVoltage)
    val swivelControl=NbtElectricalGateInput("swivelControl")
    val headControl=NbtElectricalGateInput("headControl")
    val beamControl=NbtElectricalGateInput("beamControl")
    var rotationAxis=HybridNodeDirection.YP;private set
    var blockFacing=HybridNodeDirection.XP;private set
    var swivelAngle=0.0;private set
    var headAngle=0.0;private set
    var beamWidth=0.0;private set
    var powered=false;private set
    var light=0;private set
    private var lifetime=0.0
    private var publishClock=0.0
    init {
        electricalLoadList.add(electricalLoad);electricalComponentList.addAll(lamps)
        if(motorized)electricalLoadList.addAll(listOf(swivelControl,headControl,beamControl))
        slowProcessList.add(IProcess(::tick));slowProcessList.add(NodePeriodicPublishProcess(node,.5,0.0))
        slowProcessList.add(VoltageStateWatchDog().set(electricalLoad).setUNominal(if(motorized)240.0 else 120.0).set(Failure(this).electronics()))
        slowProcessList.add(mods.eln.sim.MonsterPopFreeProcess(node.coordonate,9){light>0})
    }
    override fun initialize(){configure();connect()}
    private fun configure(){repeat(2){i->val s=inventory.getItem(i);val b=Utils.getItemObject(s)as? LampDescriptor;lamps[i].r=if(b!=null&&fixture.accepts(s)&&s.count==1)b.r else 1e19}}
    override fun inventoryChange(inventory:Container?){configure();needPublish()}
    fun aim(axis:Int,value:Double):Boolean {
        if(motorized||!value.isFinite()||axis !in 0..2||value !in 0.0..when(axis){0->360.0;1->180.0;else->45.0})return false
        when(axis){0->swivelAngle=value;1->headAngle=value;2->beamWidth=value};needPublish();return true
    }
    private fun tick(dt:Double){
        configure()
        if(motorized){val a=swivelControl.normalized*360;val h=headControl.normalized*180;val w=beamControl.normalized*45;if(a!=swivelAngle||h!=headAngle||w!=beamWidth){swivelAngle=a;headAngle=h;beamWidth=w;needPublish()}}
        lifetime+=dt
        val age=lifetime>=1.0;val elapsed=lifetime;if(age)lifetime=0.0
        var level=0;var count=0
        repeat(2){i->val stack=inventory.getItem(i);val b=Utils.getItemObject(stack)as? LampDescriptor
            if(b!=null&&fixture.accepts(stack)&&stack.count==1){
                if(age&&SourceLampLife.account(stack,b,electricalLoad.u,elapsed)<=0){inventory.setItem(i,ItemStack.EMPTY);inventory.setChanged()}
                else{val value=SourceLampLife.brightness(b,electricalLoad.u,10.0);level=maxOf(level,value);if(value>0)count++}
            }
        }
        if(level!=light){light=level;powered=light>0;node.lightValue=light;needPublish()}
        publishClock-=dt
        if(light>0&&publishClock<=0){publishClock=.15;project(light,count*16)}
    }
    private fun project(level:Int,range:Int){
        val world=world()as? ServerLevel?:return
        val c=coordonate();val origin=Vec3(c.x.toDouble(),c.y.toDouble(),c.z.toDouble())
        val cached=HashMap<BlockPos,Boolean>()
        for((ray,factor)in FloodlightOptics.rays(swivelAngle,headAngle,beamWidth,rotationAxis,blockFacing)){
            val end=(range*factor).toInt()
            var previous:BlockPos?=null
            for(step in 1..end){val at=BlockPos.containing(origin.add(ray.scale(step.toDouble())))
                val clear=cached.getOrPut(at){world.chunkSource.getChunkNow(at.x shr 4,at.z shr 4)!=null&&!world.getBlockState(at).isSolidRender}
                if(!clear){previous?.let{mods.eln.sixnode.lampsocket.LightBlockEntity.addLight(world,it.x,it.y,it.z,level,5)};break}
                previous=at
                if(step%2==0||step==end)mods.eln.sixnode.lampsocket.LightBlockEntity.addLight(world,at.x,at.y,at.z,level,5)
            }
        }
    }
    override fun onBlockActivated(p:Player,side:Direction,x:Float,y:Float,z:Float):Boolean {
        if(Utils.isPlayerUsingWrench(p)){blockFacing=blockFacing.left(rotationAxis);reconnect();needPublish();return true}
        val held=p.mainHandItem;if(!fixture.accepts(held))return false
        for(i in 0..1)if(inventory.getItem(i).isEmpty){inventory.setItem(i,held.copyWithCount(1));if(!p.isCreative)held.shrink(1);inventory.setChanged();return true}
        return false
    }
    override fun getElectricalLoad(side:Direction,lrdu:LRDU):mods.eln.sim.ElectricalLoad? {
        if(!FloodlightOptics.isMountingPlanePort(rotationAxis,side,lrdu))return null
        val port=HybridNodeDirection.fromInt(side.int)
        return when(port){blockFacing.back()->if(cord.plugged)null else electricalLoad;blockFacing.right(rotationAxis)->if(motorized)headControl else null;blockFacing.left(rotationAxis)->if(motorized)swivelControl else null;blockFacing.front()->if(motorized)beamControl else null;else->null}
    }
    override fun getConnectionMask(side:Direction,lrdu:LRDU):Int {val l=getElectricalLoad(side,lrdu)?:return 0;return if(l===electricalLoad)NodeBase.maskElectricalPower else NodeBase.maskElectricalInputGate}
    override fun getThermalLoad(side:Direction,lrdu:LRDU):mods.eln.sim.ThermalLoad?=null
    override fun hasGui()=true
    override fun multiMeterString(side:Direction)=Utils.plotUIP(electricalLoad.u,lamps.sumOf{it.current})
    override fun thermoMeterString(side:Direction)=""
    fun writeConfigTool(t:CompoundTag,invoker:Player){t.putDouble("swivelAngle",swivelAngle);t.putDouble("headAngle",headAngle);t.putDouble("beamWidth",beamWidth)}
    fun readConfigTool(t:CompoundTag,invoker:Player){for((axis,key)in listOf("swivelAngle","headAngle","beamWidth").withIndex())if(t.contains(key))aim(axis,t.getDoubleOr(key,Double.NaN))}
    private fun settings(t:CompoundTag){
        rotationAxis=HybridNodeDirection.fromInt(t.getIntOr("rotationAxis",front.int))?:HybridNodeDirection.YP
        blockFacing=HybridNodeDirection.fromInt(t.getIntOr("blockFacing",1))?:HybridNodeDirection.XP
        if(rotationAxis==blockFacing||rotationAxis.inverse==blockFacing)blockFacing=if(rotationAxis in listOf(HybridNodeDirection.YP,HybridNodeDirection.YN))HybridNodeDirection.XP else HybridNodeDirection.YN
        swivelAngle=t.getDoubleOr("swivelAngle",0.0).takeIf{it.isFinite()}?.coerceIn(0.0,360.0)?:0.0
        headAngle=t.getDoubleOr("headAngle",0.0).takeIf{it.isFinite()}?.coerceIn(0.0,180.0)?:0.0
        beamWidth=t.getDoubleOr("beamWidth",0.0).takeIf{it.isFinite()}?.coerceIn(0.0,45.0)?:0.0
    }
    private fun save(t:CompoundTag){t.putBoolean("initialPlacement",false);t.putInt("rotationAxis",rotationAxis.int);t.putInt("blockFacing",blockFacing.int);t.putBoolean("powered",powered);t.putDouble("swivelAngle",swivelAngle);t.putDouble("headAngle",headAngle);t.putDouble("beamWidth",beamWidth)}
    override fun readFromNBT(t:CompoundTag){super.readFromNBT(t);settings(t);powered=t.getBooleanOr("powered",false)}
    override fun writeToNBT(t:CompoundTag){super.writeToNBT(t);save(t)}
    override fun readItemStackNBT(t:CompoundTag?){if(t!=null)settings(t)}
    override fun getItemStackNBT()=CompoundTag().also(::save)
    override fun networkSerialize(s:DataOutputStream){super.networkSerialize(s);s.writeInt(rotationAxis.int);s.writeInt(blockFacing.int);s.writeBoolean(motorized);s.writeBoolean(powered);s.writeDouble(swivelAngle);s.writeDouble(headAngle);s.writeDouble(beamWidth);repeat(2){Utils.serialiseItemStack(s,inventory.getItem(it))}}
    override fun networkUnserialize(s:DataInputStream):Byte {val event=super.networkUnserialize(s);if(event.toInt()in 0..2)aim(event.toInt(),s.readDouble());return event}
}
