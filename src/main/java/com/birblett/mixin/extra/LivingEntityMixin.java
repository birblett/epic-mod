package com.birblett.mixin.extra;

import com.birblett.helper.Ability;
import com.birblett.interfaces.AbilityUser;
import com.birblett.interfaces.Mage;
import net.minecraft.entity.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity implements AbilityUser, Mage {

    @Unique
    private int internalCooldown = -2;
    @Unique
    private boolean teleporting = false;
    @Unique
    private boolean attacking = false;

    public LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Override
    public void setCooldown(int i) {
        this.internalCooldown = i;
    }

    @Override
    public boolean isCooledDown() {
        return this.internalCooldown <= 0;
    }

    @Override
    public void setTeleporting(boolean teleporting) {
        this.teleporting = teleporting;
    }

    @Override
    public boolean teleporting() {
        return this.teleporting;
    }

    @Override
    public void setAttack(boolean attacking) {
        this.attacking = attacking;
    }

    @Override
    public boolean attacking() {
        return this.attacking;
    }

    @Inject(method = "isInvulnerableTo", at = @At("HEAD"), cancellable = true)
    private void ignoreDamageAbilities(ServerWorld world, DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if (this.hasAbility(Ability.IGNORE_LIGHTNING) && source.isOf(DamageTypes.LIGHTNING_BOLT)
                || this.hasAbility(Ability.IGNORE_FALL) && (source.isOf(DamageTypes.FALL))
                || this.hasAbility(Ability.IGNORE_FIRE) && (source.isOf(DamageTypes.IN_FIRE) || source.isOf(DamageTypes.ON_FIRE))
                || this.hasAbility(Ability.IGNORE_EXPLOSION) && (source.isOf(DamageTypes.EXPLOSION) || source.isOf(DamageTypes.PLAYER_EXPLOSION))
                || this.hasAbility(Ability.IGNORE_SUFFOCATION) && source.isOf(DamageTypes.IN_WALL)) {
            cir.setReturnValue(true);
        } else if (this.hasAbility(Ability.BOSS_FLAG) && source.getAttacker() != null && ((AbilityUser) source.getAttacker())
                .hasAbility(Ability.BOSS_FLAG)) {
            cir.setReturnValue(true);
        } else if ((Object) (this) instanceof MobEntity m) {
            if (this.hasAbility(Ability.LINE_OF_SIGHT_DAMAGE)) {
                cir.setReturnValue(m.getTarget() == null || !m.canSee(m.getTarget()));
            } else if (this.hasAbility(Ability.IGNORE_FAR_DAMAGE) && m.getTarget() != null && m.squaredDistanceTo(m.getTarget()) >= 2500) {
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void tickEventAbilities(CallbackInfo ci) {
        if (this.internalCooldown > 0) {
            --this.internalCooldown;
        }
    }

    @Inject(method = "damage", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/damage/DamageSource;getAttacker()Lnet/minecraft/entity/Entity;"))
    private void damageEventAbilities(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        for (Ability a : this.getAbilities()) {
            switch (a) {
                case Ability.ZOMBIE_KING -> {
                    if ((Entity) this instanceof ZombieEntity zombie) {
                        if (this.internalCooldown <= 0) {
                            this.internalCooldown = 50;
                            int i = MathHelper.floor(this.getX());
                            int j = MathHelper.floor(this.getY());
                            int k = MathHelper.floor(this.getZ());
                            EntityType<? extends ZombieEntity> entityType = zombie.getType();
                            ZombieEntity zombieEntity = entityType.create(world, SpawnReason.REINFORCEMENT);
                            if (zombieEntity == null) {
                                break;
                            }
                            for(int l = 0; l < 50; ++l) {
                                int m = i + MathHelper.nextInt(this.random, 7, 40) * MathHelper.nextInt(this.random, -1, 1);
                                int n = j + MathHelper.nextInt(this.random, 7, 40) * MathHelper.nextInt(this.random, -1, 1);
                                int o = k + MathHelper.nextInt(this.random, 7, 40) * MathHelper.nextInt(this.random, -1, 1);
                                BlockPos blockPos = new BlockPos(m, n, o);
                                if (SpawnRestriction.isSpawnPosAllowed(entityType, world, blockPos)) {
                                    zombieEntity.setPosition(m, n, o);
                                    if (!world.isPlayerInRange(m, n, o, 7.0) && world.doesNotIntersectEntities(zombieEntity) && world.isSpaceEmpty(zombieEntity) && !world.containsFluid(zombieEntity.getBoundingBox())) {
                                        if (source.getAttacker() instanceof LivingEntity livingEntity) {
                                            zombieEntity.setTarget(livingEntity);
                                        }
                                        zombieEntity.initialize(world, world.getLocalDifficulty(zombieEntity.getBlockPos()), SpawnReason.REINFORCEMENT, null);
                                        world.spawnEntityAndPassengers(zombieEntity);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void writeCustomData(NbtCompound nbt, CallbackInfo ci) {
        if (this.internalCooldown >= -1) {
            nbt.putInt("EpicModCooldown", this.internalCooldown);
        }
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void readCustomData(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.contains("EpicModCooldown")) {
            this.internalCooldown = nbt.getInt("EpicModCooldown");
        }
    }

}
