package com.birblett.interfaces;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public interface RangedWeapon {

    ProjectileEntity createProjectile(World world, LivingEntity shooter, ItemStack weaponStack, ItemStack projectileStack, boolean critical);

}
