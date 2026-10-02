package com.mrfuzzihead.vinery.core.registry;

import net.minecraft.block.Block;

import com.mrfuzzihead.vinery.core.block.DarkCherryLogBlock;
import com.mrfuzzihead.vinery.core.block.DarkCherryPlanksBlock;
import com.mrfuzzihead.vinery.core.block.DarkCherrySlabBlock;

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
 * exercises the "vanilla path" rendering tier from BACKPORT_PLAN.md section 5 — each of these
 * renders through 1.7.10's own pipeline with no custom renderer at all.
 */
public final class VineryBlocks {

    private VineryBlocks() {}

    // -- Dark cherry wood ------------------------------------------------------------------------

    public static Block DARK_CHERRY_PLANKS;
    public static Block DARK_CHERRY_LOG;
    public static Block DARK_CHERRY_SLAB;
    public static Block DARK_CHERRY_DOUBLE_SLAB;

    public static void register() {
        DARK_CHERRY_PLANKS = VineryRegistry.block(new DarkCherryPlanksBlock(), "dark_cherry_planks");
        DARK_CHERRY_LOG = VineryRegistry.block(new DarkCherryLogBlock(), "dark_cherry_log");

        // Slabs need the half and double variants registered as separate blocks, exactly like
        // vanilla's stone_slab / double_stone_slab pair.
        //
        // Both register with NO ItemBlock: a slab's item form is a single ItemSlab covering both
        // halves, so letting FML auto-create an ItemBlock here would occupy the "dark_cherry_slab"
        // slot before VineryItems.register() can create the ItemSlab in it.
        DARK_CHERRY_SLAB = VineryRegistry.block(new DarkCherrySlabBlock(false), null, "dark_cherry_slab");
        DARK_CHERRY_DOUBLE_SLAB = VineryRegistry.block(new DarkCherrySlabBlock(true), null, "dark_cherry_double_slab");
    }
}
