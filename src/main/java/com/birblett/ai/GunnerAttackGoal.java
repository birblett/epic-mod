/*
 * Decompiled with CFR 0.2.2 (FabricMC 7c48b8c4).
 */
package com.birblett.ai;

import com.birblett.helper.Ability;
import com.birblett.helper.Util;
import com.birblett.interfaces.AbilityUser;
import com.birblett.interfaces.ProjectileInterface;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FireworkExplosionComponent;
import net.minecraft.component.type.FireworksComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import static com.birblett.helper.Ability.*;

public class GunnerAttackGoal extends Goal {

    enum Attack {
        NO_OP(0, Items.WOODEN_HOE),
        AUTO(30, Items.STONE_HOE),
        BURST(35, Items.IRON_HOE),
        ROCKET(40, Items.DIAMOND_HOE),
        SHOTGUN(35, Items.GOLDEN_HOE),
        SNIPE(20, Items.NETHERITE_HOE),
        TRIDENT(48, Items.WOODEN_HOE);

        public final int duration;
        public final Item item;
        Attack(int duration, Item item) {
            this.duration = duration;
            this.item = item;
        }
    }

    private static final ItemStack GUNNER_ROCKET = Items.FIREWORK_ROCKET.getDefaultStack();

    static {
        GUNNER_ROCKET.set(DataComponentTypes.FIREWORKS, new FireworksComponent(3, List.of(
                new FireworkExplosionComponent(FireworkExplosionComponent.Type.SMALL_BALL, IntList.of(0xFFFFFF), IntList.of(), true, false),
                new FireworkExplosionComponent(FireworkExplosionComponent.Type.SMALL_BALL, IntList.of(0xFFAAFF), IntList.of(), true, false),
                new FireworkExplosionComponent(FireworkExplosionComponent.Type.SMALL_BALL, IntList.of(0xFFFFAA), IntList.of(), true, false),
                new FireworkExplosionComponent(FireworkExplosionComponent.Type.SMALL_BALL, IntList.of(0xAAFFFF), IntList.of(), true, false),
                new FireworkExplosionComponent(FireworkExplosionComponent.Type.SMALL_BALL, IntList.of(0xFAAFFF), IntList.of(), true, false),
                new FireworkExplosionComponent(FireworkExplosionComponent.Type.SMALL_BALL, IntList.of(0xFFFAAF), IntList.of(), true, false),
                new FireworkExplosionComponent(FireworkExplosionComponent.Type.SMALL_BALL, IntList.of(0xaFFFFA), IntList.of(), true, false))));
    }

    private final HostileEntity mob;
    private int cooldown = 60;
    private int melee = 15;
    private int rageCooldown = 400;
    private int attackingTicks;
    private boolean enraged = false;
    private Attack attack = Attack.NO_OP;
    private Vec3d dir;
    private LivingEntity target;

    public GunnerAttackGoal(HostileEntity actor) {
        this.mob = actor;
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
    }

    @Override
    public boolean canStart() {
        if (this.target != null && this.target.isAlive() && !this.target.isInCreativeMode() && !this.target.isSpectator() &&
                this.mob.squaredDistanceTo(this.target) <= 3600) {
            this.mob.setTarget(this.target);
            return true;
        } else {
            this.target = null;
        }
        return (this.target = this.mob.getTarget()) != null && this.target.isAlive();
    }

