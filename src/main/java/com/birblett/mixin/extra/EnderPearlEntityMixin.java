package com.birblett.mixin.extra;

import com.birblett.helper.Ability;
import com.birblett.helper.Util;
import com.birblett.interfaces.AbilityUser;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.util.hit.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnderPearlEntity.class)
public class EnderPearlEntityMixin {

    @Inject(method = "onCollision", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/projectile/thrown/EnderPearlEntity;playTeleportSound(Lnet/minecraft/world/World;Lnet/minecraft/util/math/Vec3d;)V", ordinal = 1))
    private void updateBotPathing(HitResult hitResult, CallbackInfo ci, @Local Entity e) {
        if (e instanceof MobEntity m && ((AbilityUser) e).hasAbility(Ability.BOT) && m.getTarget() instanceof LivingEntity l) {
            m.getNavigation().findPathTo(l, 0);
            m.getNavigation().recalculatePath();
            Util.mobBreakBlocks(m, 2, 0);
        }
    }

}
