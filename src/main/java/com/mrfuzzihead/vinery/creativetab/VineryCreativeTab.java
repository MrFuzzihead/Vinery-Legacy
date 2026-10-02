package com.mrfuzzihead.vinery.creativetab;

import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.mrfuzzihead.vinery.core.registry.VineryBlocks;
import com.mrfuzzihead.vinery.core.registry.VineryItems;

/**
 * Vinery's creative tab.
 *
 * <p>
 * 1.7.10 predates {@code getSortedBlocks}/{@code getSortedItems}: the tab's contents come from
 * {@link #displayAllReleventItems}, which scans the global block and item registries. That means
 * Vinery's blocks appear automatically, and only the slab — registered without an item form — has to
 * be added by hand.
 */
public class VineryCreativeTab extends CreativeTabs {

    public VineryCreativeTab(String label) {
        super(label);
    }

    @Override
    public Item getTabIconItem() {
        return VineryItems.DARK_CHERRY_PLANKS_ITEM;
    }

    @Override
    public void displayAllReleventItems(List<ItemStack> items) {
        super.displayAllReleventItems(items);
        // The half slab has no ItemBlock of its own — ItemSlab is its item form — so it would
        // otherwise be missing from the tab.
        items.add(new ItemStack(VineryItems.DARK_CHERRY_SLAB));
        items.add(new ItemStack(VineryItems.DARK_CHERRY_SLAB, 1, 8));
        items.add(new ItemStack(VineryBlocks.DARK_CHERRY_DOUBLE_SLAB));
    }
}
