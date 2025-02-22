package com.birblett.mixin.extra;

import com.birblett.EpicMod;
import com.birblett.helper.Ability;
import com.birblett.interfaces.AbilityUser;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin {

    @Inject(method = "onStruckByLightning", at = @At("HEAD"))
    private void yup(ServerWorld world, LightningEntity lightning, CallbackInfo ci) {

    }

    @WrapOperation(method = "onStruckByLightning", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;damage(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/damage/DamageSource;F)Z"))
    private boolean bossLightning(Entity instance, ServerWorld serverWorld, DamageSource damageSource, float v, Operation<Boolean> original, @Local(argsOnly = true) LightningEntity lightning) {
        AbilityUser a = (AbilityUser) lightning;
        v = a.hasAbility(Ability.OWNED) || a.hasAbility(Ability.BOSS_FLAG) ? 15.0f : v;
        return original.call(instance, serverWorld, damageSource, v);
    }

    @Inject(method = "isTouchingWater", at = @At("HEAD"), cancellable = true)
    private void stopTouchingWater(CallbackInfoReturnable<Boolean> cir) {
        if (((AbilityUser) this).hasAbility(Ability.IGNORE_WATER)) {
            cir.setReturnValue(false);
        }
    }

}
