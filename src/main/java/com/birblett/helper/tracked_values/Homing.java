package com.birblett.helper.tracked_values;

import com.birblett.helper.AttributeManager;
import com.birblett.helper.PlayerTicker;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;

public class Homing extends PlayerTicker {

    private LivingEntity target;

    public Homing(ServerPlayerEntity player, AttributeManager attributeManager) {
        super(player, attributeManager);
    }

    public void setTarget(LivingEntity target) {
        this.target = target;
    }

    public LivingEntity getTarget() {
        if (this.target != null && (!this.target.isAlive() || this.target.isRegionUnloaded())) {
            this.target = null;
        }
        return this.target;
    }

}
