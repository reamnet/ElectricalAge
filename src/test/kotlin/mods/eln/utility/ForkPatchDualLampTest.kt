package mods.eln.utility

import mods.eln.sim.IProcess
import mods.eln.sim.mna.RootSystem
import mods.eln.sim.mna.component.*
import mods.eln.sim.mna.state.State
import net.minecraft.nbt.CompoundTag
import org.junit.Assert.*
import org.junit.Test

class ForkPatchDualLampTest {
    private class Circuit(val kind: DistributionKind) {
        val states=ArrayList<State>();val parts=ArrayList<Component>();val processes=ArrayList<IProcess>()
        val u=DistributionPoleUnit(kind,states,parts,processes);val root=RootSystem(.05,1)
        val low=VoltageSource("120V",u.services[0],null).setU(120.0);val high=VoltageSource("240V",u.services[2],null).setU(240.0)
        init { states.forEach(root::addState);parts.forEach(root::addComponent);root.addComponent(low);root.addComponent(high) }
        fun run(n: Int=4) { repeat(n) { root.step();u.lamp!!.sample() } }
    }
    @Test fun builtInDualVoltageFixturesUseOneSelectedServiceOnly() {
        for(kind in listOf(DistributionKind.TWIN_SECONDARY_LIGHT,DistributionKind.FEEDER_LIGHT_SINGLE)) {
            val c=Circuit(kind);c.run();val l=c.u.lamp!!
            assertEquals(120,l.selected);assertEquals(kind.lampWatts+120.0*120/1e10,c.low.p,1e-8);assertEquals(240.0*240/1e10,c.high.p,1e-8)
            assertTrue(l.chooseSupply(240));c.run();assertEquals(kind.lampWatts+240.0*240/1e10,c.high.p,1e-8);assertEquals(120.0*120/1e10,c.low.p,1e-8)
            assertFalse(l.chooseSupply(0));assertFalse(l.chooseSupply(12470));assertEquals(240,l.selected)
        }
    }
    @Test fun twinHeadsShareOne120WLoadAndCannotDrawFromAnUnselectedLiveService() {
        val c=Circuit(DistributionKind.TWIN_SECONDARY_LIGHT);val l=c.u.lamp!!;c.low.u=0.0;c.run()
        assertEquals(0,l.light);assertTrue(l.watts<1e-8);assertEquals(2,c.kind.lampHeads.size)
        l.chooseSupply(240);c.run();assertEquals(120.0,l.watts,1e-8);assertTrue(l.light>=14)
    }
    @Test fun selectedVoltageAndDuskControlsSurviveSourceKeys() {
        val c=Circuit(DistributionKind.FEEDER_LIGHT_SINGLE);val l=c.u.lamp!!;l.chooseSupply(240);l.configure(true,true);l.aim(-2,90,25)
        val t=CompoundTag();c.u.write(t);val copy=Circuit(c.kind);copy.u.read(t);copy.run();val next=copy.u.lamp!!
        assertEquals(240,next.selected);assertTrue(next.automatic);assertEquals(90,next.yaw);assertEquals(0,next.light)
        next.night=true;copy.run();assertEquals(70.0,next.watts,1e-8)
    }
    @Test fun undergroundLampNeedsRealLvAndRetainsIndependentPrimaryFuses() {
        for(kind in listOf(DistributionKind.UNDERGROUND_LIGHT_SINGLE,DistributionKind.UNDERGROUND_LIGHT_THREE)) {
            val c=Circuit(kind);c.low.u=0.0;c.run();assertEquals(0,c.u.lamp!!.light)
            c.low.u=120.0;c.run();assertEquals(70.0,c.u.lamp!!.watts,1e-8)
            c.u.fuses.indices.forEach(c.u::operateFuse);c.run();assertTrue(c.u.contacts.none { it.closed });assertEquals(70.0,c.u.lamp!!.watts,1e-8)
        }
    }
}
