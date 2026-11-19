package mods.eln.gametest.cases;

import mods.eln.Eln;
import mods.eln.gametest.*;
import mods.eln.transparentnode.solarpanel.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import java.util.*;

/** Representative clear-sky array queries plus real opaque/transparent/partial shadows and local weather. */
public final class ForkPatchSolarSamplingCases implements TestCase {
    final List<SolarPanelElement> panels = new ArrayList<>();
    public String id() { return "forkpatch_solar_sampling_96_panel_field_and_real_shadows"; }
    public int width() { return 24; } public int height() { return 12; } public int depth() { return 24; }
    public int floorDepth() { return 24; }
    public int ticks() { return 10; }
    public void build(TestContext c) {
        var server=c.level().getServer();server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(),"time set noon");server.setWeatherParameters(100000,0,false,false);
        for(int x=0;x<11;x++)for(int z=0;z<9 && panels.size()<96;z++)panels.add((SolarPanelElement)c.placeTransparent(3073,c.at(1+x*2,1,1+z*2)));
        // GameTest encloses its structures with transparent barriers. Open this field's own columns
        // so the work-count measurement represents an outdoor array rather than its test enclosure.
        int opened=0;
        for(var panel:panels) {
            var at=BlockPos.containing(new SolarPannelSlowProcess(panel).sampleOrigin());
            int surface=c.level().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,at.getX(),at.getZ());
            var roof=new BlockPos(at.getX(),surface-1,at.getZ());
            if(c.level().getBlockState(roof).is(Blocks.BARRIER)) { c.level().removeBlock(roof,false);opened++; }
        }
        c.line("solar field: opened "+opened+" transparent test-enclosure roof cells");
    }
    public void measure(TestContext c) {
        var first=BlockPos.containing(new SolarPannelSlowProcess(panels.getFirst()).sampleOrigin());
        int top=c.level().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,first.getX(),first.getZ());
        var nonAir=new ArrayList<String>();
        for(int y=first.getY();y<top;y++) { var at=new BlockPos(first.getX(),y,first.getZ());var state=c.level().getBlockState(at);if(!state.isAir())nonAir.add(at+"="+state); }
        c.line("solar field resident column: sample="+first+", top="+top+", nonAir="+nonAir);
        int blocks=0,columns=0;
        for(var panel:panels) {
            var process=new SolarPannelSlowProcess(panel);double light=process.getSolarLight();
            c.check("solar field: actual clear-noon module remains fully illuminated",light>.99,"light "+light);
            blocks+=process.lastTrace.blockReads();columns+=process.lastTrace.columns();
        }
        c.check("solar field: resident heightmaps avoid all per-voxel reads above 96 clear modules",blocks==0,"block reads "+blocks+", columns "+columns);
        c.check("solar field: clear array queries stay bounded to nearby columns",columns>=96 && columns<=96*8,"columns "+columns);
        benchmark(c);
        var panel=panels.get(30);var process=new SolarPannelSlowProcess(panel);double clear=process.getSolarLight();var above=BlockPos.containing(process.sampleOrigin()).above(3);
        c.level().setBlock(above,Blocks.STONE.defaultBlockState(),2);
        c.check("solar field: modern opaque roof transmits zero radiation",process.getSolarLight()==0,"");
        c.level().setBlock(above,Blocks.GLASS.defaultBlockState(),2);
        c.check("solar field: clear glass restores the original daylight",Math.abs(process.getSolarLight()-clear)<1e-10,"");
        c.level().setBlock(above,Blocks.OAK_LEAVES.defaultBlockState(),2);double dampening=c.level().getBlockState(above).getLightDampening();
        c.check("solar field: leaf fixture has actual partial opacity",dampening>0 && dampening<15,"dampening "+dampening);
        c.check("solar field: partial opacity uses native zero-to-fifteen units",Math.abs(process.getSolarLight()-clear*(1-dampening/15))<1e-10,"dampening "+dampening);
        c.level().removeBlock(above,false);
        var saved=mods.eln.misc.Utils.weatherSource;
        try {
            mods.eln.misc.Utils.weatherSource=(world,pos)->new double[]{.4,.5};
            c.check("solar field: local graded weather retains its native multiplier",Math.abs(process.getSolarLight()-clear*.8*.75)<1e-10,"");
            for(var invalid:List.of(new double[]{Double.NaN,0},new double[]{0,Double.POSITIVE_INFINITY},new double[0])) {
                mods.eln.misc.Utils.weatherSource=(world,pos)->invalid;
                c.check("solar field: malformed weather cannot crash or poison solar generation",process.getSolarLight()==0,"");
            }
        }finally { mods.eln.misc.Utils.weatherSource=saved; }
        c.check("solar field: daylight recovers after malformed weather clears",Math.abs(process.getSolarLight()-clear)<1e-10,"");
        var absent=new BlockPos(30000000-2048,80,-30000000+2048);
        int cx=absent.getX()>>4,cz=absent.getZ()>>4;
        c.check("solar field: missing-chunk ray begins outside resident terrain",c.level().getChunkSource().getChunkNow(cx,cz)==null,"");
        var unknown=SolarWorldRay.trace(c.level(),new net.minecraft.world.phys.Vec3(absent.getX()+.5,80.5,absent.getZ()+.5),Math.PI/2);
        c.check("solar field: real shadow ray never loads missing terrain",unknown.blockReads()==0 && unknown.columns()==1 && c.level().getChunkSource().getChunkNow(cx,cz)==null,"");
    }

    private void benchmark(TestContext c) {
        var origins=panels.stream().map(p->new SolarPannelSlowProcess(p).sampleOrigin()).toList();
        double alpha=SolarPannelSlowProcess.getSolarAlpha(c.level());
        var bean=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();
        if(!bean.isThreadAllocatedMemoryEnabled())bean.setThreadAllocatedMemoryEnabled(true);
        for(int i=0;i<2;i++) { round(c,origins,alpha,false,bean);round(c,origins,alpha,true,bean); }
        var baseline=new ArrayList<double[]>();var optimized=new ArrayList<double[]>();
        for(int i=0;i<7;i++) {
            if(i%2==0) { baseline.add(round(c,origins,alpha,false,bean));optimized.add(round(c,origins,alpha,true,bean)); }
            else { optimized.add(round(c,origins,alpha,true,bean));baseline.add(round(c,origins,alpha,false,bean)); }
        }
        var report=new LinkedHashMap<String,Object>();
        report.put("scope","Same resident scene and exact voxel traversal; full voxel baseline versus native cached heightmap shortcut, not whole-mod baseline comparison");
        report.put("panels",96);report.put("queriesPerRound",96*20);report.put("warmupPairs",2);report.put("alternatingRounds",7);
        report.put("baselineRoundsWallNsCpuNsBytesPerRay",baseline);report.put("optimizedRoundsWallNsCpuNsBytesPerRay",optimized);
        report.put("baselineMedianWallNsPerRay",median(baseline,0));report.put("optimizedMedianWallNsPerRay",median(optimized,0));
        report.put("baselineMedianCpuNsPerRay",median(baseline,1));report.put("optimizedMedianCpuNsPerRay",median(optimized,1));
        report.put("baselineMedianBytesPerRay",median(baseline,2));report.put("optimizedMedianBytesPerRay",median(optimized,2));
        c.line("SOLAR_RAY_BENCHMARK "+new com.google.gson.Gson().toJson(report));
    }
    private double[] round(TestContext c,List<net.minecraft.world.phys.Vec3> origins,double alpha,boolean optimized,com.sun.management.ThreadMXBean bean) {
        long thread=Thread.currentThread().threadId();long allocated=bean.getThreadAllocatedBytes(thread),cpu=bean.getCurrentThreadCpuTime(),started=System.nanoTime();double sum=0;
        for(int n=0;n<20;n++)for(var origin:origins) {
            var result=optimized?SolarWorldRay.trace(c.level(),origin,alpha):SolarRay.trace(origin,alpha,c.level().getMaxY(),(x,y,z)-> {
                var state=mods.eln.node.LoadedBlockStates.get(c.level(),new BlockPos(x,y,z));return state==null?-1:state.getLightDampening();
            },(x,z)->Integer.MAX_VALUE);
            sum+=result.transmission();
        }
        double wall=(System.nanoTime()-started)/(double)(origins.size()*20),usedCpu=(bean.getCurrentThreadCpuTime()-cpu)/(double)(origins.size()*20),bytes=(bean.getThreadAllocatedBytes(thread)-allocated)/(double)(origins.size()*20);
        c.check("solar field: paired tracing measurement preserves clear-scene transmission",Math.abs(sum-origins.size()*20)<1e-9,optimized?"heightmap":"voxel baseline");
        return new double[]{wall,usedCpu,bytes};
    }
    private static double median(List<double[]> rows,int column) { return rows.subList(2,7).stream().mapToDouble(r->r[column]).sorted().toArray()[2]; }
}
