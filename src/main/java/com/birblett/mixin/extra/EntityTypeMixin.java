package com.birblett.mixin.extra;

import com.birblett.helper.SpawnPools;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.mob.ZombifiedPiglinEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EntityType.class)
public class EntityTypeMixin {

    @WrapOperation(method = "spawn(Lnet/minecraft/server/world/ServerWorld;Ljava/util/function/Consumer;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/SpawnReason;ZZ)Lnet/minecraft/entity/Entity;", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;spawnEntityAndPassengers(Lnet/minecraft/entity/Entity;)V"))
    private void entitySpawnHandler(ServerWorld instance, Entity entity, Operation<Void> original) {
        switch (entity) {
            case ZombieEntity z -> entity = z instanceof ZombifiedPiglinEntity ? entity : SpawnPools.ZOMBIES.getRandomEntry().apply(z);
            case SkeletonEntity s -> entity = SpawnPools.SKELETONS.getRandomEntry().apply(s);
            default -> {}
        }
        original.call(instance, entity);
    }

}
