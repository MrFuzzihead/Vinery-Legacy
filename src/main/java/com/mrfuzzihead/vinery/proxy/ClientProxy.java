package com.mrfuzzihead.vinery.proxy;

import com.mrfuzzihead.vinery.client.VineryClient;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;

/**
 * Client-side lifecycle hook.
 *
 * <p>
 * Referenced by name from {@code @SidedProxy}, so the class name and package must stay in sync
 * with {@link CommonProxy}.
 */
public class ClientProxy extends CommonProxy {

    @Override
    public boolean isClient() {
        return true;
    }

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        // Renderers have to be registered before texture stitching so their ids resolve.
        VineryClient.registerRenderers();
    }
}
