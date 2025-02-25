package com.birblett.mixin.base;

import com.birblett.EpicMod;
import com.birblett.helper.PlayerTicker;
import com.birblett.helper.Util;
import com.birblett.interfaces.ServerPlayerEntityInterface;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.entity.Leashable;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.TridentItem;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TridentItem.class)
public class TridentItemMixin {

    @ModifyExpressionValue(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;isTouchingWaterOrRain()Z"))
    private boolean slipstreamTridentUse(boolean original, @Local(argsOnly = true) World world, @Local(argsOnly = true) PlayerEntity user, @Local(argsOnly = true) Hand hand) {
        return original ^ (world instanceof ServerWorld s && Util.getEnchantLevel(user.getStackInHand(hand), EpicMod.SLIPSTREAM, s) > 0);
    }

    @ModifyExpressionValue(method = "onStoppedUsing", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;isTouchingWaterOrRain()Z"))
    private boolean slipstreamTridentStopUse(boolean original, @Local(argsOnly = true) World world, @Local(argsOnly = true) ItemStack stack) {
        return original ^ (world instanceof ServerWorld s && Util.getEnchantLevel(stack, EpicMod.SLIPSTREAM, s) > 0);
    }

    @Inject(method = "onStoppedUsing", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;playSoundFromEntity(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/entity/Entity;Lnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FF)V"))
    private void updateVelocity(ItemStack stack, World world, LivingEntity user, int remainingUseTicks, CallbackInfoReturnable<Boolean> cir) {
        if (user instanceof ServerPlayerEntity player && Util.getEnchantLevel(stack, EpicMod.SLIPSTREAM, world) > 0) {
            PlayerTicker t = ((ServerPlayerEntityInterface) player).getTickers(PlayerTicker.ID.SLIPSTREAM);
            player.getItemCooldownManager().set(stack, 20 + t.get());
            t.set(t.get() + 10);
            player.velocityModified = true;
        }
    }

    @ModifyExpressionValue(method = "onStoppedUsing", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/projectile/ProjectileEntity;spawnWithVelocity(Lnet/minecraft/entity/projectile/ProjectileEntity$ProjectileCreator;Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/LivingEntity;FFF)Lnet/minecraft/entity/projectile/ProjectileEntity;"))
    private ProjectileEntity bup(ProjectileEntity original, @Local(argsOnly = true) LivingEntity user) {
        ((Leashable) original).attachLeash(user, true);
        return original;
    }

}
