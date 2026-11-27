package mods.eln.gametest.cases;

import mods.eln.Eln;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.mechanical.GeneratorElement;
import mods.eln.mechanical.ShaftElement;
import mods.eln.misc.LRDU;
import mods.eln.sim.mna.component.Resistor;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;

final class LargeSteamChunkCase implements TestCase {
    private static final BlockPos GENERATOR = new BlockPos(1_020_014, 100, 1_020_000);
    private static final BlockPos JOINT = GENERATOR.east(2).above();
    private final ChunkPos ownerChunk = new ChunkPos(GENERATOR.getX() >> 4, GENERATOR.getZ() >> 4);
    private final ChunkPos jointChunk = new ChunkPos(JOINT.getX() >> 4, JOINT.getZ() >> 4);
    private GeneratorElement generator;
    private ShaftElement joint;
    private Resistor load;
    private BlockEntity oldEntity;

    @Override public String id() { return "forkpatch_large_generator_real_chunk_eviction"; }
    @Override public int ticks() { return 280; }

    @Override
    public void build(TestContext ctx) {
        ticket(ctx, ownerChunk, true);
        ticket(ctx, jointChunk, true);
        ctx.floor(GENERATOR.below());
        ctx.floor(JOINT.below());
        generator = (GeneratorElement) ctx.placeByPlayer(Ids.transparent("Large Generator"), GENERATOR, 0);
        joint = (ShaftElement) ctx.placeByPlayer(Ids.transparent("Joint"), JOINT, 0);
        ctx.check("a raised shaft crosses two actual chunk columns",
            !ownerChunk.equals(jointChunk) && generator.getShaft() == joint.getShaft(), ownerChunk + " / " + jointChunk);
        generator.getShaft().setRads(200);
        load = new Resistor(generator.getElectricalLoad(generator.front, LRDU.Down), null).setR(3200.0 * 3200 / 1000);
        Eln.simulator.addElectricalComponent(load);
        ctx.after(30, () -> {
            checkPowered(ctx);
            oldEntity = ctx.level().getBlockEntity(GENERATOR);
            ticket(ctx, ownerChunk, false);
            ticket(ctx, jointChunk, false);
        });
        ctx.after(190, () -> {
            var chunks = ctx.level().getChunkSource();
            ctx.check("Minecraft evicts both shaft chunks and the old block entity",
                oldEntity != null && oldEntity.isRemoved() && chunks.getChunkNow(ownerChunk.x(), ownerChunk.z()) == null
                    && chunks.getChunkNow(jointChunk.x(), jointChunk.z()) == null,
                String.valueOf(oldEntity));
            checkPowered(ctx);
            ticket(ctx, ownerChunk, true);
            ticket(ctx, jointChunk, true);
            ctx.level().getChunk(ownerChunk.x(), ownerChunk.z());
            ctx.level().getChunk(jointChunk.x(), jointChunk.z());
        });
    }

    @Override
    public void measure(TestContext ctx) {
        try {
            BlockEntity freshEntity = ctx.level().getBlockEntity(GENERATOR);
            ctx.check("reloading creates a new Minecraft block entity",
                freshEntity != null && freshEntity != oldEntity && !freshEntity.isRemoved(), String.valueOf(freshEntity));
            checkPowered(ctx);
            ctx.level().destroyBlock(GENERATOR, false);
            ctx.check("removing the reloaded machine releases the joint and both owned ports",
                joint.getShaft().getElements().size() == 1 && ctx.nodeAt(GENERATOR) == null
                    && ctx.nodeAt(GENERATOR.east().above()) == null && ctx.nodeAt(GENERATOR.west().above()) == null,
                joint.getShaft().getElements().toString());
        } finally {
            Eln.simulator.removeElectricalComponent(load);
            ctx.level().removeBlock(JOINT, false);
            ctx.level().removeBlock(JOINT.below(), false);
            ctx.level().removeBlock(GENERATOR.below(), false);
            var section = new net.minecraft.world.phys.AABB(GENERATOR).inflate(5);
            ctx.level().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, section)
                .forEach(net.minecraft.world.entity.Entity::discard);
            ticket(ctx, ownerChunk, false);
            ticket(ctx, jointChunk, false);
        }
    }

    private void checkPowered(TestContext ctx) {
        ctx.check("the same generator and joint share one shaft carrying the load",
            ctx.nodeAt(GENERATOR) == generator.node && generator.getShaft() == joint.getShaft()
                && generator.getShaft().getElements().size() == 2 && load.getP() > 500, "load=" + load.getP() + " W");
    }

    private static void ticket(TestContext ctx, ChunkPos pos, boolean add) {
        var chunks = ctx.level().getChunkSource();
        if (add) chunks.addTicketWithRadius(TicketType.FORCED, pos, 0);
        else chunks.removeTicketWithRadius(TicketType.FORCED, pos, 0);
    }
}
