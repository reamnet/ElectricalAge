package mods.eln.transparentnode.industrial

import mods.eln.sim.IProcess
import mods.eln.sim.mna.RootSystem
import mods.eln.sim.mna.component.*
import mods.eln.sim.mna.state.State
import net.minecraft.nbt.CompoundTag
import org.junit.Assert.*
import org.junit.Test
import java.io.*

class ForkPatchLoadBankTest {
    private class Circuit(volts: Double=480.0) {
        val root=RootSystem(.05,1)
        val states=ArrayList<State>();val parts=ArrayList<Component>();val processes=ArrayList<IProcess>()
        var loaded=true;var control=0.0
        val u=LoadBankUnit(states,parts,processes,{loaded},{control})
        val feed=VoltageSource("load bank supply",u.input,null).setU(volts)
        init { states.forEach(root::addState);parts.forEach(root::addComponent);root.addComponent(feed) }
        fun tick(n: Int=1) { repeat(n) { root.step();processes.forEach { it.process(.05) } } }
    }
    @Test fun nominalSagAndReversePolarityDrawPassiveResistivePower() {
        val c=Circuit();c.tick(2);assertEquals(50000.0,c.u.watts,1e-6)
        c.feed.u=240.0;c.tick(2);assertEquals(12500.0,c.u.watts,1e-6)
        c.feed.u=0.0;c.tick(2);assertEquals(0.0,c.u.watts,0.0)
        c.feed.u=-480.0;c.tick(2);assertEquals(50000.0,c.u.watts,1e-6)
        assertEquals(c.feed.p,c.u.watts,1e-6)
    }
    @Test fun allValidDemandSettingsRemainPassiveIncludingLowVoltageResistanceFloor() {
        val c=Circuit()
        for(v in listOf(12.0,120.0,480.0,12470.0,230000.0))for(w in listOf(0.0,100.0,50000.0,1e6)) {
            c.feed.u=v;assertTrue(c.u.applySettings(doubleArrayOf(v,w)));c.tick(3)
            val r=IndustrialEnergy.resistance(v,w)
            assertEquals(v*v/r,c.u.watts,1e-4);assertTrue(c.u.watts<=w+1e-5)
        }
    }
    @Test fun acceptedInputEnergyIsMeteredExactlyOnceIncludingLastHotInterval() {
        val c=Circuit();var expected=0.0
        for(v in listOf(480.0,240.0,500.0,0.0,-480.0)) {
            c.feed.u=v
            repeat(10) { c.root.step();expected+=c.feed.p*.05;c.processes.forEach { it.process(.05) } }
        }
        assertEquals(expected,c.u.lifetimeJoules,1e-5)
        val before=c.u.lifetimeJoules;val temp=c.u.temperature
        repeat(100) { c.u.prepare() };assertEquals(before,c.u.lifetimeJoules,0.0);assertEquals(temp,c.u.temperature,0.0)
        for(dt in listOf(0.0,-1.0,Double.NaN,Double.POSITIVE_INFINITY))c.u.step(dt)
        assertEquals(before,c.u.lifetimeJoules,0.0)
    }
    @Test fun disabledSignalAndUnloadedDevicesOpenAndRecoverWithoutProbes() {
        val c=Circuit();c.tick(2)
        c.u.setEnabled(false);c.tick(2);assertTrue(c.u.watts<1e-8)
        c.u.setEnabled(true);c.tick(2);assertEquals(50000.0,c.u.watts,1e-6)
        c.control=1.0;c.tick(3);assertTrue(c.u.watts<1e-8)
        c.control=Double.NaN;c.tick(3);assertTrue(c.u.watts<1e-8)
        c.control=0.0;c.loaded=false;c.tick(3);assertTrue(c.u.watts<1e-8)
        c.loaded=true;c.tick(3);assertEquals(50000.0,c.u.watts,1e-6)
    }
    @Test fun overvoltageDisconnectsAndHealthySupplyRestores() {
        val c=Circuit();c.feed.u=700.0;c.tick(3);assertFalse(c.u.working);assertTrue(c.u.watts<1e-8)
        c.feed.u=480.0;c.tick(3);assertTrue(c.u.working);assertEquals(50000.0,c.u.watts,1e-6)
    }
    @Test fun sourceFanCoolingReachesStableEquilibriumAndRestoresAHotSavedCase() {
        val c=Circuit();c.tick(2000);assertEquals(65.0,c.u.temperature,.01)
        val t=CompoundTag().also(c.u::write);t.putDouble("industrialTemperature",130.0);c.u.read(t)
        assertFalse(c.u.working);c.tick(2);assertTrue(c.u.watts<1e-8)
        c.tick(120);assertTrue(c.u.working);assertTrue(c.u.temperature<120);assertEquals(50000.0,c.u.watts,1e-6)
    }
    @Test fun settingAndPacketValidationIsAtomic() {
        val c=Circuit()
        fun packet(values: DoubleArray): Boolean {
            val b=ByteArrayOutputStream();DataOutputStream(b).use { s->values.forEach(s::writeDouble) }
            return c.u.packet(80,DataInputStream(ByteArrayInputStream(b.toByteArray())))
        }
        for(p in listOf(doubleArrayOf(240.0),doubleArrayOf(Double.NaN,100.0),doubleArrayOf(480.0,-1.0),doubleArrayOf(480.0,Double.POSITIVE_INFINITY),doubleArrayOf(480.0,1000001.0))) {
            assertFalse(packet(p));assertEquals(480.0,c.u.nominal,0.0);assertEquals(50000.0,c.u.demand,0.0)
        }
        assertTrue(packet(doubleArrayOf(240.0,3000.0)));assertEquals(240.0,c.u.nominal,0.0)
        assertFalse(c.u.packet(81,DataInputStream(ByteArrayInputStream(byteArrayOf()))));assertTrue(c.u.enabled)
    }
    @Test fun savedHeatAndMeterHistoryRoundTripAndRejectNonfiniteData() {
        val c=Circuit();c.tick(200);val t=CompoundTag().also(c.u::write);val d=Circuit();d.u.read(t)
        assertEquals(c.u.temperature,d.u.temperature,0.0);assertEquals(c.u.lifetimeJoules,d.u.lifetimeJoules,0.0)
        t.putDouble("industrialTemperature",Double.NaN);t.putDouble("industrialLifetime",Double.POSITIVE_INFINITY)
        d.u.read(t);assertEquals(20.0,d.u.temperature,0.0);assertEquals(0.0,d.u.lifetimeJoules,0.0)
    }
    @Test fun steadyLoadAndRepeatedPrepareKeepCachedNativeLuFactors() {
        val c=Circuit();c.tick(5);val versions=c.root.systems().map { it.matrixVersion }
        c.tick(100);assertEquals(versions,c.root.systems().map { it.matrixVersion })
        c.u.setEnabled(false);c.tick(3);val off=c.root.systems().map { it.matrixVersion }
        c.tick(100);assertEquals(off,c.root.systems().map { it.matrixVersion })
    }
}
