package mods.eln.node.voxel;

import mods.eln.Eln;
import mods.eln.gametest.Reload;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.sixnode.electricalcable.UtilityCableDescriptor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.List;

public final class AwgNativeTapCase implements TestCase {
    @Override public String id() { return "awg_native_off_center_taps"; }
    @Override public int width() { return 10; }
    @Override public int height() { return 5; }
    @Override public int depth() { return 10; }
    @Override public int floorDepth() { return 10; }

    @Override public void build(TestContext ctx) {
        exercise(ctx, ctx.at(3, 2, 2), Direction.NORTH, Direction.UP);
        exercise(ctx, ctx.at(3, 2, 6), Direction.UP, Direction.SOUTH);
    }

    private static void exercise(TestContext ctx, BlockPos center, Direction mount, Direction toward) {
        var level = ctx.level();
        var shoulder = center.relative(toward);
        for (int x = -2; x <= 2; x++) ctx.floor(center.east(x).relative(mount));
        ctx.floor(shoulder.relative(mount));
        var wire = (UtilityCableDescriptor) Eln.sixNodeItem.getDescriptor(2200);
        var spool = wire.newItemStack();
        wire.setRemainingLengthMeters(spool, 8.125);
        var supply = KitSupply.awg(spool, wire);
        var trunk = CableRouter.route(CablePlacer.space(level), AwgCablePart.routeKind(wire),
            end(center.west(2), mount, 8), List.of(), end(center.east(2), mount, 8), 1500);
        if (trunk.error != null) {
            ctx.check(mount + " trunk route exists", false, trunk.error);
            return;
        }
        var placed = AwgCablePart.layNative(level, supply, trunk, null, null, null, -1);
        ctx.check(mount + " trunk installs for four meters", placed == null
            && wire.getRemainingLengthMeters(spool) == 4.125, String.valueOf(placed));
        if (placed != null) return;
        try {
            Reload.node(level, center, tag -> {
                var state = tag.getCompoundOrEmpty("p0").getCompoundOrEmpty("run");
                state.putDouble("joules", 400);
                state.putDouble("exposure", .125);
                return tag;
            });
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        var original = AwgCablePart.at(level, center).getFirst();
        double originalHeat = original.activeRun().getStoredJoules();
        double originalExposure = original.activeRun().getInsulationExposure();
        ctx.check(mount + " fixture loads warm, exposed conductor",
            Math.abs(originalHeat - 400) < 1e-8 && originalExposure == .125, originalHeat + " J / " + originalExposure);
        var target = end(center, mount, 4);
        var hit = new Vec3(target.x() / 16.0, target.y() / 16.0, target.z() / 16.0)
            .subtract(mount.getStepX() / 16.0, mount.getStepY() / 16.0, mount.getStepZ() / 16.0);
        long[][] join = new long[1][];
        var tap = CablePlacer.endForSupply(level, center, mount.getOpposite(), hit, false, join, supply);
        ctx.check(mount + " hit snaps to the off-center native grid point", target.equals(tap), String.valueOf(tap));
        if (!target.equals(tap)) return;
        var route = CableRouter.route(CablePlacer.around(CablePlacer.space(level), join[0]), AwgCablePart.routeKind(wire),
            end(shoulder, mount, 4), List.of(), tap, 1500);
        if (route.error != null) {
            ctx.check(mount + " branch route exists", false, route.error);
            return;
        }
        var failed = AwgCablePart.layNative(level, supply, route, null, join[0], null, 1);
        ctx.check(mount + " failed branch restores the original edge and spool", "forced".equals(failed)
            && AwgCablePart.at(level, center).equals(List.of(original))
            && Math.abs(original.activeRun().getStoredJoules() - originalHeat) < 1e-8
            && original.activeRun().getInsulationExposure() == originalExposure && wire.getRemainingLengthMeters(spool) == 4.125,
            failed + " / " + original.activeRun().getStoredJoules());
        placed = AwgCablePart.layNative(level, supply, route, null, join[0], null, -1);
        ctx.check(mount + " branch debits only its one new meter", placed == null
            && wire.getRemainingLengthMeters(spool) == 3.125, String.valueOf(placed));
        if (placed != null) return;

        var arms = AwgCablePart.at(level, center);
        var inherited = arms.stream().filter(p -> p.activeRun().getInsulationExposure() == .125).toList();
        var paid = inherited.stream().mapToInt(AwgNativeTapCase::length).sorted().toArray();
        double inheritedHeat = inherited.stream().mapToDouble(p -> p.activeRun().getStoredJoules()).sum();
        ctx.check(mount + " asymmetric split preserves heat, paid length and insulation exposure",
            arms.size() == 3 && Arrays.equals(paid, new int[]{4, 12}) && Math.abs(inheritedHeat - originalHeat) < 1e-8,
            Arrays.toString(paid) + " / " + inheritedHeat + " J");
        try {
            ctx.check(mount + " three arms retain their full state across node reload", Reload.node(level, center).tagsEqual(), "");
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        var node = (VoxelNode) ((VoxelNodeEntity) level.getBlockEntity(center)).getNode();
        var newArm = AwgCablePart.at(level, center).stream()
            .filter(p -> p.activeRun().getInsulationExposure() == 0).findFirst().orElseThrow();
        node.removePart(node.parts.indexOf(newArm), true);
        var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(center));
        ctx.check(mount + " removing the tap returns only the paid half meter and leaves the trunk",
            AwgCablePart.at(level, center).size() == 2 && drops.size() == 1
                && wire.getRemainingLengthMeters(drops.getFirst().getItem()) == .5, "");
    }

    private static int length(AwgCablePart part) {
        var tag = new CompoundTag();
        part.write(tag);
        return tag.getCompoundOrEmpty("run").getCompoundOrEmpty("length").getIntOr("paidVoxels", -1);
    }

    private static CableRouter.End end(BlockPos pos, Direction mount, int x) {
        return new CableRouter.End(pos.getX() * 16L + x, pos.getY() * 16L + 8 + 8 * mount.getStepY(),
            pos.getZ() * 16L + 8 + 8 * mount.getStepZ(), mount.get3DDataValue(), -1);
    }

    @Override public void measure(TestContext ctx) {}
}
