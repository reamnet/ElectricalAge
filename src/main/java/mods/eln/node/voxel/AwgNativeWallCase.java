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

import java.util.List;

public final class AwgNativeWallCase implements TestCase {
    @Override public String id() { return "awg_native_wall_to_wall"; }
    @Override public int width() { return 10; }
    @Override public int height() { return 5; }
    @Override public int depth() { return 10; }
    @Override public int floorDepth() { return 10; }
    @Override public int ticks() { return 85; }

    @Override
    public void build(TestContext ctx) {
        var nativeCorner = ctx.at(2, 1, 2);
        var awgCorner = ctx.at(6, 1, 6);
        for (var corner : List.of(nativeCorner, awgCorner)) {
            for (int offset = 0; offset <= 2; offset++) {
                ctx.floor(corner.west().south(offset));
                ctx.floor(corner.north().east(offset));
            }
        }

        var level = ctx.level();
        var nativeRoute = route(ctx, nativeCorner, RouteKind.cable("native", 2, 1));
        int tier = CablePart.tierOf("Low Voltage Cable");
        ctx.check("native kit lays the perpendicular inner-wall route", nativeRoute.error == null
            && CablePlacer.lay(level, tier, nativeRoute.steps, null) == null, String.valueOf(nativeRoute.error));
        var wire = (UtilityCableDescriptor) Eln.sixNodeItem.getDescriptor(2200);
        var spool = wire.newItemStack();
        wire.setRemainingLengthMeters(spool, 6.125);
        var awgRoute = route(ctx, awgCorner, AwgCablePart.routeKind(wire));
        ctx.check("native and AWG routers calculate equal five-meter wall centerlines", awgRoute.error == null
            && nativeRoute.error == null && awgRoute.length() == 80 && nativeRoute.length() == awgRoute.length(),
            String.valueOf(awgRoute.error));
        if (awgRoute.error != null) return;

        var result = AwgCablePart.layNative(level, KitSupply.awg(spool, wire), awgRoute, null, null, null, -1);
        ctx.check("wall-to-wall AWG turn places two independent corner edges and charges five meters",
            result == null && wire.getRemainingLengthMeters(spool) == 1.125
                && AwgCablePart.at(level, awgCorner).size() == 2, String.valueOf(result));
        if (result != null) return;

        try {
            ctx.check("the paired corner endpoints survive a world-node save round trip",
                Reload.node(level, awgCorner).tagsEqual(), "");
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        var sourceEnd = new VoltageState[1];
        var loadEnd = new VoltageState[1];
        AwgCablePart.at(level, awgCorner.south(2)).getFirst().liveLink((a, b) -> sourceEnd[0] = a);
        AwgCablePart.at(level, awgCorner.east(2)).getFirst().liveLink((a, b) -> loadEnd[0] = b);
        var source = new VoltageSource("AWG wall test", sourceEnd[0], null).setU(12);
        var load = new Resistor(loadEnd[0], null).setR(1);
        Eln.simulator.addElectricalComponent(source);
        Eln.simulator.addElectricalComponent(load);
        ctx.after(30, () -> {
            try {
                var cornerParts = AwgCablePart.at(level, awgCorner);
                double current = Math.abs(load.getCurrent());
                ctx.check("both perpendicular edges carry the load and preserve per-edge voltage drop",
                    current > 11 && cornerParts.stream().allMatch(p -> {
                        var run = p.activeRun();
                        return Math.abs(Math.abs(run.getCurrentAmps()) - current) < 1e-6
                            && Math.abs(Math.abs(run.getVoltageDrop())
                                - current * wire.resistanceOhms(1.0, run.getTemperatureCelsius())) < 1e-5;
                    }), current + " A");
            } finally {
                Eln.simulator.removeElectricalComponent(load);
                load.breakConnection();
                Eln.simulator.removeElectricalComponent(source);
                source.breakConnection();
            }
        });
        ctx.after(35, () -> tapInnerArm(ctx, awgCorner, wire, spool));
    }

    private static void tapInnerArm(TestContext ctx, BlockPos corner, UtilityCableDescriptor wire,
                                    net.minecraft.world.item.ItemStack spool) {
        var level = ctx.level();
        var original = AwgCablePart.at(level, corner).stream().filter(p -> p.visual().face == 4).findFirst().orElseThrow();
        var peer = AwgCablePart.at(level, corner).stream().filter(p -> p.visual().face == 2).findFirst().orElseThrow();
        ctx.floor(corner.above().west());
        long x = corner.getX() * 16L, y = corner.getY() * 16L + 8, z = corner.getZ() * 16L + 4;
        long[][] join = new long[1][];
        var tap = original.tapEnd(level, corner,
            new net.minecraft.world.phys.Vec3(x / 16.0 + 1.0 / 16, y / 16.0, z / 16.0), join);
        ctx.check("native snap selects the inner-corner arm away from its corner port",
            tap != null && tap.z() == z, String.valueOf(tap));
        if (tap == null) return;
        var end = new CableRouter.End(x, y + 16, z, 4, -1);
        var route = CableRouter.route(CablePlacer.around(CablePlacer.space(level), join[0]),
            AwgCablePart.routeKind(wire), end, List.of(), tap, 1500);
        if (route.error != null) {
            ctx.check("inner arm tap route exists", false, route.error);
            return;
        }
        String placed = AwgCablePart.layNative(level, KitSupply.awg(spool, wire), route, null, join[0], null, -1);
        ctx.check("inner corner tap charges only one new meter", placed == null
            && wire.getRemainingLengthMeters(spool) == .125, String.valueOf(placed));
        if (placed != null) return;
        var peerStates = new VoltageState[2];
        peer.liveLink((a, b) -> { peerStates[0] = a; peerStates[1] = b; });
        var arms = AwgCablePart.at(level, corner);
        int[] connected = {0};
        for (var arm : arms) if (arm != peer) arm.liveLink((a, b) -> {
            if (a == peerStates[0] || a == peerStates[1] || b == peerStates[0] || b == peerStates[1]) connected[0]++;
        });
        ctx.check("only the corner-side child retains the inner-corner terminal",
            arms.size() == 4 && connected[0] == 1, "");
        try {
            ctx.check("inner joint ownership survives reload with the new branch", Reload.node(level, corner).tagsEqual(), "");
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static CableRouter.Result route(TestContext ctx, BlockPos corner, RouteKind kind) {
        long x = corner.getX() * 16L, y = corner.getY() * 16L + 8, z = corner.getZ() * 16L;
        return CableRouter.route(CablePlacer.space(ctx.level()), kind,
            new CableRouter.End(x, y, z + 40, 4, -1), List.of(),
            new CableRouter.End(x + 40, y, z, 2, -1), 1500);
    }

    @Override public void measure(TestContext ctx) {}
}
