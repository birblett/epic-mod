package com.birblett.mixin.extra;

import com.birblett.helper.Ability;
import com.birblett.interfaces.AbilityUser;
import net.minecraft.entity.passive.AnimalEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AnimalEntity.class)
public class AnimalEntityMixin {

    @Inject(method = "canImmediatelyDespawn", at = @At("HEAD"), cancellable = true)
    private void allowDespawn(double distanceSquared, CallbackInfoReturnable<Boolean> cir) {
        //noinspection ConstantValue
        if (((AbilityUser) this).hasAbility(Ability.CAN_DESPAWN_WITH_PARENT) && ((AnimalEntity) (Object) this).hasPassengers() &&
                ((AbilityUser) ((AnimalEntity) (Object) this).getFirstPassenger()).hasAbilities()) {
            cir.setReturnValue(true);
        }
    }

}
