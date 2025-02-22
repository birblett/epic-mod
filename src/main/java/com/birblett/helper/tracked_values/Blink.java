package com.birblett.helper.tracked_values;

import com.birblett.EpicMod;
import com.birblett.helper.AttributeManager;
import com.birblett.helper.InputManager;
import com.birblett.helper.PlayerTicker;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;

import java.util.Set;

public class Blink extends PlayerTicker {

    private int blinkCooldown = 40;

    public Blink(ServerPlayerEntity player, AttributeManager attributeManager) {
        super(player, attributeManager);
    }

    @Override
    public void tick() {
        if (this.blinkCooldown > 0) {
            --this.blinkCooldown;
        }
    }

    @Override
    public void onInput(InputManager pressed, InputManager last) {
        if ((pressed.sneak() && last.sprint() || pressed.sprint() && last.sneak()) && this.blinkCooldown <= 0 &&
                this.hasEnchant(EquipmentSlot.FEET, EpicMod.BLINK)) {
            this.blinkCooldown = 40;
            Vec3d dir = this.player.getRotationVector().multiply(0.1);
            Box box = this.player.getBoundingBox();
            Vec3d finalPos = this.player.getPos();
            Vec3d prev = this.player.getPos();
            for (int i = 0; i < 80; ++i) {
                if (!this.world().getBlockCollisions(null, box).iterator().hasNext()) {
                    finalPos = box.getCenter();
                    finalPos = new Vec3d(finalPos.x, box.minY, finalPos.z);
                }
                box = box.offset(dir);
            }
            this.player.teleportTo(new TeleportTarget(this.world(), finalPos, this.player.getVelocity(), this.player.getYaw(),
                    this.player.getPitch(), Set.of(), TeleportTarget.NO_OP));
            this.world().sendEntityStatus(this.player, EntityStatuses.ADD_PORTAL_PARTICLES);
            this.world().playSound(null, prev.x, prev.y, prev.z, SoundEvents.ENTITY_ENDERMAN_TELEPORT, this.player.getSoundCategory(),
                    1.0F, 1.0F);
        }
    }

}
