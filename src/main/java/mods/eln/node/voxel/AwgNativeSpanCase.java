package mods.eln.node.voxel;

import mods.eln.Eln;
import mods.eln.gametest.Reload;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.sixnode.electricalcable.UtilityCableDescriptor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;

public final class AwgNativeSpanCase implements TestCase {
    @Override public String id() { return "awg_native_short_span"; }
    @Override public int width() { return 10; }
    @Override public int height() { return 5; }
    @Override public int depth() { return 10; }
    @Override public int floorDepth() { return 10; }

    @Override
    public void build(TestContext ctx) {
        var level = ctx.level();
        var wire = (UtilityCableDescriptor) Eln.sixNodeItem.getDescriptor(2200);
        var spool = wire.newItemStack();
        wire.setRemainingLengthMeters(spool, 4.125);
        var supply = KitSupply.awg(spool, wire);
        var nativeGap = ctx.at(2, 2, 1);
        var awgGap = ctx.at(2, 2, 5);
        for (int z : new int[]{1, 5})
            for (int x : new int[]{1, 3, 4}) ctx.floor(ctx.at(x, 2, z));
        var nativeStart = end(ctx.at(1, 3, 1));
        var nativeEnd = end(ctx.at(4, 3, 1));
        int tier = CablePart.tierOf("Low Voltage Cable");
        var nativeResult = CableRouter.route(CablePlacer.space(level), 2, 1, nativeStart, gapEdges(nativeGap), nativeEnd);
        ctx.check("native kit can lay three meters across a one-block gap",
            nativeResult.error == null && nativeResult.length() == 48
                && CablePlacer.lay(level, tier, nativeResult.steps, null) == null, nativeResult.error + " / " + (nativeResult.error == null ? nativeResult.length() : -1));

        var awgStart = end(ctx.at(1, 3, 5));
        var awgEnd = end(ctx.at(4, 3, 5));
        var result = CableRouter.route(CablePlacer.space(level), AwgCablePart.routeKind(wire),
            awgStart, gapEdges(awgGap), awgEnd, 1500);
        ctx.check("the same AWG dimensions select the same centerline length",
            result.error == null && result.length() == nativeResult.length(), result.error + " / " + (result.error == null ? result.length() : -1));
        if (result.error != null) return;

        var planned = AwgCablePart.nativeLayout(level, wire, result, null, null, null);
        ctx.check("native pieces produce a valid anchored AWG span", planned.error() == null, String.valueOf(planned.error()));
        var secondGap = awgGap.east();
        level.removeBlock(secondGap, false);
        var stale = AwgCablePart.layNative(level, supply, result, null, null, null, -1);
        ctx.check("a stale route across two missing supports cannot charge or place",
            stale != null && wire.getRemainingLengthMeters(spool) == 4.125 && level.isEmptyBlock(awgGap.above()), String.valueOf(stale));
        ctx.floor(secondGap);
        var failed = AwgCablePart.layNative(level, supply, result, null, null, null, 2);
        ctx.check("failed placement rolls back the unsupported piece without payment",
            "forced".equals(failed) && level.isEmptyBlock(awgGap.above()) && wire.getRemainingLengthMeters(spool) == 4.125, String.valueOf(failed));
        var placed = AwgCablePart.layNative(level, supply, result, null, null, null, -1);
        ctx.check("the previewed centerline installs for exactly three meters", placed == null
            && wire.getRemainingLengthMeters(spool) == 1.125, String.valueOf(placed));
        if (placed != null) return;

        var middle = awgGap.above();
        var part = AwgCablePart.at(level, middle).getFirst();
        ctx.check("installed span bounds match the planned bounds",
            java.util.Arrays.equals(part.boxes().getFirst(), planned.parts().get(1).boxes().getFirst()), "");
        try {
            var bytes = new java.io.ByteArrayOutputStream();
            part.serialize(new java.io.DataOutputStream(bytes));
            var client = AwgCablePart.deserialize(new java.io.DataInputStream(new java.io.ByteArrayInputStream(bytes.toByteArray())));
            ctx.check("native sag samples survive the AWG client packet",
                part.visual().hang2 != null && java.util.Arrays.equals(part.visual().hang2, client.visual().hang2), "");
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
        try {
            ctx.check("gap piece reloads as a paid one-meter run", Reload.node(level, middle).tagsEqual(), "");
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        var node = (VoxelNode) ((VoxelNodeEntity) level.getBlockEntity(middle)).getNode();
        if (node.removePart(0, true)) node.removeEmptyBlock();
        var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(middle));
        ctx.check("one removed gap piece refunds one paid meter", drops.size() == 1
            && wire.getRemainingLengthMeters(drops.getFirst().getItem()) == 1, "");
    }

    private static java.util.List<CableRouter.End> gapEdges(BlockPos gap) {
        long x = gap.getX() * 16L, y = (gap.getY() + 1) * 16L, z = gap.getZ() * 16L + 8;
        return java.util.List.of(new CableRouter.End(x, y, z, 0, -1), new CableRouter.End(x + 8, y, z, 0, -1),
            new CableRouter.End(x + 16, y, z, 0, -1));
    }

    private static CableRouter.End end(BlockPos pos) {
        return new CableRouter.End(pos.getX() * 16L + 8, pos.getY() * 16L, pos.getZ() * 16L + 8, 0, -1);
    }

    @Override public void measure(TestContext ctx) {}
}
