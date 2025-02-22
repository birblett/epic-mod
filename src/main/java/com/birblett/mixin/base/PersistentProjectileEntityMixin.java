package com.birblett.mixin.base;

import com.birblett.helper.Ability;
import com.birblett.helper.tracked_values.ArrowRain;
import com.birblett.interfaces.AbilityUser;
import com.birblett.interfaces.ProjectileInterface;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PersistentProjectileEntity.class)
public abstract class PersistentProjectileEntityMixin implements AbilityUser, ProjectileInterface {

    @Unique
    private Double targetY = null;

    @Override
    public void setTargetY(Double i) {
        this.targetY = i;
    }

    @ModifyExpressionValue(method = "getDragInWater", at = @At(value = "CONSTANT", args = "floatValue=0.6f"))
    private float modifyFloat(float original) {
        return !((PersistentProjectileEntity) (Object) this).getWorld().isClient && this.hasAbility(Ability.IGNORE_WATER) ? 1 : original;
    }

    @Inject(method = "onBlockHit", at = @At("HEAD"))
    private void onBlockHitEvents(BlockHitResult blockHitResult, CallbackInfo ci) {
        this.tryCreateArrowRain(blockHitResult.getPos());
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void removeNoClip(CallbackInfo ci) {
        PersistentProjectileEntity p = ((PersistentProjectileEntity) (Object) this);
        if (this.targetY != null && p.getY() < this.targetY) {
            p.setNoClip(false);
        }
    }

    @Inject(method = "onEntityHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/hit/EntityHitResult;getEntity()Lnet/minecraft/entity/Entity;"), cancellable = true)
    private void onEntityHitEvents(EntityHitResult entityHitResult, CallbackInfo ci) {
        this.tryCreateArrowRain(entityHitResult.getEntity().getPos().add(0, entityHitResult.getEntity().getHeight() / 2, 0));
        if (this.hasAbility(Ability.SUMMON_LIGHTNING)) {
            ci.cancel();
        }
    }

    @Unique
    private void tryCreateArrowRain(Vec3d pos) {
        PersistentProjectileEntity p = ((PersistentProjectileEntity) (Object) this);
        if (this.hasAbility(Ability.SUMMON_ARROWS) && p.getOwner() instanceof LivingEntity owner && p.getWorld() instanceof
                ServerWorld world && p.getWeaponStack() != null) {
            new ArrowRain(owner, world, pos, p.getWeaponStack().copy(), p.getItemStack().copy());
        }
    }

}
