package mods.eln.gametest.cases;

import mods.eln.Eln;
import mods.eln.gametest.*;
import mods.eln.gl.GlFrame;
import mods.eln.gui.*;
import mods.eln.misc.*;
import mods.eln.node.*;
import mods.eln.node.six.*;
import mods.eln.node.transparent.*;
import mods.eln.registry.ElnDeviceRegistry;
import mods.eln.sim.IProcess;
import mods.eln.sim.mna.component.*;
import mods.eln.sim.mna.state.State;
import mods.eln.utility.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import java.io.*;
import java.util.*;

/** Actual placed wall cables, isolated downleads, physical meter items and collision. */
public final class ForkPatchServiceCases {
    private ForkPatchServiceCases() { }
    public static List<TestCase> cases() {
        var all=new ArrayList<TestCase>();
        for(var f:List.of(Direction.XN,Direction.XP,Direction.ZN,Direction.ZP))for(boolean before:List.of(false,true))all.add(new Wired(f,before,true,false));
        all.add(new Wired(Direction.XN,false,false,false));all.add(new Wired(Direction.XN,false,false,true));
        all.add(new Passive(false));all.add(new Passive(true));all.add(new Items());return all;
    }
    private abstract static class Fixture implements TestCase {
        final List<Component> parts=new ArrayList<>();final List<State> states=new ArrayList<>();IProcess clock;
        public int width() { return 10; }public int height() { return 5; }public int depth() { return 10; }public int floorDepth() { return 10; }
        void connect() { states.forEach(Eln.simulator::addElectricalLoad);parts.forEach(Eln.simulator::addElectricalComponent); }
        void cleanup() { if(clock!=null)Eln.simulator.removeElectricalProcess(clock);for(var p:parts) { Eln.simulator.removeElectricalComponent(p);p.breakConnection(); }states.forEach(Eln.simulator::removeElectricalLoad); }
    }
    private static HouseServiceElement place(TestContext c,int id,BlockPos at,Direction front) {
        var d=Eln.transparentNodeItem.getDescriptor(id);var wall=front.getInverse().toEnumFacing();
        for(int dy=-1;dy<=1;dy++) { c.level().setBlockAndUpdate(at.relative(wall).offset(0,dy,0),Blocks.STONE.defaultBlockState());if(((HouseServiceDescriptor)d).getWide())c.level().setBlockAndUpdate(at.relative(front.right().toEnumFacing()).relative(wall).offset(0,dy,0),Blocks.STONE.defaultBlockState()); }
        var n=((TransparentNodeItem)d.parentItem).placeAt(d.newItemStack(),net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(c.level()),c.level(),at,front.getInverse());
        if(n==null)throw new IllegalStateException("service placement failed");return (HouseServiceElement)n.element;
    }
    private static State wire(TestContext c,BlockPos at,Direction mount) {
        var e=c.placeSix(ElnDeviceRegistry.highVoltageCableDescriptor.parentItemDamage,at,mount);
        return e.getElectricalLoad(LRDU.Up);
    }
    private static net.minecraft.world.phys.AABB gap(Direction front) {
        return switch(front) { case XN->new net.minecraft.world.phys.AABB(.1,.3,.3,.3,.7,.7);case XP->new net.minecraft.world.phys.AABB(.7,.3,.3,.9,.7,.7);case ZN->new net.minecraft.world.phys.AABB(.3,.3,.1,.7,.7,.3);default->new net.minecraft.world.phys.AABB(.3,.3,.7,.7,.7,.9); };
    }
    private static void panel(TestContext c,HouseServiceElement e) {
        var desc=e.getDesc();var bytes=new ByteArrayOutputStream();e.networkSerialize(new DataOutputStream(bytes));
        var render=new HouseServiceRender((TransparentNodeEntity)c.level().getBlockEntity(e.node.coordonate.pos()),desc);render.networkUnserialize(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
        c.check("service: live panel retains actual meter registers",render.getDisplay().getImported()==e.getUnit().getEnergy().getImportedKwh() && render.getDisplay().getExported()==e.getUnit().getEnergy().getExportedKwh(),"");
        GlFrame.beginRenderFrame();var f=GlFrame.open();try { desc.drawService(e.front,render.getDisplay(),0,0);int n=f.batches().values().stream().mapToInt(GlFrame.Batch::vertices).sum();c.check("service: native enclosure, meter face or disconnect geometry",n>100,"vertices="+n); }finally { GlFrame.close(); }
    }
    private static final class Wired extends Fixture {
        final Direction front;final boolean before,wide,manual;HouseServiceElement meter;State input,output;Resistor load;double elapsed;int phase;ItemStack held=ItemStack.EMPTY;String serial;
        java.util.function.Predicate<NodeBase> previous;boolean frozen;
        Wired(Direction front,boolean before,boolean wide,boolean manual) { this.front=front;this.before=before;this.wide=wide;this.manual=manual; }
        public String id() { return "forkpatch_service_"+(manual?"manual_disconnect":wide?"wide_"+front.name().toLowerCase(Locale.ROOT)+(before?"_wire_first":"_meter_first"):"compact_meter"); }
        public int ticks() { return 180; }
        public void build(TestContext c) {
            var at=c.at(4,2,4);var mount=front.getInverse();
            for(int dy=-1;dy<=1;dy++) { c.level().setBlockAndUpdate(at.relative(mount.toEnumFacing()).offset(0,dy,0),Blocks.STONE.defaultBlockState());if(wide)c.level().setBlockAndUpdate(at.relative(front.right().toEnumFacing()).relative(mount.toEnumFacing()).offset(0,dy,0),Blocks.STONE.defaultBlockState()); }
            var in=wide?at.below():at.above();var out=wide?at.below().relative(front.right().toEnumFacing()):at.below();
            if(before) { input=wire(c,in,mount);output=wire(c,out,mount); }
            meter=place(c,manual?7992:wide?8128:7987,at,front);
            if(!before) { input=wire(c,in,mount);output=wire(c,out,mount); }
            var feed=new VoltageSource("actual wall input",input,null);feed.setU(240);parts.add(feed);load=new Resistor(output,null).setR(24);parts.add(load);connect();
            clock=dt->{elapsed+=dt;
                if(phase==0 && elapsed>=1) {
                    if(!manual) { c.check("service: empty socket has no cable bypass",load.getP()<1e-7,"P="+load.getP());meter.getInventory().setItem(0,Eln.findItemStack("Smart Digital Service Meter",1));serial=ServiceMeters.INSTANCE.data(meter.rawMeter()).getStringOr("serial",""); }
                    phase=1;
                }
                if(phase==1 && elapsed>=2) { c.check("service: placed cables carry actual customer load",load.getP()>2300,"P="+load.getP());meter.setRemoteDisconnect(true);phase=2; }
                if(phase==2 && elapsed>=3) { c.check("service: open contacts isolate downleads and retain line power",load.getP()<1e-7 && meter.getUnit().getLine().get(2).getU()>239,"P="+load.getP());meter.setRemoteDisconnect(false);phase=3; }
                if(phase==3 && elapsed>=4) {
                    if(!manual) { held=meter.getInventory().getItem(0);meter.getInventory().clearContent(); }
                    else meter.setRemoteDisconnect(true);
                    phase=4;
                }
                if(phase==4 && elapsed>=5) {
                    c.check("service: inventory clearing or manual opening cannot leave a bypass",load.getP()<1e-7,"P="+load.getP());
                    if(!manual)meter.getInventory().setItem(0,held);else meter.setRemoteDisconnect(false);
                    if(wide && before && front==Direction.XN) {
                        c.level().getServer().schedule(new net.minecraft.server.TickTask(c.level().getServer().getTickCount(),()->{
                            previous=NodeBase.simulationVeto;NodeBase.simulationVeto=n->n==meter.node || previous!=null && previous.test(n);frozen=true;meter.node.reconnect();
                            var a=NodeManager.instance.getNodeFromCoordonate(new Coordonate(in,c.level()));var b=NodeManager.instance.getNodeFromCoordonate(new Coordonate(out,c.level()));a.reconnect();b.reconnect();
                        }));
                    }
                    phase=5;
                }
                if(phase==5 && elapsed>=6) {
                    if(frozen) { c.check("service: frozen meter cannot gain a cable bypass",load.getP()<1e-7,"P="+load.getP());c.level().getServer().schedule(new net.minecraft.server.TickTask(c.level().getServer().getTickCount(),()->{NodeBase.simulationVeto=previous;frozen=false;meter.node.reconnect();})); }
                    phase=6;
                }
            };Eln.simulator.addElectricalProcess(clock);
        }
        public void measure(TestContext c) {
            try {
                c.check("service: restored contacts supply customer after reconnect",phase==6 && load.getP()>2300,"P="+load.getP());
                if(!manual) { var data=ServiceMeters.INSTANCE.data(meter.getInventory().getItem(0));c.check("service: serial and import register follow the actual meter",serial.equals(data.getStringOr("serial","")) && data.getDoubleOr("meterImportKwh",0)>0,""); }
                if(wide) {
                    var ghost=meter.node.coordonate.pos().relative(front.right().toEnumFacing());var state=c.level().getBlockState(ghost);
                    c.check("service: reserved half uses directional thin ghost",state.getBlock()==Eln.ghostBlock && state.getValue(mods.eln.compat.BlockMeta.META)==mods.eln.ghost.GhostBlock.wallServiceMeta(front),"");
                    for(var p:List.of(meter.node.coordonate.pos(),ghost)) { var shape=c.level().getBlockState(p).getCollisionShape(c.level(),p);c.check("service: no invisible full cube in front of either meter half",shape.toAabbs().stream().noneMatch(b->b.intersects(gap(front))),front.name()); }
                    c.level().setBlock(ghost,Eln.ghostBlock.defaultBlockState().setValue(mods.eln.compat.BlockMeta.META,0),net.minecraft.world.level.block.Block.UPDATE_CLIENTS);WideMeterWiring.INSTANCE.syncGhostShape(meter);
                    c.check("service: loaded legacy cube ghost migrates to thin shape",c.level().getBlockState(ghost).getValue(mods.eln.compat.BlockMeta.META)==mods.eln.ghost.GhostBlock.wallServiceMeta(front),"");
                }
                panel(c,meter);
            }finally { if(frozen)NodeBase.simulationVeto=previous;cleanup(); }
        }
    }
    private static final class Passive extends Fixture {
        final boolean handhole;mods.eln.node.transparent.TransparentNodeElement element;Resistor load;
        Passive(boolean h) { handhole=h; }
        public String id() { return "forkpatch_service_"+(handhole?"handhole":"underground_riser"); }
        public void build(TestContext c) {
            State port;
            if(handhole) { element=c.placeTransparent(8130,c.at(4,1,4));port=((UndergroundHandholeElement)element).getService(); }
            else { element=place(c,8129,c.at(4,2,4),Direction.XN);port=((HouseServiceElement)element).getUnit().getLine().get(2); }
            var feed=new mods.eln.sim.nbt.NbtElectricalLoad("buried feed");feed.setRs(.00001);states.add(feed);
            var supply=new VoltageSource("utility",feed,null);supply.setU(240);parts.add(supply);parts.add(new mods.eln.sim.ElectricalConnection(feed,(mods.eln.sim.ElectricalLoad)port));load=new Resistor(port,null).setR(24);parts.add(load);connect();
        }
        public void measure(TestContext c) {
            try { c.check("service: passive buried equipment passes real power",load.getP()>2390,"P="+load.getP());
                if(handhole) { c.check("service: handhole has sides/bottom and no top electrical entry",element.getElectricalLoad(Direction.YP,LRDU.Down)==null && element.getElectricalLoad(Direction.YN,LRDU.Down)!=null,"");var desc=(UndergroundHandholeDescriptor)element.getDescriptor();GlFrame.beginRenderFrame();var f=GlFrame.open();try { desc.draw();c.check("service: native buried splice enclosure geometry",f.batches().values().stream().mapToInt(GlFrame.Batch::vertices).sum()>100,""); }finally { GlFrame.close(); } }
                else { var e=(HouseServiceElement)element;c.check("service: riser derives proportional legs",Math.abs(e.getUnit().getLine().get(0).getU()-120)<.1 && Math.abs(e.getUnit().getLine().get(1).getU()-120)<.1,"");panel(c,e); }
            }finally { cleanup(); }
        }
    }
    private static final class Items implements TestCase {
        HouseServiceElement e;
        public String id() { return "forkpatch_service_meter_item_inventory"; }
        public void build(TestContext c) { e=place(c,7987,c.at(4,2,1),Direction.XN); }
        public void measure(TestContext c) {
            var inv=e.getInventory();var analog=Eln.findItemStack("Analog Glass Service Meter",1);analog.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Billing meter"));inv.setItem(0,analog);e.getUnit().getEnergy().step(3600,1000);
            var first=inv.removeItemNoUpdate(0);var data=ServiceMeters.INSTANCE.data(first);var serial=data.getStringOr("serial","");
            c.check("service: native extraction flushes register and preserves identity",data.getDoubleOr("meterImportKwh",0)==1 && !serial.isBlank() && first.getHoverName().getString().equals("Billing meter"),"");
            c.check("service: empty socket has no inherited meter credit",e.getUnit().getMeterType()==0 && e.getUnit().getEnergy().getImportedKwh()==0,"");
            inv.setItem(0,Eln.findItemStack("Smart Digital Service Meter",1));e.getUnit().getEnergy().step(1800,-2000);e.setRemoteDisconnect(true);var second=inv.removeItemNoUpdate(0);var smart=ServiceMeters.INSTANCE.data(second);
            c.check("service: smart export register and open state follow its item",smart.getDoubleOr("meterExportKwh",0)==1 && smart.getBooleanOr("meterDisconnected",false) && !smart.getStringOr("serial","").equals(serial),"");
            inv.setItem(0,first);c.check("service: swapping meters restores only that meter's registers",e.getUnit().getEnergy().getImportedKwh()==1 && e.getUnit().getEnergy().getExportedKwh()==0,"");
            var saved=new CompoundTag();e.writeToNBT(saved);var restored=new HouseServiceElement(new TransparentNode(),e.getDescriptor());restored.front=e.front;restored.readFromNBT(saved);
            c.check("service: world NBT retains item identity and exact register",restored.getUnit().getEnergy().getImportedKwh()==1 && ServiceMeters.INSTANCE.data(restored.rawMeter()).getStringOr("serial","").equals(serial),"");
            c.check("service: socket carry data cannot clone installed meter registers",!e.getItemStackNBT().contains("meterImportKwh") && !e.getItemStackNBT().contains("inv"),"");
            var player=net.neoforged.neoforge.common.util.FakePlayerFactory.get(c.level(),new com.mojang.authlib.GameProfile(UUID.fromString("e1a0b2c3-0000-4000-8000-000000000175"),"eln_service_menu"));var spec=MenuSpecs.of(e.getDescriptor());var menu=new ElnNodeMenu(1,player.getInventory(),inv,spec,e.node,e.node.coordonate.pos(),0,ElnNodeMenu.TRANSPARENT,7987);
            c.check("service: actual native meter slot rejects unrelated hardware",menu.deviceSlots==1 && !menu.getSlot(0).mayPlace(Eln.findItemStack(PoleHardware.INSTANCE.getTransformerNames().get(0),1)),"");menu.quickMoveStack(player,0);
            boolean found=false;for(int i=0;i<player.getInventory().getContainerSize();i++) { var d=ServiceMeters.INSTANCE.data(player.getInventory().getItem(i));if(d!=null && d.getStringOr("serial","").equals(serial) && d.getDoubleOr("meterImportKwh",0)==1)found=true; }
            c.check("service: shift-click retains the physical meter ledger",found && inv.isEmpty(),"");
            inv.setItem(0,second);e.getUnit().getEnergy().step(3600,1000);inv.clearContent();c.check("service: native clear flushes the last reading before removal",ServiceMeters.INSTANCE.data(second).getDoubleOr("meterImportKwh",0)==1,"");
        }
    }
}
