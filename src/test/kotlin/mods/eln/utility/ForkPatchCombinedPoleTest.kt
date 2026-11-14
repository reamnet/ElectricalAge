package mods.eln.utility

import mods.eln.sim.IProcess
import mods.eln.sim.mna.RootSystem
import mods.eln.sim.mna.component.*
import mods.eln.sim.mna.state.State
import net.minecraft.nbt.CompoundTag
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

/** Actual physical solves exercise the two independently fused paths of source identity 8023. */
class ForkPatchCombinedPoleTest {
    private class Circuit(core: Int?=50000) {
        val states=ArrayList<State>();val parts=ArrayList<Component>();val processes=ArrayList<IProcess>()
        val u=DistributionPoleUnit(DistributionKind.FUSED_TAP_TRANSFORMER_LIGHT_THREE,states,parts,processes)
        val root=RootSystem(.05,1)
        val sources=u.lines.mapIndexed { i,l->VoltageSource("primary$i",l,null).setU(12470.0-i*1000) }
        val branch=Resistor(u.outs[0],null).setR(10000.0)
        val service=Resistor(u.services[2],null).setR(24.0)
        init { u.installTransformer(core);states.forEach(root::addState);parts.forEach(root::addComponent);sources.forEach(root::addComponent);root.addComponent(branch);root.addComponent(service) }
        fun run(n: Int=20) { repeat(n) { root.step();processes.forEach { it.process(.05) };u.lamp!!.sample() } }
    }
    @Test fun branchAndTransformerUseDifferentContactsAndMeasurements() {
        val c=Circuit();c.run()
        assertEquals(2,c.u.fuses.size);assertEquals(1,c.u.contacts.count { it.closed });assertEquals(1,c.u.transformerContacts.count { it.closed })
        assertTrue(c.u.services[2].u in 239.0..240.0);assertTrue(c.branch.p>15000);assertEquals(70.0,c.u.lamp!!.watts,.1)
        assertEquals(c.branch.current+c.u.outs[0].u/1e10,c.u.current(1),1e-8);assertTrue(abs(c.u.current(0))<.25)
        val loss=c.parts.filterIsInstance<Resistor>().sumOf { it.p }+(c.u.contacts+c.u.transformerContacts).sumOf { it.power }
        assertEquals(c.sources.sumOf { it.p },c.branch.p+c.service.p+loss,1e-5)
    }
    @Test fun openingTapLeavesTransformerAndLampPowered() {
        val c=Circuit();c.run();assertTrue(c.u.operateFuse(1));c.run()
        assertEquals(0.0,c.branch.p,1e-7);assertTrue(c.u.transformer!!.tap.enabled);assertTrue(c.service.p>2390);assertTrue(c.u.lamp!!.light>=14)
    }
    @Test fun openingTransformerLeavesTapPowered() {
        val c=Circuit();c.run();assertTrue(c.u.operateFuse(0));c.run()
        assertEquals(0.0,c.service.p,1e-7);assertEquals(0,c.u.lamp!!.light);assertTrue(c.branch.p>15000);assertTrue(c.u.contacts[0].closed)
    }
    @Test fun branchOverloadCannotBlowTheTransformerCartridge() {
        val c=Circuit();c.u.setFuseAmps(1.0);c.branch.r=1000.0;c.run()
        assertTrue(c.u.fuses[1].blown);assertFalse(c.u.fuses[0].blown);assertTrue(c.u.transformer!!.tap.enabled);assertTrue(c.service.p>2390)
    }
    @Test fun coreOverloadCannotOpenTheSeparateBranch() {
        val c=Circuit(10000);c.service.r=1.0;c.run(80)
        assertTrue(c.u.fuses[0].blown || c.u.transformer!!.protection.tripped);assertFalse(c.u.fuses[1].blown);assertTrue(c.branch.p>15000)
    }
    @Test fun phaseSelectionMovesBothFeedsWithoutConnectingPrimaries() {
        val c=Circuit();assertTrue(c.u.selectPhase(2));c.run()
        assertEquals(10470.0,c.u.outs[0].u,.01);assertEquals(240.0*10470/12470,c.u.services[2].u,.1)
        assertFalse(c.u.contacts[0].closed || c.u.transformerContacts[0].closed)
        assertTrue(abs(c.sources[0].i)<1e-4 && abs(c.sources[1].i)<1e-4)
    }
    @Test fun missingCoreDoesNotDisableTheIndependentTap() {
        val c=Circuit(null);c.run();assertTrue(c.branch.p>15000);assertEquals(0.0,c.service.p,1e-7);assertTrue(c.u.transformerContacts.none { it.closed })
    }
    @Test fun savesRetainIndependentBlownAndOpenLatches() {
        val c=Circuit();c.u.setFuseAmps(1.0);c.branch.r=1000.0;c.run();val t=CompoundTag();c.u.write(t)
        val copy=Circuit();copy.u.read(t);copy.run();assertTrue(copy.u.fuses[1].blown);assertFalse(copy.u.operateFuse(1));assertTrue(copy.service.p>2390)
    }
    @Test fun idleCombinedPoleKeepsNativeLuFactorsCached() {
        val c=Circuit();c.run();val versions=c.root.systems().map { it.matrixVersion };c.run(200);assertEquals(versions,c.root.systems().map { it.matrixVersion })
    }
}
