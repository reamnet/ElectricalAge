package mods.eln.gametest.cases;

import com.mojang.authlib.GameProfile;
import mods.eln.Eln;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.misc.Coordonate;
import mods.eln.node.NodeManager;
import mods.eln.node.voxel.AwgCableEdge;
import mods.eln.node.voxel.AwgCablePart;
import mods.eln.node.voxel.VoxelNode;
import mods.eln.node.voxel.VoxelNodeEntity;
import mods.eln.sim.ElectricalConnection;
import mods.eln.sim.mna.component.Component;
import mods.eln.sim.mna.state.State;
import mods.eln.sixnode.electricalcable.UtilityCableDescriptor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.UUID;

final class AwgRoutingReliabilityCase implements TestCase {
    enum Scenario {
        ALTERNATE, BOTH_BLOCKED, FAIL_AFTER_ONE, FAIL_HALF, INTERRUPTED
    }

    private final Scenario scenario;

    AwgRoutingReliabilityCase(Scenario scenario) {
        this.scenario = scenario;
    }

    @Override public String id() { return "awg_route_" + scenario.name().toLowerCase(java.util.Locale.ROOT); }
    @Override public int width() { return 10; }
    @Override public int height() { return 5; }
    @Override public int depth() { return 10; }
    @Override public int floorDepth() { return 10; }

    @Override public void build(TestContext ctx) {
        var player = FakePlayerFactory.get(ctx.level(), new GameProfile(
            UUID.nameUUIDFromBytes(id().getBytes(StandardCharsets.UTF_8)), "eln_awg_route"));
        player.setGameMode(GameType.SURVIVAL);
        var wire = (UtilityCableDescriptor) Eln.sixNodeItem.getDescriptor(2200);
        var spool = wire.newItemStack();
        wire.setRemainingLengthMeters(spool, 8.125);
        var start = ctx.at(1, 1, 1);
        switch (scenario) {
            case ALTERNATE, BOTH_BLOCKED -> alternate(ctx, player, spool, wire, start);
            case FAIL_AFTER_ONE, FAIL_HALF -> rollback(ctx, player, spool, wire, start);
            case INTERRUPTED -> interrupted(ctx, player, spool, wire, start);
        }
    }

    private void alternate(TestContext ctx, FakePlayer player, ItemStack spool, UtilityCableDescriptor wire, BlockPos start) {
        var end = ctx.at(3, 1, 3);
        var primary = ctx.at(2, 1, 1);
        var secondary = ctx.at(1, 1, 2);
        ctx.level().setBlockAndUpdate(primary, Blocks.STONE.defaultBlockState());
        if (scenario == Scenario.BOTH_BLOCKED) ctx.level().setBlockAndUpdate(secondary, Blocks.STONE.defaultBlockState());
        begin(ctx, player, spool, start);
        var result = click(player, spool, end);
        if (scenario == Scenario.BOTH_BLOCKED) {
            ctx.check("two blocked candidates leave both floor routes and spool untouched",
                result == InteractionResult.FAIL && wire.getRemainingLengthMeters(spool) == 8.125
                    && ctx.level().getBlockState(primary).is(Blocks.STONE) && ctx.level().getBlockState(secondary).is(Blocks.STONE)
                    && empty(ctx, start) && empty(ctx, end) && empty(ctx, ctx.at(3, 1, 1)) && empty(ctx, ctx.at(1, 1, 3)), "");
            ctx.level().removeBlock(secondary, false);
            result = click(player, spool, end);
        }
        ctx.check("primary obstruction uses the legal alternate with exactly five paid meters",
            result == InteractionResult.SUCCESS && wire.getRemainingLengthMeters(spool) == 3.125
                && ctx.level().getBlockState(primary).is(Blocks.STONE) && empty(ctx, ctx.at(3, 1, 1))
                && AwgCablePart.at(ctx.level(), ctx.at(1, 1, 3)).size() == 2
                && AwgCablePart.at(ctx.level(), secondary).size() == 1 && AwgCablePart.at(ctx.level(), end).size() == 1, "");
    }

