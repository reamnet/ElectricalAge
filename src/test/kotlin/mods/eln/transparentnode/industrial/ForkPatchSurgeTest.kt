package mods.eln.transparentnode.industrial

import mods.eln.sim.IProcess
import mods.eln.sim.mna.RootSystem
import mods.eln.sim.mna.component.*
import mods.eln.sim.mna.state.State
import mods.eln.sim.mna.state.VoltageState
import net.minecraft.nbt.CompoundTag
import org.junit.Assert.*
import org.junit.Test
import java.io.*

class ForkPatchSurgeTest {
    private class Circuit(phases:Int=1) {
        val root=RootSystem(.05,1);val parts=ArrayList<Component>();val processes=ArrayList<IProcess>()
        val pins=List(phases) {VoltageState()};var active=true
        val bank=SurgeBank(pins,parts,processes,480.0) {active}
        val feeds=pins.mapIndexed { i,p->VoltageSource("surge$i",p,null).setU(480.0)}
        init {pins.forEach(root::addState);parts.forEach(root::addComponent);feeds.forEach(root::addComponent)}
        fun tick(n:Int=1) {repeat(n) {root.step();processes.forEach {it.process(.05)}}}
    }
    @Test fun healthyShuntsRemainOpenAndDoNotDirtyNativeLuFactors() {
        val c=Circuit(3);c.tick(5);val versions=c.root.systems().map {it.matrixVersion};c.tick(100)
        assertEquals(versions,c.root.systems().map {it.matrixVersion});assertTrue(c.feeds.sumOf {it.p}<1e-6);assertEquals(SurgeBank.CAPACITY,c.bank.remaining,1e-6)
    }
    @Test fun electricalSurgeUsesRealResistiveEnergyAndExhaustionFailsOpen() {
        val c=Circuit();c.feeds[0].u=480*1.4;c.tick(2);assertTrue(c.bank.remaining<200000);assertTrue(c.bank.shunts[0].p>900000)
        c.tick(20);assertEquals(0.0,c.bank.remaining,0.0);assertTrue(c.bank.shunts[0].p<1e-6)
        assertTrue(c.bank.absorbedJoules>=200000 && c.bank.absorbedJoules<=200000+980000*.05+1e-6)
    }
    @Test fun finalPoweredIntervalIsNotTruncatedOrCreditedTwice() {
        val c=Circuit();c.feeds[0].u=672.0;var expected=0.0
        repeat(20) {c.root.step();expected+=c.feeds[0].p*.05;c.processes.forEach {it.process(.05)}}
        assertEquals(expected,c.bank.absorbedJoules,1e-6)
        val energy=c.bank.absorbedJoules;repeat(100) {c.bank.prepare()};assertEquals(energy,c.bank.absorbedJoules,0.0)
        for(dt in listOf(0.0,-1.0,Double.NaN,Double.POSITIVE_INFINITY))c.bank.step(dt)
        assertEquals(energy,c.bank.absorbedJoules,0.0)
    }
    @Test fun lightningDebitsExternalEnergyWithoutCreatingASecondHealthyGridLoad() {
        val c=Circuit();c.tick(3)
        assertTrue(c.bank.lightning());assertEquals(160000.0,c.bank.remaining,1e-6);c.tick(6)
        assertEquals(160000.0,c.bank.remaining,1e-6);assertTrue(c.feeds[0].p<1e-6)
        repeat(4) {assertTrue(c.bank.lightning())};assertFalse(c.bank.lightning());assertEquals(0.0,c.bank.remaining,0.0)
    }
    @Test fun inactiveOrNearlySpentProtectionCannotAbsorbLightning() {
        val c=Circuit();c.active=false;assertFalse(c.bank.lightning());assertEquals(200000.0,c.bank.remaining,0.0)
        c.active=true;c.bank.restore(39999.0);assertFalse(c.bank.lightning());assertEquals(39999.0,c.bank.remaining,0.0)
    }
    @Test fun sharedCartridgeUsesIndependentNativePhaseShuntsWithoutCrossFeeding() {
        val c=Circuit(3);c.feeds[0].u=672.0;c.feeds[1].u=480.0;c.feeds[2].u=-480.0;c.tick(2)
        assertEquals(672.0,c.pins[0].u,1e-8);assertEquals(480.0,c.pins[1].u,1e-8);assertEquals(-480.0,c.pins[2].u,1e-8)
        assertEquals(c.feeds.sumOf {it.p},c.bank.shunts.sumOf {it.p},1e-6)
        assertEquals(660000.0,c.bank.shunts.sumOf {it.p},1e-5)
    }
    @Test fun savedPartialAndSpentCapacityCannotRefreshOnRead() {
        val c=Circuit();c.bank.lightning();val t=CompoundTag();c.bank.write(t,"industrialSurge")
        val d=Circuit();d.bank.read(t,"industrialSurge");assertEquals(160000.0,d.bank.remaining,0.0)
        t.putDouble("industrialSurge",0.0);repeat(3) {d.bank.read(t,"industrialSurge")};assertEquals(0.0,d.bank.remaining,0.0)
        for(v in listOf(Double.NaN,Double.POSITIVE_INFINITY,-1.0)) {t.putDouble("industrialSurge",v);d.bank.read(t,"industrialSurge");assertEquals(0.0,d.bank.remaining,0.0)}
    }
    @Test fun standaloneSettingsAndEnablePacketsCannotRefillSpentCartridge() {
        val states=ArrayList<State>();val parts=ArrayList<Component>();val processes=ArrayList<IProcess>();var spent=false
        val u=SurgeArresterUnit(states,parts,processes,{0.0},{spent=it});val t=CompoundTag().also(u::write);t.putDouble("industrialSurge",0.0);u.read(t)
        assertTrue(spent);u.setEnabled(false);u.setEnabled(true);assertEquals(0.0,u.bank.remaining,0.0)
        fun packet(v:Double):Boolean {val b=ByteArrayOutputStream();DataOutputStream(b).use {it.writeDouble(v)};return u.packet(80,DataInputStream(ByteArrayInputStream(b.toByteArray())))}
        assertFalse(packet(Double.NaN));assertEquals(12470.0,u.bank.nominal,0.0);assertTrue(packet(480.0));assertEquals(0.0,u.bank.remaining,0.0)
        assertFalse(u.packet(80,DataInputStream(ByteArrayInputStream(byteArrayOf()))));assertEquals(480.0,u.bank.nominal,0.0)
    }
    @Test fun standaloneInhibitPreventsExternalStrikeConsumptionAndShuntDraw() {
        val states=ArrayList<State>();val parts=ArrayList<Component>();val processes=ArrayList<IProcess>();var signal=1.0
        val u=SurgeArresterUnit(states,parts,processes,{signal});assertFalse(u.lightning());assertEquals(200000.0,u.bank.remaining,0.0)
        signal=Double.NaN;assertFalse(u.lightning());signal=0.0;assertTrue(u.lightning());assertEquals(160000.0,u.bank.remaining,0.0)
    }
}
