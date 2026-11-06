package mods.eln.transparentnode.industrial

import mods.eln.sim.IProcess
import mods.eln.sim.mna.RootSystem
import mods.eln.sim.mna.component.*
import mods.eln.sim.mna.state.State
import net.minecraft.nbt.CompoundTag
import org.junit.Assert.*
import org.junit.Test
import java.io.*

class ForkPatchMotorDriveTest {
    private class Circuit {
        val root=RootSystem(.05,1);val states=ArrayList<State>();val parts=ArrayList<Component>();val processes=ArrayList<IProcess>()
        var inhibit=0.0;var trip=false
        val u=MotorDriveUnit(states,parts,processes,{inhibit},{trip=it})
        val feed=VoltageSource("drive supply",u.input,null).setU(480.0)
        val load=Resistor(u.output,null).setR(24.0)
        init { states.forEach(root::addState);parts.forEach(root::addComponent);root.addComponent(feed);root.addComponent(load) }
        fun tick(n:Int=1) { repeat(n) { root.step();processes.forEach { it.process(.05) } } }
        fun settings(hz:Double=30.0)=u.applySettings(doubleArrayOf(480.0,480.0,hz,5.0,100.0))
    }
    @Test fun physicalRampAndLoadedOutputMatchSourceWithoutInventingPower() {
        val c=Circuit();assertTrue(c.settings());c.tick(20);assertEquals(12.0,c.u.hertz,1e-8)
        c.tick(40);assertEquals(30.0,c.u.hertz,1e-8);c.tick()
        assertEquals(200.0,c.u.output.u,1e-6);assertEquals(30.0,c.u.hertz,1e-8)
        assertEquals(c.feed.p,c.load.p+c.u.winding.p+c.u.references.sumOf { it.p },1e-6)
    }
    @Test fun nominalShortCurrentIsBoundedByPhysicalOutputImpedanceAtEveryFrequency() {
        for(hz in listOf(0.0,15.0,30.0,60.0)) {
            val c=Circuit();c.settings(hz);c.load.r=.001;c.tick(110)
            assertTrue(c.u.outputCurrent<=100.0*hz/60+1e-6)
            assertEquals(c.feed.p,c.load.p+c.u.winding.p+c.u.references.sumOf { it.p },1e-5)
        }
    }
    @Test fun disableCoastsDownRatherThanOpeningInstantlyAndInhibitUsesTheSameRamp() {
        val c=Circuit();c.settings();c.tick(62);c.u.setEnabled(false);c.tick(10)
        assertEquals(24.0,c.u.hertz,1e-8);assertTrue(c.u.tap.enabled);c.tick(50);assertEquals(0.0,c.u.hertz,0.0);assertFalse(c.u.tap.enabled)
        c.u.setEnabled(true);c.tick(60);assertEquals(30.0,c.u.hertz,1e-8)
        c.inhibit=1.0;c.tick(10);assertEquals(24.0,c.u.hertz,1e-8)
        c.inhibit=Double.NaN;c.tick(50);assertFalse(c.u.tap.enabled)
    }
    @Test fun inputSagPassesThroughThePassiveTapAndBackfeedConservesEnergy() {
        val c=Circuit();c.settings();c.tick(62);c.feed.u=432.0;c.tick(2);assertEquals(180.0,c.u.output.u,1e-6)
        val other=VoltageSource("coasting motor",c.u.output,null).setU(300.0);c.root.addComponent(other);c.tick()
        assertTrue(c.feed.p<0);assertEquals(c.feed.p+other.p,c.load.p+c.u.winding.p+c.u.references.sumOf { it.p },1e-5)
    }
    @Test fun windingHeatIsAccountedOnceAndThermalFaultLatchesUntilCoolExplicitReset() {
        val c=Circuit();c.settings(60.0);c.load.r=.001
        repeat(1000) { if(!c.u.tripped)c.tick() }
        assertTrue(c.u.temperature>120);assertTrue(c.trip);assertFalse(c.u.tap.enabled)
        c.u.setEnabled(false);c.u.setEnabled(true);assertTrue(c.trip);assertFalse(c.u.resetTrip())
        val t=CompoundTag().also(c.u::write);t.putDouble("industrialTemperature",70.0);c.u.read(t)
        assertTrue(c.trip);assertFalse(c.u.tap.enabled);assertTrue(c.u.resetTrip());assertFalse(c.trip)
        c.load.r=24.0;c.tick(10);assertTrue(c.u.tap.enabled)
    }
    @Test fun exactAcceptedLossAndInputJoulesSurviveTrialPreparationAndTheLastTripInterval() {
        val c=Circuit();c.settings();var temperature=20.0;var joules=0.0
        repeat(80) {
            c.root.step();temperature+=(c.u.winding.p-(temperature-20)*12)*.05/15000
            joules+=kotlin.math.max(0.0,c.u.input.u*c.u.tap.current)*.05
            c.processes.forEach { it.process(.05) }
        }
        assertEquals(temperature,c.u.temperature,1e-9);assertEquals(joules,c.u.lifetimeJoules,1e-6)
        val hertz=c.u.hertz
        for(dt in listOf(0.0,-1.0,Double.NaN,Double.POSITIVE_INFINITY))c.u.step(dt)
        assertEquals(temperature,c.u.temperature,0.0);assertEquals(hertz,c.u.hertz,0.0)
    }
    @Test fun steadyFrequencyKeepsNativeLuFactorsWhileRampOnlyChangesWhenNecessary() {
        val c=Circuit();c.settings();c.tick(70);val versions=c.root.systems().map { it.matrixVersion };c.tick(100)
        assertEquals(versions,c.root.systems().map { it.matrixVersion })
    }
    @Test fun packetsRejectPartialOrNonfiniteSettingsAndPreserveTheCallerArray() {
        val c=Circuit();val values=doubleArrayOf(480.0,240.0,30.0,5.0,50.0);assertTrue(c.u.applySettings(values));values[1]=999.0
        assertEquals(240.0,c.u.settings[1],0.0)
        fun packet(p:DoubleArray):Boolean { val b=ByteArrayOutputStream();DataOutputStream(b).use { s->p.forEach(s::writeDouble) };return c.u.packet(80,DataInputStream(ByteArrayInputStream(b.toByteArray()))) }
        assertFalse(packet(doubleArrayOf(480.0,120.0,30.0)));assertEquals(240.0,c.u.settings[1],0.0)
        for(i in 0..4) { val p=c.u.settings.copyOf();p[i]=Double.NaN;assertFalse(packet(p)) }
        assertFalse(packet(doubleArrayOf(480.0,480.0,61.0,5.0,100.0)))
    }
    @Test fun saveRestoresSettingsHeatMeterAndTripButRestartsFrequencyAtZero() {
        val c=Circuit();c.settings();c.tick(80);val t=CompoundTag().also(c.u::write)
        val d=Circuit();d.u.read(t);assertArrayEquals(c.u.settings,d.u.settings,0.0);assertEquals(c.u.temperature,d.u.temperature,0.0)
        assertEquals(c.u.lifetimeJoules,d.u.lifetimeJoules,0.0);assertEquals(0.0,d.u.hertz,0.0);assertFalse(d.u.tap.enabled)
        t.putDouble("industrialTemperature",Double.NaN);t.putDouble("industrialLifetime",Double.POSITIVE_INFINITY);d.u.read(t)
        assertEquals(20.0,d.u.temperature,0.0);assertEquals(0.0,d.u.lifetimeJoules,0.0)
    }
}
