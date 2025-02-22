package com.birblett.ai;

import com.birblett.helper.Util;
import com.birblett.interfaces.Mage;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.List;

public class MageTeleportGoal extends Goal {

    private final MobEntity mob;
    private LivingEntity target = null;
    private int cooldownTicks = 0;

    public MageTeleportGoal(MobEntity mob) {
        this.mob = mob;
    }

    @Override
    public boolean canStart() {
        if ((this.target = this.mob.getTarget()) != null) {
            return true;
        }
        this.cooldownTicks = 300;
        ((Mage) this.mob).setTeleporting(false);
        return false;
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (this.target != null && this.mob.getWorld() instanceof ServerWorld world) {
            this.mob.getLookControl().lookAt(this.target, 20, 10);
            if (this.cooldownTicks <= 0 && !((Mage) this.mob).attacking()) {
                world.sendEntityStatus(this.mob, EntityStatuses.ADD_PORTAL_PARTICLES);
                List<BlockPos> list = new ArrayList<>();
                for (BlockPos p : BlockPos.iterateOutwards(this.target.getBlockPos(), 7, 2, 7)) {
                    int dist = p.getManhattanDistance(this.target.getBlockPos());
                    if (!(dist < 5 || dist > 7)) {
                        list.add(p.toImmutable());
                    }
                }
                if (!list.isEmpty()) {
                    list = list.stream().filter(pos -> world.getBlockState(pos).getCollisionShape(world, pos).isEmpty() &&
                            world.raycast(new RaycastContext(new Vec3d(pos.getX() + 0.5, pos.getY() +
                                    this.mob.getEyeHeight(EntityPose.STANDING), pos.getZ() + 0.5), this.target.getPos(),
                                    RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this.mob)).getType() ==
                                    HitResult.Type.MISS).toList();
                }
                if (list.isEmpty()) {
                    this.mob.setPosition(Vec3d.of(this.target.getBlockPos()
                            .add(0, 5, 0)).add(0.5, 0, 0.5));
                    while (!this.mob.canSee(this.target) && this.mob.getY() > this.target.getY()) {
                        this.mob.setPosition(this.mob.getPos().add(0, -1, 0));
                    }
                    Util.mobBreakBlocks(this.mob, 2, 2);
                } else {
                    int index = this.mob.getRandom().nextInt(list.size());
                    this.mob.setPosition(Vec3d.of(list.get(index)).add(0.5, 0, 0.5));
                }
                this.cooldownTicks = 300;
                this.mob.getLookControl().lookAt(this.target, 180, 180);
                this.mob.getWorld().playSound(null, this.mob.getX(), this.mob.getY(), this.mob.getZ(),
                        SoundEvents.ENTITY_ENDERMAN_TELEPORT, this.mob.getSoundCategory(), 1.0F, 1.0F);
                ((Mage) this.mob).setTeleporting(false);
            } else {
                if (!this.mob.canSee(this.target)) {
                    this.cooldownTicks -= 5;
                }
                if (this.mob.squaredDistanceTo(this.target) > 144) {
                    this.cooldownTicks -= 3;
                }
                if (this.mob.hurtTime == 9) {
                    this.cooldownTicks -= 30;
                }
                if (!(this.cooldownTicks < 41 && ((Mage) this.mob).attacking())) {
                    --this.cooldownTicks;
                    if (this.cooldownTicks < 90) {
                        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, this.mob.getX(), this.mob.getY() + 1, this.mob.getZ(), 1, 0.5, 1, 0.5, 0);
                    }
                    if (this.cooldownTicks < 40) {
                        ((Mage) this.mob).setTeleporting(true);
                    }
                }
            }
        }
    }

}
