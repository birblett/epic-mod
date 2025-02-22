package com.birblett.mixin.base;

import com.birblett.EpicMod;
import com.birblett.helper.Ability;
import com.birblett.helper.Util;
import com.birblett.interfaces.AbilityUser;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TridentEntity.class)
public abstract class TridentEntityMixin extends PersistentProjectileEntity implements AbilityUser {

    @Shadow
    private boolean dealtDamage;

    protected TridentEntityMixin(EntityType<? extends PersistentProjectileEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "onEntityHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/projectile/TridentEntity;getOwner()Lnet/minecraft/entity/Entity;"), cancellable = true)
    private void onHitBlast(EntityHitResult entityHitResult, CallbackInfo ci) {
        if (this.applyExplosion(this.getVelocity().normalize().multiply(0.5))) {
            ci.cancel();
        }
    }

    @Inject(method = "onBlockHitEnchantmentEffects", at = @At("HEAD"))
    private void onBlockHitBlast(ServerWorld world, BlockHitResult blockHitResult, ItemStack weaponStack, CallbackInfo ci) {
        this.applyExplosion(blockHitResult.getSide().getDoubleVector().multiply(0.2));
    }

    @Unique
    private boolean applyExplosion(Vec3d offset) {
        if (!this.dealtDamage && this.getWorld() instanceof ServerWorld world && Util.hasEnchant(this.getItemStack(), EpicMod.BLASTING, world)) {
            Explosion.DestructionType d = Explosion.DestructionType.KEEP;
            float power = 2.0f;
            if (this.hasAbility(Ability.BOSS_FLAG)) {
                d = Explosion.DestructionType.DESTROY;
                power = 3.0f;
                Util.mobBreakBlocks(this, 1, 1);
            }
            this.setVelocity(this.getVelocity().multiply(-0.1));
            Util.explodeAt(world, this.getOwner(), world.getDamageSources().explosion(this.getOwner(), this.getOwner()), this.getPos()
                    .add(offset), power, false, d);
            if (this.hasAbility(Ability.BOSS_FLAG)) {
                this.discard();
            }
            return this.dealtDamage = true;
        }
        return false;
    }

}
