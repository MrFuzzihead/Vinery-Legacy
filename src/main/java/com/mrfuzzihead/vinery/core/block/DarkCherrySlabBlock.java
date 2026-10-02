package com.mrfuzzihead.vinery.core.block;

import java.util.Random;

import net.minecraft.block.BlockSlab;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.mrfuzzihead.vinery.core.registry.VineryItems;

/**
 * Dark cherry slab.
 *
 * <p>
 * Follows vanilla 1.7.10 exactly: one class, two registered instances, distinguished by the
 * {@code isDouble} constructor flag that {@link BlockSlab} carries per class. Placement on the top
 * half of a block is handled by metadata bit 8, which vanilla already manages.
 */
public class DarkCherrySlabBlock extends BlockSlab {

    public DarkCherrySlabBlock(boolean isDouble) {
        super(isDouble, Material.wood);
        setStepSound(soundTypeWood);
        setHardness(2.0F);
        setResistance(3.0F);
        setBlockTextureName("vinery:dark_cherry_planks");
    }

    /** Only ever instantiated for the half-slab instance. */
    @Override
    public String func_150002_b(int meta) {
        return getUnlocalizedName();
    }

    @Override
    public Item getItemDropped(int meta, Random random, int fortune) {
        return VineryItems.DARK_CHERRY_SLAB;
    }

    /** Slabs drop two slabs, not one, per vanilla convention. */
    @Override
    protected ItemStack createStackedBlock(int meta) {
        return new ItemStack(VineryItems.DARK_CHERRY_SLAB, 2, meta & 7);
    }
}
