package mods.eln.transparentnode.cord

import mods.eln.misc.Direction
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.phys.Vec3
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.util.UUID

class CordStateTest {
    @Test fun strictAnchorRoundTripRetainsSourceUuidAndRejectsUnknownConnectors() {
        val a=CordAnchor(0,BlockPos(12,64,-8),UUID.fromString("00112233-4455-6677-8899-aabbccddeeff"),Direction.ZP,CordRating.V240_30,1)
        assertEquals(a,CordAnchor.read(a.write()))
        val t=a.write();t.putString("rating","future");assertNull(CordAnchor.read(t));t.putString("rating","V240_30");t.putInt("socket",6);assertNull(CordAnchor.read(t))
        t.putInt("socket",0);t.putInt("facing",Direction.YP.int);assertNull(CordAnchor.read(t));assertNull(CordAnchor.read(CompoundTag()))
    }
    @Test fun recessedAnchorsRetainTheirPhysicalPlaneAndRejectImpossibleSocketKinds() {
        for(front in listOf(Direction.XN,Direction.XP,Direction.ZN,Direction.ZP)) {
            val anchor=CordAnchor(0,BlockPos(12,64,-8),UUID.randomUUID(),front,CordRating.V200,1,recessed=true)
            assertEquals(anchor,CordAnchor.read(anchor.write()))
            val delta=CordGeometry.world(front,CordGeometry.tip(1,false,false,true)).subtract(CordGeometry.world(front,CordGeometry.tip(1,false,false,false)))
            assertTrue(delta.distanceTo(Vec3.atLowerCornerOf(front.toEnumFacing().unitVec3i))<1e-12)
            assertNull(CordAnchor.read(anchor.write().apply { putBoolean("strip",true) }))
            assertNull(CordAnchor.read(anchor.write().apply { putBoolean("cabinet",true) }))
            assertNull(CordAnchor.read(anchor.write().apply { putInt("socket",2) }))
        }
    }
    @Test fun compactSocketsRequireExplicitTopologyAndPreserveAllFourPositions() {
        for(rating in listOf(CordRating.V120,CordRating.V240))for(slot in 0..3) {
            val a=CordAnchor(0,BlockPos(1,64,2),UUID.randomUUID(),Direction.XN,rating,slot,cabinet=true)
            assertEquals(a,CordAnchor.read(a.write()))
            assertNull(CordAnchor.read(a.write().apply { putInt("socket",4) }))
            assertNull(CordAnchor.read(a.write().apply { putBoolean("horizontal",true) }))
            assertNull(CordAnchor.read(a.write().apply { putBoolean("strip",true) }))
            assertNull(CordAnchor.read(a.write().apply { putBoolean("recessed",true) }))
            assertNull(CordAnchor.read(a.write().apply { putString("rating","V240_30") }))
        }
        val tips=(0..3).map(CompactSocketGeometry::tip)
        assertEquals(4,tips.toSet().size)
        assertTrue(tips.all { it.z>.49 && it.z<.5 })
    }
    @Test fun cabinetCordLeavesItsFrontFaceBeforeSamplingItsOwnCollisionCell() {
        for(front in listOf(Direction.XN,Direction.XP,Direction.ZN,Direction.ZP)) {
            val center=Vec3(.5,64.5,.5)
            val start=center.add(CordGeometry.world(front,CompactSocketGeometry.tip(0)))
            val end=center.add(CordGeometry.world(front,Vec3(3.0,-.43,-.25)))
            val ground={ x: Double,z: Double,_: Double->if(x>=0 && x<1 && z>=0 && z<1)65.0 else 64.0 }
            val direct=CordPath.samples(start,end,ground)
            assertTrue(direct.any { it.y>65.0 },"fixture must reproduce the cord climbing its own enclosure")
            val path=CordPath.socketSamples(start,end,front,true,ground)
            assertEquals(start,path.first());assertEquals(end,path.last())
            assertTrue(path.all { it.y<64.5 },"$front: a front cord must descend toward the floor")
            assertEquals(direct,CordPath.socketSamples(start,end,front,false,ground))
        }
    }
    @Test fun ratingsRejectNonfiniteAndWrongNominalVoltage() {
        assertEquals(9,CordRating.entries.size)
        for(r in CordRating.entries) { assertTrue(r.accepts(r.volts));assertFalse(r.accepts(Double.NaN));assertFalse(r.accepts(Double.POSITIVE_INFINITY));assertFalse(r.accepts(r.volts+1)) }
        assertNotEquals(CordRating.V240,CordRating.V240_30);assertNull(CordRating.read("invalid"))
    }
    @Test fun terrainEnvelopeRetainsEndpointsAndNeverCrossesSampledObstacle() {
        val start=Vec3(0.0,1.0,0.0);val end=Vec3(8.0,1.0,0.0)
        val points=CordPath.samples(start,end) { x,_,_->if(x in 3.0..5.0)1.5 else 0.0 }
        assertEquals(start,points.first());assertEquals(end,points.last());assertTrue(points.size in 9..65)
        assertTrue(points.filter { it.x in 3.0..5.0 }.all { it.y>=1.525-1e-12 })
        assertTrue(CordPath.samples(Vec3(Double.NaN,1.0,0.0),end) { _,_,_->0.0 }.isEmpty())
    }
    @Test fun transportedRingFramesStayFiniteAndJoinedForVerticalAndStationaryPaths() {
        for(points in listOf(listOf(Vec3.ZERO,Vec3(0.0,2.0,0.0)),listOf(Vec3.ZERO,Vec3.ZERO))) {
            val rings=CordPath.rings(points,.018)
            assertEquals(points.size,rings.size)
            rings.forEachIndexed { i,ring->assertEquals(4,ring.size);ring.forEach { assertTrue(it.x.isFinite() && it.y.isFinite() && it.z.isFinite());assertEquals(.018,it.distanceTo(points[i]),1e-12) } }
        }
    }
}
