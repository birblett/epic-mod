package com.birblett.mixin.base;

import com.birblett.helper.Ability;
import com.birblett.interfaces.AbilityUser;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.entity.projectile.thrown.ThrownEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ThrownEntity.class)
public abstract class ThrownEntityMixin implements AbilityUser {

    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/projectile/ProjectileUtil;getCollision(Lnet/minecraft/entity/Entity;Ljava/util/function/Predicate;)Lnet/minecraft/util/hit/HitResult;"))
    private HitResult hitResult(HitResult original) {
        if (original instanceof BlockHitResult b && this.hasAbility(Ability.NOCLIP)) {
            return BlockHitResult.createMissed(b.getPos(), b.getSide(), b.getBlockPos());
        }
        return original;
    }

}
