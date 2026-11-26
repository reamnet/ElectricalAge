package mods.eln.node.voxel;

import mods.eln.Eln;
import mods.eln.generic.ItemNbt;
import mods.eln.node.six.SixNodeItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Per-spool choice between the native voxel session and the existing six-node placement backend. */
public final class AwgPlacementMode {
    private static final String KEY = "utilityLegacyPlacement";
    private AwgPlacementMode() {}

    public static boolean supports(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof SixNodeItem item && AwgCablePart.supportsSpool(item.descriptor);
    }

    public static boolean legacy(ItemStack stack) {
        var tag = ItemNbt.get(stack);
        return supports(stack) && tag != null && tag.getBooleanOr(KEY, false);
    }

    public static String label(boolean legacy) { return legacy ? "Legacy six-node cable" : "Native voxel cable"; }

    /** Client choices may change only the matching server-held spool, never its paid length or identity. */
    public static boolean apply(ServerPlayer player, int wireId, int mode) {
        var stack = player.getMainHandItem();
        if (mode < 0 || mode > 1 || player.isSpectator() || !player.getAbilities().mayBuild || !supports(stack)
            || ((SixNodeItem) stack.getItem()).descriptor.parentItemDamage != wireId) return false;
        boolean legacy = mode == 1;
        if (legacy(stack) == legacy) return true;
        var tag = ItemNbt.get(stack);
        if (tag == null) tag = new net.minecraft.nbt.CompoundTag();
        if (legacy) tag.putBoolean(KEY, true); else tag.remove(KEY);
        ItemNbt.set(stack, tag);
        RoutePlacer.forget(player.getUUID());
        KitPreview.forget(player.getUUID());
        KitPreview.send(player, true, java.util.List.of());
        AwgCablePart.forgetLegacyRouting(player);
        player.sendOverlayMessage(net.minecraft.network.chat.Component.literal("Placement: " + label(legacy)));
        return true;
    }

    public static byte[] packet(int wireId, boolean legacy) {
        return new byte[]{Eln.packetAwgPlacementMode, (byte) (wireId >>> 8), (byte) wireId, (byte) (legacy ? 1 : 0)};
    }
}
