package mods.eln.gametest;

import com.google.gson.JsonObject;
import mods.eln.Eln;
import mods.eln.generic.IDescriptorItem;
import mods.eln.gui.MenuSpecs;
import mods.eln.gui.SlotSpec;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.transparentnode.cord.CordDescriptor;
import mods.eln.transparentnode.cord.CordRating;
import mods.eln.transparentnode.cord.OutletDescriptor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/** Reviewed port contracts alongside the unchanged 1.12 comparisons. Never used to suppress their diagnostics. */
final class HistoricalCompatibilityCase implements TestCase {
    @Override
    public String id() {
        return "historical_compatibility_reviewed_contracts";
    }

    @Override
    public void build(TestContext ctx) {}

    @Override
    public void measure(TestContext ctx) {
        var original = transparent(ctx, 128, "dc_dc_converter", "TransformerDescriptor");
        transparent(ctx, 129, "variable_dc_dc_converter", "VariableDcDcDescriptor");
        transparent(ctx, 130, "winding_dc_dc_converter", "FixedDcDcDescriptor");
        var tapped = transparent(ctx, 135, "signal_tapped_dc_dc_converter", "VariableTransformerDescriptor");
        var furnace = transparent(ctx, 2176, "old_480v_arc_furnace", "OldArcFurnaceDescriptor");
        var molten = Eln.sixNodeItem.getDescriptor(2176);
        identity(ctx, "six_node", 2176, molten, molten == null ? ItemStack.EMPTY : molten.newItemStack(),
            molten == null ? -1 : molten.parentItemDamage, "copper_molten_metal_pile", "MoltenMetalPileDescriptor");

        var outlet = transparent(ctx, 16000, "400_v_industrial_outlet", "OutletDescriptor");
        var cord = Eln.sharedItem.getDescriptor(16000);
        identity(ctx, "shared_item", 16000, cord, cord == null ? ItemStack.EMPTY : cord.newItemStack(),
            cord == null ? -1 : cord.parentItemDamage, "400_v_industrial_cord", "CordDescriptor");
        ctx.check("400 V socket and cord use the same separately keyed voltage tier",
            outlet instanceof OutletDescriptor socket && socket.getRating() == CordRating.V400
                && cord instanceof CordDescriptor plug && plug.getRating() == CordRating.V400
                && CordRating.V400.accepts(400) && !CordRating.V400.accepts(800), "");

        solar(ctx, 3073, "small_solar_panel", 37, 30, 8, 8.6, false);
        solar(ctx, 3074, "small_rotating_solar_panel", 37, 30, 8, 8.6, true);
        solar(ctx, 3075, "2x3_solar_panel", 111, 90, 16, 17.2, false);
        solar(ctx, 3076, "2x3_rotating_solar_panel", 111, 90, 16, 17.2, true);

        slots(ctx, original, List.of(64, 64, 1, 1));
        slots(ctx, tapped, List.of(64, 64, 1, 1));
        if (slots(ctx, furnace, List.of(64, 64, 64, 64, 64, 5))) {
            var menu = MenuSpecs.of(furnace).slots();
            var clay = new ItemStack(Items.CLAY_BALL);
            var iron = new ItemStack(Items.IRON_INGOT);
            ctx.check("furnace output slots reject insertion",
                menu.subList(0, 4).stream().noneMatch(slot -> slot.filter().test(clay) || slot.filter().test(iron)), "");
            ctx.check("furnace input accepts the arc-material feedstocks and rejects unrelated items",
                menu.get(4).filter().test(clay) && menu.get(4).filter().test(iron)
                    && !menu.get(4).filter().test(new ItemStack(Items.STICK)), "");
        }

        compressor(ctx, 2240, "50v_compressor", "CompressorDescriptor");
        compressor(ctx, 2244, "200v_compressor", "CompressorPortsDescriptor");
    }

    private static TransparentNodeDescriptor transparent(TestContext ctx, int id, String path, String type) {
        var descriptor = Eln.transparentNodeItem.getDescriptor(id);
        identity(ctx, "transparent_node", id, descriptor, descriptor == null ? ItemStack.EMPTY : descriptor.newItemStack(),
            descriptor == null ? -1 : descriptor.parentItemDamage, path, type);
        return descriptor;
    }

    private static void identity(TestContext ctx, String family, int id, Object descriptor, ItemStack stack,
                                 int parentId, String path, String type) {
        var item = stack.getItem();
        var name = BuiltInRegistries.ITEM.getKey(item);
        ctx.check(family + " " + id + " owns eln:" + path + " (" + type + ")",
            descriptor != null && descriptor.getClass().getSimpleName().equals(type)
                && parentId == id && name.toString().equals("eln:" + path)
                && item instanceof IDescriptorItem bound && bound.getEaDescriptor() == descriptor,
            name + ", parent ID " + parentId);
    }

    private static void solar(TestContext ctx, int id, String path, double voc, double vmp, double imp, double isc,
                              boolean rotating) {
        var descriptor = transparent(ctx, id, path, "SolarPanelDescriptor");
        if (descriptor == null) return;
        var fields = GoldenCase.fields(descriptor);
        double multiplier = Eln.solarPanelPowerFactor;
        ctx.check(path + " keeps its reviewed source electrical curve and rotation",
            value(fields, "openCircuitVoltage", voc) && value(fields, "optimumVoltage", vmp)
                && value(fields, "optimumCurrent", imp * multiplier)
                && value(fields, "shortCircuitCurrent", isc * multiplier)
                && value(fields, "electricalPmax", vmp * imp * multiplier)
                && fields.has("canRotate") && fields.get("canRotate").getAsBoolean() == rotating,
            fields.toString());
    }

    private static boolean value(JsonObject fields, String name, double expected) {
        return fields.has(name) && Math.abs(fields.get(name).getAsDouble() - expected) < 1e-9;
    }

    private static boolean slots(TestContext ctx, TransparentNodeDescriptor descriptor, List<Integer> limits) {
        if (descriptor == null) return false;
        var slots = MenuSpecs.of(descriptor).slots();
        var indices = java.util.stream.IntStream.range(0, limits.size()).boxed().toList();
        boolean matches = slots.stream().map(SlotSpec::index).toList().equals(indices)
            && slots.stream().map(SlotSpec::limit).toList().equals(limits);
        ctx.check(descriptor.name + " keeps the reviewed slot indices and limits", matches,
            slots.stream().map(slot -> slot.index() + "/" + slot.limit()).toList().toString());
        return matches;
    }

    private static void compressor(TestContext ctx, int id, String path, String type) {
        var descriptor = transparent(ctx, id, path, type);
        if (!slots(ctx, descriptor, List.of(64, 64, 64, 64, 64, 5))) return;
        var input = MenuSpecs.of(descriptor).slots().get(4).filter();
        ctx.check(path + " accepts native compressor inputs and rejects unprocessable historical probes",
            input.test(new ItemStack(Items.SAND))
                && input.test(Eln.findItemStack("4x Graphite Rods", 1))
                && !input.test(Eln.findItemStack("Cinnabar Dust", 1))
                && !input.test(Eln.findItemStack("Mining Pipe", 1)), "");
    }
}
