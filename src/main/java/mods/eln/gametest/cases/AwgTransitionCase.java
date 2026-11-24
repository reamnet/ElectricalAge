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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

final class AwgTransitionCase implements TestCase {
    private final boolean reverse;
    private final Direction flatMount;
    private static final Direction WALL = Direction.NORTH;
    private UtilityCableDescriptor wire;
    private FakePlayer player;
    private ItemStack spool;
    private BlockPos corner;
    private ResistorElement load;

    AwgTransitionCase(boolean reverse) {
        this(reverse, Direction.DOWN);
    }

    AwgTransitionCase(boolean reverse, Direction flatMount) {
        this.reverse = reverse;
        this.flatMount = flatMount;
    }

    @Override public String id() {
        String flat = flatMount == Direction.DOWN ? "floor" : "ceiling";
        return "awg_voxel_" + (reverse ? "wall_to_" + flat : flat + "_to_wall");
    }

    @Override public int width() { return 10; }
    @Override public int height() { return 6; }
    @Override public int depth() { return 10; }
    @Override public int floorDepth() { return 10; }
    @Override public int ticks() { return 95; }

    @Override public void build(TestContext ctx) {
        boolean floor = flatMount == Direction.DOWN;
        Direction rise = flatMount.getOpposite();
        corner = ctx.at(4, floor ? 1 : 4, 3);
        var flatEnd = corner.south(2);
        var wallEnd = corner.relative(rise, 2);
        var sourcePos = flatEnd.south();
        var loadPos = wallEnd.relative(rise);
        var ground = loadPos.relative(rise);
        for (int i = 0; i <= 3; i++) ctx.floor(corner.south(i).relative(flatMount));
        for (int i = 0; i <= 4; i++) ctx.floor(corner.relative(rise, i).relative(WALL));
        if (!floor) ctx.level().removeBlock(ground, false);
        buildCircuit(ctx, sourcePos, loadPos, ground, rise);

        player = FakePlayerFactory.get(ctx.level(), new GameProfile(
            UUID.nameUUIDFromBytes(id().getBytes(StandardCharsets.UTF_8)), "eln_awg_transition"));
        player.setGameMode(GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        wire = (UtilityCableDescriptor) Eln.sixNodeItem.getDescriptor(2200);
        spool = wire.newItemStack();
        wire.setRemainingLengthMeters(spool, 7.125);
        player.setItemInHand(InteractionHand.MAIN_HAND, spool);
        var counts = Eln.simulator.counts();
        var rejected = AwgCablePart.transitionFailingAtForTests(ctx.level(), flatEnd, flatMount, wallEnd, WALL, spool, 4);
        ctx.check("cross-face rollback removes the corner and releases its loads", "forced".equals(rejected)
            && untouched(ctx, flatEnd, wallEnd, 7.125) && Arrays.equals(counts, Eln.simulator.counts()), "");

        spool.use(ctx.level(), player, InteractionHand.MAIN_HAND);
        ctx.check("first transition endpoint selected", (reverse ? click(wallEnd, WALL) : click(flatEnd, flatMount))
            == InteractionResult.SUCCESS, "");
        var unsupported = corner.relative(rise).relative(WALL);
        ctx.level().removeBlock(unsupported, false);
        ctx.check("missing wall support refuses a whole transition", finish(flatEnd, wallEnd) == InteractionResult.FAIL
            && untouched(ctx, flatEnd, wallEnd, 7.125), "");
        ctx.floor(unsupported);
        wire.setRemainingLengthMeters(spool, 5.9375);
        ctx.check("insufficient wire refuses a whole transition", finish(flatEnd, wallEnd) == InteractionResult.FAIL
            && untouched(ctx, flatEnd, wallEnd, 5.9375), "");
        wire.setRemainingLengthMeters(spool, 7.125);
        ctx.check("transition debits six meters for six independent parts", finish(flatEnd, wallEnd) == InteractionResult.SUCCESS
            && wire.getRemainingLengthMeters(spool) == 1.125 && AwgCablePart.at(ctx.level(), corner).size() == 2, "");
        ctx.check("inner corner emits the existing voxel cable mesh",
            !VoxelGeometry.of(new ArrayList<>(AwgCablePart.at(ctx.level(), corner))).isEmpty(), "");

        ctx.after(30, () -> {
            checkElectrical(ctx, rise);
            double heat = heat(ctx);
            try {
                for (int i = 1; i <= 2; i++) Reload.node(ctx.level(), corner.south(i));
                Reload.node(ctx.level(), corner);
                for (int i = 1; i <= 2; i++) Reload.node(ctx.level(), corner.relative(rise, i));
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
            ctx.check("transition reload preserves both arms and their heat",
                heat > 0 && Math.abs(heat(ctx) - heat) < 1e-8 && wire.getRemainingLengthMeters(spool) == 1.125, heat + " J");
        });
        ctx.after(55, () -> {
            checkElectrical(ctx, rise);
            var node = (VoxelNode) ((VoxelNodeEntity) ctx.level().getBlockEntity(corner)).getNode();
            node.removePart(0, true);
        });
    }

    private void buildCircuit(TestContext ctx, BlockPos sourcePos, BlockPos loadPos, BlockPos ground, Direction rise) {
        var source = (ElectricalSourceElement) ctx.placeSix(Wp9aCases.SOURCE, sourcePos, mods.eln.misc.Direction.fromEnumFacing(flatMount));
        source.networkUnserialize(ctx.stream(out -> {
            out.writeByte(ElectricalSourceElement.setVoltageId);
            out.writeFloat(12);
        }));
        var side = mods.eln.misc.Direction.fromEnumFacing(WALL);
        load = (ResistorElement) ctx.placeSix(Wp9aCases.POWER_RESISTOR, loadPos, side);
        Wp9aCases.orient(load, mods.eln.misc.LRDU::right, mods.eln.misc.Direction.fromEnumFacing(rise.getOpposite()));
        load.getInventory().setItem(0, Wp9aCases.coal(24));
        load.getInventory().setChanged();
        ctx.placeSix(Wp9aCases.GROUND, ground, side);
    }

    private InteractionResult finish(BlockPos flatEnd, BlockPos wallEnd) {
        return reverse ? click(flatEnd, flatMount) : click(wallEnd, WALL);
    }

    private InteractionResult click(BlockPos pos, Direction mount) {
        var support = pos.relative(mount);
        var face = mount.getOpposite();
        var hit = Vec3.atCenterOf(support).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
        player.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
        return spool.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(hit, face, support, false)));
    }

