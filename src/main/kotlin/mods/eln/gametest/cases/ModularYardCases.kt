package mods.eln.gametest.cases

import mods.eln.Eln
import mods.eln.gametest.TestCase
import mods.eln.gametest.TestContext
import mods.eln.gridnode.GridElement
import mods.eln.gridnode.GridLink
import mods.eln.gridnode.OverheadCableItem
import mods.eln.misc.Direction
import mods.eln.node.transparent.TransparentNodeItem
import mods.eln.sim.mna.component.Component
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.mna.component.VoltageSource
import mods.eln.utility.DistributionKind
import mods.eln.utility.DistributionPoles
import mods.eln.utility.TransmissionKind
import mods.eln.utility.modular.*
import kotlin.math.abs

object ModularYardCases {
    @JvmStatic fun cases(): List<TestCase> = listOf(Yard(0),Yard(1),Yard(2),DoubleBus(),Yard(0,1),Yard(0,2))

    private abstract class Fixture : TestCase {
        override fun width() = 32
        override fun height() = 28
        override fun depth() = 32
        override fun floorDepth() = 32
        override fun ticks() = 135
        protected val components=ArrayList<Component>()
        fun put(c: TestContext,id: Int,x: Int,y: Int,z: Int) = c.placeTransparent(id,c.at(x,y,z)) as GridElement
        fun join(a: GridElement,b: GridElement,from: Int,to: Int) {
            check(GridLink.addBareLink(a,b,from,to,OverheadCableItem.make(10,128))>0)
        }
        fun source(bus: mods.eln.sim.ElectricalLoad,volts: Double) {
            val component=VoltageSource("yard source",bus,null).apply { u=volts }
            components.add(component)
            Eln.simulator.addElectricalComponent(component)
        }
        fun consume(bus: mods.eln.sim.ElectricalLoad,resistance: Double): Resistor {
            val load=Resistor(bus,null).apply { r=resistance }
            components.add(load)
            Eln.simulator.addElectricalComponent(load)
            return load
        }
        override fun measure(c: TestContext) {
            for(part in components) { Eln.simulator.removeElectricalComponent(part);part.breakConnection() }
            components.clear()
        }
    }

    private class Yard(val tier: Int,val fault: Int=0) : Fixture() {
        override fun id()=when (fault) {
            1->"modular_yard_cleared_distribution_fault"
            2->"modular_yard_persistent_distribution_fault"
            else->listOf("modular_yard_distribution","modular_yard_69kv_distribution","modular_yard_230kv_to_69kv")[tier]
        }
        private lateinit var load: Resistor
        private lateinit var protection: ModularRecloserElement
        private lateinit var transformer: ModularTransformerElement
        private var out=0.0
        override fun build(c: TestContext) {
            val input=listOf(12500.0,69000.0,230000.0)[tier]
            out=listOf(400.0,12500.0,69000.0)[tier]
            val poleId=when(tier) {
                0->DistributionPoles.id(DistributionKind.THREE)
                1->TransmissionKind.POLE_69.id
                else->TransmissionKind.POLE_230.id
            }
            val tower=put(c,poleId,6,1,8)
            val rigid=put(c,Busbars.FIRST_ID+(tier+1)*2,13,10,8)
            val type=Eln.transparentNodeItem.getDescriptor(listOf(8048,8050,8052)[tier])!!
            val piece=type.newItemStack()
            val feed=(piece.item as TransparentNodeItem).placeAt(piece,c.player(),c.level(),c.at(12,10,8),Direction.XP)!!.element as GridElement
            check(rigid.node.nodeConnectionList.size==1)
            val knife=put(c,listOf(ModularDisconnectKind.SMALL,ModularDisconnectKind.GANG,
                ModularDisconnectKind.TRANSMISSION)[tier].id,19,1,8) as ModularDisconnectElement
            transformer=put(c,ModularTransformerKind.entries[tier].id,26,1,8) as ModularTransformerElement
            protection=put(c,ModularRecloserKind.entries[tier].id,26,1,21) as ModularRecloserElement
            val outgoing=put(c,Busbars.FIRST_ID+2*tier,19,3,21) as BusbarElement
            join(tower,feed,32,32)
            join(rigid,knife,32,32)
            join(knife,transformer,36,32)
            join(transformer,protection,36,32)
            join(protection,outgoing,36,32)
            check(knife.setClosed(true))
            check(protection.configure(if(fault==0)100.0 else 4.0,.15,1.0,2,true))
            check(protection.setClosed(true))
            source(tower.gridLoads(32).single(),input)
            load=consume(outgoing.load,10000.0)
            c.after(22) {
                c.checkValue("tower through a mounted feed, busbar, disconnect, transformer and recloser delivers rated voltage",out,outgoing.load.u)
                c.check("other transformer windings are isolated",transformer.outputs.drop(1).all { abs(it.u)<1e-7 },"")
                if(fault>0)load.r=50.0
            }
            if(fault>0)c.after(32) {
                c.check("substation recloser trips on downstream fault before thermal protection",!protection.control.closed &&
                    !transformer.tripped && abs(load.i)<1e-7,"")
                if(fault==1)load.r=10000.0
            }
        }
        override fun measure(c: TestContext) {
            try {
                when(fault) {
                    0->c.checkValue("operating yard supplies its external load",out/10000.0,load.i)
                    1->c.check("cleared distribution fault restores powered service on the first attempt",
                        protection.control.closed && protection.control.attempts==1 && load.i>0,"")
                    2->c.check("persistent distribution fault leaves the outgoing bus locked out",
                        protection.control.locked && protection.control.attempts==2 && abs(load.i)<1e-7 && !transformer.tripped,"")
                }
            } finally { super.measure(c) }
        }
    }

    private class DoubleBus : Fixture() {
        override fun id()="modular_yard_double_bus_transfer"
        private lateinit var load: Resistor
        override fun build(c: TestContext) {
            val a=put(c,Busbars.FIRST_ID,4,2,10) as BusbarElement
            val b=put(c,Busbars.FIRST_ID,4,2,20) as BusbarElement
            val upper=put(c,ModularDisconnectKind.SMALL.id,12,1,10) as ModularDisconnectElement
            val lower=put(c,ModularDisconnectKind.SMALL.id,12,1,20) as ModularDisconnectElement
            val common=put(c,Busbars.FIRST_ID,21,2,15) as BusbarElement
            join(a,upper,32,32);join(b,lower,32,32)
            join(upper,common,36,32);join(lower,common,36,32)
            source(a.load,115.0);source(b.load,240.0)
            load=consume(common.load,100.0)
            check(upper.setClosed(true))
            c.after(22) {
                c.checkValue("first bus feeds the common distribution bay",1.15,load.i)
                c.checkValue("second bus stays at its independent voltage",240.0,b.load.u)
                check(upper.setClosed(false))
            }
            c.after(40) {
                c.check("break-before-make disconnects the common bus",abs(load.i)<1e-8,"")
                check(lower.setClosed(true))
            }
        }
        override fun measure(c: TestContext) {
            try { c.checkValue("second bus supplies the transferred load",2.40,load.i) }
            finally { super.measure(c) }
        }
    }
}
