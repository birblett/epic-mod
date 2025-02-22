package com.birblett.helper.tracked_values;

import com.birblett.EpicMod;
import com.birblett.helper.AttributeManager;
import com.birblett.helper.InputManager;
import com.birblett.helper.PlayerTicker;
import com.birblett.helper.Util;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.projectile.WindChargeEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;
import net.minecraft.world.explosion.AdvancedExplosionBehavior;
import net.minecraft.world.explosion.ExplosionBehavior;

import java.util.Optional;
import java.util.function.Function;

public class Rocket extends PlayerTicker {

    private static final ExplosionBehavior EXPLOSION_BEHAVIOR = new AdvancedExplosionBehavior(true, false,
            Optional.of(1.22f), Registries.BLOCK.getOptional(BlockTags.BLOCKS_WIND_CHARGE_EXPLOSIONS).map(Function.identity()));

    private boolean rocket = false;
    private int rocketCooldown = 160;
    private int rocketState = 0;

    public Rocket(ServerPlayerEntity player, AttributeManager attributeManager) {
        super(player, attributeManager);
    }

    @Override
    public void onGroundTick() {
        this.rocket = this.hasEnchant(EquipmentSlot.FEET, EpicMod.ROCKET);
        this.rocketState = 0;
    }

    @Override
    public void tick() {
        if (this.rocketState > 0) {
            if (this.player.isTouchingWater()) {
                this.rocketState = 0;
            } else {
                this.world().spawnParticles(ParticleTypes.FIREWORK, this.player.getX(), this.player.getY(), this.player.getZ(), 5,
                        0, 0, 0, 0.08);
                --this.rocketState;
            }
        }
        if (this.rocketCooldown > 0) {
            --this.rocketCooldown;
        }
    }

    @Override
    public void onInput(InputManager pressed, InputManager last) {
        if (this.rocketCooldown == 0 && this.hasEnchant(EquipmentSlot.FEET, EpicMod.ROCKET) && this.rocket && pressed.jump() &&
                this.player.isSneaking() && !this.player.isOnGround()) {
            Util.playSound(this.world(), this.player, SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 1.0f);
            WindChargeEntity e = new WindChargeEntity(this.player, this.world(), this.player.getX(), this.player.getY(), this.player.getZ());
            this.world().createExplosion(e, null, EXPLOSION_BEHAVIOR, this.player.getX(), this.player.getY(),
                    this.player.getZ(), 1.2f, false, World.ExplosionSourceType.TRIGGER, ParticleTypes.GUST_EMITTER_SMALL,
                    ParticleTypes.GUST_EMITTER_LARGE, SoundEvents.ENTITY_WIND_CHARGE_WIND_BURST);
            e.discard();
            this.player.addVelocity(0, 1.9, 0);
            this.player.velocityDirty = true;
            this.player.velocityModified = true;
            this.player.setIgnoreFallDamageFromCurrentExplosion(true);
            this.rocket = false;
            this.rocketState = 10;
            this.rocketCooldown = 160;
        }
    }

}
