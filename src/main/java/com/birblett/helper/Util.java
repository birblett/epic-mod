package com.birblett.helper;

import com.birblett.EpicMod;
import com.birblett.helper.tracked_values.BurstFire;
import com.birblett.helper.tracked_values.Homing;
import com.birblett.interfaces.AbilityUser;
import com.birblett.interfaces.ProjectileInterface;
import com.birblett.interfaces.ServerPlayerEntityInterface;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.component.type.ChargedProjectilesComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonPart;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.LlamaSpitEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.item.*;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import net.minecraft.world.WorldEvents;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.world.explosion.ExplosionImpl;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.Random;

public class Util {

    public static void setAttributeBase(LivingEntity entity, RegistryEntry<EntityAttribute> attribute, double baseValue) {
        EntityAttributeInstance e;
        if ((e = entity.getAttributeInstance(attribute)) != null) {
            e.setBaseValue(baseValue);
            if (attribute == EntityAttributes.MAX_HEALTH) entity.heal((float) baseValue);
        }
    }

    public static boolean isTouchingBlock(Entity self, double xTolerance, double yTolerance, double zTolerance) {
        Box box = self.getBoundingBox().expand(xTolerance, yTolerance, zTolerance);
        double[] corners = {box.minX, box.maxX, box.minY, box.maxY, box.minZ, box.maxZ};
        for (int xPos = 0; xPos < 2; xPos++) {
            for (int yPos = 2; yPos < 4; yPos++) {
                for (int zPos = 4; zPos < 6; zPos++) {
                    BlockPos corner = BlockPos.ofFloored(corners[xPos], corners[yPos], corners[zPos]);
                    BlockState blockState = self.getWorld().getBlockState(corner);
                    VoxelShape vs = blockState.getCollisionShape(self.getWorld(), corner, ShapeContext.of(self));
                    if (!vs.isEmpty() && vs.getBoundingBox().offset(corner).intersects(box)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean isTouchingBlock(Entity self, double tolerance) {
        return isTouchingBlock(self, tolerance, tolerance, tolerance);
    }

    public static boolean hasEnchant(LivingEntity e, EquipmentSlot slot, RegistryKey<Enchantment> key) {
        return e != null && Util.hasEnchant(e.getEquippedStack(slot), key, e.getWorld());
    }

    public static boolean hasEnchant(ItemStack stack, RegistryKey<Enchantment> key, World world) {
        RegistryEntry<Enchantment> e = getEntry(world, key);
        return e != null && EnchantmentHelper.getLevel(e, stack) > 0;
    }

    public static int getEnchantLevel(ItemStack stack, RegistryKey<Enchantment> key, World world) {
        RegistryEntry<Enchantment> e = getEntry(world, key);
        return e != null ? EnchantmentHelper.getLevel(e, stack) : 0;
    }

    public static RegistryEntry<Enchantment> getEntry(World world, RegistryKey<Enchantment> e) {
        Optional<Registry<Enchantment>> reg = world.getRegistryManager().getOptional(RegistryKeys.ENCHANTMENT);
        Optional<RegistryEntry.Reference<Enchantment>> optional = Optional.empty();
        if (reg.isPresent()) {
            optional = reg.get().getEntry(e.getValue());
        }
        return optional.orElse(null);
    }

    public static boolean adaptabilityAmmo(ItemStack stack) {
        Item i = stack.getItem();
        return i == Items.WIND_CHARGE || i == Items.FIRE_CHARGE || i == Items.ENDER_PEARL || i == Items.SPLASH_POTION || i == Items.SNOWBALL
                || i == Items.EGG || i == Items.DRAGON_BREATH;
    }

    public static boolean isRiderPlayer(PlayerEntity player) {
        ItemStack stack = player.getEquippedStack(EquipmentSlot.FEET);
        return stack != ItemStack.EMPTY && hasEnchant(stack, EpicMod.RIDER, player.getWorld());
    }

    public static void mobBreakBlocks(Entity entity, int bx, int by) {
        boolean bl = false;
        int j = MathHelper.floor(entity.getWidth() / 2.0f + 1.0f) + bx;
        int k = MathHelper.floor(entity.getHeight()) + by;
        for (BlockPos blockPos : BlockPos.iterate(entity.getBlockX() - j, entity.getBlockY(), entity.getBlockZ() - j,
                entity.getBlockX() + j, entity.getBlockY() + k, entity.getBlockZ() + j)) {
            BlockState blockState = entity.getWorld().getBlockState(blockPos);
            if (!WitherEntity.canDestroy(blockState)) continue;
            bl = entity.getWorld().breakBlock(blockPos, true, entity) || bl;
        }
        if (bl) {
            entity.getWorld().syncWorldEvent(null, WorldEvents.WITHER_BREAKS_BLOCK, entity.getBlockPos(), 0);
        }
    }

    private static final Random RANDOM = new Random();

    private static final Vec3d AXIS_X = new Vec3d(1, 0, 0);
    private static final Vec3d AXIS_Y = new Vec3d(0, 1, 0);

    public static SnowballEntity shootProjectile(ServerWorld world, LivingEntity user, Item item, float damage, Vec3d vel, double speed, double divergence, double range, boolean spawn) {
        SnowballEntity e = new SnowballEntity(world, user, item.getDefaultStack());
        e.setPosition(user.getEyePos().add(user.getRotationVector().multiply(0.2)));
        if (divergence != 0) {
            vel = applyDivergence(vel, divergence).normalize();
        }
        e.setVelocity(vel.multiply(speed));
        e.setNoGravity(true);
        ((ProjectileInterface) e).setLife(range);
        ((ProjectileInterface) e).setDamage(damage);
        if (spawn) {
            world.spawnEntity(e);
        }
        return e;
    }

    public static Vec3d rotateAbout(Vec3d base, Vec3d axis, double angle) {
        double cos = Math.cos(angle);
        return base.multiply(cos).add(axis.crossProduct(base).multiply(Math.sin(angle))).add(axis.multiply(axis.dotProduct(base))
                .multiply(1 - cos));
    }

    public static Vec3d applyDivergence(Vec3d in, double maxAngle) {
        in = in.normalize();
        return rotateAbout(rotateAbout(in, (in.y == 1 || in.y == -1 ? AXIS_X : AXIS_Y).crossProduct(in).normalize(),
                RANDOM.nextDouble() * maxAngle), in, RANDOM.nextDouble() * Math.PI * 2).normalize();
    }

    public static Vec3d rotateAsPlane(Vec3d in, double angle) {
        Vec3d axis = (in = in.normalize()).y == 1 || in.y == -1 ? AXIS_X : in.crossProduct(AXIS_Y).normalize().crossProduct(in);
        return rotateAbout(in, axis, angle).normalize();
    }

    public static Vec3d rotateTowards(Vec3d base, Vec3d target, double angle) {
        if (base.lengthSquared() == 0 || target.lengthSquared() == 0) {
            return base;
        }
        base = base.normalize();
        target = target.normalize();
        return rotateAbout(base, base.crossProduct(target).normalize(), Math.clamp(Math.atan2(base.crossProduct(target).length(),
                base.dotProduct(target)), 0, Math.abs(angle)));
    }

    public static void rotateEntity(Entity e, Vec3d rotation) {
        rotation = rotation.normalize();
        e.rotate((float) Math.toDegrees(Math.atan2(rotation.x, rotation.z)), (float) Math.toDegrees(Math.asin(rotation.y)));
    }

    public static HitResult damagingRaycast(ServerWorld world, LivingEntity user, Vec3d pos, Vec3d dir, int length, double collision, float damage, ParticleEffect p, boolean ignoreBlocks, boolean blockBreakParticles) {
        return damagingRaycast(world, user, pos, dir, length, collision, damage, p, ignoreBlocks, blockBreakParticles, world.getDamageSources().thrown(user, null), true, 0.01);
    }

    public static HitResult damagingRaycast(ServerWorld world, LivingEntity user, Vec3d pos, Vec3d dir, int length, double collision, float damage, ParticleEffect p, boolean ignoreBlocks, boolean blockBreakParticles, DamageSource source, boolean ignoreIframes, double speed) {
        BlockPos b;
        VoxelShape vs;
        Box box;
        HitResult returnable = null;
        for (int iter = 0; iter < length; iter++) {
            if (!ignoreBlocks) {
                b = new BlockPos((int) Math.floor(pos.x), (int) Math.floor(pos.y), (int) Math.floor(pos.z));
                vs = world.getBlockState(b).getCollisionShape(world, b);
                box = new Box(pos.subtract(collision), pos.add(collision));
                if (!vs.isEmpty() && vs.getBoundingBox().offset(b).intersects(box)) {
                    if (blockBreakParticles) {
                        Util.emitBlockBreakParticles(world, b);
                    }
                    return new BlockHitResult(pos, Direction.UP, b, true);
                }
            }
            if (p != null && iter % 3 == 0) {
                world.spawnParticles(p, pos.x, pos.y, pos.z, 1, 0, 0, 0, speed);
            }
            if (damage > 0 || damage == -1) {
                box = new Box(pos.subtract(collision), pos.add(collision));
                for (Entity entity : world.getOtherEntities(user, box, (e) -> (true))) {
                    if (damage > 0) {
                        if (entity instanceof EnderDragonPart dragonPart) {
                            if (ignoreIframes) {
                                dragonPart.owner.hurtTime = 0;
                                dragonPart.owner.timeUntilRegen = 1;
                            }
                            dragonPart.owner.damagePart(world, dragonPart, source, damage);
                        } else {
                            if (ignoreIframes) {
                                if (entity instanceof LivingEntity livingEntity)  {
                                    livingEntity.hurtTime = 0;
                                }
                                entity.timeUntilRegen = 1;
                            }
                            entity.damage(world, source, damage);
                        }
                    }
                    if (returnable == null) {
                        if (entity instanceof LivingEntity livingEntity) {
                            returnable = new EntityHitResult(livingEntity);
                        } else if (entity instanceof EnderDragonPart dragonPart) {
                            returnable = new EntityHitResult(dragonPart.owner);
                        }
                    }
                }
                if (returnable != null) {
                    return returnable;
                }
            }
            pos = pos.add(dir);
        }
        return null;
    }

    public static void explodeAt(ServerWorld world, @Nullable Entity source, DamageSource damageSource, Vec3d pos, float power, boolean createFire, Explosion.DestructionType type) {
        ExplosionImpl explosionImpl = new ExplosionImpl(world, source, damageSource, null, pos, power, createFire, type);
        explosionImpl.explode();
        ParticleEffect particleEffect = explosionImpl.isSmall() ? ParticleTypes.EXPLOSION : ParticleTypes.EXPLOSION_EMITTER;
        for (ServerPlayerEntity serverPlayerEntity : world.getPlayers()) {
            if (!(serverPlayerEntity.squaredDistanceTo(pos) < 4096.0)) continue;
            Optional<Vec3d> optional = Optional.ofNullable(explosionImpl.getKnockbackByPlayer().get(serverPlayerEntity));
            serverPlayerEntity.networkHandler.sendPacket(new ExplosionS2CPacket(pos, optional, particleEffect, SoundEvents.ENTITY_GENERIC_EXPLODE));
        }
    }

    public static void emitBlockBreakParticles(World world, BlockPos pos) {
        BlockState blockState = world.getBlockState(pos);
        if (!blockState.isAir() && !(blockState.getBlock() instanceof AbstractFireBlock)) world.syncWorldEvent(2001, pos, Block.getRawIdFromState(blockState));
    }

    public static void playSound(ServerWorld world, LivingEntity entity, SoundEvent sound, float volume, float pitch) {
        world.playSound(null, entity.getX(), entity.getY(), entity.getZ(), sound, entity.getSoundCategory(), volume, pitch);
    }

    public static void sendActionBarMessage(Entity user, String s) {
        if (user instanceof ServerPlayerEntity player)
            player.networkHandler.sendPacket(new OverlayMessageS2CPacket(Text.of(s)));
    }

    private static boolean canApplyCrit(LivingEntity shooter, Entity e) {
        return e instanceof PersistentProjectileEntity p && (p.isCritical() || !(shooter instanceof PlayerEntity));
    }

    private static boolean canApplyCrit(LivingEntity shooter, boolean crit) {
        return crit|| !(shooter instanceof PlayerEntity);
    }

    public static void applyArrowModifications(LivingEntity shooter, ItemStack stack, ServerWorld world, ProjectileEntity entity) {
        applyArrowModifications(shooter, stack, world, entity, false);
    }

    public static void applyArrowModifications(LivingEntity shooter, ItemStack stack, ServerWorld world, ProjectileEntity entity, boolean isSummoned) {
        if (!isSummoned && Util.hasEnchant(stack, EpicMod.ARROW_RAIN, world) && canApplyCrit(shooter, entity)) {
            ((AbilityUser) entity).addAbilities(Ability.SUMMON_ARROWS, Ability.NO_KNOCKBACK);
        }
        if (Util.hasEnchant(stack, EpicMod.HEAVY_SHOT, world) || Util.hasEnchant(stack, EpicMod.BURST_FIRE, world)) {
            ((AbilityUser) entity).addAbilities(Ability.IGNORE_IFRAMES);
        }
        if (Util.hasEnchant(stack, EpicMod.THUNDERBOLT, world) && canApplyCrit(shooter, entity)) {
            ((AbilityUser) entity).addAbilities(Ability.SUMMON_LIGHTNING);
        }
        if (Util.hasEnchant(stack, EpicMod.AUTOAIM, world) && canApplyCrit(shooter, entity) && shooter instanceof ServerPlayerEntity) {
            LivingEntity target = ((Homing) ((ServerPlayerEntityInterface) shooter).getTickers(PlayerTicker.ID.HOMING)).getTarget();
            if (target != null) {
                Vec3d targetVec = target.getPos().add(0, target.getHeight() / 2, 0).subtract(entity.getPos());
                double d = targetVec.length();
                entity.setVelocity(Util.rotateTowards(entity.getVelocity(), targetVec.add(0, d * d / (18 * 18), 0), 0.14)
                        .normalize().multiply(entity.getVelocity().length()));
            }
        }

        if (Util.hasEnchant(stack, EpicMod.HITSCAN, shooter.getWorld()) && canApplyCrit(shooter, entity)) {
            entity.lastRenderX = entity.getX();
            entity.lastRenderY = entity.getY();
            entity.lastRenderZ = entity.getZ();
            for (int i = 0; i < 65; i++) {
                entity.tick();
                double x = entity.getX();
                double y = entity.getY();
                double z = entity.getZ();
                entity.lastRenderX = x;
                entity.lastRenderY = y;
                entity.lastRenderZ = z;
                double dx = entity.getVelocity().x;
                double dy = entity.getVelocity().y;
                double dz = entity.getVelocity().z;
                world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 1, 0, 0, 0, 0);
                world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, x - dx * 0.333, y - dy * 0.333, z - dz * 0.333, 1, 0, 0, 0, 0);
                world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, x - dx * 0.666, y - dy * 0.666, z - dz * 0.666, 1, 0, 0, 0, 0);
                // stop if unable to continue
                if (entity.isRemoved()) break;
                if (entity.isOnGround()) break;
            }
        }
        if (Util.canApplyCrit(shooter, shooter.isSneaking()) && shooter.isOnGround() && Util.hasEnchant(shooter, EquipmentSlot.HEAD,
                EpicMod.SNIPER) && entity instanceof PersistentProjectileEntity p) {
            p.setDamage(p.getDamage() + 1);
        }
    }

    public static void rangedWeaponFired(ServerWorld world, LivingEntity shooter, ItemStack stack, Hand hand, List<ItemStack> projectiles, float speed, float divergence, float progress, boolean crit) {
        if (shooter instanceof ServerPlayerEntity player && crit && Util.hasEnchant(stack, EpicMod.BURST_FIRE, world)) {
            BurstFire t = (BurstFire) ((ServerPlayerEntityInterface) player).getTickers(PlayerTicker.ID.BURST_FIRE);
            int duration = 4 + 4 * Util.getEnchantLevel(stack, EpicMod.BURST_FIRE, player.getServerWorld());
            t.setUsing(stack, hand, duration);
            t.setShooting(projectiles, speed, divergence);
            player.getItemCooldownManager().set(stack, duration + 1);
        }
        if (Util.hasEnchant(stack, EpicMod.MIGHTY_WIND, world)) {
            for (int i = 0; i < Math.ceil(progress * 3); i++) {
                LlamaSpitEntity llamaSpitEntity = new LlamaSpitEntity(EntityType.LLAMA_SPIT, world);
                llamaSpitEntity.setOwner(shooter);
                llamaSpitEntity.setPosition(shooter.getEyePos().x, shooter.getEyeY() - 0.1F, shooter.getEyePos().z);
                llamaSpitEntity.setVelocity(shooter.getRotationVector().multiply(progress * 2));
                llamaSpitEntity.setInvisible(true);
                world.spawnEntity(llamaSpitEntity);
                llamaSpitEntity.kill(world);
            }
            world.playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(), SoundEvents.ENTITY_BREEZE_SHOOT, shooter
                    .getSoundCategory(), 0.7F, 1.4F + (shooter.getRandom().nextFloat() - shooter
                    .getRandom().nextFloat()) * 0.2F);
            shooter.setVelocity(shooter.getVelocity().multiply(0.7).add(shooter.getRotationVector().multiply(-1.2 * progress)));
            shooter.velocityModified = true;
            world.getOtherEntities(shooter, new Box(shooter.getEyePos().add(-5, -5, -5), shooter.getEyePos()
                    .add(5, 5, 5)), e -> e.getPos().squaredDistanceTo(shooter.getEyePos().add(shooter.getRotationVector()
                    .multiply(2.5))) <= 9).forEach(e -> {
                e.addVelocity(shooter.getRotationVector().multiply(Math.min(1, progress)));
                e.velocityModified = true;
            });
        }
        if (Util.hasEnchant(stack, EpicMod.FLAK, world) && Util.canApplyCrit(shooter, crit)) {
            Vec3d base = shooter.getRotationVector();
            for (int i = 0; i < 12; ++i) {
                PersistentProjectileEntity arrow = ((ArrowItem) Items.ARROW).createArrow(world, Items.ARROW.getDefaultStack(), shooter, stack);
                Vec3d velocity = Util.applyDivergence(base, i < 6 ? 0.14 : 0.42).normalize();
                arrow.setVelocity(velocity);
                arrow.setCritical(true);
                ((AbilityUser) arrow).addAbilities(Ability.DISCARD_AFTER, Ability.IGNORE_IFRAMES, Ability.NO_KNOCKBACK);
                Util.applyArrowModifications(shooter, stack, world, arrow);
                Util.rotateEntity(arrow, velocity);
                world.spawnEntity(arrow);
            }
        }
    }

}
