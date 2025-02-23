package com.birblett.interfaces;

import net.minecraft.entity.LivingEntity;

public interface OwnedProjectile {

    default void setProjectileOwner(LivingEntity e) {};
    default LivingEntity getProjectileOwner() { return null; };

}
