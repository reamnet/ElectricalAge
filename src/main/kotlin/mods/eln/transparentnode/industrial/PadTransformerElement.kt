package mods.eln.transparentnode.industrial

import mods.eln.Eln
import mods.eln.i18n.I18N.tr
import mods.eln.misc.*
import mods.eln.node.*
import mods.eln.node.transparent.*
import mods.eln.sim.nbt.*
import mods.eln.utility.PoleHardware
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import net.minecraft.world.Container
import java.io.*

class PadTransformerElement(node: TransparentNode,descriptor: TransparentNodeDescriptor): TransparentNodeElement(node,descriptor) {
    val hardware=object: TransparentNodeElementInventory(2,1,this) {
        override fun getAccessibleSlotsFromSide(side: Int)=intArrayOf(0,1)
        override fun canInsertItem(slot: Int,stack: ItemStack?,side: Int)=accepts(slot,stack)
        override fun canExtractItem(slot: Int,stack: ItemStack?,side: Int)=true
        override fun setInventorySlotContents(slot: Int,stack: ItemStack) { super.setInventorySlotContents(slot,stack);markDirty() }
    }
    override fun getInventory()=hardware
    val tripSignal=NbtElectricalGateOutput("industrialTrip")
    val tripSource=NbtElectricalGateOutputProcess("industrialTrip",tripSignal)
    val unit=PadTransformerUnit(electricalLoadList,electricalComponentList,electricalProcessList,{
        hardware.setItem(1,Eln.findItemStack(PoleHardware.spentName,1))
        play(mods.eln.sound.SoundCommand("eln:utility_fuse_blow").smallRange().mulVolume(.5));needPublish()
    },{tripSource.state(it)})
    init { electricalLoadList.add(tripSignal);electricalComponentList.add(tripSource);slowProcessList.add(NodePeriodicPublishProcess(node,.5,0.0)) }
    fun accepts(slot: Int,stack: ItemStack?)=if(slot==0)PoleHardware.transformerWatts(stack)!=null else slot==1 && PoleHardware.fuse(stack)?.spent==false
    private fun install() { val fuse=PoleHardware.fuse(hardware.getItem(1));unit.install(PoleHardware.transformerWatts(hardware.getItem(0)),fuse?.takeUnless { it.spent }?.amps) }
    override fun initialize() { install();connect() }
    override fun inventoryChange(inventory: Container?) { install();needPublish() }
    override fun getElectricalLoad(side: Direction,lrdu: LRDU)=when {
        side==Direction.YN->when(lrdu) { LRDU.Down->unit.input;LRDU.Up->unit.output;LRDU.Left->unit.service.loads[0];LRDU.Right->unit.service.loads[1] }
        side==front && lrdu==LRDU.Up->tripSignal
        side==front.left() && lrdu==LRDU.Down->unit.input
        side==front.right() && lrdu==LRDU.Down->unit.output
        else->null
    }
    override fun getConnectionMask(side: Direction,lrdu: LRDU)=when(getElectricalLoad(side,lrdu)) { null->0;tripSignal->NodeBase.maskElectricalOutputGate;else->NodeBase.maskElectricalPower }
    override fun getThermalLoad(side: Direction,lrdu: LRDU)=null
    override fun hasGui()=true
    override fun onBlockActivated(player: net.minecraft.world.entity.player.Player,side: Direction,vx: Float,vy: Float,vz: Float)=false
    override fun thermoMeterString(side: Direction): String?=null
    override fun multiMeterString(side: Direction)=tr("Primary: %1$; secondary: %2$",Utils.plotAmpere("",unit.primaryAmps),Utils.plotAmpere("",unit.secondaryAmps))
    override fun getWaila()=linkedMapOf(tr("Status") to padStatusText(unit.status),tr("Primary current") to Utils.plotAmpere("",unit.primaryAmps),tr("Transformer rating") to Utils.plotPower("",unit.ratedWatts.toDouble()))
    override fun readFromNBT(t: CompoundTag) { super.readFromNBT(t);unit.read(t);install() }
    override fun writeToNBT(t: CompoundTag) { super.writeToNBT(t);unit.write(t) }
    override fun getItemStackNBT()=CompoundTag().also(unit::write)
    override fun readItemStackNBT(t: CompoundTag?) { if(t!=null)unit.read(t) }
    override fun networkUnserialize(s: DataInputStream): Byte { val id=super.networkUnserialize(s);if(unit.packet(id,s))needPublish();return id }
    override fun networkSerialize(out: DataOutputStream) { super.networkSerialize(out);PadTransformerDisplay().apply { capture(unit);write(out) } }
}
