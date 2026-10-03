package com.mrfuzzihead.vinery.core.block;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * A soil slab (grass, dirt, coarse dirt, dirt path).
 *
 * <p>
 * 1.7.10 ships vanilla slabs for stone, wood and stone-brick but not for the soils, and has no dirt
 * path at all — that one comes from Et Futurum (see BACKPORT_PLAN.md section 4.15).
 *
 * <p>
 * Like vanilla, the half and double variants are two instances of one class, and the item form is
 * a single {@link net.minecraft.item.ItemSlab} registered against the half variant. 1.7.10 handles
 * the drop by looking up the item of the block, so each instance needs to know its sibling —
 * {@link #pair(Block, Block)} is called once during registration.
 *
 * <p>
 * The texture comes from the vanilla grass/dirt atlas rather than a copy, so an FTB texture pack
 * that swaps dirt or grass also swaps these slabs.
 */
public class SoilSlabBlock extends BlockSlab {

    private static final Map<Block, Block> SIBLINGS = new HashMap<>();

    /**
     * Links a soil's half and double instances so the double variant can find the item form, which
     * is always registered against the half block. Only the double is mapped: the half simply refers
     * to itself, which keeps the item lookup on a single code path.
     */
    public static void pair(Block half, Block doubled) {
        SIBLINGS.put(doubled, half);
    }

    public SoilSlabBlock(boolean isDouble, Material material, String texture) {
        super(isDouble, material);
        setBlockTextureName(texture);
        setHardness(0.5F);
        setStepSound(soundTypeGravel);
    }

    @Override
    public String func_150002_b(int meta) {
        return getUnlocalizedName();
    }

    @Override
    public Item getItemDropped(int meta, Random random, int fortune) {
        // The item form is the ItemSlab registered against the half-slab block for this soil.
        Block half = SIBLINGS.get(this);
        return Item.getItemFromBlock(half != null ? half : this);
    }

    @Override
    protected ItemStack createStackedBlock(int meta) {
        return new ItemStack(getItemDropped(meta, null, 0), 2, meta & 7);
    }
}
