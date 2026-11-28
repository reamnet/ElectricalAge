package mods.eln.gametest.cases

import mods.eln.Eln
import mods.eln.gametest.Reload
import mods.eln.gametest.TestCase
import mods.eln.gametest.TestContext
import mods.eln.gridnode.GridLink
import mods.eln.misc.Coordonate
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.mna.component.VoltageSource
import mods.eln.utility.modular.ModularRecloserElement
import mods.eln.utility.modular.ModularRecloserKind
import kotlin.math.abs

class ModularRecloserCases(private val persistent: Boolean) : TestCase {
    override fun id()=if(persistent)"modular_recloser_persistent_fault_lockout" else "modular_recloser_temporary_fault"
    override fun width()=32
    override fun height()=28
    override fun depth()=16
    override fun floorDepth()=16
    override fun ticks()=125
    private lateinit var yard: ModularRecloserElement
    private lateinit var supplies: List<VoltageSource>
    private lateinit var demand: List<Resistor>

    private fun connect() {
        supplies=yard.inputs.mapIndexed { i,bus -> VoltageSource("yard relay phase $i",bus,null).apply { u=100.0+i*15 } }
        demand=yard.outputs.map { Resistor(it,null).apply { r=20.0 } }
        (supplies+demand).forEach(Eln.simulator::addElectricalComponent)
    }
    private fun disconnect() {
        (supplies+demand).forEach { Eln.simulator.removeElectricalComponent(it);it.breakConnection() }
    }

    override fun build(c: TestContext) {
        val kind=if(persistent)ModularRecloserKind.HEAVY else ModularRecloserKind.SINGLE
        yard=c.placeTransparent(kind.id,c.at(8,1,8)) as ModularRecloserElement
        check(yard.configure(12.0,.15,1.0,2,true))
        check(yard.setClosed(true))
        connect()
        c.after(12) {
            c.check("closed recloser conducts the normal load",demand.all { it.i>4.9 },"")
            demand.last().r=1.0
        }
        c.after(20) {
            c.check("delayed overcurrent trips all phases",!yard.control.closed && demand.all { abs(it.i)<1e-7 } &&
                yard.tripReason==1,"")
            if(!persistent)demand.last().r=20.0
        }
        c.after(41) {
            if(!persistent)c.check("one automatic attempt restores the cleared branch",
                yard.control.closed && !yard.control.locked && yard.control.attempts==1 && demand.last().i>4.9,"")
        }
        if(persistent)c.after(88) {
            c.check("persistent fault locks out after exactly two attempts",!yard.control.closed &&
                yard.control.locked && yard.control.attempts==2 && yard.tripReason==2 &&
                demand.all { abs(it.i)<1e-7 },"")
            c.check("manual and remote close do not bypass a fault lockout",!yard.setClosed(true),"")
            disconnect()
            val at=yard.coordonate().pos()
            Reload.node(c.level(),at)
            yard=GridLink.getElementFromCoordinate(Coordonate(at,c.level())) as ModularRecloserElement
            c.check("world-node reload preserves lockout, retry count and settings",yard.control.locked &&
                yard.control.attempts==2 && yard.control.pickupAmps==12.0 && yard.control.pickupDelay==.15,"")
            connect()
            yard.reset()
        }
    }

    override fun measure(c: TestContext) {
        try {
            c.check("a cleared line carries stable current after recovery",yard.control.closed && !yard.control.locked &&
                demand.all { it.i>4.9 },"")
        } finally { disconnect() }
    }
}
