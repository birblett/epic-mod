package com.birblett.mixin.base;

import com.birblett.helper.Ability;
import com.birblett.helper.tracked_values.ArrowRain;
import com.birblett.interfaces.AbilityUser;
import com.birblett.interfaces.ProjectileInterface;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.entity.Leashable;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
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

    @Override
    public void tryCreateArrowRain(Vec3d pos) {
        PersistentProjectileEntity p = ((PersistentProjectileEntity) (Object) this);
        if (this.hasAbility(Ability.SUMMON_ARROWS) && p.getOwner() instanceof LivingEntity owner && p.getWorld() instanceof
                ServerWorld world && p.getWeaponStack() != null) {
            new ArrowRain(owner, world, pos, p.getWeaponStack().copy(), p.getItemStack().copy());
            this.removeAbility(Ability.SUMMON_ARROWS);
        }
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
        if (this.hasAbility(Ability.SUMMON_LIGHTNING)) {
            ci.cancel();
        }
    }

}
