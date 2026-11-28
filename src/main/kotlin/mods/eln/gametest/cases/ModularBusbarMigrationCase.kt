package mods.eln.gametest.cases

import mods.eln.gametest.Reload
import mods.eln.gametest.TestCase
import mods.eln.gametest.TestContext
import mods.eln.generic.ItemNbt
import mods.eln.node.transparent.TransparentNode
import mods.eln.utility.modular.*
import net.minecraft.core.Direction

class ModularBusbarMigrationCase : TestCase {
    override fun id() = "modular_busbar_migrate_old_phase_groups"
    override fun width() = 18
    override fun depth() = 16
    override fun floorDepth() = depth()
    override fun height() = 14
    override fun ticks() = 5

    override fun build(c: TestContext) {
        val transformer = place(c, ModularTransformerKind.TRANSMISSION.id, c.at(8, 0, 8), Direction.UP) as ModularTransformerElement
        val bars = (32..34).map { terminal ->
            val socket = transformer.rigidContacts().first { it.terminal == terminal }
            val face = socket.approach.toEnumFacing().opposite
            val profile = if (terminal == 33) Busbars.FIRST_ID else Busbars.FIRST_ID + 6
            val original = place(c, profile, socket.cell.relative(face.opposite), face) {
                ItemNbt.edit(it) { tag -> tag.putInt("rigidPhase", terminal - 32) }
            } as BusbarElement
            val restored = Reload.node(c.level(), original.coordonate().pos()) { tag ->
                val element = tag.getCompoundOrEmpty("element")
                element.remove("rigidColor")
                tag.put("element", element)
                tag
            }
            (restored.fresh as TransparentNode).element as BusbarElement
        }
        c.check("old A, B and C sections remain independent when loaded with no color key",
            bars.map { BusbarColor.legacyPhase(it.color) } == listOf(0, 1, 2) &&
                bars.all { bar -> bar.node.nodeConnectionList.none { it.other(bar.node) in bars.map { b -> b.node } } }, "")
        c.check("migration preserves the old rejected transformer contact",
            transformer.node.nodeConnectionList.filterIsInstance<EquipmentJoint>().map { it.terminal }.sorted() == listOf(32, 34), "")
        paint(c, bars[1], "Orange Brush")
        c.check("painting explicitly converts the old small section to a voltage-independent conductor",
            transformer.node.nodeConnectionList.filterIsInstance<EquipmentJoint>().map { it.terminal }.sorted() == listOf(32, 33, 34), "")
        val top = transformer.rigidContacts().single { it.terminal == 33 && it.approach == mods.eln.misc.Direction.YN }
        val alternate = place(c, Busbars.FIRST_ID, top.cell.below(), Direction.UP) as BusbarElement
        paint(c, alternate, "Blue Brush")
        c.check("two different groups on alternate approaches cannot bond through one clamp",
            transformer.node.nodeConnectionList.filterIsInstance<EquipmentJoint>().none { it.terminal == 33 }, "")
        c.level().removeBlock(top.cell, false)
        c.check("removing the conflicting approach restores one exact joint",
            bars[1].node.nodeConnectionList.filterIsInstance<EquipmentJoint>().singleOrNull()?.terminal == 33, "")
    }

    override fun measure(c: TestContext) = Unit
}
