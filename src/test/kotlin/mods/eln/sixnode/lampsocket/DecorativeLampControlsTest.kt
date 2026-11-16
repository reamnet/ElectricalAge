package mods.eln.sixnode.lampsocket

import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.transparentnode.fan.FanDynamics
import mods.eln.transparentnode.fan.FanVisual
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.junit.Assert.*
import org.junit.Test
import java.io.*

class DecorativeLampControlsTest {
    @Test fun aimClampsRejectsInvalidAndRoundTripsEveryHead() {
        val a=LightAim(2,-25.0)
        assertFalse(a.set(-1,0,10.0));assertFalse(a.set(2,0,10.0));assertFalse(a.set(0,2,10.0))
        assertFalse(a.set(0,0,Double.NaN));assertFalse(a.set(0,1,Double.POSITIVE_INFINITY))
        assertTrue(a.set(0,0,200.0));assertTrue(a.set(1,1,-200.0))
        assertEquals(90.0,a.pan(0),0.0);assertEquals(-75.0,a.tilt(1),0.0)
        val tag=CompoundTag();a.write(tag)
        val saved=LightAim(2);saved.read(tag)
        val bytes=ByteArrayOutputStream();a.write(DataOutputStream(bytes))
        val input=DataInputStream(ByteArrayInputStream(bytes.toByteArray()));val published=LightAim(2);published.read(input)
        assertEquals(0,input.available())
        for(h in 0..1)for(other in listOf(saved,published)) {
            assertEquals(a.pan(h),other.pan(h),0.0);assertEquals(a.tilt(h),other.tilt(h),0.0)
        }
        val axial=LightAim(1,0.0,true);axial.set(0,0,725.0);assertEquals(5.0,axial.pan(0),0.0)
    }
    @Test fun swivelAndBeamUseTheSamePivotAndRotation() {
        val a=LightAim(1);a.set(0,0,90.0);a.set(0,1,0.0)
        val p=Vec3(.25,0.0,0.0)
        val point=a.point(Vec3(1.25,0.0,0.0),p,0)
        assertEquals(.25,point.x,1e-12);assertEquals(0.0,point.y,1e-12);assertEquals(1.0,point.z,1e-12)
        assertEquals(0.0,a.rotate(Vec3(1.0,0.0,0.0),0).x,1e-12)
        assertEquals(1.0,a.rotate(Vec3(1.0,0.0,0.0),0).z,1e-12)
        for(style in DecorativeLampStyle.entries.filter { it.aimHeadCount>0 }) {
            val aim=style.newAim()
            val original=DecorativeLampLighting.cells(style,style.bulbs,Vec3.ZERO,Direction.XN,LRDU.Up,{true},{true},aim)
            if(aim.axialPan)aim.set(0,1,30.0) else aim.set(0,0,60.0)
            val moved=DecorativeLampLighting.cells(style,style.bulbs,Vec3.ZERO,Direction.XN,LRDU.Up,{true},{true},aim)
            assertNotEquals(style.name,original,moved)
        }
    }
    @Test fun wideFloodHasOffAxisIlluminationAndRespectsMissingChunks() {
        val style=DecorativeLampStyle.AREA_FLOOD
        val queried=mutableSetOf<BlockPos>()
        val cells=DecorativeLampLighting.cells(style,1,Vec3(.5,10.5,.5),Direction.XN,LRDU.Up,{it.x<16},{queried+=it;true})
        assertTrue(cells.any { kotlin.math.abs(it.z)>2 })
        assertTrue(queried.all { it.x<16 });assertTrue(cells.all { it.x<16 })
    }
    @Test fun adjustableDropUsesQuarterMetresAndMovesTheLease() {
        val s=WarehouseSuspension();assertEquals(1.0,s.drop,0.0)
        assertFalse(s.set(Double.NaN));s.set(2.36);assertEquals(2.25,s.drop,0.0)
        s.set(99.0);assertEquals(7.0,s.drop,0.0)
        val n=CompoundTag();s.write(n);val saved=WarehouseSuspension();saved.read(n);assertEquals(7.0,saved.drop,0.0)
        val style=DecorativeLampStyle.WAREHOUSE_HIGH_BAY
        fun cells(drop: Double)=DecorativeLampLighting.cells(style,style.bulbs,Vec3(.5,20.5,.5),Direction.YP,LRDU.Up,{true},{true},style.newAim(),drop)
        assertEquals(cells(0.0).map { it.below(3) }.toSet(),cells(3.0))
    }
    @Test fun rotorStartupAndCoastArePhysicalAndSaved() {
        val r=FanDynamics();repeat(100) { r.step(.05,1.0) };assertEquals(1.0,r.speed,1e-12)
        repeat(100) { r.step(.05,0.0) };assertEquals(.5,r.speed,1e-12)
        val n=CompoundTag();r.write(n);val saved=FanDynamics();saved.read(n);assertEquals(r.speed,saved.speed,0.0)
        repeat(100) { r.step(.05,0.0) };assertEquals(0.0,r.speed,0.0)
        assertEquals(0.0,r.step(Double.NaN,1.0),0.0)
        val v=FanVisual();repeat(40) { v.step(.05f,1.0) };assertEquals(1f,v.speed,1e-6f);assertTrue(v.angle>0f)
        repeat(40) { v.step(.05f,0.0) };assertEquals(0f,v.speed,1e-6f)
    }
    @Test fun hazardIsTheBladeDiskRatherThanTheReservedVolume() {
        val c=Vec3.ZERO
        assertTrue(CeilingFanHazard.touches(AABB(-.1,-.1,-.1,.1,.1,.1),c))
        assertTrue(CeilingFanHazard.touches(AABB(2.4,-.1,-.1,2.6,.1,.1),c))
        assertFalse(CeilingFanHazard.touches(AABB(2.6,-.1,-.1,2.8,.1,.1),c))
        assertFalse(CeilingFanHazard.touches(AABB(0.0,.05,0.0,1.0,1.0,1.0),c))
    }
    @Test fun householdLampPlugsDoNotDependOnWhichBulbIsInstalled() {
        val rating=mods.eln.transparentnode.cord.CordRating.entries
        for(v in listOf(50.0,120.0,200.0,240.0,Double.NaN)) {
            assertTrue(LampCordCompatibility.accepts(mods.eln.transparentnode.cord.CordRating.V120,v))
            assertTrue(LampCordCompatibility.accepts(mods.eln.transparentnode.cord.CordRating.V240,v))
            for(r in rating.filter { it.industrial })assertEquals(r.accepts(v),LampCordCompatibility.accepts(r,v))
            assertFalse(LampCordCompatibility.accepts(mods.eln.transparentnode.cord.CordRating.V240_30,v))
        }
    }
    @Test fun motionHoldCountsPhysicalSecondsAndClearsWhenDisabled() {
        val h=MotionHold();assertFalse(h.step(.05,false,true));assertTrue(h.step(.05,true,true))
        repeat(599) { assertTrue(h.step(.05,false,true)) };assertFalse(h.step(.051,false,true))
        assertTrue(h.step(.05,true,true));assertFalse(h.step(.05,false,false));assertEquals(0.0,h.remaining,0.0)
    }
}
