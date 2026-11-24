package mods.eln.gametest.cases;

import mods.eln.Eln;
import mods.eln.gametest.Reload;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.node.voxel.AwgCablePart;
import mods.eln.node.voxel.VoxelGeometry;
import mods.eln.sixnode.electricalcable.UtilityCableDescriptor;
import mods.eln.sixnode.resistor.ResistorElement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

final class AwgWorldCase implements TestCase {
    private UtilityCableDescriptor wire;
    private ItemStack spool;
    private BlockPos first, second;
    private ResistorElement resistor;
    private FakePlayer player;

    @Override public String id() { return "awg_voxel_world_run"; }
    @Override public int ticks() { return 120; }

    @Override public void build(TestContext ctx) {
        player = FakePlayerFactory.get(ctx.level(), new com.mojang.authlib.GameProfile(
            java.util.UUID.fromString("6aed75b5-e9d7-4660-9ee0-175f18d86552"), "eln_awg_test"));
        player.setGameMode(GameType.SURVIVAL);
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
        var quads = VoxelGeometry.of(new java.util.ArrayList<>(AwgCablePart.at(ctx.level(), first)));
        ctx.check("AWG run emits the existing insulated voxel cable mesh",
            !quads.isEmpty() && quads.stream().allMatch(q -> q.sprite().equals(VoxelGeometry.cableSprite(1))), "");
        place(ctx, second);
        var bad = use(second, spool, InteractionHand.MAIN_HAND, Direction.UP);
        ctx.check("repeated click on blocked placement keeps the original debit and parts", bad == InteractionResult.FAIL
            && wire.getRemainingLengthMeters(spool) == 1.125 && AwgCablePart.at(ctx.level(), second).size() == 1, "");
        checkRejectedActions(ctx);
        checkRepeatRecovery(ctx);
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
            mine(ctx, second);
            ctx.check("removing an AWG block leaves no paid run in that block", AwgCablePart.at(ctx.level(), second).isEmpty(), "");
        });
    }

    private void place(TestContext ctx, BlockPos at) {
        ctx.check("player sneak-use places a supported AWG run",
            use(at, spool, InteractionHand.MAIN_HAND, Direction.UP) == InteractionResult.SUCCESS, "");
    }

    private InteractionResult use(BlockPos pos, ItemStack stack, InteractionHand hand, Direction face) {
        player.snapTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, -90, 90);
        player.setShiftKeyDown(true);
        player.setItemInHand(hand, stack);
        var hit = new BlockHitResult(Vec3.atLowerCornerOf(pos).add(0.5, 0, 0.5), face, pos.below(), false);
        return stack.useOn(new UseOnContext(player, hand, hit));
    }

    private void mine(TestContext ctx, BlockPos pos) {
        player.setShiftKeyDown(false);
        player.snapTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0, 90);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
        var state = ctx.level().getBlockState(pos);
        state.getBlock().onDestroyedByPlayer(state, ctx.level(), pos, player, player.getMainHandItem(), true, state.getFluidState());
    }

    private void checkRejectedActions(TestContext ctx) {
        var at = ctx.at(5, 1, 1);
        var shortSpool = wire.newItemStack();
        wire.setRemainingLengthMeters(shortSpool, 0.9375);
        var insufficient = use(at, shortSpool, InteractionHand.MAIN_HAND, Direction.UP);
        var offHand = use(at, spool, InteractionHand.OFF_HAND, Direction.UP);
        var wallClick = use(at, spool, InteractionHand.MAIN_HAND, Direction.EAST);
        player.snapTo(at.getX() + 20, at.getY(), at.getZ(), -90, 90);
        var hit = new BlockHitResult(Vec3.atLowerCornerOf(at).add(0.5, 0, 0.5), Direction.UP, at.below(), false);
        var distant = spool.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        ctx.check("server rejects short spools, off-hand, wall and out-of-range placement without mutation",
            insufficient == InteractionResult.FAIL && offHand == InteractionResult.FAIL && wallClick == InteractionResult.FAIL
                && distant == InteractionResult.FAIL && ctx.level().isEmptyBlock(at)
                && wire.getRemainingLengthMeters(shortSpool) == 0.9375
                && wire.getRemainingLengthMeters(spool) == 1.125, "");
    }

    private void checkRepeatRecovery(TestContext ctx) {
        var at = ctx.at(6, 1, 1);
        ItemStack circulating = wire.newItemStack();
        wire.setRemainingLengthMeters(circulating, 1);
        for (int cycle = 0; cycle < 3; cycle++) {
            var action = use(at, circulating, InteractionHand.MAIN_HAND, Direction.UP);
            ctx.check("one meter spool is consumed once", action == InteractionResult.SUCCESS && circulating.isEmpty(), "");
            mine(ctx, at);
            var drops = ctx.level().getEntitiesOfClass(ItemEntity.class, new AABB(at).inflate(0.5),
                e -> e.getItem().is(wire.parentItem));
            ctx.check("shears remove the aimed run and return one paid meter",
                ctx.level().isEmptyBlock(at) && drops.size() == 1
                    && wire.getRemainingLengthMeters(drops.getFirst().getItem()) == 1, "");
            circulating = drops.getFirst().getItem().copy();
            drops.getFirst().discard();
        }
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
