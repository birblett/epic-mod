package com.birblett.ai;


import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.MathHelper;

public class WaterMoveControl extends MoveControl {

    private final MobEntity mob;

    public WaterMoveControl(MobEntity mob) {
        super(mob);
        this.mob = mob;
    }

    @Override
    public void tick() {
        LivingEntity livingEntity = this.mob.getTarget();
        if (this.mob instanceof Swimmer s && s.isTargetingUnderwater() && this.mob.isTouchingWater()) {
            if (livingEntity != null && livingEntity.getY() > this.mob.getY() || s.isTargetingUnderwater()) {
                this.mob.setVelocity(this.mob.getVelocity().add(0.0, 0.002, 0.0));
            }
            if (this.state != MoveControl.State.MOVE_TO || this.mob.getNavigation().isIdle()) {
                this.mob.setMovementSpeed(0.0f);
                return;
            }
            double d = this.targetX - this.mob.getX();
            double e = this.targetY - this.mob.getY();
            double f = this.targetZ - this.mob.getZ();
            double g = Math.sqrt(d * d + e * e + f * f);
            e /= g;
            float h = (float)(MathHelper.atan2(f, d) * 57.2957763671875) - 90.0f;
            this.mob.setYaw(this.wrapDegrees(this.mob.getYaw(), h, 90.0f));
            this.mob.bodyYaw = this.mob.getYaw();
            float i = (float)(this.speed * this.mob.getAttributeValue(EntityAttributes.MOVEMENT_SPEED));
            float j = MathHelper.lerp(0.125f, this.mob.getMovementSpeed(), i);
            this.mob.setMovementSpeed(j);
            this.mob.setVelocity(this.mob.getVelocity().add((double)j * d * 0.005, (double)j * e * 0.1, (double)j * f * 0.005));
        } else {
            if (!this.mob.isOnGround()) {
                this.mob.setVelocity(this.mob.getVelocity().add(0.0, -0.008, 0.0));
            }
            super.tick();
        }
    }
}
