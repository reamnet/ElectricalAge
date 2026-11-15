package mods.eln.sixnode.lampsocket

import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import org.junit.Assert.*
import org.junit.Test

class DecorativeLampGeometryTest {
    @Test fun explicitSourceIdsLeaveUnportedFixturesReserved() {
        assertEquals(4118,DecorativeLampStyle.DESK.lookup)
        assertEquals(4119,DecorativeLampStyle.RECESSED.lookup)
        assertEquals(4121,DecorativeLampStyle.DOT.lookup)
        assertEquals(4145,DecorativeLampStyle.OUTDOOR_POST.lookup)
        assertEquals(4352,DecorativeLampStyle.WINDOW_CANDLE.lookup)
        assertEquals(4365,DecorativeLampStyle.HEAVY_BULKHEAD.lookup)
        assertEquals(4366,DecorativeLampStyle.HEAVY_WALLPACK.lookup)
        assertEquals(4367,DecorativeLampStyle.HEAVY_CANOPY.lookup)
        assertEquals(4368,DecorativeLampStyle.HEAVY_YARD.lookup)
        assertEquals(4369,DecorativeLampStyle.RIG_CAGE.lookup)
        assertEquals(4370,DecorativeLampStyle.RIG_LINEAR.lookup)
        assertEquals(4137,DecorativeLampStyle.TABLE_DRUM.lookup)
        assertEquals(4138,DecorativeLampStyle.TABLE_BANKER.lookup)
        assertEquals(4139,DecorativeLampStyle.TABLE_TASK.lookup)
        assertEquals(4374,DecorativeLampStyle.CHRISTMAS_FAIRY.lookup)
        assertEquals(4375,DecorativeLampStyle.CHRISTMAS_COLOR.lookup)
        assertEquals(4376,DecorativeLampStyle.CHRISTMAS_ICICLE.lookup)
        assertEquals(4377,DecorativeLampStyle.CHRISTMAS_STAR.lookup)
        assertEquals(4378,DecorativeLampStyle.HALLOWEEN_PUMPKIN.lookup)
        assertEquals(4379,DecorativeLampStyle.HALLOWEEN_GHOST.lookup)
        assertEquals(4380,DecorativeLampStyle.HALLOWEEN_BAT.lookup)
        assertEquals(4381,DecorativeLampStyle.HALLOWEEN_JACK.lookup)
        assertEquals(4401,DecorativeLampStyle.PARTY_FESTOON.lookup)
        assertEquals(4402,DecorativeLampStyle.PARTY_TIKI.lookup)
        assertEquals(4404,DecorativeLampStyle.PARTY_GLOBE.lookup)
        assertEquals(4405,DecorativeLampStyle.PARTY_LANTERN.lookup)
        assertEquals(4406,DecorativeLampStyle.DECK_POST_CAP.lookup)
        assertEquals(4407,DecorativeLampStyle.DECK_STEP.lookup)
        assertEquals(4480,DecorativeLampStyle.PIER_BOLLARD.lookup)
        assertEquals(4481,DecorativeLampStyle.PIER_BULKHEAD.lookup)
        assertEquals(4482,DecorativeLampStyle.PIER_GOOSE.lookup)
        assertEquals(4483,DecorativeLampStyle.PIER_LANTERN.lookup)
        assertEquals(4484,DecorativeLampStyle.PIER_DOCK.lookup)
        assertEquals(4485,DecorativeLampStyle.PATH_LOUVER.lookup)
        assertEquals(4486,DecorativeLampStyle.PATH_SHIELDED.lookup)
        assertEquals(4487,DecorativeLampStyle.PATH_BLADE.lookup)
        assertEquals(4488,DecorativeLampStyle.PATH_MUSHROOM.lookup)
        assertEquals(4489,DecorativeLampStyle.PATH_ARCH.lookup)
        assertEquals(4490,DecorativeLampStyle.STONE_BRICK.lookup)
        assertEquals(4491,DecorativeLampStyle.STONE_ROUND.lookup)
        assertEquals(4492,DecorativeLampStyle.STONE_SLOT.lookup)
        assertEquals(4493,DecorativeLampStyle.STONE_SQUARE.lookup)
        assertEquals(4494,DecorativeLampStyle.STONE_WASH.lookup)
        assertEquals(4117,DecorativeLampStyle.WAREHOUSE.lookup)
        assertEquals(4364,DecorativeLampStyle.HEAVY_FLOOD.lookup)
        assertEquals(4371,DecorativeLampStyle.RIG_FLOOD.lookup)
        assertEquals(4382,DecorativeLampStyle.DOT_PINHOLE.lookup)
        assertEquals(4383,DecorativeLampStyle.DOT_BEZEL.lookup)
        assertEquals(4384,DecorativeLampStyle.DOT_EYEBALL.lookup)
        assertEquals(4385,DecorativeLampStyle.DOT_DEEP.lookup)
        assertEquals(4386,DecorativeLampStyle.DOT_TWIN.lookup)
        assertEquals(4392,DecorativeLampStyle.AREA_FLOOD.lookup)
        assertEquals(82,DecorativeLampStyle.entries.map { it.lookup }.toSet().size)
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
