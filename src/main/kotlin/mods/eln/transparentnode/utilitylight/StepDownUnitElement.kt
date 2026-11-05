package mods.eln.transparentnode.utilitylight

import mods.eln.Eln
import mods.eln.i18n.I18N.tr
import mods.eln.misc.*
import mods.eln.node.NodeBase
import mods.eln.node.NodePeriodicPublishProcess
import mods.eln.node.transparent.*
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.entity.player.Player
import java.io.*

class StepDownUnitElement(node: TransparentNode,d: TransparentNodeDescriptor):TransparentNodeElement(node,d) {
    val unit=MainsStepDown(this)
    private val slots=TransparentNodeElementInventory(0,1,this)
    override fun getInventory()=slots
    init { thermalLoadList.add(unit.thermal);slowProcessList.add(NodePeriodicPublishProcess(node,1.0,.5)) }
    override fun initialize() { connect() }
    override fun connectJob() { super.connectJob();Eln.simulator.mna.addProcess(unit.controller) }
    override fun disconnectJob() { Eln.simulator.mna.removeProcess(unit.controller);super.disconnectJob() }
    override fun hasGui()=true
    override fun onBlockActivated(p: Player,side: Direction,x: Float,y: Float,z: Float)=false
    override fun getElectricalLoad(side: Direction,lrdu: LRDU)=if(lrdu!=LRDU.Down)null else when(side) { front.left()->unit.primary;front.right()->unit.secondary;else->null }
    override fun getConnectionMask(side: Direction,lrdu: LRDU)=if(getElectricalLoad(side,lrdu)==null)0 else NodeBase.maskElectricalPower
    override fun getThermalLoad(side: Direction,lrdu: LRDU)=if(getElectricalLoad(side,lrdu)==null)null else unit.thermal
    override fun multiMeterString(side: Direction)=tr("Input: %1$; output: %2$",Utils.plotVolt("",unit.primary.u),Utils.plotVolt("",unit.secondary.u))
    override fun thermoMeterString(side: Direction)=Utils.plotCelsius("",unit.thermal.Tc)
    override fun getWaila()=linkedMapOf(tr("Input voltage") to Utils.plotVolt("",unit.primary.u),tr("Output voltage") to Utils.plotVolt("",unit.secondary.u),tr("Output power") to Utils.plotPower("",unit.output.p),tr("Converter state") to stepDownStatus(unit.controller.status))
    override fun writeToNBT(t: CompoundTag) { super.writeToNBT(t);unit.write(t) }
    override fun readFromNBT(t: CompoundTag) { super.readFromNBT(t);unit.read(t) }
    override fun getItemStackNBT()=CompoundTag().also(unit::write)
    override fun readItemStackNBT(t: CompoundTag?) { if(t!=null)unit.read(t) }
    fun writeConfigTool(t: CompoundTag,p: Player)=unit.write(t)
    fun readConfigTool(t: CompoundTag,p: Player) { if(t.contains("mainsOutputVoltage")) { unit.read(t);needPublish() } }
    override fun networkSerialize(out: DataOutputStream) { super.networkSerialize(out);StepDownDisplay().apply { capture(unit);connections.set(node.lrduCubeMask.getTranslate(front.down()).mask);write(out) } }
    override fun networkUnserialize(input: DataInputStream): Byte {
        val id=super.networkUnserialize(input)
        try { when(id.toInt()) { 80->unit.configure(input.readInt(),unit.ratedWatts,unit.enabled);81->unit.configure(unit.outputVoltage,unit.ratedWatts,input.readBoolean());86->unit.configure(unit.outputVoltage,input.readInt(),unit.enabled);87->unit.reset() } }catch(_: IOException) {}
        needPublish();return id
    }
}
