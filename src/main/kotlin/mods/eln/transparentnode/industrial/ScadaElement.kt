package mods.eln.transparentnode.industrial

import mods.eln.i18n.I18N.tr
import mods.eln.misc.*
import mods.eln.node.*
import mods.eln.node.transparent.*
import mods.eln.sim.nbt.*
import mods.eln.sim.mna.component.Resistor
import net.minecraft.nbt.CompoundTag
import java.io.*

class ScadaElement(node:TransparentNode,d:TransparentNodeDescriptor):TransparentNodeElement(node,d) {
    val signal=NbtElectricalGateInput("industrialControl")
    val tripSignal=NbtElectricalGateOutput("industrialTrip")
    private val tripSource=NbtElectricalGateOutputProcess("industrialTrip",tripSignal)
    val unit=ScadaUnit(electricalLoadList,electricalComponentList,electricalProcessList,
        {this.node?.coordonate?.pos()},
        {val c=this.node?.coordonate;c!=null && (c.world() as? net.minecraft.server.level.ServerLevel)?.chunkSource?.getChunkNow(c.x shr 4,c.z shr 4)!=null},
        {signal.normalized},{address,circuit->ScadaLink.read(this,address,circuit)},{address,circuit,closed->ScadaLink.command(this,address,circuit,closed)})
    init {
        electricalLoadList.addAll(listOf(signal,tripSignal));electricalComponentList.addAll(listOf(Resistor(signal,null).setR(1e10),tripSource));tripSource.state(false)
        slowProcessList.add(NodePeriodicPublishProcess(node,.5,0.0))
    }
    override fun initialize() { connect() }
    override fun hasGui()=true
    override fun onBlockActivated(p:net.minecraft.world.entity.player.Player,side:Direction,x:Float,y:Float,z:Float)=false
    override fun getElectricalLoad(side:Direction,lrdu:LRDU)=when {
        side==front && lrdu==LRDU.Down->signal
        side==front && lrdu==LRDU.Up->tripSignal
        (side==front.left() || side==Direction.YN) && lrdu==LRDU.Down->unit.input
        else->null
    }
    override fun getConnectionMask(side:Direction,lrdu:LRDU)=when(getElectricalLoad(side,lrdu)) { null->0;signal->NodeBase.maskElectricalInputGate;tripSignal->NodeBase.maskElectricalOutputGate;else->NodeBase.maskElectricalPower }
    override fun getThermalLoad(side:Direction,lrdu:LRDU)=null
    override fun multiMeterString(side:Direction)=Utils.plotUIP(unit.input.u,unit.resistor.current)
    override fun thermoMeterString(side:Direction):String?=null
    override fun getWaila()=linkedMapOf(tr("Supply") to Utils.plotVolt("",unit.input.u),tr("Power") to Utils.plotPower("",unit.resistor.p),tr("Selected target") to (unit.selected+1).toString(),tr("Status") to if(unit.canCommand)tr("Remote control ready") else tr("Remote control unavailable"))
    override fun writeToNBT(t:CompoundTag) { super.writeToNBT(t);unit.write(t) }
    override fun readFromNBT(t:CompoundTag) { super.readFromNBT(t);unit.read(t) }
    override fun getItemStackNBT()=CompoundTag().also(unit::write)
    override fun readItemStackNBT(t:CompoundTag?) { if(t!=null)unit.read(t) }
    override fun networkSerialize(s:DataOutputStream) { super.networkSerialize(s);ScadaDisplay().apply { capture(unit);write(s) } }
    override fun networkUnserialize(s:DataInputStream):Byte { val id=super.networkUnserialize(s);if(unit.packet(id,s))needPublish();return id }
}
