package com.birblett.mixin.extra;

import com.birblett.ai.*;
import com.birblett.helper.Ability;
import com.birblett.interfaces.AbilityUser;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ZombieEntity.class)
public abstract class ZombieEntityMixin extends HostileEntity implements AbilityUser, Swimmer {

    @Unique
    private EntityNavigation swimNavigation = null;
    @Unique
    private final EntityNavigation landNavigation = this.navigation;
    @Unique
    private boolean targetingUnderwater = false;
    @Unique
    private Goal zombieAttackGoal;

    protected ZombieEntityMixin(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public boolean hasFinishedCurrentPath() {
        BlockPos blockPos;
        Path path = this.getNavigation().getCurrentPath();
        return path != null && (blockPos = path.getTarget()) != null && this.squaredDistanceTo(blockPos.getX(), blockPos.getY(), blockPos.getZ()) < 4.0;
    }

    @Override
    public void setTargetingUnderwater(boolean b) {
        this.targetingUnderwater = b;
    }

    public void updateGoals() {
        ZombieEntity z = (ZombieEntity) (Object) this;
        for (Ability a : this.getAbilities()) {
            switch(a) {
                case DASHMASTER -> {
                    this.applySwim(z, 3.5, 0.2);
                    this.goalSelector.add(1, new PounceGoal(z, 1.5f));
                    return;
                }
                case DULLAHAN -> {
                    this.goalSelector.add(1, new DullahanUnageGoal(z));
                    return;
                }
                case LUDFRU, KILLER_PILLAR, ZOMBIE_KING -> {
                    this.applySwim(z, 1, 0.1);
                    return;
                }
                case BOT -> {
                    this.applySwim(z, 3.5, 0.4);
                    this.goalSelector.add(1, new BotEnderPearlGoal(z));
                    this.goalSelector.add(1, new BotPanicPearlGoal(z));
                    this.goalSelector.add(1, new PounceGoal(z));
                    this.goalSelector.add(1, new BreakBlocksOnDamageGoal(z, 2, 0));
                    return;
                }
                case GUNNER -> {
                    this.applySwim(z, 1, 0.1);
                    if (this.zombieAttackGoal != null) {
                        this.goalSelector.remove(this.zombieAttackGoal);
                    }
                    this.goalSelector.add(1, new GunnerAttackGoal(this));
                }
                case ZOMBIE_MAGE -> {
                    this.goalSelector.add(2, new ArchmageAttackGoal(z));
                    this.goalSelector.add(2, new MageTeleportGoal(this));
                }
            }
        }
    }

    @Override
    public void updateSwimming() {
        if (!this.getWorld().isClient) {
            if (this.swimNavigation != null && this.canMoveVoluntarily() && this.isTouchingWater() && this.isTargetingUnderwater()) {
                this.navigation = this.swimNavigation;
                this.setSwimming(true);
            } else {
                this.navigation = this.landNavigation;
                this.setSwimming(false);
            }
        }
    }

    @WrapOperation(method = "initCustomGoals", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/ai/goal/GoalSelector;add(ILnet/minecraft/entity/ai/goal/Goal;)V"))
    private void getAttackGoal(GoalSelector instance, int priority, Goal goal, Operation<Void> original) {
        this.zombieAttackGoal = goal;
        original.call(instance, priority, goal);
    }

    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/mob/ZombieEntity;canConvertInWater()Z"))
    private boolean noBossConversion(boolean original) {
        return original && !this.hasAbility(Ability.ZOMBIE_IGNORE_WATER) && !this.hasAbility(Ability.BOSS_FLAG);
    }

    @Unique
    public boolean isTargetingUnderwater() {
        if (this.targetingUnderwater) {
            return true;
        }
        LivingEntity livingEntity = this.getTarget();
        return livingEntity != null && livingEntity.isTouchingWater();
    }

    @Unique
    private void applySwim(ZombieEntity z, double speed, double jump) {
        this.swimNavigation = new SwimmerNavigation(this, this.getWorld());
        this.moveControl = new WaterMoveControl(this);
        this.setPathfindingPenalty(PathNodeType.WATER, 0.0f);
        this.goalSelector.add(1, new TargetAboveWaterGoal(z, speed));
        this.goalSelector.add(2, new SwimmerLeaveWaterGoal(z, 1, jump));
    }

}
