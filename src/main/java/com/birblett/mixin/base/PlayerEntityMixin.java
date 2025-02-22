package com.birblett.mixin.base;

import com.birblett.EpicMod;
import com.birblett.helper.Util;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {

    @Inject(method = "getProjectileType", at = @At(value = "INVOKE", target = "Ljava/util/function/Predicate;test(Ljava/lang/Object;)Z"), cancellable = true)
    private void additionalTypes(ItemStack stack, CallbackInfoReturnable<ItemStack> cir, @Local(ordinal = 2) ItemStack stack2) {
        if ((PlayerEntity) (Object) this instanceof ServerPlayerEntity s) {
            if (Util.hasEnchant(stack, EpicMod.ADAPTABILITY, s.getWorld()) && Util.adaptabilityAmmo(stack2)) {
                cir.setReturnValue(stack2);
            }
        }
    }

}
