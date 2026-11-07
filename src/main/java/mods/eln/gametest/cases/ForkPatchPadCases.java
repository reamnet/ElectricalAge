package mods.eln.gametest.cases;

import mods.eln.Eln;
import mods.eln.gametest.*;
import mods.eln.gl.GlFrame;
import mods.eln.gui.*;
import mods.eln.misc.*;
import mods.eln.node.transparent.*;
import mods.eln.sim.IProcess;
import mods.eln.sim.mna.component.*;
import mods.eln.transparentnode.industrial.*;
import mods.eln.utility.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import java.io.*;
import java.util.*;

public final class ForkPatchPadCases {
    private ForkPatchPadCases() { }
    public static List<TestCase> cases() { return List.of(new Pad(0),new Pad(1),new Pad(2),new Pad(3),new Pad(4),new Pad(5)); }
    private static final class Pad implements TestCase {
        final int mode;PadTransformerElement element;VoltageSource feed;final List<Component> parts=new ArrayList<>();final List<Resistor> loads=new ArrayList<>();IProcess clock;double elapsed;int phase;double full;
        Pad(int mode) { this.mode=mode; }
        public String id() { return "forkpatch_pad_"+List.of("145a_secondary","core_guard","primary_fuse_replace","200a_short","split_service_sag","shared_service_guard").get(mode); }
        public int ticks() { return 160; }
        public void build(TestContext ctx) {
            element=(PadTransformerElement)ctx.placeTransparent(8068,ctx.at(3,1,1));
            int core=mode==1?1:mode>=4?0:2;
            element.getInventory().setItem(0,Eln.findItemStack(PoleHardware.INSTANCE.getTransformerNames().get(core),1));
            element.getInventory().setItem(1,Eln.findItemStack(PoleHardware.INSTANCE.getFuseNames().get(mode==2?0:10),1));
            var u=element.getUnit();feed=new VoltageSource("pad primary",u.getInput(),null);feed.setU(12470);parts.add(feed);
            if(mode>=4)for(int i=0;i<3;i++) {
                double r=mode==4?(i==2?96:48):(i==2?12:3);
                var load=new Resistor(u.getService().getLoads().get(i),null).setR(r);loads.add(load);parts.add(load);
            } else { var load=new Resistor(u.getOutput(),null).setR(mode==3?.0001:240.0/145);loads.add(load);parts.add(load); }
            for(var p:parts)Eln.simulator.addElectricalComponent(p);
            clock=dt->{elapsed+=dt;
                if((mode==2 || mode==3) && phase==0 && elapsed>=2.5) {
                    ctx.check("pad: actual primary overcurrent spends physical fuse",u.getProtection().getReason()==2 && PoleHardware.INSTANCE.fuse(element.getInventory().getItem(1)).getSpent(),u.getStatus());
                    u.resetTrip();ctx.check("pad: reset alone cannot bypass spent cartridge",!u.getTap().getEnabled(),"");
                    loads.get(0).setR(24);element.getInventory().setItem(1,Eln.findItemStack(PoleHardware.INSTANCE.getFuseNames().get(10),1));phase=1;
                }
                if(mode==4) {
                    if(phase==0 && elapsed>=1) { full=u.getOutput().getU();ctx.check("pad: both legs derive from full secondary",Math.abs(u.getService().getLoads().get(0).getU()-full*.5)<.01 && Math.abs(u.getService().getLoads().get(1).getU()-full*.5)<.01,"");feed.setU(11846.5);phase=1; }
                    if(phase==1 && elapsed>=2) { ctx.check("pad: secondary follows actual primary sag",Math.abs(u.getOutput().getU()-full*.95)<1e-5,"");feed.setU(12470);u.applySettings(new double[]{12470,208});phase=2; }
                }
            };Eln.simulator.addElectricalProcess(clock);
        }
        public void measure(TestContext ctx) {
            try {
                var u=element.getUnit();var fuse=PoleHardware.INSTANCE.fuse(element.getInventory().getItem(1));
                if(mode==0) {
                    ctx.check("pad: 145 A secondary does not blow a 200 A primary link",loads.get(0).getCurrent()>140 && u.getPrimaryAmps()>2.7 && u.getPrimaryAmps()<2.9 && !u.getProtection().getTripped() && !fuse.getSpent(),u.getStatus());
                    ctx.check("pad: native primary current meter uses correct terminal",Math.abs(u.getInput().getI()-u.getPrimaryAmps())<1e-4,"");
                } else if(mode==1 || mode==5)ctx.check("pad: shared core overload leaves healthy fuse intact",u.getProtection().getReason()==7 && !fuse.getSpent() && !u.getTap().getEnabled(),u.getStatus());
                else if(mode==4)ctx.check("pad: programmed secondary retains proportional legs",phase==2 && Math.abs(u.getOutput().getU()-208)<.2 && Math.abs(u.getService().getLoads().get(0).getU()-104)<.2,"");
                else ctx.check("pad: replacement cartridge restores corrected real load",phase==1 && u.getTap().getEnabled() && !fuse.getSpent() && loads.get(0).getP()>2300,u.getStatus());
                checkHardware(ctx);panel(ctx);ports(ctx);
                var saved=new CompoundTag();element.writeToNBT(saved);var copy=new PadTransformerElement(new TransparentNode(),element.getDescriptor());copy.front=element.front;copy.readFromNBT(saved);
                ctx.check("pad: actual installed ratings and protection persist",copy.getUnit().getRatedWatts()==u.getRatedWatts() && copy.getUnit().getFuseAmps()==u.getFuseAmps() && copy.getUnit().getProtection().getReason()==u.getProtection().getReason(),"");
                var moved=new PadTransformerElement(new TransparentNode(),element.getDescriptor());moved.front=element.front;moved.readItemStackNBT(element.getItemStackNBT());
                ctx.check("pad: carried settings cannot copy paid hardware",moved.getInventory().isEmpty() && !moved.getUnit().getTap().getEnabled(),"");
            } finally { Eln.simulator.removeElectricalProcess(clock);for(var p:parts) { Eln.simulator.removeElectricalComponent(p);p.breakConnection(); } }
        }
        void checkHardware(TestContext ctx) {
            var inventory=element.getInventory();
            ctx.check("pad: native sided automation and correct slot filters",inventory.getAccessibleSlotsFromSide(0).length==2 && !inventory.canInsertItem(0,Eln.findItemStack(PoleHardware.INSTANCE.getFuseNames().get(10),1),0) && !inventory.canInsertItem(1,Eln.findItemStack(PoleHardware.INSTANCE.getTransformerNames().get(2),1),0) && !inventory.canInsertItem(1,Eln.findItemStack(PoleHardware.INSTANCE.getSpentName(),1),0),"");
            ctx.check("pad: native item pipe capability exposed",ctx.level().getCapability(net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK,element.node.coordonate.pos(),net.minecraft.core.Direction.UP)!=null,"");
            var spec=MenuSpecs.of(element.getDescriptor());ctx.check("pad: native menu exposes only two paid slots",spec.slots().size()==2 && spec.playerInventory() && spec.slots().get(0).limit()==1 && spec.slots().get(1).limit()==1,"");
            for(int i=0;i<5;i++) { var item=Eln.findItemStack(PoleHardware.INSTANCE.getTransformerNames().get(i),1);var d=(PoleTransformerDescriptor)mods.eln.generic.GenericItemUsingDamageDescriptor.getDescriptor(item);ctx.check("pad: physical core item has exact paid capacity",d.getWatts()==PoleHardware.INSTANCE.getTransformerRatings().get(i),"");
                GlFrame.beginRenderFrame();var frame=GlFrame.open();try { d.drawItem(item,false);int vertices=frame.batches().values().stream().mapToInt(GlFrame.Batch::vertices).sum();ctx.check("pad: native transformer item can and bushing geometry",vertices>100,"vertices="+vertices); } finally { GlFrame.close(); }
            }
        }
        void ports(TestContext ctx) {
            var u=element.getUnit();ctx.check("pad: separate underground primary and secondary entries",element.getElectricalLoad(Direction.YN,LRDU.Down)==u.getInput() && element.getElectricalLoad(Direction.YN,LRDU.Up)==u.getOutput(),"");
            ctx.check("pad: both service legs use shared physical secondary",element.getElectricalLoad(Direction.YN,LRDU.Left)==u.getService().getLoads().get(0) && element.getElectricalLoad(Direction.YN,LRDU.Right)==u.getService().getLoads().get(1),"");
            if(u.getProtection().getTripped())ctx.check("pad: actual fault gate is asserted",element.getTripSignal().getU()>Eln.SVU*.5,"");
        }
        void panel(TestContext ctx) {
            var bytes=new ByteArrayOutputStream();element.networkSerialize(new DataOutputStream(bytes));var desc=(PadTransformerDescriptor)element.getDescriptor();
            var render=new PadTransformerRender((TransparentNodeEntity)ctx.level().getBlockEntity(element.node.coordonate.pos()),desc);render.networkUnserialize(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
            ctx.check("pad: live panel publishes actual fuse/core ratings and primary current",render.getDisplay().getRatedWatts()==element.getUnit().getRatedWatts() && Math.abs(render.getDisplay().getPrimaryAmps()-element.getUnit().getPrimaryAmps())<1e-6,"");
            GlFrame.beginRenderFrame();var frame=GlFrame.open();try { desc.draw(element.front,render.getDisplay());int n=frame.batches().values().stream().mapToInt(GlFrame.Batch::vertices).sum();ctx.check("pad: native enclosure, pad, door and warning geometry",n>1000,"vertices="+n); } finally { GlFrame.close(); }
        }
    }
}
