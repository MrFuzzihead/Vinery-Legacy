package com.mrfuzzihead.vinery.core.registry;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.mrfuzzihead.vinery.core.block.DarkCherryButtonBlock;
import com.mrfuzzihead.vinery.core.block.DarkCherryDoorBlock;
import com.mrfuzzihead.vinery.core.block.DarkCherryFenceGateBlock;
import com.mrfuzzihead.vinery.core.block.DarkCherryPlanksBlock;
import com.mrfuzzihead.vinery.core.block.DarkCherryPressurePlateBlock;
import com.mrfuzzihead.vinery.core.block.DarkCherrySlabBlock;
import com.mrfuzzihead.vinery.core.block.DarkCherryStairsBlock;
import com.mrfuzzihead.vinery.core.block.DarkCherryTrapDoorBlock;
import com.mrfuzzihead.vinery.core.block.FoliageBlock;
import com.mrfuzzihead.vinery.core.block.GrapeBushBlock;
import com.mrfuzzihead.vinery.core.block.SoilSlabBlock;
import com.mrfuzzihead.vinery.core.block.WoodLogBlock;

import cpw.mods.fml.common.registry.GameRegistry;

/**
 * Every block Vinery registers.
 *
 * <p>
 * 1.7.10 has no deferred registry, so these fields are populated by {@link #register()} during
 * {@code preInit}. They are deliberately non-final: explicit registration methods give us a
 * greppable insertion point and full control of ordering, which matters once this file reaches the
 * ~200 entries the 1.21 version had. Static initialisers would work too, but they make the
 * declaration order a silent dependency.
 *
 * <p>
 * Ported so far: the dark cherry wood set, deliberately chosen as the first slice because it
 * exercises the "vanilla path" rendering tier from BACKPORT_PLAN.md section 5 — every block here
 * renders through 1.7.10's own pipeline with no custom renderer at all.
 */
public final class VineryBlocks {

    private VineryBlocks() {}

    // -- Dark cherry wood ------------------------------------------------------------------------

    public static Block DARK_CHERRY_PLANKS;
    public static Block DARK_CHERRY_LOG;
    public static Block DARK_CHERRY_LEAVES;
    public static Block DARK_CHERRY_STAIRS;
    public static Block DARK_CHERRY_DOOR;
    public static Block DARK_CHERRY_TRAPDOOR;
    public static Block DARK_CHERRY_BUTTON;
    public static Block DARK_CHERRY_PRESSURE_PLATE;
    public static Block DARK_CHERRY_FENCE_GATE;
    public static Block DARK_CHERRY_SLAB;
    public static Block DARK_CHERRY_DOUBLE_SLAB;
    public static Block STRIPPED_DARK_CHERRY_LOG;

    public static Block APPLE_LOG;
    public static Block APPLE_LEAVES;

    public static Block RED_GRAPE_BUSH;
    public static Block WHITE_GRAPE_BUSH;
    public static Block SAVANNA_RED_GRAPE_BUSH;
    public static Block SAVANNA_WHITE_GRAPE_BUSH;
    public static Block TAIGA_RED_GRAPE_BUSH;
    public static Block TAIGA_WHITE_GRAPE_BUSH;

    public static Block GRASS_SLAB;
    public static Block DIRT_SLAB;
    public static Block COARSE_DIRT_SLAB;
    public static Block GRASS_DOUBLE_SLAB;
    public static Block DIRT_DOUBLE_SLAB;
    public static Block COARSE_DIRT_DOUBLE_SLAB;

    private static FoliageBlock darkCherryLeaves() {
        return new FoliageBlock(
            "vinery:dark_cherry_leaves_0",
            "vinery:dark_cherry_leaves_1",
            "vinery:dark_cherry_leaves_2",
            "vinery:dark_cherry_leaves_3");
    }

