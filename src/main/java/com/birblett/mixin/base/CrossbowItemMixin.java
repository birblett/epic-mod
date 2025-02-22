package com.birblett.mixin.base;

import com.birblett.EpicMod;
import com.birblett.helper.PlayerTicker;
import com.birblett.helper.Util;
import com.birblett.helper.tracked_values.BurstFire;
import com.birblett.interfaces.ServerPlayerEntityInterface;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.component.type.ChargedProjectilesComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CrossbowItem.class)
public abstract class CrossbowItemMixin {

    @Inject(method = "shootAll", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/CrossbowItem;shootAll(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/util/Hand;Lnet/minecraft/item/ItemStack;Ljava/util/List;FFZLnet/minecraft/entity/LivingEntity;)V"))
    private void setBurst(World world, LivingEntity shooter, Hand hand, ItemStack stack, float speed, float divergence, LivingEntity target, CallbackInfo ci, @Local ChargedProjectilesComponent component) {
        if (shooter instanceof ServerPlayerEntity player && Util.hasEnchant(stack, EpicMod.BURST_FIRE, player.getServerWorld())) {
            BurstFire t = (BurstFire) ((ServerPlayerEntityInterface) player).getTickers(PlayerTicker.ID.BURST_FIRE);
            int duration = 4 + 4 * Util.getEnchantLevel(stack, EpicMod.BURST_FIRE, player.getServerWorld());
            t.setUsing(stack, hand, duration);
            t.setShooting(component.getProjectiles(), speed, divergence);
            player.getItemCooldownManager().set(stack, duration + 1);
        }
    }

}
