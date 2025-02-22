package com.birblett.mixin.extra;

import com.birblett.helper.CustomItems;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public class ItemMixin {

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void useEvents(World world, PlayerEntity user, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        ItemStack stack = user.getStackInHand(hand);
        if (world instanceof ServerWorld serverWorld) {
            if (stack.get(DataComponentTypes.CUSTOM_DATA) instanceof NbtComponent n) {
                NbtCompound nbt = n.getNbt();
                if (nbt.contains(CustomItems.USE)) {
                    int key = nbt.getInt(CustomItems.USE);
                    CustomItems.EventResult result = CustomItems.USE_EVENTS.getOrDefault(key, (p, i, h, w) ->
                            new CustomItems.EventResult(0, null)).apply(user, stack, hand, serverWorld);
                    if (result.cooldown() >= 0) {
                        user.getItemCooldownManager().set(stack, result.cooldown());
                    }
                    if (result.result() != null) {
                        cir.setReturnValue(result.result());
                    }
                }
            }
        }
    }

    @Inject(method = "useOnBlock", at = @At("HEAD"), cancellable = true)
    private void useOnBlockEvents(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
        if (context.getPlayer() instanceof ServerPlayerEntity user) {
            ItemStack stack = context.getStack();
            if (stack.get(DataComponentTypes.CUSTOM_DATA) instanceof NbtComponent n) {
                NbtCompound nbt = n.getNbt();
                if (nbt.contains(CustomItems.USE)) {
                    int key = nbt.getInt(CustomItems.USE);
                    CustomItems.EventResult result = CustomItems.USE_ON_BLOCK_EVENTS.getOrDefault(key, (p, i, h, w) ->
                            new CustomItems.EventResult(0, null)).apply(user, stack, context.getBlockPos(), user.getServerWorld());
                    if (result.cooldown() >= 0) {
                        user.getItemCooldownManager().set(stack, result.cooldown());
                    }
                    if (result.result() != null) {
                        cir.setReturnValue(result.result());
                    }
                }
            }
        }
    }

    @Inject(method = "usageTick", at = @At("HEAD"))
    private void usageTickEvents(World world, LivingEntity player, ItemStack stack, int remainingUseTicks, CallbackInfo ci) {
        if (player instanceof ServerPlayerEntity user) {
            if (stack.get(DataComponentTypes.CUSTOM_DATA) instanceof NbtComponent n) {
                NbtCompound nbt = n.getNbt();
                if (nbt.contains(CustomItems.USE)) {
                    int key = nbt.getInt(CustomItems.USE);
                    CustomItems.EventResult result = CustomItems.USE_TICK_EVENTS.getOrDefault(key, (p, i, h, w) ->
                            new CustomItems.EventResult(0, null)).apply(user, stack, user.getStackInHand(Hand.MAIN_HAND) ==
                                    stack ? Hand.MAIN_HAND : Hand.OFF_HAND, user.getServerWorld());
                    if (result.cooldown() >= 0) {
                        user.getItemCooldownManager().set(stack, result.cooldown());
                    }
                    if (result.result() != null) {
                        ci.cancel();
                    }
                }
            }
        }
    }

    @Inject(method = "inventoryTick", at = @At("HEAD"))
    private void tickEvents(ItemStack stack, World world, Entity entity, int slot, boolean selected, CallbackInfo ci) {
        if (world instanceof ServerWorld serverWorld && entity instanceof ServerPlayerEntity user) {
            if (!user.getItemCooldownManager().isCoolingDown(stack) && stack.get(DataComponentTypes.CUSTOM_DATA) instanceof NbtComponent n) {
                NbtCompound nbt = n.getNbt();
                if (nbt.contains(CustomItems.USE)) {
                    int key = nbt.getInt(CustomItems.USE);
                    CustomItems.EventResult result = CustomItems.TICK_EVENTS.getOrDefault(key, (p, i, h, w) ->
                            new CustomItems.EventResult(0, null)).apply(user, stack, selected, serverWorld);
                    if (result.cooldown() >= 0 && user instanceof PlayerEntity player) {
                        player.getItemCooldownManager().set(stack, result.cooldown());
                    }
                    if (result.result() != null) {
                        ci.cancel();
                    }
                }
            }
        }
    }

}
