package com.mrfuzzihead.vinery.core.block;

import java.util.List;

import net.minecraft.block.BlockLeaves;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Dark cherry leaves.
 *
 * <p>
 * Extends {@link BlockLeaves} directly and implements the same three members vanilla's
 * {@code BlockOldLeaf}/{@code BlockNewLeaf} do: {@link #func_150125_e()} names the textures,
 * {@link #registerBlockIcons} resolves them into {@code field_150129_M}, and {@link #getIcon} picks
 * between the two variants by metadata.
 *
 * <p>
 * 1.7.10 metadata carries the variant: 0 fast (decaying), 1 slow (permanent). The icon array is
 * indexed by {@code meta & 3}, so the four 1.21 stage textures map straight onto the decay stages
 * vanilla already tracks, giving progressive wear for free.
 */
public class DarkCherryLeavesBlock extends BlockLeaves {

    /** Decay stage textures, indexed by metadata. */
    private static final String[] TEXTURES = { "vinery:dark_cherry_leaves_0", // fresh / permanently decaying
        "vinery:dark_cherry_leaves_1", // slow, fully grown
        "vinery:dark_cherry_leaves_2", "vinery:dark_cherry_leaves_3", };

    public DarkCherryLeavesBlock() {
        super();
        setStepSound(soundTypeGrass);
        setHardness(0.2F);
        setLightOpacity(1);
    }

    @Override
    public String[] func_150125_e() {
        return TEXTURES;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void registerBlockIcons(IIconRegister iconRegister) {
        this.field_150129_M[0] = new IIcon[TEXTURES.length];
        this.field_150129_M[1] = new IIcon[TEXTURES.length];

        for (int variant = 0; variant < 2; variant++) {
            for (int stage = 0; stage < TEXTURES.length; stage++) {
                this.field_150129_M[variant][stage] = iconRegister.registerIcon(TEXTURES[stage]);
            }
        }
    }

    @SideOnly(Side.CLIENT)
    @Override
    public IIcon getIcon(int side, int meta) {
        return this.field_150129_M[this.field_150127_b][meta & 3];
    }

    @Override
    public void getSubBlocks(Item item, CreativeTabs tab, List<ItemStack> list) {
        list.add(new ItemStack(item, 1, 0));
        list.add(new ItemStack(item, 1, 1));
    }
}
