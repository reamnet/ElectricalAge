package mods.eln.gametest.cases;

import mods.eln.Eln;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.gl.GlFrame;
import mods.eln.misc.Direction;
import mods.eln.node.transparent.*;
import mods.eln.sim.mna.component.*;
import mods.eln.transparentnode.offgrid.*;
import mods.eln.utility.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import java.io.*;
import java.util.*;

/** Actual registered equipment: placement, physical power, packets, paid fuse items and save round trips. */
public final class ForkPatchOffGridCases {
    private ForkPatchOffGridCases() { }
    public static List<TestCase> cases() {
        return Arrays.stream(OffGridType.values()).map(Cabinet::new).map(c->(TestCase)c).toList();
    }
    private static final class Cabinet implements TestCase {
        final OffGridType type;
        OffGridElement element;
        OffGridDescriptor descriptor;
        BlockPos at;
        final List<Component> fixtures=new ArrayList<>();
        Resistor load;
        Cabinet(OffGridType type) { this.type=type; }
        public String id() { return "forkpatch_offgrid_"+type.name().toLowerCase(Locale.ROOT); }
        public int ticks() { return 80; }
        public void build(TestContext ctx) {
            descriptor=(OffGridDescriptor)Eln.transparentNodeItem.getDescriptor(8192+type.ordinal());
            at=ctx.at(3,1,1);
            if(type.getWall()) {
                Direction front=new Direction[]{Direction.XN,Direction.XP,Direction.ZN,Direction.ZP}[type.ordinal()-4];
                for(int y=0;y<2;y++)ctx.level().setBlockAndUpdate(at.above(y).relative(front.getInverse().toEnumFacing()),Blocks.STONE.defaultBlockState());
                var player=FakePlayerFactory.getMinecraft(ctx.level());
                TransparentNode node=((TransparentNodeItem)descriptor.parentItem).placeAt(descriptor.newItemStack(),player,ctx.level(),at,front.getInverse());
                if(node==null)throw new IllegalStateException("Wall battery placement failed");
                element=(OffGridElement)node.element;
            } else element=(OffGridElement)ctx.placeTransparent(8192+type.ordinal(),at);
            if(type.getBank()) {
                var bank=element.getBank();bank.getState().transfer(bank.getState().getCapacity()*.5);
                load=new Resistor(bank.getPositive(),bank.getNegative()).setR(5);
                fixtures.add(load);
            } else if(type==OffGridType.INVERTER || type==OffGridType.AC_CHARGER) {
                var ac=element.getAc();
                fixtures.add(new VoltageSource("testAC",ac.getGrid(),null).setU(240));
                fixtures.add(new VoltageSource("testBattery",ac.getDc(),ac.getNegative()).setU(52));
                load=new Resistor(ac.getLoad(),null).setR(57.6);fixtures.add(load);
                element.networkUnserialize(ctx.stream(s->{s.writeByte(84);s.writeDouble(50);}));
            } else if(type==OffGridType.MPPT) {
                var solar=element.getSolar();
                fixtures.add(new VoltageSource("testPaidPV",solar.getSolar(),solar.getNegative()).setU(850));
                fixtures.add(new VoltageSource("testBattery",solar.getOutput(),solar.getBatteryReturn()).setU(52));
            } else {
                var p=element.getProtection();
                fixtures.add(new VoltageSource("testDC",p.getFirst(),p.getNegative()).setU(48));
                load=new Resistor(p.getOutput(),p.getNegative()).setR(5);fixtures.add(load);
            }
            for(Component c:fixtures)Eln.simulator.addElectricalComponent(c);
        }
        public void measure(TestContext ctx) {
            try {
                if(type.getBank()) {
                    var bank=element.getBank();
                    var side=element.front.left();
                    ctx.check("battery: live cable port matches positive terminal",element.getElectricalLoad(side,side.getLRDUGoingTo(descriptor.connectionPlane(element.front)))==bank.getPositive(),"");
                    ctx.check("battery: actual load spends stored energy",load.getP()>400 && bank.getState().getEnergy()<bank.getState().getCapacity()*.5,bank.getStatus());
                    ctx.check("battery: upper ghost exists",ctx.level().getBlockState(at.above()).is(Eln.ghostBlock),"");
                    var shape=ctx.level().getBlockState(at.above()).getShape(ctx.level(),at.above());
                    var bounds=shape.bounds();
                    double depth=Math.min(bounds.maxX-bounds.minX,bounds.maxZ-bounds.minZ);
                    ctx.check("battery: upper collision follows enclosure",Math.abs(depth-(type.getWall()?.33:.84))<1e-8,"depth="+depth);
                    if(type.getWall()) {
                        var origin=ctx.level().getBlockState(at).getCollisionShape(ctx.level(),at).bounds();
                        ctx.check("battery: native origin collision is thin too",Math.abs(Math.min(origin.maxX-origin.minX,origin.maxZ-origin.minZ)-.33)<1e-8,origin.toString());
                    }
                    var moved=new OffGridElement(new TransparentNode(),descriptor);moved.front=Direction.ZP;
                    moved.readItemStackNBT(element.getItemStackNBT());
                    ctx.checkValue("battery: moving preserves paid stored energy",moved.getBank().getState().getEnergy(),bank.getState().getEnergy());
                    var config=new CompoundTag();config.putDouble("lfpJoules",Double.MAX_VALUE);
                    moved.readConfigTool(config,FakePlayerFactory.getMinecraft(ctx.level()));
                    ctx.checkValue("battery: configuration cannot copy energy",moved.getBank().getState().getEnergy(),bank.getState().getEnergy());
                } else if(type==OffGridType.INVERTER || type==OffGridType.AC_CHARGER) {
                    var ac=element.getAc();
                    ctx.check("AC charger: real battery bus receives power",ac.getChargeWatts()>2000 && ac.getChargeWatts()<=2840.01,ac.getStatus()+" / "+ac.getChargeWatts());
                    ctx.checkValue("AC charger: control packet changes charge rating",ac.getChargeAmps(),50);
                    if(type==OffGridType.INVERTER)ctx.check("inverter: bypass supplies real load",ac.getBypass().getClosed() && load.getP()>990,ac.getStatus());
                } else if(type==OffGridType.MPPT) {
                    String[] cells={"UPS AGM Battery Cell","UPS LiFePO4 Battery Cell","UPS Industrial Battery Module"};
                    double[] capacity={960000,3600000,14400000};
                    for(int i=0;i<cells.length;i++) {
                        var item=Eln.findItemStack(cells[i],1);
                        var cell=(mods.eln.transparentnode.ups.UpsLargeCell)mods.eln.generic.GenericItemUsingDamageDescriptor.getDescriptor(item);
                        ctx.checkValue("cell: crafted empty "+cells[i],cell.getEnergy(item),0);
                        ctx.checkValue("cell: actual capacity "+cells[i],cell.getEnergyMax(item),capacity[i]);
                        cell.setEnergy(item,Double.MAX_VALUE);
                        ctx.checkValue("cell: stored charge stays within rating "+cells[i],cell.getEnergy(item),capacity[i]);
                        cell.setEnergy(item,Double.NaN);
                        ctx.checkValue("cell: invalid charge cannot create energy "+cells[i],cell.getEnergy(item),0);
                    }
                    var solar=element.getSolar();
                    ctx.check("battery MPPT: paid PV charges DC bus",solar.getOutputWatts()>1000 && solar.getPvWatts()>solar.getOutputWatts(),solar.getStatus()+" / "+solar.getDiagnostic());
                    ctx.check("battery MPPT: drop cannot duplicate DC-link energy",element.getItemStackNBT().getDoubleOr("solarJoules",-1)==0,"");
                } else {
                    ctx.check("DC protection: closed contacts supply actual load",load.getP()>400,element.getProtection().getStatus());
                    if(type==OffGridType.COMBINER) {
                        var fuse=element.getProtection().getFuses().getFirst();
                        fuse.step(1,500,30,false);
                        var replacement=Eln.findItemStack("20 A Pole Fuse",1);
                        ctx.check("PV combiner: paid replacement installs",fuse.insertCartridge(replacement),"");
                        var saved=new CompoundTag();fuse.write(saved,"testFuse");
                        var restored=new CutoutFuse();restored.read(saved,"testFuse");
                        ctx.checkValue("fuse: codec preserves actual cartridge rating",restored.rating(30),20);
                        ctx.check("fuse: removed item retains identity",mods.eln.generic.GenericItemUsingDamageDescriptor.getDescriptor(restored.removeCartridge())==PoleHardware.INSTANCE.fuse(replacement),"");
                        var empty=new CompoundTag();restored.write(empty,"testFuse");restored.read(empty,"testFuse");
                        ctx.check("fuse: removed cartridge remains absent after save",!restored.getHasCartridge() && !restored.getClosed(),"");
                    }
                }
                var bytes=new ByteArrayOutputStream();element.networkSerialize(new DataOutputStream(bytes));
                var render=new OffGridRender((TransparentNodeEntity)ctx.level().getBlockEntity(at),descriptor);
                render.networkUnserialize(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
                ctx.checkValue("offgrid: publication reaches front panel",render.getDisplay().getDc(),element.display().getDc());
                GlFrame.beginRenderFrame();GlFrame frame=GlFrame.open();
                try {
                    descriptor.draw(element.front,render.getDisplay());
                    int vertices=frame.batches().values().stream().mapToInt(GlFrame.Batch::vertices).sum();
                    ctx.check("offgrid: native enclosure and live display geometry",vertices>200,"vertices="+vertices);
                } finally { GlFrame.close(); }
            }
            finally { for(Component c:fixtures) { Eln.simulator.removeElectricalComponent(c);c.breakConnection(); } }
        }
    }
}
