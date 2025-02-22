package com.birblett.mixin.vanilla;

import com.birblett.helper.Util;
import com.birblett.interfaces.ProjectileInterface;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TridentEntity.class)
public abstract class TridentEntityMixin extends PersistentProjectileEntity implements ProjectileInterface {

    @Shadow
    @Final
    private static TrackedData<Byte> LOYALTY;
    @Shadow
    private boolean dealtDamage;

    @Unique
    int slot = 0;

    protected TridentEntityMixin(EntityType<? extends PersistentProjectileEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public void setOriginalSlot(int slot) {
        this.slot = slot;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void returnFromVoid(CallbackInfo ci) {
        if (this.getY() < this.getWorld().getBottomY() - 10 && this.dataTracker.get(LOYALTY) > 0) {
            this.setVelocity(0, 0, 0);
            this.dealtDamage = true;
        }
    }

    @Inject(method = "tryPickup", at = @At("HEAD"), cancellable = true)
    private void insert(PlayerEntity player, CallbackInfoReturnable<Boolean> cir) {
        boolean b = switch (this.pickupType.ordinal()) {
            default -> false;
            case 1 -> this.insertInSlot(player.getInventory(), this.asItemStack());
            case 2 -> player.isInCreativeMode();
        };
        cir.setReturnValue(b || this.isNoClip() && this.isOwner(player) && this.insertInSlot(player.getInventory(), this.asItemStack()));
    }

    @ModifyExpressionValue(method = "onEntityHit", at = @At(value = "CONSTANT", args = "floatValue=8.0f"))
    private float addPower(float f) {
        return f + 2 * Util.getEnchantLevel(this.getItemStack(), Enchantments.POWER, this.getWorld());
    }

    @Unique
    private boolean insertInSlot(PlayerInventory inventory, ItemStack stack) {
        if (this.slot == 40) {
            if (inventory.offHand.getFirst().isEmpty()) {
                inventory.offHand.set(0, stack);
                return true;
            } else {
                return inventory.insertStack(stack);
            }
        } else {
            return inventory.getStack(this.slot).isEmpty() ? inventory.insertStack(this.slot, stack) : inventory.insertStack(stack);
        }
    }

}
