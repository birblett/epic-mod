package com.birblett.mixin.extra;

import com.birblett.EpicMod;
import com.birblett.helper.Util;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HostileEntity.class)
public class HostileEntityMixin {

    @Inject(method = "getProjectileType", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/RangedWeaponItem;getHeldProjectiles()Ljava/util/function/Predicate;"), cancellable = true)
    private void overrideProjectile(ItemStack stack, CallbackInfoReturnable<ItemStack> cir) {
        HostileEntity self = (HostileEntity) (Object) this;
        if (Util.hasEnchant(self.getMainHandStack(), EpicMod.ADAPTABILITY, self.getWorld()) && Util.adaptabilityAmmo(self.getOffHandStack())) {
            cir.setReturnValue(self.getStackInHand(Hand.OFF_HAND));
        } else if (Util.hasEnchant(self.getOffHandStack(), EpicMod.ADAPTABILITY, self.getWorld()) && Util.adaptabilityAmmo(self.getMainHandStack())) {
            cir.setReturnValue(self.getStackInHand(Hand.MAIN_HAND));
        }
    }

}
