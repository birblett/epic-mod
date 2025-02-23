package com.birblett.helper.tracked_values;

import com.birblett.helper.Ability;
import com.birblett.helper.AnonymousTicker;
import com.birblett.helper.Util;
import com.birblett.interfaces.AbilityUser;
import com.birblett.interfaces.ProjectileInterface;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ArrowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

public class ArrowRain extends AnonymousTicker {

    private final Vec3d target;
    private final ServerWorld world;
    private final ItemStack weaponStack;
    private final ItemStack projectileStack;
    private int ticks;

    public ArrowRain(LivingEntity entity, ServerWorld world, Vec3d target, ItemStack weaponStack, ItemStack projectileStack) {
        super(entity);
        this.world = world;
        this.target = target;
        this.weaponStack = weaponStack;
        this.projectileStack = projectileStack;
        this.ticks = 16;
    }

    @Override
    public void tick() {
        if (this.ticks % 2 == 0) {
            Vec3d spawnPos = Util.applyDivergence(new Vec3d(0, 1, 0), 0.3).normalize().multiply(10 +
                    this.entity.getRandom().nextBetween(0, 200) / 100.0).add(this.target);
            Vec3d velocity = Util.applyDivergence(this.target.subtract(spawnPos).normalize(), 0.03).normalize().multiply(3);
            PersistentProjectileEntity persistentProjectileEntity = (this.projectileStack.getItem() instanceof ArrowItem a ? a : (ArrowItem)
                    Items.ARROW).createArrow(this.world, this.projectileStack, this.entity, this.weaponStack);
            persistentProjectileEntity.setCritical(true);
            persistentProjectileEntity.setPosition(spawnPos);
            persistentProjectileEntity.setNoClip(true);
            persistentProjectileEntity.setVelocity(velocity);
            persistentProjectileEntity.pickupType = PersistentProjectileEntity.PickupPermission.CREATIVE_ONLY;
            if (Util.hasEnchant(this.weaponStack, Enchantments.FLAME, this.world)) {
                persistentProjectileEntity.setOnFireFor(1000);
            }
            ((ProjectileInterface) persistentProjectileEntity).setTargetY(target.y + 5);
            ((AbilityUser) persistentProjectileEntity).addAbilities(Ability.IGNORE_IFRAMES, Ability.NO_KNOCKBACK, Ability.DISCARD_AFTER);
            Vec3d p = spawnPos.subtract(velocity);
            Util.rotateEntity(persistentProjectileEntity, velocity.normalize());
            this.world.spawnParticles(ParticleTypes.CLOUD, p.x, p.y, p.z, 10, 0, 0, 0, 0.03);
            this.world.spawnEntity(persistentProjectileEntity);
            Util.applyArrowModifications(this.entity, this.weaponStack, this.world, persistentProjectileEntity, true);
        }
        --this.ticks;
    }

    @Override
    public boolean shouldRemove() {
        return this.ticks == 0;
    }

}
