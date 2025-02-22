package com.birblett.helper.tracked_values;

import com.birblett.EpicMod;
import com.birblett.helper.AttributeManager;
import com.birblett.helper.InputManager;
import com.birblett.helper.PlayerTicker;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.server.network.ServerPlayerEntity;

public class Dash extends PlayerTicker {

    private int dashed = 9;
    private int dashCooldown = 20;
    private int penalty = 100;

    public Dash(ServerPlayerEntity player, AttributeManager attributeManager) {
        super(player, attributeManager);
    }

    @Override
    public void onGroundTick() {
        this.penalty = 0;
    }

    @Override
    public void tick() {
        if (this.dashCooldown > 0) {
            --this.dashCooldown;
        } else {
            if (this.dashed == 9) {
                this.dashed = 0;
            }
        }
        if (this.dashed != 9) {
            this.dashed = (this.dashed << 1) & 0b1111111111;
        }
    }

    @Override
    public void onInput(InputManager pressed, InputManager last) {
        if (pressed.forward() && this.hasEnchant(EquipmentSlot.LEGS, EpicMod.DASH) && this.dashed != 9) {
            if (this.dashed == 0) {
                this.dashed = 1;
            } else {
                this.dashed = 9;
                this.player.addVelocity(this.player.getRotationVector().multiply(1.25));
                this.player.velocityDirty = true;
                this.player.velocityModified = true;
                this.dashCooldown = 20 + this.penalty;
                this.penalty += 5;
            }
        }
    }

}
