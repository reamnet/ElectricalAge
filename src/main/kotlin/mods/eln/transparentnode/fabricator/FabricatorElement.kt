package mods.eln.transparentnode.fabricator

import mods.eln.i18n.I18N.tr
import mods.eln.misc.*
import mods.eln.node.NodeBase
import mods.eln.node.NodePeriodicPublishProcess
import mods.eln.node.transparent.*
import mods.eln.sim.IProcess
import mods.eln.sim.ThermalLoad
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.nbt.NbtElectricalLoad
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.Container
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import java.io.*
import java.util.UUID

/** Native silicon/copper processing; one real input pair and one paid energy budget per yield attempt. */
class FabricatorElement(node: TransparentNode, descriptor: TransparentNodeDescriptor) : TransparentNodeElement(node, descriptor) {
    val power = NbtElectricalLoad("load")
    val resistor = Resistor(power, null)
    var operation: FabricatorOperation? = FabricatorOperation.TRANSISTOR; private set
    var progressJoules = 0.0; private set
    var acceptedJoules = 0.0; private set
    var completedJoules = 0.0; private set
    var attempts = 0L; private set
    var successful = 0L; private set
    var running = false; private set
    private var seed = UUID.randomUUID().leastSignificantBits
    private var internalChange = false
    private var settingsRead = false
    private var breakHandled = false
    private var readyDirty = true
    private var readyCache = false
    private val items = object : TransparentNodeElementInventory(3, 64, this) {
        override fun getAccessibleSlotsFromSide(side: Int) = intArrayOf(0, 1, 2)
        override fun canInsertItem(slot: Int, stack: ItemStack, side: Int) = FabricationRecipes.accepts(slot, stack)
        override fun canExtractItem(slot: Int, stack: ItemStack, side: Int) = slot == 0
    }

    init {
        power.rs = 1e-6
        electricalLoadList.add(power)
        electricalComponentList.add(resistor)
        electricalProcessList.add(IProcess { seconds ->
            val watts = resistor.p
            if (running && seconds.isFinite() && seconds > 0 && watts.isFinite() && watts > 0) {
                acceptedJoules += watts * seconds
                progressJoules += FabricationRules.acceptedWork(progressJoules, watts, seconds)
            }
        })
        slowProcessList.add(IProcess { prepare() })
        slowProcessList.add(NodePeriodicPublishProcess(node, .5, 0.0))
    }

    override fun getInventory(): TransparentNodeElementInventory = items
    override fun hasGui() = true
    override fun initialize() { settingsRead = true; prepare(); connect() }
    override fun onBlockActivated(player: Player, side: Direction, x: Float, y: Float, z: Float) = false
    override fun getElectricalLoad(side: Direction, lrdu: LRDU) = if (side == Direction.ZN && lrdu == LRDU.Down) power else null
    override fun getConnectionMask(side: Direction, lrdu: LRDU) = if (getElectricalLoad(side, lrdu) != null) NodeBase.maskElectricalPower else 0
    override fun getThermalLoad(side: Direction, lrdu: LRDU): ThermalLoad? = null
    override fun multiMeterString(side: Direction) = Utils.plotUIP(power.u, resistor.current)
    override fun thermoMeterString(side: Direction) = ""

    override fun inventoryChange(inventory: Container?) {
        readyDirty = true
        if (!internalChange) {
            progressJoules = 0.0
            running = false
            resistor.highImpedance()
        }
        needPublish()
    }

    private fun ready(): Boolean {
        if (readyDirty) {
            val selected = operation
            readyCache = selected != null && attempts < Long.MAX_VALUE &&
                FabricationRecipes.wafer(items.getItem(1)) && FabricationRecipes.copper(items.getItem(2)) &&
                FabricationRecipes.canOutput(selected, items.getItem(0))
            readyDirty = false
        }
        return readyCache
    }

