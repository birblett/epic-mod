package com.birblett.mixin.base;

import com.birblett.EpicMod;
import com.birblett.helper.Util;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractHorseEntity.class)
public abstract class AbstractHorseEntityMixin {

    @Shadow @Nullable public abstract LivingEntity getControllingPassenger();

    @Unique private static final Identifier RIDER_ID = Identifier.of("rider");

    @Inject(method = "tick", at = @At("HEAD"))
    private void applyRiderBonus(CallbackInfo ci) {
        LivingEntity rider = this.getControllingPassenger();
        EntityAttributeInstance i = ((AbstractHorseEntity) (Object) this).getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
        EntityAttributeInstance j = ((AbstractHorseEntity) (Object) this).getAttributeInstance(EntityAttributes.JUMP_STRENGTH);
        if (i != null) {
            if (i.hasModifier(RIDER_ID)) {
                i.removeModifier(RIDER_ID);
            }
            if (rider != null && Util.hasEnchant(rider.getEquippedStack(EquipmentSlot.FEET), EpicMod.RIDER, ((AbstractHorseEntity) (Object) this).getWorld())) {
                i.addPersistentModifier(new EntityAttributeModifier(RIDER_ID, 0.4, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
        }
        if (j != null) {
            if (j.hasModifier(RIDER_ID)) {
                j.removeModifier(RIDER_ID);
            }
            if (rider != null && Util.hasEnchant(rider.getEquippedStack(EquipmentSlot.FEET), EpicMod.RIDER, ((AbstractHorseEntity) (Object) this).getWorld())) {
                j.addPersistentModifier(new EntityAttributeModifier(RIDER_ID, 0.3, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
        }
    }

}
