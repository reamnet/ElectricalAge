package mods.eln.transparentnode.industrial

import mods.eln.i18n.I18N.tr
import mods.eln.misc.*
import mods.eln.node.*
import mods.eln.node.transparent.*
import mods.eln.sim.nbt.*
import net.minecraft.nbt.CompoundTag
import java.io.*

/** Registered relay/shedder adapter. Other industrial kinds await their own circuits. */
class IndustrialElement(node: TransparentNode,descriptor: TransparentNodeDescriptor): TransparentNodeElement(node,descriptor) {
    val kind=(descriptor as IndustrialDescriptor).kind
    val signal=NbtElectricalGateInput("industrialControl")
    val tripSignal=NbtElectricalGateOutput("industrialTrip")
    val tripSource=NbtElectricalGateOutputProcess("industrialTrip",tripSignal)
    val unit=IndustrialControlUnit(kind,electricalLoadList,electricalComponentList,electricalProcessList,{signal.normalized},{tripSource.state(it)})
    init {
        electricalLoadList.addAll(listOf(signal,tripSignal));electricalComponentList.add(tripSource)
        electricalComponentList.add(mods.eln.sim.mna.component.Resistor(signal,null).setR(1e10))
        slowProcessList.add(NodePeriodicPublishProcess(node,.5,0.0))
    }
    override fun initialize() { unit.configure();connect() }
    override fun hasGui()=true
    override fun onBlockActivated(player: net.minecraft.world.entity.player.Player,side: Direction,vx: Float,vy: Float,vz: Float)=false
    override fun getElectricalLoad(side: Direction,lrdu: LRDU): mods.eln.sim.ElectricalLoad?=when {
        side==front && lrdu==LRDU.Up->tripSignal
        side==front && lrdu==LRDU.Down->signal
        kind==IndustrialKind.RELAY && lrdu==LRDU.Right && side==front.left()->unit.returnIn
        kind==IndustrialKind.RELAY && lrdu==LRDU.Right && side==front.right()->unit.returnOut
        (side==front.left() || side==Direction.YN) && lrdu==LRDU.Down->unit.input
        side==front.right() && kind==IndustrialKind.SHEDDER->unit.outputs[lrdu.dir]
        side==front.right() && lrdu==LRDU.Down && kind==IndustrialKind.RELAY->unit.outputs[0]
        else->null
    }
    override fun getConnectionMask(side: Direction,lrdu: LRDU)=when(getElectricalLoad(side,lrdu)) { null->0;signal->NodeBase.maskElectricalInputGate;tripSignal->NodeBase.maskElectricalOutputGate;else->NodeBase.maskElectricalPower }
    override fun getThermalLoad(side: Direction,lrdu: LRDU)=null
    override fun multiMeterString(side: Direction)=tr("Input: %1$; output: %2$",Utils.plotVolt("",unit.input.u),Utils.plotPower("",unit.inputWatts))
    override fun thermoMeterString(side: Direction): String?=null
    override fun getWaila()=linkedMapOf(tr("Supply") to Utils.plotVolt("",unit.input.u),tr("Power") to Utils.plotPower("",unit.inputWatts),tr("Status") to if(unit.protection.tripped)tr("Tripped") else if(kind==IndustrialKind.SHEDDER)shedStatusText(unit.shed.status) else if(unit.enabled)tr("Enabled") else tr("Disabled"))
    override fun readFromNBT(t: CompoundTag) { super.readFromNBT(t);unit.read(t) }
    override fun writeToNBT(t: CompoundTag) { super.writeToNBT(t);unit.write(t) }
    override fun getItemStackNBT()=CompoundTag().also(unit::write)
    override fun readItemStackNBT(t: CompoundTag?) { if(t!=null)unit.read(t) }
    override fun networkUnserialize(s: DataInputStream): Byte { val id=super.networkUnserialize(s);if(unit.packet(id,s))needPublish();return id }
    override fun networkSerialize(out: DataOutputStream) {
        super.networkSerialize(out);IndustrialDisplay(kind).apply { capture(unit);write(out) }
    }
}