    /** Readiness never advances work; only an accepted electrical step pays the attempt. */
    fun prepare() {
        val selected = operation
        if (!ready() || selected == null) {
            running = false
            resistor.highImpedance()
            progressJoules = 0.0
            return
        }
        running = true
        resistor.r = FabricationRules.RESISTANCE
        if (progressJoules + 1e-9 < FabricationRules.ATTEMPT_JOULES) return
        // Consume and debit before publishing the outcome, including failed yields.
        val success = FabricationRules.succeeds(seed, attempts, selected)
        internalChange = true
        try {
            items.removeItem(1, 1)
            items.removeItem(2, 1)
            progressJoules = 0.0
            attempts++
            completedJoules += FabricationRules.ATTEMPT_JOULES
            if (success) {
                val old = items.getItem(0)
                if (old.isEmpty) items.setItem(0, FabricationRecipes.output(selected))
                else old.grow(selected.outputCount)
                successful++
            }
            items.setChanged()
        } finally { internalChange = false }
        readyDirty = true
        running = ready()
        resistor.r = if (running) FabricationRules.RESISTANCE else 1e12
        needPublish()
    }

    private fun select(next: FabricatorOperation?) {
        if (operation == next) return
        operation = next
        inventoryChange(items)
    }
    override fun networkUnserialize(stream: DataInputStream): Byte {
        val id = super.networkUnserialize(stream)
        if (id.toInt() != 0) return id
        try { FabricatorOperation.entries.getOrNull(stream.readInt())?.let(::select) } catch (_: IOException) {}
        return unserializeNulldId
    }
    override fun onBreakElement() {
        if (breakHandled) return
        breakHandled = true
        super.onBreakElement()
    }
    override fun getItemStackNBT() = CompoundTag().apply { putInt("operation", operation?.ordinal ?: -1) }
    override fun readItemStackNBT(tag: CompoundTag?) {
        if (tag == null || settingsRead) return
        operation = FabricatorOperation.entries.getOrNull(tag.getIntOr("operation", 0))
        settingsRead = true
        progressJoules = 0.0
        readyDirty = true
    }
    fun writeConfigTool(tag: CompoundTag, player: Player) { tag.merge(itemStackNBT) }
    fun readConfigTool(tag: CompoundTag, player: Player) {
        if (!tag.contains("operation")) return
        FabricatorOperation.entries.getOrNull(tag.getIntOr("operation", -1))?.let(::select)
    }
    override fun readFromNBT(tag: CompoundTag) {
        super.readFromNBT(tag)
        settingsRead = true
        operation = FabricatorOperation.entries.getOrNull(tag.getIntOr("operation", 0))
        progressJoules = positive(tag, "powerConsumed").takeIf { it <= FabricationRules.ATTEMPT_JOULES } ?: 0.0
        acceptedJoules = positive(tag, "fabricatorAcceptedJoules")
        completedJoules = positive(tag, "fabricatorCompletedJoules")
        attempts = tag.getLongOr("fabricatorAttempts", 0).coerceAtLeast(0)
        successful = tag.getLongOr("fabricatorSuccessful", 0).coerceIn(0, attempts)
        if (tag.contains("fabricatorYieldSeed")) seed = tag.getLongOr("fabricatorYieldSeed", 0)
        readyDirty = true
    }
    override fun writeToNBT(tag: CompoundTag) {
        super.writeToNBT(tag)
        tag.putInt("operation", operation?.ordinal ?: -1)
        tag.putDouble("powerConsumed", progressJoules)
        tag.putDouble("fabricatorAcceptedJoules", acceptedJoules)
        tag.putDouble("fabricatorCompletedJoules", completedJoules)
        tag.putLong("fabricatorAttempts", attempts)
        tag.putLong("fabricatorSuccessful", successful)
        tag.putLong("fabricatorYieldSeed", seed)
    }
    private fun positive(tag: CompoundTag, key: String) = tag.getDoubleOr(key, 0.0).takeIf { it.isFinite() && it >= 0 } ?: 0.0
    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream)
        stream.writeInt(operation?.ordinal ?: -1)
        stream.writeDouble(progressJoules)
        stream.writeBoolean(running)
        stream.writeLong(attempts)
        stream.writeLong(successful)
        stream.writeDouble(acceptedJoules)
        stream.writeDouble(power.u)
        stream.writeDouble(resistor.p)
        repeat(3) { Utils.serialiseItemStack(stream, items.getItem(it)) }
    }
    override fun getWaila() = linkedMapOf(
        tr("Operation") to (operation?.let { FabricationRecipes.output(it).hoverName.string } ?: tr("None")),
        tr("Work") to tr("%1$ / %2$ J", progressJoules, FabricationRules.ATTEMPT_JOULES),
        tr("Attempts") to attempts.toString())
}
