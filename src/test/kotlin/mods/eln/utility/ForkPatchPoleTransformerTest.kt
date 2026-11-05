package mods.eln.utility

import mods.eln.sim.IProcess
import mods.eln.sim.mna.RootSystem
import mods.eln.sim.mna.component.*
import mods.eln.sim.mna.state.State
import net.minecraft.nbt.CompoundTag
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class ForkPatchPoleTransformerTest {
    private class Circuit(three: Boolean=false,core: Int?=50000,reverse: Boolean=false) {
        val states=ArrayList<State>();val parts=ArrayList<Component>();val processes=ArrayList<IProcess>()
        val u=DistributionPoleUnit(if(three)DistributionKind.TRANSFORMER_THREE else DistributionKind.TRANSFORMER_SINGLE,states,parts,processes)
        val t=u.transformer!!;val root=RootSystem(.05,1)
        val sources=if(reverse)listOf(VoltageSource("export",u.services[2],null).setU(240.0)) else u.lines.mapIndexed { i,l->VoltageSource("feed$i",l,null).setU(12470.0) }
        val loads=if(reverse)listOf(Resistor(u.lines[0],null).setR(30000.0)) else u.services.map { Resistor(it,null).setR(1000.0) }
        init { u.installTransformer(core);states.forEach(root::addState);parts.forEach(root::addComponent);sources.forEach(root::addComponent);loads.forEach(root::addComponent) }
        fun run(n: Int=1) { repeat(n) { root.step();processes.forEach { it.process(.05) } } }
        fun loss()=parts.filterIsInstance<Resistor>().sumOf { it.p }+u.contacts.sumOf { it.power }
    }
    @Test fun emptyPoleHasNoSecondarySource() {
        val c=Circuit(core=null);c.run(30);assertFalse(c.t.tap.enabled);assertTrue(c.u.contacts.none { it.closed });assertTrue(c.u.services.all { abs(it.u)<1e-8 })
    }
    @Test fun everyPaidCoreRatingFeedsTheSameSharedService() {
        for(watts in listOf(10000,25000,50000,75000,100000)) {
            val c=Circuit(core=watts);c.loads.forEachIndexed { i,l->l.r=if(i==2)24.0 else 12.0 };c.run(60)
            assertEquals(watts,c.t.ratedWatts);assertTrue(c.u.fuses.none { it.blown });assertTrue(c.u.services[2].u in 238.0..240.0)
            assertEquals(c.sources.sumOf { it.p },c.loads.sumOf { it.p }+c.loss(),1e-5)
        }
    }
    @Test fun phaseSelectionUsesExactlyOnePrimaryAndLeavesOtherPhasesIndependent() {
        val c=Circuit(three=true);c.sources[1].u=12000.0;c.sources[2].u=11000.0;c.u.selectPhase(2);c.run(30)
        assertFalse(c.u.contacts[0].closed);assertFalse(c.u.contacts[1].closed);assertTrue(c.u.contacts[2].closed)
        assertEquals(240.0*11000/12470,c.u.services[2].u,.02);assertEquals(12000.0,c.u.lines[1].u,1e-7)
        assertTrue(abs(c.sources[0].i)<1e-4 && abs(c.sources[1].i)<1e-4)
    }
    @Test fun bothWindingSettingsConservePowerAndKeep120240ServiceEquivalents() {
        val c=Circuit();c.loads[2].r=24.0
        for(voltage in listOf(120,240)) { assertTrue(c.u.configureTransformer(voltage,true));c.run(20);assertEquals(voltage.toDouble(),c.t.secondary.u,.2);assertEquals(120.0,c.u.services[0].u,.1);assertEquals(240.0,c.u.services[2].u,.3);assertEquals(c.sources.sumOf { it.p },c.loads.sumOf { it.p }+c.loss(),1e-5) }
    }
    @Test fun passiveRatioFollowsPrimarySagWithoutInventingRegulatedPower() {
        val c=Circuit();c.sources[0].u=6235.0;c.run(30)
        assertEquals(60.0,c.u.services[0].u,.01);assertEquals(60.0,c.u.services[1].u,.01);assertEquals(120.0,c.u.services[2].u,.01)
    }
    @Test fun reversePowerFlowsThroughTheSamePaidWinding() {
        val c=Circuit(reverse=true);c.run(30)
        assertTrue(c.u.lines[0].u>12460);assertTrue(c.u.current(0)<0);assertTrue(c.t.tap.current<0)
        assertEquals(c.sources.sumOf { it.p },c.loads.sumOf { it.p }+c.loss(),1e-4)
    }
    @Test fun removingTheCoreImmediatelyDisconnectsTheService() {
        val c=Circuit();c.run(10);assertTrue(c.loads.sumOf { it.p }>50)
        c.u.installTransformer(null);assertFalse(c.t.tap.enabled);c.run(10);assertEquals(0.0,c.loads.sumOf { it.p },1e-8)
    }
    @Test fun openPrimaryFuseIsolatesServiceWithoutOpeningTheThroughPrimary() {
        val c=Circuit(three=true);c.run(10);assertTrue(c.u.operateFuse(0));c.run(10)
        assertTrue(c.u.contacts.none { it.closed });assertTrue(c.u.lines.all { it.u>12460 });assertEquals(0.0,c.loads.sumOf { it.p },1e-8)
    }
    @Test fun savedSettingsCannotMintHardwareOrReenableAnIsolatedCore() {
        val c=Circuit(three=true);c.u.selectPhase(1);c.u.configureTransformer(240,false)
        val n=CompoundTag();c.u.write(n);n.putInt("mainsRatedWatts",100000)
        val copy=Circuit(three=true,core=null);copy.u.read(n);copy.run(10)
        assertEquals(1,copy.u.phase);assertEquals(240,copy.t.outputVoltage);assertEquals(0,copy.t.ratedWatts);assertFalse(copy.t.enabled);assertFalse(copy.t.tap.enabled)
    }
    @Test fun malformedSettingsDoNotAlterTheWindingAndInvalidTimeDoesNotAdvanceProtection() {
        val c=Circuit();assertFalse(c.u.configureTransformer(123,true));c.u.step(Double.NaN);c.u.step(-1.0)
        assertEquals(120,c.t.outputVoltage);assertFalse(c.t.protection.tripped);assertEquals(0.0,c.t.protection.elapsed,0.0)
    }
    @Test fun steadyTransformerRetainsNativeLuFactors() {
        val c=Circuit();c.run(30);val versions=c.root.systems().map { it.matrixVersion };c.run(200)
        assertEquals(versions,c.root.systems().map { it.matrixVersion })
    }
    @Test fun copperHeatingUsesEnergyOncePerPhysicalStep() {
        val c=Circuit();c.loads[2].r=24.0;c.run(10);c.t.thermal.EnergyTemp=0.0
        c.root.step();val expected=c.t.winding.p*.05;c.processes.forEach { it.process(.05) }
        assertEquals(expected,c.t.thermal.EnergyTemp,1e-9)
    }
    @Test fun hotCanDisconnectsAndOnlyResumesAfterCooling() {
        val c=Circuit();c.run(20);c.t.thermal.Tc=90.0;c.u.step(.05);c.run(2)
        assertFalse(c.t.tap.enabled);assertEquals(0.0,c.loads.sumOf { it.p },1e-8)
        c.t.thermal.Tc=30.0;c.u.step(.05);c.run(10);assertTrue(c.t.tap.enabled);assertTrue(c.loads.sumOf { it.p }>50)
    }
    @Test fun allServiceLoadsContributeToOneWindingCurrent() {
        val c=Circuit();c.loads[0].r=12.0;c.loads[1].r=12.0;c.loads[2].r=24.0;c.run(20)
        assertEquals(c.loads.sumOf { it.p }+c.loss(),c.sources[0].p,1e-4)
        assertTrue(c.t.tap.current>c.loads[2].p/12470.0*1.9)
    }
}
