package com.birblett.mixin.extra;

import com.birblett.interfaces.PhysicsProjectile;
import com.birblett.interfaces.PhysicsUser;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin implements PhysicsUser {

    @Unique
    private PhysicsProjectile projectile = null;

    @Override
    public void setProjectile(PhysicsProjectile p) {
        this.projectile = p;
    }

    @Override
    public boolean hasProjectile() {
        return this.projectile != null;
    }

    @Override
    public PhysicsProjectile getProjectile() {
        return this.projectile;
    }

}
