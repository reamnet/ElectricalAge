package mods.eln.utility

import mods.eln.gridnode.GridElement
import mods.eln.i18n.I18N.tr
import mods.eln.misc.*
import mods.eln.node.NodeBase
import mods.eln.node.NodePeriodicPublishProcess
import mods.eln.node.transparent.*
import mods.eln.sim.IProcess
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.transparentnode.standby.TransferContact
import mods.eln.sixnode.wirelesssignal.IWirelessSignalTx
import mods.eln.sixnode.wirelesssignal.WirelessUtils
import mods.eln.sixnode.wirelesssignal.aggregator.*
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.Container
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.IdentityHashMap
import kotlin.math.abs

/** Time-domain thermal latch used by each branch. A manual reopen cannot clear a trip. */
class PanelBreaker {
    var enabled = true; private set
    var tripped = false; private set
    var heat = 0.0; private set
    fun toggle() { if (tripped) { tripped = false; heat = 0.0; enabled = true } else enabled = !enabled }
    fun remoteEnabled(value: Boolean) { enabled=value }
    fun reset() { tripped = false; heat = 0.0; enabled = true }
    fun removed() { tripped = false; heat = 0.0 }
    fun step(seconds: Double, amps: Double, rating: Double): Boolean {
        if (!seconds.isFinite() || seconds <= 0 || !amps.isFinite() || !rating.isFinite() || rating <= 0) return false
        if (!enabled || tripped) { heat = (heat - seconds * 6.6).coerceAtLeast(0.0); return false }
        val ratio = abs(amps) / rating
        heat = if (ratio > 1.05) heat + seconds * (ratio * ratio - 1.0) else (heat - seconds * 6.6).coerceAtLeast(0.0)
        if (heat < 198.0) return false
        tripped = true; heat = 0.0; return true
    }
    fun write(t: CompoundTag, p: String) { t.putBoolean(p+"Enabled", enabled); t.putBoolean(p+"Tripped", tripped); t.putDouble(p+"Heat", heat) }
    fun read(t: CompoundTag, p: String) {
        enabled = t.getBooleanOr(p+"Enabled", true); tripped = t.getBooleanOr(p+"Tripped", false)
        heat = t.getDoubleOr(p+"Heat", 0.0).takeIf { it.isFinite() && it >= 0.0 }?.coerceAtMost(198.0) ?: 0.0
    }
}

