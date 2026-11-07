package mods.eln.transparentnode.industrial

import mods.eln.sim.IProcess
import mods.eln.sim.mna.RootSystem
import mods.eln.sim.mna.component.*
import mods.eln.sim.mna.state.State
import net.minecraft.nbt.CompoundTag
import org.junit.Assert.*
import org.junit.Test
import java.io.*

class ForkPatchChargerTest {
    private class Pack(override val capacity: Double=72000000.0,override val chargeWatts: Double=11520.0):ChargeTarget {
        override val identity=Any();override var energy=0.0
    }
    private class Circuit {
        val root=RootSystem(.05,1);val states=ArrayList<State>();val parts=ArrayList<Component>();val processes=ArrayList<IProcess>()
        var pack:Pack?=Pack();var time=0L;var loaded=true;var control=0.0
        val u=ChargerUnit(states,parts,processes,{pack},{time},{loaded},{control})
        val feed=VoltageSource("charger supply",u.input,null).setU(240.0)
        init { u.prepare();states.forEach(root::addState);parts.forEach(root::addComponent);root.addComponent(feed) }
        fun tick(n:Int=1) { repeat(n) { root.step();processes.forEach { it.process(.05) } } }
    }
    @Test fun stores94PercentOfAcceptedPowerExactlyOnceWithNoPrepareSideEffects() {
        val c=Circuit();c.tick(20)
        assertEquals(6768.0,c.pack!!.energy,1e-6);assertEquals(7200.0,c.u.lifetimeJoules,1e-6)
        assertEquals(c.pack!!.energy,c.u.sessionJoules,1e-8)
        repeat(100) { c.u.prepare() };assertEquals(6768.0,c.pack!!.energy,1e-6)
        listOf(0.0,-1.0,Double.NaN,Double.POSITIVE_INFINITY).forEach(c.u::step)
        assertEquals(6768.0,c.pack!!.energy,1e-6)
    }
    @Test fun sagIsResistiveAndOverNominalSupplyCannotExceedPackChargeRating() {
        val c=Circuit();c.feed.u=120.0;c.tick(20);assertEquals(1692.0,c.pack!!.energy,1e-6)
        assertTrue(c.u.applySettings(doubleArrayOf(240.0,50000.0,0.0,0.0)))
        c.feed.u=300.0;val before=c.pack!!.energy;c.tick(20)
        assertEquals(11520.0,c.pack!!.energy-before,1e-6);assertEquals(18000.0,c.u.watts,1e-6)
    }
    @Test fun nearFullPackNeverOverfillsAndThenOpens() {
        val c=Circuit();c.pack!!.energy=c.pack!!.capacity-1.0;c.tick(3)
        assertEquals(c.pack!!.capacity,c.pack!!.energy,0.0);assertFalse(c.u.working);assertTrue(c.u.watts<1e-8)
    }
    @Test fun replacingPackResetsSessionWithoutTransferringStoredEnergy() {
        val c=Circuit();c.tick(20);val old=c.pack!!;c.pack=Pack();c.u.prepare()
        assertEquals(0.0,c.u.sessionJoules,0.0);assertEquals(0.0,c.pack!!.energy,0.0)
        c.tick(20);assertEquals(6768.0,old.energy,1e-6);assertEquals(6768.0,c.pack!!.energy,1e-6)
        c.pack=null;c.u.prepare();c.tick(2);assertTrue(c.u.watts<1e-8);assertEquals(0.0,c.u.sessionJoules,0.0)
    }
    @Test fun overnightScheduleAndPhysicalInhibitsRecover() {
        val c=Circuit();c.u.applySettings(doubleArrayOf(240.0,7200.0,22.0,6.0))
        for((time,on) in listOf(23000L to true,12000L to false,18000L to true,0L to false,-24000L to false)) {
            c.time=time;c.u.prepare();c.tick(3);assertEquals(on,c.u.working);assertEquals(if(on)7200.0 else 0.0,c.u.watts,1e-6)
        }
        c.time=18000L
        for(control in listOf(1.0,Double.NaN)) { c.control=control;c.tick(3);assertFalse(c.u.working);assertTrue(c.u.watts<1e-8) }
        c.control=0.0;c.loaded=false;c.tick(3);assertFalse(c.u.working)
        c.loaded=true;c.feed.u=400.0;c.tick(3);assertFalse(c.u.working);assertTrue(c.u.watts<1e-8)
        c.feed.u=240.0;c.tick(3);assertTrue(c.u.working)
    }
    @Test fun packetsAreAtomicAndWorldSessionSurvivesInitialBinding() {
        val c=Circuit();c.tick(20)
        fun send(p:DoubleArray):Boolean { val bytes=ByteArrayOutputStream();DataOutputStream(bytes).use { s->p.forEach(s::writeDouble) };return c.u.packet(80,DataInputStream(ByteArrayInputStream(bytes.toByteArray()))) }
        for(p in listOf(doubleArrayOf(120.0),doubleArrayOf(240.0,50000.0,24.0,0.0),doubleArrayOf(240.0,Double.NaN,0.0,0.0))) {
            assertFalse(send(p));assertEquals(7200.0,c.u.demand,0.0)
        }
        val t=CompoundTag().also(c.u::write);val d=Circuit();d.u.read(t)
        assertEquals(c.u.sessionJoules,d.u.sessionJoules,0.0)
        assertEquals(c.u.lifetimeJoules,d.u.lifetimeJoules,0.0)
        val settings=CompoundTag().also(c.u::writeSettings);assertFalse(settings.contains("industrialSession"));assertFalse(settings.contains("industrialLifetime"))
        t.putDouble("industrialSession",Double.NaN);t.putDouble("industrialLifetime",Double.POSITIVE_INFINITY);d.u.read(t)
        assertEquals(0.0,d.u.sessionJoules,0.0);assertEquals(0.0,d.u.lifetimeJoules,0.0)
    }
    @Test fun steadyChargeKeepsNativeLuFactors() {
        val c=Circuit();c.tick(5);val versions=c.root.systems().map { it.matrixVersion };c.tick(100)
        assertEquals(versions,c.root.systems().map { it.matrixVersion })
    }
}
