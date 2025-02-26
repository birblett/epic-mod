package com.birblett.mixin.extra;

import com.birblett.helper.Ability;
import com.birblett.interfaces.AbilityUser;
import net.minecraft.entity.mob.SkeletonEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkeletonEntity.class)
public abstract class SkeletonEntityMixin implements AbilityUser {

    @Inject(method = "setConversionTime", at = @At("HEAD"), cancellable = true)
    private void stopConversion(int time, CallbackInfo ci) {
        if (this.hasAbility(Ability.SKELETON_IGNORE_SNOW)) {
            ci.cancel();
        }
    }

}
