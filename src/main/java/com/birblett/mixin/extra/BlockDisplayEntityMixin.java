package com.birblett.mixin.extra;

import com.birblett.helper.Ability;
import com.birblett.interfaces.AbilityUser;
import com.birblett.interfaces.PhysicsProjectile;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(DisplayEntity.BlockDisplayEntity.class)
public abstract class BlockDisplayEntityMixin extends DisplayEntity implements PhysicsProjectile {

    @Unique
    LivingEntity owner = null;
    @Unique
    Vec3d last = null;
    @Unique
    Vec3d vel = Vec3d.ZERO;
    @Unique
    VoxelShape v = null;
    @Shadow
    protected abstract void setBlockState(BlockState state);
    @Shadow
    protected abstract BlockState getBlockState();

    public BlockDisplayEntityMixin(EntityType<?> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public void setProjectileOwner(LivingEntity e) {
        this.owner = e;
    }

    @Override
    public void releaseProjectile() {
        this.vel = this.owner.getRotationVector().add(this.owner.getVelocity());
        this.owner = null;
    }

    @Override
    public void setBlock(BlockState b, World world, BlockPos pos) {
        this.setBlockState(b);
        this.v = b.getOutlineShape(world, pos);
        this.last = Vec3d.of(pos);
    }

    @Override
    public BlockState projectileBlockState() {
        return this.getBlockState();
    }

    public void tick() {
        super.tick();
        if (this.getWorld() instanceof ServerWorld world && ((AbilityUser) this).hasAbility(Ability.PHYSICS)) {
            if (this.owner instanceof ServerPlayerEntity p && !p.isDisconnected() && p.getWorld() == this.getWorld()) {
                this.last = this.getPos();
                Vec3d nextPos = this.owner.getEyePos().add(this.owner.getRotationVector().multiply(2)).add(-0.5, 0, -0.5);
                if (nextPos.squaredDistanceTo(this.last) > 1) {
                    Vec3d dir = nextPos.subtract(this.last);
                    nextPos = this.last.add(dir.multiply(1 / Math.sqrt(nextPos.squaredDistanceTo(this.last))));
                }
                this.setPosition(nextPos);
            } else {
                this.discard();
                Vec3d pos = this.getPos();
                FallingBlockEntity f = new FallingBlockEntity(world, pos.x + 0.5, pos.y, pos.z + 0.5, this.getBlockState());
                f.setVelocity(pos.subtract(this.last == null ? pos : this.last).add(this.vel));
                world.spawnEntity(f);
            }
        }
    }

}
