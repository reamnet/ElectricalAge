package mods.eln.node.voxel;

import mods.eln.generic.ItemNbt;
import mods.eln.sixnode.electricalcable.UtilityCableDescriptor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.world.item.ItemStack;

/**
 * Material owned by one homogeneous run, measured along the route before meshes, overlaps or sag.
 * A run is either entirely paid or entirely creative. Keep runs of different provenance separate
 * even if their electrical endpoints are joined.
 */
public final class AwgWireLength {
    private static final double VOXELS_PER_METER = 16;
    private final int descriptorId;
    private final boolean paid;
    private int routeVoxels;

    AwgWireLength(int descriptorId, int routeVoxels, boolean paid) {
        if (descriptorId < 0 || routeVoxels <= 0) throw new IllegalArgumentException("Invalid AWG run");
        this.descriptorId = descriptorId;
        this.routeVoxels = routeVoxels;
        this.paid = paid;
    }

    public static AwgWireLength creative(UtilityCableDescriptor wire, int routeVoxels) {
        requireSingleConductor(wire);
        return new AwgWireLength(wire.parentItemDamage, routeVoxels, false);
    }

    /**
     * Debit a committed route from the original spool in place. Returns null for an unusable spool.
     * The caller owns the placement transaction and must install this ledger at most once.
     */
    public static AwgWireLength purchase(UtilityCableDescriptor wire, ItemStack spool, int routeVoxels) {
        requireSingleConductor(wire);
        var length = new AwgWireLength(wire.parentItemDamage, routeVoxels, true);
        if (spool.getCount() != 1 || !spool.is(wire.parentItem)) return null;
        var tag = ItemNbt.get(spool);
        if (tag != null && tag.contains("utilityLengthMeters") && !(tag.get("utilityLengthMeters") instanceof NumericTag)) {
            return null;
        }

        double before = wire.getRemainingLengthMeters(spool);
        double cost = length.physicalMeters();
        double after = before - cost;
        // A corrupt huge spool can be too coarse to represent the debit. Reject it rather than mint wire.
        if (after < 0 || before - after != cost) return null;
        wire.setRemainingLengthMeters(spool, after);
        if (after == 0) spool.shrink(1);
        return length;
    }

    private static void requireSingleConductor(UtilityCableDescriptor wire) {
        if (!wire.getActsAsSingleConductor()) throw new IllegalArgumentException("Expected a single-conductor spool");
    }

    public double physicalMeters() {
        return routeVoxels / VOXELS_PER_METER;
    }

    public int paidVoxels() {
        return paid ? routeVoxels : 0;
    }

    /** Transfer part or all of this run to a new owner. Offsets are measured on the unextended route. */
    public AwgWireLength take(int voxels) {
        if (voxels > routeVoxels) throw new IllegalArgumentException("Cannot take more than the run owns");
        var taken = new AwgWireLength(descriptorId, voxels, paid);
        routeVoxels -= voxels;
        return taken;
    }

    /** Combine adjacent runs of the same wire and provenance. Electrical joining alone does not require this. */
    public void append(AwgWireLength other) {
        if (this == other || descriptorId != other.descriptorId || paid != other.paid
            || routeVoxels == 0 || other.routeVoxels == 0) {
            throw new IllegalArgumentException("Incompatible AWG runs");
        }
        routeVoxels = Math.addExact(routeVoxels, other.routeVoxels);
        other.routeVoxels = 0;
    }

    /** Recover intact wire once. Later failure handling can consume the same ledger for damaged recovery. */
    public ItemStack recover(UtilityCableDescriptor wire) {
        requireSingleConductor(wire);
        if (wire.parentItemDamage != descriptorId) throw new IllegalArgumentException("Wrong wire descriptor");
        ItemStack result = ItemStack.EMPTY;
        if (paidVoxels() > 0) {
            result = wire.newItemStack();
            wire.setRemainingLengthMeters(result, physicalMeters());
        }
        routeVoxels = 0;
        return result;
    }

    public CompoundTag save() {
        if (routeVoxels == 0) throw new IllegalStateException("No run remains to save");
        var tag = new CompoundTag();
        tag.putInt("wire", descriptorId);
        tag.putInt("routeVoxels", routeVoxels);
        tag.putInt("paidVoxels", paidVoxels());
        return tag;
    }

    /** The voxel owner supplies the expected descriptor ID. Invalid data cannot become refundable wire. */
    public static AwgWireLength load(CompoundTag tag, int expectedDescriptorId) {
        int wire = readInt(tag, "wire");
        int route = readInt(tag, "routeVoxels");
        int paid = readInt(tag, "paidVoxels");
        if (wire != expectedDescriptorId || (paid != 0 && paid != route)) {
            throw new IllegalArgumentException("Invalid AWG wire ledger");
        }
        return new AwgWireLength(wire, route, paid != 0);
    }

    private static int readInt(CompoundTag tag, String key) {
        if (!(tag.get(key) instanceof IntTag value)) throw new IllegalArgumentException("Invalid AWG ledger field " + key);
        return value.value();
    }
}
