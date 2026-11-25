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
    }

    private static CableRouter.Result route(TestContext ctx, BlockPos corner, RouteKind kind) {
        long x = corner.getX() * 16L, y = corner.getY() * 16L + 8, z = corner.getZ() * 16L;
        return CableRouter.route(CablePlacer.space(ctx.level()), kind,
            new CableRouter.End(x, y, z + 40, 4, -1), List.of(),
            new CableRouter.End(x + 40, y, z, 2, -1), 1500);
    }

    @Override public void measure(TestContext ctx) {}
}
