package mods.eln.gametest.cases;

import mods.eln.Eln;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.gl.GlFrame;
import mods.eln.misc.Direction;
import mods.eln.node.transparent.*;
import mods.eln.sim.mna.component.*;
import mods.eln.transparentnode.renewable.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/** Native placed cabinets, GUI packets, publication data, live geometry and automatic save round trips. */
public final class ForkPatchSolarControllerCases {
    private ForkPatchSolarControllerCases() { }
    public static List<TestCase> cases() {
        return List.of(new Cabinet(false, false, false), new Cabinet(true, false, false),
            new Cabinet(false, true, false), new Cabinet(true, true, false), new Cabinet(false, false, true));
    }
    private static final class Cabinet implements TestCase {
        final boolean hybrid, wall, export;
        SolarElement element;
        RenewableDescriptor descriptor;
        BlockPos at;
        VoltageSource utility;
        Resistor load;
        final List<Component> fixtures = new ArrayList<>();
        Cabinet(boolean hybrid, boolean wall, boolean export) { this.hybrid=hybrid; this.wall=wall; this.export=export; }
        public String id() { return "forkpatch_solar_"+(hybrid ? "hybrid" : "mppt")+"_"+(wall ? "wall" : "floor")+(export ? "_export" : ""); }
        public int ticks() { return 80; }
        public void build(TestContext ctx) {
            int id=(124<<6)+(wall ? (hybrid ? 16 : 15) : (hybrid ? 9 : 8));
            descriptor=(RenewableDescriptor) Eln.transparentNodeItem.getDescriptor(id);
            at=ctx.at(3,1,1);
            if (wall) {
                ctx.level().setBlockAndUpdate(at.relative(net.minecraft.core.Direction.SOUTH), Blocks.STONE.defaultBlockState());
                var player=FakePlayerFactory.getMinecraft(ctx.level());
                TransparentNode node=((TransparentNodeItem)descriptor.parentItem).placeAt(descriptor.newItemStack(),player,ctx.level(),at,Direction.ZP);
                if (node==null) throw new IllegalStateException("Wall cabinet placement failed");
                element=(SolarElement)node.element;
            } else element=(SolarElement)ctx.placeTransparent(id,at);
            SolarPowerUnit unit=element.getUnit();
            element.networkUnserialize(ctx.stream(s -> { s.writeByte(80); s.writeDouble(hybrid ? 240 : 120); }));
            if (hybrid) {
                for(int i=0;i<3;i++) element.networkUnserialize(ctx.stream(s -> s.writeByte(82)));
                utility=new VoltageSource("testUtility",unit.getGrid(),null).setU(120);
                fixtures.add(utility);
            } else {
                fixtures.add(new VoltageSource("testPaidPV",unit.getSolar(),unit.getNegative()).setU(850));
                if (export) {
                    element.networkUnserialize(ctx.stream(s -> s.writeByte(83)));
                    utility=new VoltageSource("testUtility",unit.getOutput(),null).setU(120);
                    fixtures.add(utility);
                }
            }
            double nominal=hybrid ? 240 : 120;
            load=new Resistor(unit.getOutput(),null).setR(nominal*nominal/(export ? 1000 : 5000));
            fixtures.add(load);
            for(Component c:fixtures) Eln.simulator.addElectricalComponent(c);
        }
        public void measure(TestContext ctx) {
            try {
                SolarPowerUnit unit=element.getUnit();
                ctx.check("solar cabinet: real load receives power",load.getP()>4800 || (export && load.getP()>990),unit.getStatus()+" / "+load.getP());
                ctx.checkValue("solar cabinet: selected output voltage",unit.getOutput().getU(),hybrid ? 240 : 120);
                ctx.check("solar cabinet: DC link remains within capacity",unit.getEnergy()>=0 && unit.getEnergy()<=SolarPowerUnit.CAPACITY,"J="+unit.getEnergy());
                if(export) ctx.check("MPPT: paid solar output reverses utility power",utility.getP()< -7500 && unit.getFedJoules()>0,"grid="+utility.getP());
                if(hybrid) ctx.check("hybrid: grid-only selection survives the command path",unit.getPriority()==SolarPriority.GRID_ONLY && unit.getGridWatts()>5000,unit.getStatus());
                {
                    var bytes=new ByteArrayOutputStream();element.networkSerialize(new DataOutputStream(bytes));
                    var render=new RenewableRender((TransparentNodeEntity)ctx.level().getBlockEntity(at),descriptor);
                    render.networkUnserialize(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
                    ctx.checkValue("solar cabinet: published output reaches the display",render.getDisplay().getOutput(),unit.getOutput().getU());
                    GlFrame.beginRenderFrame();GlFrame frame=GlFrame.open();
                    try {
                        descriptor.draw(element.front,render.getDisplay());
                        int vertices=frame.batches().values().stream().mapToInt(GlFrame.Batch::vertices).sum();
                        ctx.check("solar cabinet: enclosure and live display record native geometry",vertices>200,"vertices="+vertices);
                    } finally { GlFrame.close(); }
                }
                if(wall) {
                    var box=ctx.level().getBlockState(at).getCollisionShape(ctx.level(),at).bounds();
                    double depth=Math.min(box.maxX-box.minX,box.maxZ-box.minZ);
                    ctx.check("wall cabinet: collision depth matches its thin enclosure",Math.abs(depth-.33)<1e-9,"depth="+depth);
                }
                var dropped=element.getItemStackNBT();
                ctx.check("solar cabinet: dropped settings cannot duplicate energy",dropped.getDoubleOr("solarJoules",-1)==0 && dropped.getDoubleOr("solarFedJoules",-1)==0,"");
            } finally {
                for(Component c:fixtures) { Eln.simulator.removeElectricalComponent(c); c.breakConnection(); }
            }
        }
    }
}
