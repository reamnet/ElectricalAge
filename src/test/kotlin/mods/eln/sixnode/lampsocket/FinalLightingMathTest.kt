package mods.eln.sixnode.lampsocket

import mods.eln.misc.*
import mods.eln.transparentnode.floodlight.FloodlightOptics
import mods.eln.transparentnode.festive.FairyLightPath
import net.minecraft.world.phys.Vec3
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class FinalLightingMathTest {
    @Test fun sourceWearIsHoursAndVoltageSensitive(){
        assertEquals(0.0,SourceLampLife.lossHours(3600.0,80.0,120.0,90.0),0.0)
        assertEquals(.5,SourceLampLife.lossHours(3600.0,105.0,120.0,90.0),1e-12)
        assertEquals(1.0,SourceLampLife.lossHours(3600.0,120.0,120.0,90.0),1e-12)
        assertEquals(10.0,SourceLampLife.lossHours(3600.0,150.0,120.0,90.0),1e-12)
        assertEquals(100.0,SourceLampLife.lossHours(3600.0,-180.0,120.0,90.0),1e-12)
        assertEquals(0.0,SourceLampLife.lossHours(Double.NaN,120.0,120.0,90.0),0.0)
        assertEquals(0.0,SourceLampLife.lossHours(1.0,Double.POSITIVE_INFINITY,120.0,90.0),0.0)
    }
    @Test fun everyMountFrameHasFiniteUnitBeamsAndFourPlanePorts(){
        var frames=0
        for(axis in HybridNodeDirection.entries)for(front in HybridNodeDirection.entries){
            if(front==axis||front==axis.inverse)continue
            frames++
            val ports=Direction.values().sumOf{side->LRDU.values().count{FloodlightOptics.isMountingPlanePort(axis,side,it)}}
            assertEquals(4,ports)
            for(h in listOf(0.0,90.0,180.0,360.0))for(v in listOf(0.0,90.0,180.0))for(w in listOf(0.0,45.0)){
                val rays=FloodlightOptics.rays(h,v,w,axis,front)
                assertEquals(if(w==0.0)1 else 81,rays.size)
                val centre=rays[0].first
                for((ray,factor)in rays){assertTrue(ray.x.isFinite()&&ray.y.isFinite()&&ray.z.isFinite());assertEquals(1.0,ray.length(),1e-12);assertTrue(factor>=1.0);assertTrue(ray.dot(centre)>=kotlin.math.cos(Math.toRadians(w/2))-1e-12)}
            }
        }
        assertEquals(24,frames)
    }
    @Test fun fairySpanRetainsExactAnchorsAndSourceSag(){
        val a=Vec3(0.0,4.0,0.0);val b=Vec3(8.0,4.0,0.0);val points=FairyLightPath.points(a,b)
        assertEquals(a,points.first());assertEquals(b,points.last());assertEquals(2.8,points[points.size/2].y,1e-12)
        assertTrue(points.zipWithNext().all{(p,q)->p.distanceTo(q)<.2})
        assertTrue(FairyLightPath.bulbPoints(points,true).size<FairyLightPath.bulbPoints(points,false).size)
        assertTrue(FairyLightPath.points(a,Vec3(9.0,4.0,0.0)).isEmpty())
        assertTrue(FairyLightPath.points(a,Vec3(Double.NaN,4.0,0.0)).isEmpty())
    }
}
