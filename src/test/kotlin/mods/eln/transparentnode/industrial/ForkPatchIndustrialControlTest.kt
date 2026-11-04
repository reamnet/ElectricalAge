package mods.eln.transparentnode.industrial

import mods.eln.sim.IProcess
import mods.eln.sim.mna.RootSystem
import mods.eln.sim.mna.component.*
import mods.eln.sim.mna.state.State
import net.minecraft.nbt.CompoundTag
import org.junit.Test
import org.junit.Assert.*
import java.io.*

/** Production passive circuits, not copies of the state machines. */
class ForkPatchIndustrialControlTest {
    private class Circuit(kind: IndustrialKind=IndustrialKind.RELAY,volts: Double=kind.nominal) {
        val root=RootSystem(.05,1)
        val states=ArrayList<State>();val parts=ArrayList<Component>();val processes=ArrayList<IProcess>()
        var control=0.0;var trip=false
        val u=IndustrialControlUnit(kind,states,parts,processes,{control},{trip=it})
        val source=VoltageSource("supply",u.input,null).apply { this.u=volts }
        val loads=u.outputs.map { Resistor(it,null).setR(1e19) }
        init { states.forEach(root::addState);parts.forEach(root::addComponent);root.addComponent(source);loads.forEach(root::addComponent) }
        fun tick(count: Int=1) { repeat(count) { root.step();processes.forEach { p->p.process(.05) } } }
        fun paired() { root.addComponent(VoltageSource("neutral",u.returnIn,null).apply { u=0.0 });root.addComponent(Resistor(u.outputs[0],u.returnOut).setR(24.0)) }
    }
    @Test fun delayedOvercurrentOpensBothConductorsAndLatchesUntilExplicitReset() {
        val c=Circuit();c.u.applySettings(doubleArrayOf(480.0,10.0,.8,1.2,.5,0.0));c.loads[0].r=24.0
        c.tick(8);assertFalse(c.u.protection.tripped);c.tick(3);assertTrue(c.trip);assertEquals(2,c.u.protection.reason)
        assertFalse(c.u.contacts[0].closed);assertFalse(c.u.returnContact.closed);c.tick();assertEquals(0.0,c.loads[0].p,1e-8)
        c.loads[0].r=96.0;c.u.setEnabled(false);c.u.setEnabled(true);c.tick(5);assertTrue(c.trip)
        c.u.resetTrip();c.tick(5);assertFalse(c.trip);assertTrue(c.loads[0].p>2399)
    }
    @Test fun pairedConductorsCarryLoadAndAnActualGroundLeakTripsDifferential() {
        val c=Circuit();c.u.applySettings(doubleArrayOf(480.0,100.0,.8,1.2,.1,.1));c.paired();c.tick(10)
        assertFalse(c.trip);assertEquals(-c.u.contacts[0].current,c.u.returnContact.current,1e-6)
        c.loads[0].r=4800.0;c.tick(4);assertFalse(c.trip)
        c.loads[0].r=2400.0;c.tick(4);assertTrue(c.trip);assertEquals(3,c.u.protection.reason)
    }
    @Test fun undervoltageArmsOnlyAfterSupplyIsEstablishedAndUsesPhysicalDelay() {
        val c=Circuit(volts=0.0);c.tick(100);assertFalse(c.trip);assertFalse(c.u.protection.armed)
        c.source.u=480.0;c.tick(2);assertTrue(c.u.protection.armed)
        c.source.u=300.0;c.tick(8);assertFalse(c.trip);c.tick(3);assertTrue(c.trip);assertEquals(1,c.u.protection.reason)
    }
    @Test fun overvoltageTripsAndRemoteSignalLatchesPhysicalContacts() {
        val c=Circuit(volts=600.0);c.tick(11);assertTrue(c.trip);assertEquals(1,c.u.protection.reason)
        c.source.u=480.0;c.u.resetTrip();c.tick(3);assertFalse(c.trip)
        c.control=1.0;c.tick();assertTrue(c.trip);assertEquals(5,c.u.protection.reason)
        c.u.resetTrip();c.tick();assertTrue(c.trip);c.control=0.0;c.u.resetTrip();c.tick();assertFalse(c.trip)
    }
    @Test fun healthyCircuitsReuseNativeLuFactorsRatherThanOpeningAndReclosingContacts() {
        for(k in listOf(IndustrialKind.RELAY,IndustrialKind.SHEDDER)) {
            val c=Circuit(k);c.loads[0].r=240.0;c.tick(5)
            val before=c.root.systems().map { it.matrixVersion };c.tick(100)
            assertEquals(k.toString(),before,c.root.systems().map { it.matrixVersion })
        }
    }
    @Test fun sheddingDropsP4FirstAndNeverReclosesAKnownOversizedBranch() {
        val c=Circuit(IndustrialKind.SHEDDER);assertTrue(c.u.applySettings(doubleArrayOf(240.0,1000.0)))
        c.loads.forEachIndexed { i,r->r.r=if(i==3)72.0 else 576.0 };c.tick(30)
        assertArrayEquals(booleanArrayOf(true,true,true,false),c.u.shed.closed)
        assertEquals(800.0,c.u.shed.nominalDemand[3],.1);c.tick(300)
        assertFalse(c.u.shed.closed[3]);assertEquals("HEADROOM",c.u.shed.status)
        c.u.applySettings(doubleArrayOf(240.0,1500.0));c.tick(102);assertTrue(c.u.shed.closed.all { it });assertEquals(1100.0,c.u.inputWatts,.2)
    }
    @Test fun eachPhysicalBranchIsMeasuredAndShedEnergyDoesNotBecomeInputPower() {
        val c=Circuit(IndustrialKind.SHEDDER);val watts=doubleArrayOf(100.0,200.0,300.0,400.0)
        c.loads.forEachIndexed { i,r->r.r=240.0*240.0/watts[i] };c.tick(4)
        watts.forEachIndexed { i,w->assertEquals(w,c.u.branchWatts[i],.1) }
        assertEquals(c.u.inputWatts,c.loads.sumOf { it.p }+c.u.contacts.sumOf { it.power },1e-6)
        c.u.setEnabled(false);c.tick();assertEquals(0.0,c.u.inputWatts,1e-8);assertTrue(c.loads.all { it.p<1e-8 })
    }
    @Test fun undervoltageShedsInReversePriorityAndHealthyReturnRestoresSlowly() {
        val c=Circuit(IndustrialKind.SHEDDER,190.0);c.tick(21);assertArrayEquals(booleanArrayOf(true,true,true,false),c.u.shed.closed)
        c.tick(20);assertArrayEquals(booleanArrayOf(true,true,false,false),c.u.shed.closed)
        c.source.u=240.0;c.tick(99);assertFalse(c.u.shed.closed[2]);c.tick(2);assertTrue(c.u.shed.closed[2]);assertFalse(c.u.shed.closed[3]);c.tick(101);assertTrue(c.u.shed.closed.all { it })
    }
    @Test fun disableAndWiredInhibitOpenAllBranchesAndReenableRestoresP1First() {
        val c=Circuit(IndustrialKind.SHEDDER);c.control=1.0;c.tick();assertTrue(c.u.contacts.none { it.closed })
        c.control=0.0;c.tick(99);assertTrue(c.u.contacts.none { it.closed });c.tick(2);assertTrue(c.u.contacts[0].closed);assertTrue(c.u.contacts.drop(1).none { it.closed })
        c.u.setEnabled(false);assertTrue(c.u.contacts.none { it.closed });c.tick();assertEquals("DISABLED",c.u.shed.status)
    }
    @Test fun retryForgetsShedDemandButDoesNotInstantlyCloseBranches() {
        val c=Circuit(IndustrialKind.SHEDDER);c.u.applySettings(doubleArrayOf(240.0,1000.0));c.loads[3].r=48.0;c.tick(130)
        assertEquals("HEADROOM",c.u.shed.status);c.loads[3].r=576.0;c.u.retryShed();assertFalse(c.u.contacts[3].closed)
        c.tick(99);assertFalse(c.u.contacts[3].closed);c.tick(3);assertTrue(c.u.contacts[3].closed)
    }
    @Test fun settingsPacketsAreAtomicAndRejectTruncatedNonfiniteOrInvalidArrays() {
        val c=Circuit();fun packet(n: Int,values: DoubleArray)=c.u.packet(n.toByte(),DataInputStream(ByteArrayInputStream(ByteArrayOutputStream().also { b->DataOutputStream(b).use { s->values.forEach(s::writeDouble) } }.toByteArray())))
        val p=c.u.settings.copyOf();p[1]=25.0;assertFalse(packet(80,p.copyOf(3)));assertArrayEquals(IndustrialKind.RELAY.defaults,c.u.settings,0.0)
        p[0]=Double.NaN;assertFalse(packet(80,p));p[0]=480.0;assertTrue(packet(80,p));assertEquals(25.0,c.u.settings[1],0.0)
    }
    @Test fun savesPreserveTripLatchesPendingDelayAndShedDemandWithoutClosingFaults() {
        val c=Circuit();c.source.u=600.0;c.tick(4);val saved=CompoundTag().also(c.u::write)
        val d=Circuit();d.source.u=600.0;d.u.read(saved);d.tick(7);assertTrue(d.trip)
        val trip=CompoundTag().also(d.u::write);val copy=Circuit();copy.u.read(trip);assertFalse(copy.u.contacts[0].closed);assertEquals(1,copy.u.protection.reason)
        val s=Circuit(IndustrialKind.SHEDDER);s.u.applySettings(doubleArrayOf(240.0,1000.0));s.loads[3].r=48.0;s.tick(30)
        val t=CompoundTag().also(s.u::write);val next=Circuit(IndustrialKind.SHEDDER);next.u.read(t);next.tick(150);assertFalse(next.u.contacts[3].closed);assertEquals("HEADROOM",next.u.shed.status)
    }
}
