package com.mrfuzzihead.vinery.mixins.early;

import java.util.Objects;

import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mrfuzzihead.vinery.core.registry.MobEffectRegistry;

@Mixin(ExperienceOrb.class)
public abstract class ExperienceOrbMixin {

    @Inject(method = "playerTouch", at = @At("HEAD"))
    public void onPlayerTouch(Player player, CallbackInfo ci) {
        if (player.level().isClientSide) return;
        if (player.hasEffect(MobEffectRegistry.getHolder(MobEffectRegistry.EXPERIENCE_EFFECT))) {
            int amplifier = Objects
                .requireNonNull(player.getEffect(MobEffectRegistry.getHolder(MobEffectRegistry.EXPERIENCE_EFFECT)))
                .getAmplifier();

            int multiplier = amplifier + 1;
            ExperienceOrb self = (ExperienceOrb) (Object) this;
            int bonusXp = (int) (self.getValue() * multiplier * 0.25);

            player.giveExperiencePoints(bonusXp);
        }
    }
}