    @Override
    public void stop() {
        (this.mob).setAttacking(false);
        this.mob.setStackInHand(Hand.MAIN_HAND, Items.WOODEN_HOE.getDefaultStack());
        this.enraged = false;
        this.cooldown = 60;
        this.melee = 15;
        this.attackingTicks = 0;
        this.mob.getNavigation().stop();
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (this.mob.getWorld() instanceof ServerWorld world) {
            boolean enraged = this.mob.getHealth() / this.mob.getMaxHealth() < 0.4;
            boolean canSee = this.mob.canSee(this.target);
            if (this.melee > 0) {
                --this.melee;
            }
            this.attack(this.target);
            if (this.attackingTicks > 0) {
                if (canSee || this.attack == Attack.TRIDENT) {
                    switch (this.attack) {
                        case AUTO -> {
                            if (this.attackingTicks == 25) {
                                Util.damagingRaycast(world, this.mob, this.mob.getEyePos(), this.dir.multiply(0.125), 240, 0.08,
                                        0, ParticleTypes.ELECTRIC_SPARK, true, false);
                            } else if (this.attackingTicks < 25 && this.attackingTicks % 2 == 0) {
                                this.lookAt(this.dir);
                                Entity e = Util.shootProjectile(world, this.mob, Items.GOLD_NUGGET, 15, this.dir, 1.5,
                                        0.09, 30, true);
                                e.setGlowing(true);
                                ((AbilityUser) e).addAbilities(BOSS_FLAG, IGNORE_WATER);
                                this.mob.getLookControl().lookAt(this.mob.getEyePos().add(this.dir));
                                this.mob.playSound(SoundEvents.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 2.0f, 1.8f);
                                this.mob.playSound(SoundEvents.ENTITY_WITHER_BREAK_BLOCK, 0.2f, 2.0f);
                                this.dir = Util.rotateTowards(this.dir, this.target.getEyePos().subtract(this.mob.getEyePos())
                                        .add(this.target.getVelocity().multiply(1, 0, 1)).normalize(), this.enraged ? 0.9 : 0.5);
                            }
                        }
                        case BURST -> {
                            if (this.attackingTicks <= 15 && this.attackingTicks > 7 && this.attackingTicks % 3 == 0) {
                                Vec3d tmp = this.rotateAndPredict(1.1, 1, 1);
                                this.lookAt(tmp);
                                Entity e = Util.shootProjectile(world, this.mob, Items.GOLD_NUGGET, enraged ? 22 : 16, tmp, 1.5,
                                        0.05, 30, true);
                                ((AbilityUser) e).addAbilities(BOSS_FLAG, IGNORE_WATER);
                                e.setGlowing(true);
                                this.mob.getLookControl().lookAt(this.mob.getEyePos().add(tmp));
                                this.mob.playSound(SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 0.3f, 1.5f);
                                this.mob.playSound(SoundEvents.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 1.4f, 1.8f);
                            }
                        }
                        case ROCKET -> {
                            if (this.attackingTicks == 10 || this.attackingTicks == 30) {
                                this.updateAndLook();
                                FireworkRocketEntity e = new FireworkRocketEntity(world, GUNNER_ROCKET, this.mob, this.mob.getEyePos().x,
                                        this.mob.getEyeY(), this.mob.getEyePos().z, true);
                                e.setVelocity(this.dir.normalize().multiply(enraged ? 0.55 : 0.45));
                                ((ProjectileInterface) e).setLife(Math.max(5, this.target.getEyePos().subtract(this.mob.getEyePos()).length()));
                                ((AbilityUser) e).addAbilities(BOSS_FLAG, IGNORE_WATER);
                                world.spawnEntity(e);
                            }
                        }
                        case SHOTGUN -> {
                            if (this.attackingTicks == 10 || this.attackingTicks == 30) {
                                this.dir = this.target.getEyePos().subtract(this.mob.getEyePos()).normalize()
                                        .add(this.target.getVelocity().multiply(1.6, 0, 1.6)).normalize();
                                Util.damagingRaycast(world, this.mob, this.mob.getEyePos(), this.dir.multiply(0.125), 96, 0.08,
                                        0, ParticleTypes.ELECTRIC_SPARK, false, false);
                                this.mob.getLookControl().lookAt(this.mob.getEyePos().add(this.dir));
                            }
                            if (this.attackingTicks == 5 || this.attackingTicks == 25) {
                                for (int i = 0; i < (enraged ? 20 : 12); ++i) {
                                    Entity e =Util.shootProjectile(world, this.mob, Items.GOLD_NUGGET, 8, this.dir, 1.3,
                                            enraged ? 0.32 : 0.24, 12, true);
                                    ((AbilityUser) e).addAbilities(BOSS_FLAG, IGNORE_WATER);
                                    e.setGlowing(true);
                                }
                                this.mob.getLookControl().lookAt(this.mob.getEyePos().add(this.dir));
                                this.mob.playSound(SoundEvents.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 0.6f, 2.0f);
                                this.mob.playSound(SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 1.0f, 1.5f);
                                this.mob.playSound(SoundEvents.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 2.0f, 1.4f);
                                this.mob.playSound(SoundEvents.ENTITY_WITHER_SHOOT, 0.3f, 1.5f);
                                this.mob.playSound(SoundEvents.ENTITY_SHULKER_BULLET_HIT, 2.0f, 0.4f);
                            }
                        }
                        case SNIPE -> {
                            if (this.attackingTicks == (enraged ? 20 : 8)) {
                                this.updateAndLook();
                                Util.damagingRaycast(world, this.mob, this.mob.getEyePos(), this.dir.multiply(0.125), 480, 0.08,
                                        0, ParticleTypes.ELECTRIC_SPARK, this.enraged, false);
                            }
                            if (this.attackingTicks == 1) {
                                this.mob.playSound(SoundEvents.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 0.6f, 2.0f);
                                this.mob.playSound(SoundEvents.ITEM_TOTEM_USE, 0.35f, 2.0f);
                                this.mob.playSound(SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 2.0f, 2.0f);
                                this.mob.playSound(SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT, 1.0f, 1.8f);
                                Util.damagingRaycast(world, this.mob, this.mob.getEyePos(), this.dir.multiply(0.25), 240, 0.08,
                                        45, ParticleTypes.CRIT, true, true);
                                this.mob.swingHand(Hand.MAIN_HAND);
                            }
                        }
                        case TRIDENT -> {
                            this.mob.getLookControl().lookAt(this.target, 180, 180);
                            if (this.attackingTicks == 48) {
                                this.mob.setCurrentHand(Hand.OFF_HAND);
                            } else if (this.attackingTicks % 8 == 0) {
                                this.updateAndLook();
                                TridentEntity tridentEntity = new TridentEntity(world, this.mob, this.mob.getOffHandStack());
                                tridentEntity.setVelocity(this.dir.multiply(1.5));
                                tridentEntity.setNoGravity(true);
                                tridentEntity.setGlowing(true);
                                ((ProjectileInterface) tridentEntity).setLife(this.target.getEyePos().subtract(this.mob.getEyePos()).length());
                                ((AbilityUser) tridentEntity).addAbilities(BOSS_FLAG, IGNORE_WATER);
                                world.spawnEntity(tridentEntity);
                            }
                        }
                        default -> {
                            this.attackingTicks = 0;
                            this.cooldown = this.enraged ? 10 : 25;
                            this.mob.setAttacking(this.enraged = false);
                            this.mob.setStackInHand(Hand.MAIN_HAND, Items.WOODEN_HOE.getDefaultStack());
                        }
                    }
                }
                if (canSee) {
                    --this.attackingTicks;
                } else {
                    this.enraged = true;
                }
                if (this.attackingTicks == 0) {
                    this.cooldown = this.enraged ? 10 : 25;
                    this.mob.setAttacking(this.enraged = false);
                    this.mob.setStackInHand(Hand.MAIN_HAND, Items.WOODEN_HOE.getDefaultStack());
                    this.mob.clearActiveItem();
                    if (this.rageCooldown <= 0) {
                        this.rageCooldown = 400;
                    }
                }
            } else {
                double d = this.mob.squaredDistanceTo(this.target);
                if (this.rageCooldown <= 0 || this.mob.isSubmergedInWater()) {
                    this.attack = Attack.TRIDENT;
                    this.attackingTicks = this.attack.duration;
                } else if (!canSee) {
                    --this.rageCooldown;
                } else if (this.cooldown > 0) {
                    if (d > 49) {
                        this.mob.getNavigation().startMovingAlong(this.mob.getNavigation().findPathTo(this.target, 5), 1.2);
                        this.mob.getLookControl().lookAt(this.target, 30.0f, 30.0f);
                    }
                    --this.cooldown;
                } else {
                    List<Attack> attacks = new ArrayList<>();
                    Collections.addAll(attacks, Attack.BURST, Attack.AUTO);
                    if (d < 49) {
                        Collections.addAll(attacks, Attack.SHOTGUN, Attack.SHOTGUN);
                    } else if (d > 169) {
                        Collections.addAll(attacks, Attack.SNIPE, Attack.SNIPE);
                    } else {
                        Collections.addAll(attacks, Attack.ROCKET, Attack.ROCKET);
                    }
                    this.mob.getNavigation().stop();
                    this.mob.getLookControl().lookAt(this.target, 180, 180);
                    this.mob.setAttacking(true);
                    this.attack = attacks.get(this.mob.getRandom().nextInt(attacks.size()));
                    this.mob.setStackInHand(Hand.MAIN_HAND, this.attack.item.getDefaultStack());
                    this.dir = this.target.getEyePos().subtract(this.mob.getEyePos()).normalize();
                    this.attackingTicks = this.attack.duration;
                    this.enraged = d >= 2500;
                }
            }
        }
    }

