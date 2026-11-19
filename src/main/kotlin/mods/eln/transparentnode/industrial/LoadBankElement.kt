package mods.eln.transparentnode.industrial

import mods.eln.i18n.I18N.tr
import mods.eln.misc.*
import mods.eln.node.*
import mods.eln.node.transparent.*
import mods.eln.sim.nbt.NbtElectricalGateInput
import mods.eln.sim.nbt.NbtElectricalGateOutput
import mods.eln.sim.nbt.NbtElectricalGateOutputProcess
import mods.eln.sim.mna.component.Resistor
import net.minecraft.nbt.CompoundTag
import java.io.*

class LoadBankElement(node: TransparentNode,d: TransparentNodeDescriptor):TransparentNodeElement(node,d) {
    val signal=NbtElectricalGateInput("industrialControl")
    // The source exposes a trip terminal even on this non-latching load. Preserve
    // the wired low output; thermal cutoff is automatic, not a latched trip.
    val tripSignal=NbtElectricalGateOutput("industrialTrip")
    private val tripSource=NbtElectricalGateOutputProcess("industrialTrip",tripSignal)
    val unit=LoadBankUnit(electricalLoadList,electricalComponentList,electricalProcessList,{
        val c=this.node?.coordonate
        c!=null && (c.world() as? net.minecraft.server.level.ServerLevel)?.chunkSource?.getChunkNow(c.x shr 4,c.z shr 4)!=null
    },{signal.normalized})
    init {
        electricalLoadList.addAll(listOf(signal,tripSignal));electricalComponentList.addAll(listOf(Resistor(signal,null).setR(1e10),tripSource))
        tripSource.state(false)
        slowProcessList.add(NodePeriodicPublishProcess(node,.5,0.0))
    }
    override fun initialize() { unit.prepare();connect() }
    override fun hasGui()=true
    override fun onBlockActivated(p: net.minecraft.world.entity.player.Player,side: Direction,x: Float,y: Float,z: Float)=false
    override fun getElectricalLoad(side: Direction,lrdu: LRDU)=when {
        side==front && lrdu==LRDU.Down->signal
        side==front && lrdu==LRDU.Up->tripSignal
        (side==front.left() || side==Direction.YN) && lrdu==LRDU.Down->unit.input
        else->null
    }
    override fun getConnectionMask(side: Direction,lrdu: LRDU)=when(getElectricalLoad(side,lrdu)) { null->0;signal->NodeBase.maskElectricalInputGate;tripSignal->NodeBase.maskElectricalOutputGate;else->NodeBase.maskElectricalPower }
    override fun getThermalLoad(side: Direction,lrdu: LRDU)=null
    override fun multiMeterString(side: Direction)=tr("Input: %1$; power: %2$",Utils.plotVolt("",unit.volts),Utils.plotPower("",unit.watts))
    override fun thermoMeterString(side: Direction)=tr("Case temperature: %1$ C",unit.temperature.toInt())
    override fun getWaila()=linkedMapOf(tr("Supply") to Utils.plotVolt("",unit.volts),tr("Power") to Utils.plotPower("",unit.watts),tr("Case temperature") to tr("%1$ C",unit.temperature.toInt()),tr("Status") to if(unit.working)tr("Enabled") else tr("Disabled"))
    override fun writeToNBT(t: CompoundTag) { super.writeToNBT(t);unit.write(t) }
    override fun readFromNBT(t: CompoundTag) { super.readFromNBT(t);unit.read(t) }
    override fun getItemStackNBT()=CompoundTag().also(unit::write)
    override fun readItemStackNBT(t: CompoundTag?) { if(t!=null)unit.read(t) }
    fun writeConfigTool(t: CompoundTag,p: net.minecraft.world.entity.player.Player)=unit.write(t)
    fun readConfigTool(t: CompoundTag,p: net.minecraft.world.entity.player.Player) {
        // Settings transfer cannot cool another machine or clone its meter history.
        if(t.contains("industrialSetting0") && t.contains("industrialSetting1")) {
            unit.applySettings(doubleArrayOf(t.getDoubleOr("industrialSetting0",Double.NaN),t.getDoubleOr("industrialSetting1",Double.NaN)));needPublish()
        }
    }
    override fun networkSerialize(out: DataOutputStream) { super.networkSerialize(out);LoadBankDisplay().apply { capture(unit);write(out) } }
    override fun networkUnserialize(s: DataInputStream): Byte { val id=super.networkUnserialize(s);if(unit.packet(id,s))needPublish();return id }
}
