package mods.eln.transparentnode.solarpanel;

import net.minecraft.world.phys.Vec3;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class ForkPatchSolarRayTest {
    @Test public void negativeCoordinatesUseTheContainingVoxel() {
        var result=SolarRay.trace(new Vec3(-.25,.25,-.25),Math.PI/2,20,(x,y,z)->x==-1&&y==0&&z==-1?15:0,(x,z)->100);
        assertEquals(0,result.transmission(),0);assertEquals(1,result.blockReads());
    }
    @Test public void visitsTheThinDiagonalCellThatDominantAxisSamplingMisses() {
        var visited=new ArrayList<String>();
        var result=SolarRay.trace(new Vec3(.01,.99,.5),Math.PI/6,20,(x,y,z)->{visited.add(x+","+y);return x==0&&y==1?15:0;},(x,z)->100);
        assertEquals(List.of("0,0","0,1"),visited);assertEquals(0,result.transmission(),0);
    }
    @Test public void nativeDampeningUsesFifteenAndStopsAtTheBuildHeight() {
        var result=SolarRay.trace(new Vec3(.5,.5,.5),Math.PI/2,2,(x,y,z)->y==1?1:0,(x,z)->100);
        assertEquals(14.0/15,result.transmission(),1e-12);assertEquals(3,result.blockReads());assertFalse(result.capped());
        var opaque=SolarRay.trace(new Vec3(.5,.5,.5),Math.PI/2,2,(x,y,z)->15,(x,z)->100);
        assertEquals(0,opaque.transmission(),0);assertEquals(1,opaque.blockReads());
    }
    @Test public void clearHeightmapColumnsSkipAllBlockReadsWithoutSkippingAnyPotentialObstruction() {
        var clear=SolarRay.trace(new Vec3(.5,2.5,.5),Math.PI/2,319,(x,y,z)->{throw new AssertionError("air was sampled");},(x,z)->2);
        assertEquals(1,clear.transmission(),0);assertEquals(0,clear.blockReads());assertEquals(1,clear.columns());
        var blocked=SolarRay.trace(new Vec3(.5,2.5,.5),Math.PI/4,319,(x,y,z)->x==2&&y==4?15:0,(x,z)->x==2?5:2);
        assertEquals(0,blocked.transmission(),0);
    }
    @Test public void unloadAndHorizontalRaysHaveFiniteWorkBudgets() {
        var unloaded=SolarRay.trace(new Vec3(.5,.5,.5),0,319,(x,y,z)->{throw new AssertionError("unloaded chunk sampled");},(x,z)->Integer.MIN_VALUE);
        assertEquals(1,unloaded.transmission(),0);assertEquals(0,unloaded.blockReads());
        var horizontal=SolarRay.trace(new Vec3(.5,2.5,.5),0,319,(x,y,z)->0,(x,z)->2);
        assertTrue(horizontal.capped());assertEquals(SolarRay.MAX_VISITS+1,horizontal.columns());assertEquals(0,horizontal.blockReads());
    }
    @Test public void invalidOrNightRaysCannotSampleTerrainOrGenerateLight() {
        for(double angle:new double[]{Double.NaN,Double.POSITIVE_INFINITY,-.1,Math.PI+.1}) {
            var r=SolarRay.trace(Vec3.ZERO,angle,319,(x,y,z)->{throw new AssertionError();},(x,z)->{throw new AssertionError();});
            assertEquals(0,r.transmission(),0);assertEquals(0,r.blockReads());assertEquals(0,r.columns());
        }
    }

    @Test public void anIntegerBoundaryStartsInTheVoxelTowardTheSun() {
        var visited=new ArrayList<Integer>();
        var r=SolarRay.trace(new Vec3(0,.5,.5),Math.PI,319,(x,y,z)->{visited.add(x);return 15;},(x,z)->100);
        assertEquals(List.of(-1),visited);assertEquals(0,r.transmission(),0);
    }

    @Test public void heightmapShortcutsAgreeWithIndependentRayBoxIntersections() {
        var random=new Random(0x5A17);
        for(int trial=0;trial<250;trial++) {
            double alpha=.03+random.nextDouble()*(Math.PI-.06);
            var origin=new Vec3(-.2,.3,.5);var obstacles=new ArrayList<int[]>();var heights=new HashMap<Integer,Integer>();
            for(int i=0;i<30;i++) {
                int x=random.nextInt(41)-20,y=random.nextInt(20);
                obstacles.add(new int[]{x,y});heights.merge(x,y+1,Math::max);
            }
            boolean blocked=false;
            for(var box:obstacles) {
                double dx=Math.cos(alpha),dy=Math.sin(alpha);
                double a=(box[0]-origin.x)/dx,b=(box[0]+1-origin.x)/dx;
                double enter=Math.max(0,Math.max(Math.min(a,b),(box[1]-origin.y)/dy));
                double leave=Math.min(Math.max(a,b),(box[1]+1-origin.y)/dy);
                blocked |= leave>enter+1e-10;
            }
            var ray=SolarRay.trace(origin,alpha,20,(x,y,z)->obstacles.stream().anyMatch(b->b[0]==x&&b[1]==y)?15:0,(x,z)->heights.getOrDefault(x,0));
            assertEquals("trial "+trial+", angle "+alpha,blocked?0:1,ray.transmission(),0);
            assertFalse(ray.capped());
        }
    }
}
