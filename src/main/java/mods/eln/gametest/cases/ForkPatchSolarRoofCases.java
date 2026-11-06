package mods.eln.gametest.cases;

import mods.eln.Eln;
import mods.eln.compat.BlockMeta;
import mods.eln.gametest.*;
import mods.eln.gl.GlFrame;
import mods.eln.misc.Coordonate;
import mods.eln.node.NodeManager;
import mods.eln.node.transparent.*;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.transparentnode.solarpanel.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import java.io.*;
import java.util.*;

/** Actual item placement, saved nodes, replicated geometry and real solar circuits on native stairs. */
public final class ForkPatchSolarRoofCases {
    static final String[] PANELS = {"Small Solar Panel", "Small Rotating Solar Panel", "2x3 Solar Panel", "2x3 Rotating Solar Panel"};
    public static List<TestCase> cases() {
        List<TestCase> result = new ArrayList<>();
        for (int type=0;type<4;type++) for (Direction up:Direction.Plane.HORIZONTAL) result.add(new Roof(type,up));
        result.add(new Blocked()); result.add(new LegacyAndPassive());
        return result;
    }
    static class Base implements TestCase {
        public String id() { return "forkpatch_solar_roof_base"; }
        public int width() { return 10; } public int height() { return 5; } public int depth() { return 10; }
        public int floorDepth() { return 10; }
        public void build(TestContext c) {}
        public void measure(TestContext c) {}
        SolarPanelDescriptor descriptor(int type) { return (SolarPanelDescriptor)Eln.transparentNodeItem.getDescriptor(SolarCases.transparent(PANELS[type])); }
        BlockPos origin(TestContext c) { return c.at(5,1,5); }
        void stairs(TestContext c,Direction up) {
            BlockPos origin=origin(c);
            for(int row=0;row<2;row++)for(int across=-1;across<=1;across++) {
                BlockPos support=origin.offset(row*up.getStepX()-across*up.getStepZ(),row-1,row*up.getStepZ()+across*up.getStepX());
                c.level().setBlock(support,Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING,up),2);
            }
        }
        TransparentNode place(TestContext c,SolarPanelDescriptor d) {
            return ((TransparentNodeItem)d.parentItem).placeAt(d.newItemStack(),net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(c.level()),c.level(),origin(c),mods.eln.misc.Direction.YN);
        }
    }
    static final class Roof extends Base {
        final int type; final Direction up; SolarPanelElement panel; Resistor load;
        Roof(int type,Direction up) { this.type=type;this.up=up; }
        public String id() { return "forkpatch_solar_roof_"+type+"_"+up.getSerializedName(); }
        public void build(TestContext c) {
            c.level().getServer().getCommands().performPrefixedCommand(c.level().getServer().createCommandSourceStack().withSuppressedOutput(),"time set noon");
            c.level().getServer().setWeatherParameters(100000,0,false,false);
            stairs(c,up);
            panel=(SolarPanelElement)Objects.requireNonNull(place(c,descriptor(type))).element;
            load=SolarCases.attachLoad(panel);
        }
        public void measure(TestContext c) {
            var d=descriptor(type);var origin=origin(c);
            c.check("roof: real placement follows stair uphill rather than player facing",panel.roofMountKnown && panel.roofMount.getUphill()==up && panel.front.toEnumFacing()==up.getOpposite(),"");
            var state=c.level().getBlockState(origin);
            c.check("roof: root block replicates the saved pitch",state.getValue(SolarPanelBlock.ROOF)==up.get3DDataValue()-1,"");
            c.check("roof: root collision is the cached thin slope",state.getCollisionShape(c.level(),origin)==SolarPanelBlock.roofShape(up.get3DDataValue()-2),"");
            int ghosts=0,high=0;
            for(var cell:d.getGhostGroupAt(panel.node.coordonate,panel.front).elementList) {
                ghosts++;if(cell.y==1)high++;
                var p=origin.offset(cell.x,cell.y,cell.z);var gs=c.level().getBlockState(p);
                c.check("roof: every reservation has a client-readable slope",gs.getBlock()==Eln.ghostBlock && gs.getValue(BlockMeta.META)==up.get3DDataValue()+7 && gs.getCollisionShape(c.level(),p)==SolarPanelBlock.roofShape(up.get3DDataValue()-2),p.toString());
                c.check("roof: stairs below reservations remain intact",c.level().getBlockState(p.below()).getBlock() instanceof StairBlock,p.toString());
            }
            c.check("roof: only large arrays reserve the three uphill cells",ghosts==(type>=2?5:0) && high==(type>=2?3:0),ghosts+"/"+high);
            if(type>=2) {
                Coordonate offset=panel.roofMount.groundOffset(new Coordonate(1,0,0,0));offset.applyTransformation(panel.front,panel.node.coordonate);
                var ground=NodeManager.instance.getNodeFromCoordonate(offset);
                c.check("roof: return terminal is a real node in an uphill ghost",ground instanceof mods.eln.misc.GhostPowerNode && Eln.ghostManager.getGhost(offset)!=null && ((mods.eln.misc.GhostPowerNode)ground).getLoad()!=null,offset.toString());
            }
            SolarCases.checkLoadedCurve(c,panel,load);
            SolarPannelSlowProcess slow=Batch1Cases.field(panel,"slowProcess");
            c.check("roof: actual noon production follows pitched incidence",slow.getSolarLight()>.45 && slow.getSolarLight()<.9,"light "+slow.getSolarLight());
            if(d.canRotate) {
                try {
                    for(float requested:new float[]{Float.NaN,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY}) {
                        double before=panel.panelAlpha;
                        var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes);out.writeByte(SolarPanelElement.unserializePannelAlpha);out.writeFloat(requested);
                        panel.networkUnserialize(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
                        c.check("roof: nonfinite manual packets preserve the existing angle",panel.panelAlpha==before,"");
                    }
                    panel.getInventory().setItem(0,Eln.sharedItem.getDescriptor(704).newItemStack());
                    slow.getSolarLight();
                    c.check("roof: tracker uses the pitched rotation axis",Math.abs(panel.panelAlpha-d.alphaTrunk(panel.roofMount.targetAlpha(slow.getSolarAlpha())))<1e-12,"");
                }catch(IOException e) { throw new RuntimeException(e); }
            }
            CompoundTag tag=new CompoundTag();panel.writeToNBT(tag);
            var old=new SolarPanelElement(new TransparentNode(),d);tag.remove("roofUphill");tag.putDouble("panelAlpha",Double.NaN);old.readFromNBT(tag);
            c.check("roof: old saves remain flat with a finite manual angle",old.roofMountKnown && !old.roofMount.getPitched() && Double.isFinite(old.panelAlpha),"");
            try {
                panel=(SolarPanelElement)((TransparentNode)Reload.node(c.level(),origin).fresh).element;
                c.check("roof: real node reload retains pitch without touching support",panel.roofMountKnown && panel.roofMount.getUphill()==up,"");
                var stairState=c.level().getBlockState(origin.below());
                c.level().setBlock(origin.below(),stairState.setValue(StairBlock.FACING,up.getOpposite()),2);
                c.check("roof: later support rotation cannot rewrite a saved array's pitch",SolarRoofMount.at(c.level(),origin).getUphill()==up && c.level().getBlockState(origin).getValue(SolarPanelBlock.ROOF)==up.get3DDataValue()-1,"");
                c.level().setBlock(origin.below(),stairState,2);
                publish(c,panel);
            }catch(Exception e) { throw new RuntimeException(e); }
            if(type>=2) {
                var cell=d.getGhostGroupAt(panel.node.coordonate,panel.front).elementList.getFirst();
                c.level().destroyBlock(origin.offset(cell.x,cell.y,cell.z),false);
                c.check("roof: breaking any reservation removes the root and its return terminal",NodeManager.instance.getNodeFromCoordonate(new Coordonate(origin,c.level()))==null && c.level().getBlockState(origin).isAir(),"");
                for(var other:d.getGhostGroupAt(panel.node.coordonate,panel.front).elementList)c.check("roof: teardown leaves no ghost reservations",Eln.ghostManager.getGhost(new Coordonate(origin.offset(other.x,other.y,other.z),c.level()))==null,"");
            }
        }
        void publish(TestContext c,SolarPanelElement p)throws IOException {
            mods.eln.client.ClientRenders.registerAll();var origin=origin(c);
            var client=new TransparentNodeEntity(origin,c.level().getBlockState(origin));client.setLevel(c.level());
            try {
                var in=new DataInputStream(new ByteArrayInputStream(p.node.getPublishPacket().toByteArray()));in.readByte();for(int i=0;i<4;i++)in.readInt();in.readUTF();client.serverPublishUnserialize(in);
                var render=client.elementRender instanceof SolarPanelRender r?r:null;
                c.check("roof: actual publication carries the saved mount and consumes all bytes",render!=null && render.roofMountKnown && render.roofMount.getUphill()==up && in.available()==0,"");
                if(render!=null) {
                    GlFrame.beginRenderFrame();var frame=GlFrame.open();try { render.refresh(.1f);render.draw();c.check("roof: pitched source panel geometry reaches the native renderer",frame.batches().values().stream().mapToInt(GlFrame.Batch::vertices).sum()>100,"");
                        boolean bounded=true;
                        for(var batch:frame.batches().values())for(int i=0;i<batch.floats;i+=GlFrame.STRIDE)for(int axis=0;axis<3;axis++)bounded &= Float.isFinite(batch.data[i+axis]) && Math.abs(batch.data[i+axis])<=3.5;
                        c.check("roof: pitched mesh stays within the native three-block culling margin",bounded,""); }finally { GlFrame.close(); }
                }
            }finally { client.clientDestroy(); }
        }
    }
    static final class Blocked extends Base {
        public String id() { return "forkpatch_solar_roof_blocked_uphill_reservation_is_atomic"; }
        public void measure(TestContext c) {
            Direction up=Direction.EAST;stairs(c,up);var blocked=origin(c).offset(1,1,0);c.level().setBlock(blocked,Blocks.STONE.defaultBlockState(),2);
            c.check("roof: obstructed uphill row prevents placement",place(c,descriptor(3))==null,"");
            c.check("roof: rejected placement preserves obstruction and creates no root",c.level().getBlockState(blocked).is(Blocks.STONE) && c.level().getBlockState(origin(c)).isAir() && NodeManager.instance.getNodeFromCoordonate(new Coordonate(origin(c),c.level()))==null,"");
            for(int z=-1;z<=1;z++)c.check("roof: rejected placement creates no lateral ghosts",Eln.ghostManager.getGhost(new Coordonate(origin(c).offset(0,0,z),c.level()))==null,"");
        }
    }
    static final class LegacyAndPassive extends Base {
        public String id() { return "forkpatch_solar_roof_flat_legacy_and_unloaded_chunk_probes"; }
        public void measure(TestContext c) {
            var p=origin(c);var node=Objects.requireNonNull(place(c,descriptor(2)));var panel=(SolarPanelElement)node.element;
            c.check("roof: ordinary floor placement retains flat orientation and footprint",!panel.roofMount.getPitched() && descriptor(2).getGhostGroupAt(node.coordonate,panel.front).elementList.stream().allMatch(g->g.y==0),"");
            c.check("roof: flat nonrotating panel retains half-height shape",Math.abs(c.level().getBlockState(p).getCollisionShape(c.level(),p).bounds().maxY-.5)<1e-12,"");
            var unloaded=new BlockPos(30000000-1024,80,30000000-1024);int cx=unloaded.getX()>>4,cz=unloaded.getZ()>>4;
            c.check("roof: probe location starts unloaded",c.level().getChunkSource().getChunkNow(cx,cz)==null,"");
            c.check("roof: passive support and entity reads return no unloaded data",mods.eln.node.LoadedBlockStates.get(c.level(),unloaded)==null && mods.eln.node.LoadedBlockEntities.INSTANCE.get(c.level(),unloaded)==null && !SolarRoofMount.at(c.level(),unloaded).getPitched(),"");
            c.check("roof: passive shape probes cannot force terrain loading",c.level().getChunkSource().getChunkNow(cx,cz)==null,"");
            try {
                var restored=(SolarPanelElement)((TransparentNode)Reload.node(c.level(),p).fresh).element;
                c.check("roof: flat legacy footprint survives a real owner reload",restored.roofMountKnown && !restored.roofMount.getPitched(),"");
            }catch(ReflectiveOperationException e) { throw new RuntimeException(e); }
            // Return nodes are intentionally unsaved and reconstructed by their owning array.
            c.level().destroyBlock(p,false);
            c.check("roof: flat array cleanup removes the owner and its unsaved return node",NodeManager.instance.getNodeFromCoordonate(new Coordonate(p,c.level()))==null,"");
            c.level().setBlock(p.below(),Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.HALF,net.minecraft.world.level.block.state.properties.Half.TOP).setValue(StairBlock.FACING,Direction.EAST),2);
            var top=(SolarPanelElement)Objects.requireNonNull(place(c,descriptor(1))).element;
            c.check("roof: top-half stairs retain ordinary flat mounting",!top.roofMount.getPitched() && c.level().getBlockState(p).getValue(SolarPanelBlock.ROOF)==0,"");
        }
    }
}
