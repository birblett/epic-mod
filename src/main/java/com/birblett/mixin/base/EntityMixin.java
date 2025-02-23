package com.birblett.mixin.base;

import com.birblett.EpicMod;
import com.birblett.helper.Ability;
import com.birblett.interfaces.AbilityUser;
import com.birblett.interfaces.OwnedProjectile;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;
import java.util.HashSet;

@Mixin(Entity.class)
public class EntityMixin implements AbilityUser {

    @Unique
    HashSet<Ability> abilities = null;

    @Override
    public void addAbilities(Ability... abilities) {
        if (this.abilities == null) {
            this.abilities = new HashSet<>();
        }
        this.abilities.addAll(Arrays.asList(abilities));
    }

    @Override
    public boolean hasAbility(Ability ability) {
        return this.abilities != null && this.abilities.contains(ability);
    }

    @Override
    public boolean hasAbilities() {
        return this.abilities != null && !this.abilities.isEmpty();
    }

    @Override
    public Ability[] getAbilities() {
        return this.abilities == null ? new Ability[]{} : this.abilities.toArray(new Ability[]{});
    }

    @WrapOperation(method = "onStruckByLightning", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;damage(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/damage/DamageSource;F)Z"))
    private boolean bossLightning(Entity instance, ServerWorld serverWorld, DamageSource damageSource, float v, Operation<Boolean> original, @Local(argsOnly = true) LightningEntity lightning) {
        AbilityUser a = (AbilityUser) lightning;
        v = a.hasAbility(Ability.OWNED) || a.hasAbility(Ability.BOSS_FLAG) ? 15.0f : v;
        if (a.hasAbility(Ability.OWNED)) {
            try {
                damageSource = new DamageSource(serverWorld.getRegistryManager().getOrThrow(RegistryKeys.DAMAGE_TYPE)
                        .getOrThrow(DamageTypes.LIGHTNING_BOLT), lightning, ((OwnedProjectile) lightning).getProjectileOwner());
            } catch (Exception e) {
                EpicMod.LOGGER.info("problem creating lightning damage source");
            }
        }
        return original.call(instance, serverWorld, damageSource, v);
    }

    @Inject(method = "isTouchingWater", at = @At("HEAD"), cancellable = true)
    private void stopTouchingWater(CallbackInfoReturnable<Boolean> cir) {
        if (this.hasAbility(Ability.IGNORE_WATER)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "writeNbt", at = @At("TAIL"))
    private void writeAbilityNbt(NbtCompound nbt, CallbackInfoReturnable<NbtCompound> cir) {
        if (this.abilities != null) {
            NbtCompound compound = new NbtCompound();
            for (Ability j : this.abilities) {
                compound.putBoolean(j.name(), true);
            }
            nbt.put("EpicAbilities", compound);
        }
    }

    @Inject(method = "readNbt", at = @At("TAIL"))
    private void readAbilityNbt(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.contains("EpicAbilities")) {
        NbtCompound compound = nbt.getCompound("EpicAbilities");
            this.abilities = new HashSet<>();
            for (String keys : compound.getKeys()) {
                try {
                    this.abilities.add(Ability.valueOf(keys));
                } catch(Exception ignored) {}
            }
            this.updateGoals();
        }
    }

}
