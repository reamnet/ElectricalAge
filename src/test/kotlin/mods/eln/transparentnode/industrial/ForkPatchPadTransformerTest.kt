package mods.eln.transparentnode.industrial

import mods.eln.sim.IProcess
import mods.eln.sim.mna.RootSystem
import mods.eln.sim.mna.component.*
import mods.eln.sim.mna.state.State
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.utility.ServiceTap
import net.minecraft.nbt.CompoundTag
import org.junit.Test
import org.junit.Assert.*
import java.io.*

class ForkPatchPadTransformerTest {
    private class Circuit(core: Int=50000,fuse: Double=200.0) {
        val states=ArrayList<State>();val parts=ArrayList<Component>();val processes=ArrayList<IProcess>()
        var fuseBlows=0;var alarm=false
        val u=PadTransformerUnit(states,parts,processes,{fuseBlows++},{alarm=it})
        val root=RootSystem(.05,1)
        val source=VoltageSource("primary",u.input,null).setU(12470.0)
        val load=Resistor(u.output,null).setR(240.0/145)
        init { u.install(core,fuse);states.forEach(root::addState);parts.forEach(root::addComponent);root.addComponent(source);root.addComponent(load) }
        fun run(n: Int=1) { repeat(n) { root.step();processes.forEach { p->p.process(.05) } } }
        fun loss()=parts.filterIsInstance<Resistor>().sumOf { it.p }
    }
    @Test fun a145Amp240VoltLoadDoesNotBlowA200AmpPrimaryLink() {
        val c=Circuit();c.run(400)
        assertTrue(c.load.current in 140.0..146.0);assertTrue(c.u.primaryAmps in 2.7..2.9)
        assertFalse(c.u.protection.tripped);assertEquals(0,c.fuseBlows)
        assertEquals(c.source.p,c.load.p+c.loss(),1e-5)
        assertEquals(c.u.primaryAmps,c.u.input.i,1e-4)
    }
    @Test fun anUndersizedCoreTripsWithoutConsumingAHealthyPrimaryFuse() {
        val c=Circuit(core=25000);c.run(38);assertFalse(c.alarm);c.run(5)
        assertTrue(c.alarm);assertEquals(7,c.u.protection.reason);assertEquals(0,c.fuseBlows);assertFalse(c.u.tap.enabled)
        c.load.r=24.0;c.u.resetTrip();c.run(60);assertFalse(c.alarm);assertTrue(c.load.p>2300)
    }
    @Test fun actualPrimaryOvercurrentBlowsOnceAndRequiresAPaidReplacement() {
        val c=Circuit(fuse=1.0);c.run(38);assertFalse(c.alarm);c.run(5)
        assertEquals(2,c.u.protection.reason);assertEquals(1,c.fuseBlows);c.run(100);assertEquals(1,c.fuseBlows)
        c.u.resetTrip();c.run(5);assertFalse(c.u.tap.enabled);assertEquals(0.0,c.load.p,1e-8)
        c.load.r=24.0;c.u.install(50000,1.0);c.run(20);assertTrue(c.load.p>2300);assertEquals(1,c.fuseBlows)
    }
    @Test fun aRealShortBlowsA200AmpLinkAndPendingDelaySurvivesReload() {
        val c=Circuit();c.load.r=.0001;c.run(38);assertTrue(c.u.primaryAmps>200);assertFalse(c.alarm)
        val saved=CompoundTag().also(c.u::write);c.u.read(saved);assertEquals(2,c.u.protection.pendingReason);assertTrue(c.u.protection.elapsed>1.8)
        c.run(5);assertEquals(1,c.fuseBlows);assertEquals(2,c.u.protection.reason)
    }
    @Test fun overloadTimeCannotPreheatAnOtherwiseHealthyFuseTimer() {
        val p=Protection();repeat(35) { p.stepPad(.05,12470.0,3.0,12470.0,200.0,25000.0) }
        p.stepPad(.05,12470.0,201.0,12470.0,200.0,25000.0);assertFalse(p.tripped);assertEquals(.05,p.elapsed,1e-12)
        repeat(39) { p.stepPad(.05,12470.0,201.0,12470.0,200.0,25000.0) };assertTrue(p.tripped);assertEquals(2,p.reason)
    }
    @Test fun serviceLegsAndFullSecondaryShareOneCapacityGuardAndFollowSag() {
        val c=Circuit(core=10000);c.load.r=1e19
        val loads=c.u.service.loads.mapIndexed { i,b->Resistor(b,null).setR(if(i==2)96.0 else 48.0).also(c.root::addComponent) }
        c.run(8);val nominal=c.u.service.loads.map { it.u };nominal.forEachIndexed { i,v->assertEquals(if(i==2)240.0 else 120.0,v,.2) }
        assertEquals(c.source.p,loads.sumOf { it.p }+c.load.p+c.loss(),1e-5)
        c.source.u=11846.5;c.run(8);c.u.service.loads.forEachIndexed { i,b->assertEquals(nominal[i]*.95,b.u,1e-6) }
        c.source.u=12470.0;c.u.applySettings(doubleArrayOf(12470.0,208.0));c.run(8)
        assertEquals(208.0,c.u.output.u,.2);assertEquals(104.0,c.u.service.loads[0].u,.2)
        c.u.setEnabled(false);c.run(3);assertTrue(loads.all { it.p<1e-8 })
    }
    @Test fun combiningIndividuallySmallLegLoadsStillTripsTheSharedCoreLimit() {
        val c=Circuit(core=10000);c.load.r=1e19
        c.u.service.loads.forEach { c.root.addComponent(Resistor(it,null).setR(if(it===c.u.service.loads[2])12.0 else 3.0)) }
        c.run(45);assertEquals(7,c.u.protection.reason);assertEquals(0,c.fuseBlows)
    }
    @Test fun missingHardwareAndManualIsolationCannotCreateOutputPower() {
        val c=Circuit();c.u.install(null,200.0);c.run(5);assertTrue(c.load.p<1e-8);assertEquals("NEEDS_TRANSFORMER",c.u.status)
        c.u.install(50000,null);c.run(5);assertTrue(c.load.p<1e-8);assertEquals("NEEDS_FUSE",c.u.status)
        c.u.install(50000,200.0);c.u.setEnabled(false);c.run(5);assertTrue(c.load.p<1e-8)
    }
    @Test fun settingsPacketsAreAtomicRejectInvalidVoltagesAndCannotUpgradeCapacity() {
        val c=Circuit(core=10000);fun packet(vararg p: Double)=c.u.packet(80,DataInputStream(ByteArrayInputStream(ByteArrayOutputStream().also { b->DataOutputStream(b).use { s->p.forEach(s::writeDouble) } }.toByteArray())))
        assertFalse(packet(12470.0));assertEquals(240.0,c.u.secondaryVolts,0.0)
        assertFalse(packet(Double.NaN,240.0));assertFalse(packet(12470.0,481.0));assertTrue(packet(12470.0,208.0));assertEquals(10000,c.u.ratedWatts)
    }
    @Test fun passiveTransformerTerminalCurrentsAreDistinctAndConservePower() {
        val root=RootSystem(.05,1);val a=NbtElectricalLoad("primary");val b=NbtElectricalLoad("secondary");root.addState(a);root.addState(b)
        val tap=ServiceTap(a,b,.02);val supply=VoltageSource("grid",a,null).setU(12000.0);val load=Resistor(b,null).setR(2.4)
        listOf(tap,supply,load).forEach(root::addComponent);root.step()
        assertEquals(2.0,tap.current,1e-8);assertEquals(-100.0,tap.getCurrentInto(b),1e-8)
        assertEquals(2.0,a.i,1e-8);assertEquals(100.0,b.i,1e-8);assertEquals(0.0,a.u*tap.getCurrentInto(a)+b.u*tap.getCurrentInto(b),1e-6)
    }
    @Test fun healthyPadReusesNativeLuFactorsAndTrippedStatePersists() {
        val c=Circuit();c.run(5);val versions=c.root.systems().map { it.matrixVersion };c.run(100);assertEquals(versions,c.root.systems().map { it.matrixVersion })
        c.u.install(10000,200.0);c.run(45);val saved=CompoundTag().also(c.u::write)
        val copy=Circuit();copy.u.read(saved);assertEquals(7,copy.u.protection.reason);assertFalse(copy.u.tap.enabled)
        copy.u.setEnabled(false);copy.u.setEnabled(true);assertFalse(copy.u.tap.enabled)
    }
    @Test fun passivePadCanReturnSecondaryGenerationToThePrimaryWithoutInventingPower() {
        val states=ArrayList<State>();val parts=ArrayList<Component>();val processes=ArrayList<IProcess>()
        val u=PadTransformerUnit(states,parts,processes);u.install(50000,200.0)
        val root=RootSystem(.05,1);states.forEach(root::addState);parts.forEach(root::addComponent)
        val generator=VoltageSource("secondary generation",u.output,null).setU(240.0)
        val gridLoad=Resistor(u.input,null).setR(12470.0*12470/30000)
        root.addComponent(generator);root.addComponent(gridLoad)
        repeat(100) { root.step();processes.forEach { p->p.process(.05) } }
        assertTrue(u.tap.current<0);assertFalse(u.protection.tripped);assertTrue(gridLoad.p>29000)
        assertEquals(generator.p,gridLoad.p+parts.filterIsInstance<Resistor>().sumOf { it.p },1e-5)
    }

}
