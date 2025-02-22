package com.birblett.interfaces;

import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public interface PhysicsProjectile {

    void setProjectileOwner(LivingEntity e);
    void releaseProjectile();
    void setBlock(BlockState state, World world, BlockPos pos);
    BlockState projectileBlockState();

}
