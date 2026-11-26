package mods.eln.node.voxel;

import mods.eln.Eln;
import mods.eln.gametest.Reload;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.mna.component.VoltageSource;
import mods.eln.sim.mna.state.VoltageState;
import mods.eln.sixnode.electricalcable.UtilityCableDescriptor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.phys.AABB;

import java.io.*;
import java.util.*;

/** Native staircase fixtures also exercise partial-plane inner/outer turns and decorative corner fills. */
public final class AwgNativeDecorCase implements TestCase {
    private final boolean slabs;
    public AwgNativeDecorCase() { this(false); }
    public AwgNativeDecorCase(boolean slabs) { this.slabs = slabs; }
    @Override public String id() { return slabs ? "awg_native_decorative_slabs" : "awg_native_decorative_stairs"; }
    @Override public int width() { return 10; }
    @Override public int height() { return 6; }
    @Override public int depth() { return 10; }
    @Override public int floorDepth() { return 10; }
    @Override public int ticks() { return 50; }

    @Override public void build(TestContext ctx) {
        var level = ctx.level();
        var wire = (UtilityCableDescriptor) Eln.sixNodeItem.getDescriptor(2200);
        var stair = slabs ? Blocks.STONE_SLAB.defaultBlockState()
            : Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, net.minecraft.core.Direction.EAST);
        for (int z : new int[]{1, 5}) {
            for (int x = 1; x <= 7; x++) for (int y = 1; y <= 3; y++) {
                ctx.floor(ctx.at(x, y, z - 1)); ctx.floor(ctx.at(x, y, z + 1));
            }
            level.setBlockAndUpdate(ctx.at(3, 1, z), stair);
            ctx.floor(ctx.at(4, 1, z));
            level.setBlockAndUpdate(ctx.at(4, 2, z), stair);
            for (int x = 5; x <= 7; x++) for (int y = 1; y <= 2; y++) ctx.floor(ctx.at(x, y, z));
        }
        var kind = AwgCablePart.routeKind(wire);
        var nativeRoute = CableRouter.route(CablePlacer.space(level), kind, end(ctx.at(1, 1, 1)), List.of(), end(ctx.at(6, 3, 1)), 1500);
        var route = CableRouter.route(CablePlacer.space(level), kind, end(ctx.at(1, 1, 5)), List.of(), end(ctx.at(6, 3, 5)), 1500);
        ctx.check("paired native and AWG stair routes agree", nativeRoute.error == null && route.error == null
            && route.length() == nativeRoute.length(), nativeRoute.error + " / " + route.error);
        if (route.error != null || nativeRoute.error != null) return;
        ctx.check("native converter installs stair fills", CablePlacer.lay(level, CablePart.tierOf("Low Voltage Cable"), nativeRoute.steps, null) == null, "");
        var layout = AwgCablePart.nativeLayout(level, wire, route, null, null, null);
        ctx.check("AWG converts native partial planes and decorative fills", layout.error() == null, String.valueOf(layout.error()));
        if (layout.error() != null) return;
        var pieces = CableRouter.pieces(route.steps, kind.width(), kind.height());
        ctx.check("fixture includes an inside-block outer wrap", route.steps.stream().filter(CableRouter.Step::outer).count()
            > pieces.stream().filter(p -> p.face() < 0).count(), "");
        var nativeBounds = new HashMap<BlockPos, Set<String>>();
        for (var piece : pieces) {
            var pos = new BlockPos((int) piece.bx(), (int) piece.by(), (int) piece.bz());
            var geometry = CablePart.utilityGeometry(wire.name, piece.face(), piece.points(), kind.width(), kind.height());
            nativeBounds.computeIfAbsent(pos, p -> new HashSet<>()).addAll(voxels(geometry.boxes()));
        }
        var awgBounds = new HashMap<BlockPos, Set<String>>();
        for (int i = 0; i < layout.parts().size(); i++)
            awgBounds.computeIfAbsent(layout.positions().get(i), p -> new HashSet<>()).addAll(voxels(layout.parts().get(i).boxes()));
        ctx.check("AWG uses exactly native installed collision geometry", nativeBounds.equals(awgBounds), "");
        var hosts = new HashMap<BlockPos, net.minecraft.world.level.block.state.BlockState>();
        for (var pos : layout.positions()) hosts.put(pos, level.getBlockState(pos));
        var spool = wire.newItemStack(); wire.setRemainingLengthMeters(spool, 20.125);
        var counts = Eln.simulator.counts();
        String failed = AwgCablePart.layNative(level, KitSupply.awg(spool, wire), route, null, null, null, layout.parts().size() - 1);
        ctx.check("decorative rollback restores all hosts, loads and paid wire", "forced".equals(failed)
            && Arrays.equals(counts, Eln.simulator.counts()) && wire.getRemainingLengthMeters(spool) == 20.125
            && hosts.entrySet().stream().allMatch(e -> level.getBlockState(e.getKey()) == e.getValue()), String.valueOf(failed));
        String placed = AwgCablePart.layNative(level, KitSupply.awg(spool, wire), route, null, null, null, -1);
        ctx.check("decorative geometry buys no extra conductor", placed == null
            && wire.getRemainingLengthMeters(spool) == 20.125 - route.length() / 16.0, String.valueOf(placed));
        if (placed != null) return;
        try {
            for (var p : layout.positions().stream().distinct().toList()) ctx.check("stair host and geometry survive reload", Reload.node(level, p).tagsEqual(), p.toString());
            for (var p : layout.positions().stream().distinct().toList()) for (var part : AwgCablePart.at(level, p)) {
                var bytes = new ByteArrayOutputStream(); part.serialize(new DataOutputStream(bytes));
                var client = AwgCablePart.deserialize(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
                ctx.check("client retains native visual bounds", boxes(part.boxes()).equals(boxes(client.boxes())), "");
            }
        } catch (ReflectiveOperationException | IOException e) { throw new IllegalStateException(e); }
        var from = new VoltageState[1]; var to = new VoltageState[1];
        AwgCablePart.at(level, layout.positions().getFirst()).getFirst().liveLink((a, b) -> from[0] = a);
        AwgCablePart.at(level, layout.positions().getLast()).getLast().liveLink((a, b) -> to[0] = b);
        var source = new VoltageSource("AWG stair source", from[0], null).setU(12);
        var load = new Resistor(to[0], null).setR(1);
        Eln.simulator.addElectricalComponent(source); Eln.simulator.addElectricalComponent(load);
        ctx.after(20, () -> {
            try {
                var parts = hosts.keySet().stream().flatMap(p -> AwgCablePart.at(level, p).stream()).toList();
                double resistance = parts.stream().filter(p -> p.activeRun() != null).mapToDouble(p ->
                    wire.resistanceOhms(p.geometry.length() / 16.0, p.activeRun().getTemperatureCelsius())).sum();
                double current = Math.abs(load.getCurrent());
                ctx.check("partial stair circuit matches paid conductor resistance", current > 10
                    && Math.abs(current - 12 / (1 + resistance)) < .001, "I=" + current + ", R=" + resistance);
                ctx.check("every stair arm carries the same current", parts.stream().filter(p -> p.activeRun() != null)
                    .allMatch(p -> Math.abs(Math.abs(p.activeRun().getCurrentAmps()) - current) < 1e-6), "I=" + current);
            } finally {
                Eln.simulator.removeElectricalComponent(load); Eln.simulator.removeElectricalComponent(source);
                load.breakConnection(); source.breakConnection();
                for (var pos : hosts.keySet()) {
                    var node = ctx.voxelAt(pos);
                    for (int i = node.parts.size() - 1; i >= 0; i--) node.removePart(i, true);
                    node.removeEmptyBlock();
                }
            }
            double refund = level.getEntitiesOfClass(ItemEntity.class, new AABB(ctx.at(0, 1, 4)).expandTowards(10, 5, 3))
                .stream().filter(e -> e.getItem().is(wire.parentItem)).mapToDouble(e -> wire.getRemainingLengthMeters(e.getItem())).sum();
            ctx.check("removal restores hosts and refunds only centerline", Math.abs(refund - route.length() / 16.0) < 1e-9
                && hosts.entrySet().stream().allMatch(e -> level.getBlockState(e.getKey()) == e.getValue()), "refund=" + refund);
        });
    }

    private static Set<String> voxels(List<int[]> boxes) {
        var out = new HashSet<String>();
        for (var b : boxes) for (int x = b[0]; x < b[3]; x++) for (int y = b[1]; y < b[4]; y++) for (int z = b[2]; z < b[5]; z++)
            out.add(x + "," + y + "," + z);
        return out;
    }

    private static Set<String> boxes(List<int[]> boxes) {
        var out = new HashSet<String>();
        for (var b : boxes) out.add(Arrays.toString(b));
        return out;
    }
    private static CableRouter.End end(BlockPos p) { return new CableRouter.End(p.getX() * 16L + 8, p.getY() * 16L, p.getZ() * 16L + 8, 0, -1); }
    @Override public void measure(TestContext ctx) {}
}
