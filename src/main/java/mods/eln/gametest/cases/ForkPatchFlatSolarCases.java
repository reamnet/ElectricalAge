package mods.eln.gametest.cases;

import mods.eln.Eln;
import mods.eln.compat.BlockMeta;
import mods.eln.gametest.*;
import mods.eln.ghost.GhostBlock;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.node.NodeManager;
import mods.eln.node.transparent.*;
import mods.eln.transparentnode.solarpanel.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import java.util.*;

/** Real new/old arrays, ordinary collision queries and server-thread state migration. */
public final class ForkPatchFlatSolarCases {
    public static List<TestCase> cases() {
        List<TestCase> result=new ArrayList<>();
        for(var front:List.of(Direction.XN,Direction.XP,Direction.ZN,Direction.ZP))result.add(new Flat(front));
        result.add(new Guards());result.add(new Rotating());return result;
    }
    static class Flat extends ForkPatchSolarRoofCases.Base {
        final Direction front;SolarPanelElement panel;
        Flat(Direction front) { this.front=front; }
        public String id() { return "forkpatch_flat_solar_saved_collision_"+front.name().toLowerCase(Locale.ROOT); }
        public void build(TestContext c) {
            var fp=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(c.level());float yaw=fp.getYRot(),pitch=fp.getXRot();
            try {
                fp.setXRot(0);fp.setYRot(switch(front) { case XN->270;case XP->90;case ZN->0;default->180; });
                panel=(SolarPanelElement)Objects.requireNonNull(place(c,descriptor(2))).element;
            }finally { fp.setXRot(pitch);fp.setYRot(yaw); }
            c.check("flat solar: front follows actual item placement",panel.front==front,"");
            for(var cell:descriptor(2).getGhostGroupAt(panel.node.coordonate,front).elementList) {
                var p=origin(c).offset(cell.x,cell.y,cell.z);var state=c.level().getBlockState(p);
                c.check("flat solar: new reservations publish the half-height native state",state.getValue(BlockMeta.META)==GhostBlock.tFlatSolar,"");
                c.level().setBlock(p,state.setValue(BlockMeta.META,GhostBlock.tCube),2);
            }
            try { panel=(SolarPanelElement)((TransparentNode)Reload.node(c.level(),origin(c)).fresh).element; }
            catch(ReflectiveOperationException e) { throw new RuntimeException(e); }
            ((TransparentNodeEntity)c.level().getBlockEntity(origin(c))).onLoad();
        }
        public void measure(TestContext c) {
            int returns=0;
            for(var cell:descriptor(2).getGhostGroupAt(panel.node.coordonate,front).elementList) {
                var p=origin(c).offset(cell.x,cell.y,cell.z);var state=c.level().getBlockState(p);var shape=state.getCollisionShape(c.level(),p);
                c.check("flat solar: scheduled owner-load repair upgrades old cube states",state.getValue(BlockMeta.META)==GhostBlock.tFlatSolar,"");
                c.check("flat solar: every array cell has the source half-height collision and outline",shape.bounds().maxY==.5 && state.getShape(c.level(),p).bounds().maxY==.5,"");
                c.check("flat solar: ordinary world collision leaves space above the panel",c.level().noCollision(new AABB(p.getX()+.2,p.getY()+.55,p.getZ()+.2,p.getX()+.8,p.getY()+.95,p.getZ()+.8)),"");
                c.check("flat solar: ordinary world collision still stops objects at the panel",!c.level().noCollision(new AABB(p.getX()+.2,p.getY()+.2,p.getZ()+.2,p.getX()+.8,p.getY()+.4,p.getZ()+.8)),"");
                c.check("flat solar: client shape needs no observer data",state.getCollisionShape(net.minecraft.world.level.EmptyBlockGetter.INSTANCE,BlockPos.ZERO).bounds().maxY==.5,"");
                if(NodeManager.instance.getNodeFromCoordonate(new Coordonate(p,c.level())) instanceof mods.eln.misc.GhostPowerNode)returns++;
            }
            c.check("flat solar: metadata repair preserves exactly one real return terminal",returns==1,"");
            c.check("flat solar: completed repair is idempotent",SolarGhostShapes.repair(c.level(),origin(c)),"");
            c.level().destroyBlock(origin(c),false);
            c.check("flat solar: owner teardown still removes all reservations",descriptor(2).getGhostGroup(front).elementList.stream().allMatch(g->Eln.ghostManager.getGhost(new Coordonate(origin(c).offset(g.x,g.y,g.z),c.level()))==null),"");
        }
    }
    static final class Guards extends ForkPatchSolarRoofCases.Base {
        SolarPanelElement panel;
        public String id() { return "forkpatch_flat_solar_repair_preserves_foreign_reservations_and_missing_chunks"; }
        public void build(TestContext c) { panel=(SolarPanelElement)Objects.requireNonNull(place(c,descriptor(2))).element; }
        public void measure(TestContext c) {
            var owner=panel.node.coordonate;var cell=descriptor(2).getGhostGroupAt(owner,panel.front).elementList.stream().filter(g->NodeManager.instance.getNodeFromCoordonate(owner.newWithOffset(g.x,g.y,g.z))==null).findFirst().orElseThrow();
            var at=owner.newWithOffset(cell.x,cell.y,cell.z);var foreign=owner.newWithOffset(4,0,0);
            Eln.ghostManager.removeGhost(at);Eln.ghostManager.createGhost(at,foreign,-1,Eln.ghostBlock,GhostBlock.tCube);
            SolarGhostShapes.repair(c.level(),origin(c));
            c.check("flat solar: repair cannot claim another owner's cube reservation",c.level().getBlockState(at.pos()).getValue(BlockMeta.META)==GhostBlock.tCube && Eln.ghostManager.getGhost(at).getObservatorCoordonate().equals(foreign),"");
            Eln.ghostManager.removeGhost(at);c.level().setBlock(at.pos(),Blocks.STONE.defaultBlockState(),2);SolarGhostShapes.repair(c.level(),origin(c));
            c.check("flat solar: missing ownership never permits replacement of a real block",c.level().getBlockState(at.pos()).is(Blocks.STONE),"");
            var far=new BlockPos(30000000-4096,80,30000000-4096);int cx=far.getX()>>4,cz=far.getZ()>>4;
            c.check("flat solar: deferred repair does not load unknown terrain",c.level().getChunkSource().getChunkNow(cx,cz)==null && !SolarGhostShapes.repair(c.level(),far) && c.level().getChunkSource().getChunkNow(cx,cz)==null,"");
            c.level().destroyBlock(origin(c),false);
        }
    }
    static final class Rotating extends ForkPatchSolarRoofCases.Base {
        public String id() { return "forkpatch_flat_solar_rotating_reservations_retain_full_collision"; }
        public void measure(TestContext c) {
            var panel=(SolarPanelElement)Objects.requireNonNull(place(c,descriptor(3))).element;SolarGhostShapes.repair(c.level(),origin(c));
            for(var cell:descriptor(3).getGhostGroupAt(panel.node.coordonate,panel.front).elementList) {
                var at=origin(c).offset(cell.x,cell.y,cell.z);var state=c.level().getBlockState(at);
                c.check("flat solar: rotating array still reserves its source full-height envelope",state.getValue(BlockMeta.META)==GhostBlock.tCube && state.getCollisionShape(c.level(),at).bounds().maxY==1,"");
            }
            c.level().destroyBlock(origin(c),false);
        }
    }
}
