package com.birblett.mixin.extra;

import com.birblett.helper.Ability;
import com.birblett.interfaces.AbilityUser;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.EvokerFangsEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EvokerFangsEntity.class)
public class EvokerFangsEntityMixin {

    @Inject(method = "damage(Lnet/minecraft/entity/LivingEntity;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;isTeammate(Lnet/minecraft/entity/Entity;)Z"), cancellable = true)
    private void ignoreIframes(LivingEntity target, CallbackInfo ci) {
        if (((AbilityUser) this).hasAbility(Ability.BOSS_FLAG) && ((AbilityUser) target).hasAbility(Ability.BOSS_FLAG)) {
            ci.cancel();
        } else if (((AbilityUser) this).hasAbility(Ability.IGNORE_IFRAMES)) {
            target.timeUntilRegen = 9;
            target.hurtTime = 0;
        }
    }

}
