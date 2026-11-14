package mods.eln.utility

import mods.eln.Eln
import mods.eln.sim.IProcess
import mods.eln.sim.mna.RootSystem
import mods.eln.sim.mna.component.*
import mods.eln.sim.mna.state.State
import net.minecraft.nbt.CompoundTag
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class ForkPatchDistributionTest {
    private class Circuit(val kind: DistributionKind,voltage: Double=12470.0,loadOhms: Double=1000.0,reverse: Boolean=false) {
        val states=ArrayList<State>();val parts=ArrayList<Component>();val processes=ArrayList<IProcess>()
        val u=DistributionPoleUnit(kind,states,parts,processes)
        val root=RootSystem(.05,1)
        val sources=u.lines.indices.map { i->VoltageSource("phase$i",if(reverse)u.outs[i] else u.lines[i],null).setU(voltage) }
        val loads=u.outs.indices.map { i->Resistor(if(reverse)u.lines[i] else u.outs[i],null).setR(loadOhms) }
        init { states.forEach(root::addState);parts.forEach(root::addComponent);sources.forEach(root::addComponent);loads.forEach(root::addComponent) }
        fun run(n: Int=1) { repeat(n) { root.step();processes.forEach { it.process(.05) } } }
        fun loss()=parts.filterIsInstance<Resistor>().sumOf { it.p }+u.contacts.sumOf { it.power }
    }
    @Test fun threePhasesStayIndependentWithoutInventedSecondaryVoltage() {
        val c=Circuit(DistributionKind.THREE);c.sources.forEachIndexed { i,s->s.u=100.0*(i+1) };c.run(4)
        c.u.lines.forEachIndexed { i,l->assertEquals(100.0*(i+1),l.u,1e-7) }
        c.u.services.forEach { assertEquals(0.0,it.u,1e-9) }
    }
    @Test fun aHealthyCutoutCarriesPowerAndContactLoss() {
        val c=Circuit(DistributionKind.CUTOUT_SINGLE,loadOhms=249.4);c.run(100)
        assertFalse(c.u.fuses[0].blown);assertTrue(c.loads[0].current in 49.9..50.1)
        assertEquals(c.sources.sumOf { it.p },c.loads.sumOf { it.p }+c.loss(),1e-4)
    }
    @Test fun anOverloadedCutoutOpensAndCannotBeClosedWithoutReplacement() {
        val c=Circuit(DistributionKind.CUTOUT_SINGLE,loadOhms=62.35);c.run(10)
        assertTrue(c.u.fuses[0].blown);assertFalse(c.u.operateFuse(0));c.run(2);assertEquals(0.0,c.loads[0].p,1e-6)
    }
    @Test fun oneBlownPhaseDoesNotOpenTheOtherTwoCutouts() {
        val c=Circuit(DistributionKind.CUTOUT_THREE);c.loads[0].r=30.0;c.run(10)
        assertTrue(c.u.fuses[0].blown);assertFalse(c.u.fuses[1].blown);assertFalse(c.u.fuses[2].blown)
        assertTrue(c.u.outs[1].u>12400);assertEquals(0.0,c.u.outs[0].u,1e-6)
    }
    @Test fun aSavedClosedFlagCannotReviveABlownCutout() {
        val c=Circuit(DistributionKind.CUTOUT_SINGLE,loadOhms=62.35);c.run(10)
        val saved=CompoundTag();c.u.write(saved);saved.putBoolean("utilityFuse0Closed",true);c.u.read(saved);c.run(2)
        assertTrue(c.u.fuses[0].blown);assertFalse(c.u.fuses[0].closed);assertFalse(c.u.contacts[0].closed);assertEquals(0.0,c.loads[0].p,1e-6)
    }
    @Test fun recloserRetriesThenLocksOutUntilExplicitReset() {
        val c=Circuit(DistributionKind.RECLOSER_THREE,loadOhms=1.0);c.u.configureRecloser(100.0,1.0,2);c.run(100)
        assertTrue(c.u.recloser.locked);assertEquals(2,c.u.recloser.failures);assertFalse(c.u.switch(true))
        c.loads.forEach { it.r=1000.0 };c.u.resetRecloser();c.run(20);assertFalse(c.u.recloser.locked);assertTrue(c.loads.all { it.p>100000 })
    }
    @Test fun manualRecloserOpeningDoesNotAutoRetry() {
        val c=Circuit(DistributionKind.RECLOSER_THREE);c.run(4);assertTrue(c.u.switch(false));c.run(300)
        assertTrue(c.u.recloser.manualOff);assertFalse(c.u.recloser.closed);assertTrue(c.loads.all { abs(it.p)<1e-6 })
    }
    @Test fun recloserAndRegulatorSettingsKeepFiniteSavedState() {
        val c=Circuit(DistributionKind.RECLOSER_THREE,loadOhms=1.0);c.u.configureRecloser(80.0,2.0,3);c.run(10)
        val saved=CompoundTag();c.u.write(saved);val remaining=c.u.recloser.remaining;c.u.read(saved)
        assertEquals(remaining,c.u.recloser.remaining,0.0);assertFalse(c.u.recloser.closed)
        assertFalse(c.u.configureRecloser(Double.NaN,2.0,3));assertFalse(c.u.configureRegulator(Double.POSITIVE_INFINITY,true))
    }
    @Test fun voltageRegulationMovesAtMostOnePercentPerSecond() {
        val c=Circuit(DistributionKind.REGULATOR_SINGLE,voltage=12000.0);c.run(20)
        assertTrue(c.u.regulatorTaps[0].ratio in 1.009..1.011);c.run(120)
        assertEquals(12470.0,c.u.outs[0].u,.3)
        assertEquals(c.sources.sumOf { it.p },c.loads.sumOf { it.p }+c.loss(),1e-3)
    }
    @Test fun regulatorCannotExceedItsTenPercentTapRange() {
        val c=Circuit(DistributionKind.REGULATOR_THREE,voltage=10000.0);c.u.configureRegulator(20000.0,true);c.run(300)
        c.u.regulatorTaps.forEach { assertEquals(1.1,it.ratio,1e-10) };assertTrue(c.u.outs.all { it.u in 10999.0..11000.1 })
    }
    @Test fun disabledAutomaticRegulationHoldsItsSavedTap() {
        val c=Circuit(DistributionKind.REGULATOR_SINGLE);c.u.configureRegulator(13000.0,false);c.run(100)
        assertEquals(1.0,c.u.regulatorTaps[0].ratio,0.0)
        c.u.regulatorTaps[0].ratio=1.04;val t=CompoundTag();c.u.write(t);c.u.regulatorTaps[0].ratio=.9;c.u.read(t);assertEquals(1.04,c.u.regulatorTaps[0].ratio,0.0)
    }
    @Test fun gangSwitchOpensEveryPhaseAndSupportsReversePower() {
        val c=Circuit(DistributionKind.AIR_BREAK_THREE,voltage=240.0,loadOhms=24.0,reverse=true);c.run(10)
        assertTrue(c.u.contacts.all { it.current<0 });assertEquals(c.sources.sumOf { it.p },c.loads.sumOf { it.p }+c.loss(),1e-6)
        assertTrue(c.u.switch(false));c.run(4);assertTrue(c.loads.all { abs(it.p)<1e-7 })
    }
    @Test fun fusedTapSelectsOnePhaseWithoutParallelingTheOthers() {
        val c=Circuit(DistributionKind.FUSED_TAP_THREE,voltage=100.0);c.sources[1].u=200.0;c.sources[2].u=300.0;c.run(4)
        assertEquals(100.0,c.u.outs[0].u,.01);assertTrue(c.u.selectPhase(2));c.run(4)
        assertEquals(300.0,c.u.outs[0].u,.01);assertFalse(c.u.contacts[0].closed);assertFalse(c.u.contacts[1].closed);assertTrue(c.u.contacts[2].closed)
    }
    @Test fun capacitorBankRetainsNativeHistoryAndSteadyFactorization() {
        val c=Circuit(DistributionKind.CAPACITOR_THREE);c.run(100)
        assertTrue(c.u.capacitorPins.all { it.u.isFinite() && it.u>12460.0 })
        val versions=c.root.systems().map { it.matrixVersion };c.run(100);assertEquals(versions,c.root.systems().map { it.matrixVersion })
    }
    @Test fun healthyContactsKeepCachedNativeLuFactors() {
        for(kind in listOf(DistributionKind.CUTOUT_SINGLE,DistributionKind.CUTOUT_THREE,DistributionKind.RECLOSER_THREE,DistributionKind.AIR_BREAK_THREE,DistributionKind.REGULATOR_SINGLE)) {
            val c=Circuit(kind);c.run(30);val versions=c.root.systems().map { it.matrixVersion };c.run(100);assertEquals(kind.name,versions,c.root.systems().map { it.matrixVersion })
        }
    }
}
