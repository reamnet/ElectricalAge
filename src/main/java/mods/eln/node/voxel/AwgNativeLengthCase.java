package mods.eln.node.voxel;

import mods.eln.Eln;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.sixnode.electricalcable.UtilityCableDescriptor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.List;

/** The shared router's waypoint length is not limited by the older centered AWG placer. */
public final class AwgNativeLengthCase implements TestCase {
    @Override public String id() { return "awg_native_long_route"; }
    @Override public int width() { return 24; }
    @Override public int height() { return 8; }
    @Override public int depth() { return 24; }
    @Override public int floorDepth() { return 24; }

    @Override public void build(TestContext ctx) {
        var wire = (UtilityCableDescriptor) Eln.sixNodeItem.getDescriptor(2200);
        var kind = AwgCablePart.routeKind(wire);
        int tier = CablePart.tierOf("Low Voltage Cable");
        for (int x = 0; x < width(); x++)
            for (int z = 0; z < depth(); z++)
                ctx.level().setBlockAndUpdate(ctx.at(x, 3, z), Blocks.STONE.defaultBlockState());
        for (int y : new int[]{1, 4}) {
            var route = CableRouter.route(CablePlacer.space(ctx.level()), kind, end(ctx.at(1, y, 1)),
                List.of(end(ctx.at(22, y, 1)), end(ctx.at(22, y, 22)), end(ctx.at(1, y, 22))),
                end(ctx.at(1, y, 20)), 1500);
            ctx.check("shared router finds the 65 meter waypoint route", route.error == null && route.length() == 65 * 16,
                String.valueOf(route.error) + ": " + route.length());
            if (route.error != null) return;
            if (y == 1) {
                var error = CablePlacer.lay(ctx.level(), tier, route.steps, null);
                ctx.check("native cable accepts the long route", error == null, String.valueOf(error));
                continue;
            }
            var spool = wire.newItemStack();
            wire.setRemainingLengthMeters(spool, 65.125);
            var layout = AwgCablePart.nativeLayout(ctx.level(), wire, route, null, null, null);
            ctx.check("AWG accepts the same native route beyond 64 meters", layout.error() == null, String.valueOf(layout.error()));
            if (layout.error() != null) return;
            var counts = Eln.simulator.counts();
            var failed = AwgCablePart.layNative(ctx.level(), KitSupply.awg(spool, wire), route, null, null, null, 35);
            ctx.check("long-route interruption returns the world, simulator and spool to baseline",
                "forced".equals(failed) && java.util.Arrays.equals(counts, Eln.simulator.counts())
                    && wire.getRemainingLengthMeters(spool) == 65.125
                    && layout.positions().stream().allMatch(ctx.level()::isEmptyBlock), String.valueOf(failed));
            var error = AwgCablePart.layNative(ctx.level(), KitSupply.awg(spool, wire), route, null, null, null, -1);
            ctx.check("AWG pays exactly the native centerline length", error == null
                && wire.getRemainingLengthMeters(spool) == .125, String.valueOf(error));
            if (error != null) return;
            for (BlockPos pos : layout.positions().stream().distinct().toList()) {
                var node = ctx.voxelAt(pos);
                for (int i = node.parts.size() - 1; i >= 0; i--)
                    if (node.parts.get(i) instanceof AwgCablePart) node.removePart(i, true);
                node.removeEmptyBlock();
            }
            double refunded = ctx.level().getEntitiesOfClass(ItemEntity.class,
                new AABB(net.minecraft.world.phys.Vec3.atLowerCornerOf(ctx.at(0, y, 0)),
                    net.minecraft.world.phys.Vec3.atLowerCornerOf(ctx.at(width(), y + 2, depth()))))
                .stream().filter(e -> e.getItem().is(wire.parentItem))
                .mapToDouble(e -> wire.getRemainingLengthMeters(e.getItem())).sum();
            ctx.check("long-route removal refunds exactly the paid 65 meters", refunded == 65, "refund=" + refunded);
        }
    }

    private static CableRouter.End end(BlockPos pos) {
        return new CableRouter.End(pos.getX() * 16L + 8, pos.getY() * 16L, pos.getZ() * 16L + 8, 0, -1);
    }

    @Override public void measure(TestContext ctx) {}
}
