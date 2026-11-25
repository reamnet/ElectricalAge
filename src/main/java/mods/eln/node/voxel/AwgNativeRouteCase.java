package mods.eln.node.voxel;

import com.mojang.authlib.GameProfile;
import mods.eln.Eln;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.sixnode.electricalcable.UtilityCableDescriptor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import java.util.UUID;

public final class AwgNativeRouteCase implements TestCase {
    @Override public String id() { return "awg_native_kit_session"; }
    @Override public int width() { return 10; }
    @Override public int height() { return 5; }
    @Override public int depth() { return 10; }
    @Override public int floorDepth() { return 10; }

    @Override
    public void build(TestContext ctx) {
        var wire = (UtilityCableDescriptor) Eln.sixNodeItem.getDescriptor(2200);
        var spool = wire.newItemStack();
        wire.setRemainingLengthMeters(spool, 12.125);
        var player = FakePlayerFactory.get(ctx.level(),
            new GameProfile(UUID.fromString("e7e89587-9791-46cc-a9ab-0d82ef2d78b1"), "eln_awg_native"));
        player.setGameMode(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, spool);
        var start = ctx.at(1, 0, 1);
        var pivot = ctx.at(4, 0, 1);
        var end = ctx.at(4, 0, 4);
        click(player, start);
        var plan = RoutePlacer.PLANS.get(player.getUUID());
        ctx.check("normal click starts a shared session with the original spool", plan != null && plan.wire == wire, "");
        var aim = end(end);
        var first = CablePlacer.route(ctx.level(), plan, end(pivot), null);
        var straight = KitPreview.awgAnswer(ctx.level(), plan, end(pivot), null, 194, first, player);
        ctx.check("native preview proposes a three-meter straight AWG route", straight.ok() && first.length() == 48, straight.message());

        click(player, pivot);
        var bent = CablePlacer.route(ctx.level(), plan, aim, null);
        var preview = KitPreview.awgAnswer(ctx.level(), plan, aim, null, 194, bent, player);
        ctx.check("crosshair aim past a waypoint previews the full six-meter route",
            preview.ok() && bent.length() == 96 && preview.boxes().size() > straight.boxes().size(), preview.message());
        var shortSpool = KitPreview.awgAnswer(ctx.level(), plan, aim, null, 95, bent, player);
        ctx.check("underfunded preview is invalid", !shortSpool.ok(), shortSpool.message());
        var refused = AwgCablePart.layNative(ctx.level(), KitSupply.held(player), bent, null, null, player, 1);
        ctx.check("failed shared lay rolls back nodes and debit", "forced".equals(refused)
            && ctx.level().isEmptyBlock(start.above()) && wire.getRemainingLengthMeters(spool) == 12.125, "" + refused);
        click(player, end);
        ctx.check("click adds the second waypoint without payment",
            CablePlacer.waypoints(player.getUUID()) == 2 && wire.getRemainingLengthMeters(spool) == 12.125, "");
        plan.awgPreview = new CablePlacer.AwgPreview(aim, null, java.util.List.of(end(pivot)), bent);
        var obstruction = ctx.at(2, 1, 1);
        ctx.level().setBlockAndUpdate(obstruction, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        click(player, end);
        ctx.check("a route obstructed after the preview cannot be confirmed or silently rerouted",
            CablePlacer.waypoints(player.getUUID()) == 2 && wire.getRemainingLengthMeters(spool) == 12.125
                && ctx.level().isEmptyBlock(start.above()), "");
        ctx.level().removeBlock(obstruction, false);
        plan.awgPreview = new CablePlacer.AwgPreview(aim, null, java.util.List.of(end(pivot)), bent);
        wire.setRemainingLengthMeters(spool, 5.9375);
        click(player, end);
        ctx.check("a spool shortened after preview cannot fund confirmation or install a partial route",
            CablePlacer.waypoints(player.getUUID()) == 2 && wire.getRemainingLengthMeters(spool) == 5.9375
                && ctx.level().isEmptyBlock(start.above()) && ctx.level().isEmptyBlock(pivot.above())
                && ctx.level().isEmptyBlock(end.above()), "");
        wire.setRemainingLengthMeters(spool, 12.125);
        click(player, end);
        ctx.check("repeat click confirms the displayed route and charges exactly six meters",
            !CablePlacer.active(player.getUUID()) && wire.getRemainingLengthMeters(spool) == 6.125
                && !AwgCablePart.at(ctx.level(), pivot.above()).isEmpty(), "");
        click(player, end);
        player.setShiftKeyDown(true);
        click(player, ctx.at(5, 0, 4));
        ctx.check("replayed click and cancellation cannot spend materials again",
            !CablePlacer.active(player.getUUID()) && wire.getRemainingLengthMeters(spool) == 6.125, "");
        player.setShiftKeyDown(false);
        var branchStart = ctx.at(2, 0, 4);
        var junction = ctx.at(2, 1, 1);
        click(player, branchStart);
        click(player, junction, Vec3.atLowerCornerOf(junction).add(.5, 1.0 / 16, .5));
        ctx.check("clicking an existing wire attaches a real three-way junction without buying old wire",
            !CablePlacer.active(player.getUUID()) && wire.getRemainingLengthMeters(spool) == 3.125
                && AwgCablePart.at(ctx.level(), junction).size() == 3,
            "spool: " + wire.getRemainingLengthMeters(spool));
        CablePlacer.forget(player.getUUID());
    }

    private static CableRouter.End end(BlockPos support) {
        var hit = Vec3.atLowerCornerOf(support).add(0.5, 1, 0.5);
        return CablePlacer.surface(support, Direction.UP, hit.x - support.getX(), 1, hit.z - support.getZ());
    }

    private static void click(FakePlayer player, BlockPos support) {
        var hit = Vec3.atLowerCornerOf(support).add(0.5, 1, 0.5);
        click(player, support, hit);
    }

    private static void click(FakePlayer player, BlockPos support, Vec3 hit) {
        player.snapTo(hit.x, hit.y, hit.z, 0, 90);
        player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
            new BlockHitResult(hit, Direction.UP, support, false)));
    }

    @Override public void measure(TestContext ctx) {}
}
