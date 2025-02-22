package com.birblett.ai;

import com.birblett.helper.Ability;
import com.birblett.interfaces.AbilityUser;
import com.birblett.interfaces.ProjectileInterface;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

public class BotEnderPearlGoal extends Goal {

    private final ZombieEntity zombie;
    private LivingEntity target;
    private int cooldownTicks = 60;

    public BotEnderPearlGoal(ZombieEntity mob) {
        this.zombie = mob;
    }

    @Override
    public boolean canStart() {
        if (this.cooldownTicks <= 0) {
            this.target = this.zombie.getTarget();
            if (this.target != null) {
                if (this.cooldownTicks < -1) {
                    this.zombie.equipStack(EquipmentSlot.HEAD, Items.DISPENSER.getDefaultStack());
                    return true;
                }
                double x = this.target.getX() - this.zombie.getX(), y = this.target.getY() - this.zombie.getY(), z = this.target.getZ() - this.zombie.getZ();
                boolean b = this.zombie.canSee(this.target);
                if (x * x + y * y + z * z > 64) {
                    return b;
                }
                if (b) {
                    return this.zombie.getNavigation().findPathTo(this.target, 0) == null;
                }
            }
        } else {
            this.zombie.equipStack(EquipmentSlot.HEAD, Items.OBSERVER.getDefaultStack());
            --this.cooldownTicks;
        }
        return false;
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (this.cooldownTicks > -15) {
            this.zombie.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 2, 1, false, false));
        } else if (this.target != null) {
            double x = this.target.getX() - this.zombie.getX(), y = this.target.getY() - this.zombie.getY(), z = this.target.getZ() -
                    this.zombie.getZ();
            Vec3d vel = new Vec3d(x, y, z).normalize().multiply(0.9);
            this.zombie.getLookControl().lookAt(this.target, 180.0F, 180.0F);
            if (this.zombie.getWorld() instanceof ServerWorld world) {
                world.playSound(null, this.zombie.getX(), this.zombie.getY(), this.zombie.getZ(),
                        SoundEvents.ENTITY_ENDER_PEARL_THROW, SoundCategory.NEUTRAL, 0.5f, 0.4f /
                                (world.getRandom().nextFloat() * 0.4f + 0.8f));
                EnderPearlEntity e = ProjectileEntity.spawnWithVelocity(EnderPearlEntity::new, world, Items.ENDER_PEARL.getDefaultStack(),
                        this.zombie, 0.0f, 2, 0.1f);
                ((ProjectileInterface) e).setLife(Math.sqrt(x * x + y * y + z * z));
                e.setNoGravity(true);
                e.setGlowing(true);
                e.setVelocity(vel.x, vel.y, vel.z);
                ((AbilityUser) e).addAbilities(Ability.IGNORE_WATER);
            }
            this.cooldownTicks = 100;
        }
        --this.cooldownTicks;
    }

}
