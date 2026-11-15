package mods.eln.sixnode.lampsocket

import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import org.junit.Assert.*
import org.junit.Test

class DecorativeLampGeometryTest {
    @Test fun explicitSourceIdsLeaveUnportedFixturesReserved() {
        assertEquals(4119,DecorativeLampStyle.RECESSED.lookup)
        assertEquals(4145,DecorativeLampStyle.OUTDOOR_POST.lookup)
        assertEquals(4352,DecorativeLampStyle.WINDOW_CANDLE.lookup)
        assertEquals(4365,DecorativeLampStyle.HEAVY_BULKHEAD.lookup)
        assertEquals(4366,DecorativeLampStyle.HEAVY_WALLPACK.lookup)
        assertEquals(4367,DecorativeLampStyle.HEAVY_CANOPY.lookup)
        assertEquals(4368,DecorativeLampStyle.HEAVY_YARD.lookup)
        assertEquals(4369,DecorativeLampStyle.RIG_CAGE.lookup)
        assertEquals(4370,DecorativeLampStyle.RIG_LINEAR.lookup)
        assertEquals(39,DecorativeLampStyle.entries.map { it.lookup }.toSet().size)
        assertFalse(DecorativeLampStyle.entries.any { it.lookup==4126 }) // motion sensor is separate work
    }
    @Test fun ceilingFixtureHeadsStayBelowTheirMount() {
        val origin=Vec3(10.5,20.5,30.5)
        for(style in DecorativeLampStyle.entries.filter { it.ceiling }) for(front in LRDU.values()) for(head in style.heads) {
            val point=style.worldPoint(origin,Direction.YP,front,head)
            assertTrue("${style.name} $front $point",point.y<21.00001)
        }
    }
    @Test fun floorPostHeadAndLeaseAreTwoBlocksAboveItsOwner() {
        val style=DecorativeLampStyle.OUTDOOR_POST
        val cells=DecorativeLampLighting.cells(style,1,Vec3(.5,.5,.5),Direction.YN,LRDU.Up,{true},{true})
        assertEquals(setOf(BlockPos(0,2,0)),cells)
    }
    @Test fun emptyFixturesCreateNoLeases() {
        for(style in DecorativeLampStyle.entries) assertTrue(DecorativeLampLighting.cells(style,0,Vec3.ZERO,Direction.XN,LRDU.Up,{true},{true}).isEmpty())
    }
    @Test fun blockedBeamsStopBeforeTheObstacleAndNeverQueryPastIt() {
        val style=DecorativeLampStyle.POT_BAFFLE
        val seen=mutableListOf<BlockPos>()
        val cells=DecorativeLampLighting.cells(style,1,Vec3(.5,.5,.5),Direction.XN,LRDU.Up,{true},{ seen.add(it);it.x<3 })
        assertEquals(3,seen.maxOf { it.x })
        assertTrue(cells.none { it.x>=3 })
        assertTrue(cells.any { it.x==2 })
    }
    @Test fun unloadedChunksNeverReachTheStateQuery() {
        val seen=mutableListOf<BlockPos>()
        DecorativeLampLighting.cells(DecorativeLampStyle.POT_BAFFLE,1,Vec3(14.5,.5,.5),Direction.XN,LRDU.Up,{it.x<16},{seen.add(it);true})
        assertTrue(seen.isNotEmpty());assertTrue(seen.all { it.x<16 })
    }
    @Test fun cylinderSconceLightsBothVerticalDirections() {
        val cells=DecorativeLampLighting.cells(DecorativeLampStyle.SCONCE_CYLINDER,2,Vec3(.5,10.5,.5),Direction.XN,LRDU.Up,{true},{true})
        assertTrue(cells.any { it.y<8 });assertTrue(cells.any { it.y>12 })
    }
    @Test fun partialChandeliersOnlyUseInstalledHeads() {
        val style=DecorativeLampStyle.CHANDELIER
        val origin=Vec3(.5,.5,.5)
        val one=DecorativeLampLighting.cells(style,1,origin,Direction.XN,LRDU.Up,{true},{true})
        assertEquals(setOf(BlockPos.containing(style.worldPoint(origin,Direction.XN,LRDU.Up,style.heads.first()))),one)
    }
}
