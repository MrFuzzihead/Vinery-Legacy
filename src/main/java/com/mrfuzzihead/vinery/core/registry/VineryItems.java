package com.mrfuzzihead.vinery.core.registry;

import net.minecraft.item.Item;
import net.minecraft.item.ItemSlab;

import com.mrfuzzihead.vinery.core.block.DarkCherrySlabBlock;

/**
 * Every item Vinery registers, plus handles for the items auto-created for blocks.
 *
 * <p>
 * Each block in {@link VineryBlocks} gets a matching item handle here so recipes, the creative
 * tab and JEI have something to reference. Slabs are the exception: {@link ItemSlab} needs both the
 * half and double block instances, so it is constructed by hand instead of auto-created.
 *
 * <p>
 * {@link #register()} must run after {@link VineryBlocks#register()}.
 */
public final class VineryItems {

    private VineryItems() {}

    // -- Block items -----------------------------------------------------------------------------

    public static Item DARK_CHERRY_PLANKS_ITEM;
    public static Item DARK_CHERRY_LOG_ITEM;
    public static Item DARK_CHERRY_LEAVES_ITEM;
    public static Item DARK_CHERRY_STAIRS_ITEM;
    public static Item DARK_CHERRY_DOOR_ITEM;
    public static Item DARK_CHERRY_TRAPDOOR_ITEM;
    public static Item DARK_CHERRY_BUTTON_ITEM;
    public static Item DARK_CHERRY_PRESSURE_PLATE_ITEM;
    public static Item DARK_CHERRY_FENCE_GATE_ITEM;
    public static Item DARK_CHERRY_SLAB;

    public static void register() {
        DARK_CHERRY_PLANKS_ITEM = Item.getItemFromBlock(VineryBlocks.DARK_CHERRY_PLANKS);
        DARK_CHERRY_LOG_ITEM = Item.getItemFromBlock(VineryBlocks.DARK_CHERRY_LOG);
        DARK_CHERRY_LEAVES_ITEM = Item.getItemFromBlock(VineryBlocks.DARK_CHERRY_LEAVES);
        DARK_CHERRY_STAIRS_ITEM = Item.getItemFromBlock(VineryBlocks.DARK_CHERRY_STAIRS);
        DARK_CHERRY_DOOR_ITEM = Item.getItemFromBlock(VineryBlocks.DARK_CHERRY_DOOR);
        DARK_CHERRY_TRAPDOOR_ITEM = Item.getItemFromBlock(VineryBlocks.DARK_CHERRY_TRAPDOOR);
        DARK_CHERRY_BUTTON_ITEM = Item.getItemFromBlock(VineryBlocks.DARK_CHERRY_BUTTON);
        DARK_CHERRY_PRESSURE_PLATE_ITEM = Item.getItemFromBlock(VineryBlocks.DARK_CHERRY_PRESSURE_PLATE);
        DARK_CHERRY_FENCE_GATE_ITEM = Item.getItemFromBlock(VineryBlocks.DARK_CHERRY_FENCE_GATE);

        DARK_CHERRY_SLAB = VineryRegistry.item(
            new ItemSlab(
                VineryBlocks.DARK_CHERRY_SLAB,
                (DarkCherrySlabBlock) VineryBlocks.DARK_CHERRY_SLAB,
                (DarkCherrySlabBlock) VineryBlocks.DARK_CHERRY_DOUBLE_SLAB,
                true),
            "dark_cherry_slab");
    }
}
