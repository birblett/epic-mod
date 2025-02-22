package com.birblett.ai;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

public class PounceGoal extends Goal {

    private final ZombieEntity zombie;
    private LivingEntity target;
    private int cooldownTicks = 40;
    private final float pounceStrength;

    public PounceGoal(ZombieEntity mob, float pounceStrength) {
        this.zombie = mob;
        this.pounceStrength = pounceStrength;
    }

    public PounceGoal(ZombieEntity mob) {
        this(mob, 1);
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }

    @Override
    public boolean canStart() {
        if (this.cooldownTicks <= 0) {
            if (this.cooldownTicks < -1) {
                return true;
            }
            this.target = this.zombie.getTarget();
            if (this.target != null) {
                double y = this.target.getY() - this.zombie.getY();
                if (Math.abs(y) <= 1) {
                    double x = this.target.getX() - this.zombie.getX(), z = this.target.getZ() - this.zombie.getZ();
                    double sqd = x * x + z * z + Math.clamp(y * y, -1, 1);
                    return sqd >= 9 && sqd <= 49;
                }
            }
        } else {
            --this.cooldownTicks;
        }
        return false;
    }

    @Override
    public void tick() {
        if (this.cooldownTicks > -30) {
            this.zombie.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 2, 1, false, false));
        } else if (this.target != null) {
            double x = this.target.getX() - this.zombie.getX(), y = this.target.getY() - this.zombie.getY(), z = this.target.getZ() - this.zombie.getZ();
            this.zombie.getLookControl().lookAt(this.target, 180.0F, 180.0F);
            if (this.zombie.getWorld() instanceof ServerWorld world) {
                world.spawnParticles(ParticleTypes.CLOUD, this.zombie.getX(), this.zombie.getY() + 0.4, this.zombie.getZ(), 10, 0.04, 0.04, 0.04, 0.03);
            }
            Vec3d vel = new Vec3d(x, 0, z).normalize().multiply(this.pounceStrength).add(0, 0.1, 0);
            this.zombie.setVelocity(vel);
            this.cooldownTicks = 60;
        }
        --this.cooldownTicks;
    }

}
