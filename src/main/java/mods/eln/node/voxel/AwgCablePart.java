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
import net.minecraft.world.phys.Vec3;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/** Controlled surface-mounted straight runs of the existing utility wire spools. */
public final class AwgCablePart extends VoxelPart {
    static final String KIND = "awg_cable";
    private static final int LEGACY_WIRE_ID = 2200;
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
    private final UtilityCableDescriptor wire;
    private CompoundTag saved;
    private AwgCableRun run;
    private Terminal from, to;
    private int cornerEnd = -1;
    private int cornerFace = -1;
    private int secondCornerFace = -1;
    private List<CablePart> nativeVisuals;
    private int outerEnds;
    private mods.eln.sim.mna.component.VoltageSource outerJoint;

    private AwgCablePart(UtilityCableDescriptor wire, int x0, int z0, int x1, int z1) {
        this(wire, 0, new int[]{x0, 0, z0, x1, 0, z1});
    }

    private AwgCablePart(UtilityCableDescriptor wire, int face, int[] points) {
        if (!valid(face, points)) throw new IllegalArgumentException("Invalid AWG run");
        x0 = points[0];
        z0 = points[2];
        x1 = face < 0 ? x0 : points[3];
        z1 = face < 0 ? z0 : points[5];
        this.wire = wire;
        geometry = CablePart.utilityGeometry(wire.name, face, points, width(wire), height(wire));
        if (!geometry.valid()) throw new IllegalArgumentException("AWG cross-section does not fit");
    }

