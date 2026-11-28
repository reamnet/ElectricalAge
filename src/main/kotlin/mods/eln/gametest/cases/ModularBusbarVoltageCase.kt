package mods.eln.gametest.cases

import mods.eln.Eln
import mods.eln.gametest.TestCase
import mods.eln.gametest.TestContext
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.mna.component.VoltageSource
import mods.eln.utility.insulator.LineInsulatorElement
import mods.eln.utility.modular.BusbarElement
import mods.eln.utility.modular.Busbars
import net.minecraft.core.Direction

class ModularBusbarVoltageCase : TestCase {
    override fun id() = "modular_same_busbar_four_voltage_systems"
    override fun width() = 21
    override fun depth() = 17
    override fun floorDepth() = depth()
    override fun height() = 8
    override fun ticks() = 90

    private val volts = listOf(230.0,12_500.0,69_000.0,230_000.0)
    private val feeds = listOf(8044,8048,8050,8052)
    private val sources = ArrayList<VoltageSource>()
    private val bars = ArrayList<BusbarElement>()
    private val loads = ArrayList<Resistor>()

    override fun build(c: TestContext) {
        for ((i, voltage) in volts.withIndex()) {
            val base = c.at(8,0,2+i*4)
            val near = place(c, Busbars.FIRST_ID, base, Direction.UP) as BusbarElement
            val far = place(c, Busbars.FIRST_ID, near.coordonate().pos(), Direction.EAST) as BusbarElement
            val feed = place(c, feeds[i], near.coordonate().pos(), Direction.WEST) as LineInsulatorElement
            c.check("the voltage-rated feed terminates on the same small copper profile", near.node.nodeConnectionList.size == 2, "")
            bars += far
            sources += VoltageSource("rigid feed $voltage V", feed.bus, null).apply { u = voltage }
            loads += Resistor(far.load, null).apply { r = voltage / 3.0 }
        }
        sources.forEach(Eln.simulator::addElectricalComponent)
        loads.forEach(Eln.simulator::addElectricalComponent)
        c.after(30) {
            volts.forEachIndexed { i, voltage ->
                c.checkValue("busbar follows its $voltage V network", bars[i].load.u, voltage)
                c.checkValue("busbar carries the same loaded current", loads[i].i, 3.0)
            }
            val baselineHeat = bars.first().heat.storedJoules
            loads.first().r = .2
            c.after(40) {
                c.check("overload produces Joule heat with no voltage-based trip",
                    loads.first().i > bars.first().bar.ratedAmps && bars.first().heat.storedJoules > baselineHeat && !bars.first().isBurnt, "")
            }
        }
    }

    override fun measure(c: TestContext) {
        (sources + loads).forEach { Eln.simulator.removeElectricalComponent(it); it.breakConnection() }
    }
}
