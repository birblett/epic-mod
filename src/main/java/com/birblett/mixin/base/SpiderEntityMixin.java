package com.birblett.mixin.base;

import com.birblett.helper.Util;
import net.minecraft.block.Blocks;
import net.minecraft.block.PowderSnowBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.Flutterer;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.SpiderEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(SpiderEntity.class)
public abstract class SpiderEntityMixin extends HostileEntity {

    @Shadow public abstract void setClimbingWall(boolean climbing);

    @Shadow public abstract boolean isClimbing();

    protected SpiderEntityMixin(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    protected ActionResult interactMob(PlayerEntity player, Hand hand) {
        if (!this.getWorld().isClient && Util.isRiderPlayer(player) && !this.hasPassengers() && !player.shouldCancelInteraction()) {
            player.startRiding(this);
            return ActionResult.SUCCESS;
        }
        return super.interactMob(player, hand);
    }

    public void travel(Vec3d movementInput) {
        if (this.isAlive() && this.getFirstPassenger() instanceof ServerPlayerEntity player && Util.isRiderPlayer(player)) {
            Vec3d v = getInput(player);
            this.setRotation(player.getYaw(), player.getPitch() * 0.5F);
            this.setClimbingWall(this.horizontalCollision);
            this.travelControlled(v);
        } else {
            super.travel(movementInput);
        }
    }

    @Unique
    protected Vec3d getInput(ServerPlayerEntity player) {
        Vec3d out = Vec3d.ZERO;
        PlayerInput p = player.getPlayerInput();
        if (p.forward() || p.backward() || p.left() || p.right()) {
            if (p.forward()) {
                out = out.add(0, 0, 0.8);
            }
            if (p.backward()) {
                out = out.subtract(0, 0, 0.8);
            }
            if (p.left()) {
                out = out.add(0.5, 0, 0);
            }
            if (p.right()) {
                out = out.subtract(0.5, 0 ,0);
            }
            if (this.isOnGround() && p.jump()) {
                this.jump();
            }
        }
        return out;
    }

    @Unique
    public void travelControlled(Vec3d movementInput) {
        FluidState fluidState = this.getWorld().getFluidState(this.getBlockPos());
        if ((this.isTouchingWater() || this.isInLava()) && this.shouldSwimInFluids() && !this.canWalkOnFluid(fluidState)) {
            this.travelInFluid(movementInput);
        } else if (this.isGliding()) {
            this.travelGliding();
        } else {
            this.travelMidAir(movementInput);
        }
    }

    @Unique
    private void travelMidAir(Vec3d movementInput) {
        BlockPos blockPos = this.getVelocityAffectingPos();
        float f = this.isOnGround() ? this.getWorld().getBlockState(blockPos).getBlock().getSlipperiness() : 1.0F;
        float g = f * 0.91F;
        Vec3d vec3d = this.applyMovementInput(movementInput, f);
        double d = vec3d.y;
        StatusEffectInstance statusEffectInstance = this.getStatusEffect(StatusEffects.LEVITATION);
        if (statusEffectInstance != null) {
            d += (0.05 * (double)(statusEffectInstance.getAmplifier() + 1) - vec3d.y) * 0.2;
        } else if (this.getWorld().isClient && !this.getWorld().isChunkLoaded(blockPos)) {
            if (this.getY() > (double)this.getWorld().getBottomY()) {
                d = -0.1;
            } else {
                d = 0.0;
            }
        } else {
            d -= this.getEffectiveGravity();
        }

        if (this.hasNoDrag()) {
            this.setVelocity(vec3d.x, d, vec3d.z);
        } else {
            float h = this instanceof Flutterer ? g : 0.98F;
            this.setVelocity(vec3d.x * (double)g, d * (double)h, vec3d.z * (double)g);
        }

    }

    @Unique
    private Vec3d applyMovementInput(Vec3d movementInput, float slipperiness) {
        this.updateVelocity(this.getMovementSpeed(slipperiness), movementInput);
        this.setVelocity(this.applyClimbingSpeed(this.getVelocity()));
        this.move(MovementType.SELF, this.getVelocity());
        Vec3d vec3d = this.getVelocity();
        if ((this.horizontalCollision || this.jumping) && (this.isClimbing() || this.getBlockStateAtPos().isOf(Blocks.POWDER_SNOW) && PowderSnowBlock.canWalkOnPowderSnow(this))) {
            vec3d = new Vec3d(vec3d.x, 0.2, vec3d.z);
        }
        return vec3d;
    }

    @Unique
    private float getMovementSpeed(float slipperiness) {
        return this.isOnGround() ? this.getMovementSpeed() * (0.21600002F / (slipperiness * slipperiness * slipperiness)) : this.getOffGroundSpeed();
    }

    @Unique
    private Vec3d applyClimbingSpeed(Vec3d motion) {
        if (this.isClimbing()) {
            this.onLanding();
            double d = MathHelper.clamp(motion.x, -0.3, 0.3);
            double e = MathHelper.clamp(motion.z, -0.3, 0.3);
            double g = Math.max(motion.y + 0.2, -0.15000000596046448);
            motion = new Vec3d(d, g, e);
        }
        return motion;
    }

    @Unique
    private void travelInFluid(Vec3d movementInput) {
        boolean bl = this.getVelocity().y <= 0.0;
        double d = this.getY();
        double e = this.getEffectiveGravity();
        Vec3d vec3d2;
        if (this.isTouchingWater()) {
            float f = this.isSprinting() ? 0.9F : this.getBaseWaterMovementSpeedMultiplier();
            float g = 0.02F;
            float h = (float)this.getAttributeValue(EntityAttributes.WATER_MOVEMENT_EFFICIENCY);
            if (!this.isOnGround()) {
                h *= 0.5F;
            }
            if (h > 0.0F) {
                f += (0.54600006F - f) * h;
                g += (this.getMovementSpeed() - g) * h;
            }
            if (this.hasStatusEffect(StatusEffects.DOLPHINS_GRACE)) {
                f = 0.96F;
            }
            this.updateVelocity(g, movementInput);
            this.move(MovementType.SELF, this.getVelocity());
            Vec3d vec3d = this.getVelocity();
            if (this.horizontalCollision && this.isClimbing()) {
                vec3d = new Vec3d(vec3d.x, 0.2, vec3d.z);
            }
            vec3d = vec3d.multiply(f, 0.800000011920929, f);
            this.setVelocity(this.applyFluidMovingSpeed(e, bl, vec3d));
        } else {
            this.updateVelocity(0.02F, movementInput);
            this.move(MovementType.SELF, this.getVelocity());
            if (this.getFluidHeight(FluidTags.LAVA) <= this.getSwimHeight()) {
                this.setVelocity(this.getVelocity().multiply(0.5, 0.800000011920929, 0.5));
                vec3d2 = this.applyFluidMovingSpeed(e, bl, this.getVelocity());
                this.setVelocity(vec3d2);
            } else {
                this.setVelocity(this.getVelocity().multiply(0.5));
            }
            if (e != 0.0) {
                this.setVelocity(this.getVelocity().add(0.0, -e / 4.0, 0.0));
            }
        }
        vec3d2 = this.getVelocity();
        if (this.horizontalCollision && this.doesNotCollide(vec3d2.x, vec3d2.y + 0.6000000238418579 - this.getY() + d, vec3d2.z)) {
            this.setVelocity(vec3d2.x, 0.30000001192092896, vec3d2.z);
        }

    }

    @Unique
    private void travelGliding() {
        Vec3d vec3d = this.getVelocity();
        double d = vec3d.horizontalLength();
        this.setVelocity(this.calcGlidingVelocity(vec3d));
        this.move(MovementType.SELF, this.getVelocity());
        if (!this.getWorld().isClient) {
            double e = this.getVelocity().horizontalLength();
            this.checkGlidingCollision(d, e);
        }

    }

    @Unique
    private Vec3d calcGlidingVelocity(Vec3d oldVelocity) {
        Vec3d vec3d = this.getRotationVector();
        float f = this.getPitch() * 0.017453292F;
        double d = Math.sqrt(vec3d.x * vec3d.x + vec3d.z * vec3d.z);
        double e = oldVelocity.horizontalLength();
        double g = this.getEffectiveGravity();
        double h = MathHelper.square(Math.cos(f));
        oldVelocity = oldVelocity.add(0.0, g * (-1.0 + h * 0.75), 0.0);
        double i;
        if (oldVelocity.y < 0.0 && d > 0.0) {
            i = oldVelocity.y * -0.1 * h;
            oldVelocity = oldVelocity.add(vec3d.x * i / d, i, vec3d.z * i / d);
        }

        if (f < 0.0F && d > 0.0) {
            i = e * (double)(-MathHelper.sin(f)) * 0.04;
            oldVelocity = oldVelocity.add(-vec3d.x * i / d, i * 3.2, -vec3d.z * i / d);
        }

        if (d > 0.0) {
            oldVelocity = oldVelocity.add((vec3d.x / d * e - oldVelocity.x) * 0.1, 0.0, (vec3d.z / d * e - oldVelocity.z) * 0.1);
        }

        return oldVelocity.multiply(0.9900000095367432, 0.9800000190734863, 0.9900000095367432);
    }

    @Unique
    private void checkGlidingCollision(double oldSpeed, double newSpeed) {
        if (this.horizontalCollision) {
            double d = oldSpeed - newSpeed;
            float f = (float)(d * 10.0 - 3.0);
            if (f > 0.0F) {
                this.playSound(f > 4 ? this.getFallSounds().big() : this.getFallSounds().small(), 1.0F, 1.0F);
                this.serverDamage(this.getDamageSources().flyIntoWall(), f);
            }
        }

    }


}
