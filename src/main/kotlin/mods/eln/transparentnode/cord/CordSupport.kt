package mods.eln.transparentnode.cord

import mods.eln.item.HeatingCorpElement
import mods.eln.misc.*
import mods.eln.node.NodeBase
import mods.eln.node.six.SixNodeElement
import mods.eln.node.transparent.TransparentNodeElement
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.sixnode.batterycharger.BatteryChargerElement
import mods.eln.sixnode.lampsupply.LampSupplyElement
import mods.eln.transparentnode.fabricator.FabricatorElement
import mods.eln.transparentnode.wiremachine.WireMachineElement
import mods.eln.transparentnode.electricalmachine.*
import mods.eln.transparentnode.electricalfurnace.ElectricalFurnaceElement
import mods.eln.transparentnode.eggincubator.EggIncubatorElement
import mods.eln.transparentnode.thermaldissipatoractive.ThermalDissipatorActiveElement
import mods.eln.transparentnode.electricalantennatx.ElectricalAntennaTxElement
import mods.eln.transparentnode.turret.TurretElement
import mods.eln.transparentnode.autominer.AutoMinerElement
import mods.eln.transparentnode.teleporter.TeleporterElement
import net.minecraft.nbt.CompoundTag

interface CordDevice {
    val cord: ApplianceCord
    fun acceptsCord(rating: CordRating): Boolean
}
class MachineCordDevice(val owner: Any,val input: NbtElectricalLoad,val nominal: ()->Double): CordDevice {
    override val cord by lazy { ApplianceCord(this,owner,input) }
    override fun acceptsCord(rating: CordRating)=rating.accepts(nominal())
}

/** Explicit consumers only; raw inlets remain owned by their native physics, menus and inventories. */
object CordSupport {
    @JvmStatic fun create(owner: Any): MachineCordDevice? {
        if(owner is BatteryChargerElement)return MachineCordDevice(owner,owner.powerLoad) { owner.descriptor.nominalVoltage }
        if(owner is ElectricalMachineElement)return MachineCordDevice(owner,owner.cordInput()) { owner.cordNominalVoltage() }
        // Bind explicit power loads, never whichever load happens to be first in a list.
        when(owner) {
            is AutoMinerElement -> return MachineCordDevice(owner,owner.cordInput()) { owner.cordNominalVoltage() }
            is TeleporterElement -> return MachineCordDevice(owner,owner.cordInput()) { owner.cordNominalVoltage() }
            is EggIncubatorElement -> return MachineCordDevice(owner,owner.cordInput()) { owner.cordNominalVoltage() }
            is ThermalDissipatorActiveElement -> return MachineCordDevice(owner,owner.cordInput()) { owner.cordNominalVoltage() }
            is ElectricalAntennaTxElement -> return MachineCordDevice(owner,owner.cordInput()) { owner.cordNominalVoltage() }
            is TurretElement -> return MachineCordDevice(owner,owner.cordInput()) { owner.cordNominalVoltage() }
        }
        if(owner !is TransparentNodeElement)return null
        val nominal: ()->Double=when(owner) {
            is FabricatorElement -> { { 200.0 } }
            is WireMachineElement -> { { 200.0 } }
            is ElectricalFurnaceElement -> { { (Utils.getItemObject(owner.inventory.getItem(ElectricalFurnaceElement.heatingCorpSlotId)) as? HeatingCorpElement)?.electricalNominalU ?: Double.NaN } }
            else -> return null
        }
        val inlet=owner.electricalLoadList.filterIsInstance<NbtElectricalLoad>().firstOrNull() ?: return null
        return MachineCordDevice(owner,inlet,nominal)
    }
    @JvmStatic fun device(owner: Any): CordDevice?=when(owner) {
        is CordDevice -> owner
        is TransparentNodeElement -> owner.machineCord()
        is SixNodeElement -> owner.machineCord()
        else -> null
    }
    fun coordinate(owner: Any): Coordonate=when(owner) {
        is TransparentNodeElement -> owner.node.coordonate
        is SixNodeElement -> owner.sixNode.coordonate
        else -> error("Unsupported cord owner")
    }
    fun resident(owner: Any): Boolean=when(owner) {
        is TransparentNodeElement -> (mods.eln.node.NodeManager.instance.getNodeFromCoordonate(owner.node.coordonate) as? mods.eln.node.transparent.TransparentNode)?.element===owner
        is SixNodeElement -> (mods.eln.node.NodeManager.instance.getNodeFromCoordonate(owner.sixNode.coordonate) as? mods.eln.node.six.SixNode)?.getElement(owner.slot)===owner
        else -> false
    }
    @JvmStatic fun active(owner: Any)=resident(owner) && when(owner) { is TransparentNodeElement->owner.node.isAdded && !owner.isBurnt;is SixNodeElement->owner.sixNode.isAdded && !owner.isBurnt;else->false }
    fun reconnect(owner: Any) { when(owner) { is TransparentNodeElement->if(owner.node.isAdded && !owner.node.isDestructing)owner.reconnect();is SixNodeElement->if(owner.sixNode.isAdded && !owner.sixNode.isDestructing)owner.reconnect() } }
    fun publish(owner: Any) { when(owner) { is TransparentNodeElement->owner.needPublish();is SixNodeElement->owner.needPublish() } }
    fun processes(owner: Any)=when(owner) { is TransparentNodeElement->owner.slowProcessList;is SixNodeElement->owner.slowProcessList;else->error("Unsupported owner") }
    fun drop(owner: Any,stack: net.minecraft.world.item.ItemStack) { when(owner) { is TransparentNodeElement->owner.node.dropItem(stack);is SixNodeElement->owner.sixNode.dropItem(stack) } }
    @JvmStatic fun beforeConnect(owner: Any) { device(owner)?.cord }
    private fun auxiliaryNodes(owner: Any): List<NodeBase> = when(owner) {
        is AutoMinerElement -> owner.cordPowerNodes()
        is TeleporterElement -> owner.cordPowerNodes()
        else -> emptyList()
    }
    /** Called only after the native owner has completed resume, never by a cord poll. */
    @JvmStatic fun afterConnect(owner: Any) {
        if(active(owner))auxiliaryNodes(owner).forEach { it.reconnect() }
    }
    @JvmStatic fun beforeDisconnect(owner: Any) {
        auxiliaryNodes(owner).forEach { it.disconnect() }
        WirelessOutletPower.disconnectSupply(owner)
        device(owner)?.cord?.disconnect()
    }
    @JvmStatic fun broken(owner: Any) { device(owner)?.cord?.onBroken() }
    @JvmStatic fun read(owner: Any,tag: CompoundTag) { device(owner)?.cord?.read(tag) }
    @JvmStatic fun write(owner: Any,tag: CompoundTag) { device(owner)?.cord?.write(tag) }
    @JvmStatic fun blocks(owner: Any,load: ElectricalLoad?): Boolean {
        val d=device(owner) ?: return false
        return load!=null && d.cord.plugged && load===d.cord.input
    }
    @JvmStatic fun load(owner: Any,load: ElectricalLoad?): ElectricalLoad?=if(blocks(owner,load))null else load
    @JvmStatic fun mask(owner: Any,load: ElectricalLoad?,mask: Int)=if(blocks(owner,load))mask and NodeBase.maskElectricalPower.inv() else mask
}
