package com.birblett.mixin.base;

import com.birblett.EpicMod;
import com.birblett.helper.PlayerTicker;
import com.birblett.helper.Util;
import com.birblett.interfaces.ItemSpoofer;
import com.birblett.interfaces.ServerPlayerEntityInterface;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.LlamaSpitEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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

    @WrapOperation(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;setCurrentHand(Lnet/minecraft/util/Hand;)V"))
    private void spoofItem(PlayerEntity user, Hand hand, Operation<Void> original) {
        original.call(user, hand);
        if (user instanceof ServerPlayerEntity player && hand == Hand.MAIN_HAND && Util.adaptabilityAmmo(user.getProjectileType(user
                .getStackInHand(hand)))) {
            ((ItemSpoofer) player.networkHandler).mainHand();
        }
    }

    @Inject(method = "onStoppedUsing", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/BowItem;load(Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/LivingEntity;)Ljava/util/List;"), cancellable = true)
    private void cancelAdaptability(ItemStack stack, World world, LivingEntity user, int remainingUseTicks, CallbackInfoReturnable<Boolean> cir, @Local(ordinal = 1) ItemStack proj, @Local float progress) {
        if (progress < 0.7f && Util.adaptabilityAmmo(proj)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "onStoppedUsing", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;incrementStat(Lnet/minecraft/stat/Stat;)V"))
    private void recoil(ItemStack stack, World world, LivingEntity user, int remainingUseTicks, CallbackInfoReturnable<Boolean> cir, @Local float f) {
        if (world instanceof ServerWorld serverWorld && Util.hasEnchant(stack, EpicMod.MIGHTY_WIND, world)) {
            for (int i = 0; i < Math.ceil(f * 3); i++) {
                LlamaSpitEntity llamaSpitEntity = new LlamaSpitEntity(EntityType.LLAMA_SPIT, serverWorld);
                llamaSpitEntity.setOwner(user);
                llamaSpitEntity.setPosition(user.getEyePos().x, user.getEyeY() - 0.1F, user.getEyePos().z);
                llamaSpitEntity.setVelocity(user.getRotationVector().multiply(f * 2));
                llamaSpitEntity.setInvisible(true);
                serverWorld.spawnEntity(llamaSpitEntity);
                llamaSpitEntity.kill(serverWorld);
            }
            serverWorld.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ENTITY_BREEZE_SHOOT, user
                    .getSoundCategory(), 0.7F, 1.4F + (user.getRandom().nextFloat() - user
                    .getRandom().nextFloat()) * 0.2F);
            user.setVelocity(user.getVelocity().multiply(0.7).add(user.getRotationVector().multiply(-1.2 * f)));
            user.velocityModified = true;
            serverWorld.getOtherEntities(user, new Box(user.getEyePos().add(-5, -5, -5), user.getEyePos()
                    .add(5, 5, 5)), e -> e.getPos().squaredDistanceTo(user.getEyePos().add(user.getRotationVector()
                    .multiply(2.5))) <= 9).forEach(e -> {
                e.addVelocity(user.getRotationVector().multiply(Math.min(1, f)));
                e.velocityModified = true;
            });
        }
    }

}