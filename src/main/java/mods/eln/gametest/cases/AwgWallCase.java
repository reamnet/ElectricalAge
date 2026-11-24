package mods.eln.gametest.cases;

import com.mojang.authlib.GameProfile;
import mods.eln.Eln;
import mods.eln.gametest.Reload;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.node.voxel.AwgCablePart;
import mods.eln.node.voxel.VoxelGeometry;
import mods.eln.node.voxel.VoxelNode;
import mods.eln.node.voxel.VoxelNodeEntity;
import mods.eln.sixnode.electricalcable.UtilityCableDescriptor;
import mods.eln.sixnode.electricalsource.ElectricalSourceElement;
import mods.eln.sixnode.resistor.ResistorElement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

final class AwgWallCase implements TestCase {
    private final Direction mount;
    private final Direction ceilingAxis;
    private FakePlayer player;
    private UtilityCableDescriptor wire;
    private ItemStack spool;
    private BlockPos start, middle, end;
    private Direction along;
    private ResistorElement resistor;

    AwgWallCase(Direction mount) {
        this(mount, null);
    }

    AwgWallCase(Direction mount, Direction ceilingAxis) {
        this.mount = mount;
        this.ceilingAxis = ceilingAxis;
    }

    @Override public String id() {
        return ceilingAxis == null ? "awg_voxel_wall_" + mount.getSerializedName()
            : "awg_voxel_ceiling_" + ceilingAxis.getAxis().getSerializedName();
    }
    @Override public int width() { return 10; }
    @Override public int height() { return 6; }
    @Override public int depth() { return 10; }
    @Override public int floorDepth() { return 10; }
    @Override public int ticks() { return 85; }

    @Override public void build(TestContext ctx) {
        along = switch (mount) {
            case EAST -> Direction.UP;
            case WEST -> Direction.DOWN;
            case UP -> ceilingAxis;
            default -> Direction.EAST;
        };
        start = ctx.at(3, along == Direction.DOWN ? 4 : along == Direction.UP ? 1 : 2, 4);
        middle = start.relative(along);
        end = middle.relative(along);
        for (var pos : new BlockPos[]{start, middle, end}) ctx.floor(pos.relative(mount));
        player = FakePlayerFactory.get(ctx.level(), new GameProfile(
            UUID.nameUUIDFromBytes(id().getBytes(StandardCharsets.UTF_8)), "eln_awg_wall"));
        player.setGameMode(GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        wire = (UtilityCableDescriptor) Eln.sixNodeItem.getDescriptor(2200);
        spool = wire.newItemStack();
        wire.setRemainingLengthMeters(spool, 4.125);
        player.setItemInHand(InteractionHand.MAIN_HAND, spool);
        if (mount == Direction.NORTH || mount == Direction.EAST || mount == Direction.UP) buildCircuit(ctx);

        var counts = Eln.simulator.counts();
        var forced = AwgCablePart.surfaceRouteFailingAtForTests(ctx.level(), start, end, mount, spool, 1);
        ctx.check("an intermediate failure rolls back wall placement and simulation processes",
            "forced".equals(forced) && untouched(ctx, 4.125) && Arrays.equals(counts, Eln.simulator.counts()), "");
        spool.use(ctx.level(), player, InteractionHand.MAIN_HAND);
        ctx.check("same-face wall start is accepted", click(start) == InteractionResult.SUCCESS, "");

        ctx.level().removeBlock(middle.relative(mount), false);
        ctx.check("unsupported intermediate surface rejects the entire wall route",
            click(end) == InteractionResult.FAIL && untouched(ctx, 4.125), "");
        ctx.floor(middle.relative(mount));
        ctx.level().setBlockAndUpdate(middle, Blocks.STONE.defaultBlockState());
        ctx.check("obstructed wall route leaves the obstruction and spool intact",
            click(end) == InteractionResult.FAIL && ctx.level().getBlockState(middle).is(Blocks.STONE)
                && ctx.level().isEmptyBlock(start) && ctx.level().isEmptyBlock(end)
                && wire.getRemainingLengthMeters(spool) == 4.125, "");
        ctx.level().removeBlock(middle, false);
        wire.setRemainingLengthMeters(spool, 2.9375);
        ctx.check("short spool refuses the complete wall route", click(end) == InteractionResult.FAIL
            && untouched(ctx, 2.9375), "");
        wire.setRemainingLengthMeters(spool, 4.125);
        ctx.check("three wall blocks consume exactly three meters",
            click(end) == InteractionResult.SUCCESS && wire.getRemainingLengthMeters(spool) == 1.125
                && AwgCablePart.at(ctx.level(), start).size() == 1
                && AwgCablePart.at(ctx.level(), middle).size() == 1
                && AwgCablePart.at(ctx.level(), end).size() == 1, "");
        ctx.check("wall part produces the insulated cable mesh",
            !VoxelGeometry.of(new ArrayList<>(AwgCablePart.at(ctx.level(), start))).isEmpty(), "");

        ctx.after(30, () -> {
            if (resistor != null) checkPower(ctx);
            double heat = run(ctx, middle).getStoredJoules();
            try {
                if (mount == Direction.SOUTH) Reload.node(ctx.level(), middle, tag -> {
                    tag.getCompoundOrEmpty("p0").getCompoundOrEmpty("run").putInt("jacketLost", 1);
                    return tag;
                });
                for (var pos : new BlockPos[]{start, middle, end}) Reload.node(ctx.level(), pos);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
            ctx.check("wall save/load restores heat and paid material", Math.abs(run(ctx, middle).getStoredJoules() - heat) < 1e-8
                && wire.getRemainingLengthMeters(spool) == 1.125
                && run(ctx, middle).getJacketDamaged() == (mount == Direction.SOUTH), "");
        });
        ctx.after(60, () -> {
            if (resistor != null) checkPower(ctx);
            var node = (VoxelNode) ((VoxelNodeEntity) ctx.level().getBlockEntity(middle)).getNode();
            if (node.removePart(0, true)) node.removeEmptyBlock();
        });
    }

    private void buildCircuit(TestContext ctx) {
        var sourcePos = start.relative(along.getOpposite());
        var loadPos = end.relative(along);
        var groundPos = loadPos.relative(along);
        for (var pos : new BlockPos[]{sourcePos, loadPos, groundPos}) ctx.floor(pos.relative(mount));
        var side = mods.eln.misc.Direction.fromEnumFacing(mount);
        if (along == Direction.UP) ctx.level().removeBlock(sourcePos, false);
        var source = (ElectricalSourceElement) ctx.placeSix(Wp9aCases.SOURCE, sourcePos, side);
        source.networkUnserialize(ctx.stream(out -> {
            out.writeByte(ElectricalSourceElement.setVoltageId);
            out.writeFloat(12);
        }));
        resistor = (ResistorElement) ctx.placeSix(Wp9aCases.POWER_RESISTOR, loadPos, side);
        Wp9aCases.orient(resistor, mods.eln.misc.LRDU::right, mods.eln.misc.Direction.fromEnumFacing(along.getOpposite()));
        resistor.getInventory().setItem(0, Wp9aCases.coal(24));
        resistor.getInventory().setChanged();
        ctx.placeSix(Wp9aCases.GROUND, groundPos, side);
    }

    private InteractionResult click(BlockPos pos) {
        var clicked = pos.relative(mount);
        var face = mount.getOpposite();
        var hit = Vec3.atCenterOf(clicked).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
        player.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
        return spool.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(hit, face, clicked, false)));
    }

