package mods.eln.node.voxel;

import mods.eln.Eln;
import mods.eln.gametest.Reload;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.gametest.cases.VoxelDeviceCases;
import mods.eln.misc.Direction;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sixnode.electricalcable.UtilityCableDescriptor;
import mods.eln.sixnode.electricalsource.ElectricalSourceElement;
import net.minecraft.core.BlockPos;

import java.util.*;

/** Native machine pin snapping at several offsets feeds independently paid AWG conductors. */
public final class AwgNativeTerminalCase implements TestCase {
    private final int offset;
    private final boolean across;
    public AwgNativeTerminalCase(int offset) { this(offset, false); }
    public AwgNativeTerminalCase(int offset, boolean across) { this.offset = offset; this.across = across; }
    @Override public String id() { return "awg_native_terminal_" + (offset < 0 ? "minus" + -offset : offset) + (across ? "_across" : ""); }
    @Override public int width() { return 10; }
    @Override public int height() { return 5; }
    @Override public int depth() { return 10; }
    @Override public int floorDepth() { return 10; }
    @Override public int ticks() { return 55; }

    @Override public void build(TestContext ctx) {
        var wire = (UtilityCableDescriptor) Eln.sixNodeItem.getDescriptor(2200);
        var positions = new ArrayList<BlockPos>();
        var endpoints = new ArrayList<BlockPos>();
        var loads = new ArrayList<Resistor>();
        for (int z : new int[]{2, 6}) {
            var pos = ctx.at(2, 1, z);
            var node = VoxelDeviceCases.block(ctx, pos);
            boolean made = node.createSubBlockAt(Eln.sixNodeItem.getDescriptor(192).newItemStack(), 6, Direction.YN, across ? 4 : 2, 0, offset, ctx.player());
            ctx.check("native off-center source placed", made, "offset=" + offset);
            if (!made) return;
            var source = (ElectricalSourceElement) node.getElement(6);
            ctx.setVolts(source, 12);
            var pin = VoxelNode.pins(source).stream().filter(p -> p.out() == 5).findFirst().orElseThrow();
            var start = CablePlacer.deviceEnd(node, pos, 6,
                new double[]{pin.tip()[0] / 16.0, pin.tip()[1] / 16.0, pin.tip()[2] / 16.0}, true, 1);
            ctx.check("shared snapping picks exact offset source terminal", start != null
                && start.z() == pos.getZ() * 16L + pin.tip()[2], "");
            if (start == null) return;
            var endPos = pos.east(2);
            var end = new CableRouter.End(endPos.getX() * 16L + 8, pos.getY() * 16L,
                pos.getZ() * 16L + pin.tip()[2], 0, -1);
            var route = CableRouter.route(CablePlacer.space(ctx.level()), AwgCablePart.routeKind(wire), start, List.of(), end, 1500);
            ctx.check("native terminal route found", route.error == null, String.valueOf(route.error));
            if (route.error != null) return;
            String error;
            if (z == 2) {
                error = CablePlacer.lay(ctx.level(), CablePart.tierOf("Low Voltage Cable"), route.steps, null);
                for (var p : CableRouter.pieces(route.steps, 2, 1)) positions.add(new BlockPos((int) p.bx(), (int) p.by(), (int) p.bz()));
            } else {
                var spool = wire.newItemStack(); wire.setRemainingLengthMeters(spool, 5.125);
                var layout = AwgCablePart.nativeLayout(ctx.level(), wire, route, null, null, null);
                error = AwgCablePart.layNative(ctx.level(), KitSupply.awg(spool, wire), route, null, null, null, -1);
                positions.addAll(layout.positions());
                ctx.check("offset machine route keeps exact AWG payment", error == null
                    && wire.getRemainingLengthMeters(spool) == 5.125 - route.length() / 16.0, String.valueOf(error));
            }
            ctx.check("paired native and AWG machine routes install", error == null, String.valueOf(error));
            if (error != null) return;
            endpoints.add(endPos);
        }
        Runnable attach = () -> {
            var nativeLoad = ctx.voxelAt(endpoints.get(0)).cables.live().getFirst().load;
            final mods.eln.sim.ElectricalLoad[] awgLoad = new mods.eln.sim.ElectricalLoad[1];
            AwgCablePart.at(ctx.level(), endpoints.get(1)).getLast().liveLink((a, b) -> awgLoad[0] = b);
            loads.add(new Resistor(nativeLoad, null).setR(1));
            loads.add(new Resistor(awgLoad[0], null).setR(1));
            for (var load : loads) Eln.simulator.addElectricalComponent(load);
        };
        Runnable verify = () -> {
            double nativeCurrent = Math.abs(loads.get(0).getCurrent()), current = Math.abs(loads.get(1).getCurrent());
            var parts = positions.stream().distinct().flatMap(p -> AwgCablePart.at(ctx.level(), p).stream()).toList();
            double resistance = parts.stream().mapToDouble(p -> wire.resistanceOhms(p.geometry.length() / 16.0, p.activeRun().getTemperatureCelsius())).sum();
            ctx.check("native and AWG offset terminals power real loads", nativeCurrent > 10 && current > 10, nativeCurrent + " / " + current);
            var source = (ElectricalSourceElement) ctx.voxelAt(ctx.at(2, 1, 6)).getElement(6);
            double sourceResistance = VoxelNode.pins(source).stream().filter(p -> p.out() == 5).findFirst().orElseThrow().load().getRs();
            ctx.check("offset terminal current follows AWG and native source resistance", Math.abs(current - 12 / (1 + resistance + sourceResistance + 1e-6)) < .001,
                "I=" + current + " wire R=" + resistance + " source Rs=" + sourceResistance);
            ctx.check("offset pin feeds each conductor exactly once", parts.stream().allMatch(p -> Math.abs(Math.abs(p.activeRun().getCurrentAmps()) - current) < 1e-6), "");
        };
        attach.run();
        ctx.after(15, () -> {
            try { verify.run(); } finally { for (var load : loads) { Eln.simulator.removeElectricalComponent(load); load.breakConnection(); } loads.clear(); }
            try { for (var pos : positions.stream().distinct().toList()) ctx.check("offset pins and AWG survive reload", Reload.node(ctx.level(), pos).tagsEqual(), pos.toString()); }
            catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
            attach.run();
        });
        ctx.after(35, () -> {
            try { verify.run(); } finally { for (var load : loads) { Eln.simulator.removeElectricalComponent(load); load.breakConnection(); } loads.clear(); }
        });
    }
    @Override public void measure(TestContext ctx) {}
}
