package com.birblett.ai;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.MoveToTargetPosGoal;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldView;

public class SwimmerLeaveWaterGoal extends MoveToTargetPosGoal {
    private final HostileEntity mob;
    private LivingEntity target;
    private final double jump;
    private int jumpCooldown = 10;

    public SwimmerLeaveWaterGoal(HostileEntity mob, double speed, double jump) {
        super(mob, speed, 8, 2);
        this.jump = jump;
        this.mob = mob;
    }

    @Override
    public boolean canStart() {
        if (this.jumpCooldown > 0) {
            --this.jumpCooldown;
            return false;
        } else {
            Path path;
            this.target = this.mob.getTarget();
            return (this.target = this.mob.getTarget()) != null && ((path = this.mob.getNavigation().getCurrentPath()) != null &&
                    (path.getCurrentNodeIndex() >= path.getLength() || path.getCurrentNode() == path.getEnd())) &&
                    !this.target.isTouchingWater() && this.mob.isTouchingWater();
        }
    }

    @Override
    public boolean shouldContinue() {
        return super.shouldContinue();
    }

    @Override
    protected boolean isTargetPos(WorldView world, BlockPos pos) {
        BlockPos blockPos = pos.up();
        if (!world.isAir(blockPos) || !world.isAir(blockPos.up())) {
            return false;
        }
        return world.getBlockState(pos).hasSolidTopSurface(world, pos, this.mob);
    }

    @Override
    public void start() {
        if (this.mob instanceof Swimmer s) {
            Vec3d v = this.target.getPos().subtract(this.mob.getPos()).normalize();
            this.mob.addVelocity(v.add(0, this.jump, 0));
            this.jumpCooldown = 20;
        }
        super.start();
    }

    @Override
    public void stop() {
        super.stop();
    }
}