/** Native three-bus, twelve-branch service panel. Branches are real solver contacts. */
class BreakerPanelElement(node: TransparentNode, descriptor: TransparentNodeDescriptor) : GridElement(node, descriptor, 64) {
    private val inventory = object : TransparentNodeElementInventory(12, 1, this) {
        override fun getAccessibleSlotsFromSide(side: Int) = IntArray(12) { it }
        override fun canInsertItem(slot: Int, stack: ItemStack, side: Int) = slot in 0..11 && isBreaker(stack)
        override fun canExtractItem(slot: Int, stack: ItemStack, side: Int) = slot in 0..11
    }
    override fun getInventory(): Container = inventory
    val buses = List(3) { NbtElectricalLoad("panelBus$it").apply { rs = .00001 } }
    private val serviceSplit = WiredServiceSplit(electricalLoadList,electricalComponentList,buses)
    inner class Branch(val index: Int) {
        val load = NbtElectricalLoad("panelBranch$index").apply { rs = .00001 }
        val contacts = buses.map { TransferContact(it, load) }
        val reference = Resistor(load, null).setR(1e10)
        val wirelessDemand = Resistor(load,null).setR(1e19)
        val breaker = PanelBreaker()
        var use240 = false
        val bus get() = if (use240) 2 else index % 2
        val rating get() = breakerRating(inventory.getItem(index))
        val contact get() = contacts[bus]
        var previousRating=0.0
        var powerChannel="";var controlChannel="";var aggregator=0;var dusk=false;var wirelessOn=true
        private var wirelessRp=0.0
        private val consumerDemand=IdentityHashMap<Any,Double>()
        val aggregators=arrayOf<IWirelessSignalAggregator>(BiggerAggregator(),SmallerAggregator(),ToogleAggregator())
        val handle=WirelessPowerHandle(object:WirelessPowerSupply {
            override val owner get()=this@BreakerPanelElement
            override val coordinate get()=node.coordonate
            override val powerLoad get()=load
            override val range get()=transmissionRange
            override fun getChannelState(id:Int)=wirelessOn
            override fun addToRp(resistance:Double) { if(resistance.isFinite()&&resistance>0.0)wirelessRp+=1.0/resistance }
            override fun setDemand(consumer:Any,resistance:Double) { if(resistance.isFinite()&&resistance>0.0)consumerDemand[consumer]=resistance }
            override fun removeDemand(consumer:Any) { consumerDemand.remove(consumer) }
        },index)
        fun configure() { contacts.forEachIndexed { i, c -> c.closed = i == bus && rating > 0 && breaker.enabled && !breaker.tripped && wirelessOn } }
        fun toggleVoltage() { use240=!use240;configure() }
        fun write(t: CompoundTag) { val p="panel$index"; t.putBoolean(p+"240",use240);t.putString(p+"Power",powerChannel);t.putString(p+"Control",controlChannel);t.putInt(p+"Aggregator",aggregator);t.putBoolean(p+"Dusk",dusk);(aggregators[2] as ToogleAggregator).writeToNBT(t,p+"Toggle"); breaker.write(t,p) }
        fun read(t: CompoundTag) { val p="panel$index"; use240=t.getBooleanOr(p+"240",false);powerChannel=t.getStringOr(p+"Power","").take(64);controlChannel=t.getStringOr(p+"Control","").take(64);aggregator=t.getIntOr(p+"Aggregator",0).coerceIn(0,2);dusk=t.getBooleanOr(p+"Dusk",false);if(t.contains(p+"Togglestate"))(aggregators[2] as ToogleAggregator).readFromNBT(t,p+"Toggle");breaker.read(t,p);previousRating=rating;configure() }
        fun updateWirelessLoad() { val conductance=wirelessRp+consumerDemand.values.sumOf{1.0/it};wirelessDemand.r=if(conductance>0.0)1.0/conductance else 1e19;wirelessRp=0.0 }
    }
    val branches = List(12) { Branch(it) }
    var selected = 0; private set
    var feederFromBottom = false; private set
    var selectedTerminal = 50; private set
    var transmissionRange=32;private set
    private val txSet=HashMap<String,HashSet<IWirelessSignalTx>>()
    private val txStrength=HashMap<IWirelessSignalTx,Double>()
    private var signalClock=0.0
    private var darkOutside=false
    init {
        electricalLoadList.addAll(buses)
        // Open contacts retain pin adjacency; unused service buses still need a
        // passive reference so one live bank cannot share a singular matrix.
        electricalComponentList.addAll(buses.map { Resistor(it,null).setR(1e10) })
        electricalLoadList.addAll(branches.map { it.load })
        branches.forEach { electricalComponentList.addAll(it.contacts); electricalComponentList.add(it.reference);electricalComponentList.add(it.wirelessDemand) }
        electricalProcessList.add(IProcess { serviceSplit.update() })
        slowProcessList.add(IProcess(::step))
        slowProcessList.add(NodePeriodicPublishProcess(node, .5, .25))
    }
    private fun step(dt:Double) {
        val level=node.coordonate.world() as? net.minecraft.server.level.ServerLevel
        if(level!=null)darkOutside=if(darkOutside)level.skyDarken>2 else level.skyDarken>=4
        signalClock-=dt
        if(signalClock<=0.0) { signalClock=1.0;txSet.clear();txStrength.clear();WirelessUtils.getTx(WirelessUtils.buildSpot(node.coordonate,null,0),txSet,txStrength) }
        branches.forEach { b->
            b.wirelessOn=when { b.controlChannel.isBlank()||b.controlChannel.equals("true",true)->true;b.controlChannel.equals("false",true)->false;else->txSet[b.controlChannel]?.let{b.aggregators[b.aggregator].aggregate(it)>=.5}?:false }
            if(b.dusk)b.wirelessOn=b.wirelessOn&&darkOutside
            b.updateWirelessLoad()
            if(b.breaker.step(dt,b.contact.current,b.rating)){play(mods.eln.sound.SoundCommand("eln:circuit_breaker_close").smallRange().mulVolume(.22));needPublish()};b.configure()
        }
    }
    override fun initialize() { (transparentNodeDescriptor as BreakerPanelDescriptor).checkWallSupport(this); connect(); branches.forEach { it.configure() } }
    override fun connectJob() { super.connectJob();branches.forEach { WirelessPowerChannels.register(it.powerChannel,it.handle) } }
    override fun disconnectJob() { branches.forEach { WirelessPowerChannels.unregister(it.handle) };WirelessPowerChannels.disconnectOwner(this);super.disconnectJob() }
    override fun onNeighborBlockChange() { super.onNeighborBlockChange(); (transparentNodeDescriptor as BreakerPanelDescriptor).checkWallSupport(this) }
    override fun inventoryChange(inventory: Container?) { branches.forEach { b->val next=b.rating;if(next!=b.previousRating){b.breaker.removed();b.previousRating=next};b.configure() };needPublish() }
    override fun allowsNewGridLinks() = false
    override fun selectedGridTerminal(side: Direction) = selectedTerminal
    override fun getGridElectricalLoad(side: Direction) = buses[2]
    override fun gridLoads(terminal: Int): List<mods.eln.sim.ElectricalLoad> = when(terminal) {
        50 -> buses
        in 0..5 -> listOf(buses[2])
        40, 41, 42 -> listOf(buses[terminal-40])
        in 64..75 -> listOf(branches[terminal-64].load)
        else -> emptyList()
    }
    override fun gridCircuitKey(terminal: Int) = when { terminal<6 -> "LEGACY";terminal==50->"SERVICE_BUNDLE";terminal in 40..42->listOf("SERVICE_L1","SERVICE_L2","SERVICE_240")[terminal-40];terminal in 64..75->listOf("SERVICE_L1","SERVICE_L2","SERVICE_240")[branches[terminal-64].bus];else->"LEGACY" }
    override fun renderWireCount(terminal: Int) = if (terminal == 50) 3 else 1
    override fun gridCablePoint(terminal: Int, wire: Int) = Vec3(.36, if (terminal >= 64) -.43 else .40, if (terminal == 50) (wire-1)*.22 else 0.0)
    override fun getElectricalLoad(side: Direction, lrdu: LRDU): mods.eln.sim.ElectricalLoad? {
        if (!(transparentNodeDescriptor as BreakerPanelDescriptor).acceptsCable(front, side, lrdu)) return null
        return when(side) { front.left()->buses[0]; front.right()->buses[1]; Direction.YP,Direction.YN->if ((side==Direction.YN)==feederFromBottom) buses[2] else branches[selected].load; else->null }
    }
    override fun getConnectionMask(side: Direction, lrdu: LRDU) = if (getElectricalLoad(side,lrdu)!=null) NodeBase.maskElectricalPower else 0
    override fun getThermalLoad(side: Direction, lrdu: LRDU) = null
    override fun hasGui() = true
    override fun onBlockActivated(player: Player, side: Direction, x: Float, y: Float, z: Float) = false
    override fun multiMeterString(side: Direction) = tr("Circuit %1$: %2$ V / %3$ A", selected+1, branches[selected].load.u.toInt(), branches[selected].contact.current.toInt())
    override fun thermoMeterString(side: Direction) = ""
    fun remoteCircuit(index:Int,closed:Boolean):Boolean {
        val branch=branches.getOrNull(index) ?: return false
        if(closed && (branch.breaker.tripped || branch.rating<=0))return false
        branch.breaker.remoteEnabled(closed);branch.configure();needPublish();return true
    }
    override fun getWaila() = linkedMapOf(tr("Circuits") to tr("%1$ installed; %2$ tripped",branches.count{it.rating>0},branches.count{it.breaker.tripped}))
    private fun writeSettings(t:CompoundTag) { branches.forEach { it.write(t) };t.putInt("panelSelected",selected);t.putInt("panelTerminal",selectedTerminal);t.putInt("panelRange",transmissionRange);t.putBoolean("panelFeederBottom",feederFromBottom) }
    private fun readSettings(t:CompoundTag) { branches.forEach { it.read(t) };selected=t.getIntOr("panelSelected",0).coerceIn(0,11);selectedTerminal=t.getIntOr("panelTerminal",50).takeIf { it==50||it in 40..42||it in 64..75 }?:50;transmissionRange=t.getIntOr("panelRange",32).coerceIn(1,512);feederFromBottom=t.getBooleanOr("panelFeederBottom",false) }
    override fun writeToNBT(t: CompoundTag) { super.writeToNBT(t);writeSettings(t) }
    override fun readFromNBT(t: CompoundTag) { super.readFromNBT(t);readSettings(t) }
    override fun getItemStackNBT()=CompoundTag().also(::writeSettings)
    override fun readItemStackNBT(t: CompoundTag?) { if(t!=null)readSettings(t) }
    private fun changePowerChannel(i:Int,v:String) { val b=branches.getOrNull(i)?:return;WirelessPowerChannels.unregister(b.handle);b.powerChannel=v.trim().take(64);if(node.isAdded)WirelessPowerChannels.register(b.powerChannel,b.handle) }
    private fun changeControlChannel(i:Int,v:String) { branches.getOrNull(i)?.controlChannel=v.trim().take(64) }
    override fun networkSerialize(out: DataOutputStream) { super.networkSerialize(out);out.writeInt(selected);out.writeInt(selectedTerminal);out.writeInt(transmissionRange);out.writeBoolean(feederFromBottom);branches.forEach { out.writeDouble(it.rating);out.writeBoolean(it.breaker.enabled);out.writeBoolean(it.breaker.tripped);out.writeBoolean(it.contact.closed);out.writeBoolean(it.use240);out.writeBoolean(it.dusk);out.writeUTF(it.powerChannel);out.writeUTF(it.controlChannel);out.writeInt(it.aggregator);out.writeDouble(it.load.u);out.writeDouble(it.contact.current) };node!!.lrduCubeMask.getTranslate(front.inverse).serialize(out) }
    override fun networkUnserialize(input: DataInputStream): Byte { val id=super.networkUnserialize(input);when(id.toInt()) {
        80->input.readInt().takeIf{it in 0..11}?.let{if(selected!=it){selected=it;reconnect()}}
        81,82->{val text=input.readUTF();val split=text.indexOf('\n');if(split>0){val i=text.substring(0,split).toIntOrNull();val value=text.substring(split+1).take(64);if(i!=null&&i in 0..11){if(id.toInt()==81)changePowerChannel(i,value)else changeControlChannel(i,value)}}}
        83->input.readInt().takeIf{it in 0..11}?.let{branches[it].breaker.toggle();branches[it].configure()}
        84->input.readInt().takeIf{it in 0..11}?.let{branches[it].toggleVoltage()}
        85->input.readInt().takeIf{it in 0..11}?.let{branches[it].aggregator=(branches[it].aggregator+1)%3}
        86->input.readInt().takeIf{it in listOf(32,64,128,256,512)}?.let{transmissionRange=it}
        88->input.readInt().takeIf{it in 0..11}?.let{branches[it].dusk=!branches[it].dusk}
        89->{val b=input.readBoolean();if(b!=feederFromBottom){feederFromBottom=b;reconnect()}}
        87->input.readInt().takeIf{it==50||it in 40..42||it in 64..75}?.let{selectedTerminal=it}
    };needPublish();return id }
    companion object {
        fun isBreaker(stack: ItemStack?) = breakerRating(stack) > 0.0
        fun breakerRating(stack: ItemStack?): Double {
            if (stack == null || stack.isEmpty) return 0.0
            val descriptor = mods.eln.generic.GenericItemBlockUsingDamageDescriptor.getDescriptor(stack) as? mods.eln.sixnode.electricalbreaker.ElectricalBreakerDescriptor ?: return 0.0
            return descriptor.currentLimit.takeIf { it.isFinite() && it > 0.0 } ?: 400.0
        }
    }
}