    public static void register() {
        // Order matters: stairs delegate their texture to the block passed to the constructor, and
        // every other entry here is referenced by the items registered in VineryItems.
        DARK_CHERRY_PLANKS = VineryRegistry.block(new DarkCherryPlanksBlock(), "dark_cherry_planks");
        DARK_CHERRY_LOG = VineryRegistry
            .block(new WoodLogBlock("vinery:dark_cherry_log_side", "vinery:dark_cherry_log_top"), "dark_cherry_log");
        DARK_CHERRY_LEAVES = VineryRegistry.block(darkCherryLeaves(), "dark_cherry_leaves");

        DARK_CHERRY_STAIRS = VineryRegistry.block(new DarkCherryStairsBlock(DARK_CHERRY_PLANKS), "dark_cherry_stairs");

        DARK_CHERRY_DOOR = VineryRegistry.block(new DarkCherryDoorBlock(), "dark_cherry_door");
        DARK_CHERRY_TRAPDOOR = VineryRegistry.block(new DarkCherryTrapDoorBlock(), "dark_cherry_trapdoor");
        DARK_CHERRY_BUTTON = VineryRegistry.block(new DarkCherryButtonBlock(), "dark_cherry_button");
        DARK_CHERRY_PRESSURE_PLATE = VineryRegistry
            .block(new DarkCherryPressurePlateBlock(), "dark_cherry_pressure_plate");
        DARK_CHERRY_FENCE_GATE = VineryRegistry.block(new DarkCherryFenceGateBlock(), "dark_cherry_fence_gate");

        // Slabs need the half and double variants registered as separate blocks, exactly like
        // vanilla's stone_slab / double_stone_slab pair.
        //
        // Both register with NO ItemBlock: a slab's item form is a single ItemSlab covering both
        // halves, so letting FML auto-create an ItemBlock here would occupy the "dark_cherry_slab"
        // slot before VineryItems.register() can create the ItemSlab in it.
        DARK_CHERRY_SLAB = VineryRegistry.block(new DarkCherrySlabBlock(false), null, "dark_cherry_slab");
        DARK_CHERRY_DOUBLE_SLAB = VineryRegistry.block(new DarkCherrySlabBlock(true), null, "dark_cherry_double_slab");

        STRIPPED_DARK_CHERRY_LOG = VineryRegistry.block(
            new WoodLogBlock("vinery:stripped_dark_cherry_log_side", "vinery:stripped_dark_cherry_log_top"),
            "stripped_dark_cherry_log");

        APPLE_LOG = VineryRegistry
            .block(new WoodLogBlock("vinery:apple_log_side", "vinery:apple_log_top"), "apple_log");
        APPLE_LEAVES = VineryRegistry.block(
            new FoliageBlock(
                "vinery:apple_leaves_0",
                "vinery:apple_leaves_1",
                "vinery:apple_leaves_2",
                "vinery:apple_leaves_3"),
            "apple_leaves");

        // Bushes only grow on soil and drop seeds once ripe; see GrapeBushBlock.
        RED_GRAPE_BUSH = VineryRegistry.block(new GrapeBushBlock("plains", true), "red_grape_bush");
        WHITE_GRAPE_BUSH = VineryRegistry.block(new GrapeBushBlock("plains", false), "white_grape_bush");
        SAVANNA_RED_GRAPE_BUSH = VineryRegistry.block(new GrapeBushBlock("savanna", true), "savanna_grape_bush_red");
        SAVANNA_WHITE_GRAPE_BUSH = VineryRegistry
            .block(new GrapeBushBlock("savanna", false), "savanna_grape_bush_white");
        TAIGA_RED_GRAPE_BUSH = VineryRegistry.block(new GrapeBushBlock("taiga", true), "taiga_grape_bush_red");
        TAIGA_WHITE_GRAPE_BUSH = VineryRegistry.block(new GrapeBushBlock("taiga", false), "taiga_grape_bush_white");

        // Soil slabs reuse the vanilla grass/dirt textures so texture packs keep working on them.
        GRASS_SLAB = VineryRegistry.block(new SoilSlabBlock(false, Material.grass, "grass"), null, "grass_slab");
        GRASS_DOUBLE_SLAB = VineryRegistry
            .block(new SoilSlabBlock(true, Material.grass, "grass"), null, "grass_double_slab");
        SoilSlabBlock.pair(GRASS_SLAB, GRASS_DOUBLE_SLAB);

        DIRT_SLAB = VineryRegistry.block(new SoilSlabBlock(false, Material.ground, "dirt"), null, "dirt_slab");
        DIRT_DOUBLE_SLAB = VineryRegistry
            .block(new SoilSlabBlock(true, Material.ground, "dirt"), null, "dirt_double_slab");
        SoilSlabBlock.pair(DIRT_SLAB, DIRT_DOUBLE_SLAB);

        COARSE_DIRT_SLAB = VineryRegistry
            .block(new SoilSlabBlock(false, Material.ground, "dirt"), null, "coarse_dirt_slab");
        COARSE_DIRT_DOUBLE_SLAB = VineryRegistry
            .block(new SoilSlabBlock(true, Material.ground, "dirt"), null, "coarse_dirt_double_slab");
        SoilSlabBlock.pair(COARSE_DIRT_SLAB, COARSE_DIRT_DOUBLE_SLAB);
    }

