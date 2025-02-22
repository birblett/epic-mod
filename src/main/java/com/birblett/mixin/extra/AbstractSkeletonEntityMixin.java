package com.birblett.mixin.extra;

import com.birblett.EpicMod;
import com.birblett.helper.Util;
import net.minecraft.entity.CrossbowUser;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.CrossbowAttackGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.AbstractSkeletonEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSkeletonEntity.class)
public abstract class AbstractSkeletonEntityMixin extends HostileEntity implements CrossbowUser {

    @Unique private final CrossbowAttackGoal<AbstractSkeletonEntityMixin> crossbowAttackGoal = new CrossbowAttackGoal<>(this, 2.0, 10);

    protected AbstractSkeletonEntityMixin(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    @ModifyArg(method = "updateAttackType", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/ai/goal/GoalSelector;add(ILnet/minecraft/entity/ai/goal/Goal;)V"), index = 1)
    private Goal goal(Goal goal) {
        if (this.isHolding(Items.CROSSBOW)) {
            goal = this.crossbowAttackGoal;
        }
        return goal;
    }

    @Inject(method = "updateAttackType", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/ai/goal/GoalSelector;remove(Lnet/minecraft/entity/ai/goal/Goal;)V"))
    private void abilityGoals(CallbackInfo ci) {

    }

    @Override
    public void setCharging(boolean charging) {
        this.setAttacking(true);
    }

    @Override
    public void postShoot() {
        this.despawnCounter = 0;
    }

    @Inject(method = "shootAt", at = @At("HEAD"), cancellable = true)
    private void crossbowShoot(LivingEntity target, float pullProgress, CallbackInfo ci) {
        if (this.isHolding(Items.CROSSBOW)) {
            this.shoot(this, 1.6F);
            ItemStack s;
            if (Util.hasEnchant(this.getOffHandStack(), EpicMod.ADAPTABILITY, this.getWorld()) && Util.adaptabilityAmmo(s = this.getMainHandStack()) || Util.hasEnchant(this.getMainHandStack(), EpicMod.ADAPTABILITY, this.getWorld()) && Util.adaptabilityAmmo(s = this.getOffHandStack())) {
                s.setCount(Math.max(2, s.getCount()));
            }
            ci.cancel();
        }
    }

}
