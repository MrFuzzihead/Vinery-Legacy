package com.mrfuzzihead.vinery.core.compat.rei.press;

import java.util.Collections;
import java.util.List;

import net.minecraft.world.item.crafting.RecipeHolder;

import com.mrfuzzihead.vinery.Vinery;
import com.mrfuzzihead.vinery.core.recipe.ApplePressMashingRecipe;

import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;

@SuppressWarnings("all")
public class ApplePressDisplay extends BasicDisplay {

    public static final CategoryIdentifier<ApplePressDisplay> APPLE_PRESS_DISPLAY = CategoryIdentifier
        .of(Vinery.MOD_ID, "apple_press_display");

    public ApplePressDisplay(RecipeHolder<ApplePressMashingRecipe> recipe) {
        this(
            Collections.singletonList(EntryIngredients.ofIngredient(recipe.value().input)),
            Collections.singletonList(
                EntryIngredients.of(
                    recipe.value()
                        .getResultItem(null))));
    }

    public ApplePressDisplay(List<EntryIngredient> inputs, List<EntryIngredient> outputs) {
        super(inputs, outputs);
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return APPLE_PRESS_DISPLAY;
    }

}
