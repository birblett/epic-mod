package com.birblett.mixin.extra;

import com.birblett.helper.Ability;
import com.birblett.interfaces.AbilityUser;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PersistentProjectileEntity.class)
public abstract class PersistentProjectileEntityMixin implements AbilityUser {

    @ModifyExpressionValue(method = "getDragInWater", at = @At(value = "CONSTANT", args = "floatValue=0.6f"))
    private float modifyFloat(float original) {
        return !((PersistentProjectileEntity) (Object) this).getWorld().isClient && this.hasAbility(Ability.IGNORE_WATER) ? 1 : original;
    }

}
