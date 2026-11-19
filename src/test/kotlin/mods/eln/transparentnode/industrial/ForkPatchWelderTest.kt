package mods.eln.transparentnode.industrial

import mods.eln.sim.IProcess
import mods.eln.sim.mna.RootSystem
import mods.eln.sim.mna.component.*
import mods.eln.sim.mna.state.State
import net.minecraft.nbt.CompoundTag
import org.junit.Assert.*
import org.junit.Test
import java.io.*

class ForkPatchWelderTest {
    private class Job:WeldJob {
        override var identity:Any?=Any()
        var valid=true;var blocked=false;var count=0
        override fun ready()=valid && !blocked && identity!=null
        override fun complete():Boolean { if(!ready())return false;count++;blocked=true;return true }
    }
    private class Circuit {
        val root=RootSystem(.05,1);val states=ArrayList<State>();val parts=ArrayList<Component>();val processes=ArrayList<IProcess>()
        val job=Job();var loaded=true;var control=0.0
        val u=WelderUnit(states,parts,processes,job,{loaded},{control})
        val feed=VoltageSource("welder supply",u.input,null).setU(240.0)
        init { u.prepare();states.forEach(root::addState);parts.forEach(root::addComponent);root.addComponent(feed) }
        fun tick(n:Int=1) { repeat(n) { root.step();processes.forEach { it.process(.05) } } }
    }
    @Test fun cycleRequires120kJAndBlockedOutputCannotDuplicate() {
        val c=Circuit();c.tick(399);assertEquals(0,c.job.count);assertEquals(119700.0,c.u.workJoules,1e-6)
        c.tick(2);assertEquals(1,c.job.count);assertEquals(0.0,c.u.workJoules,0.0)
        c.tick(500);assertEquals(1,c.job.count);assertTrue(c.u.watts<1e-8)
        c.job.blocked=false;c.u.prepare();c.tick(400);assertEquals(2,c.job.count)
    }
    @Test fun voltageSagReducesRealWorkAndMeterAccountsOnlyAcceptedIntervals() {
        val c=Circuit();c.feed.u=120.0;c.tick(400)
        assertEquals(30000.0,c.u.workJoules,1e-6);assertEquals(30000.0,c.u.lifetimeJoules,1e-6)
        assertEquals(1500.0,c.u.watts,1e-6);repeat(100) { c.u.prepare() }
        assertEquals(30000.0,c.u.workJoules,1e-6)
        for(dt in listOf(0.0,-1.0,Double.NaN,Double.POSITIVE_INFINITY))c.u.step(dt)
        assertEquals(30000.0,c.u.workJoules,1e-6)
    }
    @Test fun differentWorkpieceCannotReuseEnergyEvenIfChangedBetweenPrepareAndStep() {
        val c=Circuit();c.tick(200);assertEquals(60000.0,c.u.workJoules,1e-6)
        c.job.identity=Any();c.tick();assertEquals(0.0,c.u.workJoules,0.0)
        c.tick(399);assertEquals(0,c.job.count);c.tick();assertEquals(1,c.job.count)
    }
    @Test fun disableInhibitUnloadedAndOvervoltageOpenWithoutLosingExistingWork() {
        val c=Circuit();c.tick(100);val work=c.u.workJoules
        c.u.setEnabled(false);c.tick(3);assertFalse(c.u.working);assertEquals(work,c.u.workJoules,0.0)
        c.u.setEnabled(true);c.control=1.0;c.u.prepare();c.tick(3);assertFalse(c.u.working)
        c.control=Double.NaN;c.tick(3);assertFalse(c.u.working)
        c.control=0.0;c.loaded=false;c.tick(3);assertFalse(c.u.working)
        c.loaded=true;c.feed.u=400.0;c.tick(3);assertFalse(c.u.working);assertTrue(c.u.watts<1e-8)
        c.feed.u=240.0;c.tick(3);assertTrue(c.u.working);assertEquals(6000.0,c.u.watts,1e-6)
    }
    @Test fun progressResumesOnlyForVerifiedSavedWorkpieceAndNeverViaSettings() {
        val c=Circuit();c.tick(200);val t=CompoundTag().also(c.u::write)
        val d=Circuit();d.u.read(t,true);assertEquals(60000.0,d.u.workJoules,1e-6)
        d.u.read(t,false);assertEquals(0.0,d.u.workJoules,0.0)
        val settings=CompoundTag().also(c.u::writeSettings);assertFalse(settings.contains("industrialWork"));assertFalse(settings.contains("industrialLifetime"))
        t.putDouble("industrialWork",Double.NaN);t.putDouble("industrialLifetime",Double.POSITIVE_INFINITY);d.u.read(t)
        assertEquals(0.0,d.u.workJoules,0.0);assertEquals(0.0,d.u.lifetimeJoules,0.0)
    }
    @Test fun settingsPacketsRejectTruncatedNonfiniteAndOutOfRangeValuesAtomically() {
        val c=Circuit()
        fun send(p:DoubleArray):Boolean { val b=ByteArrayOutputStream();DataOutputStream(b).use { s->p.forEach(s::writeDouble) };return c.u.packet(80,DataInputStream(ByteArrayInputStream(b.toByteArray()))) }
        for(p in listOf(doubleArrayOf(120.0),doubleArrayOf(240.0,Double.NaN),doubleArrayOf(480.0,999.0),doubleArrayOf(481.0,2000.0))) {
            assertFalse(send(p));assertEquals(240.0,c.u.nominal,0.0);assertEquals(6000.0,c.u.demand,0.0)
        }
        assertTrue(send(doubleArrayOf(480.0,20000.0)));assertEquals(20000.0,c.u.demand,0.0)
    }
    @Test fun steadyWorkRetainsNativeLuFactors() {
        val c=Circuit();c.tick(5);val versions=c.root.systems().map { it.matrixVersion };c.tick(100)
        assertEquals(versions,c.root.systems().map { it.matrixVersion })
    }
}
