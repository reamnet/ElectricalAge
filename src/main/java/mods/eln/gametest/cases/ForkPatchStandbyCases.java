package mods.eln.gametest.cases;

import mods.eln.Eln;
import mods.eln.gametest.*;
import mods.eln.gl.GlFrame;
import mods.eln.misc.Direction;
import mods.eln.node.transparent.*;
import mods.eln.oil.Oil;
import mods.eln.sim.IProcess;
import mods.eln.sim.mna.component.*;
import mods.eln.transparentnode.standby.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import java.io.*;
import java.util.*;

/** Registered standby hardware, native transactions, actual transfer/reset and persistence. */
public final class ForkPatchStandbyCases {
    private ForkPatchStandbyCases() { }
    public static List<TestCase> cases() {
        var cases=new ArrayList<TestCase>();
        for(var tier:GeneratorTier.values())cases.add(new Engine(tier));
        cases.add(new Ats(false));cases.add(new Ats(true));
        cases.add(new Reset(false));cases.add(new Reset(true));cases.add(new Fuel());
        return cases;
    }
    private static int id(GeneratorTier tier) { return ((tier==GeneratorTier.SMALL?124:125)<<6)+tier.getSubId(); }
    private abstract static class Fixture implements TestCase {
        final List<Component> parts=new ArrayList<>();
        IProcess clock;
        public int width() { return 10; } public int height() { return 5; } public int depth() { return 10; } public int floorDepth() { return 10; }
        void connect() { for(var p:parts)Eln.simulator.addElectricalComponent(p); }
        void cleanup() { if(clock!=null)Eln.simulator.removeElectricalProcess(clock);for(var p:parts) { Eln.simulator.removeElectricalComponent(p);p.breakConnection(); } }
        void panel(TestContext ctx,BackupElement element) {
            var bytes=new ByteArrayOutputStream();element.networkSerialize(new DataOutputStream(bytes));
            var descriptor=(BackupDescriptor)element.getDescriptor();
            var render=new BackupRender((TransparentNodeEntity)ctx.level().getBlockEntity(element.node.coordonate.pos()),descriptor);
            render.networkUnserialize(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
            ctx.check("standby: published output reaches display",Double.isFinite(render.getDisplay().getOutput()),render.getDisplay().getStatus());
            GlFrame.beginRenderFrame();var frame=GlFrame.open();
            try { descriptor.draw(element.front,render.getDisplay());int n=frame.batches().values().stream().mapToInt(GlFrame.Batch::vertices).sum();ctx.check("standby: native enclosure and front panel geometry",n>200,"vertices="+n); }
            finally { GlFrame.close(); }
        }
    }
    private static final class Engine extends Fixture {
        final GeneratorTier tier;StandbyGeneratorElement generator;Resistor load;double initial;
        Engine(GeneratorTier tier) { this.tier=tier; }
        public String id() { return "forkpatch_standby_engine_"+tier.name().toLowerCase(Locale.ROOT); }
        public int ticks() { return 140; }
        public void build(TestContext ctx) {
            generator=(StandbyGeneratorElement)ctx.placeTransparent(ForkPatchStandbyCases.id(tier),ctx.at(5,1,5));
            var fuel=(StandbyTank)generator.getUnit().getFuel();
            if(tier==GeneratorTier.SMALL) {
                var player=FakePlayerFactory.getMinecraft(ctx.level());player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Oil.Cut.GASOLINE.bucketItem.get()));
                ctx.check("generator: real bucket interaction fills native tank",generator.onBlockActivated(player,Direction.XP,0,0,0) && fuel.getTank().getFluidAmount()==1000,"");
                fuel.getTank().fill(new FluidStack(Oil.Cut.GASOLINE.source.get(),19000),true);
            } else fuel.getTank().fill(new FluidStack(Oil.Cut.GASOLINE.source.get(),20000),true);
            initial=fuel.getAvailableJoules();
            generator.networkUnserialize(ctx.stream(s->{s.writeByte(80);s.writeDouble(240);}));
            generator.networkUnserialize(ctx.stream(s->s.writeByte(82)));
            load=new Resistor(generator.getUnit().getOutput(),null).setR(240*240/(tier.getWatts()*.5));parts.add(load);connect();
        }
        public void measure(TestContext ctx) {
            try {
                var u=generator.getUnit();
                ctx.check("generator: actual half-rated load supplied",u.getRunning() && load.getP()>tier.getWatts()*.49,u.getStatus()+" / "+load.getP());
                ctx.check("generator: power and current within tier",u.getWatts()<=tier.getWatts()*1.000001 && u.getSource().getCurrent()<=tier.getAmps()*1.000001,"");
                ctx.check("generator: fuel pays for generation",u.getFuel().getAvailableJoules()<initial,"");
                ctx.checkValue("generator: tier tank capacity",u.getFuel().getCapacity(),tier.getTankMb());
                ctx.check("generator: native pipe capability is its transactional tank",ctx.level().getCapability(Capabilities.Fluid.BLOCK,generator.node.coordonate.pos(),null)==((StandbyTank)u.getFuel()).getTank(),"");
                int ghosts=0;for(int x=0;x<width();x++)for(int y=0;y<height();y++)for(int z=0;z<depth();z++)if(ctx.level().getBlockState(ctx.at(x,y,z)).is(Eln.ghostBlock))ghosts++;
                ctx.checkValue("generator: complete tier multiblock",ghosts,tier.getWidth()*tier.getHeight()*tier.getDepth()-1);
                var copy=new StandbyGeneratorElement(new TransparentNode(),generator.getDescriptor());copy.front=generator.front;
                copy.readItemStackNBT(generator.getItemStackNBT());
                ctx.checkValue("generator: moving preserves paid fuel",copy.getUnit().getFuel().getAvailableJoules(),u.getFuel().getAvailableJoules());
                var config=new CompoundTag();generator.writeConfigTool(config,FakePlayerFactory.getMinecraft(ctx.level()));
                var empty=new StandbyGeneratorElement(new TransparentNode(),generator.getDescriptor());empty.front=generator.front;empty.readConfigTool(config,FakePlayerFactory.getMinecraft(ctx.level()));
                ctx.checkValue("generator: settings copy cannot duplicate fuel",empty.getUnit().getFuel().getAvailableJoules(),0);
                ctx.check("generator: audio/exhaust activates only while running",GeneratorEffects.INSTANCE.running(u.getStatus()) && GeneratorEffects.INSTANCE.volume(u.getStatus(),u.getWatts())>0,"");
                panel(ctx,generator);
            } finally { cleanup(); }
        }
    }
    private static final class Ats extends Fixture {
        final boolean wall;AtsElement ats;SwitchableVoltageSource mains;Resistor load;double elapsed;int phase;
        Ats(boolean wall) { this.wall=wall; }
        public String id() { return "forkpatch_standby_ats_"+(wall?"wall":"floor"); }
        public int ticks() { return 260; }
        public void build(TestContext ctx) {
            int id=(124<<6)+(wall?14:6);var p=ctx.at(4,1,4);
            var d=(BackupDescriptor)Eln.transparentNodeItem.getDescriptor(id);
            if(wall) {
                ctx.level().setBlockAndUpdate(p.south(),Blocks.STONE.defaultBlockState());
                var n=((TransparentNodeItem)d.parentItem).placeAt(d.newItemStack(),FakePlayerFactory.getMinecraft(ctx.level()),ctx.level(),p,Direction.ZP);
                if(n==null)throw new IllegalStateException("Wall ATS placement failed");ats=(AtsElement)n.element;
            } else ats=(AtsElement)ctx.placeTransparent(id,p);
            mains=new SwitchableVoltageSource("testMains");mains.connectTo(ats.getUnit().getMains(),null);mains.setU(240);mains.setEnabled(true);
            parts.add(mains);parts.add(new VoltageSource("testBackup",ats.getUnit().getBackup(),null).setU(240));load=new Resistor(ats.getUnit().getOutput(),null).setR(57.6);parts.add(load);connect();
            clock=seconds->{
                elapsed+=seconds;
                var u=ats.getUnit();
                if(u.getMainContact().getClosed() && u.getBackupContact().getClosed())throw new IllegalStateException("ATS paralleled its sources");
                if(phase==0 && elapsed>=2) { ctx.check("ATS: mains initially supplies load",u.getMainContact().getClosed() && load.getP()>990,u.getStatus());mains.setEnabled(false);phase=1; }
                if(phase==1 && elapsed>=4) { ctx.check("ATS: backup supplies failed mains without backfeed",u.getBackupContact().getClosed() && u.getMainContact().getCurrent()==0 && load.getP()>990,u.getStatus());mains.setEnabled(true);phase=2; }
            };Eln.simulator.addElectricalProcess(clock);
        }
        public void measure(TestContext ctx) {
            try {
                var u=ats.getUnit();ctx.check("ATS: stable mains return transfers back",phase==2 && u.getMainContact().getClosed() && !u.getBackupContact().getClosed() && load.getP()>990,u.getStatus());
                if(wall) { var box=((BackupDescriptor)ats.getDescriptor()).wallBox(ats.front);ctx.checkValue("ATS: thin wall enclosure",Math.min(box.maxX-box.minX,box.maxZ-box.minZ),.33); }
                panel(ctx,ats);
            } finally { cleanup(); }
        }
    }
    private static final class Reset extends Fixture {
        final boolean persistent;AtsElement ats;StandbyGeneratorElement generator;Resistor load;double elapsed,latchedFuel=-1;int phase;
        Reset(boolean persistent) { this.persistent=persistent; }
        public String id() { return "forkpatch_standby_linked_reset_"+(persistent?"still_faulted":"corrected_load"); }
        public int ticks() { return 340; }
        public void build(TestContext ctx) {
            ats=(AtsElement)ctx.placeTransparent((124<<6)+6,ctx.at(3,1,3));
            generator=(StandbyGeneratorElement)ctx.placeTransparent((124<<6)+7,ctx.at(7,1,6));
            ((StandbyTank)generator.getUnit().getFuel()).getTank().fill(new FluidStack(Oil.Cut.GASOLINE.source.get(),20000),true);
            for(var e:List.of(ats,generator))e.networkUnserialize(ctx.stream(s->{s.writeByte(81);s.writeUTF("native reset");}));
            parts.add(new Resistor(generator.getUnit().getOutput(),ats.getUnit().getBackup()).setR(.01));
            load=new Resistor(ats.getUnit().getOutput(),null).setR(240*240/20000.0);parts.add(load);connect();
            clock=seconds->{
                elapsed+=seconds;
                if(phase==0 && elapsed>=8) {
                    // Force a real overload directly on the alternator before testing the remote reset.
                    if(!generator.getUnit().getFault().isEmpty()) {
                        if(!persistent)load.setR(57.6);
                        double before=generator.getUnit().getFuel().getAvailableJoules();
                        ats.networkUnserialize(ctx.stream(s->s.writeByte(83)));
                        ctx.check("ATS reset: actual command reaches linked faulted engine",ats.getResetCount()==1,"count="+ats.getResetCount());
                        ctx.checkValue("ATS reset: fuel unchanged by command",generator.getUnit().getFuel().getAvailableJoules(),before);phase=1;
                    }
                }
                if(phase==1 && elapsed>=14 && !generator.getUnit().getFault().isEmpty()) { latchedFuel=generator.getUnit().getFuel().getAvailableJoules();phase=2; }
            };Eln.simulator.addElectricalProcess(clock);
        }
        public void measure(TestContext ctx) {
            try {
                ctx.check("ATS reset: command was issued",phase>0,generator.getUnit().getStatus());
                if(persistent)ctx.check("ATS reset: bad load relatches without automatic restart",phase==2 && !generator.getUnit().getRunning() && generator.getUnit().getFuel().getAvailableJoules()==latchedFuel,generator.getUnit().getStatus());
                else ctx.check("ATS reset: corrected backup load is restored",generator.getUnit().getRunning() && ats.getUnit().getBackupContact().getClosed() && load.getP()>990,generator.getUnit().getStatus()+" / "+load.getP());
                panel(ctx,ats);
            } finally { cleanup(); }
        }
    }
    private static final class Fuel implements TestCase {
        public String id() { return "forkpatch_standby_fuel_transactions"; }
        public void build(TestContext ctx) { }
        public void measure(TestContext ctx) {
            var store=new StandbyTank(1000,()->kotlin.Unit.INSTANCE);var fuel=new FluidStack(Oil.Cut.GASOLINE.source.get(),500);
            var data=new CompoundTag();data.putString("blend","premium");fuel.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
            var resource=FluidResource.of(fuel);
            try(var tx=Transaction.openRoot()) { ctx.checkValue("fuel: simulated insertion reports amount",store.getTank().insert(0,resource,500,tx),500); }
            ctx.checkValue("fuel: aborted transaction cannot add fuel",store.getTank().getFluidAmount(),0);
            try(var tx=Transaction.openRoot()) { store.getTank().insert(0,resource,500,tx);tx.commit(); }
            ctx.checkValue("fuel: water rejected",store.getTank().fill(new FluidStack(Fluids.WATER,100),true),0);
            ctx.checkValue("fuel: component variants cannot mix",store.getTank().fill(new FluidStack(Oil.Cut.GASOLINE.source.get(),100),true),0);
            double initial=store.getAvailableJoules();ctx.check("fuel: one joule accepted",store.consume(1),"");
            ctx.checkValue("fuel: energy paid exactly",store.getAvailableJoules(),initial-1);
            var tag=new CompoundTag();store.write(tag);var restored=new StandbyTank(1000,()->kotlin.Unit.INSTANCE);restored.read(tag);
            ctx.checkValue("fuel: tank and fractional reserve survive save",restored.getAvailableJoules(),store.getAvailableJoules());
            ctx.check("fuel: codec preserves fluid components",FluidStack.isSameFluidSameComponents(fuel,restored.getTank().getFluid()),"");
            restored.getTank().drain(1000,true);double reserve=restored.getReserveJoules();
            ctx.checkValue("fuel: pipes cannot extract paid reserve",restored.getAvailableJoules(),reserve);
            ctx.check("fuel: invalid and excessive requests fail",!restored.consume(Double.NaN) && !restored.consume(-1) && !restored.consume(reserve+1),"");
        }
    }
}
