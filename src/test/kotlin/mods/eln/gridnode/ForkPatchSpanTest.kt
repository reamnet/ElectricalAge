package mods.eln.gridnode

import mods.eln.sim.mna.RootSystem
import mods.eln.sim.mna.component.*
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.utility.HouseServiceUnit
import mods.eln.sim.IProcess
import mods.eln.sim.mna.state.State
import org.junit.Test
import org.junit.Assert.*

class ForkPatchSpanTest {
    private class Circuit(volts: Double=240.0) {
        val a=NbtElectricalLoad("a").apply { rs=.1 };val b=NbtElectricalLoad("b").apply { rs=.2 }
        val wire=GridSpanConnection(a,b,.8);val source=VoltageSource("supply",a,null).setU(volts)
        val load=Resistor(b,null).setR(24.0);val root=RootSystem(.05,1)
        init { root.addState(a);root.addState(b);root.addComponent(source);root.addComponent(wire);root.addComponent(load) }
        fun run(n: Int=2) { repeat(n) { root.step() } }
    }
    @Test fun actualCableLossIncludesBothTerminalContacts() {
        val c=Circuit();c.run();assertEquals(1.1,c.wire.r,1e-12);assertEquals(240/25.1*24,c.b.u,1e-8)
        assertEquals(c.source.p,c.load.p+c.wire.p,1e-7)
    }
    @Test fun terminalHeatingCannotEraseThePaidWireResistance() {
        val c=Circuit();c.run();c.a.rs=.7;c.b.rs=.4;c.run()
        assertEquals(1.9,c.wire.r,1e-12);assertEquals(240/25.9*24,c.b.u,1e-8);assertEquals(.8,c.wire.spanOhms,0.0)
    }
    @Test fun steadySpanReusesNativeLuFactors() {
        val c=Circuit();c.run();val versions=c.root.systems().map { it.matrixVersion };c.run(100)
        assertEquals(versions,c.root.systems().map { it.matrixVersion })
    }
    @Test fun reverseFlowRetainsPositivePhysicalWireLoss() {
        val c=Circuit(-240.0);c.run();assertTrue(c.wire.p>0);assertEquals(-240/25.1*24,c.b.u,1e-8)
        assertEquals(c.source.p,c.load.p+c.wire.p,1e-7)
    }
    @Test fun removingAndRestoringASpanDoesNotLeaveADanglingConnection() {
        val c=Circuit();c.run();c.root.removeComponent(c.wire);c.run();assertEquals(0.0,c.load.p,1e-8)
        c.root.addComponent(c.wire);c.run();assertEquals(240/25.1*24,c.b.u,1e-8);assertEquals(1.1,c.wire.r,1e-12)
    }
    @Test fun independentBundleChannelsDoNotTurnTwoLegsIntoTheSameBus() {
        val circuits=listOf(120.0,111.0,240.0).map(::Circuit);circuits.forEach { it.run() }
        circuits.zip(listOf(120.0,111.0,240.0)).forEach { (c,v)->assertEquals(v/25.1*24,c.b.u,1e-8) }
        assertTrue(circuits[0].b.u!=circuits[1].b.u)
    }
    @Test fun overheadServiceRetainsIndependentFeedsWithoutWiredSplitTaps() {
        val states=ArrayList<State>();val parts=ArrayList<Component>();val processes=ArrayList<IProcess>()
        val unit=HouseServiceUnit(false,false,states,parts,processes);unit.wiredSplitAllowed={false}
        val root=RootSystem(.05,1);states.forEach(root::addState);parts.forEach(root::addComponent)
        val volts=listOf(120.0,111.0,240.0)
        for(i in 0..2) {
            val feed=NbtElectricalLoad("feed$i").apply { rs=.00001 };root.addState(feed)
            root.addComponent(VoltageSource("supply$i",feed,null).setU(volts[i]));root.addComponent(GridSpanConnection(feed,unit.line[i],.8))
            root.addComponent(Resistor(unit.line[i],null).setR(24.0))
        }
        repeat(10) { root.step();processes.forEach { it.process(.05) } }
        for(i in 0..2)assertEquals(volts[i]*24/(24+.80002),unit.line[i].u,1e-7)
    }
    @Test fun invalidSpanResistanceIsRejected() {
        for(value in listOf(-1.0,Double.NaN,Double.POSITIVE_INFINITY)) org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            GridSpanConnection(NbtElectricalLoad("a"),NbtElectricalLoad("b"),value)
        }
    }
}
