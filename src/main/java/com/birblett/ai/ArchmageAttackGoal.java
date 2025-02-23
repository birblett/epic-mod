package com.birblett.ai;

import com.birblett.helper.Ability;
import com.birblett.helper.Util;
import com.birblett.interfaces.AbilityUser;
import com.birblett.interfaces.Mage;
import com.birblett.interfaces.ProjectileInterface;
import net.minecraft.block.BlockState;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.EvokerFangsEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.event.GameEvent;

import java.util.ArrayList;
import java.util.List;

import static com.birblett.ai.ArchmageAttackGoal.ZombieMageAttack.*;
import static com.birblett.helper.Ability.*;

public class ArchmageAttackGoal extends Goal {

    enum ZombieMageAttack {
        CROSS_LIGHTNING(45, Items.COPPER_BLOCK),
        ERUPTION(80, Items.MAGMA_BLOCK),
        FANGS(35, Items.EMERALD_BLOCK),
        ICICLES(40, Items.PACKED_ICE);

        public final int duration;
        public final Item headItem;
        ZombieMageAttack(int i, Item item) {
            this.duration = i;
            this.headItem = item;
        }
    }

    private final ZombieEntity zombie;
    private LivingEntity target = null;
    private Vec3d[] targetVec = null;
    private int cooldownTicks = 0;
    private int attackingTicks = 0;
    private ZombieMageAttack attack = null;
    private ZombieMageAttack lastAttack = null;

    public ArchmageAttackGoal(ZombieEntity mob) {
        this.zombie = mob;
    }

    @Override
    public boolean canStart() {
        boolean b = (this.zombie.isAlive() && !((Mage) this.zombie).teleporting() && (this.target = this.zombie.getTarget()) != null &&
                (this.zombie.canSee(this.target) || attackingTicks > 0));
        if (!b) {
            this.cooldownTicks = 10;
            this.attackingTicks = 0;
            this.attack = null;
            this.zombie.equipStack(EquipmentSlot.HEAD, Items.CREAKING_HEART.getDefaultStack());
            ((Mage) this.zombie).setAttack(false);
            this.zombie.setAttacking(false);
        }
        return b;
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }

    private static final Vec3d[] CROSS_STRAIGHT =  new Vec3d[]{new Vec3d(1, 0, 0), new Vec3d(-1, 0, 0),
            new Vec3d(0, 0, 1), new Vec3d(0, 0, -1)};
    private static final Vec3d[] CROSS_DIAGONAL =  new Vec3d[]{new Vec3d(1, 0, 1), new Vec3d(-1, 0, -1),
            new Vec3d(-1, 0, 1), new Vec3d(1, 0, -1)};

