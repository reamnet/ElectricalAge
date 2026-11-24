package mods.eln.gametest.cases;

import com.mojang.authlib.GameProfile;
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
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import java.util.Arrays;
import java.util.UUID;

final class AwgJunctionCase implements TestCase {
    private UtilityCableDescriptor wire;
    private ItemStack spool;
    private BlockPos center, branchFirst, branchEnd;
    private ResistorElement straight, branch;
    private FakePlayer player;

    @Override public String id() { return "awg_voxel_world_three_way_junction"; }
    @Override public int width() { return 10; }
    @Override public int height() { return 5; }
    @Override public int depth() { return 10; }
    @Override public int floorDepth() { return 10; }
    @Override public int ticks() { return 100; }

    @Override public void build(TestContext ctx) {
        player = FakePlayerFactory.get(ctx.level(), new GameProfile(
            UUID.fromString("50726059-2904-4fca-89da-705880ccf153"), "eln_awg_junction"));
        player.setGameMode(GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        wire = (UtilityCableDescriptor) Eln.sixNodeItem.getDescriptor(2200);
        spool = wire.newItemStack();
        wire.setRemainingLengthMeters(spool, 9.125);
        player.setItemInHand(InteractionHand.MAIN_HAND, spool);
        center = ctx.at(2, 1, 3);
        branchFirst = ctx.at(2, 1, 4);
        branchEnd = ctx.at(2, 1, 5);
        Wp9aCases.source(ctx, Wp9aCases.SOURCE, 0, 3, 12);
        straight = Wp9aCases.resistor(ctx, Wp9aCases.POWER_RESISTOR, 6, 3, 36);
        ctx.placeSix(Wp9aCases.GROUND, ctx.at(7, 1, 3));
        branch = Wp9aCases.resistor(ctx, Wp9aCases.POWER_RESISTOR, 2, 6, 24);
        Wp9aCases.orient(branch, mods.eln.misc.LRDU::right, mods.eln.misc.Direction.ZN);
        ctx.placeSix(Wp9aCases.GROUND, ctx.at(2, 1, 7));
        begin(ctx, ctx.at(1, 1, 3));
        check(ctx, click(ctx.at(5, 1, 3), false) == InteractionResult.SUCCESS, "five-meter trunk placed");
        ctx.after(25, () -> branch(ctx));
        ctx.after(50, () -> {
            checkCircuit(ctx);
            var before = AwgCablePart.at(ctx.level(), center).stream().mapToDouble(p -> p.activeRun().getStoredJoules()).sum();
            try {
                for (int x = 1; x <= 5; x++) Reload.node(ctx.level(), ctx.at(x, 1, 3));
                Reload.node(ctx.level(), branchFirst);
                Reload.node(ctx.level(), branchEnd);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
            var after = AwgCablePart.at(ctx.level(), center).stream().mapToDouble(p -> p.activeRun().getStoredJoules()).sum();
            check(ctx, Math.abs(after - before) < 1e-8 && wire.getRemainingLengthMeters(spool) == 1.625,
                "reloading every wire block preserves junction heat and spool balance");
        });
        ctx.after(75, () -> {
            checkCircuit(ctx);
            // Bring one real, powered world run close to fusion without heating the entire circuit.
            var physics = new mods.eln.sixnode.electricalcable.WireThermalPhysics(wire.getMaterial(), 3.309, 1.0);
            try {
                Reload.node(ctx.level(), branchFirst, tag -> {
                    var part = tag.getCompoundOrEmpty("p0");
                    var state = part.getCompoundOrEmpty("run");
                    state.putDouble("joules", physics.getFailureEnthalpy() - 0.01);
                    return tag;
                });
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        });
        ctx.after(85, () -> {
            var fused = AwgCablePart.at(ctx.level(), branchFirst).getFirst().activeRun();
            check(ctx, fused.getConductorMelted() && fused.getCurrentAmps() == 0 && branchCurrent() < 1e-6
                && Math.abs(Wp9aCases.current(straight)) > 0.5, "the preheated powered branch fuses independently");
            remove(ctx, branchFirst);
            check(ctx, ctx.level().getEntitiesOfClass(ItemEntity.class, new AABB(branchFirst)).isEmpty(),
                "fused branch material cannot be refunded");
            remove(ctx, branchEnd);
        });
    }

    private static void remove(TestContext ctx, BlockPos pos) {
        var voxel = (VoxelNode) ((VoxelNodeEntity) ctx.level().getBlockEntity(pos)).getNode();
        if (voxel.removePart(0, true)) voxel.removeEmptyBlock();
    }

    private void branch(TestContext ctx) {
        var crossingStart = ctx.at(2, 1, 1);
        begin(ctx, crossingStart);
        check(ctx, click(branchEnd, false) == InteractionResult.FAIL && ctx.level().isEmptyBlock(crossingStart)
            && AwgCablePart.at(ctx.level(), center).size() == 1 && wire.getRemainingLengthMeters(spool) == 4.125,
            "a through-route cannot silently short to an unselected crossing");
        spool.use(ctx.level(), player, InteractionHand.MAIN_HAND);
        var original = AwgCablePart.at(ctx.level(), center).getFirst();
        var expected = new CompoundTag();
        original.write(expected);
        var counts = Eln.simulator.counts();
        var error = AwgCablePart.routeFailingAtForTests(ctx.level(), center, branchEnd, spool, 1);
        var actual = new CompoundTag();
        original.write(actual);
        check(ctx, "forced".equals(error) && expected.equals(actual) && Arrays.equals(counts, Eln.simulator.counts())
            && AwgCablePart.at(ctx.level(), center).getFirst() == original && ctx.level().isEmptyBlock(branchFirst)
            && wire.getRemainingLengthMeters(spool) == 4.125, "mid-placement rollback restores the intact hot trunk");

        begin(ctx, branchEnd);
        ctx.level().setBlockAndUpdate(branchFirst, Blocks.STONE.defaultBlockState());
        check(ctx, click(center, true) == InteractionResult.FAIL && wire.getRemainingLengthMeters(spool) == 4.125
            && AwgCablePart.at(ctx.level(), center).getFirst() == original, "blocked tap leaves trunk and spool intact");
        ctx.level().removeBlock(branchFirst, false);
        wire.setRemainingLengthMeters(spool, 2.4375);
        check(ctx, click(center, true) == InteractionResult.FAIL && wire.getRemainingLengthMeters(spool) == 2.4375
            && AwgCablePart.at(ctx.level(), center).getFirst() == original, "underfunded tap leaves trunk unsplit");
        wire.setRemainingLengthMeters(spool, 4.125);
        check(ctx, click(center, true) == InteractionResult.SUCCESS && wire.getRemainingLengthMeters(spool) == 1.625
            && AwgCablePart.at(ctx.level(), center).size() == 3, "straight trunk splits into a tee with only 2.5 m charged");
        var north = ctx.at(2, 1, 1);
        begin(ctx, north);
        check(ctx, click(center, true) == InteractionResult.FAIL && wire.getRemainingLengthMeters(spool) == 1.625
            && ctx.level().isEmptyBlock(north), "a fourth arm is refused");
        spool.use(ctx.level(), player, InteractionHand.MAIN_HAND);
    }

    private void checkCircuit(TestContext ctx) {
        double direct = Math.abs(Wp9aCases.current(straight));
        double lateral = branchCurrent();
        var arms = AwgCablePart.at(ctx.level(), center);
        var currents = arms.stream().mapToDouble(p -> Math.abs(p.activeRun().getCurrentAmps())).sorted().toArray();
        ctx.check("unequal loads and arm currents obey Kirchhoff's current law",
            direct > 0.5 && lateral > 5 && lateral > direct * 5 && currents.length == 3
                && Math.abs(currents[0] - direct) < 0.05 && Math.abs(currents[1] - lateral) < 0.05
                && Math.abs(currents[2] - lateral - direct) < 0.05, direct + " A and " + lateral + " A");
        for (var part : arms) {
            var run = part.activeRun();
            double expected = wire.resistanceOhms(0.5, run.getTemperatureCelsius());
            ctx.check("each arm retains independent half-meter resistance",
                Math.abs(run.getVoltageDrop() / run.getCurrentAmps() - expected) < 2e-6, "" + run.getVoltageDrop());
        }
        check(ctx, arms.get(0).activeRun().getStoredJoules() > arms.get(1).activeRun().getStoredJoules(),
            "the higher-current feeder has heated more than the straight load arm");
    }

    private double branchCurrent() {
        return Math.abs(Wp9aCases.load(branch, mods.eln.misc.Direction.ZN).getU()
            - Wp9aCases.load(branch, mods.eln.misc.Direction.ZP).getU()) / branch.nominalRs;
    }

    private void begin(TestContext ctx, BlockPos start) {
        spool.use(ctx.level(), player, InteractionHand.MAIN_HAND);
        check(ctx, click(start, false) == InteractionResult.SUCCESS, "route start selected");
    }

    private InteractionResult click(BlockPos pos, boolean wireTop) {
        player.snapTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0, 90);
        var hit = new BlockHitResult(Vec3.atLowerCornerOf(pos).add(0.5, wireTop ? 1.0 / 16 : 0, 0.5),
            Direction.UP, wireTop ? pos : pos.below(), false);
        return spool.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
    }

    private static void check(TestContext ctx, boolean ok, String message) {
        ctx.check(message, ok, "");
    }

    @Override public void measure(TestContext ctx) {
        check(ctx, Math.abs(Wp9aCases.current(straight)) > 0.5 && branchCurrent() < 1e-6,
            "removing the branch leaves the straight load powered");
        double refunded = ctx.level().getEntitiesOfClass(ItemEntity.class, new AABB(branchEnd).inflate(0.5)).stream()
            .map(ItemEntity::getItem).filter(s -> s.is(wire.parentItem)).mapToDouble(wire::getRemainingLengthMeters).sum();
        check(ctx, refunded == 1.0 && wire.getRemainingLengthMeters(spool) + refunded + 5.5 + 1.0 == 9.125,
            "refund, inventory, fused loss and remaining wire account for all 9.125 m");
    }
}
