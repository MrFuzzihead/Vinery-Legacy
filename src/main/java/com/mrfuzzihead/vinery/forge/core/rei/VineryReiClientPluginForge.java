package com.mrfuzzihead.vinery.forge.core.rei;

import com.mrfuzzihead.vinery.core.compat.rei.VineryReiClientPlugin;

import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.forge.REIPluginClient;

@REIPluginClient
public class VineryReiClientPluginForge implements REIClientPlugin {

    @Override
    public void registerCategories(CategoryRegistry registry) {
        VineryReiClientPlugin.registerCategories(registry);
    }

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        VineryReiClientPlugin.registerDisplays(registry);
    }
}
