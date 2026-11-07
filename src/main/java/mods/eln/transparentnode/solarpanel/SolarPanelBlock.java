package mods.eln.transparentnode.solarpanel;

import mods.eln.node.transparent.TransparentNodeBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Native replicated roof geometry. Shapes never query block entities or load chunks. */
public final class SolarPanelBlock extends TransparentNodeBlock {
    public static final IntegerProperty ROOF = IntegerProperty.create("roof", 0, 4);
    private static final VoxelShape[] PITCHED = new VoxelShape[4];
    static {
        for (Direction up : Direction.Plane.HORIZONTAL) {
            VoxelShape shape = Shapes.empty();
            for (int strip = 0; strip < 8; strip++) {
                double a = strip / 8.0, b = (strip + 1) / 8.0;
                double h0 = (up.getStepX() + up.getStepZ()) * (a - .5);
                double h1 = (up.getStepX() + up.getStepZ()) * (b - .5);
                shape = Shapes.or(shape, up.getAxis() == Direction.Axis.X
                    ? Shapes.box(a, Math.min(h0, h1), 0, b, Math.max(h0, h1) + .16, 1)
                    : Shapes.box(0, Math.min(h0, h1), a, 1, Math.max(h0, h1) + .16, b));
            }
            PITCHED[up.get3DDataValue() - 2] = shape.optimize();
        }
    }

    public SolarPanelBlock(Properties properties, SolarPanelDescriptor descriptor) {
        super(properties, descriptor);
        registerDefaultState(defaultBlockState().setValue(ROOF, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(ROOF);
    }

    @Override
    public BlockState placedState(mods.eln.misc.Direction front, Level world, BlockPos pos) {
        Direction up = SolarRoofMount.fromSupport(world, pos).getUphill();
        return super.placedState(front, world, pos).setValue(ROOF, up == null ? 0 : up.get3DDataValue() - 1);
    }

    public boolean hasFlatReservations() {
        var group = descriptor.getGhostGroup(mods.eln.misc.Direction.XN);
        return !((SolarPanelDescriptor) descriptor).canRotate && group != null && group.size() > 0;
    }

    @Override
    protected void tick(BlockState state, net.minecraft.server.level.ServerLevel world, BlockPos pos, net.minecraft.util.RandomSource random) {
        // Scheduled block ticks run on the server thread, unlike the electrical simulator's callbacks.
        if (!SolarGhostShapes.repair(world, pos)) world.scheduleTick(pos, this, 20);
    }

    public static VoxelShape roofShape(int horizontalIndex) {
        return PITCHED[horizontalIndex];
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        int roof = state.getValue(ROOF);
        return roof == 0 ? super.getShape(state, world, pos, context) : roofShape(roof - 1);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return getShape(state, world, pos, context);
    }
}
