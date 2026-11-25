package mods.eln.gametest.cases;

import mods.eln.Eln;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.misc.Coordonate;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeManager;
import mods.eln.node.voxel.AwgCablePart;
import mods.eln.node.voxel.VoxelNode;
import mods.eln.sixnode.electricalcable.UtilityCableDescriptor;
import mods.eln.sixnode.resistor.ResistorElement;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Set;

/** Actual chunk eviction; EA nodes intentionally keep simulating outside loaded Minecraft chunks. */
final class AwgChunkLifecycleCase implements TestCase {
    private static final BlockPos ORIGIN = new BlockPos(1_000_014, 100, 1_000_000);
    private final ChunkPos westChunk = new ChunkPos(ORIGIN.getX() >> 4, ORIGIN.getZ() >> 4);
    private final ChunkPos eastChunk = new ChunkPos((ORIGIN.getX() + 2) >> 4, ORIGIN.getZ() >> 4);
    private ResistorElement load;
    private NodeBase original;
    private BlockEntity anchor;
    private AwgCablePart part, melted, feeder;
    private Set<Object> terminals;
    private boolean observedUnload;
    private double loadAmps, feederAmps, wireAmps;
    // Sample after MNA, before slow thermal processes restamp temperature-dependent resistances.
    private final mods.eln.sim.IProcess sample = seconds -> {
        loadAmps = Math.abs(Wp9aCases.current(load));
        feederAmps = feeder.activeRun().getCurrentAmps();
        wireAmps = part.activeRun().getCurrentAmps();
    };

    @Override public String id() { return "awg_actual_chunk_lifecycle"; }
    @Override public int ticks() { return 480; }

