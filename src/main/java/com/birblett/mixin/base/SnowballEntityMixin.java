package com.birblett.mixin.base;

import com.birblett.helper.Ability;
import com.birblett.interfaces.AbilityUser;
import com.birblett.interfaces.ProjectileInterface;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.dragon.EnderDragonPart;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SnowballEntity.class)
public abstract class SnowballEntityMixin extends ThrownItemEntity implements ProjectileInterface, AbilityUser {

    @Unique
    private float damage = 0;

    public SnowballEntityMixin(EntityType<? extends ThrownItemEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public void setDamage(float f) {
        this.damage = f;
    }

    @Inject(method = "onEntityHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;serverDamage(Lnet/minecraft/entity/damage/DamageSource;F)V"), cancellable = true)
    private void damageTarget(EntityHitResult entityHitResult, CallbackInfo ci, @Local Entity entity) {
        if (entity.getWorld() instanceof ServerWorld world && this.damage > 0) {
            if (entity instanceof LivingEntity e) {
                if (this.hasAbility(Ability.BOSS_FLAG) && e.isBlocking() && e instanceof PlayerEntity p) {
                    if (p.getMainHandStack().isOf(Items.SHIELD)) {
                        p.disableShield(p.getMainHandStack());
                    }
                    if (p.getOffHandStack().isOf(Items.SHIELD)) {
                        p.disableShield(p.getOffHandStack());
                    }
                }
                e.timeUntilRegen = 9;
                e.hurtTime = 0;
            } else if (entityHitResult.getEntity() instanceof EnderDragonPart e) {
                e.owner.timeUntilRegen = 9;
                e.owner.hurtTime = 0;
            }
            entity.damage(world, this.getDamageSources().thrown(this, this.getOwner()), this.damage);
            ci.cancel();
        }
    }

    @Inject(method = "onCollision", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/projectile/thrown/SnowballEntity;getWorld()Lnet/minecraft/world/World;"), cancellable = true)
    private void cancelCollision(HitResult hitResult, CallbackInfo ci) {
        if (!this.getWorld().isClient && hitResult instanceof BlockHitResult && this.hasAbility(Ability.NOCLIP)) {
            ci.cancel();
        }
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if (this.damage > 0) {
            nbt.putFloat("EpicModDamage", this.damage);
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("EpicModDamage", NbtElement.FLOAT_TYPE)) {
            this.damage = nbt.getFloat("EpicModDamage");
        }
    }
}
