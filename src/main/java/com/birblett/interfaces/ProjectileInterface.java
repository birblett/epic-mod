package com.birblett.interfaces;

import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;

public interface ProjectileInterface {

    default void setLife(double life) {}
    default void setOriginalSlot(int slot) {}
    default void setDamage(float f) {}
    default void setTargetY(Double i) {}
    default void setItems(ItemStack weaponStack, ItemStack projectileStack) {}
    default void tryCreateArrowRain(Vec3d pos) {};

}
