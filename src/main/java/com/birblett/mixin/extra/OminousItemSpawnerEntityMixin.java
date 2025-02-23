package com.birblett.mixin.extra;

import com.birblett.interfaces.OwnedProjectile;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.OminousItemSpawnerEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(OminousItemSpawnerEntity.class)
public abstract class OminousItemSpawnerEntityMixin implements OwnedProjectile {

    @Shadow
    public abstract ItemStack getItem();

    LivingEntity owner = null;

    @Override
    public void setProjectileOwner(LivingEntity e) {
        this.owner = e;
    }

    @WrapOperation(method = "spawnItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;spawnEntity(Lnet/minecraft/entity/Entity;)Z"))
    private boolean e(ServerWorld world, Entity entity, Operation<Boolean> original) {
        if (this.getItem().isOf(Items.ENDER_PEARL) && this.owner != null) {
            entity = new EnderPearlEntity(world, this.owner, this.getItem());
            entity.noClip = true;
            entity.setVelocity(0, -0.3, 0);
            entity.setPosition(((OminousItemSpawnerEntity) (Object) (this)).getPos());
        }
        return original.call(world, entity);
    }

    @WrapOperation(method = "create", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/random/Random;nextBetween(II)I"))
    private static int createPearl(Random instance, int min, int max, Operation<Integer> original, @Local(argsOnly = true) ItemStack stack) {
        if (stack.isOf(Items.ENDER_PEARL)) {
            max = (min = 30) + 20;
        }
        return original.call(instance, min, max);
    }

}
