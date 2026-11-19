package mods.eln.transparentnode.industrial

import mods.eln.sim.IProcess
import mods.eln.sim.mna.RootSystem
import mods.eln.sim.mna.component.*
import mods.eln.sim.mna.state.State
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import org.junit.Assert.*
import org.junit.Test
import java.io.*

class ForkPatchScadaTest {
    private class Circuit {
        val root=RootSystem(.05,1);val states=ArrayList<State>();val parts=ArrayList<Component>();val processes=ArrayList<IProcess>()
        var here:BlockPos?=BlockPos(0,64,0);var loaded=true;var inhibit=0.0;var allow=true
        var reads=0;val calls=ArrayList<Triple<Long,Int,Boolean>>()
        val u=ScadaUnit(states,parts,processes,{here},{loaded},{inhibit},{_,_->reads++;"Actual target"},{a,c,on->calls.add(Triple(a,c,on));allow})
        val feed=VoltageSource("console supply",u.input,null).setU(120.0)
        init { states.forEach(root::addState);parts.forEach(root::addComponent);root.addComponent(feed) }
        fun tick(n:Int=1) { repeat(n) { root.step();processes.forEach { it.process(.05) } } }
        fun bind(x:Double=1.0,circuit:Double=1.0)=u.bind(doubleArrayOf(x,64.0,0.0,circuit))
    }
    @Test fun passiveTwentyWattSupplyFollowsSagAndOpensWhenDisabledOrInhibited() {
        val c=Circuit();c.tick(3);assertEquals(20.0,c.u.resistor.p,1e-8)
        c.feed.u=60.0;c.tick(3);assertEquals(5.0,c.u.resistor.p,1e-8);assertFalse(c.u.canCommand)
        c.u.setEnabled(false);c.tick(2);assertTrue(c.u.resistor.p<1e-8)
        c.u.setEnabled(true);c.feed.u=120.0;c.inhibit=1.0;c.tick(3);assertTrue(c.u.resistor.p<1e-8)
        c.inhibit=0.0;c.tick(3);assertEquals(20.0,c.u.resistor.p,1e-8)
    }
    @Test fun eightExplicitSlotsRememberIndependentAddressesAndCircuits() {
        val c=Circuit()
        for(i in 0..7) { assertTrue(c.u.select(i));assertTrue(c.bind(i+1.0,i+1.0)) }
        for(i in 7 downTo 0) {
            assertTrue(c.u.select(i));assertEquals(BlockPos(i+1,64,0).asLong(),c.u.targets[i]);assertEquals(i,c.u.circuits[i]);assertEquals(i+1.0,c.u.settings[0],0.0)
        }
        assertFalse(c.u.select(8));assertEquals(0,c.u.selected);assertFalse(c.u.select(-1))
    }
    @Test fun bindingChecksSphericalRangeAndInvalidPacketsDoNotPartiallyChangeAddresses() {
        val c=Circuit();assertTrue(c.bind(256.0));val old=c.u.settings.copyOf();val address=c.u.targets[0]
        assertFalse(c.bind(257.0));assertArrayEquals(old,c.u.settings,0.0);assertEquals(address,c.u.targets[0])
        assertTrue(c.u.bind(doubleArrayOf(181.0,64.0,181.0,12.0)))
        assertFalse(c.u.bind(doubleArrayOf(182.0,64.0,182.0,1.0)))
        assertFalse(c.u.bind(doubleArrayOf(Double.NaN,64.0,0.0,1.0)))
        c.here=null;assertFalse(c.bind())
    }
    @Test fun remoteOperationsRequireHealthySupplyEnableLoadedOwnerAndFiniteReleasedSignal() {
        val c=Circuit();assertTrue(c.bind());c.tick(3);assertTrue(c.u.command(false));assertEquals(1,c.calls.size)
        for(v in listOf(0.0,89.0,157.0,1000.0)) { c.feed.u=v;c.tick(3);assertFalse(c.u.command(true));assertEquals(1,c.calls.size) }
        c.feed.u=120.0;c.tick(3)
        c.u.setEnabled(false);assertFalse(c.u.command(true));c.u.setEnabled(true)
        c.loaded=false;assertFalse(c.u.command(true));c.loaded=true
        c.inhibit=1.0;assertFalse(c.u.command(true));c.inhibit=Double.NaN;assertFalse(c.u.command(true));c.inhibit=0.0
        assertTrue(c.u.command(true));assertEquals(2,c.calls.size);assertEquals(Triple(BlockPos(1,64,0).asLong(),0,true),c.calls.last())
    }
    @Test fun telemetryPollingUsesPhysicalTimeOnceAndPausesWhenOwnerIsUnloaded() {
        val c=Circuit();c.bind();c.tick(19);assertEquals(0,c.reads);c.tick();assertEquals(1,c.reads)
        repeat(100) { c.u.canCommand };assertEquals(1,c.reads)
        for(dt in listOf(0.0,-1.0,Double.NaN,Double.POSITIVE_INFINITY))c.u.step(dt)
        assertEquals(1,c.reads);c.loaded=false;c.tick(100);assertEquals(1,c.reads)
        c.loaded=true;c.tick(20);assertEquals(2,c.reads)
    }
    @Test fun completePacketParsingProtectsTargetSelectionAndAtomicBind() {
        val c=Circuit();c.bind();val old=c.u.targets.copyOf();val settings=c.u.settings.copyOf()
        fun packet(id:Int,write:(DataOutputStream)->Unit):Boolean { val b=ByteArrayOutputStream();DataOutputStream(b).use(write);return c.u.packet(id.toByte(),DataInputStream(ByteArrayInputStream(b.toByteArray()))) }
        assertFalse(packet(87) { it.writeDouble(2.0);it.writeDouble(64.0) });assertArrayEquals(old,c.u.targets);assertArrayEquals(settings,c.u.settings,0.0)
        assertFalse(packet(87) { s->doubleArrayOf(999.0,64.0,0.0,1.0).forEach(s::writeDouble) });assertArrayEquals(old,c.u.targets)
        assertFalse(packet(84) { it.writeInt(99) });assertEquals(0,c.u.selected)
        assertTrue(packet(87) { s->doubleArrayOf(2.0,64.0,0.0,3.0).forEach(s::writeDouble) });assertEquals(BlockPos(2,64,0).asLong(),c.u.targets[0]);assertEquals(2,c.u.circuits[0])
        assertFalse(packet(85) {});assertTrue(c.calls.isEmpty())
    }
    @Test fun rejectedCommandFeedbackDoesNotPretendTheTargetAcceptedIt() {
        val c=Circuit();c.tick(3);assertFalse(c.u.command(true));assertTrue(c.calls.isEmpty())
        c.bind();c.allow=false;assertFalse(c.u.command(true));assertEquals(1,c.calls.size)
        assertTrue(c.u.telemetry.contains("rejected"))
    }
    @Test fun sourceCompatibleNbtRetainsAllSlotsAndRejectsMalformedArrays() {
        val c=Circuit();for(i in 0..7) { c.u.select(i);c.bind(i+1.0,i+1.0) };c.tick(20)
        val saved=CompoundTag().also(c.u::write);val d=Circuit();d.u.read(saved)
        assertEquals(7,d.u.selected);assertArrayEquals(c.u.targets,d.u.targets);assertArrayEquals(c.u.circuits,d.u.circuits);assertEquals(c.u.lifetimeJoules,d.u.lifetimeJoules,0.0)
        saved.putLongArray("industrialTargets",longArrayOf(0));saved.putIntArray("industrialCircuits",intArrayOf(99));saved.putDouble("industrialLifetime",Double.NaN)
        d.u.read(saved);assertArrayEquals(c.u.targets,d.u.targets);assertArrayEquals(c.u.circuits,d.u.circuits);assertEquals(0.0,d.u.lifetimeJoules,0.0)
    }
    @Test fun steadySupplyAndPollingReuseNativeLuAndMeterExactlyAcceptedPower() {
        val c=Circuit();c.bind();c.tick(5);val versions=c.root.systems().map { it.matrixVersion };val before=c.u.lifetimeJoules
        c.tick(100);assertEquals(versions,c.root.systems().map { it.matrixVersion });assertEquals(100.0,c.u.lifetimeJoules-before,1e-8)
    }
}
