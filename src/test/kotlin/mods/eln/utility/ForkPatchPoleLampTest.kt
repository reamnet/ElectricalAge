package mods.eln.utility

import mods.eln.sim.mna.RootSystem
import mods.eln.sim.mna.component.*
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.transparentnode.utilitylight.StreetLightMount
import mods.eln.misc.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.phys.Vec3
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class ForkPatchPoleLampTest {
    private class Circuit(volts: Double=120.0) {
        val root=RootSystem(.05,1);val line=NbtElectricalLoad("lamp");val parts=ArrayList<Component>();val lamp=PoleLamp(line,parts)
        val source=VoltageSource("service",line,null).setU(volts)
        init { root.addState(line);parts.forEach(root::addComponent);root.addComponent(source) }
        fun run(n: Int=3) { repeat(n) { root.step();lamp.sample() } }
    }
    @Test fun enabledLampDrawsItsActualSeventyWatts() { val c=Circuit();c.run();assertEquals(70.0,c.source.p,1e-8);assertEquals(70.0,c.lamp.watts,1e-8);assertTrue(c.lamp.light in 14..15) }
    @Test fun unpoweredLampCannotInventLight() { val c=Circuit(0.0);c.run();assertEquals(0,c.lamp.light);assertEquals(0.0,c.lamp.watts,0.0) }
    @Test fun voltageSagDimsTheLampAndReducesPower() { val c=Circuit(60.0);c.run();assertEquals(17.5,c.lamp.watts,1e-8);assertEquals(3,c.lamp.light) }
    @Test fun manualOffOpensTheLoadAndExtinguishesLight() { val c=Circuit();c.run();c.lamp.configure(false,false);c.run();assertEquals(0,c.lamp.light);assertTrue(c.lamp.watts<1e-10) }
    @Test fun duskControlOnlyConsumesPowerAtNight() { val c=Circuit();c.lamp.configure(true,true);c.run();assertTrue(c.lamp.watts<1e-10);assertEquals(0,c.lamp.light);c.lamp.night=true;c.run();assertEquals(70.0,c.lamp.watts,1e-8);assertTrue(c.lamp.light>=14);c.lamp.night=false;c.run();assertEquals(0,c.lamp.light) }
    @Test fun nativeLuFactorsStayCachedWhileBrightnessIsSteady() { val c=Circuit();c.run(10);val versions=c.root.systems().map { it.matrixVersion };c.run(200);assertEquals(versions,c.root.systems().map { it.matrixVersion }) }
    @Test fun sourceUtilityLampSettingsRoundTripAndRejectBadAim() {
        val c=Circuit();assertTrue(c.lamp.aim(-3,355,-45));c.lamp.configure(false,true);val n=CompoundTag();c.lamp.write(n)
        val copy=Circuit();copy.lamp.read(n);assertEquals(-3,copy.lamp.heightStep);assertEquals(355,copy.lamp.yaw);assertEquals(-45,copy.lamp.pitch);assertFalse(copy.lamp.enabled);assertTrue(copy.lamp.automatic)
        assertFalse(copy.lamp.aim(9,999,61));assertEquals(-3,copy.lamp.heightStep);assertEquals(355,copy.lamp.yaw)
    }
    @Test fun sourceTransformerLampKeysRemainReadable() {
        val line=NbtElectricalLoad("lamp");val lamp=PoleLamp(line,ArrayList(),transformer=true);val n=CompoundTag()
        n.putInt("mainsLampHeightStep",1);n.putInt("mainsLampYaw",270);n.putInt("mainsLampPitch",30);n.putBoolean("mainsLampEnabled",false);n.putBoolean("mainsLampAutomatic",true)
        lamp.read(n);assertEquals(1,lamp.heightStep);assertEquals(270,lamp.yaw);assertEquals(30,lamp.pitch);assertFalse(lamp.enabled);assertTrue(lamp.automatic)
    }
    @Test fun heightYawAndPitchUseTheSameFixturePivotAsTheLightPosition() {
        val head=Vec3(-1.15,5.2,0.0)
        for(front in listOf(Direction.XN,Direction.XP,Direction.ZN,Direction.ZP)) {
            val base=StreetLightMount.adjust(head,front,0,0,0);val raised=StreetLightMount.adjust(head,front,1,0,0)
            assertEquals(.25,raised.y-base.y,1e-10)
            val a=StreetLightMount.adjust(head,front,-4,90,30);assertTrue(a.x.isFinite() && a.y in 3.8..4.5 && a.z.isFinite())
            assertTrue(base.distanceToSqr(a)>.1)
        }
        assertEquals(0,StreetLightMount.turn(355,5));assertEquals(355,StreetLightMount.turn(0,-5))
    }
    @Test fun malformedSavedAimIsClampedToValidDefaults() {
        val c=Circuit();val n=CompoundTag();n.putInt("utilityLampHeight",-100);n.putInt("utilityLampPitch",200);n.putInt("utilityLampYaw",-1);c.lamp.read(n)
        assertEquals(0,c.lamp.heightStep);assertEquals(0,c.lamp.pitch);assertEquals(0,c.lamp.yaw)
    }
}
