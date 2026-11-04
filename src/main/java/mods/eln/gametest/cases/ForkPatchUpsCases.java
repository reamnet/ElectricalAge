package mods.eln.gametest.cases;

import mods.eln.Eln;
import mods.eln.gametest.*;
import mods.eln.gl.GlFrame;
import mods.eln.gui.ElnNodeMenu;
import mods.eln.misc.Direction;
import mods.eln.node.transparent.*;
import mods.eln.sim.IProcess;
import mods.eln.sim.mna.component.*;
import mods.eln.transparentnode.ups.*;
import mods.eln.transparentnode.standby.*;
import mods.eln.oil.Oil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import java.io.*;
import java.util.*;

/** Real cell items, native menus/hoppers, backup/standby behavior and persistent inventory energy. */
public final class ForkPatchUpsCases {
    private ForkPatchUpsCases() { }
    public static List<TestCase> cases() {
        var tests=new ArrayList<TestCase>();
        tests.add(new Backup(null,false));tests.add(new Backup(null,true));
        for(var front:List.of(Direction.XN,Direction.XP,Direction.ZN,Direction.ZP))tests.add(new Backup(front,false));
        tests.add(new Cells());tests.add(new Generator(false));tests.add(new Generator(true));
        return tests;
    }
    private static void fill(UpsElement e,int count,double fraction,String name) {
        for(int i=0;i<count;i++) { var item=Eln.findItemStack(name,1);var type=UpsBatteryBankKt.upsBattery(item);type.setEnergy(item,type.getEnergyMax(item)*fraction);e.getInventory().setItem(i,item); }
    }
    private static void exact(TestContext c,String name,double a,double b) { c.check(name,Double.isFinite(a) && Math.abs(a-b)<1e-6,"actual="+a+", expected="+b); }
    private static void panel(TestContext ctx,UpsElement e) {
        var bytes=new ByteArrayOutputStream();e.networkSerialize(new DataOutputStream(bytes));var desc=(UpsDescriptor)e.getDescriptor();
        var render=new UpsRender((TransparentNodeEntity)ctx.level().getBlockEntity(e.node.coordonate.pos()),desc);
        render.networkUnserialize(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
        exact(ctx,"UPS: publication preserves stored energy",render.getDisplay().getEnergy(),e.getBank().getEnergy());
        GlFrame.beginRenderFrame();var frame=GlFrame.open();
        try { desc.draw(e.front,render.getDisplay());int n=frame.batches().values().stream().mapToInt(GlFrame.Batch::vertices).sum();ctx.check("UPS: native live panel and enclosure geometry",n>200,"vertices="+n); }
        finally { GlFrame.close(); }
    }
    private abstract static class Fixture implements TestCase {
        final List<Component> parts=new ArrayList<>();IProcess clock;
        void connect() { for(var p:parts)Eln.simulator.addElectricalComponent(p); }
        void cleanup() { if(clock!=null)Eln.simulator.removeElectricalProcess(clock);for(var p:parts) { Eln.simulator.removeElectricalComponent(p);p.breakConnection(); } }
    }
    private static final class Backup extends Fixture {
        final Direction front;final boolean empty;UpsElement ups;SwitchableVoltageSource feed;Resistor load;double elapsed;int phase;double before;
        Backup(Direction front,boolean empty) { this.front=front;this.empty=empty; }
        public String id() { return "forkpatch_ups_"+(front==null?(empty?"empty_unloaded":"floor_backup"):"wall_"+front.name().toLowerCase(Locale.ROOT)); }
        public int ticks() { return 180; }
        public void build(TestContext ctx) {
            var at=ctx.at(3,1,1);int id=front==null?7947:7953;
            if(front!=null) {
                var desc=Eln.transparentNodeItem.getDescriptor(id);ctx.level().setBlockAndUpdate(at.relative(front.getInverse().toEnumFacing()),Blocks.STONE.defaultBlockState());
                var n=((TransparentNodeItem)desc.parentItem).placeAt(desc.newItemStack(),FakePlayerFactory.getMinecraft(ctx.level()),ctx.level(),at,front.getInverse());
                if(n==null)throw new IllegalStateException("UPS wall placement failed");ups=(UpsElement)n.element;
            } else ups=(UpsElement)ctx.placeTransparent(id,at);
            fill(ups,24,empty?0:.5,"UPS AGM Battery Cell");
            feed=new SwitchableVoltageSource("paidInput");feed.connectTo(ups.getUnit().getPrimary(),null);feed.setU(240);feed.setEnabled(true);parts.add(feed);
            if(!empty) { load=new Resistor(ups.getUnit().getSecondary(),null).setR(14.4);parts.add(load); }
            connect();
            if(!empty) {
                clock=seconds->{
                    elapsed+=seconds;
                    if(phase==0 && elapsed>=2) { ctx.check("UPS: online source supplies real load",load.getP()>990,ups.getUnit().getStatus());before=ups.getBank().getEnergy();feed.setEnabled(false);phase=1; }
                    if(phase==1)ctx.check("UPS: no output interruption during input outage",ups.getUnit().getSecondary().getU()>119.8,ups.getUnit().getStatus());
                    if(phase==1 && elapsed>=4) { ctx.check("UPS: backup spends actual cell energy",ups.getBank().getEnergy()<before-1900,"");feed.setEnabled(true);phase=2; }
                };Eln.simulator.addElectricalProcess(clock);
            }
        }
        public void measure(TestContext ctx) {
            try {
                ctx.check("UPS: charged bank powers configured output",ups.getUnit().getSecondary().getU()>119.8,ups.getUnit().getStatus());
                if(empty)ctx.check("UPS: idle empty rack charges without an output cable",ups.getBank().getEnergy()>100,ups.getUnit().getStatus());
                else ctx.check("UPS: restored input feeds load and charging",phase==2 && ups.getUnit().getInputWatts()>ups.getUnit().getOutputWatts(),ups.getUnit().getStatus());
                if(front!=null) { var d=(UpsDescriptor)ups.getDescriptor();var box=d.wallBox(front);exact(ctx,"UPS: wall enclosure collision depth",Math.min(box.maxX-box.minX,box.maxZ-box.minZ),.33);int ports=0;for(var side:Direction.values())for(var lrdu:mods.eln.misc.LRDU.values())if(ups.getElectricalLoad(side,lrdu)!=null)ports++;ctx.check("UPS: exactly two cable ports on the wall",ports==2,"ports="+ports); }
                ctx.check("UPS: native hopper/pipe item capability exposed",ctx.level().getCapability(Capabilities.Item.BLOCK,ups.node.coordonate.pos(),net.minecraft.core.Direction.UP)!=null,"");
                panel(ctx,ups);
            } finally { cleanup(); }
        }
    }
    private static final class Cells implements TestCase {
        UpsElement ups;
        public String id() { return "forkpatch_ups_cell_inventory"; }
        public void build(TestContext ctx) { ups=(UpsElement)ctx.placeTransparent(7947,ctx.at(3,1,1));ups.getUnit().settings(new CompoundTag()); }
        public void measure(TestContext ctx) {
            var bank=ups.getBank();var inventory=ups.getInventory();
            fill(ups,23,.5,"Portable Battery Pack");ctx.check("UPS: partial rack cannot add capacity",bank.getInstalled()==23 && bank.getActiveCount()==0 && bank.getCapacity()==0,"");
            var last=Eln.findItemStack("Portable Battery Pack",1);var type=UpsBatteryBankKt.upsBattery(last);type.setEnergy(last,type.getEnergyMax(last)*.5);inventory.setItem(23,last);
            double nativeCapacity=type.getEnergyMax(last);exact(ctx,"UPS: native pack capacity preserved",bank.getCapacity(),24*nativeCapacity);
            double start=bank.getEnergy();exact(ctx,"UPS: accepted cell charge bounded",bank.transfer(2400,1),2400);
            var removed=inventory.decrStackSize(0,1);
            exact(ctx,"UPS: legacy removal flushes paid charge",UpsBatteryBankKt.upsBattery(removed).getEnergy(removed),nativeCapacity*.5+100);
            ctx.check("UPS: removing one cell deactivates its rack",bank.getActiveCount()==0,"");inventory.setItem(0,removed);
            exact(ctx,"UPS: reinsertion cannot duplicate charge",bank.getEnergy(),start+2400);
            fill(ups,48,.5,"Portable Battery");var small=inventory.getItem(0);var smallType=UpsBatteryBankKt.upsBattery(small);
            exact(ctx,"UPS: two racks use actual native cell energy",bank.getCapacity(),48*smallType.getEnergyMax(small));
            exact(ctx,"UPS: native cell power limits preserved",bank.availableDischargeWatts(.05),48*smallType.getDischagePower(small));
            ctx.check("UPS: incompatible items rejected",UpsBatteryBankKt.upsBattery(Eln.findItemStack("Portable Condensator",1))==null && UpsBatteryBankKt.upsBattery(Eln.findItemStack("Iron Plate",1))==null,"");
            bank.transfer(-40,.1);var saved=new CompoundTag();ups.writeToNBT(saved);
            var restored=new UpsElement(new TransparentNode(),ups.getDescriptor());restored.front=ups.front;restored.readFromNBT(saved);
            exact(ctx,"UPS: actual cell energy survives inventory NBT",restored.getBank().getEnergy(),bank.getEnergy());
            var moved=new UpsElement(new TransparentNode(),ups.getDescriptor());moved.front=ups.front;moved.readItemStackNBT(ups.getItemStackNBT());
            ctx.check("UPS: dropped settings cannot copy installed cells",moved.getBank().getInstalled()==0 && !ups.getItemStackNBT().contains("inv"),"");
            var player=FakePlayerFactory.get(ctx.level(),new com.mojang.authlib.GameProfile(UUID.fromString("e1a0b2c3-0000-4000-8000-000000000173"),"eln_ups_menu"));
            var spec=mods.eln.gui.MenuSpecs.of(ups.getDescriptor());
            var menu=new ElnNodeMenu(1,player.getInventory(),inventory,spec,ups.node,ups.node.coordonate.pos(),0,ElnNodeMenu.TRANSPARENT,7947);
            ctx.check("UPS: native menu exposes both complete racks",menu.deviceSlots==48 && menu.slots.size()==84,"");
            ctx.check("UPS: slot filter rejects capacitors",!menu.getSlot(0).mayPlace(Eln.findItemStack("Portable Condensator",1)),"");
            double held=UpsBatteryBankKt.upsBattery(inventory.getItem(0)).getEnergy(inventory.getItem(0));
            menu.quickMoveStack(player,0);
            boolean found=false;for(int i=0;i<player.getInventory().getContainerSize();i++) { var item=player.getInventory().getItem(i);var battery=UpsBatteryBankKt.upsBattery(item);if(battery!=null && Math.abs(battery.getEnergy(item)-held)<1e-6)found=true; }
            ctx.check("UPS: shift-click preserves exact charged item",found && inventory.getItem(0).isEmpty(),"");
            var remaining=inventory.getItem(24);double expected=UpsBatteryBankKt.upsBattery(remaining).getEnergy(remaining)+1;
            exact(ctx,"UPS: active second rack accepts cached charge",bank.transfer(24,.1),24);inventory.clearContent();
            exact(ctx,"UPS: clearing flushes cached energy before removal",UpsBatteryBankKt.upsBattery(remaining).getEnergy(remaining),expected);
        }
    }
    private static final class Generator extends Fixture {
        final boolean empty;UpsElement ups;StandbyGeneratorElement gen;Resistor load;double elapsed;int phase;
        Generator(boolean empty) { this.empty=empty; }
        public String id() { return "forkpatch_ups_standby_"+(empty?"empty_start":"stop_restart"); }
        public int width() { return 10; }public int height() { return 5; }public int depth() { return 10; }public int floorDepth() { return 10; }public int ticks() { return 360; }
        public void build(TestContext ctx) {
            ups=(UpsElement)ctx.placeTransparent(7947,ctx.at(3,1,3));gen=(StandbyGeneratorElement)ctx.placeTransparent(7943,ctx.at(7,1,6));
            fill(ups,48,empty?0:.5,"UPS AGM Battery Cell");((StandbyTank)gen.getUnit().getFuel()).getTank().fill(new FluidStack(Oil.Cut.GASOLINE.source.get(),20000),true);gen.getUnit().setMode(StandbyMode.MANUAL);
            parts.add(new Resistor(gen.getUnit().getOutput(),ups.getUnit().getPrimary()).setR(.05));load=new Resistor(ups.getUnit().getSecondary(),null).setR(empty?14.4:2.4);parts.add(load);connect();
            if(!empty) { clock=seconds->{elapsed+=seconds;
                if(phase==0 && elapsed>=8) { ctx.check("UPS standby: generator supports loaded UPS",gen.getUnit().getRunning() && load.getP()>5900,gen.getUnit().getStatus());gen.getUnit().setMode(StandbyMode.OFF);phase=1; }
                if(phase==1)ctx.check("UPS standby: output survives generator stop",ups.getUnit().getSecondary().getU()>119.8,ups.getUnit().getStatus());
                if(phase==1 && elapsed>=10) { gen.getUnit().setMode(StandbyMode.MANUAL);phase=2; }
            };Eln.simulator.addElectricalProcess(clock); }
        }
        public void measure(TestContext ctx) {
            try {
                ctx.check("UPS standby: generator remains fault-free",gen.getUnit().getRunning() && gen.getUnit().getFault().isEmpty(),gen.getUnit().getStatus());
                ctx.check("UPS standby: real load receives regulated output",load.getP()>(empty?990:5900),"P="+load.getP());
                ctx.check("UPS standby: generator is never backfed",gen.getUnit().getSource().getP()>=0,"");
                if(empty)ctx.check("UPS standby: empty rack recovers with real fuel and cells",ups.getBank().getEnergy()>100,ups.getUnit().getStatus());
                panel(ctx,ups);
            } finally { cleanup(); }
        }
    }
}
