package mods.eln.gridnode;

import mods.eln.generic.GenericItemBlockUsingDamageDescriptor;
import mods.eln.node.voxel.CableSpec;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import mods.eln.sixnode.electricalcable.UtilityCableDescriptor;
import mods.eln.sixnode.electricalcable.UtilityCableMaterial;
import net.minecraft.world.item.ItemStack;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/** Native span geometry parameters, derived from the conductor already paid for by its link. */
public record GridCableAppearance(float radius, int tint, boolean metal) {
    public static final int PACKET_VERSION = 0x47524431; // GRD1; old unversioned publications remain readable.
    public static final GridCableAppearance LEGACY = new GridCableAppearance(.05f, 0xffffffff, false);

    public static GridCableAppearance of(ItemStack paid) {
        if (OverheadCableItem.is(paid))
            return new GridCableAppearance(new CableSpec(OverheadCableItem.mm2(paid), 0).width(0) / 32f, 0xffb87345, true);
        Object descriptor = GenericItemBlockUsingDamageDescriptor.getDescriptor(paid);
        return descriptor instanceof ElectricalCableDescriptor cable ? of(cable) : LEGACY;
    }

    public static GridCableAppearance of(ElectricalCableDescriptor cable) {
        boolean bare = cable instanceof UtilityCableDescriptor utility && !utility.getInsulated() && !utility.getMelted();
        int colour = bare && ((UtilityCableDescriptor)cable).getMaterial() == UtilityCableMaterial.ALUMINUM
            ? 0xffaeb7bd : bare ? 0xffb87345 : 0xffffffff;
        return new GridCableAppearance(Math.max(.001f, Math.min(1f, cable.render.width / 2)), colour, bare);
    }

    public void write(DataOutputStream stream) throws IOException {
        stream.writeFloat(radius);stream.writeInt(tint);stream.writeByte(metal ? 1 : 0);
    }

    public static GridCableAppearance read(DataInputStream stream) throws IOException {
        float radius = stream.readFloat();int tint = stream.readInt();int material = stream.readUnsignedByte();
        if (!Float.isFinite(radius) || radius <= 0 || radius > 1 || (tint >>> 24) != 255 || material > 1)
            throw new IOException("Invalid grid cable appearance");
        return new GridCableAppearance(radius, tint, material == 1);
    }
}
