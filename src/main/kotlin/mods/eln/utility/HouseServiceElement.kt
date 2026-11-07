package mods.eln.utility

import mods.eln.Eln
import mods.eln.i18n.I18N.tr
import mods.eln.misc.*
import mods.eln.node.*
import mods.eln.node.transparent.*
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack
import net.minecraft.world.entity.player.Player
import java.io.*

/** Service cabinets use placed cables; weatherheads terminate independent overhead service buses. */
class HouseServiceElement(node: TransparentNode,d: TransparentNodeDescriptor): mods.eln.gridnode.GridElement(node,d,64) {
    private val serviceDesc=d as HouseServiceDescriptor
    val desc get()=serviceDesc
    val unit=HouseServiceUnit(desc.meter,desc.disconnect,electricalLoadList,electricalComponentList,electricalProcessList)
    private val meters=object: TransparentNodeElementInventory(if(desc.meter)1 else 0,1,this) {
        fun raw()=super.getStackInSlot(0)
        override fun getStackInSlot(slot: Int): ItemStack { if(slot==0)syncMeter();return super.getStackInSlot(slot) }
        override fun setInventorySlotContents(slot: Int,stack: ItemStack) {
            if(slot!=0 || !desc.meter)return
            val old=raw()
            if(old===stack) { super.setInventorySlotContents(slot,stack);return }
            syncMeter();super.setInventorySlotContents(slot,stack)
            unit.installMeter(UtilityItems.part(stack) ?: 0,ServiceMeters.data(stack));syncMeter();markDirty()
        }
        override fun clearContent() { if(desc.meter)setInventorySlotContents(0,ItemStack.EMPTY) }
        override fun getAccessibleSlotsFromSide(side: Int)=if(desc.meter)intArrayOf(0) else intArrayOf()
        override fun canInsertItem(slot: Int,stack: ItemStack?,side: Int)=desc.meter && slot==0 && UtilityItems.part(stack) in 1..2
        override fun canExtractItem(slot: Int,stack: ItemStack?,side: Int)=desc.meter && slot==0
    }
    override fun getInventory()=meters
    fun rawMeter()=meters.raw()
    fun syncMeter() { if(desc.meter)ServiceMeters.store(meters.raw(),unit.energy,unit.disconnected) }
    init {
        unit.wiredSplitAllowed={ !desc.overhead && gridLinkList.isEmpty() && gridLinksBooting.isEmpty() }
        slowProcessList.add(mods.eln.sim.IProcess { desc.checkSupport(this);if(desc.wide)WideMeterWiring.syncGhostShape(this) })
        slowProcessList.add(NodePeriodicPublishProcess(node,.5,0.0))
    }
    override fun initialize() { unit.configure();super.initialize();if(desc.wide)WideMeterWiring.refreshCables(this) }
    override fun inventoryChange(inventory: Container?) { unit.configure();needPublish() }
    override fun onBreakElement() { syncMeter();super.onBreakElement() }
    override fun onNeighborBlockChange() { super.onNeighborBlockChange();desc.checkSupport(this) }
    fun setRemoteDisconnect(open: Boolean): Boolean {
        val changed=open!=unit.disconnected
        if(!unit.setRemoteDisconnect(open))return false
        if(changed) { syncMeter();play(mods.eln.sound.SoundCommand(if(open)"eln:circuit_breaker_open" else "eln:circuit_breaker_close").smallRange().mulVolume(.25));needPublish() }
        return true
    }
    override fun onBlockActivated(player: Player,side: Direction,x: Float,y: Float,z: Float): Boolean {
        if(desc.disconnect && player.mainHandItem.isEmpty && !player.isShiftKeyDown) { setRemoteDisconnect(!unit.disconnected);return true }
        if(desc.meter && unit.meterType==0 && UtilityItems.part(player.mainHandItem) in 1..2) {
            meters.setItem(0,player.mainHandItem.copyWithCount(1));if(!player.abilities.instabuild)player.mainHandItem.shrink(1);player.inventory.setChanged();return true
        }
        return super.onBlockActivated(player,side,x,y,z)
    }
    var terminal=50;private set
    override fun allowsNewGridLinks()=desc.overhead
    override fun selectedGridTerminal(side: Direction)=terminal
    override fun getGridElectricalLoad(side: Direction)=unit.line[unit.circuit]
    override fun gridLoads(terminal: Int): List<mods.eln.sim.ElectricalLoad> = when(terminal) {
        50->unit.line;51->if(desc.meter)unit.load else emptyList();40,41,42->listOf(unit.load[terminal-40]);in 0..5->listOf(unit.line[unit.circuit]);else->emptyList()
    }
    override fun gridCircuitKey(terminal: Int)=when(terminal) { 50,51->"SERVICE_BUNDLE";40->"SERVICE_L1";41->"SERVICE_L2";42->"SERVICE_240";else->"LEGACY" }
    override fun renderWireCount(terminal: Int)=if(terminal in 50..51)3 else 1
    override fun gridCablePoint(terminal: Int,wire: Int): net.minecraft.world.phys.Vec3 {
        val i=if(terminal in 50..51)wire else (terminal-40).coerceIn(0,2)
        return net.minecraft.world.phys.Vec3(if(!desc.meter)desc.mast.headX else .36,if(!desc.meter)desc.mast.headY else if(terminal==50).38 else -.40,if(!desc.meter)desc.mast.headZ+(i-1)*desc.mast.spacing else (i-1)*.17)
    }
    override fun getElectricalLoad(side: Direction,lrdu: LRDU): mods.eln.sim.ElectricalLoad? {
        if(desc.wide)return if(side!=Direction.YN)null else when(lrdu) { WideMeterWiring.port(this,false)->unit.line[unit.circuit];WideMeterWiring.port(this,true)->unit.load[unit.circuit];else->null }
        if(!desc.acceptsCable(front,side,lrdu))return null
        return when(side) { Direction.YP->unit.line[unit.circuit];Direction.YN->unit.load[unit.circuit];front.left()->unit.load[0];front.right()->unit.load[1];else->null }
    }
    override fun getConnectionMask(side: Direction,lrdu: LRDU)=if(getElectricalLoad(side,lrdu)!=null)NodeBase.maskElectricalPower else 0
    override fun getThermalLoad(side: Direction,lrdu: LRDU)=null
    override fun hasGui()=true
    override fun multiMeterString(side: Direction)=tr("Service: %1$ / %2$ / %3$",Utils.plotVolt("",unit.load[0].u),Utils.plotVolt("",unit.load[1].u),Utils.plotVolt("",unit.load[2].u))
    override fun thermoMeterString(side: Direction): String?=null
    override fun getWaila()=linkedMapOf(tr("Service") to multiMeterString(front)).apply { if(desc.meter) { put(tr("Imported energy"),tr("%1$ kWh",unit.energy.importedKwh));put(tr("Exported energy"),tr("%1$ kWh",unit.energy.exportedKwh)) } }
    override fun writeToNBT(t: CompoundTag) { syncMeter();super.writeToNBT(t);unit.write(t);t.putInt("serviceTerminal",terminal) }
    override fun readFromNBT(t: CompoundTag) {
        super.readFromNBT(t);unit.read(t);terminal=t.getIntOr("serviceTerminal",50).takeIf { it in listOf(50,40,41,42) } ?: 50
        if(desc.meter && unit.meterType>0)ServiceMeters.data(meters.raw())?.let { unit.installMeter(unit.meterType,it) }
        syncMeter()
    }
    override fun getItemStackNBT()=CompoundTag().also { t->t.putInt("serviceCircuit",unit.circuit);t.putInt("serviceTerminal",terminal);if(desc.disconnect)t.putBoolean("meterDisconnected",unit.disconnected) }
    override fun readItemStackNBT(t: CompoundTag?) { if(t!=null) { unit.read(t);terminal=t.getIntOr("serviceTerminal",50).takeIf { it in listOf(50,40,41,42) } ?: 50 } }
    override fun networkUnserialize(s: DataInputStream): Byte {
        val id=super.networkUnserialize(s)
        try { when(id.toInt()) { 87->s.readInt().takeIf { it in listOf(50,40,41,42) }?.let { terminal=it };88->if(unit.selectCircuit(s.readInt()))reconnect();89->setRemoteDisconnect(s.readBoolean()) } } catch(_: IOException) {}
        needPublish();return id
    }
    override fun networkSerialize(out: DataOutputStream) { syncMeter();super.networkSerialize(out);HouseServiceDisplay().apply { capture(this@HouseServiceElement);write(out) } }
}
