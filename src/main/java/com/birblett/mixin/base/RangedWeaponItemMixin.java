package com.birblett.mixin.base;

import com.birblett.EpicMod;
import com.birblett.helper.PlayerTicker;
import com.birblett.helper.Util;
import com.birblett.interfaces.ProjectileInterface;
import com.birblett.interfaces.RangedWeapon;
import com.birblett.interfaces.ServerPlayerEntityInterface;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.*;
import net.minecraft.entity.projectile.thrown.EggEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Consumer;
import java.util.function.Predicate;

@Mixin(RangedWeaponItem.class)
public abstract class RangedWeaponItemMixin implements RangedWeapon {

    @Shadow protected abstract ProjectileEntity createArrowEntity(World world, LivingEntity shooter, ItemStack weaponStack, ItemStack projectileStack, boolean critical);

    @Override
    public ProjectileEntity createProjectile(World world, LivingEntity shooter, ItemStack weaponStack, ItemStack projectileStack, boolean critical) {
        return this.createArrowEntity(world, shooter, weaponStack, projectileStack, critical);
    }

    @WrapOperation(method = "shootAll", at = @At(value = "INVOKE", target = "net/minecraft/entity/projectile/ProjectileEntity.spawn(Lnet/minecraft/entity/projectile/ProjectileEntity;Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/item/ItemStack;Ljava/util/function/Consumer;)Lnet/minecraft/entity/projectile/ProjectileEntity;"))
    private ProjectileEntity projectileEnchantmentEffects(ProjectileEntity entity, ServerWorld world, ItemStack projectileStack, Consumer<ProjectileEntity> beforeSpawn, Operation<ProjectileEntity> original, @Local(argsOnly = true, ordinal = 0) float speed, @Local(argsOnly = true, ordinal = 0) LivingEntity shooter, @Local(argsOnly = true) ItemStack stack) {
        original.call(entity, world, projectileStack, beforeSpawn);
        Util.applyProjectileMods(shooter, stack, projectileStack, world, entity);
        return entity;
    }

    @Inject(method = "getHeldProjectile", at = @At("TAIL"), cancellable = true)
    private static void testAdaptability(LivingEntity entity, Predicate<ItemStack> predicate, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack stack;
        if (Util.hasEnchant(entity.getMainHandStack(), EpicMod.ADAPTABILITY, entity.getWorld()) && Util.adaptabilityAmmo(stack = entity.getOffHandStack()) ||
                Util.hasEnchant(entity.getOffHandStack(), EpicMod.ADAPTABILITY, entity.getWorld()) && Util.adaptabilityAmmo(stack = entity.getMainHandStack())) {
            cir.setReturnValue(stack);
        }
    }

    @Inject(method = "createArrowEntity", at = @At("HEAD"), cancellable = true)
    private void shootAltItems(World world, LivingEntity shooter, ItemStack weaponStack, ItemStack projectileStack, boolean critical, CallbackInfoReturnable<ProjectileEntity> cir) {
        Item item = projectileStack.getItem();
        ProjectileEntity p = null;
        if (item == Items.WIND_CHARGE) {
            p = new WindChargeEntity(shooter instanceof ServerPlayerEntity user ? user : null, world, shooter.getPos().getX(),
                    shooter.getEyePos().getY(), shooter.getPos().getZ());
            ((ProjectileInterface) p).setLife(100);
            p.setOwner(shooter);
        } else if (item == Items.FIRE_CHARGE) {
            p = new SmallFireballEntity(world, shooter, Vec3d.ZERO);
            ((ProjectileInterface) p).setLife(100);
        } else if (item == Items.ENDER_PEARL) {
            p = new EnderPearlEntity(world, shooter, projectileStack);
        } else if (item == Items.SPLASH_POTION) {
            p = new PotionEntity(world, shooter, projectileStack);
        } else if (item == Items.SNOWBALL) {
            p = new SnowballEntity(world, shooter, projectileStack);
        } else if (item == Items.EGG) {
            p = new EggEntity(world, shooter, projectileStack);
        } else if (item == Items.DRAGON_BREATH) {
            p = new DragonFireballEntity(world, shooter, Vec3d.ZERO);
            ((ProjectileInterface) p).setLife(80);
        }
        if (p != null) {
            p.setPos(shooter.getX(), shooter.getEyeY(), shooter.getZ());
            cir.setReturnValue(p);
        }
    }

    @ModifyExpressionValue(method = "createArrowEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ArrowItem;createArrow(Lnet/minecraft/world/World;Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;)Lnet/minecraft/entity/projectile/PersistentProjectileEntity;"))
    private PersistentProjectileEntity applyFocus(PersistentProjectileEntity original, @Local(argsOnly = true) LivingEntity shooter) {
        if (shooter instanceof ServerPlayerEntity player && ((ServerPlayerEntityInterface) player).getTickers(PlayerTicker.ID.FOCUS).get() > 0) {
            original.setDamage(original.getDamage() + 1.5);
            original.setGlowing(true);
        }
        return original;
    }

}
