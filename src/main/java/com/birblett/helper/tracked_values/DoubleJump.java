package com.birblett.helper.tracked_values;

import com.birblett.EpicMod;
import com.birblett.helper.AttributeManager;
import com.birblett.helper.InputManager;
import com.birblett.helper.PlayerTicker;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.server.network.ServerPlayerEntity;

public class DoubleJump extends PlayerTicker {

    private int doubleJumps = 0;

    public DoubleJump(ServerPlayerEntity player, AttributeManager attributeManager) {
        super(player, attributeManager);
    }

    @Override
    public void onGroundTick() {
        this.doubleJumps = this.getEnchantLevel(EquipmentSlot.LEGS, EpicMod.DOUBLE_JUMP) << 1;
    }

    @Override
    public void onInput(InputManager pressed, InputManager last) {
        if (pressed.jump() && this.doubleJumps > 0 && !this.player.isOnGround()) {
            this.player.jump();
            --this.doubleJumps;
            this.player.velocityDirty = true;
            this.player.velocityModified = true;
        }
    }

}
