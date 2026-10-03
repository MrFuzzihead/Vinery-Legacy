package com.mrfuzzihead.vinery.core.registry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    private static final List<Block> ALL_BLOCKS = new ArrayList<>();
    private static final List<Item> ALL_ITEMS = new ArrayList<>();

    private VineryRegistry() {}

    /** Every block registered so far, in registration order. Used by the self-test command. */
    public static List<Block> blocks() {
        return Collections.unmodifiableList(ALL_BLOCKS);
    }

    /** Every explicitly registered item, in registration order. */
    public static List<Item> items() {
        return Collections.unmodifiableList(ALL_ITEMS);
    }

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
        // 1.7.10 has no automatic unlocalized name: Block#getUnlocalizedName is
        // "tile." + unlocalizedName, and FML never calls setBlockName. Without this every block
        // looks up "tile.null.name". The namespaced form matches the keys the lang converter emits
        // (tile.vinery.<name>.name), so the 1.21 translations carry over unchanged.
        block.setBlockName(Vinery.MOD_ID + "." + name);

        if (itemClass == null) {
            GameRegistry.registerBlock(block, (Class<? extends ItemBlock>) null, name);
        } else {
            GameRegistry.registerBlock(block, itemClass, name);
        }
        blockCount++;
        ALL_BLOCKS.add(block);
        return block;
    }

    /** Registers an item. {@code GameRegistry.registerItem} returns void, so the item is returned as passed. */
    public static <T extends Item> T item(T item, String name) {
        // Standalone items need the same treatment; ItemBlock-derived ones inherit the block's name.
        item.setUnlocalizedName(Vinery.MOD_ID + "." + name);
        GameRegistry.registerItem(item, name);
        itemCount++;
        ALL_ITEMS.add(item);
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