    private Vec3d rotateAndPredict(double scale, double maxRotate, double predictiveRotate) {
        Vec3d targetVec = this.target.getEyePos().subtract(this.mob.getEyePos()).normalize();
        this.dir = Util.rotateTowards(this.dir, targetVec, maxRotate);
        return Util.rotateTowards(this.dir, targetVec.add(this.target.getVelocity().multiply(scale, 0, scale))
                .normalize(), predictiveRotate);
    }

    private void lookAt(Vec3d lookDir) {
        Vec3d p = this.mob.getEyePos().add(lookDir);
        this.mob.getLookControl().lookAt(p.x, p.y, p.z, 180, 180);
    }

    private void updateAndLook() {
        this.dir = this.target.getEyePos().subtract(this.mob.getEyePos()).normalize();
        Vec3d p = this.mob.getEyePos().add(this.dir);
        this.mob.getLookControl().lookAt(p.x, p.y, p.z, 180, 180);
    }

    private void attack(LivingEntity target) {
        if (this.canAttack(target)) {
            this.resetCooldown();
            this.mob.swingHand(Hand.MAIN_HAND);
            this.mob.tryAttack(MeleeAttackGoal.getServerWorld(this.mob), target);
        }
    }

    private void resetCooldown() {
        this.melee = 15;
    }

    private boolean isCooledDown() {
        return this.melee <= 0;
    }

    private boolean canAttack(LivingEntity target) {
        return this.isCooledDown() && this.mob.isInAttackRange(target) && this.mob.getVisibilityCache().canSee(target);
    }

}

