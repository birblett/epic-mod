package com.birblett.mixin.extra;

import com.birblett.helper.Ability;
import com.birblett.interfaces.AbilityUser;
import com.birblett.interfaces.Lootable;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.loot.LootTable;
import net.minecraft.registry.RegistryKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(MobEntity.class)
public abstract class MobEntityMixin implements AbilityUser, Lootable {

    @Shadow
    private Optional<RegistryKey<LootTable>> lootTable;

    @Override
    public void setLootTable(RegistryKey<LootTable> id) {
        this.lootTable = Optional.ofNullable(id);
    }

    @Inject(method = "checkDespawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/mob/MobEntity;discard()V", ordinal = 0))
    private void despawnWithPassengers0(CallbackInfo ci) {
        if (this.hasAbilities() && ((MobEntity) (Object) this).hasPassengers()) for (Entity e : ((MobEntity) (Object) this).getPassengerList()) e.discard();
    }

    @Inject(method = "checkDespawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/mob/MobEntity;discard()V", ordinal = 1))
    private void despawnWithPassengers1(CallbackInfo ci) {
        if (this.hasAbilities() && ((MobEntity) (Object) this).hasPassengers()) for (Entity e : ((MobEntity) (Object) this).getPassengerList()) e.discard();
    }

    @Inject(method = "checkDespawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/mob/MobEntity;discard()V", ordinal = 2))
    private void despawnWithPassengers2(CallbackInfo ci) {
        if (this.hasAbilities() && ((MobEntity) (Object) this).hasPassengers()) for (Entity e : ((MobEntity) (Object) this).getPassengerList()) e.discard();
    }

    @WrapOperation(method = "checkDespawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/SpawnGroup;getDespawnStartRange()I"))
    private int importantDespawnRange(SpawnGroup instance, Operation<Integer> original) {
        return this.hasAbility(Ability.BOSS_FLAG) ? Math.max(original.call(instance), 128) : (this.hasAbility(Ability.ELITE) ?
                Math.max(original.call(instance), 96) : original.call(instance));
    }

}
