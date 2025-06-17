package com.birblett.mixin.base;

import com.birblett.EpicMod;
import com.birblett.helper.PlayerTicker;
import com.birblett.helper.Util;
import com.birblett.interfaces.ServerPlayerEntityInterface;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(BowItem.class)
public abstract class BowItemMixin extends RangedWeaponItem {

    public BowItemMixin(Settings settings) {
        super(settings);
    }

    @Override
    public void usageTick(World w, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        super.usageTick(w, user, stack, remainingUseTicks);
        if (w instanceof ServerWorld world && user instanceof ServerPlayerEntity player && Util.hasEnchant(stack, EpicMod.FOCUS, world)) {
            if (remainingUseTicks == 71981) {
                Util.playSound(world, user, SoundEvents.ENTITY_ARROW_HIT_PLAYER, 1.0f, 2.0f);
                ((ServerPlayerEntityInterface) player).getTickers(PlayerTicker.ID.FOCUS).set(5);
            }
        }
    }

    @Inject(method = "onStoppedUsing", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/BowItem;load(Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/LivingEntity;)Ljava/util/List;"), cancellable = true)
    private void cancelAdaptability(ItemStack stack, World world, LivingEntity user, int remainingUseTicks, CallbackInfoReturnable<Boolean> cir, @Local(ordinal = 1) ItemStack proj, @Local float progress) {
        if (progress < 0.7f && Util.adaptabilityAmmo(proj)) {
            cir.setReturnValue(false);
        }
    }

    @WrapOperation(method = "onStoppedUsing", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/BowItem;shootAll(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/util/Hand;Lnet/minecraft/item/ItemStack;Ljava/util/List;FFZLnet/minecraft/entity/LivingEntity;)V"))
    private void weaponFiredEvent(BowItem instance, ServerWorld serverWorld, LivingEntity shooter, Hand hand, ItemStack stack, List<ItemStack> projectiles, float speed, float divergence, boolean crit, LivingEntity target, Operation<Void> original, @Local float f) {
        original.call(instance, serverWorld, shooter, hand, stack, projectiles, speed, divergence, crit, target);
        Util.rangedWeaponFired(serverWorld, shooter, stack, hand, projectiles, speed, divergence, f, crit);
    }

}