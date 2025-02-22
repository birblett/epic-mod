package com.birblett.interfaces;

import net.minecraft.entity.LivingEntity;

public interface ProjectileInterface {

    default void setLife(double life) {}
    default void setOriginalSlot(int slot) {}
    default void setDamage(float f) {}
    default void setTargetY(Double i) {}

}
