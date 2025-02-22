package com.birblett.mixin.base;

import com.birblett.EpicMod;
import com.birblett.helper.Ability;
import com.birblett.helper.Util;
import com.birblett.interfaces.AbilityUser;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FishingRodItem.class)
public class FishingRodMixin {

    @WrapOperation(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/projectile/ProjectileEntity;spawn(Lnet/minecraft/entity/projectile/ProjectileEntity;Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/item/ItemStack;)Lnet/minecraft/entity/projectile/ProjectileEntity;"))
    private ProjectileEntity setGrappling(ProjectileEntity entity, ServerWorld world, ItemStack projectileStack, Operation<ProjectileEntity> original, @Local ItemStack stack, @Local(argsOnly = true) PlayerEntity user) {
        if (Util.hasEnchant(stack, EpicMod.GRAPPLING, world)) {
            ((AbilityUser) entity).addAbilities(Ability.GRAPPLING);
        }
        return original.call(entity, world, projectileStack);
    }

}
