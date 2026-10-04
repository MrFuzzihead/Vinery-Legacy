package com.mrfuzzihead.vinery.proxy;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;

import com.mrfuzzihead.vinery.client.VineryClient;
import com.mrfuzzihead.vinery.core.block.FoliageBlock;
import com.mrfuzzihead.vinery.core.registry.VineryRegistry;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

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
        FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    /**
     * Keeps the leaves' opacity flag in step with the player's graphics setting.
     *
     * <p>
     * {@code BlockLeaves#isOpaqueCube} returns the negation of a field that only
     * {@code RenderGlobal} updates, and only for {@code Blocks.leaves} and {@code Blocks.leaves2}.
     * Without this, a modded leaves block reports itself opaque for the whole session, and every
     * neighbouring block culls its faces against it — which is why leaves looked see-through from
     * below and above. Vanilla's own update happens on world load; ticking makes a mid-session
     * graphics change take effect immediately.
     *
     * @see FoliageBlock#updateGraphicsLevel(boolean)
     */
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        boolean fancyGraphics = Minecraft.getMinecraft().gameSettings.fancyGraphics;
        for (Block block : VineryRegistry.blocks()) {
            if (block instanceof FoliageBlock leaves) {
                leaves.updateGraphicsLevel(fancyGraphics);
            }
        }
    }
}
