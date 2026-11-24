package mods.eln.node.voxel;

import mods.eln.Eln;
import mods.eln.generic.ItemNbt;
import mods.eln.sim.Ambient;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.mna.state.VoltageState;
import mods.eln.sixnode.electricalcable.UtilityCableDescriptor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/** Controlled floor-mounted straight runs of the existing 12 AWG spool. */
public final class AwgCablePart extends VoxelPart {
    static final String KIND = "awg_cable";
    private static final int WIRE_ID = 2200;
    private static final int MAX_ROUTE_BLOCKS = 64;
    private static final Map<Player, Selection> SELECTIONS = new WeakHashMap<>();

    private record Selection(ItemStack spool, ServerLevel level, BlockPos start, long expires) {}
    private static final Map<ServerLevel, Map<Point, Terminal>> TERMINALS = new WeakHashMap<>();

    private record Point(long x, int y, long z) {}

    private static final class Terminal {
        final ElectricalLoad load = new ElectricalLoad();
        int users;
    }

    private final int x0, z0, x1, z1;
    final CablePart geometry;
    private CompoundTag saved;
    private AwgCableRun run;
    private Terminal from, to;

    private AwgCablePart(int x0, int z0, int x1, int z1) {
        if (!valid(x0, z0, x1, z1)) throw new IllegalArgumentException("Invalid floor AWG run");
        this.x0 = x0;
        this.z0 = z0;
        this.x1 = x1;
        this.z1 = z1;
        geometry = new CablePart(1, new CableSpec(3, CableSpec.BLACK), 0, new int[]{x0, 0, z0, x1, 0, z1});
    }

    private static boolean valid(int x0, int z0, int x1, int z1) {
        return x0 >= 0 && x0 <= 16 && x1 >= 0 && x1 <= 16 && z0 >= 0 && z0 <= 16 && z1 >= 0 && z1 <= 16
            && (x0 == x1 ? z0 != z1 && x0 > 0 && x0 < 16 : z0 == z1 && z0 > 0 && z0 < 16);
    }

    static UtilityCableDescriptor wire() {
        return (UtilityCableDescriptor) Eln.sixNodeItem.getDescriptor(WIRE_ID);
    }

    public static boolean supportsSpool(mods.eln.node.six.SixNodeDescriptor descriptor) {
        return descriptor instanceof UtilityCableDescriptor && descriptor.parentItemDamage == WIRE_ID;
    }

    /** Sneak-use in air switches the current spool between one-block and route placement. */
    public static InteractionResult toggleRouting(net.minecraft.world.level.Level level, Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (level instanceof ServerLevel server && !player.isSpectator() && player.getAbilities().mayBuild) {
            var spool = player.getMainHandItem();
            if (selection(server, player, spool) == null) {
                SELECTIONS.put(player, new Selection(spool, server, null, server.getGameTime() + CablePlacer.IDLE_TICKS));
                tell(player, "AWG route: sneak-right-click the first floor block");
            } else {
                SELECTIONS.remove(player);
                tell(player, "AWG route cancelled");
            }
        }
        return InteractionResult.SUCCESS;
    }

    private static Selection selection(ServerLevel level, Player player, ItemStack spool) {
        var selected = SELECTIONS.get(player);
        if (selected != null && (selected.spool() != spool || selected.level() != level || level.getGameTime() >= selected.expires())) {
            SELECTIONS.remove(player);
            return null;
        }
        return selected;
    }

    private static void tell(Player player, String message) {
        player.sendOverlayMessage(net.minecraft.network.chat.Component.literal(message));
    }

