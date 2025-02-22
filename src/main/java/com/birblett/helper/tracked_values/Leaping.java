package com.birblett.helper.tracked_values;

import com.birblett.helper.AttributeManager;
import com.birblett.helper.PlayerTicker;
import net.minecraft.server.network.ServerPlayerEntity;

public class Leaping extends PlayerTicker {

    private int leaping = 0;

    public Leaping(ServerPlayerEntity player, AttributeManager attributeManager) {
        super(player, attributeManager);
    }

    @Override
    public void onGroundTick() {
        this.leaping = 0;
    }

    @Override
    public int get() {
        return leaping;
    }

    @Override
    public void set(int value) {
        this.leaping = value;
    }
}
