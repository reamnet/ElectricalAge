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
import mods.eln.sixnode.electricalcable.UtilityCableMaterial;
import mods.eln.sixnode.electricalcable.UtilityCables;
import mods.eln.sixnode.electricalcable.WirePhysics;
import mods.eln.sixnode.resistor.ResistorElement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.UUID;

final class AwgCatalogCase implements TestCase {
    private final UtilityCableMaterial material;
    private final boolean highVoltage;
    private final int expected;
    private ResistorElement load;
    private FakePlayer player;
    private BlockPos at;

    AwgCatalogCase(UtilityCableMaterial material, boolean highVoltage, int expected) {
        this.material = material;
        this.highVoltage = highVoltage;
        this.expected = expected;
    }

    @Override public String id() {
        return "awg_catalog_" + (highVoltage ? "high_voltage" : material.name().toLowerCase(Locale.ROOT));
    }
    @Override public int ticks() { return 5 * expected + 5; }

    @Override public void build(TestContext ctx) {
        var catalog = UtilityCables.INSTANCE.getAll().stream()
            .filter(w -> w.getActsAsSingleConductor() && !w.getMelted() && w.getMaterial() == material
                && (w.parentItemDamage >= 2432) == highVoltage).toList();
        ctx.check("every catalog item is recognized, with intact historical identity", catalog.size() == expected
            && catalog.stream().allMatch(w -> AwgCablePart.supportsSpool(w)
                && Eln.sixNodeItem.getDescriptor(w.parentItemDamage) == w && !w.newItemStack().isEmpty()), "" + catalog.size());
        if (highVoltage) {
            var classes = catalog.stream().collect(Collectors.groupingBy(UtilityCableDescriptor::getConductorAreaMm2,
                Collectors.mapping(UtilityCableDescriptor::getInsulationVoltageRating, Collectors.toSet())));
            var ratings = Set.of(1000.0, 5000.0, 20000.0, 40000.0, 150000.0);
            ctx.check("both high-voltage gauges retain all five original insulation ratings",
                classes.keySet().equals(Set.of(3.309, 33.631)) && classes.values().stream().allMatch(ratings::equals)
                    && catalog.stream().allMatch(UtilityCableDescriptor::getInsulated), classes.toString());
        }
        ctx.check("melted and multicore entries stay out of voxel placement",
            UtilityCables.INSTANCE.getAll().stream().filter(w -> w.getMelted() || !w.getActsAsSingleConductor())
                .noneMatch(AwgCablePart::supportsSpool), "");
        Wp9aCases.source(ctx, Wp9aCases.SOURCE, 0, 1, 0.5);
        load = CableCases.end(ctx, 2, 1);
        at = ctx.at(1, 1, 1);
        player = FakePlayerFactory.get(ctx.level(), new GameProfile(
            UUID.nameUUIDFromBytes(id().getBytes(StandardCharsets.UTF_8)), "eln_awg_catalog"));
        player.setGameMode(GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        player.snapTo(at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, -90, 90);
        for (int i = 0; i < catalog.size(); i++) {
            var wire = catalog.get(i);
            int tick = i * 5 + 1;
            ctx.after(tick, () -> exercise(ctx, wire));
        }
    }

    private void exercise(TestContext ctx, UtilityCableDescriptor wire) {
        var spool = wire.newItemStack();
        wire.setRemainingLengthMeters(spool, 1.375);
        player.setItemInHand(InteractionHand.MAIN_HAND, spool);
        var hit = new BlockHitResult(Vec3.atLowerCornerOf(at).add(0.5, 0, 0.5), Direction.UP, at.below(), false);
        var result = spool.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        var part = AwgCablePart.at(ctx.level(), at).getFirst();
        var box = part.boxes().getFirst();
        ctx.check("registered spool places one meter with descriptor-sized mesh",
            result == InteractionResult.SUCCESS && wire.getRemainingLengthMeters(spool) == 0.375
                && box[5] - box[2] == Math.ceil(wire.render.width * 16)
                && box[4] - box[1] == Math.ceil(wire.render.height * 16)
                && !VoxelGeometry.of(List.of(part)).isEmpty(), wire.name);
        ctx.after(2, () -> {
            double amps = Wp9aCases.current(load);
            var run = part.activeRun();
            double predicted = WirePhysics.INSTANCE.resistivity20(material) / wire.getConductorAreaMm2()
                * (1 + WirePhysics.INSTANCE.temperatureCoefficient(material) * (run.getTemperatureCelsius() - 20));
            ctx.check("world voltage drop follows the original material, area and temperature",
                amps > 0.3 && Math.abs(amps - run.getCurrentAmps()) < 1e-6
                    && Math.abs(run.getVoltageDrop() - amps * predicted) < 1e-5 * predicted + 1e-8
                    && Double.isFinite(run.getStoredJoules()), wire.name + " " + run.getVoltageDrop() + " V");
            try {
                ctx.check("catalog descriptor and thermal state survive voxel reload",
                    Reload.node(ctx.level(), at).tagsEqual(), wire.name);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
            var node = (VoxelNode) ((VoxelNodeEntity) ctx.level().getBlockEntity(at)).getNode();
            if (node.removePart(0, true)) node.removeEmptyBlock();
            var drops = ctx.level().getEntitiesOfClass(ItemEntity.class, new AABB(at).inflate(0.25));
            ctx.check("the original paid spool item is recovered exactly once",
                drops.size() == 1 && drops.getFirst().getItem().is(wire.parentItem)
                    && wire.getRemainingLengthMeters(drops.getFirst().getItem()) == 1
                    && wire.getRemainingLengthMeters(spool) + 1 == 1.375, wire.name);
            drops.forEach(ItemEntity::discard);
        });
    }

    @Override public void measure(TestContext ctx) {}
}