    /** Sneak-use on the floor lays one meter, or selects a route endpoint while in route mode. */
    public static InteractionResult useSpool(UseOnContext ctx) {
        Player player = ctx.getPlayer();
        if (player == null || ctx.getHand() != InteractionHand.MAIN_HAND || ctx.getClickedFace() != Direction.UP)
            return InteractionResult.FAIL;
        // Only the server lays the run or changes the spool.
        if (!(ctx.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        BlockPos floor = ctx.getClickedPos();
        BlockPos pos = floor.above();
        ItemStack spool = ctx.getItemInHand();
        var hit = ctx.getClickLocation();
        if (player.isSpectator() || !player.getAbilities().mayBuild || player.getMainHandItem() != spool
            || !level.hasChunkAt(floor) || !level.mayInteract(player, floor) || !level.mayInteract(player, pos)
            || !new net.minecraft.world.phys.AABB(floor).inflate(0.01).contains(hit)
            || !(player.getEyePosition().distanceTo(hit) <= player.blockInteractionRange())) return InteractionResult.FAIL;

        var selected = selection(level, player, spool);
        if (selected != null) {
            if (!canRouteIn(level, pos, player)) return InteractionResult.FAIL;
            if (selected.start() == null) {
                SELECTIONS.put(player, new Selection(spool, level, pos, level.getGameTime() + CablePlacer.IDLE_TICKS));
                tell(player, "AWG route start set. Sneak-right-click the destination floor block");
            } else if (!selected.start().equals(pos)) {
                String error = route(level, selected.start(), pos, spool, player);
                if (error != null) {
                    tell(player, error);
                    return InteractionResult.FAIL;
                }
                SELECTIONS.remove(player);
                tell(player, "AWG route placed");
            }
            return InteractionResult.SUCCESS;
        }

        boolean alongX = ctx.getHorizontalDirection().getAxis() == Direction.Axis.X;
        String error = place(level, pos, alongX ? 0 : 8, alongX ? 8 : 0, alongX ? 16 : 8, alongX ? 8 : 16, spool, player);
        return error == null ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }

    /**
     * Lay one supported run, then publish the debit to the supplied stack. A refused part
     * leaves the original stack untouched.
     */
    public static String place(ServerLevel level, BlockPos pos, int x0, int z0, int x1, int z1, ItemStack spool) {
        return place(level, pos, x0, z0, x1, z1, spool, null);
    }

    private static String place(ServerLevel level, BlockPos pos, int x0, int z0, int x1, int z1, ItemStack spool, Player player) {
        if (!valid(x0, z0, x1, z1)) return "invalid AWG endpoints";
        if (!level.hasChunkAt(pos) || !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) {
            return "AWG run needs a loaded block and a solid floor";
        }
        var part = new AwgCablePart(x0, z0, x1, z1);
        return lay(level, List.of(pos), List.of(part), part.geometry.length(), spool, player);
    }

    private static boolean canRouteIn(ServerLevel level, BlockPos pos, Player player) {
        if (level.isOutsideBuildHeight(pos) || !level.getWorldBorder().isWithinBounds(pos)
            || !level.hasChunkAt(pos) || !level.mayInteract(player, pos) || !level.mayInteract(player, pos.below())
            || !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) return false;
        var state = level.getBlockState(pos);
        // Only air and existing voxel nodes can be restored by the normal laying rollback.
        return state.isAir() || state.getBlock() instanceof VoxelNodeBlock;
    }

    private static String route(ServerLevel level, BlockPos start, BlockPos end, ItemStack spool, Player player) {
        long dx = (long) end.getX() - start.getX(), dz = (long) end.getZ() - start.getZ();
        if (start.getY() != end.getY()) return "AWG routing needs endpoints on the same floor level";
        long blocks = Math.abs(dx) + Math.abs(dz) + 1;
        if (blocks > MAX_ROUTE_BLOCKS) return "AWG route exceeds 64 blocks";
        long sx = 16L * start.getX() + 8, ex = 16L * end.getX() + 8;
        long sz = 16L * start.getZ() + 8, ez = 16L * end.getZ() + 8;
        long y = 16L * start.getY(), ox = Long.signum(dx) * 8L, oz = Long.signum(dz) * 8L;
        if (dx == 0 || dz == 0) {
            return laySteps(level, List.of(floorStep(sx - ox, y, sz - oz),
                floorStep(ex + ox, y, ez + oz)), (int) blocks * 16, spool, player);
        }
        var xFirst = List.of(floorStep(sx - ox, y, sz), floorStep(ex, y, sz), floorStep(ex, y, ez + oz));
        String result = laySteps(level, xFirst, (int) blocks * 16, spool, player);
        if (result == null) return null;
        var zFirst = List.of(floorStep(sx, y, sz - oz), floorStep(sx, y, ez), floorStep(ex + ox, y, ez));
        return laySteps(level, zFirst, (int) blocks * 16, spool, player);
    }

    private static CableRouter.Step floorStep(long x, long y, long z) {
        return new CableRouter.Step(x, y, z, 0, false);
    }

    private static String laySteps(ServerLevel level, List<CableRouter.Step> steps, int routeVoxels, ItemStack spool, Player player) {
        var positions = new java.util.ArrayList<BlockPos>();
        var parts = new java.util.ArrayList<AwgCablePart>();
        int measured = 0;
        for (var piece : CableRouter.pieces(steps, 2, 1)) {
            var pos = new BlockPos((int) piece.bx(), (int) piece.by(), (int) piece.bz());
            if (!canRouteIn(level, pos, player)) return "AWG route blocked or unsupported at " + pos.toShortString();
            var points = piece.points();
            for (int i = 3; i < points.length; i += 3) {
                var part = new AwgCablePart(points[i - 3], points[i - 1], points[i], points[i + 2]);
                positions.add(pos);
                parts.add(part);
                measured += part.geometry.length();
            }
        }
        if (measured != routeVoxels) return "AWG route length mismatch";
        return lay(level, positions, parts, measured, spool, player);
    }

    private static String lay(ServerLevel level, List<BlockPos> positions, List<AwgCablePart> parts,
                              int voxels, ItemStack spool, Player player) {
        boolean creative = player != null && player.isCreative();
        if (spool.getCount() != 1 || !spool.is(wire().parentItem)) return "invalid AWG spool";
        var copy = creative ? ItemStack.EMPTY : spool.copy();
        var length = creative ? AwgWireLength.creative(wire(), voxels) : AwgWireLength.purchase(wire(), copy, voxels);
        if (length == null) return "insufficient or invalid AWG spool";
        for (int i = 0; i < parts.size(); i++) {
            var part = parts.get(i);
            var coord = new mods.eln.misc.Coordonate(positions.get(i), level);
            part.saved = AwgCableRun.Companion.create(wire(), length.take(part.geometry.length()),
                new VoltageState(), new VoltageState(), Ambient.relative(coord) + 20).save();
        }
        var error = Laying.lay(level, positions, new java.util.ArrayList<VoxelPart>(parts), player);
        if (error == null && !creative) {
            ItemNbt.set(spool, ItemNbt.get(copy));
            spool.setCount(copy.getCount());
        }
        return error;
    }

    @Override public String kind() { return KIND; }
    @Override public List<int[]> boxes() { return geometry.boxes(); }

    boolean joins(AwgCablePart other) {
        // Collinear endpoints may meet, but never exempt overlapping collinear boxes.
        if ((x0 == x1) == (other.x0 == other.x1)) return false;
        return other.hasEnd(x0, z0) || other.hasEnd(x1, z1);
    }

    private boolean hasEnd(int x, int z) {
        return x == x0 && z == z0 || x == x1 && z == z1;
    }

    ElectricalLoad terminal(Direction face, int along) {
        int x = face == Direction.WEST ? 0 : face == Direction.EAST ? 16 : along;
        int z = face == Direction.NORTH ? 0 : face == Direction.SOUTH ? 16 : along;
        return x == x0 && z == z0 && from != null ? from.load : x == x1 && z == z1 && to != null ? to.load : null;
    }

    private Point point(BlockPos pos, int x, int z) {
        return new Point(16L * pos.getX() + x, pos.getY(), 16L * pos.getZ() + z);
    }

    private static Terminal acquire(Map<Point, Terminal> points, Point point) {
        var terminal = points.computeIfAbsent(point, key -> new Terminal());
        if (terminal.users++ == 0) {
            terminal.load.setRs(1e-6);
            Eln.simulator.addElectricalLoad(terminal.load);
        }
        return terminal;
    }

    private static void release(Map<Point, Terminal> points, Point point, Terminal terminal) {
        if (--terminal.users == 0) {
            Eln.simulator.removeElectricalLoad(terminal.load);
            points.remove(point);
        }
    }

    void attach(VoxelNode node) {
        var level = (ServerLevel) node.coordonate.world();
        var pos = node.coordonate.pos();
        var points = TERMINALS.computeIfAbsent(level, key -> new HashMap<>());
        from = acquire(points, point(pos, x0, z0));
        to = acquire(points, point(pos, x1, z1));
        run = AwgCableRun.Companion.load(wire(), saved, from.load, to.load);
        run.attach(Eln.simulator, () -> Ambient.relative(node.coordonate) + 20);
        saved = null;
    }

    void detach(VoxelNode node) {
        run.detach();
        saved = run.save();
        run = null;
        var level = (ServerLevel) node.coordonate.world();
        var pos = node.coordonate.pos();
        var points = TERMINALS.get(level);
        release(points, point(pos, x0, z0), from);
        release(points, point(pos, x1, z1), to);
        if (points.isEmpty()) TERMINALS.remove(level);
        from = to = null;
    }

    public static List<AwgCablePart> at(ServerLevel level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof VoxelNodeEntity e) || !(e.getNode() instanceof VoxelNode node)) return List.of();
        return node.parts.stream().filter(AwgCablePart.class::isInstance).map(AwgCablePart.class::cast).toList();
    }

