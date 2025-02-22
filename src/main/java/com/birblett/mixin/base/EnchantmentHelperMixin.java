package com.birblett.mixin.base;

import com.birblett.EpicMod;
import com.birblett.helper.Util;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EnchantmentHelper.class)
public class EnchantmentHelperMixin {

    @ModifyExpressionValue(method = "getTridentSpinAttackStrength", at = @At(value = "INVOKE", target = "Lorg/apache/commons/lang3/mutable/MutableFloat;floatValue()F"))
    private static float addSlipstream(float f, @Local(argsOnly = true) ItemStack stack, @Local(argsOnly = true) LivingEntity user) {
        int i;
        if (user instanceof ServerPlayerEntity player && (i = Util.getEnchantLevel(stack, EpicMod.SLIPSTREAM, player.getServerWorld())) > 0) {
            f += (i + 1) * 0.5f;
        }
        return f;
    }

}
