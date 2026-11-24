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
import net.minecraft.world.item.ItemStack;

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

    /**
     * Lay one supported run, then publish the debit to the supplied stack. A refused part
     * leaves the original stack untouched.
     */
    public static String place(ServerLevel level, BlockPos pos, int x0, int z0, int x1, int z1, ItemStack spool) {
        if (!valid(x0, z0, x1, z1)) return "invalid AWG endpoints";
        if (!level.hasChunkAt(pos) || !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) {
            return "AWG run needs a loaded block and a solid floor";
        }
        var part = new AwgCablePart(x0, z0, x1, z1);
        int voxels = part.geometry.length();
        var copy = spool.copy();
        var length = AwgWireLength.purchase(wire(), copy, voxels);
        if (length == null) return "insufficient or invalid AWG spool";
        var coord = new mods.eln.misc.Coordonate(pos, level);
        part.saved = AwgCableRun.Companion.create(wire(), length, new VoltageState(), new VoltageState(),
            Ambient.relative(coord) + 20).save();
        var error = Laying.lay(level, List.of(pos), List.of(part), null);
        if (error == null) {
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
