package mods.eln.utility

import mods.eln.Eln
import mods.eln.gridnode.*
import mods.eln.misc.*
import mods.eln.node.NodeBase
import mods.eln.node.NodePeriodicPublishProcess
import mods.eln.node.transparent.*
import mods.eln.sim.IProcess
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.sim.mna.component.Resistor
import mods.eln.transparentnode.standby.TransferContact
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import java.io.*

/** Source7875 prerequisite: fixed120kV/30kV scalar transformer with real passive copper losses. */
class GridSourceTransformerDescriptor:GridDescriptor(mods.eln.i18n.I18N.TR_NAME(mods.eln.i18n.I18N.Type.NONE,"Grid DC-DC Converter"),requireNotNull(Eln.obj.getObj("GridConverter")),GridSourceTransformerElement::class.java,GridSourceTransformerRender::class.java,"textures/wire.png",mods.eln.registry.ElnDeviceRegistry.veryHighVoltageCableDescriptor) {
    init {ghostGroup=mods.eln.ghost.GhostGroup().apply{for(x in 0..1)for(y in 0..1)for(z in -1..0)if(x!=0||y!=0||z!=0)addElement(x,y,z,Eln.ghostBlock,mods.eln.ghost.GhostBlock.tClearance)}}
    override fun rotationIsFixed()=true
}
class GridSourceTransformerElement(node:TransparentNode,d:TransparentNodeDescriptor):GridElement(node,d,8) {
    val primaryLoad=NbtElectricalLoad("primaryLoad").apply{rs=.00001}
    val secondaryLoad=NbtElectricalLoad("secondaryLoad").apply{rs=.00001}
    private val winding=NbtElectricalLoad("gridWinding")
    private val out=NbtElectricalLoad("gridOutput")
    private val contact=TransferContact(primaryLoad,winding).apply{closed=true}
    val tap=ServiceTap(winding,out,.25)
    val protection=SubstationProtection()
    private val items=TransparentNodeElementInventory(0,1,this)
    init {
        electricalLoadList.addAll(listOf(primaryLoad,secondaryLoad,winding,out))
        electricalComponentList.addAll(listOf(contact,tap,Resistor(out,secondaryLoad).apply{r=.01}))
        listOf(primaryLoad,secondaryLoad,winding,out).forEach{electricalComponentList.add(Resistor(it,null).apply{r=1e12})}
        electricalProcessList.add(IProcess{dt->
            val ratio=maxOf(kotlin.math.abs(primaryLoad.u)/120000,kotlin.math.abs(secondaryLoad.u)/30000)
            if(protection.sample(dt,ratio,kotlin.math.abs(contact.current)/((d as GridSourceTransformerDescriptor).cableDescriptor.electricalMaximalCurrent/4))) {contact.closed=false;needPublish()}
        })
        slowProcessList.add(NodePeriodicPublishProcess(node,1.0,.5))
    }
    override fun getInventory()=items
    override fun getGridElectricalLoad(side:Direction)=when(side){front.left()->primaryLoad;front.right()->secondaryLoad;else->null}
    override fun getElectricalLoad(side:Direction,lrdu:LRDU)=getGridElectricalLoad(side)
    override fun getConnectionMask(side:Direction,lrdu:LRDU)=if(getElectricalLoad(side,lrdu)!=null)NodeBase.maskElectricalPower else 0
    override fun getThermalLoad(side:Direction,lrdu:LRDU):mods.eln.sim.ThermalLoad?=null
    override fun hasGui()=true
    override fun onBlockActivated(player:Player,side:Direction,x:Float,y:Float,z:Float)=false
    override fun thermoMeterString(side:Direction)=""
    override fun multiMeterString(side:Direction)=Utils.plotVolt("Primary:",primaryLoad.u)+Utils.plotVolt("Secondary:",secondaryLoad.u)
    override fun getCablePoint(side:Direction,i:Int):Vec3 {
        require(i in 0..1)
        val d=descriptor as GridSourceTransformerDescriptor
        val index=if(side==front.left())1 else 0
        return (if(i==0)d.plus else d.gnd)[index].boundingBox().centre()
    }
    override fun networkSerialize(s:DataOutputStream){super.networkSerialize(s);s.writeBoolean(protection.tripped);s.writeDouble(primaryLoad.u);s.writeDouble(secondaryLoad.u)}
    override fun networkUnserialize(s:DataInputStream):Byte {val id=super.networkUnserialize(s);if(id.toInt()==80&&protection.reset(maxOf(kotlin.math.abs(primaryLoad.u)/120000,kotlin.math.abs(secondaryLoad.u)/30000))){contact.closed=true;needPublish()};return id}
    override fun getItemStackNBT()=CompoundTag().also(protection::write)
    override fun readItemStackNBT(t:CompoundTag?){super.readItemStackNBT(t);if(t!=null){protection.read(t);contact.closed=!protection.tripped}}
    override fun readFromNBT(t:CompoundTag){super.readFromNBT(t);protection.read(t);contact.closed=!protection.tripped}
    override fun writeToNBT(t:CompoundTag){super.writeToNBT(t);protection.write(t)}
}
class GridSourceTransformerRender(tile:TransparentNodeEntity,d:TransparentNodeDescriptor):GridRender(tile,d) {
    var tripped=false;var primary=0.0;var secondary=0.0
    override fun networkUnserialize(s:DataInputStream){super.networkUnserialize(s);tripped=s.readBoolean();primary=s.readDouble();secondary=s.readDouble()}
}