    private boolean untouched(TestContext ctx, double remaining) {
        return wire.getRemainingLengthMeters(spool) == remaining && ctx.level().isEmptyBlock(start)
            && ctx.level().isEmptyBlock(middle) && ctx.level().isEmptyBlock(end)
            && mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(start, ctx.level())) == null;
    }

    private mods.eln.node.voxel.AwgCableRun run(TestContext ctx, BlockPos pos) {
        return AwgCablePart.at(ctx.level(), pos).getFirst().activeRun();
    }

    private void checkPower(TestContext ctx) {
        double current = loadCurrent();
        var cable = run(ctx, middle);
        ctx.check("wall source and resistor share current across all three blocks",
            current > 5 && Math.abs(cable.getCurrentAmps() - current) < 1e-6
                && Math.abs(run(ctx, start).getCurrentAmps() - current) < 1e-6
                && Math.abs(run(ctx, end).getCurrentAmps() - current) < 1e-6, current + " A");
        ctx.check("wall conductor voltage drop matches one meter of temperature-adjusted copper",
            Math.abs(cable.getVoltageDrop() / current - wire.resistanceOhms(1, cable.getTemperatureCelsius())) < 2e-6,
            cable.getVoltageDrop() + " V");
    }

    private double loadCurrent() {
        var forward = mods.eln.misc.Direction.fromEnumFacing(along);
        return (Wp9aCases.load(resistor, forward.getInverse()).getU() - Wp9aCases.load(resistor, forward).getU()) / resistor.nominalRs;
    }

    @Override public void measure(TestContext ctx) {
        var drops = ctx.level().getEntitiesOfClass(ItemEntity.class, new AABB(middle).expandTowards(0, -5, 0).inflate(1.5))
            .stream().map(ItemEntity::getItem).toList();
        double refund = drops.stream().filter(item -> item.is(wire.parentItem)).mapToDouble(wire::getRemainingLengthMeters).sum();
        double scrap = drops.stream().filter(item -> item.is(mods.eln.sixnode.electricalcable.UtilityCables.INSTANCE.getScraps().parentItem))
            .map(mods.eln.generic.ItemNbt::get).mapToDouble(tag -> tag.getDoubleOr("wireScrapMeters", 0)).sum();
        ctx.check("removing the wall segment recovers exactly its paid meter in the appropriate form",
            ctx.level().isEmptyBlock(middle) && (mount == Direction.SOUTH ? refund == 0 && scrap == 1 : refund == 1 && scrap == 0)
                && 2 + refund + scrap + wire.getRemainingLengthMeters(spool) == 4.125, refund + " m intact / " + scrap + " m scrap");
        if (resistor != null) ctx.check("removing the segment opens the wall circuit", Math.abs(loadCurrent()) < 1e-6, "");
    }
}
