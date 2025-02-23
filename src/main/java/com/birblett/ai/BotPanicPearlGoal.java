package com.birblett.ai;

import com.birblett.interfaces.OwnedProjectile;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.OminousItemSpawnerEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;

public class BotPanicPearlGoal extends Goal {

    private final ZombieEntity zombie;
    private int cooldownTicks = 60;

    public BotPanicPearlGoal(ZombieEntity mob) {
        this.zombie = mob;
    }

    @Override
    public boolean canStart() {
        LivingEntity target = this.zombie.getTarget();
        if (target != null) {
            if (this.cooldownTicks <= 0 && this.zombie.getWorld() instanceof ServerWorld world) {
                OminousItemSpawnerEntity e = OminousItemSpawnerEntity.create(world, Items.ENDER_PEARL.getDefaultStack());
                e.setPosition(target.getPos().add(0, 0.25, 0));
                ((OwnedProjectile) e).setProjectileOwner(this.zombie);
                world.spawnEntity(e);
                this.cooldownTicks = 60;
            } else if ((this.zombie.hurtTime == 8 || this.zombie.hurtTime == 9)) {
                this.cooldownTicks -= 15;
            } else if (!this.zombie.canSee(target)) {
                --this.cooldownTicks;
            }
        }
        return false;
    }

}
