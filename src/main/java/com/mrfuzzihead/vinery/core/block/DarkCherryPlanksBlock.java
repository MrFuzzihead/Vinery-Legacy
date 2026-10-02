package com.mrfuzzihead.vinery.core.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

/**
 * Dark cherry planks.
 *
 * <p>
 * Plain full cube, so it renders through 1.7.10's own cube pipeline with no custom renderer at
 * all — see BACKPORT_PLAN.md section 5 tier 1. That is what makes it replaceable by FTB texture
 * packs and correct under every shader.
 */
public class DarkCherryPlanksBlock extends Block {

    public DarkCherryPlanksBlock() {
        super(Material.wood);
        setBlockTextureName("vinery:dark_cherry_planks");
        setStepSound(soundTypeWood);
        setHardness(2.0F);
        setResistance(3.0F);
        setLightOpacity(255);
    }
}
