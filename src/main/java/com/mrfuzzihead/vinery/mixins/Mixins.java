package com.mrfuzzihead.vinery.mixins;

import javax.annotation.Nonnull;

import com.gtnewhorizon.gtnhmixins.builders.IBaseTransformer.Phase;
import com.gtnewhorizon.gtnhmixins.builders.IMixins;
import com.gtnewhorizon.gtnhmixins.builders.MixinBuilder;

/**
 * Every mixin Vinery applies, grouped by concern and tagged with the phase it runs in.
 *
 * <p>
 * Names are resolved relative to the {@code package} of the matching mixin config, so entries
 * here are short names, not fully-qualified class names:
 *
 * <ul>
 * <li>{@link Phase#EARLY} entries live in {@code com.mrfuzzihead.vinery.mixins.early} and are
 * applied by {@link EarlyMixinsLoader} before the game is built — use these for Minecraft,
 * Forge and Vinery's own classes.
 * <li>{@link Phase#LATE} entries live in {@code com.mrfuzzihead.vinery.mixins.late} and are
 * applied by {@link LateMixinsLoader} once all mods are present — use these for
 * third-party mod classes, gated with {@link MixinBuilder#addRequiredMod}.
 * </ul>
 *
 * <h2>Why the checklist below is not live yet</h2>
 *
 * Two hard rules govern this file, both learned by hitting them:
 *
 * <ol>
 * <li>A listed name that does not resolve to a compiled class is a {@code ClassNotFoundException}
 * during mod construction — it kills the game before the main menu.
 * <li>A builder with <b>no</b> mixins is rejected outright by {@code MixinBuilder.validateBuilder}
 * ("No mixin class registered for IMixins"), so placeholder entries are not possible either.
 * </ol>
 *
 * So a group appears here only once every one of its mixins is ported and compiled. The 1.21
 * originals are parked in {@code src/port-holding/java/com/mrfuzzihead/vinery/mixins/}. To enable a
 * group: port its classes to 1.7.10, move them into
 * {@code src/main/java/com/mrfuzzihead/vinery/mixins/early|late/}, and add the matching constant.
 *
 * <p>
 * BACKPORT_PLAN.md section 6 tracks what each unported mixin is expected to become — several will
 * not be mixins at all on 1.7.10 and are marked as removals.
 */
// spotless:off
public enum Mixins implements IMixins {

    // ---- Porting checklist (add a constant per group once its mixins are compiled) --------------
    //
    // EARLY / common — vanilla block behaviour
    //     MINECRAFT_BLOCKS(
    //         new MixinBuilder("Vanilla block behaviour patches")
    //             .addCommonMixins("BlockMixin", "PlantBlockMixin", "SpreadingSnowyDirtBlockMixin")),
    //   all three are removals: BlockMixin becomes canConnectFenceTo/canConnectRedstone overrides,
    //   PlantBlockMixin moves into our own BlockBush subclass, SpreadingSnowyDirtBlockMixin is dropped.
    //
    // EARLY / common — vanilla item use
    //     MINECRAFT_ITEMS(
    //         new MixinBuilder("Vanilla item use patches")
    //             .addCommonMixins("BoneMealItemMixin")),
    //   ShovelItemMixin is a removal: Et Futurum's BlockDirtPath plus our own ItemSpade replaces it.
    //
    // EARLY / common — entity behaviour
    //     MINECRAFT_ENTITIES(
    //         new MixinBuilder("Vanilla entity behaviour patches")
    //             .addCommonMixins("LivingEntityMixin")),
    //   ExperienceOrbMixin is a removal: becomes a PlayerPickupXpEvent handler.
    //
    // EARLY / common — winemaker spawning
    //     MINECRAFT_WORLDGEN(
    //         new MixinBuilder("Winemaker spawning patch")
    //             .addCommonMixins("WanderingTraderManagerMixin")),
    //   a removal: no wandering trader exists in 1.7.10, so this becomes our own spawn routine.
    //
    // EARLY / client — player behaviour
    //     MINECRAFT_CLIENT_PLAYER(
    //         new MixinBuilder("Client player behaviour patches")
    //             .addClientMixins("ClientPlayerEntityMixin")),
    //   retargeted at EntityPlayerSP.
    //
    // EARLY / client — winemaker armour models
    //     VINERY_ARMOUR_CLIENT(
    //         new MixinBuilder("Winemaker armour model patches")
    //             .addClientMixins(
    //                 "BootsItemMixin", "ChestplateItemMixin", "HelmetItemMixin", "LeggingsItemMixin")),
    //   these target Vinery's own item classes but touch client-only rendering APIs, so they stay
    //   client mixins; the 1.7.10 equivalent is ItemArmor#getArmorTexture + RenderBiped layers.
    //
    // LATE / common — D-Mod foxes foraging grape bushes
    //     DMOD_FOX_BERRIES(
    //         Phase.LATE,
    //         new MixinBuilder("Lets D-Mod foxes forage Vinery grape bushes")
    //             .addCommonMixins("dmod.FoxEntityEatSweetBerriesGoalMixin")
    //             .addRequiredMod(TargetMods.DMOD)),
    //   possibly unnecessary: D-Mod exposes Compat.registerBerryBushHandler for exactly this.

    /**
     * No mixins are ported yet. The enum must still declare at least its scaffolding, and an empty
     * enum body is valid, so this stays until the first group above lands.
     */
    ;
    // spotless:on

    private final MixinBuilder builder;

    Mixins(MixinBuilder builder) {
        this(Phase.EARLY, builder);
    }

    Mixins(Phase phase, MixinBuilder builder) {
        this.builder = builder.setPhase(phase);
    }

    @Nonnull
    @Override
    public MixinBuilder getBuilder() {
        return builder;
    }
}
