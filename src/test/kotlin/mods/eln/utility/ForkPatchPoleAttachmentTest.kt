package mods.eln.utility

import mods.eln.sim.mna.RootSystem
import mods.eln.sim.mna.component.*
import mods.eln.sim.nbt.NbtElectricalLoad
import net.minecraft.nbt.CompoundTag
import org.junit.Assert.*
import org.junit.Test

class ForkPatchPoleAttachmentTest {
    private class Circuit(a: Double=120.0,b: Double=240.0) {
        val root=RootSystem(.05,1);val low=NbtElectricalLoad("low");val high=NbtElectricalLoad("high");val parts=ArrayList<Component>()
        val lamp=PoleLamp(low,parts,source240=high,removable=true)
        val s120=VoltageSource("120",low,null).setU(a);val s240=VoltageSource("240",high,null).setU(b)
        init { root.addState(low);root.addState(high);parts.forEach(root::addComponent);root.addComponent(s120);root.addComponent(s240) }
        fun run(n: Int=4) { repeat(n) { root.step();lamp.sample() } }
    }
    @Test fun absentAttachmentNeverConsumesPowerOrEmitsLight() { val c=Circuit();c.run();assertFalse(c.lamp.installed);assertEquals(0,c.lamp.light);assertTrue(c.lamp.watts<1e-10) }
    @Test fun attachmentSelectsOneServiceCircuitAndCannotDrawTwice() { val c=Circuit();assertTrue(c.lamp.install());c.run();assertEquals(240,c.lamp.selected);assertEquals(70.0,c.s240.p,1e-8);assertTrue(c.s120.p<1e-10);assertEquals(70.0,c.lamp.watts,1e-8) }
    @Test fun autoSelectsTheAvailable120VoltService() { val c=Circuit(b=0.0);c.lamp.install();c.run();assertEquals(120,c.lamp.selected);assertEquals(70.0,c.s120.p,1e-8);assertTrue(c.s240.p<1e-10) }
    @Test fun manualSupplyDoesNotFallBackOrParallelServiceCircuits() { val c=Circuit(b=0.0);c.lamp.install();assertTrue(c.lamp.chooseSupply(240));c.run();assertEquals(240,c.lamp.selected);assertTrue(c.lamp.watts<1e-10);assertEquals(0,c.lamp.light);assertFalse(c.lamp.chooseSupply(230));assertEquals(240,c.lamp.supply) }
    @Test fun automaticHysteresisAvoidsSupplyChatter() { val c=Circuit();c.lamp.install();c.run();c.s240.u=220.0;c.run();assertEquals(240,c.lamp.selected);c.s240.u=180.0;c.run();assertEquals(120,c.lamp.selected);c.s240.u=240.0;c.run();assertEquals(120,c.lamp.selected) }
    @Test fun removalIsSingleUseAndImmediatelyOpensBothLoads() { val c=Circuit();c.lamp.install();c.run();assertTrue(c.lamp.remove());assertFalse(c.lamp.remove());assertEquals(0,c.lamp.light);c.run();assertTrue(c.lamp.watts<1e-10);assertFalse(c.lamp.installed) }
    @Test fun savedAttachmentRetainsPaidPresenceAndValidatedControls() { val c=Circuit();c.lamp.install();c.lamp.configure(false,true);c.lamp.chooseSupply(120);c.lamp.aim(-2,270,-30);val n=CompoundTag();c.lamp.write(n);val copy=Circuit();copy.lamp.read(n);assertTrue(copy.lamp.installed);assertFalse(copy.lamp.enabled);assertTrue(copy.lamp.automatic);assertEquals(120,copy.lamp.supply);assertEquals(-2,copy.lamp.heightStep);assertEquals(270,copy.lamp.yaw);assertEquals(-30,copy.lamp.pitch) }
    @Test fun steadyAttachedLampKeepsCachedLuFactors() { val c=Circuit();c.lamp.install();c.run(10);val versions=c.root.systems().map { it.matrixVersion };c.run(200);assertEquals(versions,c.root.systems().map { it.matrixVersion }) }
}