    /**
     * Vanilla recipes. 1.7.10 has no recipe system, so these are registered directly and replace the
     * JSON recipes of the 1.21 version (see BACKPORT_PLAN.md section 4.11).
     *
     * <p>
     * These are approximations of the 1.21 shapes, chosen to avoid depending on vanilla items
     * that Vinery does not yet provide. A faithful port needs a conversion pass over the 147 recipe
     * JSONs.
     */
    public static void registerRecipes() {
        GameRegistry.addRecipe(
            new ItemStack(DARK_CHERRY_PLANKS, 4),
            new Object[] { " L ", " L ", 'L', Item.getItemFromBlock(DARK_CHERRY_LOG) });

        GameRegistry.addRecipe(
            new ItemStack(DARK_CHERRY_PLANKS, 4),
            new Object[] { " L ", " L ", 'L', VineryItems.DARK_CHERRY_LOG_ITEM });

        // 5 planks: the classic staircase shape.
        GameRegistry.addRecipe(
            new ItemStack(DARK_CHERRY_STAIRS, 4),
            new Object[] { " P ", "PPP", "P  ", 'P', Item.getItemFromBlock(DARK_CHERRY_PLANKS) });

        GameRegistry.addRecipe(
            new ItemStack(VineryItems.DARK_CHERRY_SLAB, 6),
            new Object[] { "PPP", "   ", "   ", 'P', Item.getItemFromBlock(DARK_CHERRY_PLANKS) });

        GameRegistry.addRecipe(
            new ItemStack(DARK_CHERRY_PRESSURE_PLATE, 2),
            new Object[] { " P ", " P ", 'P', Item.getItemFromBlock(DARK_CHERRY_PLANKS) });

        GameRegistry.addRecipe(
            new ItemStack(DARK_CHERRY_DOOR, 3),
            new Object[] { "PPP", "PP ", "P  ", 'P', Item.getItemFromBlock(DARK_CHERRY_PLANKS) });

        GameRegistry.addRecipe(
            new ItemStack(DARK_CHERRY_TRAPDOOR, 2),
            new Object[] { "PPP", "PPP", "   ", 'P', Item.getItemFromBlock(DARK_CHERRY_PLANKS) });

        GameRegistry.addRecipe(
            new ItemStack(DARK_CHERRY_FENCE_GATE, 1),
            new Object[] { "P P", "PPP", "P P", 'P', Item.getItemFromBlock(DARK_CHERRY_PLANKS) });
    }
}
