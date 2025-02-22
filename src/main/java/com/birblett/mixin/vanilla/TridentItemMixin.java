package com.birblett.mixin.vanilla;

import com.birblett.interfaces.ProjectileInterface;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.TridentItem;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TridentItem.class)
public class TridentItemMixin {

    @ModifyExpressionValue(method = "onStoppedUsing", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/projectile/ProjectileEntity;spawnWithVelocity(Lnet/minecraft/entity/projectile/ProjectileEntity$ProjectileCreator;Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/LivingEntity;FFF)Lnet/minecraft/entity/projectile/ProjectileEntity;"))
    private ProjectileEntity modifyTrident(ProjectileEntity original, @Local(ordinal = 0, argsOnly = true) ItemStack stack, @Local(ordinal = 0, argsOnly = true) LivingEntity user) {
        if (user instanceof ServerPlayerEntity player) {
            ((ProjectileInterface) original).setOriginalSlot(stack == player.getOffHandStack() ? 40 : player.getInventory().getSlotWithStack(stack));
        }
        return original;
    }

}
