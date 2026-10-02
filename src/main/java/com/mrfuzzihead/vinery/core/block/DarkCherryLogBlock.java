package com.mrfuzzihead.vinery.core.block;

import net.minecraft.block.BlockRotatedPillar;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.IIcon;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Dark cherry log.
 *
 * <p>
 * A 1.7.10 {@link BlockRotatedPillar}, so all four axis rotations and the upright/wall facing
 * come for free with no custom renderer — see BACKPORT_PLAN.md section 5 tier 1.
 *
 * <p>
 * 1.7.10's pillar API splits icon lookup into {@link #getTopIcon(int)} and
 * {@link #getSideIcon(int)} rather than taking a side parameter, so the two faces are registered
 * separately and the axis check is handled by the base class.
 */
public class DarkCherryLogBlock extends BlockRotatedPillar {

    @SideOnly(Side.CLIENT)
    private IIcon sideIcon;

    @SideOnly(Side.CLIENT)
    private IIcon endIcon;

    public DarkCherryLogBlock() {
        super(Material.wood);
        setStepSound(soundTypeWood);
        setHardness(2.0F);
        setResistance(3.0F);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void registerBlockIcons(IIconRegister iconRegister) {
        sideIcon = iconRegister.registerIcon("vinery:dark_cherry_log_side");
        endIcon = iconRegister.registerIcon("vinery:dark_cherry_log_top");
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
