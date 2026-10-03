package com.mrfuzzihead.vinery.client;

import com.mrfuzzihead.vinery.core.block.NineBottleStorageBlock;
import com.mrfuzzihead.vinery.core.block.WineRackGeometry;
import com.mrfuzzihead.vinery.core.registry.VineryBlocks;

import cpw.mods.fml.client.registry.RenderingRegistry;

/**
 * Client-only registrations.
 *
 * <p>
 * Kept apart from the common proxies so that nothing here is referenced from a class a dedicated
 * server has to load.
 */
public final class VineryClient {

    private VineryClient() {}

    /** Called from {@code ClientProxy} during client setup. */
    public static void registerRenderers() {
        // The big wine rack is the first block drawn through the box renderer; the rest of the rack
        // and lattice families follow once this path is confirmed visually.
        //
        // The id must come from Forge rather than being -1: RenderBlocks returns false for -1 before
        // it ever consults the handler map, which renders the block as nothing at all.
        int renderId = RenderingRegistry.getNextAvailableRenderId();
        NineBottleStorageBlock rack = (NineBottleStorageBlock) VineryBlocks.DARK_CHERRY_WINE_RACK_BIG;
        rack.setRenderId(renderId);

        RenderingRegistry
            .registerBlockHandler(renderId, new cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler() {

                @Override
                public void renderInventoryBlock(net.minecraft.block.Block block, int metadata, int renderPass,
                    net.minecraft.client.renderer.RenderBlocks renderBlocks) {
                    com.mrfuzzihead.vinery.client.render.BoxRenderer
                        .renderInventory(WineRackGeometry.BIG, ((NineBottleStorageBlock) block).faceIcons());
                }

                @Override
                public boolean renderWorldBlock(net.minecraft.world.IBlockAccess world, int x, int y, int z,
                    net.minecraft.block.Block block, int metadata,
                    net.minecraft.client.renderer.RenderBlocks renderBlocks) {
                    com.mrfuzzihead.vinery.client.render.BoxRenderer.renderWorld(
                        x,
                        y,
                        z,
                        WineRackGeometry.BIG,
                        ((NineBottleStorageBlock) block).faceIcons(),
                        world.getLightBrightnessForSkyBlocks(x, y, z, 0),
                        metadata & 3);
                    return true;
                }

                @Override
                public boolean shouldRender3DInInventory(int metadata) {
                    return true;
                }

                @Override
                public int getRenderId() {
                    return renderId;
                }
            });
    }
}