    @Override
    public void tick() {
        if (this.zombie.getWorld() instanceof ServerWorld world) {
            boolean phase2 = this.zombie.getHealth() / this.zombie.getMaxHealth() < 0.5f;
            if (this.attack != null) {
                --this.attackingTicks;
                switch (this.attack) {
                    case CROSS_LIGHTNING -> {
                        Vec3d tmp = this.targetVec[0];
                        if (this.attackingTicks == 40) {
                            for (int i = 0; i < (phase2 ? 40 : 20); ++i) {
                                double diff = i / 3.0;
                                world.spawnParticles(ParticleTypes.END_ROD, tmp.x + diff, tmp.y + 1, tmp.z, 10, 0.1,
                                        1, 0.1, 0);
                                world.spawnParticles(ParticleTypes.END_ROD, tmp.x - diff, tmp.y + 1, tmp.z, 10, 0.1,
                                        1, 0.1, 0);
                                world.spawnParticles(ParticleTypes.END_ROD, tmp.x, tmp.y + 1, tmp.z + diff, 10, 0.1,
                                        1, 0.1, 0);
                                world.spawnParticles(ParticleTypes.END_ROD, tmp.x, tmp.y + 1, tmp.z - diff, 10, 0.1,
                                        1, 0.1, 0);
                            }
                        } else if (this.attackingTicks == 30) {
                            for (int i = 0; i < (phase2 ? 40 : 20); ++i) {
                                double diff = i / 3.0 * 0.7071;
                                world.spawnParticles(ParticleTypes.END_ROD, tmp.x + diff, tmp.y + 1, tmp.z + diff, 10,
                                        0.1, 1, 0.1, 0);
                                world.spawnParticles(ParticleTypes.END_ROD, tmp.x - diff, tmp.y + 1, tmp.z - diff, 10,
                                        0.1, 1, 0.1, 0);
                                world.spawnParticles(ParticleTypes.END_ROD, tmp.x - diff, tmp.y + 1, tmp.z + diff, 10,
                                        0.1, 1, 0.1, 0);
                                world.spawnParticles(ParticleTypes.END_ROD, tmp.x + diff, tmp.y + 1, tmp.z - diff, 10,
                                        0.1, 1, 0.1, 0);
                            }
                        } else if (this.attackingTicks == 20 || this.attackingTicks == 0) {
                            for (int i = 1; i < (phase2 ? 14 : 7); i += 2) {
                                double mul = this.attackingTicks == 20 ? i : i * 0.7071;
                                for (Vec3d vec : this.attackingTicks == 20 ? CROSS_STRAIGHT : CROSS_DIAGONAL) {
                                    LightningEntity e = new LightningEntity(EntityType.LIGHTNING_BOLT, world);
                                    ((AbilityUser) e).addAbilities(BOSS_FLAG);
                                    e.setPosition(tmp.add(vec.multiply(mul)));
                                    world.spawnEntity(e);
                                }
                            }
                        }
                    }
                    case ERUPTION -> {
                        if (phase2 && (this.attackingTicks == 39 || this.attackingTicks == 79)) {
                            this.attackingTicks -= 10;
                        } else if (this.attackingTicks < 20 || this.attackingTicks >= 40 && this.attackingTicks < 60) {
                            boolean second = this.attackingTicks < 20;
                            if (this.attackingTicks % 3 == 0) {
                                this.zombie.playSound(SoundEvents.ENTITY_BLAZE_SHOOT, 0.6f, 1.4f);
                            }
                            double mul = (second ? (this.attackingTicks - 20) : (60 - this.attackingTicks)) / 80.0;
                            Vec3d tmp = this.targetVec[0].subtract(this.zombie.getEyePos()).multiply(1, 0, 1).normalize()
                                    .multiply(0.25 + mul).add(0, 0.6, 0);
                            double c1 = second ? 0.866 : 0.9397, s1 = second ? 0.5 : 0.342;
                            for (int i = 0; i < (second ? 12 : 18); ++i) {
                                Item it = List.of(Items.MAGMA_BLOCK, Items.MAGMA_BLOCK, Items.FIRE_CHARGE, Items.OBSIDIAN)
                                        .get(this.zombie.getRandom().nextInt(4));
                                SnowballEntity magma = new SnowballEntity(world, this.zombie, it.getDefaultStack());
                                magma.setVelocity(tmp.addRandom(this.zombie.getRandom(), second ? 0.08f : 0.1f));
                                ((AbilityUser) magma).addAbilities(IGNORE_IFRAMES, CLIP_OWNER, BOSS_FLAG, NOCLIP, IGNORE_WATER);
                                ((ProjectileInterface) magma).setDamage(15);
                                ((ProjectileInterface) magma).setLife(18);
                                magma.setGlowing(true);
                                magma.noClip = true;
                                world.spawnEntity(magma);
                                tmp = new Vec3d(tmp.x * c1 + tmp.z * s1, tmp.y, -tmp.x * s1 + tmp.z * c1);
                            }
                        }
                    }
                    case FANGS -> {
                        if (this.attackingTicks == 34) {
                            Vec3d tmp = this.targetVec[0].subtract(this.zombie.getEyePos()).multiply(1, 0, 1).normalize().multiply(0.7);
                            double c1 = 0.866, s1 = 0.5, c2 = 0.5, s2 = 0.866, c3 = 0, s3 = 1;
                            this.targetVec = new Vec3d[]{tmp,
                                    new Vec3d(tmp.x * c1 + tmp.z * s1, tmp.y, -tmp.x * s1 + tmp.z * c1),
                                    new Vec3d(tmp.x * c1 + tmp.z * -s1, tmp.y, -tmp.x * -s1 + tmp.z * c1),
                                    new Vec3d(tmp.x * c2 + tmp.z * s2, tmp.y, -tmp.x * s2 + tmp.z * c2),
                                    new Vec3d(tmp.x * c2 + tmp.z * -s2, tmp.y, -tmp.x * -s2 + tmp.z * c2),
                                    new Vec3d(tmp.x * c3 + tmp.z * s3, tmp.y, -tmp.x * s3 + tmp.z * c3),
                                    new Vec3d(tmp.x * c3 + tmp.z * -s3, tmp.y, -tmp.x * -s3 + tmp.z * c3)};
                        } else if (this.attackingTicks < 30) {
                            int i = 29 - this.attackingTicks;
                            for (int j = 0; j < (phase2 ? 7 : 5); ++j) {
                                Vec3d offset = this.targetVec[j].multiply(i);
                                float f = (float) MathHelper.atan2(offset.z, offset.x);
                                Vec3d summon = this.zombie.getEyePos().add(offset);
                                this.conjureFangs(summon.x, summon.z, this.target.getY() - 1, Math.max(summon.y, this.target.getY() +
                                        this.target.getHeight()), f * i, phase2 ? 8 : 10);
                            }
                        }
                    }
                    case ICICLES -> {
                        if (this.attackingTicks < 21) {
                            Item i = List.of(Items.PACKED_ICE, Items.ICE, Items.BLUE_ICE).get(this.zombie.getRandom().nextInt(3));
                            SnowballEntity icicle = new SnowballEntity(world, this.zombie, i.getDefaultStack());
                            Vec3d spawnPos = this.targetVec[0].add(0, 6, 0);
                            icicle.setPosition(spawnPos.addRandom(this.zombie.getRandom(), 1));
                            icicle.setVelocity(new Vec3d(0, -0.5, 0).addRandom(this.zombie.getRandom(), 0.3f));
                            ((AbilityUser) icicle).addAbilities(IGNORE_IFRAMES, CLIP_OWNER, BOSS_FLAG, IGNORE_WATER);
                            ((ProjectileInterface) icicle).setDamage(18);
                            world.spawnParticles(ParticleTypes.POOF, spawnPos.x, spawnPos.y, spawnPos.z, 2, 0.5, 0.3, 0.5, 0.05);
                            world.spawnEntity(icicle);
                            int max = phase2 ? 20 : 18;
                            if (this.attackingTicks % (phase2 ? 4 : 6) == 0) {
                                for (int start = phase2 ? 0 : 1; start < (phase2 ? 3 : 2); ++start) {
                                    Vec3d aim = this.target.getEyePos().subtract(this.zombie.getEyePos()).normalize();
                                    if (start != 1) {
                                        aim = Util.rotateAsPlane(aim, (start - 1) * 0.56);
                                    }
                                    icicle = new SnowballEntity(world, this.zombie, i.getDefaultStack());
                                    double mul = 0.5 + (max - this.attackingTicks) / 25.0;
                                    icicle.setVelocity(aim.multiply(mul));
                                    icicle.setNoGravity(true);
                                    icicle.setGlowing(true);
                                    icicle.noClip = true;
                                    ((AbilityUser) icicle).addAbilities(IGNORE_IFRAMES, CLIP_OWNER, BOSS_FLAG, NOCLIP);
                                    ((ProjectileInterface) icicle).setDamage(21);
                                    ((ProjectileInterface) icicle).setLife(((max - this.attackingTicks) + 6) * 3);
                                    world.spawnEntity(icicle);
                                    world.playSound(null, this.zombie.getX(), this.zombie.getY(), this.zombie.getZ(),
                                            SoundEvents.BLOCK_GLASS_BREAK, this.zombie.getSoundCategory(), 1.0F, 2.0F);
                                }
                            }
                        }
                    }
                }
                if (this.attackingTicks <= 0) {
                    this.lastAttack = attack;
                    this.attack = null;
                    this.cooldownTicks = 20;
                    this.zombie.equipStack(EquipmentSlot.HEAD, Items.CREAKING_HEART.getDefaultStack());
                    this.zombie.setAttacking(false);
                    ((Mage) this.zombie).setAttack(false);
                }
            } else if (this.cooldownTicks <= 0) {
                List<ZombieMageAttack> possibleAttacks = new ArrayList<>();
                for (ZombieMageAttack attack : ZombieMageAttack.values()) {
                    if (this.lastAttack != attack) {
                        possibleAttacks.add(attack);
                    }
                }
                if (this.lastAttack != ICICLES && this.target.squaredDistanceTo(this.zombie) < 4) {
                    possibleAttacks.add(ICICLES);
                } else if (this.lastAttack != CROSS_LIGHTNING && this.target.squaredDistanceTo(this.zombie) > 9) {
                    possibleAttacks.add(CROSS_LIGHTNING);
                } else if (this.lastAttack != FANGS) {
                    possibleAttacks.add(FANGS);
                }
                this.attack = possibleAttacks.get(this.zombie.getRandom().nextInt(possibleAttacks.size()));
                this.zombie.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, this.attackingTicks = this.attack.duration,
                        1, false, false));
                this.targetVec = new Vec3d[]{this.target.getPos()};
                this.zombie.equipStack(EquipmentSlot.HEAD, this.attack.headItem.getDefaultStack());
                ((Mage) this.zombie).setAttack(true);
                this.zombie.setAttacking(true);
            } else {
                --this.cooldownTicks;
            }
        }
    }

    private void conjureFangs(double x, double z, double maxY, double y, float yaw, int warmup) {
        BlockPos blockPos = BlockPos.ofFloored(x, y, z);
        boolean bl = false;
        double d = 0.0;
        do {
            VoxelShape voxelShape;
            BlockPos blockPos2 = blockPos.down();
            BlockState blockState = this.zombie.getWorld().getBlockState(blockPos2);
            if (blockState.getCollisionShape(this.zombie.getWorld(), blockPos2).isEmpty()) continue;
            if (!this.zombie.getWorld().isAir(blockPos) && !(voxelShape = this.zombie.getWorld().getBlockState(blockPos)
                    .getCollisionShape(this.zombie.getWorld(), blockPos)).isEmpty()) {
                d = voxelShape.getMax(Direction.Axis.Y);
            }
            bl = true;
            break;
        } while ((blockPos = blockPos.down()).getY() >= MathHelper.floor(maxY) - 1);
        if (!bl && this.target != null) {
            blockPos = blockPos.withY(this.target.getBlockY());
        }
        if (this.zombie.getWorld() instanceof ServerWorld world) {
            y = (double) blockPos.getY() + d;
            world.spawnParticles(ParticleTypes.ENCHANTED_HIT, x, y + 1.2, z, 10, 0, 1, 0, 0.1);
            Entity e = new EvokerFangsEntity(this.zombie.getWorld(), x, y, z, yaw, warmup, this.zombie);
            ((AbilityUser) e).addAbilities(BOSS_FLAG, IGNORE_IFRAMES);
            world.spawnEntity(e);
            world.emitGameEvent(GameEvent.ENTITY_PLACE, new Vec3d(x, (double)blockPos.getY() + d, z), GameEvent.Emitter.of(this.zombie));
        }
    }

}
