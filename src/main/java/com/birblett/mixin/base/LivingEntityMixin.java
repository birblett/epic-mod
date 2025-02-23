package com.birblett.mixin.base;

import com.birblett.EpicMod;
import com.birblett.helper.Ability;
import com.birblett.helper.AnonymousTicker;
import com.birblett.helper.PlayerTicker;
import com.birblett.helper.Util;
import com.birblett.helper.tracked_values.Homing;
import com.birblett.interfaces.AbilityUser;
import com.birblett.interfaces.ServerPlayerEntityInterface;
import com.birblett.interfaces.TickedEntity;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity implements TickedEntity {

    @Unique
    private final ArrayList<AnonymousTicker> tickers = new ArrayList<>();

    public LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Override
    public void addTicker(AnonymousTicker ticker) {
        this.tickers.add(ticker);
    }

    @Inject(method = "isInvulnerableTo", at = @At("HEAD"), cancellable = true)
    private void ignoreDamageAbilities(ServerWorld world, DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        //noinspection ConstantValue
        if (source.isIn(EpicMod.INDIRECT_DAMAGE) && Util.hasEnchant(((LivingEntity) (Object) this).getEquippedStack(EquipmentSlot.CHEST), EpicMod.MAGIC_GUARD, world)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "applyDamage", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;applyArmorToDamage(Lnet/minecraft/entity/damage/DamageSource;F)F"))
    private void getHomingTarget(ServerWorld world, DamageSource source, float amount, CallbackInfo ci) {
        if (source.getAttacker() instanceof ServerPlayerEntity player) {
            ((Homing) ((ServerPlayerEntityInterface) player).getTickers(PlayerTicker.ID.HOMING)).setTarget((LivingEntity) (Object) this);
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void handleTicked(CallbackInfo ci) {
        if (!this.tickers.isEmpty()) {
            if (this.isOnGround()) {
                this.tickers.forEach(AnonymousTicker::onGroundTick);
            }
            this.tickers.forEach(AnonymousTicker::tick);
            this.tickers.removeIf(AnonymousTicker::shouldRemove);
        }
    }

    @WrapOperation(method = "damage", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/damage/DamageSource;isIn(Lnet/minecraft/registry/tag/TagKey;)Z", ordinal = 6))
    private boolean yup(DamageSource instance, TagKey<DamageType> tag, Operation<Boolean> original, @Local(argsOnly = true) DamageSource source) {
        boolean b = source.getSource() instanceof AbilityUser a && a.hasAbility(Ability.NO_KNOCKBACK);
        return b || original.call(instance, tag);
    }

}
