package mods.eln.node.voxel;

import mods.eln.Eln;
import mods.eln.gametest.Reload;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.sixnode.electricalcable.UtilityCableDescriptor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.Arrays;
import java.util.List;

public final class AwgNativeSlabCase implements TestCase {
    @Override public String id() { return "awg_native_slab_surface"; }
    @Override public int width() { return 10; }
    @Override public int height() { return 5; }
    @Override public int depth() { return 10; }
    @Override public int floorDepth() { return 10; }

    @Override public void build(TestContext ctx) {
        var level = ctx.level();
        var slab = Blocks.STONE_SLAB.defaultBlockState();
        for (int z : new int[]{2, 5})
            for (int x = 2; x <= 5; x++) level.setBlockAndUpdate(ctx.at(x, 2, z), slab);
        var wire = (UtilityCableDescriptor) Eln.sixNodeItem.getDescriptor(2200);
        var kind = AwgCablePart.routeKind(wire);
        var nativeRoute = CableRouter.route(CablePlacer.space(level), kind,
            end(ctx.at(2, 2, 2)), List.of(), end(ctx.at(5, 2, 2)), 1500);
        int tier = CablePart.tierOf("Low Voltage Cable");
        ctx.check("native kit lays on the 8-voxel slab plane", nativeRoute.error == null && nativeRoute.length() == 48
            && CablePlacer.lay(level, tier, nativeRoute.steps, null) == null, String.valueOf(nativeRoute.error));
        var route = CableRouter.route(CablePlacer.space(level), kind,
            end(ctx.at(2, 2, 5)), List.of(), end(ctx.at(5, 2, 5)), 1500);
        if (route.error != null) {
            ctx.check("AWG slab route found", false, route.error);
            return;
        }
        var spool = wire.newItemStack();
        wire.setRemainingLengthMeters(spool, 5.125);
        var supply = KitSupply.awg(spool, wire);
        var counts = Eln.simulator.counts();
        var failed = AwgCablePart.layNative(level, supply, route, null, null, null, 2);
        ctx.check("forced rollback restores the exact original slab states and simulator counts",
            "forced".equals(failed) && Arrays.equals(counts, Eln.simulator.counts())
                && wire.getRemainingLengthMeters(spool) == 5.125
                && java.util.stream.IntStream.rangeClosed(2, 5).allMatch(x -> level.getBlockState(ctx.at(x, 2, 5)) == slab),
            String.valueOf(failed));
        var layout = AwgCablePart.nativeLayout(level, wire, route, null, null, null);
        var placed = AwgCablePart.layNative(level, supply, route, null, null, null, -1);
        ctx.check("AWG pays the same three-meter centerline on slabs", placed == null && route.length() == 48
            && wire.getRemainingLengthMeters(spool) == 2.125, String.valueOf(placed));
        if (placed != null) return;
        var middle = ctx.at(3, 2, 5);
        var part = AwgCablePart.at(level, middle).getFirst();
        var generic = (VoxelNode) ((VoxelNodeEntity) level.getBlockEntity(ctx.at(3, 2, 2))).getNode();
        ctx.check("native, preview and installed AWG bounds agree", part.boxes().getFirst()[1] == 8
            && Arrays.equals(part.boxes().getFirst(), generic.parts.getFirst().boxes().getFirst())
            && Arrays.equals(part.boxes().getFirst(), layout.parts().get(1).boxes().getFirst()), "");
        try {
            ctx.check("partial plane and original host survive reload", Reload.node(level, middle).tagsEqual(), "");
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        var node = (VoxelNode) ((VoxelNodeEntity) level.getBlockEntity(middle)).getNode();
        if (node.removePart(0, true)) node.removeEmptyBlock();
        var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(middle));
        ctx.check("removal restores the slab and refunds exactly one meter",
            level.getBlockState(middle) == slab && drops.size() == 1
                && wire.getRemainingLengthMeters(drops.getFirst().getItem()) == 1, "");
    }

    private static CableRouter.End end(BlockPos pos) {
        return new CableRouter.End(pos.getX() * 16L + 8, pos.getY() * 16L + 8, pos.getZ() * 16L + 8, 0, -1);
    }

    @Override public void measure(TestContext ctx) {}
}
