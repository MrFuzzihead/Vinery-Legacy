package com.mrfuzzihead.vinery.client;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

import com.mrfuzzihead.vinery.client.render.BoxRenderer;
import com.mrfuzzihead.vinery.core.block.WineRackBlock;
import com.mrfuzzihead.vinery.core.registry.VineryRegistry;

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
        if (block instanceof WineRackBlock rack) {
            IIcon[] faceIcons = rack.faceIcons();
            if (faceIcons != null) {
                return faceIcons;
            }
        }
        return BoxRenderer.iconsOf(block, metadata);
    }

    public static void registerRenderers() {
        // The wine racks are drawn through the box renderer. Every rack — all ten woods, and later
        // the other two sizes — shares one render id: Forge dispatches on Block#getRenderType but
        // passes the actual Block to the handler, so one handler serves them all and each block
        // supplies its own geometry. Adding a rack therefore needs no change here at all.
        //
        // The id must come from Forge rather than being -1: RenderBlocks returns false for -1 before
        // it ever consults the handler map, which renders the block as nothing at all.
        int renderId = RenderingRegistry.getNextAvailableRenderId();
        for (Block block : VineryRegistry.blocks()) {
            if (block instanceof WineRackBlock rack) {
                rack.setRenderId(renderId);
            }
        }

        RenderingRegistry.registerBlockHandler(renderId, new ISimpleBlockRenderingHandler() {

            @Override
            public void renderInventoryBlock(Block block, int metadata, int renderPass, RenderBlocks renderer) {
                if (!(block instanceof WineRackBlock rack)) {
                    return;
                }
                BoxRenderer.renderInventory(block, metadata, rack.geometry(), iconsFor(block, metadata), renderer);
            }

            @Override
            public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int renderId,
                RenderBlocks renderer) {
                if (!(block instanceof WineRackBlock rack)) {
                    return false;
                }

                // The sixth argument is NOT the block metadata. Forge's
                // RenderingRegistry#renderWorldBlock passes its own `modelId` through in that slot
                // (it is the key it just looked the handler up with), so the render id arrives here
                // instead. Reading the facing from it makes the block permanently face one way.
                int metadata = world.getBlockMetadata(x, y, z);

                BoxRenderer
                    .renderWorld(world, x, y, z, block, metadata, rack.geometry(), iconsFor(block, metadata), renderer);
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

}
