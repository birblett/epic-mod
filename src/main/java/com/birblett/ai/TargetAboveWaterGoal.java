package com.birblett.ai;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.ZombieEntity;

public class TargetAboveWaterGoal extends Goal {

    private final ZombieEntity mob;
    private final double speed;
    private boolean foundTarget;
    private LivingEntity target;

    public TargetAboveWaterGoal(ZombieEntity mob, double speed) {
        this.mob = mob;
        this.speed = speed;
    }

    @Override
    public boolean canStart() {
        return (this.target = this.mob.getTarget()) != null && this.mob.isTouchingWater() && this.target.getY() > this.mob.getY();
    }

    @Override
    public boolean shouldContinue() {
        return this.canStart() && !this.foundTarget;
    }

    @Override
    public void tick() {
        this.mob.getNavigation().startMovingTo(this.target.getX(), this.target.getY(), this.target.getZ(), this.speed);
    }

    @Override
    public void start() {
        ((Swimmer) this.mob).setTargetingUnderwater(true);
        this.foundTarget = false;
    }

    @Override
    public void stop() {
        ((Swimmer) this.mob).setTargetingUnderwater(false);
    }
}