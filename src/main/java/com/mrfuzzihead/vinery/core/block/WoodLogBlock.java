package com.mrfuzzihead.vinery.core.block;

import net.minecraft.block.BlockRotatedPillar;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.IIcon;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * A wood log, parameterised by texture.
 *
 * <p>
 * Vinery spans a dozen wood types, each with a log and a stripped log, so a single
 * texture-parameterised class covers all of them instead of two dozen near-identical classes.
 *
 * <p>
 * 1.7.10's pillar API splits icon lookup into {@link #getTopIcon(int)} and
 * {@link #getSideIcon(int)} rather than taking a side parameter, and the base class already handles
 * the four axis rotations and the upright/wall facing.
 */
public class WoodLogBlock extends BlockRotatedPillar {

    private final String sideTexture;
    private final String endTexture;

    @SideOnly(Side.CLIENT)
    private IIcon sideIcon;

    @SideOnly(Side.CLIENT)
    private IIcon endIcon;

    public WoodLogBlock(String sideTexture, String endTexture) {
        super(Material.wood);
        this.sideTexture = sideTexture;
        this.endTexture = endTexture;
        setStepSound(soundTypeWood);
        setHardness(2.0F);
        setResistance(3.0F);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void registerBlockIcons(IIconRegister iconRegister) {
        sideIcon = iconRegister.registerIcon(sideTexture);
        endIcon = iconRegister.registerIcon(endTexture);
    }

    @SideOnly(Side.CLIENT)
    @Override
    protected IIcon getTopIcon(int meta) {
        return endIcon;
    }

    @SideOnly(Side.CLIENT)
    @Override
    protected IIcon getSideIcon(int meta) {
        return sideIcon;
    }
}
