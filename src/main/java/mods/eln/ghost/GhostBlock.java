package mods.eln.ghost;

import mods.eln.Eln;
import mods.eln.compat.BlockMeta;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The invisible blocks that make up a multiblock device around its node (1.12 GhostBlock). META: tCube (full block),
 * tFloor (thin floor plate), tLadder (climbable, no shape). Breaking or using one is forwarded to its GhostElement,
 * i.e. to the device. 1.12's per-device collision boxes for tCube ghosts come with the multiblock batch (P2).
 */
public class GhostBlock extends Block {
    public static final int tCube = 0;
    public static final int tFloor = 1;
    public static final int tLadder = 2;

    public static final int tFlatSolar = 13; // Fixed flat arrays: source half-height owner collision.
    public static final int tBatteryFloor = 7;
    public static final int tClearance = 8; // 3..7 retain the native cabinet/battery shapes.
    public static int wallServiceMeta(Direction front) {
        return switch (front) { case XN -> 3; case XP -> 4; case ZN -> 5; case ZP -> 6;
            default -> throw new IllegalArgumentException("A wall cabinet needs a horizontal front"); };
    }
    private static final VoxelShape[] CABINETS = {
        Shapes.box(.67,0,.04,1,1,.96), Shapes.box(0,0,.04,.33,1,.96),
        Shapes.box(.04,0,.67,.96,1,1), Shapes.box(.04,0,0,.96,1,.33),
        Shapes.box(.06,0,.08,.94,1,.92)
    };
    private static final VoxelShape FLAT_SOLAR = Shapes.box(0, 0, 0, 1, .5, 1);
    private static final VoxelShape FLOOR = Block.box(0, 0, 0, 16, 1, 16);

    public GhostBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(BlockMeta.META, tCube));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockMeta.META);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int meta = state.getValue(BlockMeta.META);
        // Four unused native metadata values carry roof pitch to remote clients and survive saves.
        if (meta >= 9 && meta <= 12) return mods.eln.transparentnode.solarpanel.SolarPanelBlock.roofShape(meta - 9);
        if (meta >= 3 && meta <= tBatteryFloor) return CABINETS[meta - 3];
        switch (meta) {
            case tFlatSolar:
                return FLAT_SOLAR;
            case tFloor:
                return FLOOR;
            case tLadder:
            case tClearance:
                return Shapes.empty();
            default:
                return Shapes.block();
        }
    }

    /** A machine's other blocks break as the machine does: fast with a pickaxe (BreakTool). */
    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        return mods.eln.node.BreakTool.progress(state, player, level, pos, mods.eln.node.BreakTool.Kind.PICKAXE);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public boolean isLadder(BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos, net.minecraft.world.entity.LivingEntity entity) {
        return state.getValue(BlockMeta.META) == tLadder;
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        GhostElement element = getElement(level, pos);
        if (element != null) element.breakBlock();
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return activate(level, pos, player, hit);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        // a kit routes to a machine's port on its ghost (the Auto Miner's power: Baughn, a click there opened its screen)
        if (stack.getItem() instanceof mods.eln.node.voxel.PipeTool) return InteractionResult.PASS;
        return activate(level, pos, player, hit);
    }

    private InteractionResult activate(Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            GhostElement element = getElement(level, pos);
            if (element != null) {
                float vx = (float) (hit.getLocation().x - pos.getX()), vy = (float) (hit.getLocation().y - pos.getY()),
                    vz = (float) (hit.getLocation().z - pos.getZ());
                element.onBlockActivated(player, Direction.fromIntMinecraftSide(hit.getDirection().get3DDataValue()), vx, vy, vz);
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** Middle-click on a machine's ghost: the machine (#161). */
    @Override
    public ItemStack getCloneItemStack(net.minecraft.world.level.LevelReader level, BlockPos pos, BlockState state, boolean includeData,
                                      net.minecraft.world.entity.player.Player player) {
        if (!(level instanceof Level world) || world.isClientSide()) return ItemStack.EMPTY;
        GhostElement g = getElement(world, pos);
        if (g == null) return ItemStack.EMPTY;
        var n = mods.eln.node.NodeManager.instance.getNodeFromCoordonate(g.getObservatorCoordonate());
        return n instanceof mods.eln.node.Node node ? node.pickItem(null, level) : ItemStack.EMPTY;
    }

    GhostElement getElement(Level world, BlockPos pos) {
        return Eln.ghostManager.getGhost(new Coordonate(pos, world));
    }

    public String getNodeUuid() {
        return "g";
    }
}
