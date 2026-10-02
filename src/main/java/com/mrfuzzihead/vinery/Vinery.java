package com.mrfuzzihead.vinery;

import net.minecraft.util.ResourceLocation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mrfuzzihead.vinery.core.registry.VineryBlocks;
import com.mrfuzzihead.vinery.core.registry.VineryItems;
import com.mrfuzzihead.vinery.core.registry.VineryRegistry;
import com.mrfuzzihead.vinery.creativetab.VineryCreativeTab;
import com.mrfuzzihead.vinery.proxy.CommonProxy;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

/**
 * Mod entry point.
 *
 * <p>
 * 1.7.10 Forge is lifecycle-driven rather than event-bus driven: everything that creates
 * registry objects has to happen during {@code preInit}, because the game blocks on it before
 * constructing the world. Items in particular must exist before {@code init}, because crafting
 * and loot tables resolve during that phase.
 */
@Mod(modid = Vinery.MOD_ID, name = Vinery.MOD_NAME, version = Tags.VERSION, acceptedMinecraftVersions = "[1.7.10]")
public class Vinery {

    public static final String MOD_ID = "vinery";
    public static final String MOD_NAME = "Vinery";

    @Mod.Instance(MOD_ID)
    public static Vinery instance;

    @SidedProxy(
        clientSide = "com.mrfuzzihead.vinery.proxy.ClientProxy",
        serverSide = "com.mrfuzzihead.vinery.proxy.CommonProxy")
    public static CommonProxy proxy;

    public static final Logger LOG = LogManager.getLogger("Vinery");

    /** Vinery's creative tab. 1.7.10 has no static tab registry, so it is created and held here. */
    public static VineryCreativeTab CREATIVE_TAB;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOG.info("Vinery preInit");
        // 1.7.10 has no deferred registry: everything must be created here, before the game builds
        // the world. Order is explicit because items reference the blocks they belong to.
        CREATIVE_TAB = new VineryCreativeTab("Vinery");
        VineryBlocks.register();
        VineryItems.register();
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        LOG.info("Vinery init");
        VineryBlocks.registerRecipes();
        proxy.init(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        LOG.info("Vinery postInit");
        LOG.info("Registered {} block(s) and {} item(s)", VineryRegistry.blockCount(), VineryRegistry.itemCount());
        proxy.postInit(event);
    }

    /** Builds a {@code vinery:<path>} namespaced location. */
    public static ResourceLocation rl(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
