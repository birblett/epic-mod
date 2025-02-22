package com.birblett.mixin.base;

import com.birblett.helper.GunHoe;
import com.birblett.helper.Util;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HoeItem.class)
public abstract class HoeItemMixin extends MiningToolItem {

    protected HoeItemMixin(ToolMaterial material, TagKey<Block> effectiveBlocks, float attackDamage, float attackSpeed, Settings settings) {
        super(material, effectiveBlocks, attackDamage, attackSpeed, settings);
    }

    @Override
    public ActionResult use(World w, PlayerEntity user, Hand hand) {
        int i;
        if (user instanceof ServerPlayerEntity player && (i = this.tryShoot(player, user.getStackInHand(hand))) > 0) {
            if (i == 1) {
                user.swingHand(hand, true);
                return ActionResult.SUCCESS;
            } else {
                return ActionResult.FAIL;
            }
        }
        return super.use(w, user, hand);
    }

    @Inject(method = "useOnBlock", at = @At("HEAD"), cancellable = true)
    private void useGun(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
        int i;
        if (context.getPlayer() instanceof ServerPlayerEntity player && (i = this.tryShoot(player, context.getStack())) > 0) {
            if (i == 1) {
                player.swingHand(context.getHand(), true);
                cir.setReturnValue(ActionResult.SUCCESS);
            } else {
                cir.setReturnValue(ActionResult.FAIL);
            }
        }
    }

    @Unique
    private int tryShoot(ServerPlayerEntity player, ItemStack stack) {
        for (var k : GunHoe.GUN_HOES.keySet()) {
            if (Util.hasEnchant(stack, k, player.getWorld())) {
                int cd = GunHoe.GUN_HOES.get(k).apply(player, stack);
                if (cd > 0) {
                    player.getItemCooldownManager().set(stack, cd);
                }
                return cd >= 0 ? 1 : 2;
            }
        }
        return 0;
    }

}
