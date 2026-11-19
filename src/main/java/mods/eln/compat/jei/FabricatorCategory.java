package mods.eln.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import mods.eln.Eln;
import mods.eln.transparentnode.fabricator.FabricationRecipes;
import net.minecraft.network.chat.Component;

/** The displayed yield belongs to each paid attempt, including an ALU attempt that produces nothing. */
public class FabricatorCategory extends AbstractRecipeCategory<FabricationRecipes.Example> {
    public static final IRecipeType<FabricationRecipes.Example> TYPE = IRecipeType.create("eln", "fabricator", FabricationRecipes.Example.class);
    public FabricatorCategory(IGuiHelper gui) {
        super(TYPE, Component.translatable("block.eln.fabricator"), gui.createDrawableItemStack(Eln.findItemStack("Fabricator", 1)), 184, 84);
    }
    @Override public void setRecipe(IRecipeLayoutBuilder layout, FabricationRecipes.Example example, IFocusGroup focuses) {
        for (int i = 0; i < example.getInputs().size(); i++)
            layout.addInputSlot(8 + 24 * i, 8).setStandardSlotBackground().add(example.getInputs().get(i).copy());
        layout.addOutputSlot(148, 8).setOutputSlotBackground().add(example.getOutput().copy());
    }
    @Override public void createRecipeExtras(IRecipeExtrasBuilder extras, FabricationRecipes.Example example, IFocusGroup focuses) {
        extras.addRecipeArrowWidget().setPosition(84, 8);
        extras.addText(Component.literal(mods.eln.i18n.I18N.tr("Energy per attempt: %1$ J", 4000)), 184, 12).setPosition(0, 40);
        extras.addText(Component.literal(mods.eln.i18n.I18N.tr("Yield: %1$ percent per paid attempt", example.getOperation().getChancePercent())), 184, 24).setPosition(0, 56);
    }
}
