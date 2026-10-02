package com.mrfuzzihead.vinery.proxy;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

/**
 * Side-agnostic hook for the mod lifecycle.
 *
 * <p>
 * The client implementation lives in {@link ClientProxy}; this base class is what a dedicated
 * server gets, so every method here must stay free of {@code net.minecraft.client} references.
 */
public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {}

    public void init(FMLInitializationEvent event) {}

    public void postInit(FMLPostInitializationEvent event) {}

    /**
     * True when running on a physical client. Callers use this instead of
     * {@code FMLCommonHandler.instance().getSide().isClient()} so that dedicated-server-only code
     * stays out of the bytecode path.
     */
    public boolean isClient() {
        return false;
    }
}
