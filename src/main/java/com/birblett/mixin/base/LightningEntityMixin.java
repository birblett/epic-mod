package com.birblett.mixin.base;

import com.birblett.helper.Ability;
import com.birblett.interfaces.AbilityUser;
import com.birblett.interfaces.OwnedProjectile;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

@Mixin(LightningEntity.class)
public abstract class LightningEntityMixin extends Entity implements AbilityUser, OwnedProjectile {

    @Shadow
    @Final
    private Set<Entity> struckEntities;
    @Shadow
    private int remainingActions;
    @Shadow
    private int ambientTick = 2;
    @Unique
    private LivingEntity owner;

    public LightningEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Override
    public void setProjectileOwner(LivingEntity e) {
        this.owner = e;
    }

    @Override
    public LivingEntity getProjectileOwner() {
        return this.owner;
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getOtherEntities(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Box;Ljava/util/function/Predicate;)Ljava/util/List;", ordinal = 1))
    private List<Entity> getModifiedList(World instance, @Nullable Entity except, Box box, Predicate<? super Entity> predicate, Operation<List<Entity>> original) {
        boolean isBossSummoned = this.hasAbility(Ability.BOSS_FLAG);
        if (isBossSummoned || this.hasAbility(Ability.OWNED)) {
            Predicate<? super Entity> p = entity -> !((AbilityUser) entity).hasAbility(Ability.BOSS_FLAG) && !this.struckEntities.contains(entity);
            this.ambientTick = this.remainingActions = 0;
            List<Entity> l = original.call(instance, except, new Box(this.getX() - 1.2, this.getY() - 3.0, this.getZ() - 1.2,
                    this.getX() + 1.2, this.getY() + (isBossSummoned ? 6.0 : 3.0), this.getZ() + 1.2), p);
            for (Entity entity : l) {
                if (entity instanceof LivingEntity target) {
                    if (isBossSummoned) {
                        target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 100));
                        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100));
                    }
                    target.timeUntilRegen = 9;
                    target.hurtTime = 0;
                }
            }
            return l;
        }
        return original.call(instance, except, box, predicate);
    }

    @Inject(method = "spawnFire", at = @At("HEAD"), cancellable = true)
    private void cancelFire(int spreadAttempts, CallbackInfo ci) {
        if (this.hasAbility(Ability.BOSS_FLAG) || this.hasAbility(Ability.OWNED)) {
            ci.cancel();
        }
    }

}
