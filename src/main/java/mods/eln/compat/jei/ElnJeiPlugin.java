package mods.eln.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mods.eln.Eln;
import mods.eln.compat.JeiPages;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import net.minecraft.resources.Identifier;

/**
 * JEI (loaded only when JEI is installed): the cable-kit recipes, which are custom recipes JEI can't list itself, as
 * examples in the crafting category (Baughn: they weren't in JEI; the grid takes whole stacks all the same); and the
 * Tree Resin Collector as a machine making tree resin (ResinCategory). What it shows beyond the game's recipes is in
 * JeiPages, which the GameTest jei_every_item_reachable checks.
 */
@JeiPlugin
public class ElnJeiPlugin implements IModPlugin {
    @Override
    public Identifier getPluginUid() {
        return Identifier.fromNamespaceAndPath("eln", "jei");
    }

    /** A recipe type per machine list (JeiPages.MACHINES; MachineCategory). */
    static final java.util.Map<String, mezz.jei.api.recipe.types.IRecipeType<mods.eln.misc.Recipe>> TYPES = new java.util.LinkedHashMap<>();

    static {
        for (String k : JeiPages.MACHINES.keySet()) TYPES.put(k, mezz.jei.api.recipe.types.IRecipeType.create("eln", k, mods.eln.misc.Recipe.class));
    }

    static final java.util.Map<mods.eln.transparentnode.wiremachine.WireMachineKind, mezz.jei.api.recipe.types.IRecipeType<mods.eln.transparentnode.wiremachine.WireProductionRecipes.Step>> WIRE_TYPES = new java.util.LinkedHashMap<>();
    static {
        for (var kind : mods.eln.transparentnode.wiremachine.WireMachineKind.values())
            WIRE_TYPES.put(kind, mezz.jei.api.recipe.types.IRecipeType.create("eln", "wire_" + kind.name().toLowerCase(java.util.Locale.ROOT), mods.eln.transparentnode.wiremachine.WireProductionRecipes.Step.class));
    }

