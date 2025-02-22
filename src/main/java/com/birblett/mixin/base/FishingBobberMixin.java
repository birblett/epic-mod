package com.birblett.mixin.base;

import com.birblett.helper.Ability;
import com.birblett.helper.Util;
import com.birblett.interfaces.AbilityUser;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FishingBobberEntity.class)
public abstract class FishingBobberMixin implements AbilityUser {

    @Shadow
    private int removalTimer;

    @Inject(method = "onBlockHit", at = @At("HEAD"), cancellable = true)
    private void hookOnBlock(BlockHitResult blockHitResult, CallbackInfo ci) {
        if (this.hasAbility(Ability.GRAPPLING)) {
            ci.cancel();
        }
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/projectile/FishingBobberEntity;move(Lnet/minecraft/entity/MovementType;Lnet/minecraft/util/math/Vec3d;)V"))
    private void noBobberMovement(FishingBobberEntity instance, MovementType movementType, Vec3d vec3d, Operation<Void> original) {
        if (this.hasAbility(Ability.GRAPPLING) && com.birblett.helper.Util.isTouchingBlock(instance, 0.05)) {
            instance.refreshPositionAndAngles(instance.getPos(), 0, 0);
            instance.setNoGravity(false);
            instance.setVelocity(0, 0.03, 0);
            instance.horizontalCollision = true;
            instance.velocityModified = true;
            instance.resetPosition();
            instance.velocityDirty = true;
            this.removalTimer = 0;
        } else {
            original.call(instance, movementType, vec3d);
        }
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/projectile/FishingBobberEntity;setVelocity(Lnet/minecraft/util/math/Vec3d;)V", ordinal = 4))
    private void cancelBobberMovement1(FishingBobberEntity instance, Vec3d vec3d, Operation<Void> original) {
        if (!this.hasAbility(Ability.GRAPPLING) || !com.birblett.helper.Util.isTouchingBlock(instance, 0.05)) {
            original.call(instance, vec3d);
        }
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/projectile/FishingBobberEntity;setVelocity(Lnet/minecraft/util/math/Vec3d;)V", ordinal = 5))
    private void cancelBobberMovement2(FishingBobberEntity instance, Vec3d vec3d, Operation<Void> original) {
        if (!this.hasAbility(Ability.GRAPPLING) || !com.birblett.helper.Util.isTouchingBlock(instance, 0.05)) {
            original.call(instance, vec3d);
        }
    }

    @WrapOperation(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/projectile/FishingBobberEntity;pullHookedEntity(Lnet/minecraft/entity/Entity;)V"))
    private void grapple(FishingBobberEntity instance, Entity entity, Operation<Void> original) {
        FishingBobberEntity self = (FishingBobberEntity) (Object) this;
        PlayerEntity player;
        if (this.hasAbility(Ability.GRAPPLING) && (player = self.getPlayerOwner()) != null && self.getHookedEntity() != null) {
            double touchingWaterPullSpeed = player.isTouchingWater() ? 0.4 : 1.0;
            Vec3d pullStrength = self.getHookedEntity().getPos().subtract(player.getPos()).normalize().multiply(touchingWaterPullSpeed);
            player.setVelocity(player.getVelocity().add(pullStrength));
            player.velocityModified = true;
        }
        else {
            original.call(instance, entity);
        }
    }

    @Inject(method = "use", at = @At(value = "RETURN", ordinal = 1), cancellable = true)
    private void grappleInBlock(ItemStack usedItem, CallbackInfoReturnable<Integer> cir, @Local PlayerEntity player) {
        FishingBobberEntity self = (FishingBobberEntity) (Object) this;
        if (this.hasAbility(Ability.GRAPPLING) && Util.isTouchingBlock(self, 0.05)) {
            double touchingWaterPullSpeed = player.isTouchingWater() ? 0.4 : 1.0;
            Vec3d pullStrength = self.getPos().subtract(player.getPos()).normalize().multiply(touchingWaterPullSpeed);
            player.setVelocity(player.getVelocity().add(pullStrength));
            player.velocityModified = true;
            cir.setReturnValue(1);
        }
    }

}
