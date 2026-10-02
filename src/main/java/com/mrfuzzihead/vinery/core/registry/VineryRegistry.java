package com.mrfuzzihead.vinery.core.registry;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.tileentity.TileEntity;

import com.mrfuzzihead.vinery.Vinery;

import cpw.mods.fml.common.registry.GameRegistry;

/**
 * Thin wrapper over {@code GameRegistry} so every registration in the mod goes through one place.
 *
 * <p>
 * Two 1.7.10 behaviours are worth remembering:
 *
 * <ul>
 * <li>{@code GameData} prefixes registered names with the active mod id, so
 * {@code registerBlock(b, "dark_cherry_planks")} becomes {@code vinery_dark_cherry_planks}.
 * Names are <b>not</b> namespaced on 1.7.10 — passing a name containing a colon triggers an
 * FML "illegal extra prefix" warning.
 * <li>Textures are resolved separately from registry names, so blocks and items must be given an
 * explicit {@code vinery:<path>} icon name.
 * </ul>
 *
 * <p>
 * Note the return types forced by 1.7.10's {@code GameRegistry}: {@code registerBlock} returns
 * the erased {@code Block}, and both {@code registerItem} and {@code registerTileEntity} return
 * {@code void}. The wrappers below hide that behind something usable.
 */
public final class VineryRegistry {

    private static int blockCount;
    private static int itemCount;

    private VineryRegistry() {}

    /** How many blocks have been registered so far. Reported once during {@code postInit}. */
    public static int blockCount() {
        return blockCount;
    }

    /** How many items have been registered so far. Reported once during {@code postInit}. */
    public static int itemCount() {
        return itemCount;
    }

    /** Registers a block together with a standard {@link ItemBlock}. */
    public static <T extends Block> T block(T block, String name) {
        return block(block, ItemBlock.class, name);
    }

    /**
     * Registers a block with a specific {@link ItemBlock} subclass.
     *
     * @param itemClass the item class to auto-create, or {@code null} to register the block with no
     *                  item form at all (a slab registers this way, because a single ItemSlab covers
     *                  both the half and double variants)
     */
    @SuppressWarnings("unchecked")
    public static <T extends Block> T block(T block, Class<? extends ItemBlock> itemClass, String name) {
        if (itemClass == null) {
            GameRegistry.registerBlock(block, (Class<? extends ItemBlock>) null, name);
        } else {
            GameRegistry.registerBlock(block, itemClass, name);
        }
        blockCount++;
        return block;
    }

    /** Registers an item. {@code GameRegistry.registerItem} returns void, so the item is returned as passed. */
    public static <T extends Item> T item(T item, String name) {
        GameRegistry.registerItem(item, name);
        itemCount++;
        return item;
    }

    public static <T extends TileEntity> Class<? super T> tileEntity(Class<T> tileEntityClass, String name) {
        GameRegistry.registerTileEntity(
            tileEntityClass,
            Vinery.rl(name)
                .toString());
        return tileEntityClass;
    }
}
