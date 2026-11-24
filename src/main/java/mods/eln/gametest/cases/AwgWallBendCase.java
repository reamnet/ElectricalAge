package mods.eln.gametest.cases;

import com.mojang.authlib.GameProfile;
import mods.eln.Eln;
import mods.eln.gametest.Reload;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.node.voxel.AwgCablePart;
import mods.eln.node.voxel.VoxelGeometry;
import mods.eln.sixnode.electricalcable.UtilityCableDescriptor;
import mods.eln.sixnode.electricalsource.ElectricalSourceElement;
import mods.eln.sixnode.resistor.ResistorElement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.UUID;

final class AwgWallBendCase implements TestCase {
    private final Direction mount;
    private FakePlayer player;
    private UtilityCableDescriptor wire;
    private ItemStack spool;
    private BlockPos elbow;
    private ResistorElement load;

    AwgWallBendCase(Direction mount) {
        this.mount = mount;
    }

    @Override public String id() { return mount == Direction.UP ? "awg_voxel_ceiling_bend" : "awg_voxel_wall_bend_" + mount.getSerializedName(); }
    @Override public int width() { return 10; }
    @Override public int height() { return 6; }
    @Override public int depth() { return 10; }
    @Override public int floorDepth() { return 10; }
    @Override public int ticks() { return 65; }

    @Override public void build(TestContext ctx) {
        var horizontal = mount.getAxis() == Direction.Axis.Z ? Direction.EAST : Direction.SOUTH;
        var start = ctx.at(3, 1, 3);
        var other = mount == Direction.UP ? Direction.EAST : Direction.UP;
        var end = start.relative(horizontal, 2).relative(other, 2);
        boolean alternate = mount == Direction.SOUTH || mount == Direction.WEST;
        boolean horizontalFirst = (mount.getAxis() == Direction.Axis.Z) != alternate;
        elbow = horizontalFirst ? start.relative(horizontal, 2) : start.relative(other, 2);
        var blocked = horizontalFirst ? start.relative(other) : start.relative(horizontal);
        for (int along = 0; along <= 2; along++)
            for (int up = 0; up <= 2; up++)
                ctx.floor(start.relative(horizontal, along).relative(other, up).relative(mount));
        if (alternate) ctx.level().setBlockAndUpdate(blocked, Blocks.STONE.defaultBlockState());
        if (mount == Direction.NORTH) buildCircuit(ctx, start.west(), end.above());

        player = FakePlayerFactory.get(ctx.level(), new GameProfile(
            UUID.nameUUIDFromBytes(id().getBytes(StandardCharsets.UTF_8)), "eln_awg_bend"));
        player.setGameMode(GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        wire = (UtilityCableDescriptor) Eln.sixNodeItem.getDescriptor(2200);
        spool = wire.newItemStack();
        wire.setRemainingLengthMeters(spool, 6.125);
        player.setItemInHand(InteractionHand.MAIN_HAND, spool);

        var refusal = AwgCablePart.surfaceRouteFailingAtForTests(ctx.level(), start, end, mount, spool, 3);
        ctx.check("failure after the elbow rolls back all wall pieces and the spool", "forced".equals(refusal)
            && wire.getRemainingLengthMeters(spool) == 6.125 && ctx.level().isEmptyBlock(start)
            && ctx.level().isEmptyBlock(elbow) && ctx.level().isEmptyBlock(end), "");

        spool.use(ctx.level(), player, InteractionHand.MAIN_HAND);
        ctx.check("wall bend consumes five meters and leaves its obstruction alone", click(start) == InteractionResult.SUCCESS
            && click(end) == InteractionResult.SUCCESS && wire.getRemainingLengthMeters(spool) == 1.125
            && (!alternate || ctx.level().getBlockState(blocked).is(Blocks.STONE)), "");
        ctx.check("both half-meter arms produce a wall elbow mesh",
            AwgCablePart.at(ctx.level(), elbow).size() == 2
                && !VoxelGeometry.of(new ArrayList<>(AwgCablePart.at(ctx.level(), elbow))).isEmpty(), "");
        ctx.after(35, () -> {
            checkPower(ctx);
            double heat = heat(ctx);
            try {
                Reload.node(ctx.level(), elbow);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
            ctx.check("wall elbow reload preserves both thermal states and paid balance",
                AwgCablePart.at(ctx.level(), elbow).size() == 2 && Math.abs(heat(ctx) - heat) < 1e-8
                    && wire.getRemainingLengthMeters(spool) == 1.125, "");
        });
    }

    private void buildCircuit(TestContext ctx, BlockPos sourcePos, BlockPos loadPos) {
        var side = mods.eln.misc.Direction.fromEnumFacing(mount);
        for (var pos : new BlockPos[]{sourcePos, loadPos, loadPos.above()}) ctx.floor(pos.relative(mount));
        var source = (ElectricalSourceElement) ctx.placeSix(Wp9aCases.SOURCE, sourcePos, side);
        source.networkUnserialize(ctx.stream(out -> {
            out.writeByte(ElectricalSourceElement.setVoltageId);
            out.writeFloat(12);
        }));
        load = (ResistorElement) ctx.placeSix(Wp9aCases.POWER_RESISTOR, loadPos, side);
        Wp9aCases.orient(load, mods.eln.misc.LRDU::right, mods.eln.misc.Direction.YN);
        load.getInventory().setItem(0, Wp9aCases.coal(24));
        load.getInventory().setChanged();
        ctx.placeSix(Wp9aCases.GROUND, loadPos.above(), side);
    }

    private InteractionResult click(BlockPos pos) {
        var face = mount.getOpposite();
        var support = pos.relative(mount);
        var hit = Vec3.atCenterOf(support).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
        player.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
        return spool.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(hit, face, support, false)));
    }

    private double heat(TestContext ctx) {
        return AwgCablePart.at(ctx.level(), elbow).stream().mapToDouble(p -> p.activeRun().getStoredJoules()).sum();
    }

    private void checkPower(TestContext ctx) {
        if (load == null) return;
        double amps = (Wp9aCases.load(load, mods.eln.misc.Direction.YN).getU()
            - Wp9aCases.load(load, mods.eln.misc.Direction.YP).getU()) / load.nominalRs;
        ctx.check("the wall elbow powers the resistor through both half-meter arms", amps > 5
            && AwgCablePart.at(ctx.level(), elbow).stream().allMatch(p -> {
                var run = p.activeRun();
                return Math.abs(run.getCurrentAmps() - amps) < 1e-6
                    && Math.abs(run.getVoltageDrop() - amps * wire.resistanceOhms(0.5, run.getTemperatureCelsius())) < 4e-6;
            }), amps + " A, " + heat(ctx) + " J at the elbow");
    }

    @Override public void measure(TestContext ctx) {
        checkPower(ctx);
    }
}
