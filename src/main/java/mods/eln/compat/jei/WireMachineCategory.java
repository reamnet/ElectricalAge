package mods.eln.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import mods.eln.Eln;
import mods.eln.transparentnode.wiremachine.WireMachineKind;
import mods.eln.transparentnode.wiremachine.WireProductionRecipes;
import net.minecraft.network.chat.Component;

/** Variable length examples; the displayed ingot surplus remains in the real machine's paid buffer. */
public class WireMachineCategory extends AbstractRecipeCategory<WireProductionRecipes.Step> {
    public WireMachineCategory(IGuiHelper gui, IRecipeType<WireProductionRecipes.Step> type, WireMachineKind kind) {
        super(type, Component.literal(kind.getTitle()), gui.createDrawableItemStack(Eln.findItemStack(kind.getTitle(), 1)), 184, 128);
    }
    @Override public void setRecipe(IRecipeLayoutBuilder b, WireProductionRecipes.Step r, IFocusGroup focuses) {
        for (int i = 0; i < r.getInputs().size(); i++)
            b.addInputSlot(2 + 20 * (i % 4), 2 + 20 * (i / 4)).setStandardSlotBackground().add(r.getInputs().get(i).copy());
        b.addOutputSlot(156, 12).setOutputSlotBackground().add(r.getOutput().copy());
        for (int i = 0; i < r.getCatalysts().size(); i++)
            b.addSlot(mezz.jei.api.recipe.RecipeIngredientRole.CRAFTING_STATION, 2 + 20 * i, 48).setStandardSlotBackground().add(r.getCatalysts().get(i).copy());
    }
    @Override public void createRecipeExtras(IRecipeExtrasBuilder b, WireProductionRecipes.Step r, IFocusGroup focuses) {
        b.addRecipeArrowWidget().setPosition(112, 12);
        b.addText(Component.literal("32 m  /  " + mods.eln.misc.Utils.plotEnergy("", r.getEnergyJoules()).trim()), 184, 12).setPosition(0, 72);
        if (r.getMetalKg() > 0)
            b.addText(Component.literal(String.format(java.util.Locale.ROOT, "Metal: %.4f kg\nLoad in batches; surplus retained", r.getMetalKg())), 184, 38).setPosition(0, 88);
    }
}
