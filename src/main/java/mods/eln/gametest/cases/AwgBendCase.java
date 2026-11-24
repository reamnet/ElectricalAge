package mods.eln.gametest.cases;

import mods.eln.Eln;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

final class AwgBendCase implements TestCase {
    private UtilityCableDescriptor wire;
    private ItemStack spool;
    private BlockPos bend;
    private ResistorElement resistor;

    @Override public String id() { return "awg_voxel_world_floor_bend"; }
    @Override public int width() { return 10; }
    @Override public int height() { return 5; }
    @Override public int depth() { return 10; }
    @Override public int floorDepth() { return 10; }
    @Override public int ticks() { return 60; }

    @Override public void build(TestContext ctx) {
        var player = FakePlayerFactory.get(ctx.level(), new com.mojang.authlib.GameProfile(
            java.util.UUID.fromString("50726059-2904-4fca-89da-705880ccf152"), "eln_awg_bend"));
        player.setGameMode(GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        wire = (UtilityCableDescriptor) Eln.sixNodeItem.getDescriptor(2200);
        spool = wire.newItemStack();
        wire.setRemainingLengthMeters(spool, 6.25);
        player.setItemInHand(InteractionHand.MAIN_HAND, spool);
        Wp9aCases.source(ctx, Wp9aCases.SOURCE, 0, 1, 12);
        resistor = Wp9aCases.resistor(ctx, Wp9aCases.POWER_RESISTOR, 3, 4, CableCases.CORE);
        Wp9aCases.orient(resistor, mods.eln.misc.LRDU::right, mods.eln.misc.Direction.ZN);
        ctx.placeSix(Wp9aCases.GROUND, ctx.at(3, 1, 5));
        BlockPos start = ctx.at(1, 1, 1);
        bend = ctx.at(3, 1, 1);
        BlockPos end = ctx.at(3, 1, 3);
        spool.use(ctx.level(), player, InteractionHand.MAIN_HAND);
        ctx.check("two clicks lay an L on one floor", click(player, start) == InteractionResult.SUCCESS
            && click(player, end) == InteractionResult.SUCCESS, "");
        var elbow = AwgCablePart.at(ctx.level(), bend);
        ctx.check("the elbow has two independent half-meter arms with its rendered overlap free",
            elbow.size() == 2 && wire.getRemainingLengthMeters(spool) == 1.25
                && !VoxelGeometry.of(new java.util.ArrayList<>(elbow)).isEmpty(), "");
    }

    private InteractionResult click(FakePlayer player, BlockPos pos) {
        player.snapTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0, 90);
        var hit = new BlockHitResult(Vec3.atLowerCornerOf(pos).add(0.5, 0, 0.5), Direction.UP, pos.below(), false);
        return spool.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
    }

    @Override public void measure(TestContext ctx) {
        var elbow = AwgCablePart.at(ctx.level(), bend);
        double measured = (Wp9aCases.load(resistor, mods.eln.misc.Direction.ZN).getU()
            - Wp9aCases.load(resistor, mods.eln.misc.Direction.ZP).getU()) / resistor.nominalRs;
        ctx.check("real resistor and both elbow arms carry the same current",
            measured > 5 && elbow.stream().allMatch(p -> Math.abs(p.activeRun().getCurrentAmps() - measured) < 0.05),
            measured + " A");
        for (var part : elbow) {
            var run = part.activeRun();
            double expected = wire.resistanceOhms(0.5, run.getTemperatureCelsius());
            ctx.check("the elbow arm drops the voltage of half a meter of wire",
                Math.abs(run.getVoltageDrop() / run.getCurrentAmps() - expected) < 2e-6, "" + run.getVoltageDrop());
        }
    }
}
