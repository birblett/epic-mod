package com.birblett.mixin.base;

import com.birblett.helper.Util;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(CrossbowItem.class)
public abstract class CrossbowItemMixin {

    @WrapOperation(method = "shootAll", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/CrossbowItem;shootAll(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/util/Hand;Lnet/minecraft/item/ItemStack;Ljava/util/List;FFZLnet/minecraft/entity/LivingEntity;)V"))
    private void weaponFiredEvent(CrossbowItem instance, ServerWorld serverWorld, LivingEntity shooter, Hand hand, ItemStack stack, List<ItemStack> projectiles, float speed, float divergence, boolean crit, LivingEntity target, Operation<Void> original) {
        original.call(instance, serverWorld, shooter, hand, stack, projectiles, speed, divergence, crit, target);
        Util.rangedWeaponFired(serverWorld, shooter, stack, hand, projectiles, speed, divergence, 1, crit);
    }

}
