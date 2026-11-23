package mods.eln.node.voxel;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import mods.eln.registry.ElnRegistry;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;

import java.util.stream.Stream;

/** One block of preset LV or MV cable, equivalent to one retired cable item in a recipe. */
public record LightingCableIngredient(int tier) implements ICustomIngredient {
    public static final MapCodec<LightingCableIngredient> CODEC = Codec.intRange(1, 2)
        .fieldOf("tier").xmap(LightingCableIngredient::new, LightingCableIngredient::tier);

    @Override
    public boolean test(ItemStack stack) {
        return stack.is(ElnRegistry.CABLE_KITS.get(tier).get())
            && KitItem.length(stack) == 16 && CableKitItem.isPreset(stack);
    }

    @Override
    public Stream<Holder<Item>> items() {
        return Stream.of(ElnRegistry.CABLE_KITS.get(tier).get().builtInRegistryHolder());
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public IngredientType<?> getType() {
        return ElnRegistry.LIGHTING_CABLE_INGREDIENT.get();
    }

    @Override
    public SlotDisplay display() {
        return new SlotDisplay.ItemStackSlotDisplay(ItemStackTemplate.fromNonEmptyStack(CableKitItem.kit(tier, 16)));
    }
}
