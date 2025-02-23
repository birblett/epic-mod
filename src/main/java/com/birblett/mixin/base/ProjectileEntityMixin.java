package com.birblett.mixin.base;

import com.birblett.helper.Ability;
import com.birblett.interfaces.AbilityUser;
import com.birblett.interfaces.OwnedProjectile;
import com.birblett.interfaces.ProjectileInterface;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.dragon.EnderDragonPart;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ProjectileEntity.class)
public abstract class ProjectileEntityMixin implements ProjectileInterface, AbilityUser {

    @Shadow
    private boolean leftOwner;
    @Shadow
    protected abstract void onCollision(HitResult hitResult);

    @Unique
    private Double life = null;

    @Override
    public void setLife(double life) {
        this.life = life;
    }

    @Inject(method = "onBlockHit", at = @At("HEAD"))
    private void dismountRiders(BlockHitResult blockHitResult, CallbackInfo ci) {
        ProjectileEntity p = (ProjectileEntity) (Object) this;
        if (!p.getWorld().isClient && p.getFirstPassenger() instanceof LivingEntity e) {
            this.leftOwner = false;
            e.setPosition(p.getPos().subtract(p.getVelocity().normalize().multiply(0.05)));
            e.dismountVehicle();
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void tickLife(CallbackInfo ci) {
        if (this.life != null) {
            ProjectileEntity self = (ProjectileEntity) (Object) this;
            if (this.life > 0) {
                this.life -= Math.max(self.getVelocity().length(), 0.25);
            } else {
                this.onCollision(new BlockHitResult(self.getPos(), Direction.getFacing(self.getVelocity()), self.getBlockPos(), false));
                if (!(self instanceof TridentEntity || self instanceof ArrowEntity)) {
                    self.discard();
                }
            }
        }
    }

    @Inject(method = "onEntityHit", at = @At("HEAD"), cancellable = true)
    private void onEntityHitEvents(EntityHitResult entityHitResult, CallbackInfo ci) {
        Entity et = entityHitResult.getEntity();
        if (this.hasAbility(Ability.SUMMON_LIGHTNING) && et.getWorld() instanceof ServerWorld world) {
            ProjectileEntity p = ((ProjectileEntity) (Object) this);
            summonLightning(world, p.getPos(), p);
            ci.cancel();
        }
        if (this.hasAbility(Ability.IGNORE_IFRAMES)) {
            if (entityHitResult.getEntity() instanceof LivingEntity e) {
                e.timeUntilRegen = 9;
                e.hurtTime = 0;
            } else if (entityHitResult.getEntity() instanceof EnderDragonPart e) {
                e.owner.timeUntilRegen = 9;
                e.owner.hurtTime = 0;
            }
        }
        if (this.hasAbility(Ability.SUMMON_ARROWS)) {

        }
    }

    @Inject(method = "onBlockHit", at = @At("HEAD"), cancellable = true)
    private void onBlockHitEvents(BlockHitResult blockHitResult, CallbackInfo ci) {
        ProjectileEntity p = (ProjectileEntity) (Object) this;
        if (this.hasAbility(Ability.SUMMON_LIGHTNING) && p.getWorld() instanceof ServerWorld world) {
            summonLightning(world, blockHitResult.getPos(), p);
            ci.cancel();
        }
    }

    @Unique
    private static void summonLightning(ServerWorld world, Vec3d pos, ProjectileEntity p) {
        LightningEntity entity = new LightningEntity(EntityType.LIGHTNING_BOLT, world);
        entity.setPosition(pos);
        world.spawnEntity(entity);
        ((AbilityUser) entity).addAbilities(Ability.OWNED);
        if (p.getOwner() instanceof LivingEntity e) {
            ((OwnedProjectile) entity).setProjectileOwner(e);
        }
        p.discard();
    }

    @Inject(method = "onCollision", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/projectile/ProjectileEntity;onBlockHit(Lnet/minecraft/util/hit/BlockHitResult;)V"), cancellable = true)
    private void cancelCollision(HitResult hitResult, CallbackInfo ci) {
        if (this.hasAbility(Ability.NOCLIP)) {
            ci.cancel();
        }
    }

    @Inject(method = "onCollision", at = @At("TAIL"))
    private void discardAfterCollision(HitResult hitResult, CallbackInfo ci) {
        ProjectileEntity p = (ProjectileEntity) (Object) this;
        if (!p.isRemoved() && this.hasAbility(Ability.DISCARD_AFTER)) {
            p.discard();
        }
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void writeCustomData(NbtCompound nbt, CallbackInfo ci) {
        if (this.life != null) {
            nbt.putDouble("EpicModLife", this.life);
        }
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void readCustomData(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.contains("EpicModLife", NbtElement.DOUBLE_TYPE)) {
            this.life = nbt.getDouble("EpicModLife");
        }
    }

}
