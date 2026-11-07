package mods.eln.utility

import mods.eln.sim.IProcess
import mods.eln.sim.mna.RootSystem
import mods.eln.sim.mna.component.*
import mods.eln.sim.mna.state.State
import net.minecraft.nbt.CompoundTag
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class ForkPatchLegacyDistributionTest {
    private class Circuit(val kind: DistributionKind) {
        val states=ArrayList<State>();val parts=ArrayList<Component>();val processes=ArrayList<IProcess>();val u=DistributionPoleUnit(kind,states,parts,processes);val root=RootSystem(.05,1)
        val sources=u.lines.mapIndexed { i,l->VoltageSource("phase$i",l,null).setU(1000.0*(i+1)) }
        val loads=u.outs.map { Resistor(it,null).setR(10000.0) }
        val lampSource=if(kind.light)VoltageSource("lamp supply",u.services[0],null).setU(120.0)else null
        init { states.forEach(root::addState);parts.forEach(root::addComponent);sources.forEach(root::addComponent);loads.forEach(root::addComponent);lampSource?.let(root::addComponent) }
        fun run(n: Int=10) { repeat(n) { root.step();processes.forEach { it.process(.05) };u.lamp?.sample() } }
    }
    @Test fun fusedTapLampUsesOnlyTheIndependentServiceSupply() {
        val c=Circuit(DistributionKind.FUSED_TAP_LIGHT_THREE);c.run();assertEquals(70.0,c.u.lamp!!.watts,1e-8);assertEquals(1000.0,c.u.outs[0].u,.01)
        c.lampSource!!.u=0.0;c.run();assertEquals(0,c.u.lamp!!.light);assertTrue(c.u.outs[0].u>999)
    }
    @Test fun blowingTheHvTapDoesNotExtinguishItsSeparateLvLamp() {
        val c=Circuit(DistributionKind.FUSED_TAP_LIGHT_THREE);c.u.setFuseAmps(1.0);c.loads[0].r=100.0;c.run()
        assertTrue(c.u.fuses[0].blown);assertEquals(0.0,c.loads[0].p,1e-7);assertEquals(70.0,c.u.lamp!!.watts,1e-8);assertTrue(c.u.lamp!!.light>=14);assertTrue(c.u.lines[2].u>2999)
    }
    @Test fun tapSelectionMovesOnlyTheHvBranchAndKeepsStableFactors() {
        val c=Circuit(DistributionKind.FUSED_TAP_LIGHT_THREE);assertTrue(c.u.selectPhase(2));c.run();assertEquals(3000.0,c.u.outs[0].u,.01);assertEquals(70.0,c.u.lamp!!.watts,1e-8)
        val versions=c.root.systems().map { it.matrixVersion };c.run(100);assertEquals(versions,c.root.systems().map { it.matrixVersion })
    }
    @Test fun retiredCabinetCarriesThreeIndependentPrimaryCircuitsWithoutAnLvSource() {
        val c=Circuit(DistributionKind.UNDERGROUND_THREE);c.run();c.u.outs.forEachIndexed { i,l->assertEquals(1000.0*(i+1),l.u,.01) };assertTrue(c.u.services.all { abs(it.u)<1e-8 })
        val loss=c.parts.filterIsInstance<Resistor>().sumOf { it.p }+c.u.contacts.sumOf { it.power };assertEquals(c.sources.sumOf { it.p },c.loads.sumOf { it.p }+loss,1e-5)
    }
    @Test fun retiredCabinetFaultOpensOnlyItsOwnPhase() {
        val c=Circuit(DistributionKind.UNDERGROUND_THREE);c.u.setFuseAmps(1.0);c.loads[0].r=100.0;c.run()
        assertTrue(c.u.fuses[0].blown);assertFalse(c.u.fuses[1].blown);assertFalse(c.u.fuses[2].blown);assertEquals(0.0,c.loads[0].p,1e-7);assertTrue(c.u.outs[1].u>1999 && c.u.outs[2].u>2999)
    }
    @Test fun retiredCabinetKeepsEachSavedCartridgeLatch() {
        val c=Circuit(DistributionKind.UNDERGROUND_THREE);c.u.setFuseAmps(1.0);c.loads[0].r=100.0;c.run();val n=CompoundTag();c.u.write(n);n.putBoolean("utilityFuse0Closed",true);c.u.read(n);c.run()
        assertFalse(c.u.operateFuse(0));assertFalse(c.u.contacts[0].closed);assertTrue(c.u.contacts[1].closed && c.u.contacts[2].closed)
    }
}
