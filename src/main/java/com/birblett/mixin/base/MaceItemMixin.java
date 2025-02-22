package com.birblett.mixin.base;

import com.birblett.EpicMod;
import com.birblett.helper.PlayerTicker;
import com.birblett.helper.Util;
import com.birblett.interfaces.ServerPlayerEntityInterface;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MaceItem;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(MaceItem.class)
public abstract class MaceItemMixin extends Item {

    public MaceItemMixin(Settings settings) {
        super(settings);
    }

    public ActionResult use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (Util.hasEnchant(stack, EpicMod.LEAPING, world) && player instanceof ServerPlayerEntity user &&
                ((ServerPlayerEntityInterface) user).getTickers(PlayerTicker.ID.LEAPING).get() == 0) {
            user.setOnGround(false);
            ((ServerPlayerEntityInterface) user).getTickers(PlayerTicker.ID.LEAPING).set(1);
            user.setVelocity(user.getVelocity().x, 1.0, user.getVelocity().z);
            user.velocityModified = true;
            user.getItemCooldownManager().set(stack, 30);
            stack.damage(1, user, LivingEntity.getSlotForHand(hand));
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }

}
