package mods.eln.gametest.cases

import mods.eln.Eln
import mods.eln.gametest.TestCase
import mods.eln.gametest.TestContext
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.mna.component.VoltageSource
import mods.eln.utility.modular.*
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.TicketType
import net.minecraft.world.level.ChunkPos

/** Both terminals must keep their native joint when Minecraft discards their block entities. */
class ModularRigidChunkCase : TestCase {
    private val pos = BlockPos(1_045_006, 100, 1_045_005)
    private val bay = ChunkPos(pos.x shr 4, pos.z shr 4)
    private lateinit var device: ModularTransformerElement
    private lateinit var source: VoltageSource
    private lateinit var load: Resistor
    private val bars = ArrayList<BusbarElement>()
    private var evictedEntity: net.minecraft.world.level.block.entity.BlockEntity? = null

    override fun id() = "modular_rigid_contacts_survive_chunk_eviction"
    override fun ticks() = 470

    override fun build(c: TestContext) {
        ticket(c, true)
        c.floor(pos.below())
        device = place(c, ModularTransformerKind.DISTRIBUTION.id, pos.below(), Direction.UP) as ModularTransformerElement
        check(device.configure(3200, 230, 3, 1, 75, 110))
        for (terminal in listOf(32,36)) {
            val contact = device.rigidContacts().first { it.terminal == terminal }
            val face = contact.approach.toEnumFacing().opposite
            bars += place(c, Busbars.FIRST_ID + 2, contact.cell.relative(face.opposite), face) as BusbarElement
        }
        source = VoltageSource("rigid bay chunk source", bars[0].load, null).apply { u = 3200.0 }
        load = Resistor(bars[1].load, null).apply { r = 1000.0 }
        Eln.simulator.addElectricalComponent(source)
        Eln.simulator.addElectricalComponent(load)
        c.after(30) {
            powered(c)
            evictedEntity = requireNotNull(c.level().getBlockEntity(pos))
            ticket(c, false)
        }
        c.after(390) {
            c.check("Minecraft removed the original block entity and evicted the bay chunk",
                c.level().chunkSource.getChunkNow(bay.x(), bay.z()) == null && evictedEntity?.isRemoved == true, "")
            powered(c)
            ticket(c, true)
            c.level().getChunk(bay.x(), bay.z())
        }
    }

    override fun measure(c: TestContext) {
        try {
            val fresh = c.level().getBlockEntity(pos)
            c.check("the restored block entity represents the same resident rigid bay",
                fresh != null && fresh !== evictedEntity && c.nodeAt(pos) === device.node && device.tap == 1, "")
            powered(c)
        } finally {
            for (part in listOf(load, source)) {
                Eln.simulator.removeElectricalComponent(part)
                part.breakConnection()
            }
            c.level().removeBlock(pos, false)
            for (bar in bars) c.level().removeBlock(bar.coordonate().pos(), false)
            c.level().removeBlock(pos.below(), false)
            ticket(c, false)
        }
    }

    private fun powered(c: TestContext) {
        c.checkValue("current passes through the 3.2 kV to 230 V rigid transformer bay", load.i, .232875)
        c.check("only the two phase-A rigid joints are present",
            device.node.nodeConnectionList.filterIsInstance<EquipmentJoint>().size == 2 &&
                device.gridLinkList.isEmpty() && device.outputs.drop(1).all { kotlin.math.abs(it.u) < 1e-7 }, "")
    }

    private fun ticket(c: TestContext, enabled: Boolean) {
        if (enabled) c.level().chunkSource.addTicketWithRadius(TicketType.FORCED, bay, 0)
        else c.level().chunkSource.removeTicketWithRadius(TicketType.FORCED, bay, 0)
    }
}