    @Override public void build(TestContext ctx) {
        var level = ctx.level();
        // Keep the remote circuit loaded while constructing it, outside the harness ticket halo.
        level.getChunkSource().addTicketWithRadius(TicketType.FORCED, westChunk, 0);
        level.getChunkSource().addTicketWithRadius(TicketType.FORCED, eastChunk, 0);
        var remote = new TestContext() {
            @Override public net.minecraft.server.level.ServerLevel level() { return level; }
            @Override public BlockPos at(int x, int y, int z) { return ORIGIN.offset(x, y, z); }
            @Override public void floor(BlockPos p) { level.setBlockAndUpdate(p, Blocks.STONE.defaultBlockState()); }
            @Override public void line(String text) { ctx.line(text); }
            @Override public void check(String what, boolean ok, String detail) { ctx.check(what, ok, detail); }
            @Override public void checkValue(String what, double measured, double expected) { ctx.checkValue(what, measured, expected); }
        };
        for (int x = 0; x <= 4; x++) remote.floor(remote.at(x, 0, 0));
        Wp9aCases.source(remote, Wp9aCases.SOURCE, 0, 0, 12);
        load = Wp9aCases.resistor(remote, Wp9aCases.POWER_RESISTOR, 3, 0, CableCases.CORE);
        remote.placeSix(Wp9aCases.GROUND, remote.at(4, 1, 0));
        var wire = (UtilityCableDescriptor) Eln.sixNodeItem.getDescriptor(2200);
        var spool = wire.newItemStack();
        wire.setRemainingLengthMeters(spool, 2.125);
        for (int x = 1; x <= 2; x++) {
            String error = AwgCablePart.place(level, remote.at(x, 1, 0), 0, 8, 16, 8, spool);
            if (error != null) throw new IllegalStateException(error);
        }
        ctx.check("cross-boundary route debits exactly two meters", wire.getRemainingLengthMeters(spool) == 0.125, "");
        var pos = remote.at(2, 1, 0);
        // Seed persistent damage/heat through the real save schema before testing chunk eviction.
        var meltedPos = remote.at(2, 1, 1);
        remote.floor(meltedPos.below());
        var extra = wire.newItemStack();
        wire.setRemainingLengthMeters(extra, 1.125);
        String error = AwgCablePart.place(level, meltedPos, 0, 8, 16, 8, extra);
        if (error != null) throw new IllegalStateException(error);
        try {
            mods.eln.gametest.Reload.node(level, pos, tag -> {
                var state = tag.getCompoundOrEmpty("p0").getCompoundOrEmpty("run");
                state.putDouble("exposure", 0.375);
                state.putDouble("joules", new mods.eln.sixnode.electricalcable.WireThermalPhysics(
                    wire.getMaterial(), wire.getConductorAreaMm2(), 1).sensibleEnthalpy(60));
                return tag;
            });
            mods.eln.gametest.Reload.node(level, meltedPos, tag -> {
                var state = tag.getCompoundOrEmpty("p0").getCompoundOrEmpty("run");
                state.putInt("fused", 1);
                state.putInt("jacketLost", 1);
                return tag;
            });
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        melted = AwgCablePart.at(level, meltedPos).getFirst();
        original = NodeManager.instance.getNodeFromCoordonate(new Coordonate(pos, level));
        anchor = level.getBlockEntity(pos);
        part = AwgCablePart.at(level, pos).getFirst();
        feeder = AwgCablePart.at(level, remote.at(1, 1, 0)).getFirst();
        Eln.simulator.addElectricalProcess(sample);
        for (int cycle = 0; cycle < 3; cycle++) {
            int offset = cycle * 150;
            ctx.after(25 + offset, () -> {
                checkPowered(ctx);
                anchor = level.getBlockEntity(pos);
                terminals = Set.copyOf(((VoxelNode) original).conductorNames().keySet());
                level.getChunkSource().removeTicketWithRadius(TicketType.FORCED, eastChunk, 0);
                level.getChunkSource().removeTicketWithRadius(TicketType.FORCED, westChunk, 0);
            });
            ctx.after(100 + offset, () -> {
                observedUnload = level.getChunkSource().getChunkNow(eastChunk.x(), eastChunk.z()) == null && anchor.isRemoved();
                ctx.check("server actually evicts target chunk and its block entity", observedUnload, "");
                ctx.check("both sides of the boundary actually unload", level.getChunkSource().getChunkNow(westChunk.x(), westChunk.z()) == null, "");
                ctx.check("unloaded AWG retains its node and active run", original == NodeManager.instance.getNodeFromCoordonate(
                    new Coordonate(pos, level)) && part.activeRun() != null, "");
                checkPowered(ctx);
                ctx.check("unload preserves owned terminal identities", terminals.equals(((VoxelNode) original).conductorNames().keySet()), "");
                level.getChunkSource().addTicketWithRadius(TicketType.FORCED, eastChunk, 0);
                level.getChunkSource().addTicketWithRadius(TicketType.FORCED, westChunk, 0);
                level.getChunk(eastChunk.x(), eastChunk.z());
            });
            ctx.after(130 + offset, () -> {
                ctx.check("reload creates a fresh block entity for the same AWG node", observedUnload
                    && level.getBlockEntity(pos) != anchor && AwgCablePart.at(level, pos).getFirst() == part, "");
                ctx.check("reload preserves owned terminal identities", terminals.equals(((VoxelNode) original).conductorNames().keySet()), "");
                checkPowered(ctx);
            });
        }
    }

    private void checkPowered(TestContext ctx) {
        double amps = loadAmps;
        ctx.check("eviction preserves accumulated insulation exposure and elevated temperature",
            part.activeRun().getInsulationExposure() == 0.375 && part.activeRun().getTemperatureCelsius() > 30, "");
        ctx.check("melted wire remains electrically inactive", melted.activeRun().getConductorMelted()
            && melted.activeRun().getCurrentAmps() == 0, "");
        ctx.check("both paid conductors carry the load current", amps > 1 && part.activeRun() != null
            && Math.abs(Math.abs(wireAmps) - amps) < 1e-6
            && Math.abs(Math.abs(feederAmps) - amps) < 1e-6, "load=" + amps + " A, feeder=" + feederAmps
                + " A, second=" + wireAmps + " A");
    }

    @Override public void measure(TestContext ctx) {
        try {
            checkPowered(ctx);
            for (int x = 0; x <= 4; x++) {
                ctx.level().removeBlock(ORIGIN.offset(x, 1, 0), false);
                ctx.level().removeBlock(ORIGIN.offset(x, 0, 0), false);
            }
            ctx.level().removeBlock(ORIGIN.offset(2, 1, 1), false);
            ctx.level().removeBlock(ORIGIN.offset(2, 0, 1), false);
            ctx.level().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                new net.minecraft.world.phys.AABB(ORIGIN).inflate(6)).forEach(net.minecraft.world.entity.Entity::discard);
            ctx.check("removed terminal loads leave MNA and release component references", terminals.stream().allMatch(t ->
                !Eln.simulator.mna.addStates.contains(t)
                    && Eln.simulator.mna.systems().stream().noneMatch(system -> system.states.contains(t))
                    && ((mods.eln.sim.ElectricalLoad) t).getConnectedComponents().isEmpty()), "");
            ctx.check("removal after chunk reload retires the AWG node", NodeManager.instance.getNodeFromCoordonate(
                new Coordonate(ORIGIN.offset(2, 1, 0), ctx.level())) == null && part.activeRun() == null, "");
        } finally {
            Eln.simulator.removeElectricalProcess(sample);
            ctx.level().getChunkSource().removeTicketWithRadius(TicketType.FORCED, westChunk, 0);
            ctx.level().getChunkSource().removeTicketWithRadius(TicketType.FORCED, eastChunk, 0);
        }
    }
}
