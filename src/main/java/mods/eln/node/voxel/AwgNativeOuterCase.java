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
import net.minecraft.core.Direction;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;

import java.util.Arrays;
import java.util.List;

public final class AwgNativeOuterCase implements TestCase {
    @Override public String id() { return "awg_native_outer_knuckles"; }
    @Override public int width() { return 10; }
    @Override public int height() { return 5; }
    @Override public int depth() { return 10; }
    @Override public int floorDepth() { return 10; }
    @Override public int ticks() { return 85; }

    @Override public void build(TestContext ctx) {
        exercise(ctx, ctx.at(2, 2, 2), Direction.UP, Direction.EAST, 2200, true);
        exercise(ctx, ctx.at(6, 2, 2), Direction.EAST, Direction.DOWN, 2266, false);
        exercise(ctx, ctx.at(2, 2, 6), Direction.EAST, Direction.SOUTH, 2434, false);
    }

    private void exercise(TestContext ctx, BlockPos support, Direction first, Direction second, int id, boolean power) {
        ctx.floor(support);
        var level = ctx.level();
        var wire = (UtilityCableDescriptor) Eln.sixNodeItem.getDescriptor(id);
        var kind = AwgCablePart.routeKind(wire);
        var start = face(support, first);
        var end = face(support, second);
        var route = CableRouter.route(CablePlacer.space(level), kind, start, List.of(), end, 1500);
        var label = first + " to " + second;
        ctx.check(label + " native route uses one full-block knuckle and one paid meter",
            route.error == null && route.length() == 16 && route.steps.stream().filter(CableRouter.Step::outer).count() == 1,
            String.valueOf(route.error));
        if (route.error != null || route.length() != 16) return;

        var layout = AwgCablePart.nativeLayout(level, wire, route, null, null, null);
        ctx.check(label + " AWG conversion accepts the native centerline", layout.error() == null, String.valueOf(layout.error()));
        if (layout.error() != null) return;
        var nativePieces = CableRouter.pieces(route.steps, kind.width(), kind.height());
        var cornerPos = support.relative(first).relative(second);
        var knob = layout.parts().stream().filter(part -> part.visual().isKnuckle()).findFirst().orElseThrow();
        var nativeKnob = nativePieces.stream().filter(piece -> piece.face() < 0).findFirst().orElseThrow();
        var nativeShape = CablePart.utilityGeometry(wire.name, -1, nativeKnob.points(), kind.width(), kind.height());
        ctx.check(label + " outline uses native knuckle bounds", Arrays.equals(knob.boxes().getFirst(), nativeShape.boxes().getFirst()), "");

        var spool = wire.newItemStack();
        wire.setRemainingLengthMeters(spool, 2.125);
        var supply = KitSupply.awg(spool, wire);
        level.setBlockAndUpdate(cornerPos, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        var stale = AwgCablePart.layNative(level, supply, route, null, null, null, -1);
        ctx.check(label + " stale preview refuses a newly obstructed knuckle without payment",
            stale != null && wire.getRemainingLengthMeters(spool) == 2.125
                && level.isEmptyBlock(support.relative(first)), String.valueOf(stale));
        level.removeBlock(cornerPos, false);
        var before = Eln.simulator.counts();
        var failed = AwgCablePart.layNative(level, supply, route, null, null, null, 2);
        ctx.check(label + " rollback after the knuckle removes new nodes and loads without payment",
            "forced".equals(failed) && level.isEmptyBlock(cornerPos) && level.isEmptyBlock(support.relative(first))
                && Arrays.equals(before, Eln.simulator.counts()) && wire.getRemainingLengthMeters(spool) == 2.125,
            String.valueOf(failed));
        var placed = AwgCablePart.layNative(level, supply, route, null, null, null, -1);
        ctx.check(label + " pays exactly one meter with no extra charge for the knuckle",
            placed == null && wire.getRemainingLengthMeters(spool) == 1.125, String.valueOf(placed));
        if (placed != null) return;

        var installed = AwgCablePart.at(level, cornerPos).getFirst();
        ctx.check(label + " installed knuckle matches preview and builds a native mesh",
            Arrays.equals(installed.boxes().getFirst(), knob.boxes().getFirst())
                && !VoxelGeometry.of(List.of(installed)).isEmpty(), "");
        try {
            var bytes = new java.io.ByteArrayOutputStream();
            installed.serialize(new java.io.DataOutputStream(bytes));
            var client = AwgCablePart.deserialize(new java.io.DataInputStream(new java.io.ByteArrayInputStream(bytes.toByteArray())));
            ctx.check(label + " packet reproduces knuckle bounds", Arrays.equals(client.boxes().getFirst(), knob.boxes().getFirst()), "");
            ctx.check(label + " save reload restores connector and both arms",
                Reload.node(level, cornerPos).tagsEqual()
                    && Reload.node(level, support.relative(first)).tagsEqual()
                    && Reload.node(level, support.relative(second)).tagsEqual(), "");
        } catch (java.io.IOException | ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        if (power) checkPower(ctx, support, first, second, cornerPos, wire);
    }

    private void checkPower(TestContext ctx, BlockPos support, Direction first, Direction second, BlockPos corner,
                            UtilityCableDescriptor wire) {
        var level = ctx.level();
        var a = AwgCablePart.at(level, support.relative(first)).getFirst();
        var b = AwgCablePart.at(level, support.relative(second)).getFirst();
        var from = new VoltageState[1];
        var to = new VoltageState[1];
        a.liveLink((x, y) -> from[0] = x);
        b.liveLink((x, y) -> to[0] = y);
        var returnRail = new VoltageState();
        Eln.simulator.addElectricalLoad(returnRail);
        var ground = new VoltageSource("AWG outer test ground", returnRail, null).setU(0);
        var source = new VoltageSource("AWG outer test", from[0], returnRail).setU(12);
        var load = new Resistor(to[0], returnRail).setR(1);
        var parallel = new Resistor(from[0], returnRail).setR(4);
        Eln.simulator.addElectricalComponent(ground);
        Eln.simulator.addElectricalComponent(source);
        Eln.simulator.addElectricalComponent(load);
        Eln.simulator.addElectricalComponent(parallel);
        ctx.after(25, () -> {
            double amps = load.getCurrent();
            ctx.check("ideal outer connector adds no drop and both half-meter runs heat",
                Math.abs(amps - 12 / (1 + wire.resistanceOhms(.5, a.activeRun().getTemperatureCelsius())
                    + wire.resistanceOhms(.5, b.activeRun().getTemperatureCelsius()))) < 1e-5
                    && a.activeRun().getCurrentAmps() > 11 && a.activeRun().getTemperatureCelsius() > 20,
                amps + " A");
            remove(ctx, corner);
            ctx.check("removed zero-length connector drops no material",
                level.getEntitiesOfClass(ItemEntity.class, new AABB(corner)).isEmpty(), "");
        });
        ctx.after(45, () -> {
            try {
                ctx.check("knuckle removal opens its load and leaves the parallel branch powered",
                    Math.abs(load.getCurrent()) < 1e-6 && Math.abs(parallel.getCurrent() - 3) < 1e-6, "");
            } finally {
                for (var component : List.of(load, parallel, source, ground)) {
                    Eln.simulator.removeElectricalComponent(component);
                    component.breakConnection();
                }
                Eln.simulator.removeElectricalLoad(returnRail);
            }
            remove(ctx, support.relative(first));
            remove(ctx, support.relative(second));
            double recovered = level.getEntitiesOfClass(ItemEntity.class, new AABB(corner).inflate(2)).stream()
                .map(ItemEntity::getItem).filter(stack -> stack.is(wire.parentItem))
                .mapToDouble(wire::getRemainingLengthMeters).sum();
            ctx.check("both removed paid arms refund exactly the original meter", recovered == 1, recovered + " m");
        });
    }

    private static void remove(TestContext ctx, BlockPos pos) {
        var node = (VoxelNode) ((VoxelNodeEntity) ctx.level().getBlockEntity(pos)).getNode();
        if (node.removePart(0, true)) node.removeEmptyBlock();
    }

    private static CableRouter.End face(BlockPos support, Direction face) {
        int x = 8 + 8 * face.getStepX(), y = 8 + 8 * face.getStepY(), z = 8 + 8 * face.getStepZ();
        return new CableRouter.End(support.getX() * 16L + x, support.getY() * 16L + y,
            support.getZ() * 16L + z, face.getOpposite().get3DDataValue(), -1);
    }

    @Override public void measure(TestContext ctx) {}
}
