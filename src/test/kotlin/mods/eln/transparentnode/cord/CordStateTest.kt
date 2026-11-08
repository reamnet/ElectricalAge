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
