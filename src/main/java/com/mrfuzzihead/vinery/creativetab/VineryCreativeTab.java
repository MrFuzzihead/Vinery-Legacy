package com.mrfuzzihead.vinery.creativetab;

import java.util.Iterator;
import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.mrfuzzihead.vinery.core.registry.VineryItems;

/**
 * Vinery's creative tab.
 *
 * <p>
 * 1.7.10 predates {@code getSortedBlocks}/{@code getSortedItems}: the tab's contents come from
 * {@link #displayAllReleventItems}, which scans the global block and item registries.
 *
 * <p>
 * Vinery registers ten blocks with a {@code null} ItemBlock (every slab — the half and double
 * variants share one {@link net.minecraft.item.ItemSlab}, exactly as vanilla does for stone slabs).
 * {@code new ItemStack(block)} on one of those produces a stack whose item is null, which does not
 * throw here but crashes the first time NEI or JEI renders it: their render context stringifies the
 * stack and NPEs. So only stacks with a real item are added, and the same guard is applied to
 * whatever the vanilla scan produced.
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

        // Slabs have no ItemBlock of their own, so their ItemSlab would be missed by the scan. Both
        // placements of a half slab are listed, as vanilla does for wooden slabs.
        items.add(new ItemStack(VineryItems.DARK_CHERRY_SLAB));
        items.add(new ItemStack(VineryItems.DARK_CHERRY_SLAB, 1, 8));
        items.add(new ItemStack(VineryItems.GRASS_SLAB));
        items.add(new ItemStack(VineryItems.DIRT_SLAB));
        items.add(new ItemStack(VineryItems.COARSE_DIRT_SLAB));

        dropItemlessStacks(items);
    }

    /**
     * Removes any stack without an item. Cheap insurance: a single bad entry takes the whole creative
     * screen down, and the failure surfaces as an NPE inside someone else's renderer.
     */
    private static void dropItemlessStacks(List<ItemStack> items) {
        Iterator<ItemStack> iterator = items.iterator();
        while (iterator.hasNext()) {
            ItemStack stack = iterator.next();
            if (stack == null || stack.getItem() == null) {
                iterator.remove();
            }
        }
    }
}
