package com.mrfuzzihead.vinery.core.compat.rei;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;

import com.mrfuzzihead.vinery.core.compat.rei.press.ApplePressCategory;
import com.mrfuzzihead.vinery.core.compat.rei.press.ApplePressDisplay;
import com.mrfuzzihead.vinery.core.compat.rei.press.ApplePressFermentingCategory;
import com.mrfuzzihead.vinery.core.compat.rei.press.ApplePressFermentingDisplay;
import com.mrfuzzihead.vinery.core.compat.rei.wine.FermentationBarrelCategory;
import com.mrfuzzihead.vinery.core.compat.rei.wine.FermentationBarrelDisplay;
import com.mrfuzzihead.vinery.core.recipe.ApplePressFermentingRecipe;
import com.mrfuzzihead.vinery.core.recipe.ApplePressMashingRecipe;
import com.mrfuzzihead.vinery.core.recipe.FermentationBarrelRecipe;
import com.mrfuzzihead.vinery.core.registry.ObjectRegistry;

import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.common.util.EntryStacks;

public class VineryReiClientPlugin {

    public static void registerCategories(CategoryRegistry registry) {
        registry.add(new FermentationBarrelCategory());
        registry.add(new ApplePressCategory());
        registry.add(new ApplePressFermentingCategory());
        registry.addWorkstations(
            FermentationBarrelDisplay.FERMENTATION_BARREL_DISPLAY,
            EntryStacks.of(ObjectRegistry.FERMENTATION_BARREL.get()));
        registry
            .addWorkstations(ApplePressDisplay.APPLE_PRESS_DISPLAY, EntryStacks.of(ObjectRegistry.APPLE_PRESS.get()));
        registry.addWorkstations(
            ApplePressFermentingDisplay.APPLE_PRESS_DISPLAY,
            EntryStacks.of(ObjectRegistry.APPLE_PRESS.get()));
    }

    public static void registerDisplays(DisplayRegistry registry) {
        registry.registerRecipeFiller(
            FermentationBarrelRecipe.class,
            FermentationBarrelRecipe.Type,
            FermentationBarrelDisplay::new);
        registry
            .registerRecipeFiller(ApplePressMashingRecipe.class, ApplePressMashingRecipe.Type, ApplePressDisplay::new);
        registry.registerRecipeFiller(
            ApplePressFermentingRecipe.class,
            ApplePressFermentingRecipe.Type,
            ApplePressFermentingDisplay::new);
    }

    public static List<Ingredient> ingredients(Recipe<RecipeInput> recipe, ItemStack stack) {
        List<Ingredient> l = new ArrayList<>(recipe.getIngredients());
        l.add(0, Ingredient.of(stack.getItem()));
        return l;
    }

}
