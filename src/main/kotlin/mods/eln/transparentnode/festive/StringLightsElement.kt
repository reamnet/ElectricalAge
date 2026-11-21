package mods.eln.transparentnode.festive

import mods.eln.Eln
import mods.eln.misc.*
import mods.eln.node.NodePeriodicPublishProcess
import mods.eln.node.transparent.*
import mods.eln.sim.IProcess
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.utility.WirelessPowerChannels
import mods.eln.utility.WirelessPowerHandle
import mods.eln.utility.WirelessPowerSupply
import mods.eln.transparentnode.cord.CordSupport
import net.minecraft.nbt.CompoundTag
import java.io.*
import kotlin.math.roundToInt

/** Source15W festive consumer. Wireless power is a real passive connection, never free light. */
open class StringLightsElement(node: TransparentNode,d: TransparentNodeDescriptor):TransparentNodeElement(node,d) {
    private val slots=TransparentNodeElementInventory(0,1,this)
    val input=NbtElectricalLoad("electricalLoad").apply { setRs(.002) }
    val loadResistor=Resistor(input,null)
    private val wirelessLink=Resistor(null,null).apply { r=.002 }
    private var supply: WirelessPowerSupply?=null
    private var supplyHandle:WirelessPowerHandle?=null
    val wirelessConnected get()=supply!=null
    var powerChannel="xmas";private set
    var nominalVolts=120;private set
    private var light=0
    init {
        electricalLoadList.add(input);electricalComponentList.add(loadResistor)
        configure();slowProcessList.add(IProcess { tick() });slowProcessList.add(NodePeriodicPublishProcess(node,.5,.25))
    }
    fun cordInput()=input
    fun cordNominalVoltage()=nominalVolts.toDouble()
    private fun configure() { val watts=when(descriptor){is ChristmasTreeDescriptor->35.0;is HolidayCandleDescriptor->5.0;else->15.0};loadResistor.r=nominalVolts.toDouble()*nominalVolts/watts }
    private fun unplugWireless() {
        if(supply!=null)Eln.simulator.removeElectricalComponent(wirelessLink)
        wirelessLink.connectTo(null,null);supply=null;supplyHandle=null;wirelessReceivers.remove(this);WirelessPowerChannels.untrackBridge(this)
    }
    private fun tick() {
        val here=node.coordonate
        val nearest=if(CordSupport.active(this) && machineCord()?.cord?.plugged!=true)WirelessPowerChannels.nearest(powerChannel,here) else null
        // Source chooses the nearest handle first; an OFF near channel blocks farther supplies.
        val next=nearest?.takeIf {it.element.getChannelState(it.id)}?.element
        if(nearest!==supplyHandle || next!==supply) {
            unplugWireless()
            if(next!=null && nearest!=null) { supply=next;supplyHandle=nearest;wirelessLink.connectTo(next.powerLoad,input);Eln.simulator.addElectricalComponent(wirelessLink);wirelessReceivers.add(this);WirelessPowerChannels.trackBridge(this,next.owner,::unplugWireless) }
            needPublish()
        }
        val ratio=kotlin.math.abs(input.u)/nominalVolts
        val value=if(ratio.isFinite())((ratio-.5)*30).coerceIn(0.0,15.0).roundToInt() else 0
        if(value!=light) { light=value;node.lightValue=value;needPublish() }
    }
    override fun initialize() { connect() }
    override fun connectJob() {
        machineCord()?.cord?.anchor?.let { nominalVolts=it.rating.volts.toInt();configure() }
        super.connectJob()
    }
    override fun disconnectJob() { unplugWireless();super.disconnectJob();light=0;node.lightValue=0 }
    override fun getInventory()=slots
    override fun hasGui()=true
    override fun onBlockActivated(player: net.minecraft.world.entity.player.Player,side: Direction,x: Float,y: Float,z: Float)=false
    override fun getLightValue()=light
    override fun getElectricalLoad(side: Direction,lrdu: LRDU)=null
    override fun getConnectionMask(side: Direction,lrdu: LRDU)=0
    override fun getThermalLoad(side: Direction,lrdu: LRDU)=null
    override fun multiMeterString(side: Direction)=Utils.plotUIP(input.u,loadResistor.current)
    override fun thermoMeterString(side: Direction)=""
    override fun networkUnserialize(stream: DataInputStream): Byte {
        val id=super.networkUnserialize(stream)
        when(id.toInt()) {
            80->{ val value=stream.readInt();if((value==120 || value==240) && machineCord()?.cord?.plugged!=true) { nominalVolts=value;configure() } }
            81->{ powerChannel=mods.eln.network.ClientValues.readString(stream).trim();unplugWireless() }
        }
        needPublish();return id
    }
    override fun readFromNBT(tag: CompoundTag) {
        super.readFromNBT(tag);powerChannel=tag.getStringOr("festiveChannel","xmas").trim().take(128)
        nominalVolts=tag.getIntOr("festiveVolts",240).takeIf { it==120 || it==240 }?:240;configure()
    }
    override fun writeToNBT(tag: CompoundTag) { super.writeToNBT(tag);tag.putString("festiveChannel",powerChannel);tag.putInt("festiveVolts",nominalVolts) }
    override fun getItemStackNBT()=CompoundTag().apply { putString("festiveChannel",powerChannel);putInt("festiveVolts",nominalVolts) }
    override fun readItemStackNBT(tag: CompoundTag?) { if(tag!=null) {powerChannel=tag.getStringOr("festiveChannel","xmas").trim().take(128);nominalVolts=tag.getIntOr("festiveVolts",240).takeIf {it==120 || it==240}?:240;configure()} }
    override fun networkSerialize(out: DataOutputStream) { super.networkSerialize(out);out.writeBoolean(light>0);out.writeUTF(powerChannel);out.writeInt(nominalVolts);out.writeBoolean(machineCord()?.cord?.plugged==true) }
    companion object {
        private val wirelessReceivers=java.util.Collections.newSetFromMap(java.util.IdentityHashMap<StringLightsElement,Boolean>())
        /** Remove both registration and pin adjacency before a supply leaves its solver system. */
        @JvmStatic fun disconnectSupply(owner: Any) {
            wirelessReceivers.filter {it.supply?.owner===owner}.toList().forEach {
                it.unplugWireless();it.input.state=0.0;it.light=0;it.node.lightValue=0;it.needPublish()
            }
        }
    }

}
