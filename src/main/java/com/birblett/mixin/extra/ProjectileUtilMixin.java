package com.birblett.mixin.extra;

import com.birblett.helper.Util;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ProjectileUtil.class)
public class ProjectileUtilMixin {

    @ModifyExpressionValue(method = "createArrowProjectile", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ArrowItem;createArrow(Lnet/minecraft/world/World;Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;)Lnet/minecraft/entity/projectile/PersistentProjectileEntity;"))
    private static PersistentProjectileEntity applyEnchants(PersistentProjectileEntity original, @Local(argsOnly = true) LivingEntity shooter, @Local(ordinal = 0, argsOnly = true) ItemStack projectileStack, @Local(ordinal = 1, argsOnly = true) ItemStack stack) {
        if (stack != null && shooter.getWorld() instanceof ServerWorld world) {
            Util.applyProjectileMods(shooter, stack, projectileStack, world, original);
        }
        return original;
    }

}