    public AwgCableRun activeRun() { return run; }

    void nameTerminals(BlockPos pos, Map<Object, String> names) {
        names.put(from.load, "AWG " + point(pos, x0, z0));
        names.put(to.load, "AWG " + point(pos, x1, z1));
    }

    void liveLink(java.util.function.BiConsumer<ElectricalLoad, ElectricalLoad> link) {
        if (!run.getConductorMelted()) link.accept(from.load, to.load);
    }

    ItemStack recover() {
        if (saved == null) return ItemStack.EMPTY;
        var detached = AwgCableRun.Companion.load(wire(), saved, new VoltageState(), new VoltageState());
        saved = null;
        return detached.recover();
    }

    @Override public void write(CompoundTag tag) {
        tag.putIntArray("ends", new int[]{x0, z0, x1, z1});
        tag.put("run", run == null ? saved.copy() : run.save());
    }

    static AwgCablePart read(CompoundTag tag) {
        var ends = tag.getIntArray("ends").orElse(new int[0]);
        if (ends.length != 4 || !valid(ends[0], ends[1], ends[2], ends[3])) return null;
        var part = new AwgCablePart(ends[0], ends[1], ends[2], ends[3]);
        var state = tag.getCompoundOrEmpty("run");
        if (state.getCompoundOrEmpty("length").getIntOr("routeVoxels", -1) != part.geometry.length()) return null;
        try {
            AwgCableRun.Companion.load(wire(), state, new VoltageState(), new VoltageState());
        } catch (IllegalArgumentException | IllegalStateException | java.util.NoSuchElementException e) {
            return null;
        }
        part.saved = state.copy();
        return part;
    }

    @Override public void serialize(DataOutputStream out) throws IOException {
        out.writeByte(x0); out.writeByte(z0); out.writeByte(x1); out.writeByte(z1);
    }

    static AwgCablePart deserialize(DataInputStream in) throws IOException {
        try {
            return new AwgCablePart(in.readUnsignedByte(), in.readUnsignedByte(), in.readUnsignedByte(), in.readUnsignedByte());
        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid AWG part", e);
        }
    }

    CablePart visual() { return geometry; }
}
