package com.mrfuzzihead.vinery.forge;

import java.util.List;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import com.mrfuzzihead.vinery.core.Vinery;
import com.mrfuzzihead.vinery.core.registry.CompostableRegistry;
import com.mrfuzzihead.vinery.forge.core.config.VineryForgeConfig;
import com.mrfuzzihead.vinery.forge.core.datagen.ModAdvancementGen;
import com.mrfuzzihead.vinery.forge.core.registry.VineryNeoForgeVillagers;

@Mod(Vinery.MOD_ID)
public class VineryForge {

    public VineryForge(IEventBus modEventBus, ModContainer modContainer) {
        PlatformHelperImpl.ENTITY_TYPES.register();
        Vinery.init();

        modContainer.registerConfig(ModConfig.Type.COMMON, VineryForgeConfig.COMMON_CONFIG, "vinery.toml");

        modEventBus.register(VineryForgeConfig.class);

        VineryNeoForgeVillagers.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            CompostableRegistry.registerCompostable();
            Vinery.commonSetup();
            // VineryNeoForgeVillagers.registerPOIs();
        });
    }

    private void onGatherData(GatherDataEvent event) {
        event.addProvider(
            new ModAdvancementGen(
                event.getGenerator()
                    .getPackOutput(),
                event.getLookupProvider(),
                event.getExistingFileHelper(),
                List.of(new ModAdvancementGen.MyAdvancementGenerator())));
    }
}
