package mods.eln.gametest.cases

import mods.eln.Eln
import mods.eln.gametest.Reload
import mods.eln.gametest.TestCase
import mods.eln.gametest.TestContext
import mods.eln.gridnode.GridLink
import mods.eln.misc.Coordonate
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.mna.component.VoltageSource
import mods.eln.utility.modular.ModularTransformerElement
import mods.eln.utility.modular.ModularTransformerKind
import kotlin.math.abs

class ModularTransformerCases(private val stepUp: Boolean) : TestCase {
    override fun id() = if(stepUp) "modular_transformer_step_up" else "modular_transformer_230_to_69_with_overload"
    override fun width()=32
    override fun height()=28
    override fun depth()=16
    override fun floorDepth()=16
    override fun ticks()=65
    private lateinit var unit: ModularTransformerElement
    private lateinit var kind: ModularTransformerKind
    private lateinit var sources: List<VoltageSource>
    private lateinit var loads: List<Resistor>
    private var input=0
    private var output=0

    override fun build(c: TestContext) {
        kind=if(stepUp)ModularTransformerKind.DISTRIBUTION else ModularTransformerKind.TRANSMISSION
        unit=c.placeTransparent(kind.id,c.at(10,1,8)) as ModularTransformerElement
        input=if(stepUp)400 else 230000
        output=if(stepUp)3200 else 69000
        check(unit.configure(input,output,3,2,75,110))
        c.check("out-of-range voltage and tap controls are rejected atomically",
            !unit.configure(240000,output,3,9,100,110) && unit.primary==input && unit.tap==2,"")
        sources=unit.inputs.mapIndexed { i,bus->VoltageSource("yard input $i",bus,null).apply { u=input*(1-.10*i) } }
        loads=unit.outputs.mapIndexed { i,bus->Resistor(bus,null).apply { r=1500.0+170*i } }
        (sources+loads).forEach(Eln.simulator::addElectricalComponent)
        c.after(26) {
            val series=output.toDouble()*output/(kind.kva*1000.0/3)*.008
            loads.forEachIndexed { phase,load ->
                val predicted=sources[phase].u*unit.ratio/(load.r+series)
                c.checkValue("phase conversion follows selected ratio and copper loss",predicted, load.i)
                val power=abs(sources[phase].i*sources[phase].u)
                c.checkValue("real electrical input equals delivered power plus winding loss",
                    load.i*load.i*(load.r+series),power)
            }
            loads.forEach { it.r=.12 }
        }
    }

    override fun measure(c: TestContext) {
        try {
            c.check("overload opens every active winding and deenergizes the load",
                unit.tripped && !unit.energized && loads.all { abs(it.i)<1e-6 },"")
        }finally {
            (sources+loads).forEach { Eln.simulator.removeElectricalComponent(it);it.breakConnection() }
        }
        val at=unit.coordonate().pos()
        Reload.node(c.level(),at)
        unit=GridLink.getElementFromCoordinate(Coordonate(at,c.level())) as ModularTransformerElement
        c.check("world-node reload preserves nameplate settings, tap and protection state",
            unit.primary==input && unit.secondary==output && unit.phases==3 && unit.tap==2 && unit.ratingPercent==75 && unit.tripped,"")
    }
}
