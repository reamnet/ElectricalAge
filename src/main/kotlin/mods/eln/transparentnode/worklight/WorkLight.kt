package mods.eln.transparentnode.worklight

import mods.eln.Eln
import mods.eln.misc.*
import mods.eln.node.transparent.*
import mods.eln.sim.IProcess
import mods.eln.sim.mna.component.Resistor
import mods.eln.sixnode.lampsocket.*
import mods.eln.transparentnode.*
import mods.eln.transparentnode.cord.*
import mods.eln.transparentnode.festive.WirelessFixtureFeed
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.Container
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.core.BlockPos
import java.io.*

class WorkLightDescriptor(name:String):FuelGeneratorDescriptor(name,requireNotNull(Eln.obj.getObj("gasworklight")),mods.eln.registry.ElnDeviceRegistry.meduimVoltageCableDescriptor,2000.0,240.0,1200.0){
    init{ElementClass=WorkLightElement::class.java;RenderClass=WorkLightRender::class.java;ghostGroup=mods.eln.ghost.GhostGroup().apply{addElement(0,1,0,Eln.ghostBlock,mods.eln.ghost.GhostBlock.tClearance);addElement(0,2,0,Eln.ghostBlock,mods.eln.ghost.GhostBlock.tClearance)}}
    override fun drawItem(entity:Boolean)=preserveMatrix{mods.eln.gl.GL11.glScalef(.48f,.48f,.48f);mods.eln.gl.GL11.glTranslatef(0f,-.65f,0f);drawFixture(0,2,"led",LightAim(2,-12.0))}
    fun drawFixture(light:Int,count:Int,technology:String,aim:LightAim)=WorkLightMesh.draw(requireNotNull(obj),light,count,technology,aim)
    override fun addInformation(stack:ItemStack,player:Player?,lines:MutableList<String>,advanced:Boolean){lines.add("2000 W gasoline generator; two matching replaceable bulbs");lines.add("12/120/240 V follows the installed bulbs; cord/wireless modes disable the engine")}
}
class WorkLightElement(node:TransparentNode,d:TransparentNodeDescriptor):FuelGeneratorElement(node,d),CordDevice {
    private val items=object:TransparentNodeElementInventory(1,2,this){
        override fun getAccessibleSlotsFromSide(side:Int)=intArrayOf(0)
        override fun canInsertItem(slot:Int,stack:ItemStack,side:Int)=Utils.getItemObject(stack)is mods.eln.item.LampDescriptor
        override fun canExtractItem(slot:Int,stack:ItemStack,side:Int)=true
    }
    override fun getInventory()=items
    override val cord=ApplianceCord(this,this,positiveLoad)
    override fun acceptsCord(rating:CordRating)=LampCordCompatibility.accepts(rating,operatingVoltage)
    val bulb get()=Utils.getItemObject(inventory.getItem(0))as? mods.eln.item.LampDescriptor
    val bulbCount get()=if(bulb!=null)inventory.getItem(0).count.coerceIn(0,2)else 0
    override val operatingVoltage get()=bulb?.nominalU?:120.0
    val lamp=Resistor(positiveLoad,null).apply{r=1e19}
    var wireless=false;private set
    var channel="Default channel";private set
    val aim=LightAim(2,-12.0)
    var brightness=0;private set
    private val feed=WirelessFixtureFeed(this,positiveLoad)
    private var age=0.0
    private var warmup=0.0
    private var projected=0.0
    init{electricalComponentList.remove(powerSource);powerSource.connectTo(null,null);electricalComponentList.add(lamp);slowProcessList.add(IProcess(::tick))}
    fun configureBulbs(){val b=bulb;lamp.r=if(b!=null&&bulbCount>0)b.r/bulbCount else 1e19;powerSource.setUmax(operatingVoltage);powerSource.setImax(2000*5/operatingVoltage)}
    override fun initialize(){super.initialize();configureBulbs()}
    override fun connectJob(){
        if(cord.plugged||wireless)on=false
        electricalComponentList.remove(powerSource)
        if(on){powerSource.connectTo(positiveLoad,null);electricalComponentList.add(powerSource);powerSource.p=2000.0}else {powerSource.connectTo(null,null);powerSource.p=0.0}
        super.connectJob()
    }
    override fun disconnectJob(){feed.clear();super.disconnectJob()}
    override fun inventoryChange(inventory:Container?){configureBulbs();needPublish()}
    private fun tick(dt:Double){
        configureBulbs()
        if(electricalComponentList.contains(powerSource)!=(on&&!wireless&&!cord.plugged))reconnect()
        if((wireless||cord.plugged)&&on){on=false;powerSource.p=0.0}
        feed.update(wireless&&!cord.plugged,channel)
        val b=bulb;var level=0
        if(b!=null&&bulbCount>0){
            if(kotlin.math.abs(positiveLoad.u)>b.minimalU)warmup+=dt else warmup=0.0
            level=SourceLampLife.brightness(b,positiveLoad.u,warmup)
            age+=dt
            if(age>=1.0){val elapsed=age;age=0.0;if(SourceLampLife.account(inventory.getItem(0),b,positiveLoad.u,elapsed)<=0){inventory.setItem(0,ItemStack.EMPTY);inventory.setChanged();level=0}}
        }else warmup=0.0
        if(level!=brightness){brightness=level;needPublish()}
        projected-=dt
        if(brightness>0&&projected<=0){projected=.15;project()}
    }
    private fun project(){
        val w=world()as? ServerLevel?:return;val c=coordonate()
        for(head in 0 until bulbCount){
            val lens=aim.pointZ(WorkLightOptics.lens(head),WorkLightOptics.pivot(head),head)
            val start=net.minecraft.world.phys.Vec3(c.x.toDouble(),c.y.toDouble(),c.z.toDouble()).add(WorkLightOptics.world(front,lens));val direction=WorkLightOptics.world(front,aim.rotateZ(net.minecraft.world.phys.Vec3(0.0,0.0,1.0),head))
            var last:BlockPos?=null
            for(step in 1..10){val p=BlockPos.containing(start.add(direction.scale(step.toDouble())));if(w.chunkSource.getChunkNow(p.x shr 4,p.z shr 4)==null||w.getBlockState(p).isSolidRender)break;last=p}
            last?.let{LightBlockEntity.addLight(w,it.x,it.y,it.z,brightness,5)}
        }
    }
    override fun hasGui()=true
    override fun onBlockActivated(p:Player?,side:Direction?,x:Float,y:Float,z:Float):Boolean {
        if(p==null)return false
        val held=p.mainHandItem
        if(Utils.getItemObject(held)is mods.eln.item.LampDescriptor){val current=inventory.getItem(0);if(!current.isEmpty&&!ItemStack.isSameItemSameComponents(current,held))return false;if(current.count>=2)return false;if(current.isEmpty)inventory.setItem(0,held.copyWithCount(1))else current.grow(1);if(!p.isCreative)held.shrink(1);inventory.setChanged();return true}
        if(mods.eln.compat.BucketCompat.filledBucketFluid(held)!=null){val running=on;val result=super.onBlockActivated(p,side,x,y,z);on=running;return result}
        return false
    }
    override fun getElectricalLoad(side:Direction,lrdu:LRDU)=if(cord.plugged||wireless)null else super.getElectricalLoad(side,lrdu)
    override fun getConnectionMask(side:Direction,lrdu:LRDU)=if(getElectricalLoad(side,lrdu)==null)0 else super.getConnectionMask(side,lrdu)
    override fun networkUnserialize(s:DataInputStream):Byte {
        val id=super.networkUnserialize(s)
        when(id.toInt()){
            80->{if(on)on=false else if(!wireless&&!cord.plugged&&tankLevel>0){on=true;voltageGracePeriod=FuelGeneratorDescriptor.VoltageStabilizationGracePeriod};reconnect()}
            81->{wireless=!wireless;if(wireless){on=false;powerSource.p=0.0};feed.clear();reconnect()}
            82->{channel=s.readUTF().trim().take(128);feed.clear()}
            in 83..86->aim.set((id.toInt()-83)/2,(id.toInt()-83)%2,s.readDouble())
        };needPublish();return id
    }
    fun writeConfigTool(t:CompoundTag,invoker:Player){aim.write(t)}
    fun readConfigTool(t:CompoundTag,invoker:Player){aim.read(t);needPublish()}
    private fun settings(t:CompoundTag){wireless=t.getBooleanOr("wireless",false);channel=t.getStringOr("channel","Default channel").trim().take(128);aim.read(t)}
    override fun readFromNBT(t:CompoundTag?){super.readFromNBT(t);if(t!=null){settings(t);tankLevel=tankLevel.takeIf{it.isFinite()}?.coerceIn(0.0,1.0)?:0.0}}
    override fun writeToNBT(t:CompoundTag?){super.writeToNBT(t);if(t!=null){t.putBoolean("wireless",wireless);t.putString("channel",channel);aim.write(t)}}
    override fun getItemStackNBT()=CompoundTag().apply{putDouble("tankLevel",tankLevel);putString("tankFluid",tankFluid);putBoolean("wireless",wireless);putString("channel",channel);aim.write(this)}
    override fun readItemStackNBT(t:CompoundTag?){if(t!=null){tankLevel=t.getDoubleOr("tankLevel",0.0).takeIf{it.isFinite()}?.coerceIn(0.0,1.0)?:0.0;tankFluid=t.getStringOr("tankFluid","").takeIf{it in fuels}?:fuels.firstOrNull().orEmpty();settings(t);on=false}}
    override fun networkSerialize(s:DataOutputStream){super.networkSerialize(s);s.writeDouble(tankLevel);s.writeBoolean(wireless);s.writeUTF(channel);s.writeInt(brightness);s.writeInt(bulbCount);s.writeUTF(bulb?.let{SourceLampLife.technology(it).takeIf{v->v>=0}?.let{t->SourceLampLife.technologies[t]}}?:"led");s.writeDouble(operatingVoltage);aim.write(s)}
}
