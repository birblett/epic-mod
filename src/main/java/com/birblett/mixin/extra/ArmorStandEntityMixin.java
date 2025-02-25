package com.birblett.mixin.extra;

import com.birblett.helper.Ability;
import com.birblett.helper.PlayerTicker;
import com.birblett.helper.Util;
import com.birblett.helper.tracked_values.Homing;
import com.birblett.interfaces.AbilityUser;
import com.birblett.interfaces.ServerPlayerEntityInterface;
import net.minecraft.block.Block;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Pair;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.LinkedList;
import java.util.Queue;

@Mixin(ArmorStandEntity.class)
public abstract class ArmorStandEntityMixin extends LivingEntity implements AbilityUser {

    @Shadow
    protected abstract void onBreak(ServerWorld world, DamageSource damageSource);

    @Unique
    private int ticker = -1;
    @Unique
    private Queue<Pair<Long, Float>> lastDamageQueue;

    protected ArmorStandEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void changeDummyName(CallbackInfo ci) {
        if (this.hasAbility(Ability.DUMMY) && this.isCustomNameVisible()) {
            if (this.lastDamageQueue == null) {
                this.lastDamageQueue = new LinkedList<>();
            }
            if (this.ticker > 0 && this.lastDamageTaken != 0) {
                this.setCustomName(Text.of("Last damage taken: " + String.format("%.3f", this.lastDamageTaken)));
                --this.ticker;
            } else if (this.ticker == 0) {
                double d = 0;
                long start = -1;
                long end = -1;
                for (var p : this.lastDamageQueue) {
                    if (start == -1) {
                        start = p.getLeft();
                    }
                    end = p.getLeft();
                    d += p.getRight();
                }
                double totalTime = (end - start) / 20.0;
                this.setCustomName(Text.of(String.format("Past %.2fs: DMG: %.2f - DPS: %.2f - DPH: %.2f", totalTime, d, d / totalTime, d /
                        this.lastDamageQueue.size())));
                this.lastDamageQueue.clear();
                --this.ticker;
            }
        }
    }

    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void handleDummy(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (this.hasAbility(Ability.DUMMY)) {
            cir.setReturnValue(this.dummyDamage(world, source, amount));
        }
    }

    @Inject(method = "onStruckByLightning", at = @At("HEAD"))
    private void dummyLightningHit(ServerWorld world, LightningEntity lightning, CallbackInfo ci) {
        if (this.hasAbility(Ability.DUMMY)) {
            super.onStruckByLightning(world, lightning);
        }
    }

    @Override
    public void takeKnockback(double strength, double x, double z) {
        if (!this.hasAbility(Ability.DUMMY)) {
            super.takeKnockback(strength, x, z);
        }
    }

    @Unique
    private boolean dummyDamage(ServerWorld world, DamageSource source, float amount) {
        if (this.isDead() || source.isIn(DamageTypeTags.IS_FIRE) && this.hasStatusEffect(StatusEffects.FIRE_RESISTANCE)) {
            return false;
        }
        if (source.isOf(DamageTypes.OUT_OF_WORLD) || source.getAttacker() instanceof LivingEntity e && ((AbilityUser) e)
                .hasAbility(Ability.BOSS_FLAG)) {
            this.discard();
            return true;
        }
        amount = amount < 0 ? 0 : amount;
        this.limbAnimator.setSpeed(1.5f);
        boolean bl2 = true;
        if (this.timeUntilRegen > 10.0f && !source.isIn(DamageTypeTags.BYPASSES_COOLDOWN)) {
            if (amount <= this.lastDamageTaken) {
                return false;
            }
            this.lastDamageTaken = amount = this.modifyAppliedDamage(source, this.applyArmorToDamage(source, amount));
            this.emitGameEvent(GameEvent.ENTITY_DAMAGE);
            bl2 = false;
        } else {
            this.lastDamageTaken = amount = this.modifyAppliedDamage(source, this.applyArmorToDamage(source, amount));
            this.emitGameEvent(GameEvent.ENTITY_DAMAGE);
            this.timeUntilRegen = 20;
            this.hurtTime = this.maxHurtTime = 10;
        }
        this.setAttackingPlayer(source);

        if (bl2) {
            world.sendEntityDamage(this, source);
            if (!(source.isIn(DamageTypeTags.NO_IMPACT))) {
                this.scheduleVelocityUpdate();
            }
        }
        this.playHurtSound(source);
        boolean bl3 = amount > 0.0f;
        if (bl3) {
            for (StatusEffectInstance statusEffectInstance : this.getStatusEffects()) {
                statusEffectInstance.onEntityDamage(world, this, source, amount);
            }
        }
        if (source.getAttacker() instanceof ServerPlayerEntity e) {
            Util.sendActionBarMessage(e, "Dealt " + String.format("%.3f", this.lastDamageTaken) + " damage!");
            ((Homing) ((ServerPlayerEntityInterface) e).getTickers(PlayerTicker.ID.HOMING)).setTarget(this);
        }
        long lastDamageTime = world.getTime();
        if (this.lastDamageQueue == null) {
            this.lastDamageQueue = new LinkedList<>();
        }
        var p = this.lastDamageQueue.peek();
        while (p != null && p.getLeft() < lastDamageTime - 200) {
            this.lastDamageQueue.remove();
            p = this.lastDamageQueue.peek();
        }
        this.lastDamageQueue.add(new Pair<>(lastDamageTime, this.lastDamageTaken));
        this.setCustomNameVisible(true);
        this.ticker = 80;
        return bl3;
    }

    @Inject(method = "interactAt", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/decoration/ArmorStandEntity;getPreferredEquipmentSlot(Lnet/minecraft/item/ItemStack;)Lnet/minecraft/entity/EquipmentSlot;"), cancellable = true)
    private void removeDummy(PlayerEntity player, Vec3d hitPos, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (player instanceof ServerPlayerEntity p && this.hasAbility(Ability.DUMMY) && player.getStackInHand(hand).isEmpty()) {
            if (p.isSneaking()) {
                this.setCustomNameVisible(false);
            } else {
                this.onBreak(p.getServerWorld(), p.getDamageSources().playerAttack(player));
                ItemStack itemStack = new ItemStack(Items.ARMOR_STAND);
                itemStack.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Dummy"));
                Block.dropStack(this.getWorld(), this.getBlockPos(), itemStack);
                this.discard();
                cir.setReturnValue(ActionResult.SUCCESS);
            }
        }
    }

}
