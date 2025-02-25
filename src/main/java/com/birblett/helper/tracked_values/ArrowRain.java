package com.birblett.helper.tracked_values;

import com.birblett.EpicMod;
import com.birblett.helper.Ability;
import com.birblett.helper.AnonymousTicker;
import com.birblett.helper.Util;
import com.birblett.interfaces.AbilityUser;
import com.birblett.interfaces.ProjectileInterface;
import com.birblett.interfaces.RangedWeapon;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ArrowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.RangedWeaponItem;
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
        this.ticks = 12;
    }

    @Override
    public void tick() {
        if (this.ticks % 2 == 0 && this.weaponStack.getItem() instanceof RangedWeaponItem w) {
            Vec3d spawnPos = Util.applyDivergence(new Vec3d(0, 1, 0), 0.3).normalize().multiply(10 +
                    this.entity.getRandom().nextBetween(0, 200) / 100.0).add(this.target);
            Vec3d velocity = Util.applyDivergence(this.target.subtract(spawnPos).normalize(), 0.03).normalize().multiply(3);
            ProjectileEntity projectileEntity = ((RangedWeapon) w).createProjectile(this.world, this.entity, this.weaponStack,
                    this.projectileStack, true);
            if (projectileEntity instanceof PersistentProjectileEntity p) {
                p.setCritical(true);
                p.setNoClip(true);
                p.pickupType = PersistentProjectileEntity.PickupPermission.CREATIVE_ONLY;
            } else {
                ((AbilityUser) projectileEntity).addAbilities(Ability.NOCLIP);
            }
            projectileEntity.setVelocity(velocity);
            projectileEntity.setPosition(spawnPos);
            if (Util.hasEnchant(this.weaponStack, Enchantments.FLAME, this.world)) {
                projectileEntity.setOnFireFor(1000);
            }
            ((ProjectileInterface) projectileEntity).setTargetY(target.y + 5);
            ((AbilityUser) projectileEntity).addAbilities(Ability.IGNORE_IFRAMES, Ability.NO_KNOCKBACK, Ability.DISCARD_AFTER);
            Vec3d p = spawnPos.subtract(velocity);
            Util.rotateEntity(projectileEntity, velocity.normalize());
            this.world.spawnParticles(ParticleTypes.CLOUD, p.x, p.y, p.z, 10, 0, 0, 0, 0.03);
            this.world.spawnEntity(projectileEntity);
            Util.applyProjectileMods(this.entity, this.weaponStack, this.projectileStack, this.world, projectileEntity, true);
        }
        --this.ticks;
    }

    @Override
    public boolean shouldRemove() {
        return !(this.weaponStack.getItem() instanceof RangedWeaponItem) || this.ticks == 0;
    }

}
