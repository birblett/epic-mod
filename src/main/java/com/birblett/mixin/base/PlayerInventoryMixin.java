package com.birblett.mixin.base;

import com.birblett.EpicMod;
import com.birblett.helper.Util;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(PlayerInventory.class)
public class PlayerInventoryMixin {

    @Shadow @Final public PlayerEntity player;
    @Unique ItemStack savedStack;

    @ModifyArg(method = "dropAll", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;dropItem(Lnet/minecraft/item/ItemStack;ZZ)Lnet/minecraft/entity/ItemEntity;"))
    private ItemStack excludeSoulboundFromDropPool(ItemStack stack) {
        this.savedStack = ItemStack.EMPTY;
        if (Util.hasEnchant(stack, EpicMod.SOULBOUND, this.player.getWorld())) {
            this.savedStack = stack;
            return this.savedStack != ItemStack.EMPTY ? ItemStack.EMPTY : stack;
        }
        return stack;
    }

    @ModifyArg(method = "dropAll", at = @At(value = "INVOKE", target = "Ljava/util/List;set(ILjava/lang/Object;)Ljava/lang/Object;"))
    private Object keepSoulboundItems(Object stack) {
        return this.savedStack;
    }

}
