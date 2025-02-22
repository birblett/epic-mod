package com.birblett.mixin.base;

import com.birblett.EpicMod;
import com.birblett.helper.Util;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.TrackTargetGoal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.SpiderEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ActiveTargetGoal.class)
public abstract class ActiveTargetGoalMixin extends TrackTargetGoal {

    public ActiveTargetGoalMixin(MobEntity mob, boolean checkVisibility) {
        super(mob, checkVisibility);
    }

    @WrapOperation(method = "getAndUpdateTargetPredicate", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/ai/TargetPredicate;setBaseMaxDistance(D)Lnet/minecraft/entity/ai/TargetPredicate;"))
    private TargetPredicate setForSpider(TargetPredicate instance, double baseMaxDistance, Operation<TargetPredicate> original) {
        return this.mob instanceof SpiderEntity ? original.call(instance, baseMaxDistance).setPredicate((target, world) -> {
            ItemStack stack = target.getEquippedStack(EquipmentSlot.FEET);
            return stack == ItemStack.EMPTY || !Util.hasEnchant(stack, EpicMod.RIDER, world);
        }) : original.call(instance, baseMaxDistance);
    }

}
