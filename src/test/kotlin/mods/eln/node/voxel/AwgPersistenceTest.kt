package mods.eln.node.voxel

import mods.eln.Eln
import mods.eln.cable.CableRenderDescriptor
import mods.eln.node.NodeManager
import mods.eln.sim.mna.state.VoltageState
import mods.eln.sixnode.electricalcable.UtilityCableDescriptor
import mods.eln.sixnode.electricalcable.UtilityCableMaterial.COPPER
import net.minecraft.nbt.CompoundTag
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AwgPersistenceTest {
    @Test fun corruptRunsPreserveBothPaidCableAndOtherPartsAcrossRepeatedSaves() {
        val previousManager = NodeManager.instance
        val id = 32001
        val previousWire = Eln.sixNodeItem.getDescriptor(id)
        val wire = UtilityCableDescriptor("12 AWG", COPPER, 3.309, 20.0, true, 600.0, false, 105.0,
            "AWG persistence fixture", CableRenderDescriptor("eln", "sprites/cable.png", 1.25f, .95f))
            .apply { parentItemDamage = id }
        Eln.sixNodeItem.add(id, wire)
        NodeManager.registerUuid("v", VoxelNode::class.java)
        try {
            val run = AwgCableRun.create(wire, AwgWireLength(id, 16, true), VoltageState(), VoltageState(), 35.0).save()
            val good = CompoundTag().apply {
                putString("kind", "awg_cable")
                putInt("id", 1)
                putIntArray("ends", intArrayOf(0, 8, 16, 8))
                put("run", run)
            }
            assertNotNull(VoxelPart.load(good))
            val written = CompoundTag()
            VoxelPart.load(good)!!.write(written)
            assertEquals(run, written.getCompoundOrEmpty("run"))
            val mutations: List<(CompoundTag) -> Unit> = listOf(
                { it.getCompoundOrEmpty("run").remove("version") },
                { it.getCompoundOrEmpty("run").putDouble("joules", Double.NaN) },
                { it.getCompoundOrEmpty("run").getCompoundOrEmpty("length").putInt("wire", -1) },
                { it.getCompoundOrEmpty("run").getCompoundOrEmpty("length").putInt("routeVoxels", 15) },
                { it.putIntArray("ends", intArrayOf(0, 0, 0, 0)) }
            )
            for (mutate in mutations) {
                val bad = good.copy().also(mutate)
                assertNull(VoxelPart.load(bad))
                val original = CompoundTag().apply {
                    putString("tag", "v")
                    put("node", CompoundTag().apply { putInt("cy", 64) })
                    putInt("parts", 2)
                    put("p0", good.copy())
                    put("p1", bad)
                }
                var nodes = CompoundTag().apply { put("n0", original) }
                repeat(2) {
                    val manager = NodeManager("test")
                    manager.loadFromNbt(nodes, 0)
                    assertTrue(manager.nodes.isEmpty())
                    assertEquals(1, manager.getOrphans(0).size)
                    val saved = CompoundTag()
                    manager.saveToNbt(saved, 0)
                    assertEquals(nodes, saved)
                    nodes = saved
                }
            }
        } finally {
            NodeManager.instance = previousManager
            Eln.sixNodeItem.add(id, previousWire)
        }
    }
}