    private void rollback(TestContext ctx, FakePlayer player, ItemStack spool, UtilityCableDescriptor wire, BlockPos start) {
        var source = Wp9aCases.source(ctx, Wp9aCases.SOURCE, 0, 1, 12);
        var existingSpool = wire.newItemStack();
        wire.setRemainingLengthMeters(existingSpool, 1);
        if (AwgCablePart.place(ctx.level(), start, 0, 4, 16, 4, existingSpool) != null)
            throw new IllegalStateException("Could not install independent AWG run");
        var original = AwgCablePart.at(ctx.level(), start).getFirst();
        var originalTag = new CompoundTag();
        original.write(originalTag);
        var existingBlock = ctx.level().getBlockState(start);
        int length = scenario == Scenario.FAIL_AFTER_ONE ? 3 : 6;
        var end = start.offset(length - 1, 0, 0);
        VoxelNode emptyBefore = null;
        if (scenario == Scenario.FAIL_HALF) {
            var emptySlot = start.east();
            var fixtureSpool = wire.newItemStack();
            wire.setRemainingLengthMeters(fixtureSpool, 1);
            if (AwgCablePart.place(ctx.level(), emptySlot, 0, 4, 16, 4, fixtureSpool) != null)
                throw new IllegalStateException("Could not create existing voxel block");
            emptyBefore = (VoxelNode) ((VoxelNodeEntity) ctx.level().getBlockEntity(emptySlot)).getNode();
            emptyBefore.removePart(0, false);
        }
        var processes = Eln.simulator.counts();
        var topology = electricalCounts();
        var error = AwgCablePart.routeFailingAtForTests(ctx.level(), start, end, spool, length / 2);
        var after = new CompoundTag();
        original.write(after);
        ctx.check("late placement rejection preserves the original block, run state and spool",
            "forced".equals(error)
                && wire.getRemainingLengthMeters(spool) == 8.125 && ctx.level().getBlockState(start) == existingBlock
                && AwgCablePart.at(ctx.level(), start).equals(java.util.List.of(original)) && originalTag.equals(after), "" + error);
        boolean noOrphans = true;
        for (int x = 1; x < length; x++) {
            var pos = start.offset(x, 0, 0);
            if (x == 1 && emptyBefore != null) {
                noOrphans &= ctx.level().getBlockEntity(pos) instanceof VoxelNodeEntity existing
                    && existing.getNode() == emptyBefore && emptyBefore.parts.isEmpty();
            } else noOrphans &= empty(ctx, pos);
        }
        ctx.check("rollback leaves the adjacent source device and state intact",
            NodeManager.instance.getNodeFromCoordonate(new Coordonate(start.west(), ctx.level())) == source.sixNode, "");
        ctx.check("rollback detaches all temporary loads, edges and processes",
            noOrphans && Arrays.equals(processes, Eln.simulator.counts()) && Arrays.equals(topology, electricalCounts()), "");
        ctx.check("rolling back drops no unpaid wire",
            ctx.level().getEntitiesOfClass(ItemEntity.class, new AABB(start).expandTowards(length, 1, 1)).isEmpty(), "");

        begin(ctx, player, spool, start);
        ctx.check("player can retry successfully after rollback",
            click(player, spool, end) == InteractionResult.SUCCESS && wire.getRemainingLengthMeters(spool) == 8.125 - length, "");
        var node = (VoxelNode) ((VoxelNodeEntity) ctx.level().getBlockEntity(end)).getNode();
        if (node.removePart(0, true)) node.removeEmptyBlock();
        var refunds = ctx.level().getEntitiesOfClass(ItemEntity.class, new AABB(end));
        ctx.check("retry route refunds exactly one paid meter when the end is removed",
            refunds.size() == 1 && wire.getRemainingLengthMeters(refunds.getFirst().getItem()) == 1.0
                && wire.getRemainingLengthMeters(spool) + (length - 1) + 1 == 8.125, "");
    }

    private void interrupted(TestContext ctx, FakePlayer player, ItemStack spool, UtilityCableDescriptor wire, BlockPos start) {
        begin(ctx, player, spool, start);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        AwgCablePart.checkRoutingSelection(new PlayerTickEvent.Post(player));
        player.setItemInHand(InteractionHand.MAIN_HAND, spool);
        var end = ctx.at(4, 1, 1);
        ctx.check("switching items abandons a pending route; the next click only places one meter",
            click(player, spool, end) == InteractionResult.SUCCESS && wire.getRemainingLengthMeters(spool) == 7.125
                && empty(ctx, start) && empty(ctx, ctx.at(2, 1, 1)) && AwgCablePart.at(ctx.level(), end).size() == 1, "");
        begin(ctx, player, spool, start);
        spool.use(ctx.level(), player, InteractionHand.MAIN_HAND);
        ctx.check("explicit cancellation leaves the pending start and spool unchanged",
            empty(ctx, start) && wire.getRemainingLengthMeters(spool) == 7.125, "");
    }

    private void begin(TestContext ctx, FakePlayer player, ItemStack spool, BlockPos start) {
        player.setShiftKeyDown(true);
        player.setItemInHand(InteractionHand.MAIN_HAND, spool);
        spool.use(ctx.level(), player, InteractionHand.MAIN_HAND);
        if (click(player, spool, start) != InteractionResult.SUCCESS) throw new IllegalStateException("AWG start refused");
    }

    private InteractionResult click(FakePlayer player, ItemStack spool, BlockPos pos) {
        player.snapTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, -90, 90);
        var hit = new BlockHitResult(Vec3.atLowerCornerOf(pos).add(0.5, 0, 0.5), Direction.UP, pos.below(), false);
        return spool.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
    }

    private static boolean empty(TestContext ctx, BlockPos pos) {
        return ctx.level().isEmptyBlock(pos) && ctx.level().getBlockEntity(pos) == null
            && NodeManager.instance.getNodeFromCoordonate(new Coordonate(pos, ctx.level())) == null;
    }

    private static int[] electricalCounts() {
        var root = Eln.simulator.mna;
        var states = new HashSet<State>(root.addStates);
        for (var system : root.systems()) states.addAll(system.states);
        var components = new HashSet<Component>(root.addComponents);
        for (var state : states) components.addAll(state.getConnectedComponents());
        return new int[]{states.size() + root.stats()[3],
            (int) components.stream().filter(AwgCableEdge.class::isInstance).count(),
            (int) components.stream().filter(ElectricalConnection.class::isInstance).count()};
    }

    @Override public void measure(TestContext ctx) {}
}
