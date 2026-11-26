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
import net.minecraft.nbt.CompoundTag;

import java.util.*;

/** Paired native joins: two distinct taps in one host, or another arm on an existing tee. */
public final class AwgNativeRepeatTapCase implements TestCase {
    private final boolean loop;
    public AwgNativeRepeatTapCase(boolean loop) { this.loop = loop; }
    @Override public String id() { return loop ? "awg_native_two_taps_one_block" : "awg_native_four_way_tap"; }
    @Override public int width() { return 10; }
    @Override public int height() { return 5; }
    @Override public int depth() { return 10; }
    @Override public int floorDepth() { return 10; }
    @Override public int ticks() { return 65; }

    @Override public void build(TestContext ctx) {
        var wire = (UtilityCableDescriptor) Eln.sixNodeItem.getDescriptor(2200);
        var kind = AwgCablePart.routeKind(wire);
        var spool = wire.newItemStack(); wire.setRemainingLengthMeters(spool, 20.125);
        var center = ctx.at(4, 1, 6);
        var added = new HashSet<String>();
        var all = new HashSet<BlockPos>();
        for (int z : new int[]{2, 6}) {
            var middle = ctx.at(4, 1, z);
            var trunk = CableRouter.route(CablePlacer.space(ctx.level()), kind, end(ctx.at(1, 1, z), 8), List.of(), end(ctx.at(7, 1, z), 8), 1500);
            String error = z == 2 ? CablePlacer.lay(ctx.level(), 1, trunk.steps, null)
                : AwgCablePart.layNative(ctx.level(), KitSupply.awg(spool, wire), trunk, null, null, null, -1);
            ctx.check("paired trunk installs", error == null, String.valueOf(error)); if (error != null) return;
            if (z == 6) {
                for (var p : CableRouter.pieces(trunk.steps, 2, 1)) all.add(new BlockPos((int) p.bx(), (int) p.by(), (int) p.bz()));
                try { Reload.node(ctx.level(), middle, tag -> { var run = tag.getCompoundOrEmpty("p0").getCompoundOrEmpty("run"); run.putDouble("joules", 400); run.putDouble("exposure", .125); return tag; }); }
                catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
            }
            for (int branch = 0; branch < (loop ? 1 : 2); branch++) {
                var a = loop ? end(middle, 4) : end(branch == 0 ? middle.south() : middle.north(), 8);
                var b = end(middle, loop ? 12 : 8);
                long[] startJoin = loop ? point(a) : null, endJoin = point(b);
                var space = CablePlacer.around(CablePlacer.space(ctx.level()), endJoin);
                if (startJoin != null) space = CablePlacer.around(space, startJoin);
                var waypoints = loop ? List.of(end(middle.south(), 4), end(middle.south(), 12)) : List.<CableRouter.End>of();
                var route = CableRouter.route(space, kind, a, waypoints, b, 1500);
                ctx.check("native repeated join route exists", route.error == null, String.valueOf(route.error)); if (route.error != null) return;
                if (z == 2) {
                    error = CablePlacer.lay(ctx.level(), 1, route.steps, null);
                    ctx.check("native permits repeated join", error == null, String.valueOf(error));
                } else {
                    var before = AwgCablePart.at(ctx.level(), middle).stream().map(AwgNativeRepeatTapCase::save).toList();
                    var counts = Eln.simulator.counts(); double have = wire.getRemainingLengthMeters(spool);
                    error = AwgCablePart.layNative(ctx.level(), KitSupply.awg(spool, wire), route, startJoin, endJoin, null, 1);
                    ctx.check("multi-cut/fourth-arm rollback preserves original damage, loads and material", "forced".equals(error)
                        && before.equals(AwgCablePart.at(ctx.level(), middle).stream().map(AwgNativeRepeatTapCase::save).toList())
                        && Arrays.equals(counts, Eln.simulator.counts()) && have == wire.getRemainingLengthMeters(spool), String.valueOf(error));
                    var layout = AwgCablePart.nativeLayout(ctx.level(), wire, route, startJoin, endJoin, null);
                    error = AwgCablePart.layNative(ctx.level(), KitSupply.awg(spool, wire), route, startJoin, endJoin, null, -1);
                    ctx.check("AWG repeated join pays only new conductor", error == null
                        && wire.getRemainingLengthMeters(spool) == have - route.length() / 16.0, String.valueOf(error));
                    if (error != null) return;
                    for (int i = 0; i < layout.parts().size(); i++) { all.add(layout.positions().get(i)); added.add(key(layout.positions().get(i), layout.parts().get(i))); }
                }
                if (error != null) return;
            }
        }
        var hot = AwgCablePart.at(ctx.level(), center).stream().filter(p -> p.activeRun().getInsulationExposure() == .125).toList();
        ctx.check("repeated cuts preserve total heat, original length and damage", hot.size() == (loop ? 3 : 2)
            && hot.stream().mapToInt(p -> p.geometry.length()).sum() == 16
            && Math.abs(hot.stream().mapToDouble(p -> p.activeRun().getStoredJoules()).sum() - 400) < 1e-8, "");
        try { for (var p : all) ctx.check("repeated joins survive reload", Reload.node(ctx.level(), p).tagsEqual(), p.toString()); }
        catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
        var sourceEnd = new VoltageState[1]; var loadEnd = new VoltageState[1];
        AwgCablePart.at(ctx.level(), ctx.at(1, 1, 6)).getFirst().liveLink((a, b) -> sourceEnd[0] = a);
        AwgCablePart.at(ctx.level(), ctx.at(7, 1, 6)).getLast().liveLink((a, b) -> loadEnd[0] = b);
        var source = new VoltageSource("AWG repeated tap", sourceEnd[0], null).setU(12);
        var load = new Resistor(loadEnd[0], null).setR(1);
        Eln.simulator.addElectricalComponent(source); Eln.simulator.addElectricalComponent(load);
        double[] sample = new double[3];
        mods.eln.sim.IProcess sampleCurrents = dt -> {
            if (!loop) return;
            var middle = AwgCablePart.at(ctx.level(), center).stream().filter(p -> p.geometry.length() == 8 && p.activeRun().getInsulationExposure() > 0).findFirst().orElseThrow();
            var branch = AwgCablePart.at(ctx.level(), center).stream().filter(p -> added.contains(key(center, p))).findFirst().orElseThrow();
            sample[0] = Math.abs(load.getCurrent()); sample[1] = Math.abs(middle.activeRun().getCurrentAmps()); sample[2] = Math.abs(branch.activeRun().getCurrentAmps());
        };
        Eln.simulator.addElectricalProcess(sampleCurrents);
        ctx.after(20, () -> {
            var parts = all.stream().flatMap(p -> AwgCablePart.at(ctx.level(), p).stream()).toList();
            ctx.check("repeated junction circuit conducts quantitatively", Math.abs(load.getCurrent()) > 10
                && parts.stream().filter(p -> p.activeRun() != null).allMatch(p -> {
                    var run = p.activeRun(); return Math.abs(Math.abs(run.getVoltageDrop()) - Math.abs(run.getCurrentAmps())
                        * wire.resistanceOhms(p.geometry.length() / 16.0, run.getTemperatureCelsius())) < 1e-5;
                }), "I=" + load.getCurrent());
            Eln.simulator.removeElectricalProcess(sampleCurrents);
            if (loop) {
                ctx.check("same-host loop splits current without merging paid edges", sample[2] > .1
                    && Math.abs(sample[1] + sample[2] - sample[0]) < 1e-6, Arrays.toString(sample));
            } else ctx.check("four incident arms share one owned junction", AwgCablePart.at(ctx.level(), center).size() == 4, "");
            var remove = AwgCablePart.at(ctx.level(), center).stream().filter(p -> added.contains(key(center, p))).findFirst().orElseThrow();
            var node = ctx.voxelAt(center); node.removePart(node.parts.indexOf(remove), true);
        });
        ctx.after(40, () -> {
            try { ctx.check("removing one added arm leaves the original powered trunk", Math.abs(load.getCurrent()) > 10, "I=" + load.getCurrent()); }
            finally { Eln.simulator.removeElectricalComponent(source); Eln.simulator.removeElectricalComponent(load); source.breakConnection(); load.breakConnection(); }
        });
    }
    private static CompoundTag save(AwgCablePart p) { var tag = new CompoundTag(); p.write(tag); return tag; }
    private static String key(BlockPos p, AwgCablePart part) { return p + ":" + part.geometry.face + ":" + Arrays.toString(part.geometry.points); }
    private static long[] point(CableRouter.End e) { return new long[]{e.x(), e.y(), e.z()}; }
    private static CableRouter.End end(BlockPos p, int x) { return new CableRouter.End(p.getX() * 16L + x, p.getY() * 16L, p.getZ() * 16L + 8, 0, -1); }
    @Override public void measure(TestContext ctx) {}
}
