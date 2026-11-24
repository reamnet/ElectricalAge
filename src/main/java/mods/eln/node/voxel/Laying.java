package mods.eln.node.voxel;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Laying a routed run, all or nothing, for cables and pipes alike (Baughn 2026-11-11: no free cable or pipe on a refused
 * route). Every new part is checked against the world and against the route's other new parts in its block (pieces of
 * one cable may overlap where they touch; nothing else may) before anything is placed; then all are placed, and if one
 * still fails, those already placed are taken back.
 */
final class Laying {
    private Laying() {
    }

    /** Why the parts (one per position, same order) can't all be laid, or null. */
    static String check(Level level, List<BlockPos> at, List<? extends VoxelPart> parts) {
        return check(level, at, parts, java.util.Map.of());
    }

    static String check(Level level, List<BlockPos> at, List<? extends VoxelPart> parts,
                        java.util.Map<BlockPos, AwgCablePart> replaced) {
        for (int i = 0; i < parts.size(); i++) {
            VoxelPart part = parts.get(i);
            BlockPos bp = at.get(i);
            if (part instanceof PipePart pp && !pp.valid()) return "bad piece at " + bp.toShortString();
            if (part instanceof CablePart cp && !cp.valid()) return "bad piece at " + bp.toShortString();
            BlockState s = level.getBlockState(bp);
            VoxelNode existing = s.getBlock() instanceof VoxelNodeBlock && level.getBlockEntity(bp) instanceof VoxelNodeEntity ve
                && ve.getNode() instanceof VoxelNode n ? n : null;
            if (existing == null && Mimic.allowed(s)) {   // (through a stair, under a fence: clear of its own voxels)
                for (int[] hb : Mimic.boxes(s, level, bp))
                    for (int[] pb : part.boxes())
                        if (overlap(hb, pb)) return "the " + s.getBlock().getName().getString() + " is in the way at " + bp.toShortString();
            } else if (existing == null && !(s.isAir() || s.canBeReplaced())) return "a block is in the way at " + bp.toShortString();
            if (existing != null) {
                String why = existing.canAdd(part, replaced.get(bp));
                if (why != null) return why + " at " + bp.toShortString();
            }
            // the route's own other parts in this block
            for (int j = 0; j < i; j++) {
                if (!at.get(j).equals(bp)) continue;
                VoxelPart other = parts.get(j);
                if (part instanceof CablePart a && other instanceof CablePart b && (a.touches(b) || b.touches(a))) continue;
                if (part instanceof AwgCablePart a && other instanceof AwgCablePart b && a.joins(b)) continue;
                for (int[] x : part.boxes())
                    for (int[] y : other.boxes())
                        if (overlap(x, y)) return "the route crosses itself at " + bp.toShortString();
            }
        }
        return null;
    }

    /** Check, then place every part; null if all are laid, else why not (and none is left laid). */
    static String lay(Level level, List<BlockPos> at, List<? extends VoxelPart> parts, Player player) {
        return lay(level, at, parts, player, -1);
    }

    static String lay(Level level, List<BlockPos> at, List<? extends VoxelPart> parts, Player player, int failAt) {
        String err = check(level, at, parts);
        if (err != null) return err;
        boolean[] newNodes = new boolean[parts.size()];
        int attempted = -1;
        boolean committed = false;
        try {
            for (int i = 0; i < parts.size(); i++) {
                var pos = at.get(i);
                newNodes[i] = !(level.getBlockState(pos).getBlock() instanceof VoxelNodeBlock);
                attempted = i;
                if (i == failAt) return "forced";
                err = PipeKitItem.place(level, pos, parts.get(i), player);
                if (err != null) return err + " at " + pos.toShortString();
            }
            CableLinks.laid(at, parts);
            committed = true;
            return null;
        } finally {
            if (!committed)
                for (int k = attempted; k >= 0; k--) takeBack(level, at.get(k), parts.get(k), newNodes[k]);
        }
    }

    /** Drop only parts owned by this transaction. An existing voxel block stays even if empty. */
    private static void takeBack(Level level, BlockPos pos, VoxelPart part, boolean newNode) {
        if (!(level.getBlockEntity(pos) instanceof VoxelNodeEntity e) || !(e.getNode() instanceof VoxelNode n)) return;
        int idx = n.parts.indexOf(part);
        if (idx >= 0 && n.removePart(idx, false) && newNode) n.removeEmptyBlock();
    }

    static boolean overlap(int[] a, int[] b) {
        return a[0] < b[3] && b[0] < a[3] && a[1] < b[4] && b[1] < a[4] && a[2] < b[5] && b[2] < a[5];
    }

    /** For tests: lay these parts with a failure forced at index failAt (as if placing it failed). */
    static String layFailingAtForTests(Level level, List<BlockPos> at, List<? extends VoxelPart> parts, int failAt) {
        return lay(level, at, parts, null, failAt);
    }
}
