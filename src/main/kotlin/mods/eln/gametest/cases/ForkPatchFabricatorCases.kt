package mods.eln.gametest.cases

import mods.eln.Eln
import mods.eln.gametest.*
import mods.eln.generic.ItemNbt
import mods.eln.gl.GlFrame
import mods.eln.gui.MenuSpecs
import mods.eln.misc.*
import mods.eln.node.NodeManager
import mods.eln.node.NodeMenus
import mods.eln.node.transparent.*
import mods.eln.sim.IProcess
import mods.eln.sim.mna.component.*
import mods.eln.sixnode.electricalcable.ElectricalCableElement
import mods.eln.transparentnode.fabricator.*
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.phys.*
import java.io.*
import kotlin.math.abs

object ForkPatchFabricatorCases {
    @JvmStatic fun cases(): List<TestCase> = FabricatorOperation.entries.map { Production(it, Direction.XN) } +
        listOf(Direction.XP, Direction.ZN, Direction.ZP).map { Production(FabricatorOperation.TRANSISTOR, it) } +
        listOf(Sag(), Outage(), FailedYield(), ExactCapacity(), Blocked(false), Blocked(true), Guards(), WorldReload(), BreakOwnership(), Automation(), Recipes(), ProductionChain(), YieldCampaign(), PlateReadiness()) +
        listOf(2112, 2116, 2240, 2244, 2304, 2308, 2368, 2372).flatMap { id ->
            listOf(InheritedMachineBudget(id, 1), InheritedMachineBudget(id, -1))
        }
    private fun item(name: String, count: Int = 1) = Eln.findItemStack(name, count)
    private fun packet(value: Int? = null): DataInputStream {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { it.writeByte(0); if (value != null) it.writeInt(value) }
        return DataInputStream(ByteArrayInputStream(bytes.toByteArray()))
    }
    private open class Base : TestCase {
        lateinit var e: FabricatorElement
        val parts = mutableListOf<Component>()
        override fun id() = "forkpatch_fabricator_base"
        override fun build(c: TestContext) {}
        override fun measure(c: TestContext) {}
        fun place(c: TestContext, front: Direction = Direction.XN) {
            val p = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(c.level())
            val yaw = p.yRot; val pitch = p.xRot
            try {
                p.xRot = 0f
                p.yRot = when (front) { Direction.XN -> 270f; Direction.XP -> 90f; Direction.ZN -> 0f; else -> 180f }
                e = c.placeTransparent(4480, c.at(4, 1, 1)) as FabricatorElement
            } finally { p.yRot = yaw; p.xRot = pitch }
        }
        fun stock(operation: FabricatorOperation = FabricatorOperation.TRANSISTOR, count: Int = 1, seed: Long = 0) {
            e.inventory.setItem(1, item("Silicon Wafer", count)); e.inventory.setItem(2, item("Copper Plate", count)); e.inventory.setChanged()
            e.networkUnserialize(packet(operation.ordinal))
            val tag = CompoundTag(); e.writeToNBT(tag); tag.putLong("fabricatorYieldSeed", seed)
            e.readFromNBT(tag); e.prepare()
        }
        fun feed(volts: Double = 200.0, routed: TestContext? = null): VoltageSource {
            val load = if (routed == null) e.power else
                (routed.placeSix(mods.eln.registry.ElnDeviceRegistry.meduimVoltageCableDescriptor.parentItemDamage, routed.at(4, 1, 0)) as ElectricalCableElement).electricalLoad
            return VoltageSource("fabricator supply", load, null).setU(volts).also { parts.add(it); Eln.simulator.addElectricalComponent(it) }
        }
        fun clean() { parts.forEach { Eln.simulator.removeElectricalComponent(it); it.breakConnection() }; parts.clear() }
        fun panel(c: TestContext, animate: Boolean = false) {
            mods.eln.client.ClientRenders.registerAll()
            val pos = e.node.coordonate.pos(); val client = TransparentNodeEntity(pos, c.level().getBlockState(pos)); client.setLevel(c.level())
            try {
                val stream = DataInputStream(ByteArrayInputStream(e.node.publishPacket.toByteArray()))
                stream.readByte(); repeat(4) { stream.readInt() }; stream.readUTF(); client.serverPublishUnserialize(stream)
                val render = client.elementRender as? FabricatorRender
                c.check("fabricator: registered client factory reads complete native publication", render != null && stream.available() == 0, "")
                if (render != null) {
                    c.check("fabricator: slots, counters, paid progress and real electrical meters reach the screen", render.operation == e.operation && render.progressJoules == e.progressJoules && render.attempts == e.attempts && render.successful == e.successful && render.volts == e.power.u && render.stacks.size == 3, "")
                    fun mesh(draw: () -> Unit): Pair<Int, List<Float>> {
                        GlFrame.beginRenderFrame(); val frame = GlFrame.open()
                        try { draw(); return frame.batches().values.sumOf { it.vertices() } to frame.batches().values.flatMap { it.data.take(it.floats) } }
                        finally { GlFrame.close() }
                    }
                    val insulator = Eln.transparentNodeItem.getDescriptor(4609) as mods.eln.transparentnode.wiremachine.WireMachineDescriptor
                    val body = mesh { insulator.draw(0f, 0.0) }.first
                    val full = mesh(render::draw)
                    c.check("fabricator: source enclosure and both etcher parts emit native geometry", full.first >= body + 128, "body=$body full=${full.first}")
                    val phase = render.phase; repeat(3) { mesh(render::draw) }
                    c.check("fabricator: mesh draws cannot advance animation or physical work", render.phase == phase && render.progressJoules == e.progressJoules, "")
                    if (animate) {
                        render.refresh(.1f)
                        c.check("fabricator: accepted running power animates both real etcher axes on the client clock", render.phase != phase && mesh(render::draw).second != full.second, "")
                    }
                }
            } finally { client.clientDestroy() }
            val spec = MenuSpecs.of(e.descriptor); val player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(c.level())
            val menu = NodeMenus.menu(1, player, e.node, -1)
            c.check("fabricator: real native menu retains output0, wafer1 and copper2", spec.slots().size == 3 && spec.slots()[0].kind() == "Output" && menu?.deviceInventory === e.inventory, "")
            menu?.removed(player)
        }
    }
    private class Production(val operation: FabricatorOperation, val front: Direction) : Base() {
        override fun id() = "forkpatch_fabricator_production_${operation.ordinal}_${front.name.lowercase()}"
        override fun ticks() = 100
        override fun build(c: TestContext) { place(c, front); stock(operation); feed(routed = if (front == Direction.XN) c else null) }
        override fun measure(c: TestContext) {
            try {
                val success = FabricationRules.succeeds(0, 0, operation); val out = e.inventory.getItem(0)
                c.check("fabricator: every source operation consumes one actual input pair for one attempt", e.attempts == 1L && e.inventory.getItem(1).isEmpty && e.inventory.getItem(2).isEmpty, "${operation.name}: ${e.attempts}")
                c.check("fabricator: source batch identity/count and deterministic yield are retained", if (success) ItemStack.isSameItemSameComponents(out, FabricationRecipes.output(operation)) && out.count == operation.outputCount else out.isEmpty, "$out")
                c.check("fabricator: success and failure both pay the complete4 kJ budget", e.completedJoules == 4000.0 && e.acceptedJoules + 1e-6 >= 4000.0 && e.successful == if (success) 1L else 0L, "${e.acceptedJoules}")
                c.check("fabricator: source global ZN floor inlet remains fixed across all fronts", e.front == front && Direction.values().all { side -> LRDU.values().all { edge -> (e.getElectricalLoad(side, edge) === e.power) == (side == Direction.ZN && edge == LRDU.Down) } }, "")
                c.check("fabricator: exhausted inputs open the real resistor", !e.running && e.resistor.r >= 1e9, "")
                panel(c)
            } finally { clean() }
        }
    }
    private class Sag : Base() {
        override fun id() = "forkpatch_fabricator_sag_and_stable_cached_lu"
        override fun build(c: TestContext) { place(c); stock(); feed(100.0) }
        override fun measure(c: TestContext) {
            try {
                c.check("fabricator: half voltage accepts quarter power and only partial physical work", e.resistor.p in 249.9..250.1 && e.progressJoules in 400.0..600.0 && e.attempts == 0L && e.inventory.getItem(0).isEmpty, "${e.resistor.p}, ${e.progressJoules}")
                val work = e.progressJoules; val paid = e.acceptedJoules; val system = e.power.subSystem!!; val version = system.matrixVersion
                repeat(2000) { e.prepare() }
                c.check("fabricator: readiness checks cannot create work or rebuild stable native LU", e.progressJoules == work && e.acceptedJoules == paid && system.matrixVersion == version && Batch1Cases.field<Boolean>(system, "matrixValid"), "")
                panel(c, true)
            } finally { clean() }
        }
    }
    private class Outage : Base() {
        lateinit var source: VoltageSource; lateinit var clock: IProcess
        var seconds = 0.0; var stage = 0; var paid = 0.0
        override fun id() = "forkpatch_fabricator_outage_retains_same_job_paid_work"
        override fun ticks() = 180
        override fun build(c: TestContext) {
            place(c); stock(); source = feed(0.0)
            clock = IProcess { dt -> seconds += dt
                if (stage == 0 && seconds >= 1) { c.check("fabricator: unpowered readiness mints neither work nor outputs", e.progressJoules == 0.0 && e.attempts == 0L, ""); source.u = 200.0; stage = 1 }
                else if (stage == 1 && seconds >= 2) { paid = e.progressJoules; source.u = 0.0; stage = 2 }
                else if (stage == 2 && seconds >= 4) { c.check("fabricator: outage preserves the same input's actual paid work", paid > 500 && e.progressJoules >= paid && e.progressJoules <= paid + 51, ""); source.u = 200.0; stage = 3 }
            }; Eln.simulator.addSlowProcess(clock)
        }
        override fun measure(c: TestContext) {
            try { c.check("fabricator: restored real power finishes the one paid job", stage == 3 && e.attempts == 1L && e.inventory.getItem(0).count == 16 && e.acceptedJoules >= 4000.0, "") }
            finally { Eln.simulator.removeSlowProcess(clock); clean() }
        }
    }
    private class FailedYield : Base() {
        lateinit var clock: IProcess; var observed = false
        override fun id() = "forkpatch_fabricator_failed_yield_cannot_reuse_attempt_energy"
        override fun ticks() = 180
        override fun build(c: TestContext) {
            place(c); stock(FabricatorOperation.ALU, 2); feed()
            clock = IProcess {
                if (!observed && e.attempts == 1L) {
                    observed = true
                    c.check("fabricator: first failed yield consumes materials and debits4 kJ once", e.successful == 0L && e.inventory.getItem(0).isEmpty && e.inventory.getItem(1).count == 1 && e.inventory.getItem(2).count == 1 && e.completedJoules == 4000.0 && e.progressJoules < 200.0, "")
                }
            }; Eln.simulator.addSlowProcess(clock)
        }
        override fun measure(c: TestContext) {
            try { c.check("fabricator: next yield needs another complete real4 kJ budget", observed && e.attempts == 2L && e.successful == 1L && e.completedJoules == 8000.0 && e.acceptedJoules >= 8000.0 && e.inventory.getItem(0).count == 2 && e.inventory.getItem(1).isEmpty && e.inventory.getItem(2).isEmpty, "${e.acceptedJoules}") }
            finally { Eln.simulator.removeSlowProcess(clock); clean() }
        }
    }
    private class ExactCapacity : Base() {
        override fun id() = "forkpatch_fabricator_exact_64_item_output_is_legal"
        override fun ticks() = 100
        override fun build(c: TestContext) { place(c); stock(); e.inventory.setItem(0, FabricationRecipes.output(FabricatorOperation.TRANSISTOR).copyWithCount(48)); e.inventory.setChanged(); e.prepare(); feed() }
        override fun measure(c: TestContext) {
            try { c.check("fabricator: a batch may fill output to exactly its64-item capacity", e.inventory.getItem(0).count == 64 && e.attempts == 1L && e.inventory.getItem(1).isEmpty && e.inventory.getItem(2).isEmpty, "") }
            finally { clean() }
        }
    }
    private class Blocked(val metadata: Boolean) : Base() {
        override fun id() = "forkpatch_fabricator_blocked_output_${if (metadata) "components" else "wrong_item"}"
        override fun build(c: TestContext) {
            place(c); stock()
            val out = if (metadata) FabricationRecipes.output(FabricatorOperation.TRANSISTOR) else ItemStack(Items.DIRT)
            if (metadata) ItemNbt.edit(out) { it.putString("program", "owned existing settings") }
            e.inventory.setItem(0, out); e.inventory.setChanged(); e.prepare(); feed()
        }
        override fun measure(c: TestContext) {
            try { c.check("fabricator: incompatible output identity/components preserve inputs and cannot accept work", e.attempts == 0L && e.progressJoules == 0.0 && e.acceptedJoules == 0.0 && e.inventory.getItem(1).count == 1 && e.inventory.getItem(2).count == 1 && e.resistor.r >= 1e9, "") }
            finally { clean() }
        }
    }
    private class Guards : Base() {
        override fun id() = "forkpatch_fabricator_packets_settings_and_invalid_saved_work"
        override fun build(c: TestContext) { place(c); stock(); feed(100.0) }
        override fun measure(c: TestContext) {
            try {
                val work = e.progressJoules; val energy = e.acceptedJoules
                e.networkUnserialize(packet()); e.networkUnserialize(packet(-1)); e.networkUnserialize(packet(Int.MAX_VALUE)); e.networkUnserialize(packet(0))
                val player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(c.level())
                e.readConfigTool(CompoundTag().apply { putInt("operation", 0); putDouble("powerConsumed", 4000.0); putLong("fabricatorYieldSeed", 99); putLong("fabricatorAttempts", 99) }, player)
                c.check("fabricator: invalid/no-op packets and copied paid fields cannot alter the live job", e.operation == FabricatorOperation.TRANSISTOR && e.progressJoules == work && e.acceptedJoules == energy && e.attempts == 0L, "")
                e.networkUnserialize(packet(1))
                c.check("fabricator: a changed output cannot inherit another job's paid work", e.operation == FabricatorOperation.D_FLIP_FLOP && e.progressJoules == 0.0, "")
                val picked = ItemNbt.get(e.dropItemStack)!!
                c.check("fabricator: ordinary picks carry settings without slots, work or yield state", picked.contains("operation") && !picked.contains("inv") && !picked.contains("powerConsumed") && !picked.contains("fabricatorYieldSeed") && !picked.contains("fabricatorAttempts"), "")
                for (invalid in listOf(Double.NaN, Double.POSITIVE_INFINITY, -1.0, 8000.0)) {
                    val tag = CompoundTag(); e.writeToNBT(tag); tag.putDouble("powerConsumed", invalid); tag.putInt("operation", 999)
                    val copy = FabricatorElement(TransparentNode(), e.descriptor); copy.readFromNBT(tag); copy.prepare()
                    c.check("fabricator: invalid saved work/operation fails closed", copy.operation == null && copy.progressJoules == 0.0 && !copy.running, "")
                }
                e.inventory.setItem(1, ItemStack.EMPTY); e.inventory.setChanged()
                val noInput = CompoundTag(); e.writeToNBT(noInput); noInput.putDouble("powerConsumed", 4000.0); e.readFromNBT(noInput); e.prepare()
                c.check("fabricator: stored budget without actual inputs cannot mint an output", e.attempts == 0L && e.inventory.getItem(0).isEmpty && e.progressJoules == 0.0, "")
            } finally { clean() }
        }
    }
    private class WorldReload : Base() {
        override fun id() = "forkpatch_fabricator_actual_owner_reload_retains_work_and_yield_counter"
        override fun build(c: TestContext) { place(c); stock(FabricatorOperation.ALU); feed(100.0) }
        override fun measure(c: TestContext) {
            try {
                val work = e.progressJoules; val energy = e.acceptedJoules; val before = CompoundTag(); e.writeToNBT(before)
                clean(); val pos = e.node.coordonate.pos(); e = (Reload.node(c.level(), pos).fresh as TransparentNode).element as FabricatorElement
                val after = CompoundTag(); e.writeToNBT(after)
                c.check("fabricator: actual owner reload retains partial paid work and deterministic next yield", work > 0 && e.progressJoules == work && e.acceptedJoules == energy && e.operation == FabricatorOperation.ALU && after.getLongOr("fabricatorYieldSeed", -1) == before.getLongOr("fabricatorYieldSeed", -2) && e.attempts == 0L, "")
                e.readItemStackNBT(CompoundTag().apply { putInt("operation", 0); putDouble("powerConsumed", 4000.0) })
                c.check("fabricator: repeated carried reads cannot reset/refill an initialized owner", e.operation == FabricatorOperation.ALU && e.progressJoules == work, "")
                panel(c)
            } finally { clean() }
        }
    }
    private class BreakOwnership : Base() {
        override fun id() = "forkpatch_fabricator_actual_break_place_drops_inputs_once_without_carried_work"
        override fun build(c: TestContext) { place(c); stock(FabricatorOperation.ALU); feed(100.0) }
        override fun measure(c: TestContext) {
            val player = TransportTestPlayer(c.level(), com.mojang.authlib.GameProfile(java.util.UUID.nameUUIDFromBytes(id().toByteArray()), "fabCarrier"))
            try {
                val pos = e.node.coordonate.pos(); val machine = e.descriptor.parentItem; clean(); c.level().destroyBlock(pos, true)
                val drops = c.level().getEntitiesOfClass(ItemEntity::class.java, AABB(pos).inflate(2.0)); val devices = drops.filter { it.item.item === machine }
                c.check("fabricator: real destruction drops one device and both owned inputs", devices.size == 1 && drops.count { FabricationRecipes.wafer(it.item) } == 1 && drops.count { FabricationRecipes.copper(it.item) } == 1 && drops.size == 3, "${drops.size}")
                val physical = devices.single().item; val carried = ItemNbt.get(physical)!!
                c.check("fabricator: device drop carries operation without inventory/work/yield state", carried.getIntOr("operation", -1) == 3 && !carried.contains("inv") && !carried.contains("powerConsumed") && !carried.contains("fabricatorYieldSeed"), "")
                e.onBreakElement(); c.level().destroyBlock(pos, true)
                c.check("fabricator: repeated removal cannot duplicate material drops", c.level().getEntitiesOfClass(ItemEntity::class.java, AABB(pos).inflate(2.0)).size == 3, "")
                drops.forEach { it.discard() }; player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL); player.setItemInHand(InteractionHand.MAIN_HAND, physical)
                val dest = c.at(6, 1, 1); val floor = dest.below(); val hit = BlockHitResult(Vec3(floor.x + .5, floor.y + 1.0, floor.z + .5), net.minecraft.core.Direction.UP, floor, false)
                physical.useOn(net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND, hit))
                val restored = (NodeManager.instance.getNodeFromCoordonate(Coordonate(dest, c.level())) as TransparentNode).element as FabricatorElement
                c.check("fabricator: real survival placement consumes carrier and cannot copy paid work or inputs", player.mainHandItem.isEmpty && restored.operation == FabricatorOperation.ALU && restored.progressJoules == 0.0 && restored.acceptedJoules == 0.0 && restored.attempts == 0L && restored.inventory.isEmpty, "")
            } finally { clean(); player.inventory.clearContent() }
        }
    }
    private class Automation : Base() {
        override fun id() = "forkpatch_fabricator_native_slot_filters_and_transactional_output"
        override fun ticks() = 100
        override fun build(c: TestContext) { place(c); stock(FabricatorOperation.OP_AMP); feed() }
        override fun measure(c: TestContext) {
            try {
                val handler = requireNotNull(c.level().getCapability(net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK, e.node.coordonate.pos(), net.minecraft.core.Direction.UP)); val resource = handler.getResource(0)
                net.neoforged.neoforge.transfer.transaction.Transaction.openRoot().use { tx -> c.check("fabricator: output extraction can be simulated", handler.extract(0, resource, 1, tx) == 1, "") }
                c.check("fabricator: aborted extraction preserves all paid chips", e.inventory.getItem(0).count == 4, "")
                net.neoforged.neoforge.transfer.transaction.Transaction.openRoot().use { tx -> handler.extract(0, resource, 1, tx); tx.commit() }
                c.check("fabricator: committed extraction moves exactly one paid chip", e.inventory.getItem(0).count == 3, "")
                net.neoforged.neoforge.transfer.transaction.Transaction.openRoot().use { tx -> c.check("fabricator: pipes cannot insert through output or use chips as wafers", handler.insert(0, resource, 1, tx) == 0 && handler.insert(1, resource, 1, tx) == 0, "") }
                c.check("fabricator: actual server inventory accepts only the source wafer/plate slots", e.inventory.canInsertItem(1, item("Silicon Wafer"), 0) && e.inventory.canInsertItem(2, item("Copper Plate"), 0) && !e.inventory.canInsertItem(1, item("Silicon Plate"), 0) && !e.inventory.canExtractItem(1, item("Silicon Wafer"), 0), "")
                panel(c)
            } finally { clean() }
        }
    }
    /** Both existing plate recipes and the new Fabricator run from one real floor-cable bus. */
    private class ProductionChain : Base() {
        lateinit var plate: mods.eln.transparentnode.electricalmachine.ElectricalMachineElement
        lateinit var clock: IProcess; lateinit var accounting: IProcess
        var stage = 0; var plateJoules = 0.0
        override fun id() = "forkpatch_fabricator_native_ingot_plate_wafer_chip_automated_chain"
        override fun ticks() = 400
        override fun build(c: TestContext) {
            val player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(c.level())
            val yaw = player.yRot; val pitch = player.xRot
            try { player.yRot = 270f; player.xRot = 0f; plate = c.placeTransparent(2372, c.at(2, 1, 1)) as mods.eln.transparentnode.electricalmachine.ElectricalMachineElement }
            finally { player.yRot = yaw; player.xRot = pitch }
            place(c)
            plate.inventory.setItem(plate.inSlotId, item("Silicon Ingot", 4)); plate.inventory.setChanged()
            e.inventory.setItem(2, item("Copper Plate")); e.inventory.setChanged(); e.prepare()
            c.placeSix(mods.eln.registry.ElnDeviceRegistry.meduimVoltageCableDescriptor.parentItemDamage, c.at(2, 1, 0))
            val wire = c.placeSix(mods.eln.registry.ElnDeviceRegistry.meduimVoltageCableDescriptor.parentItemDamage, c.at(3, 1, 0)) as ElectricalCableElement
            c.placeSix(mods.eln.registry.ElnDeviceRegistry.meduimVoltageCableDescriptor.parentItemDamage, c.at(4, 1, 0))
            parts.add(VoltageSource("fabrication bus", wire.electricalLoad, null).setU(200.0).also { Eln.simulator.addElectricalComponent(it) })
            val plateResistor = plate.electricalComponentList.filterIsInstance<Resistor>().first()
            accounting = IProcess { dt -> if (dt > 0 && plateResistor.p.isFinite() && plateResistor.p > 0) plateJoules += plateResistor.p * dt }
            Eln.simulator.addElectricalProcess(accounting)
            fun transfer(target: mods.eln.node.transparent.TransparentNodeElement, slot: Int): Boolean {
                val from = requireNotNull(c.level().getCapability(net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK, plate.node.coordonate.pos(), net.minecraft.core.Direction.DOWN))
                val to = requireNotNull(c.level().getCapability(net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK, target.node.coordonate.pos(), net.minecraft.core.Direction.UP))
                val resource = from.getResource(0); if (resource.isEmpty) return false
                net.neoforged.neoforge.transfer.transaction.Transaction.openRoot().use { tx ->
                    if (to.insert(slot, resource, 1, tx) != 1 || from.extract(0, resource, 1, tx) != 1) return false
                    tx.commit(); return true
                }
            }
            clock = IProcess {
                if (stage == 0 && !plate.inventory.getItem(0).isEmpty && transfer(plate, 0)) stage = 1
                else if (stage == 1 && !plate.inventory.getItem(0).isEmpty && transfer(e, 1)) stage = 2
            }; Eln.simulator.addSlowProcess(clock)
        }
        override fun measure(c: TestContext) {
            try {
                val output = e.inventory.getItem(0)
                c.check("fabrication chain: four actual silicon ingots pass through native plate and wafer production into16 transistors", stage == 2 && ItemStack.isSameItemSameComponents(output, FabricationRecipes.output(FabricatorOperation.TRANSISTOR)) && output.count == 16 && plate.inventory.isEmpty && e.inventory.getItem(1).isEmpty && e.inventory.getItem(2).isEmpty, "stage=$stage input=${plate.inventory.getItem(plate.inSlotId)} plateOut=${plate.inventory.getItem(0)} output=$output")
                c.check("fabrication chain: original and new plate operations each pay their own10 kJ", plateJoules + 1e-5 >= 20000.0, "$plateJoules")
                c.check("fabrication chain: wafer transfer cannot inherit plate processing work", e.completedJoules == 4000.0 && e.acceptedJoules + 1e-5 >= 4000.0 && e.attempts == 1L, "${e.acceptedJoules}")
            } finally { Eln.simulator.removeSlowProcess(clock); Eln.simulator.removeElectricalProcess(accounting); clean() }
        }
    }
    /** A real input/output campaign survives a queued main-thread owner reload after its first failed yield. */
    private class YieldCampaign : Base() {
        lateinit var player: TransportTestPlayer; lateinit var clock: IProcess; lateinit var accounting: IProcess
        var queued = false; var reloaded = false; var paid = 0.0
        override fun id() = "forkpatch_fabricator_64_paid_yield_attempts_with_actual_owner_reload_and_output_automation"
        override fun ticks() = 1600
        override fun build(c: TestContext) {
            place(c); stock(FabricatorOperation.ALU, 64); feed(400.0)
            player = TransportTestPlayer(c.level(), com.mojang.authlib.GameProfile(java.util.UUID.nameUUIDFromBytes(id().toByteArray()), "fabCampaign"))
            accounting = IProcess { dt -> if (dt > 0 && e.resistor.p.isFinite() && e.resistor.p > 0) paid += e.resistor.p * dt }
            Eln.simulator.addElectricalProcess(accounting)
            clock = IProcess {
                if (!queued && e.attempts == 1L) {
                    queued = true
                    c.level().server.schedule(net.minecraft.server.TickTask(c.level().server.tickCount) {
                        val work = e.progressJoules; val attempts = e.attempts; val pos = e.node.coordonate.pos()
                        clean(); e = (Reload.node(c.level(), pos).fresh as TransparentNode).element as FabricatorElement
                        c.check("fabricator campaign: actual scheduled owner reload retains paid work and consumed-attempt boundary", e.progressJoules == work && e.attempts == attempts && e.operation == FabricatorOperation.ALU, "")
                        reloaded = true; feed(400.0)
                    })
                }
                if (!e.inventory.getItem(0).isEmpty) {
                    val handler = requireNotNull(c.level().getCapability(net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK, e.node.coordonate.pos(), net.minecraft.core.Direction.UP))
                    val resource = handler.getResource(0)
                    net.neoforged.neoforge.transfer.transaction.Transaction.openRoot().use { tx ->
                        if (handler.extract(0, resource, 2, tx) == 2) { tx.commit(); player.inventory.add(resource.toStack().copyWithCount(2)) }
                    }
                }
            }; Eln.simulator.addSlowProcess(clock)
        }
        override fun measure(c: TestContext) {
            try {
                val expected = (0L until 64L).count { FabricationRules.succeeds(0, it, FabricatorOperation.ALU) }
                val chips = (0 until player.inventory.containerSize).sumOf { slot -> player.inventory.getItem(slot).let { if (it.item === item("Arithmetic Logic Unit").item) it.count else 0 } }
                c.check("fabricator campaign: every failed/successful attempt consumes exactly one pair across a real reload", reloaded && e.attempts == 64L && e.successful == expected.toLong() && e.inventory.isEmpty, "attempts=${e.attempts} expected successes=$expected actual=${e.successful}")
                c.check("fabricator campaign: real automation owns exactly the deterministic produced chips", chips == expected * 2, "$chips")
                c.check("fabricator campaign: all64 attempts including failures pay independent real input energy", e.completedJoules == 64 * 4000.0 && e.acceptedJoules + 1e-5 >= e.completedJoules && paid + 1e-5 >= e.completedJoules, "accepted=${e.acceptedJoules} independently measured=$paid")
            } finally { Eln.simulator.removeSlowProcess(clock); Eln.simulator.removeElectricalProcess(accounting); clean(); player.inventory.clearContent() }
        }
    }
    private class PlateReadiness : Base() {
        lateinit var plate: mods.eln.transparentnode.electricalmachine.ElectricalMachineElement
        override fun id() = "forkpatch_fabricator_legacy_plate_machine_readiness_cannot_mint_work"
        override fun build(c: TestContext) {
            plate = c.placeTransparent(2372, c.at(4, 1, 1)) as mods.eln.transparentnode.electricalmachine.ElectricalMachineElement
            plate.inventory.setItem(plate.inSlotId, item("Silicon Plate")); plate.inventory.setChanged()
            val source = VoltageSource("plate readiness supply", plate.electricalLoadList.first(), null).setU(100.0)
            parts.add(source); Eln.simulator.addElectricalComponent(source)
        }
        override fun measure(c: TestContext) {
            try {
                val process = Batch1Cases.field<mods.eln.sim.ElectricalStackMachineProcess>(plate, "slowRefreshProcess")
                val work = process.processState(); val load = plate.electricalLoadList.first(); val system = load.subSystem!!; val version = system.matrixVersion
                repeat(2000) { process.process(.05) }
                c.check("plate machine: repeated readiness cannot pay or finish an inherited native recipe", work > 0 && work < 1 && process.processState() == work && plate.inventory.getItem(0).isEmpty && plate.inventory.getItem(plate.inSlotId).count == 1, "$work -> ${process.processState()}")
                c.check("plate machine: unchanged readiness preserves native LU factors", system.matrixVersion == version && Batch1Cases.field<Boolean>(system, "matrixValid"), "")
            } finally { clean() }
        }
    }
    /** Every voltage tier/polarity of the shared legacy machine owner pays only solved electrical steps. */
    private class InheritedMachineBudget(val lookup: Int, val sign: Int) : Base() {
        lateinit var machine: mods.eln.transparentnode.electricalmachine.ElectricalMachineElement
        lateinit var process: mods.eln.sim.ElectricalStackMachineProcess
        lateinit var recipe: mods.eln.misc.Recipe
        lateinit var meter: IProcess
        var paid = 0.0
        override fun id() = "forkpatch_fabricator_inherited_machine_${lookup}_${if (sign > 0) "positive" else "negative"}_paid_steps"
        override fun build(c: TestContext) {
            machine = c.placeTransparent(lookup, c.at(4, 1, 1)) as mods.eln.transparentnode.electricalmachine.ElectricalMachineElement
            val descriptor = machine.descriptor as mods.eln.transparentnode.electricalmachine.ElectricalMachineDescriptor
            recipe = descriptor.recipe.recipes.first()
            machine.inventory.setItem(machine.inSlotId, recipe.input.copy()); machine.inventory.setChanged()
            process = Batch1Cases.field(machine, "slowRefreshProcess")
            val resistor = machine.electricalComponentList.filterIsInstance<Resistor>().first()
            val nominal = if (lookup % 64 == 0) 50.0 else 200.0
            val source = VoltageSource("inherited machine supply", machine.electricalLoadList.first(), null).setU(sign * nominal * .1)
            parts.add(source); Eln.simulator.addElectricalComponent(source)
            meter = IProcess { dt -> if (dt > 0 && resistor.p.isFinite() && resistor.p > 0) paid += resistor.p * dt }
            Eln.simulator.addElectricalProcess(meter)
        }
        override fun measure(c: TestContext) {
            try {
                val work = process.processState() * recipe.energy
                c.check("native machine: every inherited family/tier/polarity pays partial work from an independent accepted-step meter", work > 0 && work < recipe.energy && abs(work - paid) < 1e-5, "$lookup/$sign work=$work paid=$paid")
                val system = machine.electricalLoadList.first().subSystem!!; val version = system.matrixVersion
                repeat(2000) { process.process(.05) }
                c.check("native machine: repeated readiness cannot advance any inherited recipe or rebuild stable LU", process.processState() * recipe.energy == work && machine.inventory.getItem(machine.inSlotId).count == recipe.input.count && machine.inventory.getItem(0).isEmpty && system.matrixVersion == version && Batch1Cases.field<Boolean>(system, "matrixValid"), "$lookup/$sign")
            } finally { Eln.simulator.removeElectricalProcess(meter); clean() }
        }
    }
    private class Recipes : Base() {
        override fun id() = "forkpatch_fabricator_paid_native_wafer_and_machine_bootstrap_recipes"
        override fun measure(c: TestContext) {
            val wafer = requireNotNull(Eln.plateMachineRecipes.getRecipe(item("Silicon Plate")))
            c.check("fabricator: native wafer route consumes one paid plate and10 kJ", wafer.input.count == 1 && wafer.output.single().item === item("Silicon Wafer").item && wafer.output.single().count == 1 && wafer.energy == 10000.0, "")
            val plate = requireNotNull(Eln.plateMachineRecipes.getRecipe(item("Silicon Ingot", 4)))
            c.check("fabricator: original four-ingot silicon plate route remains unchanged", plate.input.count == 4 && plate.output.single().item === item("Silicon Plate").item && plate.energy == 10000.0, "")
            val chip = net.minecraft.core.registries.BuiltInRegistries.ITEM.getTagOrEmpty(OreDictTags.recipeTag(Eln.dictCheapChip)).first().value()
            val keys = mapOf('I' to ItemStack(Items.IRON_INGOT), 'C' to ItemStack(chip), 'P' to item("Copper Plate"), 'M' to item("Machine Block"), 'E' to item("Electrical Motor"))
            val input = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, listOf("ICI", "PMP", "IEI").flatMap { row -> row.map { keys[it]!!.copy() } })
            val result = c.level().recipeAccess().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, c.level()).map { it.value().assemble(input) }.orElse(ItemStack.EMPTY)
            c.check("fabricator: actual native recipe manager crafts the machine from existing paid parts", result.item === item("Fabricator").item && result.count == 1, "$result")
            val examples = FabricationRecipes.examples()
            c.check("fabricator: all optional JEI examples use executable native input/output predicates", examples.size == 15 && examples.all { FabricationRecipes.wafer(it.inputs[0]) && FabricationRecipes.copper(it.inputs[1]) && FabricationRecipes.canOutput(it.operation, ItemStack.EMPTY) && it.output.count == it.operation.outputCount && it.operation.chancePercent in listOf(50, 100) }, "")
            c.check("fabricator: source basic component identities do not replace any previous item", listOf("Silicon Wafer", "Transistor", "NTC Thermistor", "Nibble Memory Chip", "Arithmetic Logic Unit").all { !item(it).isEmpty }, "")
        }
    }
}
