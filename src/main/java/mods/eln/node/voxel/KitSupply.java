package mods.eln.node.voxel;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * What a route is laid from: a cable or pipe kit, or a Toolbag's kits of its chosen kind taken as one (ToolbagItem). The placers
 * (CablePlacer, PipePlacer), their preview (KitPreview) and their lapse checks ask the item in the main hand for one.
 */
public interface KitSupply {
    /** A pipe (else a cable). */
    boolean pipe();

    /** CablePart.TIERS or PipePart tier. */
    int tier();

    /** A power cable's spec (CableKitItem.specOf: the tier's preset if none is set), else null. */
    CableSpec spec();

    /** Voxels it holds (several kits together may hold more than one kit's KitItem.MAX). */
    int length();

    /** Use up len voxels (no more than length()). */
    void take(int len);

    default mods.eln.sixnode.electricalcable.UtilityCableDescriptor wire() { return null; }

    default ItemStack spool() { return ItemStack.EMPTY; }

    /** What to sneak-click for to undo, in the placers' messages. */
    default String undoHint() {
        return "sneak-click";
    }

    /** One kit. */
    static KitSupply of(ItemStack kit) {
        boolean pipe = kit.getItem() instanceof PipeKitItem;
        int tier = kit.getItem() instanceof PipeKitItem p ? p.tier : kit.getItem() instanceof CableKitItem c ? c.tier : -1;
        CableSpec spec = CableKitItem.specOf(kit);
        return new KitSupply() {
            public boolean pipe() {
                return pipe;
            }

            public int tier() {
                return tier;
            }

            public CableSpec spec() {
                return spec;
            }

            public int length() {
                return KitItem.length(kit);
            }

            public void take(int len) {
                int left = KitItem.length(kit) - len;
                if (left <= 0) kit.shrink(1);
                else KitItem.setLength(kit, left);
            }
        };
    }

    static KitSupply awg(ItemStack stack, mods.eln.sixnode.electricalcable.UtilityCableDescriptor wire) {
        return new KitSupply() {
            public boolean pipe() { return false; }
            public int tier() { return 1; }
            public CableSpec spec() { return null; }
            public int length() {
                return stack.getCount() == 1 ? (int) Math.floor(wire.getRemainingLengthMeters(stack) * 16) : 0;
            }
            public void take(int len) {
                throw new UnsupportedOperationException("AWG spool debits are part of placement");
            }
            public mods.eln.sixnode.electricalcable.UtilityCableDescriptor wire() { return wire; }
            public ItemStack spool() { return stack; }
        };
    }

    /** The supply in p's main hand: a kit, or a Toolbag with a kind chosen that it holds; else null. */
    static KitSupply held(Player p) {
        if (p == null) return null;
        ItemStack s = p.getMainHandItem();
        if (s.getItem() instanceof CableKitItem || s.getItem() instanceof PipeKitItem) return of(s);
        if (s.getItem() instanceof mods.eln.node.six.SixNodeItem item
            && AwgCablePart.supportsSpool(item.descriptor))
            return awg(s, (mods.eln.sixnode.electricalcable.UtilityCableDescriptor) item.descriptor);
        if (s.getItem() instanceof ToolbagItem) return ToolbagItem.supply(s);
        return null;
    }

    /** Does supply s lay what a route of this kind was started with (pipe or cable, tier, spec)? */
    static boolean lays(KitSupply s, boolean pipe, int tier, CableSpec spec) {
        return s != null && s.pipe() == pipe && s.tier() == tier && java.util.Objects.equals(s.spec(), spec);
    }
}
