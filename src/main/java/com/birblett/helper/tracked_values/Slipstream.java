package com.birblett.helper.tracked_values;

import com.birblett.helper.AttributeManager;
import com.birblett.helper.PlayerTicker;
import net.minecraft.server.network.ServerPlayerEntity;

public class Slipstream extends PlayerTicker {

    private int penalty;

    public Slipstream(ServerPlayerEntity player, AttributeManager attributeManager) {
        super(player, attributeManager);
    }

    @Override
    public void onGroundTick() {
        this.penalty = 0;
    }

    @Override
    public void set(int value) {
        this.penalty = value;
    }

    @Override
    public int get() {
        return this.penalty;
    }

}
