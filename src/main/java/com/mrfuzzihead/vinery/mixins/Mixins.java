package com.mrfuzzihead.vinery.mixins;

import javax.annotation.Nonnull;

import com.gtnewhorizon.gtnhmixins.builders.IBaseTransformer.Phase;
import com.gtnewhorizon.gtnhmixins.builders.IMixins;
import com.gtnewhorizon.gtnhmixins.builders.MixinBuilder;

/**
 * Every mixin Vinery applies, grouped by concern and tagged with the phase it runs in.
 *
 * <p>Names are resolved relative to the {@code package} of the matching mixin config, so entries
 * here are short names, not fully-qualified class names:
 *
 * <ul>
 *   <li>{@link Phase#EARLY} entries live in {@code com.mrfuzzihead.vinery.mixins.early} and are
 *       applied by {@link EarlyMixinsLoader} before the game is built — use these for Minecraft,
 *       Forge and Vinery's own classes.
 *   <li>{@link Phase#LATE} entries live in {@code com.mrfuzzihead.vinery.mixins.late} and are
 *       applied by {@link LateMixinsLoader} once all mods are present — use these for
 *       third-party mod classes, gated with {@link MixinBuilder#addRequiredMod}.
 * </ul>
 *
 * <p>Mixins marked "planned removal" are on the 1.7.10 port's hit list: the behaviour they patch
 * has a native Forge equivalent and the class gets deleted once that port lands. See
 * BACKPORT_PLAN.md section 6.
 */
// spotless:off
public enum Mixins implements IMixins {

    // ------------------------------------------------------------------------------------------
    // Early: Minecraft / Forge / Vinery classes
    // ------------------------------------------------------------------------------------------

    /** Vanilla block behaviour (connection suppression, plant placement). */
    MINECRAFT_BLOCKS(
        new MixinBuilder("Vanilla block behaviour patches")
            .addCommonMixins(
                "BlockMixin", // planned removal: becomes BlockFence#canConnectFenceTo / Block#canConnectRedstone overrides
                "PlantBlockMixin", // planned removal: 1.7.10 has BlockBush, handled in our own block class
                "SpreadingSnowyDirtBlockMixin")), // planned removal: no snowy dirt in 1.7.10

    /** Vanilla item use / tool behaviour. */
    MINECRAFT_ITEMS(
        new MixinBuilder("Vanilla item use patches")
            .addCommonMixins(
                "BoneMealItemMixin",
                "ShovelItemMixin")), // planned removal: Et Futurum BlockDirtPath + our own ItemSpade

    /** Entity behaviour: wine effects and XP orbs. */
    MINECRAFT_ENTITIES(
        new MixinBuilder("Vanilla entity behaviour patches")
            .addCommonMixins(
                "LivingEntityMixin",
                "ExperienceOrbMixin")), // planned removal: becomes a PlayerPickupXpEvent handler

    /** Winemaker spawning. */
    MINECRAFT_WORLDGEN(
        new MixinBuilder("Winemaker spawning patch")
            .addCommonMixins(
                "WanderingTraderManagerMixin")), // planned removal: becomes our own spawn routine

    /** Client-only player behaviour (double jump, etc.). */
    MINECRAFT_CLIENT_PLAYER(
        new MixinBuilder("Client player behaviour patches")
            .addClientMixins(
                "ClientPlayerEntityMixin")),

    /** Winemaker armour models. Targets our own item classes but uses client-only rendering APIs. */
    VINERY_ARMOUR_CLIENT(
        new MixinBuilder("Winemaker armour model patches")
            .addClientMixins(
                "BootsItemMixin",
                "ChestplateItemMixin",
                "HelmetItemMixin",
                "LeggingsItemMixin")),

    // ------------------------------------------------------------------------------------------
    // Late: third-party mod classes (applied only when the target mod is present)
    // ------------------------------------------------------------------------------------------

    /** D-Mod foxes eating Vinery grape bushes. */
    DMOD_FOX_BERRIES(
        Phase.LATE,
        new MixinBuilder("Lets D-Mod foxes forage Vinery grape bushes")
            .addCommonMixins("dmod.FoxEntityEatSweetBerriesGoalMixin")
            .addRequiredMod(TargetMods.DMOD));
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