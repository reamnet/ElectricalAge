package mods.eln.gametest.cases

import mods.eln.Eln
import mods.eln.gametest.Reload
import mods.eln.gametest.TestCase
import mods.eln.gametest.TestContext
import mods.eln.gridnode.GridLink
import mods.eln.misc.Coordonate
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.mna.component.VoltageSource
import mods.eln.sim.process.destruct.FailureCause
import mods.eln.utility.modular.BusbarElement
import mods.eln.utility.modular.Busbars
import kotlin.math.abs

class ModularBusbarBurnCase : TestCase {
    override fun id()="modular_busbar_melt_opens_rigid_run"
    override fun width()=32
    override fun height()=28
    override fun depth()=16
    override fun ticks()=65
    private lateinit var bar: BusbarElement
    private lateinit var source: VoltageSource
    private lateinit var load: Resistor
    override fun build(c:TestContext) {
        val left=c.placeTransparent(Busbars.FIRST_ID,c.at(5,3,6)) as BusbarElement
        bar=c.placeTransparent(Busbars.FIRST_ID,c.at(6,3,6)) as BusbarElement
        val right=c.placeTransparent(Busbars.FIRST_ID,c.at(7,3,6)) as BusbarElement
        source=VoltageSource("busbar failure fixture",left.load,null).apply { u=48.0 }
        load=Resistor(right.load,null).apply { r=2.0 }
        Eln.simulator.addElectricalComponent(source)
        Eln.simulator.addElectricalComponent(load)
        c.after(15) {
            c.checkValue("intact rigid bar conducts before the thermal failure",24.0,load.i)
            bar.heat.integrateEnergy(bar.heat.physics.fusionJoules*1.3+bar.heat.physics.meltingEnthalpy-bar.heat.storedJoules)
            c.check("supplied enthalpy crosses the material fusion threshold",bar.heat.failed,"phase=${bar.heat.phaseJoules}")
        }
    }
    override fun measure(c:TestContext) {
        try {
            c.check("melted stock fails through the native burn path and opens the circuit",
                bar.burnt==FailureCause.OVERHEAT && bar.node.nodeConnectionList.isEmpty() && abs(load.i)<1e-7,
                "burnt=${bar.burnt}, joints=${bar.node.nodeConnectionList.size}, amps=${load.i}, fusion=${bar.heat.phaseJoules}")
        }finally {
            for(part in listOf(source,load)) { Eln.simulator.removeElectricalComponent(part);part.breakConnection() }
        }
        val pos=bar.coordonate().pos()
        Reload.node(c.level(),pos)
        val fresh=GridLink.getElementFromCoordinate(Coordonate(pos,c.level())) as BusbarElement
        c.check("failed bar remains isolated after reload",fresh.isBurnt && fresh.node.nodeConnectionList.isEmpty(),"")
    }
}
