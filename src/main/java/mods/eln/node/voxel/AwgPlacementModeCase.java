package mods.eln.node.voxel;

import mods.eln.Eln;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.generic.ItemNbt;
import mods.eln.network.ServerPacketHandler;
import mods.eln.sixnode.electricalcable.UtilityCableDescriptor;
import mods.eln.sixnode.electricalcable.UtilityCableElement;
import mods.eln.sixnode.electricalcable.UtilityCableMaterial;
import mods.eln.sixnode.electricalcable.UtilityCables;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.List;

public final class AwgPlacementModeCase implements TestCase {
    @Override public String id() { return "awg_spool_placement_modes"; }
    @Override public int width() { return 10; }
    @Override public int height() { return 5; }
    @Override public int depth() { return 10; }
    @Override public int floorDepth() { return 10; }

    @Override public void build(TestContext ctx) {
        // This fixture changes game mode and item settings; keep the harness's shared player untouched.
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.get(ctx.level(), new com.mojang.authlib.GameProfile(
            java.util.UUID.fromString("1eb093ac-44c0-48d1-8a24-a8a36674b263"), "eln_awg_modes"));
        player.setGameMode(GameType.SURVIVAL);
        var wires = UtilityCables.INSTANCE.getAll().stream().filter(AwgCablePart::supportsSpool).toList();
        ctx.check("all70 copper/aluminum variants offer the placement menu", wires.size() == 70, "count=" + wires.size());
        try {
            for (var wire : wires) {
                var spool = wire.newItemStack(); wire.setRemainingLengthMeters(spool, 4.125);
                var before = ItemNbt.get(spool);
                player.setItemInHand(InteractionHand.MAIN_HAND, spool);
                ctx.check("old/default spool uses native placement", !AwgPlacementMode.legacy(spool) && KitSupply.held(player) != null, wire.name);
                ServerPacketHandler.handle(AwgPlacementMode.packet(wire.parentItemDamage, true), player);
                ctx.check("legacy mode persists on the actual spool and disables native preview", AwgPlacementMode.legacy(spool.copy())
                    && KitSupply.held(player) == null && wire.getRemainingLengthMeters(spool) == 4.125, wire.name);
                ServerPacketHandler.handle(AwgPlacementMode.packet(wire.parentItemDamage, false), player);
                ctx.check("returning to native preserves original length/UID/components", !AwgPlacementMode.legacy(spool)
                    && before.equals(ItemNbt.get(spool)), wire.name);
            }
            var wire = wires.getFirst(); var spool = wire.newItemStack();
            player.setItemInHand(InteractionHand.MAIN_HAND, spool);
            var before = ItemNbt.get(spool);
            var packet = AwgPlacementMode.packet(wire.parentItemDamage, true);
            byte[] invalid = packet.clone(); invalid[3] = 7;
            for (byte[] bad : List.of(new byte[]{Eln.packetAwgPlacementMode}, Arrays.copyOf(packet, 5), invalid,
                AwgPlacementMode.packet(wire.parentItemDamage + 1, true))) ServerPacketHandler.handle(bad, player);
            ctx.check("malformed or stale different-spool mode packets cannot change the spool", before.equals(ItemNbt.get(spool)), "");
            player.setPos(Vec3.atCenterOf(ctx.at(2, 1, 1)));
            var support = ctx.at(2, 0, 1); var hit = Vec3.atLowerCornerOf(support).add(.5, 1, .5);
            CablePlacer.use(player, ctx.level(), support, Direction.UP, KitSupply.held(player), hit, false);
            ctx.check("native route starts before mode switch", CablePlacer.active(player.getUUID()), "");
            ServerPacketHandler.handle(packet, player);
            ctx.check("mode change cancels stale route without payment", !CablePlacer.active(player.getUUID())
                && AwgPlacementMode.legacy(spool) && wire.getRemainingLengthMeters(spool) == wire.getRemainingLengthMeters(wire.newItemStack()), "");
            for (var material : UtilityCableMaterial.values()) {
                var selected = wires.stream().filter(w -> w.getMaterial() == material).findFirst().orElseThrow();
                exercisePlacement(ctx, player, selected, material == UtilityCableMaterial.COPPER ? 2 : 6);
            }
        } finally {
            CablePlacer.forget(player.getUUID()); AwgCablePart.forgetLegacyRouting(player);
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }
    }

    private static void exercisePlacement(TestContext ctx, net.minecraft.server.level.ServerPlayer player, UtilityCableDescriptor wire, int z) {
        var spool = wire.newItemStack(); wire.setRemainingLengthMeters(spool, 4.125);
        player.setItemInHand(InteractionHand.MAIN_HAND, spool); player.setShiftKeyDown(false);
        ServerPacketHandler.handle(AwgPlacementMode.packet(wire.parentItemDamage, true), player);
        var oldFloor = ctx.at(2, 0, z); player.setPos(Vec3.atCenterOf(oldFloor.above()));
        click(player, oldFloor);
        var node = ctx.nodeAt(oldFloor.above());
        ctx.check(wire.getMaterial() + " main-hand legacy places the original six-node cable for one meter",
            node instanceof mods.eln.node.six.SixNode six && six.getElement(mods.eln.misc.Direction.YN.getInt()) instanceof UtilityCableElement
                && wire.getRemainingLengthMeters(spool) == 3.125, "node=" + node);
        ServerPacketHandler.handle(AwgPlacementMode.packet(wire.parentItemDamage, false), player);
        player.setPos(Vec3.atCenterOf(ctx.at(5, 1, z)));
        click(player, ctx.at(4, 0, z)); click(player, ctx.at(6, 0, z)); click(player, ctx.at(6, 0, z));
        ctx.check(wire.getMaterial() + " switching back lays native voxel wire with exact two-meter payment",
            !CablePlacer.active(player.getUUID()) && !AwgCablePart.at(ctx.level(), ctx.at(5, 1, z)).isEmpty()
                && wire.getRemainingLengthMeters(spool) == 1.125, "remaining=" + wire.getRemainingLengthMeters(spool));
    }

    private static void click(net.minecraft.server.level.ServerPlayer player, net.minecraft.core.BlockPos floor) {
        var hit = new BlockHitResult(Vec3.atLowerCornerOf(floor).add(.5, 1, .5), Direction.UP, floor, false);
        player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
    }
    @Override public void measure(TestContext ctx) {}
}
