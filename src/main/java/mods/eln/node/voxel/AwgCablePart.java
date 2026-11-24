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

/** Controlled floor- and wall-mounted straight runs of the existing 12 AWG spool. */
public final class AwgCablePart extends VoxelPart {
    static final String KIND = "awg_cable";
    private static final int WIRE_ID = 2200;
    private static final int MAX_ROUTE_BLOCKS = 64;
    private static final Map<Player, Selection> SELECTIONS = new WeakHashMap<>();

    private record Selection(ItemStack spool, ServerLevel level, BlockPos start, Direction mount, long expires) {}
    private static final Map<ServerLevel, Map<Point, Terminal>> TERMINALS = new WeakHashMap<>();

    private record Point(long x, long y, long z, int face) {}

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
        this(0, new int[]{x0, 0, z0, x1, 0, z1});
    }

    private AwgCablePart(int face, int[] points) {
        if (!valid(face, points)) throw new IllegalArgumentException("Invalid AWG run");
        x0 = points[0];
        z0 = points[2];
        x1 = points[3];
        z1 = points[5];
        geometry = new CablePart(1, new CableSpec(3, CableSpec.BLACK), face, points);
    }

    private static boolean valid(int face, int[] points) {
        if (face < 0 || face > 5 || face == Direction.UP.get3DDataValue() || points.length != 6) return false;
        int normal = CablePart.normalAxis(face);
        if (points[normal] != CablePart.plane(face) || points[normal + 3] != points[normal]) return false;
        int varying = 0;
        for (int axis = 0; axis < 3; axis++) {
            if (axis == normal) continue;
            int a = points[axis], b = points[axis + 3];
            if (a < 0 || a > 16 || b < 0 || b > 16) return false;
            if (a != b) varying++;
            else if (a == 0 || a == 16) return false;
        }
        return varying == 1;
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
                SELECTIONS.put(player, new Selection(spool, server, null, null, server.getGameTime() + CablePlacer.IDLE_TICKS));
                tell(player, "AWG route: sneak-right-click the first support face");
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

    public static void checkRoutingSelection(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player && SELECTIONS.containsKey(player)) {
            selection(player.level(), player, player.getMainHandItem());
        }
    }

    private static void tell(Player player, String message) {
        player.sendOverlayMessage(net.minecraft.network.chat.Component.literal(message));
    }

    /** Sneak-use on the floor lays one meter, or selects a route endpoint while in route mode. */
    public static InteractionResult useSpool(UseOnContext ctx) {
        Player player = ctx.getPlayer();
        Direction mount = ctx.getClickedFace().getOpposite();
        if (player == null || ctx.getHand() != InteractionHand.MAIN_HAND || mount == Direction.UP)
            return InteractionResult.FAIL;
        // Only the server lays the run or changes the spool.
        if (!(ctx.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        BlockPos floor = ctx.getClickedPos();
        ItemStack spool = ctx.getItemInHand();
        var selected = selection(level, player, spool);
        if (selected == null && mount != Direction.DOWN) return InteractionResult.FAIL;
        BlockPos pos = selected != null && mount == Direction.DOWN && hasCenterWire(level, floor)
            ? floor : floor.relative(ctx.getClickedFace());
        var hit = ctx.getClickLocation();
        if (player.isSpectator() || !player.getAbilities().mayBuild || player.getMainHandItem() != spool
            || !level.hasChunkAt(floor) || !level.mayInteract(player, floor) || !level.mayInteract(player, pos)
            || !new net.minecraft.world.phys.AABB(floor).inflate(0.01).contains(hit)
            || !(player.getEyePosition().distanceTo(hit) <= player.blockInteractionRange())) return InteractionResult.FAIL;

        if (selected != null) {
            if (!canRouteIn(level, pos, mount, player)) return InteractionResult.FAIL;
            if (selected.start() == null) {
                SELECTIONS.put(player, new Selection(spool, level, pos, mount, level.getGameTime() + CablePlacer.IDLE_TICKS));
                tell(player, "AWG route start set. Sneak-right-click the destination support face");
            } else if (selected.mount() != mount) {
                return InteractionResult.FAIL;
            } else if (!selected.start().equals(pos)) {
                String error = mount == Direction.DOWN ? route(level, selected.start(), pos, spool, player)
                    : wallRoute(level, selected.start(), pos, mount, spool, player, -1);
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
        if (!valid(0, new int[]{x0, 0, z0, x1, 0, z1})) return "invalid AWG endpoints";
        if (!level.hasChunkAt(pos) || !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) {
            return "AWG run needs a loaded block and a solid floor";
        }
        var part = new AwgCablePart(x0, z0, x1, z1);
        return lay(level, List.of(pos), List.of(part), part.geometry.length(), spool, player, -1, List.of());
    }

    private static boolean canRouteIn(ServerLevel level, BlockPos pos, Direction mount, Player player) {
        var support = pos.relative(mount);
        if (level.isOutsideBuildHeight(pos) || !level.getWorldBorder().isWithinBounds(pos)
            || !level.hasChunkAt(pos) || !level.hasChunkAt(support)
            || (player != null && (!level.mayInteract(player, pos) || !level.mayInteract(player, support)))
            || !level.getBlockState(support).isFaceSturdy(level, support, mount.getOpposite())) return false;
        var state = level.getBlockState(pos);
        // Only air and existing voxel nodes can be restored by the normal laying rollback.
        return state.isAir() || state.getBlock() instanceof VoxelNodeBlock;
    }

    private static String route(ServerLevel level, BlockPos start, BlockPos end, ItemStack spool, Player player) {
        return route(level, start, end, spool, player, -1);
    }

    /** Force an intermediate write failure through the same accounting and rollback path as player placement. */
    public static String routeFailingAtForTests(ServerLevel level, BlockPos start, BlockPos end, ItemStack spool, int failAt) {
        return route(level, start, end, spool, null, failAt);
    }

    private static String route(ServerLevel level, BlockPos start, BlockPos end, ItemStack spool, Player player, int failAt) {
        if (start.equals(end)) return "AWG route needs two distinct endpoints";
        long dx = (long) end.getX() - start.getX(), dz = (long) end.getZ() - start.getZ();
        if (start.getY() != end.getY()) return "AWG routing needs endpoints on the same floor level";
        long blocks = Math.abs(dx) + Math.abs(dz) + 1;
        if (blocks > MAX_ROUTE_BLOCKS) return "AWG route exceeds 64 blocks";
        long sx = 16L * start.getX() + 8, ex = 16L * end.getX() + 8;
        long sz = 16L * start.getZ() + 8, ez = 16L * end.getZ() + 8;
        long y = 16L * start.getY(), ox = Long.signum(dx) * 8L, oz = Long.signum(dz) * 8L;
        boolean tapStart = hasCenterWire(level, start), tapEnd = hasCenterWire(level, end);
        long head = tapStart ? 0 : 1, tail = tapEnd ? 0 : 1;
        int routeVoxels = (int) blocks * 16 - (tapStart ? 8 : 0) - (tapEnd ? 8 : 0);
        RouteLayout layout;
        if (dx == 0 || dz == 0) {
            layout = plan(level, List.of(floorStep(sx - head * ox, y, sz - head * oz),
                floorStep(ex + tail * ox, y, ez + tail * oz)), routeVoxels, player, start, end, tapStart, tapEnd);
        } else {
            layout = plan(level, List.of(floorStep(sx - head * ox, y, sz), floorStep(ex, y, sz),
                floorStep(ex, y, ez + tail * oz)), routeVoxels, player, start, end, tapStart, tapEnd);
            if (layout.error() != null) {
                layout = plan(level, List.of(floorStep(sx, y, sz - head * oz), floorStep(sx, y, ez),
                    floorStep(ex + tail * ox, y, ez)), routeVoxels, player, start, end, tapStart, tapEnd);
            }
        }
        if (layout.error() != null) return layout.error();
        return lay(level, layout.positions(), layout.parts(), routeVoxels, spool, player, failAt, layout.splits());
    }

    private static boolean hasCenterWire(ServerLevel level, BlockPos pos) {
        return level.hasChunkAt(pos) && at(level, pos).stream().anyMatch(AwgCablePart::passesCenter);
    }

    private boolean passesCenter() {
        if (geometry.face != 0) return false;
        return x0 == x1 ? x0 == 8 && Math.min(z0, z1) <= 8 && Math.max(z0, z1) >= 8
            : z0 == 8 && Math.min(x0, x1) <= 8 && Math.max(x0, x1) >= 8;
    }

    private record Split(VoxelNode node, AwgCablePart original, List<AwgCablePart> halves) {
        void apply() { node.replaceAwgParts(node.parts.indexOf(original), 1, halves); }
        void undo() { node.replaceAwgParts(node.parts.indexOf(halves.getFirst()), 2, List.of(original)); }
    }

    /** Validate one explicit center tap, with at most three non-overlapping incident arms. */
    private static String planTap(ServerLevel level, BlockPos pos, AwgCablePart arm, List<Split> splits) {
        var wires = at(level, pos).stream().filter(AwgCablePart::passesCenter).toList();
        if (wires.isEmpty() || wires.size() > 2 || !arm.hasEnd(8, 8)) return "AWG junction is occupied";
        for (var existing : wires) {
            var state = existing.run;
            if (state == null || state.getConductorMelted()) return "AWG junction needs live conductor";
            if (existing.hasEnd(8, 8)) continue;
            if (wires.size() != 1 || (existing.x0 == existing.x1) == (arm.x0 == arm.x1))
                return "AWG tap must approach the side of a straight run";
            var first = new AwgCablePart(existing.x0, existing.z0, 8, 8);
            var second = new AwgCablePart(8, 8, existing.x1, existing.z1);
            var detached = AwgCableRun.Companion.load(wire(), state.save(), new VoltageState(), new VoltageState());
            var split = detached.split(first.geometry.length(), new VoltageState());
            first.saved = split.getFirst().save();
            second.saved = split.getSecond().save();
            var node = (VoxelNode) ((VoxelNodeEntity) level.getBlockEntity(pos)).getNode();
            splits.add(new Split(node, existing, List.of(first, second)));
        }
        return null;
    }

    public static String wallRouteFailingAtForTests(ServerLevel level, BlockPos start, BlockPos end,
                                                    Direction mount, ItemStack spool, int failAt) {
        return wallRoute(level, start, end, mount, spool, null, failAt);
    }

    private static String wallRoute(ServerLevel level, BlockPos start, BlockPos end, Direction mount,
                                    ItemStack spool, Player player, int failAt) {
        int face = mount.get3DDataValue(), normal = CablePart.normalAxis(face);
        long[] delta = {(long) end.getX() - start.getX(), (long) end.getY() - start.getY(), (long) end.getZ() - start.getZ()};
        if (mount.getAxis().isVertical() || delta[normal] != 0) return "AWG wall endpoints must share one wall";
        int along = -1;
        for (int axis = 0; axis < 3; axis++) if (delta[axis] != 0) {
            if (along >= 0) return "AWG wall routes must be straight";
            along = axis;
        }
        if (along < 0 || Math.abs(delta[along]) >= MAX_ROUTE_BLOCKS) return "AWG route needs 2–64 wall blocks";
        long[] a = {16L * start.getX() + 8, 16L * start.getY() + 8, 16L * start.getZ() + 8};
        long[] b = {16L * end.getX() + 8, 16L * end.getY() + 8, 16L * end.getZ() + 8};
        a[normal] += CablePart.plane(face) - 8;
        b[normal] = a[normal];
        a[along] -= Long.signum(delta[along]) * 8;
        b[along] += Long.signum(delta[along]) * 8;
        var steps = List.of(new CableRouter.Step(a[0], a[1], a[2], face, false),
            new CableRouter.Step(b[0], b[1], b[2], face, false));
        var positions = new java.util.ArrayList<BlockPos>();
        var parts = new java.util.ArrayList<AwgCablePart>();
        int voxels = 0;
        for (var piece : CableRouter.pieces(steps, 2, 1)) {
            var pos = new BlockPos((int) piece.bx(), (int) piece.by(), (int) piece.bz());
            if (!canRouteIn(level, pos, mount, player)) return "AWG wall route blocked or unsupported at " + pos.toShortString();
            var part = new AwgCablePart(piece.face(), piece.points());
            positions.add(pos);
            parts.add(part);
            voxels += part.geometry.length();
        }
        if (voxels != (Math.abs(delta[along]) + 1) * 16) return "AWG wall route length mismatch";
        return lay(level, positions, parts, voxels, spool, player, failAt, List.of());
    }

    private static CableRouter.Step floorStep(long x, long y, long z) {
        return new CableRouter.Step(x, y, z, 0, false);
    }

    private record RouteLayout(List<BlockPos> positions, List<AwgCablePart> parts, List<Split> splits, String error) {
        static RouteLayout rejected(String error) {
            return new RouteLayout(List.of(), List.of(), List.of(), error);
        }
    }

    private static RouteLayout plan(ServerLevel level, List<CableRouter.Step> steps, int routeVoxels, Player player,
                                    BlockPos start, BlockPos end, boolean tapStart, boolean tapEnd) {
        var positions = new java.util.ArrayList<BlockPos>();
        var parts = new java.util.ArrayList<AwgCablePart>();
        int measured = 0;
        for (var piece : CableRouter.pieces(steps, 2, 1)) {
            var pos = new BlockPos((int) piece.bx(), (int) piece.by(), (int) piece.bz());
            if (!canRouteIn(level, pos, Direction.DOWN, player)) return RouteLayout.rejected("AWG route blocked or unsupported at " + pos.toShortString());
            if (!pos.equals(start) && !pos.equals(end) && hasCenterWire(level, pos))
                return RouteLayout.rejected("AWG route would cross existing wire at " + pos.toShortString());
            var points = piece.points();
            for (int i = 3; i < points.length; i += 3) {
                var part = new AwgCablePart(points[i - 3], points[i - 1], points[i], points[i + 2]);
                positions.add(pos);
                parts.add(part);
                measured += part.geometry.length();
            }
        }
        if (measured != routeVoxels) return RouteLayout.rejected("AWG route length mismatch");
        var splits = new java.util.ArrayList<Split>();
        String error = tapStart ? planTap(level, start, parts.getFirst(), splits) : null;
        if (error == null && tapEnd) error = planTap(level, end, parts.getLast(), splits);
        if (error != null) return RouteLayout.rejected(error);
        var replaced = new HashMap<BlockPos, AwgCablePart>();
        for (var split : splits) replaced.put(split.node().coordonate.pos(), split.original());
        return new RouteLayout(positions, parts, splits, Laying.check(level, positions, parts, replaced));
    }

    private static String lay(ServerLevel level, List<BlockPos> positions, List<AwgCablePart> parts,
                              int voxels, ItemStack spool, Player player, int failAt, List<Split> splits) {
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
        int applied = 0;
        boolean committed = false;
        try {
            for (var split : splits) {
                split.apply();
                applied++;
            }
            var error = Laying.lay(level, positions, parts, player, failAt);
            if (error != null) return error;
            if (!creative) {
                ItemNbt.set(spool, ItemNbt.get(copy));
                spool.setCount(copy.getCount());
            }
            committed = true;
            return null;
        } finally {
            if (!committed) for (int i = applied - 1; i >= 0; i--) splits.get(i).undo();
        }
    }

    @Override public String kind() { return KIND; }
    @Override public List<int[]> boxes() { return geometry.boxes(); }

    boolean joins(AwgCablePart other) {
        // Collinear endpoints may meet, but never exempt overlapping collinear boxes.
        if (geometry.face != 0 || other.geometry.face != 0 || (x0 == x1) == (other.x0 == other.x1)) return false;
        return other.hasEnd(x0, z0) || other.hasEnd(x1, z1);
    }

    private boolean hasEnd(int x, int z) {
        return x == x0 && z == z0 || x == x1 && z == z1;
    }

    ElectricalLoad terminal(Direction exit, Direction mount) {
        if (geometry.face != mount.get3DDataValue() || exit.getAxis() == mount.getAxis()) return null;
        int normal = CablePart.normalAxis(geometry.face), boundary = CablePart.normalAxis(exit.get3DDataValue());
        int across = 3 - normal - boundary;
        int plane = CablePart.plane(exit.get3DDataValue());
        var p = geometry.points;
        // The terminal must leave the block in the cable's direction, at the center of the legacy edge.
        if (p[boundary] == p[boundary + 3]) return null;
        if (from != null && p[boundary] == plane && p[across] == 8) return from.load;
        return to != null && p[boundary + 3] == plane && p[across + 3] == 8 ? to.load : null;
    }

    private Point point(BlockPos pos, int end) {
        int i = end * 3;
        var p = geometry.points;
        return new Point(16L * pos.getX() + p[i], 16L * pos.getY() + p[i + 1],
            16L * pos.getZ() + p[i + 2], geometry.face);
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
        from = acquire(points, point(pos, 0));
        to = acquire(points, point(pos, 1));
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
        release(points, point(pos, 0), from);
        release(points, point(pos, 1), to);
        if (points.isEmpty()) TERMINALS.remove(level);
        from = to = null;
    }

    public static List<AwgCablePart> at(ServerLevel level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof VoxelNodeEntity e) || !(e.getNode() instanceof VoxelNode node)) return List.of();
        return node.parts.stream().filter(AwgCablePart.class::isInstance).map(AwgCablePart.class::cast).toList();
    }

    public AwgCableRun activeRun() { return run; }

    void nameTerminals(BlockPos pos, Map<Object, String> names) {
        names.put(from.load, "AWG " + point(pos, 0));
        names.put(to.load, "AWG " + point(pos, 1));
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
        if (geometry.face == 0) tag.putIntArray("ends", new int[]{x0, z0, x1, z1});
        else {
            tag.putInt("face", geometry.face);
            tag.putIntArray("ends", geometry.points);
        }
        tag.put("run", run == null ? saved.copy() : run.save());
    }

    static AwgCablePart read(CompoundTag tag) {
        var ends = tag.getIntArray("ends").orElse(new int[0]);
        int face = tag.getIntOr("face", 0);
        if (face == 0 && ends.length == 4) ends = new int[]{ends[0], 0, ends[1], ends[2], 0, ends[3]};
        if (!valid(face, ends)) return null;
        var part = new AwgCablePart(face, ends);
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
        if (geometry.face == 0) {
            out.writeByte(x0); out.writeByte(z0); out.writeByte(x1); out.writeByte(z1);
        } else {
            out.writeByte(geometry.face);
            for (int coordinate : geometry.points) out.writeByte(coordinate);
        }
    }

    static AwgCablePart deserialize(DataInputStream in) throws IOException {
        try {
            if (in.available() == 4) {
                return new AwgCablePart(in.readUnsignedByte(), in.readUnsignedByte(), in.readUnsignedByte(), in.readUnsignedByte());
            }
            int face = in.readUnsignedByte();
            int[] coordinates = new int[6];
            for (int i = 0; i < coordinates.length; i++) coordinates[i] = in.readUnsignedByte();
            return new AwgCablePart(face, coordinates);
        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid AWG part", e);
        }
    }

    CablePart visual() { return geometry; }
}
