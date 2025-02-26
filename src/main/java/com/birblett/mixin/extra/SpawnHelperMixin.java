package com.birblett.mixin.extra;

import com.birblett.helper.SpawnPools;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.SpawnHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SpawnHelper.class)
public class SpawnHelperMixin {

    @WrapOperation(method = "spawnEntitiesInChunk(Lnet/minecraft/entity/SpawnGroup;Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/world/chunk/Chunk;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/world/SpawnHelper$Checker;Lnet/minecraft/world/SpawnHelper$Runner;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;spawnEntityAndPassengers(Lnet/minecraft/entity/Entity;)V"))
    private static void mobSpawnHook(ServerWorld instance, Entity entity, Operation<Void> original) {
        switch (entity) {
            case ZombieEntity z -> entity = z instanceof ZombifiedPiglinEntity || z instanceof DrownedEntity || z instanceof
                    ZombieVillagerEntity ? entity :
                SpawnPools.ZOMBIES.getRandomEntry().apply(z);
            case SkeletonEntity s -> entity = SpawnPools.SKELETONS.getRandomEntry().apply(s);
            default -> {}
        }
        original.call(instance, entity);
    }

}
