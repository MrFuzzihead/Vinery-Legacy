package com.mrfuzzihead.vinery.mixins;

import javax.annotation.Nonnull;

import com.gtnewhorizon.gtnhmixins.builders.ITargetMod;
import com.gtnewhorizon.gtnhmixins.builders.TargetModBuilder;

/**
 * Mods Vinery integrates with. Referenced from {@link Mixins} via
 * {@link com.gtnewhorizon.gtnhmixins.builders.MixinBuilder#addRequiredMod} so a mixin is only
 * applied when its target actually loaded.
 *
 * <p>Most of these are integrated through their published API rather than mixins — see
 * BACKPORT_PLAN.md section 4.15. They are listed here so a mixin into one of them can be added
 * later without inventing the detection logic.
 */
// spotless:off
public enum TargetMods implements ITargetMod {

    /** D-Mod (makamys) — foxes. Provides {@code Compat.registerBerryBushHandler}. */
    DMOD("dmod"),

    /** Et Futurum Requiem, GTNH fork — banners, composter, dirt paths, stripped logs, chest boats. */
    ET_FUTURUM("etfuturum");
    // spotless:on

    private final TargetModBuilder builder;

    TargetMods(String coreModClass, String modId) {
        this.builder = new TargetModBuilder().setCoreModClass(coreModClass)
            .setModId(modId);
    }

    TargetMods(String modId) {
        this.builder = new TargetModBuilder().setModId(modId);
    }

    @Nonnull
    @Override
    public TargetModBuilder getBuilder() {
        return builder;
    }
}