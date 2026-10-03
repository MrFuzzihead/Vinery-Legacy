package com.mrfuzzihead.vinery.client;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

import com.mrfuzzihead.vinery.client.render.BoxRenderer;
import com.mrfuzzihead.vinery.core.block.NineBottleStorageBlock;
import com.mrfuzzihead.vinery.core.block.WineRackGeometry;
import com.mrfuzzihead.vinery.core.registry.VineryBlocks;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
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

    /**
     * The rack's per-face icons, falling back to the block's own icons if the table was never filled.
     * registerBlockIcons only runs for blocks that own a stitched icon, so a defensive path here
     * avoids drawing nothing at all if that ever fails.
     */
    private static IIcon[] iconsFor(Block block, int metadata) {
        if (block instanceof NineBottleStorageBlock rack) {
            IIcon[] faceIcons = rack.faceIcons();
            if (faceIcons != null) {
                return faceIcons;
            }
        }
        return BoxRenderer.iconsOf(block, metadata);
    }

    public static void registerRenderers() {
        // The big wine rack is the first block drawn through the box renderer; the rest of the rack
        // and lattice families follow once this path is confirmed visually.
        //
        // The id must come from Forge rather than being -1: RenderBlocks returns false for -1 before
        // it ever consults the handler map, which renders the block as nothing at all.
        int renderId = RenderingRegistry.getNextAvailableRenderId();
        NineBottleStorageBlock rack = (NineBottleStorageBlock) VineryBlocks.DARK_CHERRY_WINE_RACK_BIG;
        rack.setRenderId(renderId);

        com.mrfuzzihead.vinery.Vinery.LOG.info(
            "[Vinery] registerRenderers: renderType(before)={} allocated renderId={}",
            VineryBlocks.DARK_CHERRY_WINE_RACK_BIG.getRenderType(),
            renderId);

        RenderingRegistry.registerBlockHandler(renderId, new ISimpleBlockRenderingHandler() {

            @Override
            public void renderInventoryBlock(Block block, int metadata, int renderPass, RenderBlocks renderer) {
                BoxRenderer.renderInventory(block, metadata, WineRackGeometry.BIG, iconsFor(block, metadata), renderer);
            }

            @Override
            public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int metadata,
                RenderBlocks renderer) {
                // Log once, but always render. An early return here would make the block appear on
                // the first chunk rebuild and then vanish, since chunks re-render on every change.
                if (!loggedFirstDraw) {
                    loggedFirstDraw = true;
                    com.mrfuzzihead.vinery.Vinery.LOG.info(
                        "[Vinery] renderWorld CALLED at x={} y={} z={} boxes={} icons={}",
                        x,
                        y,
                        z,
                        WineRackGeometry.BIG.length,
                        iconsFor(block, metadata).length);
                }

                BoxRenderer.renderWorld(
                    world,
                    x,
                    y,
                    z,
                    block,
                    metadata,
                    WineRackGeometry.BIG,
                    iconsFor(block, metadata),
                    renderer);
                return true;
            }

            @Override
            public boolean shouldRender3DInInventory(int metadata) {
                // False, matching TFC's RenderPottery and other 1.7.10 renderers that draw custom
                // geometry. RenderBlocks#renderStandardBlock needs RenderBlocks#blockAccess to
                // resolve lighting, and that field is only set during world rendering — calling it
                // from a GUI context throws a NullPointerException in getMixedBrightnessForBlock.
                // The item therefore uses the block's flat icon, which is what vanilla does for any
                // block whose item form is a sprite.
                return false;
            }

            @Override
            public int getRenderId() {
                return renderId;
            }
        });
    }

    private static boolean loggedFirstDraw;
}
