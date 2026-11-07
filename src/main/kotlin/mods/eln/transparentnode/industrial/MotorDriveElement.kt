package mods.eln.transparentnode.industrial

import mods.eln.i18n.I18N.tr
import mods.eln.misc.*
import mods.eln.node.*
import mods.eln.node.transparent.*
import mods.eln.sim.nbt.*
import mods.eln.sim.mna.component.Resistor
import net.minecraft.nbt.CompoundTag
import java.io.*

class MotorDriveElement(node:TransparentNode,d:TransparentNodeDescriptor):TransparentNodeElement(node,d) {
    val signal=NbtElectricalGateInput("industrialControl")
    val tripSignal=NbtElectricalGateOutput("industrialTrip")
    private val tripSource=NbtElectricalGateOutputProcess("industrialTrip",tripSignal)
    val unit=MotorDriveUnit(electricalLoadList,electricalComponentList,electricalProcessList,{signal.normalized},{tripSource.state(it)})
    init {
        electricalLoadList.addAll(listOf(signal,tripSignal));electricalComponentList.addAll(listOf(Resistor(signal,null).setR(1e10),tripSource))
        slowProcessList.add(NodePeriodicPublishProcess(node,.5,0.0))
    }
    override fun initialize() { connect() }
    override fun hasGui()=true
    override fun onBlockActivated(p:net.minecraft.world.entity.player.Player,side:Direction,x:Float,y:Float,z:Float)=false
    override fun getElectricalLoad(side:Direction,lrdu:LRDU)=when {
        side==front && lrdu==LRDU.Down->signal
        side==front && lrdu==LRDU.Up->tripSignal
        (side==front.left() || side==Direction.YN) && lrdu==LRDU.Down->unit.input
        side==front.right() && lrdu==LRDU.Down->unit.output
        else->null
    }
    override fun getConnectionMask(side:Direction,lrdu:LRDU)=when(getElectricalLoad(side,lrdu)) { null->0;signal->NodeBase.maskElectricalInputGate;tripSignal->NodeBase.maskElectricalOutputGate;else->NodeBase.maskElectricalPower }
    override fun getThermalLoad(side:Direction,lrdu:LRDU)=null
    override fun multiMeterString(side:Direction)=tr("Input: %1$; output: %2$",Utils.plotVolt("",unit.input.u),Utils.plotVolt("",unit.output.u))
    override fun thermoMeterString(side:Direction)=tr("Case temperature: %1$ C",unit.temperature.toInt())
    override fun getWaila()=linkedMapOf(tr("Input voltage") to Utils.plotVolt("",unit.input.u),tr("Output voltage") to Utils.plotVolt("",unit.output.u),tr("Actual frequency") to tr("%1$ Hz",unit.hertz.toInt()),tr("Status") to if(unit.tripped)tr("Tripped") else if(unit.enabled)tr("Enabled") else tr("Ramping down"))
    override fun writeToNBT(t:CompoundTag) { super.writeToNBT(t);unit.write(t) }
    override fun readFromNBT(t:CompoundTag) { super.readFromNBT(t);unit.read(t) }
    override fun getItemStackNBT()=CompoundTag().also(unit::write)
    override fun readItemStackNBT(t:CompoundTag?) { if(t!=null)unit.read(t) }
    fun writeConfigTool(t:CompoundTag,p:net.minecraft.world.entity.player.Player)=unit.write(t)
    fun readConfigTool(t:CompoundTag,p:net.minecraft.world.entity.player.Player) {
        if(t.contains("industrialSetting4")) { unit.applySettings(DoubleArray(5) { t.getDoubleOr("industrialSetting$it",Double.NaN) });needPublish() }
    }
    override fun networkSerialize(s:DataOutputStream) { super.networkSerialize(s);MotorDriveDisplay().apply { capture(unit);write(s) } }
    override fun networkUnserialize(s:DataInputStream):Byte { val id=super.networkUnserialize(s);if(unit.packet(id,s))needPublish();return id }
}