    private boolean untouched(TestContext ctx, BlockPos floorEnd, BlockPos wallEnd, double meters) {
        return ctx.level().isEmptyBlock(corner) && ctx.level().isEmptyBlock(floorEnd) && ctx.level().isEmptyBlock(wallEnd)
            && wire.getRemainingLengthMeters(spool) == meters;
    }

    private double heat(TestContext ctx) {
        return AwgCablePart.at(ctx.level(), corner).stream().mapToDouble(p -> p.activeRun().getStoredJoules()).sum();
    }

    private double current(Direction rise) {
        var forward = mods.eln.misc.Direction.fromEnumFacing(rise);
        return (Wp9aCases.load(load, forward.getInverse()).getU() - Wp9aCases.load(load, forward).getU()) / load.nominalRs;
    }

    private void checkElectrical(TestContext ctx, Direction rise) {
        double amps = current(rise);
        var runs = AwgCablePart.at(ctx.level(), corner);
        ctx.check("explicit inner-corner joint carries resistor current through both arms",
            amps > 5 && runs.size() == 2 && runs.stream().allMatch(p -> {
                var run = p.activeRun();
                return Math.abs(run.getCurrentAmps() - amps) < 1e-6
                    && Math.abs(run.getVoltageDrop() - amps * wire.resistanceOhms(1.0, run.getTemperatureCelsius())) < 8e-6;
            }), amps + " A / " + heat(ctx) + " J");
    }

    @Override public void measure(TestContext ctx) {
        double refunded = ctx.level().getEntitiesOfClass(ItemEntity.class, new AABB(corner).expandTowards(0, -5, 0).inflate(1.5)).stream()
            .map(ItemEntity::getItem).filter(s -> s.is(wire.parentItem)).mapToDouble(wire::getRemainingLengthMeters).sum();
        ctx.check("one surviving corner arm is open and only the removed paid meter returns",
            AwgCablePart.at(ctx.level(), corner).size() == 1 && Math.abs(current(flatMount.getOpposite())) < 1e-6
                && refunded == 1 && refunded + 5 + wire.getRemainingLengthMeters(spool) == 7.125, refunded + " m");
    }
}
