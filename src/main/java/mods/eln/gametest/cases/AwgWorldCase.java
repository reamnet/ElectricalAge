package mods.eln.gametest.cases;

import mods.eln.Eln;
import mods.eln.gametest.Reload;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.node.voxel.AwgCablePart;
import mods.eln.node.voxel.VoxelNode;
import mods.eln.node.voxel.VoxelNodeEntity;
import mods.eln.sixnode.electricalcable.UtilityCableDescriptor;
import mods.eln.sixnode.resistor.ResistorElement;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

final class AwgWorldCase implements TestCase {
    private UtilityCableDescriptor wire;
    private ItemStack spool;
    private BlockPos first, second;
    private ResistorElement resistor;

    @Override public String id() { return "awg_voxel_world_run"; }
    @Override public int ticks() { return 120; }

    @Override public void build(TestContext ctx) {
        wire = (UtilityCableDescriptor) Eln.sixNodeItem.getDescriptor(2200);
        spool = wire.newItemStack();
        wire.setRemainingLengthMeters(spool, 3.125);
        first = ctx.at(1, 1, 1);
        second = ctx.at(2, 1, 1);
        Wp9aCases.source(ctx, Wp9aCases.SOURCE, 0, 1, 12);
        resistor = CableCases.end(ctx, 3, 1);
        place(ctx, first);
        var one = run(ctx, first);
        ctx.check("one physical floor AWG run is installed", wire.getRemainingLengthMeters(spool) == 2.125
            && Double.isFinite(one.getTemperatureCelsius()), "");
        place(ctx, second);
        var bad = AwgCablePart.place(ctx.level(), second, 0, 8, 16, 8, spool);
        ctx.check("obstructed placement keeps the original debit and parts", bad != null
            && wire.getRemainingLengthMeters(spool) == 1.125 && AwgCablePart.at(ctx.level(), second).size() == 1, "");
        ctx.after(35, () -> {
            checkCircuit(ctx);
            var original = run(ctx, second);
            double heat = original.getStoredJoules();
            try {
                Reload.node(ctx.level(), second);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
            var restored = run(ctx, second);
            ctx.check("node reload restores AWG heat and replaces the active run",
                restored != original && Math.abs(restored.getStoredJoules() - heat) < 1e-9
                    && original.getCurrentAmps() == 0, "" + heat);
        });
        ctx.after(75, () -> {
            checkCircuit(ctx);
            var node = node(ctx, second);
            node.removePart(0, true);
            node.removeEmptyBlock();
            ctx.check("removing an AWG block leaves no paid run in that block", AwgCablePart.at(ctx.level(), second).isEmpty(), "");
        });
    }

    private void place(TestContext ctx, BlockPos at) {
        var error = AwgCablePart.place(ctx.level(), at, 0, 8, 16, 8, spool);
        if (error != null) throw new IllegalStateException(error);
    }

    private VoxelNode node(TestContext ctx, BlockPos pos) {
        return (VoxelNode) ((VoxelNodeEntity) ctx.level().getBlockEntity(pos)).getNode();
    }

    private mods.eln.node.voxel.AwgCableRun run(TestContext ctx, BlockPos pos) {
        return AwgCablePart.at(ctx.level(), pos).getFirst().activeRun();
    }

    private void checkCircuit(TestContext ctx) {
        var a = run(ctx, first);
        var b = run(ctx, second);
        double amps = a.getCurrentAmps();
        double expectedR = 0.017241 / 3.309 * (1 + 0.00393 * (a.getTemperatureCelsius() - 20));
        double loadCurrent = Wp9aCases.current(resistor);
        ctx.check("adjacent runs carry the six-node Power Resistor's current",
            amps > 5 && amps < 13 && Math.abs(amps - b.getCurrentAmps()) < 1e-7
                && Math.abs(Math.abs(loadCurrent) - amps) < 0.05, amps + " A, load " + loadCurrent);
        ctx.check("world edge has length- and temperature-based voltage drop",
            Math.abs(a.getVoltageDrop() / amps - expectedR) < 2e-6, a.getVoltageDrop() + " V");
    }

    @Override public void measure(TestContext ctx) {
        ctx.check("severed run opens the circuit", Math.abs(run(ctx, first).getCurrentAmps()) < 1e-7
            && Math.abs(Wp9aCases.current(resistor)) < 1e-5, "" + Wp9aCases.current(resistor));
        double returned = ctx.level().getEntitiesOfClass(ItemEntity.class, new AABB(second).inflate(2)).stream()
            .map(ItemEntity::getItem).filter(s -> s.is(wire.parentItem)).mapToDouble(wire::getRemainingLengthMeters).sum();
        ctx.check("removed world wire refunds its paid meter exactly once",
            returned == 1 && wire.getRemainingLengthMeters(spool) + returned + 1 == 3.125, "" + returned);
    }
}