    /**
     * The machines' recipe lists are filled when a server starts (ElnMachineRecipes, after the registries are bound), so a
     * client on a dedicated server had none, and no machine list either: JEI showed no EA machine recipes (Baughn, on
     * Incognito; it worked in single player). JEI runs once the client has joined and its registries are bound: fill them
     * here if they're empty.
     */
    static void ensureRecipes() {
        mods.eln.registry.ElnRegistry.afterRegistration();
        if (Eln.maceratorRecipes.getRecipes().isEmpty()) mods.eln.registry.ElnMachineRecipes.registerAll();
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        ensureRecipes();
        registration.addRecipeCategories(new FabricatorCategory(registration.getJeiHelpers().getGuiHelper()));
        for (var e : JeiPages.MACHINES.entrySet()) {
            var list = e.getValue().get();
            if (list.getMachines().isEmpty() || list.getRecipes().isEmpty()) continue;
            registration.addRecipeCategories(new MachineCategory(registration.getJeiHelpers().getGuiHelper(), TYPES.get(e.getKey()), e.getKey(),
                list.getMachines().get(0)));
        }
        for (var entry : WIRE_TYPES.entrySet())
            registration.addRecipeCategories(new WireMachineCategory(registration.getJeiHelpers().getGuiHelper(), entry.getValue(), entry.getKey()));
        var gui = registration.getJeiHelpers().getGuiHelper();
        if (!JeiPages.resins().isEmpty()) registration.addRecipeCategories(new ResinCategory(gui, JeiPages.collector()));
        if (!JeiPages.anneals().isEmpty()) registration.addRecipeCategories(new AnnealCategory(gui, JeiPages.furnace()));
        if (!JeiPages.draws().isEmpty()) registration.addRecipeCategories(new WireDrawingCategory(gui, JeiPages.drawers().getLast()));
        if (!JeiPages.oilSteps().isEmpty()) registration.addRecipeCategories(new OilRubberCategory(gui, JeiPages.oilSteps().get(0).machine()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        ensureRecipes();
        registration.addRecipes(FabricatorCategory.TYPE, mods.eln.transparentnode.fabricator.FabricationRecipes.examples());
        var wireSteps = mods.eln.transparentnode.wiremachine.WireProductionRecipes.steps();
        for (var entry : WIRE_TYPES.entrySet())
            registration.addRecipes(entry.getValue(), wireSteps.stream().filter(step -> step.getKind() == entry.getKey()).toList());
        registration.addRecipes(RecipeTypes.CRAFTING, mods.eln.node.voxel.CableKitRecipes.examples());
        registration.addRecipes(RecipeTypes.CRAFTING, mods.eln.node.voxel.PipeKitRecipes.examples());
        for (var e : JeiPages.MACHINES.entrySet()) {
            var list = e.getValue().get();
            if (!list.getMachines().isEmpty() && !list.getRecipes().isEmpty()) registration.addRecipes(TYPES.get(e.getKey()), list.getRecipes());
        }
        registration.addRecipes(ResinCategory.TYPE, JeiPages.resins());
        registration.addRecipes(AnnealCategory.TYPE, JeiPages.anneals());
        registration.addRecipes(WireDrawingCategory.TYPE, JeiPages.draws());
        registration.addRecipes(OilRubberCategory.TYPE, JeiPages.oilSteps());
        // what no recipe makes (ores, oil, fuel cuts, a blown fuse, a print, creative-only items): an information page
        for (JeiPages.Info i : JeiPages.infos()) registration.addItemStackInfo(i.items(), net.minecraft.network.chat.Component.translatable(i.key()));
    }

    /** What JeiPages.hidden() names is kept out of the list. */
    @Override
    public void onRuntimeAvailable(mezz.jei.api.runtime.IJeiRuntime runtime) {
        List<ItemStack> hidden = new java.util.ArrayList<>();
        for (net.minecraft.world.item.Item i : JeiPages.hidden()) hidden.add(new ItemStack(i));
        if (!hidden.isEmpty()) runtime.getIngredientManager().removeIngredientsAtRuntime(mezz.jei.api.constants.VanillaTypes.ITEM_STACK, hidden);
    }

    /** The bike trunk's panel lies right of the trunk's rectangle: JEI keeps its ingredient list off it (AdventurAgent). */
    @Override
    public void registerGuiHandlers(mezz.jei.api.registration.IGuiHandlerRegistration registration) {
        registration.addGuiContainerHandler(mods.eln.client.gui.BikeTrunkScreen.class,
            new mezz.jei.api.gui.handlers.IGuiContainerHandler<mods.eln.client.gui.BikeTrunkScreen>() {
                @Override
                public List<net.minecraft.client.renderer.Rect2i> getGuiExtraAreas(mods.eln.client.gui.BikeTrunkScreen screen) {
                    return screen.extraAreas();
                }
            });
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addCraftingStation(FabricatorCategory.TYPE, Eln.findItemStack("Fabricator", 1));
        for (var entry : WIRE_TYPES.entrySet())
            registration.addCraftingStation(entry.getValue(), Eln.findItemStack(entry.getKey().getTitle(), 1));
        for (var e : JeiPages.MACHINES.entrySet()) {
            var list = e.getValue().get();
            if (list.getRecipes().isEmpty()) continue;
            for (ItemStack m : list.getMachines()) registration.addCraftingStation(TYPES.get(e.getKey()), m);
        }
        if (!JeiPages.resins().isEmpty()) registration.addCraftingStation(ResinCategory.TYPE, JeiPages.collector());
        if (!JeiPages.anneals().isEmpty()) registration.addCraftingStation(AnnealCategory.TYPE, JeiPages.furnace());
        for (ItemStack m : JeiPages.drawers()) registration.addCraftingStation(WireDrawingCategory.TYPE, m);
        for (var st : JeiPages.oilSteps()) registration.addCraftingStation(OilRubberCategory.TYPE, st.machine());
    }
}
