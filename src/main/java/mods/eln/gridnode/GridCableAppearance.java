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

/** Visual parameters derived from the same paid stack used by the electrical span. */
public record GridCableAppearance(float radius, int tint, Surface surface) {
    public static final int PACKET_VERSION = 0x47524432; // GRD2
    public static final int PREVIOUS_PACKET_VERSION = 0x47524431; // GRD1
    public static final int COPPER = 0xffd96b2e;
    public static final int ALUMINUM = 0xffc7ccd6;
    public static final int JACKET = 0xff222629;
    public static final GridCableAppearance LEGACY = new GridCableAppearance(.05f, 0xffffffff, Surface.LEGACY_TEXTURE);

    // The first two wire values retain their GRD1 meanings.
    public enum Surface { LEGACY_TEXTURE, NATIVE_METAL, JACKET, AWG_METAL }

    public boolean usesAwgGeometry() {
        return surface == Surface.JACKET || surface == Surface.AWG_METAL;
    }

    public float outsideRadius() {
        return surface == Surface.JACKET ? radius * 1.18f + .002f : radius;
    }

    public static GridCableAppearance of(ItemStack paid) {
        if (OverheadCableItem.is(paid))
            return new GridCableAppearance(new CableSpec(OverheadCableItem.mm2(paid), 0).width(0) / 32f, 0xffb87345, Surface.NATIVE_METAL);
        Object descriptor = GenericItemBlockUsingDamageDescriptor.getDescriptor(paid);
        return descriptor instanceof ElectricalCableDescriptor cable ? of(cable) : LEGACY;
    }

    public static GridCableAppearance of(ElectricalCableDescriptor cable) {
        if (cable instanceof UtilityCableDescriptor utility) {
            // ForkPatch overhead dimensions are independent of the six-node cable model.
            float radius = (float) (Math.sqrt(utility.getConductorAreaMm2() / Math.PI) * .006);
            int metal = utility.getMaterial() == UtilityCableMaterial.ALUMINUM ? ALUMINUM : COPPER;
            return new GridCableAppearance(radius, metal, utility.getInsulated() ? Surface.JACKET : Surface.AWG_METAL);
        }
        return new GridCableAppearance(Math.max(.001f, Math.min(1f, cable.render.width / 2)), 0xffffffff, Surface.LEGACY_TEXTURE);
    }

    public void write(DataOutputStream stream) throws IOException {
        stream.writeFloat(radius);
        stream.writeInt(tint);
        stream.writeByte(surface.ordinal());
    }

    public static GridCableAppearance read(DataInputStream stream, boolean previousVersion) throws IOException {
        float radius = stream.readFloat();
        int tint = stream.readInt();
        int material = stream.readUnsignedByte();
        Surface[] surfaces = Surface.values();
        if (!Float.isFinite(radius) || radius <= 0 || radius > 1 || (tint >>> 24) != 255 ||
            material >= (previousVersion ? 2 : surfaces.length))
            throw new IOException("Invalid grid cable appearance");
        GridCableAppearance appearance = new GridCableAppearance(radius, tint, surfaces[material]);
        if (appearance.outsideRadius() > 1) throw new IOException("Invalid grid cable outside radius");
        return appearance;
    }
}