    private static boolean valid(int face, int[] points) {
        if (face == -1) {
            if (points.length != 3) return false;
            int boundaries = 0;
            for (int value : points) {
                if (value < 0 || value > 16) return false;
                if (value == 0 || value == 16) boundaries++;
            }
            return boundaries == 2;
        }
        if (face < 0 || face > 5 || points.length != 6) return false;
        int normal = CablePart.normalAxis(face);
        if (points[normal] < 0 || points[normal] > 16 || points[normal + 3] != points[normal]) return false;
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

    static RouteKind routeKind(UtilityCableDescriptor wire) {
        return RouteKind.cable("awg", width(wire), height(wire));
    }

    static int width(UtilityCableDescriptor wire) {
        return Math.max(1, (int) Math.ceil(wire.render.width * 16));
    }

    static int height(UtilityCableDescriptor wire) {
        return Math.max(1, (int) Math.ceil(wire.render.height * 16));
    }

    private static UtilityCableDescriptor wire(int id) {
        var descriptor = Eln.sixNodeItem.getDescriptor(id);
        return supportsSpool(descriptor) ? (UtilityCableDescriptor) descriptor : null;
    }

    private static UtilityCableDescriptor wire(ItemStack spool) {
        return spool.getItem() instanceof mods.eln.node.six.SixNodeItem item && supportsSpool(item.descriptor)
            ? (UtilityCableDescriptor) item.descriptor : null;
    }

    public static boolean supportsSpool(mods.eln.node.six.SixNodeDescriptor descriptor) {
        return descriptor instanceof UtilityCableDescriptor wire && wire.getActsAsSingleConductor() && !wire.getMelted();
    }

    /** Sneak-use in air switches the current spool between one-block and route placement. */
    public static InteractionResult toggleRouting(net.minecraft.world.level.Level level, Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (level instanceof ServerLevel server && !player.isSpectator() && player.getAbilities().mayBuild) {
            var spool = player.getMainHandItem();
            if (selection(server, player, spool) == null) {
                SELECTIONS.put(player, new Selection(spool, server, null, null, server.getGameTime() + RoutePlacer.IDLE_TICKS));
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
        if (player == null || ctx.getHand() != InteractionHand.MAIN_HAND)
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
                SELECTIONS.put(player, new Selection(spool, level, pos, mount, level.getGameTime() + RoutePlacer.IDLE_TICKS));
                tell(player, "AWG route start set. Sneak-right-click the destination support face");
            } else if (!selected.start().equals(pos) || selected.mount() != mount) {
                String error;
                if (selected.mount() != mount)
                    error = transition(level, selected.start(), selected.mount(), pos, mount, spool, player, -1);
                else if (mount == Direction.DOWN) error = route(level, selected.start(), pos, spool, player);
                else error = surfaceRoute(level, selected.start(), pos, mount, spool, player, -1);
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
        var wire = wire(spool);
        if (wire == null) return "invalid AWG spool";
        var part = new AwgCablePart(wire, x0, z0, x1, z1);
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
        var wire = wire(spool);
        if (wire == null) return "invalid AWG spool";
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
            layout = plan(level, wire, List.of(floorStep(sx - head * ox, y, sz - head * oz),
                floorStep(ex + tail * ox, y, ez + tail * oz)), routeVoxels, player, start, end, tapStart, tapEnd);
        } else {
            layout = plan(level, wire, List.of(floorStep(sx - head * ox, y, sz), floorStep(ex, y, sz),
                floorStep(ex, y, ez + tail * oz)), routeVoxels, player, start, end, tapStart, tapEnd);
            if (layout.error() != null) {
                layout = plan(level, wire, List.of(floorStep(sx, y, sz - head * oz), floorStep(sx, y, ez),
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
        if (geometry.face != 0 || geometry.points[1] != 0) return false;
        return x0 == x1 ? x0 == 8 && Math.min(z0, z1) <= 8 && Math.max(z0, z1) >= 8
            : z0 == 8 && Math.min(x0, x1) <= 8 && Math.max(x0, x1) >= 8;
    }

    private record Split(VoxelNode node, AwgCablePart original, List<AwgCablePart> halves) {
        void apply() { node.replaceAwgParts(node.parts.indexOf(original), 1, halves); }
        void undo() { node.replaceAwgParts(node.parts.indexOf(halves.getFirst()), 2, List.of(original)); }
    }

    private static String planTap(ServerLevel level, BlockPos pos, AwgCablePart arm, List<Split> splits) {
        return planTap(level, pos, arm, new int[]{8, 0, 8}, splits);
    }

    /** Split a paid run at the shared native tap point, with at most three incident arms. */
    private static String planTap(ServerLevel level, BlockPos pos, AwgCablePart arm, int[] point, List<Split> splits) {
        int face = arm.geometry.face;
        var wires = at(level, pos).stream().filter(p -> p.geometry.face == face
            && p.geometry.passes(point[0], point[1], point[2])).toList();
        if (wires.isEmpty() || wires.size() > 2 || !arm.hasPointEnd(point)) return "AWG junction is occupied";
        for (var existing : wires) {
            if (existing.isCornerPort(point)) return "Tap a conductor arm away from the corner port";
            if (!existing.isStraightAt(point)) return "AWG taps need a straight section outside the sampled sag";
            if (!CablePlacer.supported(level, pos, existing.geometry)) return "AWG junction support changed";
            var state = existing.run;
            if (state == null || state.getConductorMelted()) return "AWG junction needs live conductor";
            if (existing.hasPointEnd(point)) continue;
            if (wires.size() != 1 || CableRouter.axis(existing.direction()) == CableRouter.axis(arm.direction()))
                return "AWG tap must approach the side of a straight run";
            int[] firstPoints = existing.geometry.points.clone();
            int[] secondPoints = existing.geometry.points.clone();
            System.arraycopy(point, 0, firstPoints, 3, 3);
            System.arraycopy(point, 0, secondPoints, 0, 3);
            var first = new AwgCablePart(existing.wire, face, firstPoints);
            var second = new AwgCablePart(existing.wire, face, secondPoints);
            existing.copyCornerOwnership(first, 0);
            existing.copyCornerOwnership(second, 1);
            if (existing.geometry.hang2 != null) {
                int splitAt = first.geometry.length() * 8;
                first.geometry.hang2 = hangSlice(existing.geometry.hang2, 0, splitAt);
                second.geometry.hang2 = hangSlice(existing.geometry.hang2, splitAt, existing.geometry.length() * 8);
            }
            var detached = AwgCableRun.Companion.load(existing.wire, state.save(), new VoltageState(), new VoltageState());
            var split = detached.split(first.geometry.length(), new VoltageState());
            first.saved = split.getFirst().save();
            second.saved = split.getSecond().save();
            var node = (VoxelNode) ((VoxelNodeEntity) level.getBlockEntity(pos)).getNode();
            splits.add(new Split(node, existing, List.of(first, second)));
        }
        return null;
    }

    public static String surfaceRouteFailingAtForTests(ServerLevel level, BlockPos start, BlockPos end,
                                                    Direction mount, ItemStack spool, int failAt) {
        return surfaceRoute(level, start, end, mount, spool, null, failAt);
    }

    private static String surfaceRoute(ServerLevel level, BlockPos start, BlockPos end, Direction mount,
                                    ItemStack spool, Player player, int failAt) {
        var wire = wire(spool);
        if (wire == null) return "invalid AWG spool";
        int face = mount.get3DDataValue(), normal = CablePart.normalAxis(face);
        long[] delta = {(long) end.getX() - start.getX(), (long) end.getY() - start.getY(), (long) end.getZ() - start.getZ()};
        if (mount == Direction.DOWN || delta[normal] != 0) return "AWG endpoints must share one support plane";
        int first = -1, second = -1;
        long blocks = 1;
        for (int axis = 0; axis < 3; axis++) if (delta[axis] != 0) {
            blocks += Math.abs(delta[axis]);
            if (first < 0) first = axis;
            else second = axis;
        }
        if (first < 0 || blocks > MAX_ROUTE_BLOCKS) return "AWG route needs 2–64 surface blocks";
        int voxels = (int) blocks * 16;
        long[] a = {16L * start.getX() + 8, 16L * start.getY() + 8, 16L * start.getZ() + 8};
        long[] b = {16L * end.getX() + 8, 16L * end.getY() + 8, 16L * end.getZ() + 8};
        a[normal] += CablePart.plane(face) - 8;
        b[normal] = a[normal];
        var layout = surfacePlan(level, wire, mount, surfaceSteps(a, b, face, first, second), voxels, player);
        if (layout.error() != null && second >= 0)
            layout = surfacePlan(level, wire, mount, surfaceSteps(a, b, face, second, first), voxels, player);
        if (layout.error() != null) return layout.error();
        return lay(level, layout.positions(), layout.parts(), voxels, spool, player, failAt, List.of());
    }

    public static String transitionFailingAtForTests(ServerLevel level, BlockPos start, Direction startMount, BlockPos end,
                                                     Direction endMount, ItemStack spool, int failAt) {
        return transition(level, start, startMount, end, endMount, spool, null, failAt);
    }

    private static String transition(ServerLevel level, BlockPos start, Direction startMount, BlockPos end,
                                     Direction endMount, ItemStack spool, Player player, int failAt) {
        var wire = wire(spool);
        if (wire == null) return "invalid AWG spool";
        boolean flatFirst = startMount.getAxis().isVertical();
        if (flatFirst == endMount.getAxis().isVertical()) return "AWG transition needs a flat support and a wall";
        BlockPos flatEnd = flatFirst ? start : end;
        BlockPos wall = flatFirst ? end : start;
        Direction flatMount = flatFirst ? startMount : endMount;
        Direction wallMount = flatFirst ? endMount : startMount;
        int flatFace = flatMount.get3DDataValue(), face = wallMount.get3DDataValue();
        int axis = CablePart.normalAxis(face), across = 2 - axis;
        int riseSign = flatMount == Direction.DOWN ? 1 : -1;
        long[] a = {16L * flatEnd.getX() + 8, 16L * flatEnd.getY() + CablePart.plane(flatFace), 16L * flatEnd.getZ() + 8};
        long[] b = {16L * wall.getX() + 8, 16L * wall.getY() + (riseSign > 0 ? 16 : 0), 16L * wall.getZ() + 8};
        int sign = wallMount.getAxisDirection().getStep();
        long horizontal = (b[axis] - a[axis]) * sign;
        long vertical = (b[1] - a[1]) * riseSign;
        if (a[across] != b[across] || horizontal < 0 || vertical <= 0)
            return "AWG run must approach the wall squarely and turn into the open space";
        if (horizontal + vertical + 16 > MAX_ROUTE_BLOCKS * 16L) return "AWG transition exceeds 64 meters";
        a[axis] -= sign * 8L;
        b[axis] += sign * 8L;
        var corner = b.clone();
        corner[1] = a[1];
        int flatVoxels = (int) Math.abs(corner[axis] - a[axis]);
        int wallVoxels = (int) vertical;
        var flat = surfacePlan(level, wire, flatMount, List.of(surfaceStep(a, flatFace), surfaceStep(corner, flatFace)), flatVoxels, player);
        if (flat.error() != null) return flat.error();
        var rise = surfacePlan(level, wire, wallMount, List.of(surfaceStep(corner, face), surfaceStep(b, face)), wallVoxels, player);
        if (rise.error() != null) return rise.error();
        flat.parts().getLast().markCorner(1, face);
        rise.parts().getFirst().markCorner(0, flatFace);
        var positions = new java.util.ArrayList<>(flat.positions());
        positions.addAll(rise.positions());
        var parts = new java.util.ArrayList<>(flat.parts());
        parts.addAll(rise.parts());
        return lay(level, positions, parts, flatVoxels + wallVoxels, spool, player, failAt, List.of());
    }

    private static List<CableRouter.Step> surfaceSteps(long[] a, long[] b, int face, int first, int second) {
        var head = a.clone();
        var tail = b.clone();
        int last = second < 0 ? first : second;
        head[first] -= Long.signum(b[first] - a[first]) * 8;
        tail[last] += Long.signum(b[last] - a[last]) * 8;
        var start = surfaceStep(head, face);
        var end = surfaceStep(tail, face);
        if (second < 0) return List.of(start, end);
        var bend = a.clone();
        bend[first] = b[first];
        return List.of(start, surfaceStep(bend, face), end);
    }

    private static CableRouter.Step surfaceStep(long[] p, int face) {
        return new CableRouter.Step(p[0], p[1], p[2], face, false);
    }

    private static RouteLayout surfacePlan(ServerLevel level, UtilityCableDescriptor wire, Direction mount, List<CableRouter.Step> steps,
                                        int routeVoxels, Player player) {
        var positions = new java.util.ArrayList<BlockPos>();
        var parts = new java.util.ArrayList<AwgCablePart>();
        int voxels = 0;
        for (var piece : CableRouter.pieces(steps, width(wire), height(wire))) {
            var pos = new BlockPos((int) piece.bx(), (int) piece.by(), (int) piece.bz());
            if (!canRouteIn(level, pos, mount, player))
                return RouteLayout.rejected("AWG surface route blocked or unsupported at " + pos.toShortString());
            var points = piece.points();
            for (int i = 3; i < points.length; i += 3) {
                var part = new AwgCablePart(wire, piece.face(), java.util.Arrays.copyOfRange(points, i - 3, i + 3));
                if (at(level, pos).stream().anyMatch(part::joins))
                    return RouteLayout.rejected("AWG surface route would tap existing wire");
                positions.add(pos);
                parts.add(part);
                voxels += part.geometry.length();
            }
        }
        if (voxels != routeVoxels) return RouteLayout.rejected("AWG surface route length mismatch");
        return new RouteLayout(positions, parts, List.of(), Laying.check(level, positions, parts));
    }

    private static CableRouter.Step floorStep(long x, long y, long z) {
        return new CableRouter.Step(x, y, z, 0, false);
    }

    record RouteLayout(List<BlockPos> positions, List<AwgCablePart> parts, List<Split> splits, String error) {
        static RouteLayout rejected(String error) {
            return new RouteLayout(List.of(), List.of(), List.of(), error);
        }
    }

    private static RouteLayout plan(ServerLevel level, UtilityCableDescriptor wire, List<CableRouter.Step> steps, int routeVoxels, Player player,
                                    BlockPos start, BlockPos end, boolean tapStart, boolean tapEnd) {
        var positions = new java.util.ArrayList<BlockPos>();
        var parts = new java.util.ArrayList<AwgCablePart>();
        int measured = 0;
        for (var piece : CableRouter.pieces(steps, width(wire), height(wire))) {
            var pos = new BlockPos((int) piece.bx(), (int) piece.by(), (int) piece.bz());
            if (!canRouteIn(level, pos, Direction.DOWN, player)) return RouteLayout.rejected("AWG route blocked or unsupported at " + pos.toShortString());
            if (!pos.equals(start) && !pos.equals(end) && hasCenterWire(level, pos))
                return RouteLayout.rejected("AWG route would cross existing wire at " + pos.toShortString());
            var points = piece.points();
            for (int i = 3; i < points.length; i += 3) {
                var part = new AwgCablePart(wire, points[i - 3], points[i - 1], points[i], points[i + 2]);
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

    CableRouter.End tapEnd(ServerLevel level, BlockPos pos, Vec3 hit, long[][] join) {
        if (geometry.isKnuckle() || run == null || run.getConductorMelted() || !CablePlacer.supported(level, pos, geometry)) return null;
        int[] local = CablePlacer.nearestTap(geometry, hit.x - pos.getX(), hit.y - pos.getY(), hit.z - pos.getZ());
        if (local == null || isCornerPort(local) || !isStraightAt(local)) return null;
        long[] point = {pos.getX() * 16L + local[0], pos.getY() * 16L + local[1], pos.getZ() * 16L + local[2]};
        join[0] = point;
        return new CableRouter.End(point[0], point[1], point[2], geometry.face, -1);
    }

    private boolean isCornerPort(int[] point) {
        for (int end = 0; end < 2; end++)
            if ((cornerAt(end) >= 0 || (outerEnds & (1 << end)) != 0)
                && sameEndpoint(geometry.points, end * 3, point, 0)) return true;
        return false;
    }

    /** The child retaining the old physical endpoint retains that endpoint's connection namespace. */
    private void copyCornerOwnership(AwgCablePart child, int oldEnd) {
        if (cornerAt(oldEnd) >= 0) child.markCorner(oldEnd, cornerAt(oldEnd));
        child.setOuterEnds(outerEnds & (1 << oldEnd));
    }

    private boolean isStraightAt(int[] point) {
        if (geometry.hang2 == null) return true;
        var start = geometry.points;
        int at = (Math.abs(start[0] - point[0]) + Math.abs(start[1] - point[1])
            + Math.abs(start[2] - point[2])) * 8;
        int[] samples = geometry.hang2;
        for (int i = 0; i < samples.length; i += 3 + 9 * samples[i + 2])
            if (samples[i] <= at && at <= samples[i + 1]) return false;
        return true;
    }

    /** Keep complete native sample ranges on one side of a straight split and rebase their arc offsets. */
    private static int[] hangSlice(int[] samples, int from, int to) {
        int[] slice = new int[samples.length];
        int used = 0;
        for (int i = 0; i < samples.length;) {
            int size = 3 + 9 * samples[i + 2];
            if (samples[i] >= from && samples[i + 1] <= to) {
                System.arraycopy(samples, i, slice, used, size);
                slice[used] -= from;
                slice[used + 1] -= from;
                used += size;
            }
            i += size;
        }
        return used == 0 ? null : java.util.Arrays.copyOf(slice, used);
    }

    private static int[] localTap(BlockPos pos, long[] point) {
        return new int[]{Math.toIntExact(point[0] - pos.getX() * 16L),
            Math.toIntExact(point[1] - pos.getY() * 16L), Math.toIntExact(point[2] - pos.getZ() * 16L)};
    }

    /**
     * Recheck the cached centerline against the live world. At present an air run must be
     * straight and anchored on both ends. Use the same support and clearance probes as Router.
     */
    private static String checkNativeSpans(CableRouter.Space space, RouteKind kind, List<CableRouter.Step> steps) {
        int lattice = kind.lattice();
        int air = 0, previousDirection = -1;
        for (int i = 1; i < steps.size(); i++) {
            var a = steps.get(i - 1);
            var b = steps.get(i);
            int direction = CableRouter.dirOf(a, b);
            if (direction < 0) {
                if (b.outer()) {
                    int turn = CableRouter.reverse(b.attach());
                    long[] point = {b.x(), b.y(), b.z()};
                    if (CableRouter.axis(a.attach()) == CableRouter.axis(turn)
                        || Router.solidBeyond(space, kind, point, a.attach(), turn, true)
                        || !Router.solidBeyond(space, kind, point, a.attach(), CableRouter.reverse(turn), true)
                        || Router.knuckleBlocked(space, kind, point, a.attach(), turn))
                        return "AWG outer corner support or clearance changed";
                }
                if (air != 0) return "AWG spans need a supported landing before turning";
                previousDirection = -1;
                continue;
            }
            if (a.attach() != b.attach()) return "AWG face changes need an explicit corner";
            long[] p = {a.x(), a.y(), a.z()};
            long length = Math.abs(a.x() - b.x()) + Math.abs(a.y() - b.y()) + Math.abs(a.z() - b.z());
            if (length % lattice != 0) return "AWG span is off the routing grid";
            int axis = CableRouter.axis(direction);
            for (long traveled = 0; traveled < length; traveled += lattice) {
                boolean supported = Router.surfaceSupported(space, kind, p, a.attach(), direction);
                if (air > 0 && direction != previousDirection)
                    return "AWG spans must stay straight until they land";
                if (!supported) {
                    if (previousDirection != direction) return "AWG spans need a straight supported takeoff";
                    air += lattice;
                    if (kind.airCost(air) < 0) return "AWG span exceeds the native air allowance";
                } else air = 0;
                if (Router.surfaceStepBlocked(space, kind, p, a.attach(), direction, !supported))
                    return "AWG route clearance changed";
                previousDirection = direction;
                p[axis] += CableRouter.DV[direction][axis] * lattice;
            }
        }
        return air == 0 ? null : "AWG spans need a supported landing";
    }

    private static boolean canHostNative(ServerLevel level, BlockPos pos, Direction mount, Player player) {
        if (!level.hasChunkAt(pos) || level.isOutsideBuildHeight(pos) || !level.getWorldBorder().isWithinBounds(pos)
            || player != null && !level.mayInteract(player, pos)) return false;
        if (mount != null) {
            var support = pos.relative(mount);
            if (!level.hasChunkAt(support) || player != null && !level.mayInteract(player, support)) return false;
        }
        var state = level.getBlockState(pos);
        return state.isAir() || state.getBlock() instanceof VoxelNodeBlock || Mimic.allowed(state);
    }

    /** Translate the native route once for both preview and placement. Unsupported geometry has no paid edge. */
    static RouteLayout nativeLayout(ServerLevel level, UtilityCableDescriptor wire, CableRouter.Result route,
                                    long[] startJoin, long[] endJoin, Player player) {
        // The shared router bounds search work; the live spool bounds paid length.
        // MAX_ROUTE_BLOCKS belongs only to the older direct placement helpers.
        if (route.length() <= 0)
            return RouteLayout.rejected("AWG routes need a positive conductor length");
        var space = CablePlacer.space(level);
        if (startJoin != null) space = CablePlacer.around(space, startJoin);
        if (endJoin != null) space = CablePlacer.around(space, endJoin);
        var kind = routeKind(wire);
        var invalid = checkNativeSpans(space, kind, route.steps);
        if (invalid != null) return RouteLayout.rejected(invalid);
        var positions = new java.util.ArrayList<BlockPos>();
        var parts = new java.util.ArrayList<AwgCablePart>();
        int voxels = 0;
        for (var piece : CableRouter.centerlinePieces(route.steps, kind.width(), kind.height())) {
            var pos = new BlockPos((int) piece.bx(), (int) piece.by(), (int) piece.bz());
            if (!canHostNative(level, pos, piece.face() < 0 ? null : Direction.from3DDataValue(piece.face()), player))
                return RouteLayout.rejected("AWG wire needs a clear, loaded block at " + pos.toShortString());
            var points = piece.points();
            if (piece.face() < 0) {
                if (!valid(-1, points)) return RouteLayout.rejected("Invalid AWG knuckle geometry");
                positions.add(pos);
                parts.add(new AwgCablePart(wire, -1, points));
                continue;
            }
            for (int i = 3; i < points.length; i += 3) {
                int[] ends = java.util.Arrays.copyOfRange(points, i - 3, i + 3);
                AwgCablePart part;
                try {
                    part = new AwgCablePart(wire, piece.face(), ends);
                } catch (IllegalArgumentException e) {
                    return RouteLayout.rejected("AWG wire needs a clear planar edge");
                }
                if (!parts.isEmpty()) {
                    var previous = parts.getLast();
                    if (previous.geometry.face >= 0 && previous.geometry.face != part.geometry.face) {
                        if (!sameWorldEndpoint(previous, positions.getLast(), 1, part, pos, 0)
                            || !previous.canMarkCorner(1, part.geometry.face)
                            || !part.canMarkCorner(0, previous.geometry.face))
                            return RouteLayout.rejected("AWG face changes need a supported inner corner");
                        previous.markCorner(1, part.geometry.face);
                        part.markCorner(0, previous.geometry.face);
                    }
                }
                positions.add(pos);
                parts.add(part);
                voxels += part.geometry.length();
            }
        }
        if (parts.isEmpty() || voxels != route.length())
            return RouteLayout.rejected("AWG wire cannot follow decorative extensions");
        long knuckles = parts.stream().filter(p -> p.geometry.isKnuckle()).count();
        if (knuckles != CableRouter.pieces(route.steps, kind.width(), kind.height()).stream().filter(p -> p.face() < 0).count())
            return RouteLayout.rejected("AWG corner geometry mismatch");
        var visualError = bindNativeVisuals(route.steps, kind, positions, parts);
        if (visualError != null) return RouteLayout.rejected(visualError);
        for (int i = 0; i < parts.size(); i++) {
            var part = parts.get(i);
            if (!part.geometry.isKnuckle()) continue;
            if (i == 0 || i == parts.size() - 1
                || !part.bindOuter(positions.get(i), parts.get(i - 1), positions.get(i - 1), 1)
                || !part.bindOuter(positions.get(i), parts.get(i + 1), positions.get(i + 1), 0))
                return RouteLayout.rejected("AWG knuckle needs two matching paid conductor ends");
        }
        var splits = new java.util.ArrayList<Split>();
        String error = startJoin == null ? null : planTap(level, positions.getFirst(), parts.getFirst(), localTap(positions.getFirst(), startJoin), splits);
        if (error == null && endJoin != null) {
            if (startJoin != null && positions.getFirst().equals(positions.getLast()))
                return RouteLayout.rejected("AWG wire cannot tap the same block twice");
            error = planTap(level, positions.getLast(), parts.getLast(), localTap(positions.getLast(), endJoin), splits);
        }
        if (error != null) return RouteLayout.rejected(error);
        for (int i = 0; i < parts.size(); i++) {
            if ((i == 0 && startJoin != null) || (i == parts.size() - 1 && endJoin != null)) continue;
            if (at(level, positions.get(i)).stream().anyMatch(parts.get(i)::joins))
                return RouteLayout.rejected("Select the existing AWG wire as a route endpoint to tap it");
        }
        var replaced = new HashMap<BlockPos, AwgCablePart>();
        for (var split : splits) replaced.put(split.node().coordonate.pos(), split.original());
        return new RouteLayout(positions, parts, splits, Laying.check(level, positions, parts, replaced));
    }

    private boolean bindOuter(BlockPos pos, AwgCablePart arm, BlockPos armPos, int end) {
        if (arm.geometry.isKnuckle() || arm.cornerAt(end) >= 0) return false;
        int exit = arm.outerExit(end);
        if (exit < 0 || !armPos.relative(Direction.from3DDataValue(exit)).equals(pos)) return false;
        Point endpoint = arm.point(armPos, end, true);
        if (!endpoint.equals(point(pos, 0)) && !endpoint.equals(point(pos, 1))) return false;
        arm.outerEnds |= 1 << end;
        return true;
    }

    private int outerExit(int end) {
        for (var port : geometry.cablePorts())
            if (sameEndpoint(geometry.points, end * 3, new int[]{port.x(), port.y(), port.z()}, 0)) return port.face();
        return -1;
    }

    static String layNative(ServerLevel level, KitSupply kit, CableRouter.Result route, long[] startJoin, long[] endJoin,
                            Player player, int failAt) {
        var layout = nativeLayout(level, kit.wire(), route, startJoin, endJoin, player);
        if (layout.error() != null) return layout.error();
        var space = CablePlacer.space(level);
        if (startJoin != null) space = CablePlacer.around(space, startJoin);
        if (endJoin != null) space = CablePlacer.around(space, endJoin);
        var hung = CableHang.laid(space, routeKind(kit.wire()), route.steps);
        if (!hung.hangs().isEmpty()) {
            for (int i = 0; i < layout.parts().size(); i++) {
                var pos = layout.positions().get(i);
                var part = layout.parts().get(i);
                var piece = new CableRouter.Piece(pos.getX(), pos.getY(), pos.getZ(),
                    part.geometry.face, part.geometry.points);
                var samples = CableHang.encode(piece, hung.route(), hung.hangs());
                if (samples != null) part.setHang(samples);
            }
        }
        return lay(level, layout.positions(), layout.parts(), route.length(), kit.spool(), player, failAt, layout.splits());
    }

    private static String lay(ServerLevel level, List<BlockPos> positions, List<AwgCablePart> parts,
                              int voxels, ItemStack spool, Player player, int failAt, List<Split> splits) {
        boolean creative = player != null && player.isCreative();
        var wire = parts.getFirst().wire;
        if (spool.getCount() != 1 || !spool.is(wire.parentItem)) return "invalid AWG spool";
        var copy = creative ? ItemStack.EMPTY : spool.copy();
        var length = creative ? AwgWireLength.creative(wire, voxels) : AwgWireLength.purchase(wire, copy, voxels);
        if (length == null) return "insufficient or invalid AWG spool";
        for (int i = 0; i < parts.size(); i++) {
            var part = parts.get(i);
            if (part.geometry.isKnuckle()) continue;
            var coord = new mods.eln.misc.Coordonate(positions.get(i), level);
            part.saved = AwgCableRun.Companion.create(wire, length.take(part.geometry.length()),
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
    @Override public List<int[]> boxes() {
        return visuals().stream().flatMap(p -> p.boxes().stream()).toList();
    }

    List<CablePart> visuals() { return nativeVisuals == null ? List.of(geometry) : nativeVisuals; }

    private static boolean sameWorldEndpoint(AwgCablePart a, BlockPos pa, int ea, AwgCablePart b, BlockPos pb, int eb) {
        for (int axis = 0; axis < 3; axis++) {
            int[] ba = {pa.getX(), pa.getY(), pa.getZ()}, bb = {pb.getX(), pb.getY(), pb.getZ()};
            if (ba[axis] * 16L + a.geometry.points[ea * 3 + axis] != bb[axis] * 16L + b.geometry.points[eb * 3 + axis]) return false;
        }
        return true;
    }

    /** Assign the native converter's visual segments to paid edges in the same host; visuals own no wire or loads. */
    private static String bindNativeVisuals(List<CableRouter.Step> steps, RouteKind kind,
                                            List<BlockPos> positions, List<AwgCablePart> parts) {
        var assigned = new java.util.ArrayList<java.util.ArrayList<CablePart>>();
        for (var p : parts) assigned.add(new java.util.ArrayList<>());
        for (var piece : CableRouter.pieces(steps, kind.width(), kind.height())) {
            var pos = new BlockPos((int) piece.bx(), (int) piece.by(), (int) piece.bz());
            if (piece.face() < 0) continue;
            for (int j = 3; j < piece.points().length; j += 3) {
                int[] line = java.util.Arrays.copyOfRange(piece.points(), j - 3, j + 3);
                int best = -1, score = -1;
                for (int i = 0; i < parts.size(); i++) {
                    var p = parts.get(i);
                    if (!positions.get(i).equals(pos) || p.geometry.isKnuckle()) continue;
                    int overlap = visualOverlap(p.geometry, piece.face(), line);
                    if (overlap > score) { best = i; score = overlap; }
                }
                if (best < 0) return "Native decorative piece has no paid owner";
                var owner = parts.get(best);
                var visual = CablePart.utilityGeometry(owner.wire.name, piece.face(), line, kind.width(), kind.height());
                if (!visual.valid()) return "Invalid native decorative geometry";
                assigned.get(best).add(visual);
            }
        }
        for (int i = 0; i < parts.size(); i++) {
            var p = parts.get(i);
            if (p.geometry.isKnuckle()) continue;
            var visual = assigned.get(i);
            if (visual.isEmpty()) return "Native paid edge has no visual geometry";
            if (visual.size() != 1 || visual.getFirst().face != p.geometry.face
                || !java.util.Arrays.equals(visual.getFirst().points, p.geometry.points)) p.nativeVisuals = List.copyOf(visual);
        }
        return null;
    }

    private static int visualOverlap(CablePart paid, int face, int[] visual) {
        int[] p = paid.points;
        int axis = -1;
        for (int a = 0; a < 3; a++) if (p[a] != p[a + 3]) axis = a;
        if (face == paid.face && axis >= 0) {
            boolean aligned = true;
            for (int a = 0; a < 3; a++) if (a != axis && (p[a] != visual[a] || p[a] != visual[a + 3])) aligned = false;
            if (aligned) {
                int overlap = Math.min(Math.max(p[axis], p[axis + 3]), Math.max(visual[axis], visual[axis + 3]))
                    - Math.max(Math.min(p[axis], p[axis + 3]), Math.min(visual[axis], visual[axis + 3]));
                if (overlap > 0) return overlap;
            }
        }
        for (int i = 0; i < 6; i += 3) for (int j = 0; j < 6; j += 3)
            if (sameEndpoint(p, i, visual, j)) return 0;
        return -1;
    }

    boolean joins(AwgCablePart other) {
        if (geometry.isKnuckle() || other.geometry.isKnuckle()) return false;
        if (geometry.face != other.geometry.face) {
            for (int i = 0; i < 2; i++) for (int j = 0; j < 2; j++)
                if (cornerAt(i) == other.geometry.face && other.cornerAt(j) == geometry.face
                    && sameEndpoint(geometry.points, i * 3, other.geometry.points, j * 3)) return true;
            return false;
        }
        if (CableRouter.axis(direction()) == CableRouter.axis(other.direction())) return false;
        int[] a = geometry.points, b = other.geometry.points;
        for (int i = 0; i < 6; i += 3)
            for (int j = 0; j < 6; j += 3)
                if (sameEndpoint(a, i, b, j)) return true;
        return false;
    }

    private static boolean sameEndpoint(int[] a, int i, int[] b, int j) {
        return a[i] == b[j] && a[i + 1] == b[j + 1] && a[i + 2] == b[j + 2];
    }

    private boolean canMarkCorner(int end, int otherFace) {
        return end >= 0 && end <= 1 && otherFace >= 0 && otherFace <= 5
            && CablePart.normalAxis(otherFace) != CablePart.normalAxis(geometry.face);
    }

    private void markCorner(int end, int otherFace) {
        if (!canMarkCorner(end, otherFace)) throw new IllegalArgumentException("Invalid AWG corner");
        if (cornerEnd >= 0 && cornerEnd != end) secondCornerFace = otherFace;
        else { cornerEnd = end; cornerFace = otherFace; }
    }

    private int cornerAt(int end) { return end == cornerEnd ? cornerFace : secondCornerFace; }

    private int direction() {
        var p = geometry.points;
        if (p[0] != p[3]) return 4;
        return p[1] != p[4] ? 0 : 2;
    }

    private boolean hasPointEnd(int[] point) {
        return !geometry.isKnuckle()
            && (sameEndpoint(geometry.points, 0, point, 0) || sameEndpoint(geometry.points, 3, point, 0));
    }

    record Contact(int x, int y, int z, int out, int mount, ElectricalLoad load) {
        boolean at(int[] p) { return x == p[0] && y == p[1] && z == p[2]; }
    }

    /** Explicit conductor ends, independent of legacy centered edge addressing. */
    List<Contact> contacts() {
        if (geometry.isKnuckle() || from == null) return List.of();
        var contacts = new java.util.ArrayList<Contact>();
        int d = CableRouter.dirOf(new long[]{geometry.points[0], geometry.points[1], geometry.points[2]},
            new long[]{geometry.points[3], geometry.points[4], geometry.points[5]});
        for (int end = 0; end < 2; end++) {
            if (cornerAt(end) >= 0 || (outerEnds & (1 << end)) != 0) continue;
            int i = end * 3;
            contacts.add(new Contact(geometry.points[i], geometry.points[i + 1], geometry.points[i + 2],
                end == 0 ? CableRouter.reverse(d) : d, geometry.face, end == 0 ? from.load : to.load));
        }
        return contacts;
    }

    ElectricalLoad terminal(Direction exit, Direction mount) {
        if (geometry.face != mount.get3DDataValue() || exit.getAxis() == mount.getAxis()
            || geometry.points[CablePart.normalAxis(geometry.face)] != CablePart.plane(geometry.face)) return null;
        int normal = CablePart.normalAxis(geometry.face), boundary = CablePart.normalAxis(exit.get3DDataValue());
        int across = 3 - normal - boundary;
        int plane = CablePart.plane(exit.get3DDataValue());
        var p = geometry.points;
        // The terminal must leave the block in the cable's direction, at the center of the legacy edge.
        if (p[boundary] == p[boundary + 3]) return null;
        if ((outerEnds & 1) == 0 && cornerAt(0) < 0 && from != null && p[boundary] == plane && p[across] == 8) return from.load;
        return (outerEnds & 2) == 0 && cornerAt(1) < 0 && to != null && p[boundary + 3] == plane && p[across + 3] == 8 ? to.load : null;
    }

    private Point point(BlockPos pos, int end) {
        return point(pos, end, (outerEnds & (1 << end)) != 0);
    }

    private Point point(BlockPos pos, int end, boolean outer) {
        var p = geometry.points;
        int i = geometry.isKnuckle() ? 0 : end * 3;
        int channel = cornerAt(end) >= 0 ? 8 + ((1 << geometry.face) | (1 << cornerAt(end))) : geometry.face;
        if (geometry.isKnuckle()) {
            var ports = geometry.cablePorts();
            channel = outerChannel(ports.get(1 - end).face(), CableRouter.reverse(ports.get(end).face()));
        } else if (outer) {
            channel = outerChannel(geometry.face, outerExit(end));
        }
        return new Point(16L * pos.getX() + p[i], 16L * pos.getY() + p[i + 1], 16L * pos.getZ() + p[i + 2], channel);
    }

    private static int outerChannel(int face, int exit) {
        return 64 + 6 * face + exit;
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
        if (geometry.isKnuckle()) {
            outerJoint = new mods.eln.sim.mna.component.VoltageSource("AWG knuckle", from.load, to.load).setU(0);
            Eln.simulator.addElectricalComponent(outerJoint);
            return;
        }
        run = AwgCableRun.Companion.load(wire, saved, from.load, to.load);
        run.attach(Eln.simulator, () -> Ambient.relative(node.coordonate) + 20);
        saved = null;
    }

    void detach(VoxelNode node) {
        if (outerJoint != null) {
            Eln.simulator.removeElectricalComponent(outerJoint);
            outerJoint.breakConnection();
            outerJoint = null;
        } else {
            run.detach();
            saved = run.save();
            run = null;
        }
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
        if (outerJoint != null || !run.getConductorMelted()) link.accept(from.load, to.load);
    }

    ItemStack recover() {
        if (saved == null) return ItemStack.EMPTY;
        var detached = AwgCableRun.Companion.load(wire, saved, new VoltageState(), new VoltageState());
        saved = null;
        return detached.recover();
    }

    @Override public void write(CompoundTag tag) {
        if (geometry.isKnuckle()) {
            tag.putInt("face", -1);
            tag.putInt("wire", wire.parentItemDamage);
            tag.putIntArray("ends", geometry.points);
            return;
        }
        if (outerEnds != 0) tag.putInt("outerEnds", outerEnds);
        if (geometry.face == 0 && geometry.points[1] == 0) tag.putIntArray("ends", new int[]{x0, z0, x1, z1});
        else {
            tag.putInt("face", geometry.face);
            tag.putIntArray("ends", geometry.points);
        }
        if (cornerEnd >= 0) {
            tag.putInt("cornerEnd", cornerEnd);
            tag.putInt("cornerFace", cornerFace);
        }
        if (secondCornerFace >= 0) tag.putInt("secondCornerFace", secondCornerFace);
        if (nativeVisuals != null) {
            int[] visual = new int[nativeVisuals.size() * 7];
            for (int i = 0; i < nativeVisuals.size(); i++) {
                visual[i * 7] = nativeVisuals.get(i).face;
                System.arraycopy(nativeVisuals.get(i).points, 0, visual, i * 7 + 1, 6);
            }
            tag.putIntArray("nativeVisuals", visual);
        }
        if (geometry.hang2 != null) tag.putIntArray("hang2", geometry.hang2);
        tag.put("run", run == null ? saved.copy() : run.save());
    }

    static AwgCablePart read(CompoundTag tag) {
        var ends = tag.getIntArray("ends").orElse(new int[0]);
        int face = tag.getIntOr("face", 0);
        if (face == 0 && ends.length == 4) ends = new int[]{ends[0], 0, ends[1], ends[2], 0, ends[3]};
        if (!valid(face, ends)) return null;
        if (face == -1) {
            var descriptor = wire(tag.getIntOr("wire", -1));
            return descriptor == null || tag.contains("run") ? null : new AwgCablePart(descriptor, face, ends);
        }
        var state = tag.getCompoundOrEmpty("run");
        var wire = wire(state.getCompoundOrEmpty("length").getIntOr("wire", -1));
        if (wire == null) return null;
        AwgCablePart part;
        try {
            part = new AwgCablePart(wire, face, ends);
            if (state.getCompoundOrEmpty("length").getIntOr("routeVoxels", -1) != part.geometry.length()) return null;
            if (tag.contains("cornerEnd") || tag.contains("cornerFace"))
                part.markCorner(tag.getIntOr("cornerEnd", -1), tag.getIntOr("cornerFace", -1));
            if (tag.contains("secondCornerFace")) {
                if (part.cornerEnd < 0) return null;
                part.markCorner(1 - part.cornerEnd, tag.getIntOr("secondCornerFace", -1));
            }
            if (tag.contains("nativeVisuals")) part.setNativeVisuals(tag.getIntArray("nativeVisuals").orElseThrow());
            part.setOuterEnds(tag.getIntOr("outerEnds", 0));
            if (tag.contains("hang2"))
                part.setHang(tag.getIntArray("hang2").orElseThrow());
            AwgCableRun.Companion.load(wire, state, new VoltageState(), new VoltageState());
        } catch (IllegalArgumentException | IllegalStateException | java.util.NoSuchElementException e) {
            return null;
        }
        part.saved = state.copy();
        return part;
    }

    @Override public void serialize(DataOutputStream out) throws IOException {
        if (nativeVisuals == null && secondCornerFace < 0) { serializeBase(out); return; }
        var bytes = new java.io.ByteArrayOutputStream();
        serializeBase(new DataOutputStream(bytes));
        out.writeByte(0x9e);
        out.writeShort(bytes.size());
        out.write(bytes.toByteArray());
        out.writeByte(secondCornerFace + 1);
        out.writeByte(nativeVisuals == null ? 0 : nativeVisuals.size());
        if (nativeVisuals != null) for (var visual : nativeVisuals) {
            out.writeByte(visual.face);
            for (int v : visual.points) out.writeByte(v);
        }
    }

    private void serializeBase(DataOutputStream out) throws IOException {
        if (geometry.isKnuckle()) {
            out.writeByte(0x9f);
            out.writeShort(wire.parentItemDamage);
            for (int value : geometry.points) out.writeByte(value);
            return;
        }
        boolean hung = geometry.hang2 != null;
        out.writeByte(0x80 | (hung ? 0x40 : 0) | (outerEnds != 0 ? 0x20 : 0) | geometry.face);
        out.writeShort(wire.parentItemDamage);
        for (int coordinate : geometry.points) out.writeByte(coordinate);
        if (outerEnds != 0) out.writeByte(outerEnds);
        if (hung) {
            out.writeByte(cornerEnd + 1);
            if (cornerEnd >= 0) out.writeByte(cornerFace);
            out.writeShort(geometry.hang2.length);
            for (int v : geometry.hang2) out.writeShort(v);
        } else if (cornerEnd >= 0) {
            out.writeByte(cornerEnd);
            out.writeByte(cornerFace);
        }
    }

    /** Bounds-check the native sampled mesh before saving or rendering it. */
    private void setHang(int[] values) {
        if (values.length < 21 || values.length > 2048) throw new IllegalArgumentException("Invalid AWG span mesh");
        int last = 0;
        for (int i = 0; i < values.length;) {
            if (values.length - i < 3) throw new IllegalArgumentException("Truncated AWG span mesh");
            int from = values[i], to = values[i + 1], count = values[i + 2];
            if (from < last || to <= from || to > geometry.length() * 8
                || count < 2 || count > (values.length - i - 3) / 9)
                throw new IllegalArgumentException("Invalid AWG span samples");
            last = to;
            i += 3;
            for (int point = 0; point < count; point++)
                for (int axis = 0; axis < 9; axis++) {
                    int max = axis < 3 ? 512 : 64;
                    int value = values[i++];
                    if (value < -max || value > max) throw new IllegalArgumentException("Invalid AWG span coordinate");
                }
        }
        geometry.hang2 = values;
    }

    private void setOuterEnds(int mask) {
        if (mask < 0 || mask > 3) throw new IllegalArgumentException("Invalid AWG outer end mask");
        for (int end = 0; end < 2; end++)
            if ((mask & (1 << end)) != 0 && (cornerAt(end) >= 0 || outerExit(end) < 0))
                throw new IllegalArgumentException("Invalid AWG outer endpoint");
        outerEnds = mask;
    }

    static AwgCablePart deserialize(DataInputStream in) throws IOException {
        try {
            int size = in.available();
            int header = in.readUnsignedByte();
            if (header == 0x9e) {
                int length = in.readUnsignedShort();
                if (length < 9 || length > in.available() - 2) throw new IOException("Invalid AWG native geometry payload");
                byte[] base = in.readNBytes(length);
                if ((base[0] & 0xff) == 0x9e) throw new IOException("Nested AWG native geometry payload");
                var part = deserialize(new DataInputStream(new java.io.ByteArrayInputStream(base)));
                int second = in.readUnsignedByte();
                if (second > 0) {
                    if (part.cornerEnd < 0) throw new IOException("Missing AWG primary corner");
                    part.markCorner(1 - part.cornerEnd, second - 1);
                }
                int count = in.readUnsignedByte();
                if (count > 16 || in.available() != count * 7) throw new IOException("Invalid AWG native visual count");
                if (count > 0) {
                    int[] visual = new int[count * 7];
                    for (int i = 0; i < visual.length; i++) visual[i] = in.readUnsignedByte();
                    part.setNativeVisuals(visual);
                }
                return part;
            }
            if (size == 4)
                return new AwgCablePart(wire(LEGACY_WIRE_ID), header, in.readUnsignedByte(), in.readUnsignedByte(), in.readUnsignedByte());
            var wire = wire((header & 0x80) != 0 ? in.readUnsignedShort() : LEGACY_WIRE_ID);
            if (wire == null) throw new IOException("Unknown AWG wire descriptor");
            if (header == 0x9f) {
                if (in.available() != 3) throw new IOException("Invalid AWG knuckle payload");
                return new AwgCablePart(wire, -1,
                    new int[]{in.readUnsignedByte(), in.readUnsignedByte(), in.readUnsignedByte()});
            }
            int[] coordinates = new int[6];
            for (int i = 0; i < coordinates.length; i++) coordinates[i] = in.readUnsignedByte();
            var part = new AwgCablePart(wire, header & 0x1f, coordinates);
            int outer = (header & 0xa0) == 0xa0 ? in.readUnsignedByte() : 0;
            if ((header & 0xc0) == 0xc0) {
                int corner = in.readUnsignedByte();
                if (corner > 0) part.markCorner(corner - 1, in.readUnsignedByte());
                int count = in.readUnsignedShort();
                if (count > 2048 || in.available() != count * 2) throw new IOException("Invalid AWG span payload");
                int[] values = new int[count];
                for (int i = 0; i < count; i++) values[i] = in.readShort();
                part.setHang(values);
            } else if (in.available() == 2) part.markCorner(in.readUnsignedByte(), in.readUnsignedByte());
            else if (in.available() != 0) throw new IOException("Invalid AWG corner payload");
            part.setOuterEnds(outer);
            return part;
        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid AWG part", e);
        }
    }

    private void setNativeVisuals(int[] encoded) {
        if (geometry.isKnuckle() || encoded.length == 0 || encoded.length > 16 * 7 || encoded.length % 7 != 0)
            throw new IllegalArgumentException("Invalid AWG native visuals");
        var visuals = new java.util.ArrayList<CablePart>();
        for (int i = 0; i < encoded.length; i += 7) {
            int face = encoded[i];
            int[] line = java.util.Arrays.copyOfRange(encoded, i + 1, i + 7);
            for (int v : line) if (v < 0 || v > 16) throw new IllegalArgumentException("Invalid native visual point");
            if (face < 0 || face > 5 || visualOverlap(geometry, face, line) < 0)
                throw new IllegalArgumentException("Unowned native visual");
            var visual = CablePart.utilityGeometry(wire.name, face, line, width(wire), height(wire));
            if (!visual.valid()) throw new IllegalArgumentException("Invalid native visual");
            visuals.add(visual);
        }
        nativeVisuals = List.copyOf(visuals);
    }

    CablePart visual() { return geometry; }

    int bareTint() {
        if (wire.getInsulated()) return 0;
        return switch (wire.getMaterial()) {
            case COPPER -> 0xffb87345;
            case ALUMINUM -> 0xffaeb7bd;
        };
    }
}
