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
import java.io.*;
import java.util.*;

/** Native registered controls, actual signal terminals, display packets, menus and persistent faults. */
public final class ForkPatchIndustrialCases {
    private ForkPatchIndustrialCases() { }
    public static List<TestCase> cases() { return List.of(new Relay(0),new Relay(1),new Relay(2),new Shed(0),new Shed(1),new Shed(2)); }
    private abstract static class Fixture implements TestCase {
        final List<Component> parts=new ArrayList<>();IProcess clock;IndustrialElement element;VoltageSource feed;
        void place(TestContext ctx,int id,double volts) {
            element=(IndustrialElement)ctx.placeTransparent(id,ctx.at(3,1,1));
            feed=new VoltageSource("control supply",element.getUnit().getInput(),null);feed.setU(volts);parts.add(feed);
        }
        void connect() { for(var p:parts)Eln.simulator.addElectricalComponent(p); }
        void cleanup() { if(clock!=null)Eln.simulator.removeElectricalProcess(clock);for(var p:parts) { Eln.simulator.removeElectricalComponent(p);p.breakConnection(); } }
        void panel(TestContext ctx) {
            var u=element.getUnit();var kind=element.getKind();
            var desc=(IndustrialDescriptor)element.getDescriptor();var bytes=new ByteArrayOutputStream();element.networkSerialize(new DataOutputStream(bytes));
            var render=new IndustrialRender((TransparentNodeEntity)ctx.level().getBlockEntity(element.node.coordonate.pos()),desc);render.networkUnserialize(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
            ctx.check("controls: publication retains actual status and input power",render.getDisplay().getTrip()==u.getProtection().getTripped() && Math.abs(render.getDisplay().getWatts()-u.getInputWatts())<1e-6,"");
            ctx.check("controls: all branch states reach live panel",Arrays.equals(render.getDisplay().getClosed(),u.getShed().getClosed()),"");
            GlFrame.beginRenderFrame();var frame=GlFrame.open();
            try { desc.draw(element.front,render.getDisplay());int n=frame.batches().values().stream().mapToInt(GlFrame.Batch::vertices).sum();ctx.check("controls: native enclosure, legends, lamps and digits",n>2000,"vertices="+n); }
            finally { GlFrame.close(); }
            var spec=MenuSpecs.of(desc);ctx.check("controls: native menu has controls without phantom inventory slots",!spec.playerInventory() && spec.slots().isEmpty() && spec.width()==368,"");
            var original=element.front;
            try { for(var front:List.of(Direction.XN,Direction.XP,Direction.ZN,Direction.ZP)) {
                element.front=front;
                ctx.check("controls: input terminal follows orientation",element.getElectricalLoad(front.left(),LRDU.Down)==u.getInput(),front.name());
                ctx.check("controls: wired control and trip signal terminals",element.getElectricalLoad(front,LRDU.Down)==element.getSignal() && element.getElectricalLoad(front,LRDU.Up)==element.getTripSignal(),front.name());
                if(kind==IndustrialKind.SHEDDER)for(var lrdu:LRDU.values())ctx.check("controls: branch order follows physical LRDU",element.getElectricalLoad(front.right(),lrdu)==u.getOutputs().get(lrdu.dir),lrdu.name());
                else ctx.check("controls: independent paired return terminals",element.getElectricalLoad(front.left(),LRDU.Right)==u.getReturnIn() && element.getElectricalLoad(front.right(),LRDU.Right)==u.getReturnOut(),front.name());
            } } finally { element.front=original; }
            var saved=new net.minecraft.nbt.CompoundTag();element.writeToNBT(saved);
            var restored=new IndustrialElement(new TransparentNode(),desc);restored.front=original;restored.readFromNBT(saved);
            ctx.check("controls: native NBT preserves settings and latched faults",Arrays.equals(u.getSettings(),restored.getUnit().getSettings()) && u.getProtection().getTripped()==restored.getUnit().getProtection().getTripped(),"");
            ctx.check("controls: native NBT retains disconnected branch demand",Arrays.equals(u.getShed().getNominalDemand(),restored.getUnit().getShed().getNominalDemand()),"");
        }
    }
    private static final class Relay extends Fixture {
        final int type;Resistor leak;double elapsed;int phase;
        Relay(int type) { this.type=type; }
        public String id() { return "forkpatch_relay_"+List.of("paired_leak_reset","wired_trip","overcurrent").get(type); }
        public int ticks() { return 120; }
        public void build(TestContext ctx) {
            place(ctx,8066,480);var u=element.getUnit();
            u.applySettings(new double[]{480,type==2?10:100,.8,1.2,type==2?.5:.1,type==0?.1:0});
            if(type==0) {
                var neutral=new VoltageSource("return reference",u.getReturnIn(),null);neutral.setU(0);parts.add(neutral);parts.add(new Resistor(u.getOutputs().get(0),u.getReturnOut()).setR(24));
                leak=new Resistor(u.getOutputs().get(0),null).setR(1e19);parts.add(leak);
                clock=dt->{elapsed+=dt;
                    if(phase==0 && elapsed>=1) { ctx.check("relay: paired conductors carry healthy load",!u.getProtection().getTripped() && Math.abs(u.getContacts().get(0).getCurrent()+u.getReturnContact().getCurrent())<1e-5,"");leak.setR(2400);phase=1; }
                    if(phase==1 && elapsed>=2) { ctx.check("relay: real ground leakage latches differential fault",u.getProtection().getTripped() && u.getProtection().getReason()==3,"");u.setEnabled(false);u.setEnabled(true);ctx.check("relay: enabling does not clear latched trip",!u.getContacts().get(0).getClosed() && !u.getReturnContact().getClosed(),"");phase=2; }
                    if(phase==2 && elapsed>=3) { leak.setR(1e19);u.resetTrip();phase=3; }
                };Eln.simulator.addElectricalProcess(clock);
            } else {
                parts.add(new Resistor(u.getOutputs().get(0),null).setR(type==2?24:48));
                if(type==1) { var signal=new VoltageSource("wired relay trip",element.getSignal(),null);signal.setU(Eln.SVU);parts.add(signal); }
            }
            connect();
        }
        public void measure(TestContext ctx) {
            try { var u=element.getUnit();
                if(type==0)ctx.check("relay: reset corrected circuit restores both conductors",phase==3 && !u.getProtection().getTripped() && u.getContacts().get(0).getClosed() && u.getReturnContact().getClosed() && u.getInputWatts()>9500,"");
                else { ctx.check("relay: native fault opens both conductors",u.getProtection().getTripped() && u.getProtection().getReason()==(type==1?5:2) && !u.getContacts().get(0).getClosed() && !u.getReturnContact().getClosed(),"");ctx.check("relay: actual gate output publishes latched trip",element.getTripSignal().getU()>Eln.SVU*.5,""); }
                panel(ctx);
            } finally { cleanup(); }
        }
    }
    private static final class Shed extends Fixture {
        final int type;final List<Resistor> loads=new ArrayList<>();double elapsed;int phase;VoltageSource inhibit;
        Shed(int type) { this.type=type; }
        public String id() { return "forkpatch_shedder_"+List.of("headroom_retry","voltage_restore","wired_inhibit").get(type); }
        public int ticks() { return type==1?300:type==2?260:240; }
        public void build(TestContext ctx) {
            place(ctx,8070,type==1?190:240);var u=element.getUnit();u.applySettings(new double[]{240,type==0?1000:50000});
            for(int i=0;i<4;i++) { var r=new Resistor(u.getOutputs().get(i),null).setR(type==0 && i==3?72:576);loads.add(r);parts.add(r); }
            if(type==2) { inhibit=new VoltageSource("wired inhibit",element.getSignal(),null);inhibit.setU(Eln.SVU);parts.add(inhibit); }
            clock=dt->{elapsed+=dt;
                if(type==0) {
                    if(phase==0 && elapsed>=2) { ctx.check("shedder: only lowest priority branch shed",Arrays.equals(u.getShed().getClosed(),new boolean[]{true,true,true,false}),"");phase=1; }
                    if(phase==1 && elapsed>=4) { ctx.check("shedder: remembered 800 W branch waits for headroom",u.getShed().getStatus().equals("HEADROOM") && Math.abs(u.getShed().getNominalDemand()[3]-800)<.2,"");loads.get(3).setR(576);u.retryShed();ctx.check("shedder: retry still respects restoration delay",!u.getContacts().get(3).getClosed(),"");phase=2; }
                } else if(type==1) {
                    if(phase==0 && elapsed>=2.2) { ctx.check("shedder: undervoltage sheds P4 then P3",Arrays.equals(u.getShed().getClosed(),new boolean[]{true,true,false,false}),"");feed.setU(240);phase=1; }
                    if(phase==1 && elapsed>=7.4) { ctx.check("shedder: restores P3 before P4",Arrays.equals(u.getShed().getClosed(),new boolean[]{true,true,true,false}),"");phase=2; }
                } else if(phase==0 && elapsed>=2) { ctx.check("shedder: actual wired inhibit opens all branches",u.getContacts().stream().noneMatch(c->c.getClosed()),"");inhibit.setU(0);phase=1; }
            };Eln.simulator.addElectricalProcess(clock);connect();
        }
        public void measure(TestContext ctx) {
            try { var u=element.getUnit();
                if(type==2)ctx.check("shedder: restoration preserves P1 priority after wired release",u.getContacts().get(0).getClosed() && u.getContacts().get(1).getClosed() && !u.getContacts().get(2).getClosed() && !u.getContacts().get(3).getClosed(),Arrays.toString(u.getShed().getClosed()));
                else ctx.check("shedder: corrected supply/load restores every branch",u.getContacts().stream().allMatch(c->c.getClosed()),u.getShed().getStatus());
                for(int i=0;i<4;i++)ctx.check("shedder: displayed power is actual branch load",Math.abs(u.getBranchWatts()[i]-loads.get(i).getP())<.2,"branch="+i);
                panel(ctx);
            } finally { cleanup(); }
        }
    }
}
