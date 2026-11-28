package mods.eln.gametest.cases

import mods.eln.Eln
import mods.eln.gametest.Reload
import mods.eln.gametest.TestCase
import mods.eln.gametest.TestContext
import mods.eln.node.transparent.TransparentNode
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.mna.component.VoltageSource
import mods.eln.utility.modular.*
import net.minecraft.core.Direction
import kotlin.math.abs

class ModularRigidEquipmentCase(private val variant: Int) : TestCase {
    private val ids = ModularDisconnectKind.entries.map { it.id } + ModularTransformerKind.entries.map { it.id } +
        ModularRecloserKind.entries.map { it.id }
    override fun id() = "modular_rigid_terminal_placement_${ids[variant]}"
    override fun width() = 18
    override fun depth() = 18
    override fun height() = 18
    override fun floorDepth() = 18
    override fun ticks() = 55

    private lateinit var device: RigidEquipment
    private val sources = ArrayList<VoltageSource>()
    private val loads = ArrayList<Resistor>()
    private val voltage = listOf(125.0, 185.0, 245.0)
    private lateinit var inputs: List<BusbarElement>
    private lateinit var outputs: List<BusbarElement>

    override fun build(c: TestContext) {
        val p = c.player()
        val yaw = p.yRot
        p.yRot = (variant % 4) * 90f
        try {
            device = place(c, ids[variant], c.at(8, 0, 8), Direction.UP) as RigidEquipment
        } finally { p.yRot = yaw }
        val size = Busbars.FIRST_ID + 6
        fun bank(first: Int): List<BusbarElement> = List(device.terms.phases) { phase ->
            val choices = device.rigidContacts().filter { it.terminal == first + phase }
            val contact = choices[variant % choices.size]
            val face = contact.approach.toEnumFacing().opposite
            val bar = place(c, size + (phase % 2), contact.cell.relative(face.opposite), face, phase) as BusbarElement
            val joint = bar.node.nodeConnectionList.filterIsInstance<EquipmentJoint>().single()
            c.check("phase $phase has a native electrical joint at the visible clamp",
                joint.electrical().joins(bar.load, device.gridLoads(contact.terminal).single()) &&
                    joint.approach == contact.approach && joint.point == contact.point, "")
            bar
        }
        inputs = bank(32)
        outputs = bank(36)
        for (i in inputs.indices) {
            sources += VoltageSource("rigid phase $i", inputs[i].load, null).apply { u = voltage[i] }
            loads += Resistor(outputs[i].load, null).apply { r = 1000.0 }
        }
        sources.forEach(Eln.simulator::addElectricalComponent)
        loads.forEach(Eln.simulator::addElectricalComponent)
        when (val stage = device) {
            is ModularDisconnectElement -> check(stage.setClosed(true))
            is ModularRecloserElement -> check(stage.setClosed(true))
        }
        c.after(20) {
            val ratio = (device as? ModularTransformerElement)?.ratio ?: 1.0
            loads.forEachIndexed { i, load ->
                c.checkValue("phase $i passes only its own source through the two busbar banks", voltage[i] * ratio / 1000, load.i)
            }
            when (val stage = device) {
                is ModularDisconnectElement -> check(stage.setClosed(false))
                is ModularRecloserElement -> check(stage.setClosed(false))
                is ModularTransformerElement -> check(stage.toggle())
            }
        }
        c.after(32) {
            c.check("opening equipment interrupts current on every rigid terminal", loads.all { abs(it.i) < 1e-7 }, "")
        }
    }

    override fun measure(c: TestContext) {
        try {
            val at = device.coordonate().pos()
            val expected = device.terms.phases * 2
            Reload.node(c.level(), at)
            val fresh = (c.nodeAt(at) as TransparentNode).element as RigidEquipment
            c.check("reload rebuilds exactly $expected native joints",
                fresh.node.nodeConnectionList.filterIsInstance<EquipmentJoint>().size == expected &&
                    fresh.gridLinkList.isEmpty(), "")
            val bar = outputs.first()
            val cell = bar.coordonate().pos()
            val contact = fresh.rigidContacts().single { it.cell == cell }
            c.level().destroyBlock(cell, false)
            c.check("breaking a rigid section removes its electrical joint",
                fresh.node.nodeConnectionList.filterIsInstance<EquipmentJoint>().size == expected - 1, "")
            val face = contact.approach.toEnumFacing().opposite
            place(c, Busbars.FIRST_ID + 7, cell.relative(face.opposite), face)
            c.check("placing a replacement creates one new connection",
                fresh.node.nodeConnectionList.filterIsInstance<EquipmentJoint>().size == expected, "")
        } finally {
            (sources + loads).forEach { Eln.simulator.removeElectricalComponent(it); it.breakConnection() }
        }
    }

    companion object {
        @JvmStatic fun cases(): List<TestCase> = (0 until 10).map(::ModularRigidEquipmentCase)
    }
}
