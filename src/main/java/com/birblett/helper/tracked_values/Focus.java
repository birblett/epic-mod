package com.birblett.helper.tracked_values;

import com.birblett.helper.AttributeManager;
import com.birblett.helper.PlayerTicker;
import net.minecraft.server.network.ServerPlayerEntity;

public class Focus extends PlayerTicker {

    private int focusTicks = 0;

    public Focus(ServerPlayerEntity player, AttributeManager attributeManager) {
        super(player, attributeManager);
    }

    @Override
    public void tick() {
        if (this.focusTicks > 0) {
            --this.focusTicks;
        }
    }

    @Override
    public void set(int value) {
        this.focusTicks = value;
    }

    @Override
    public int get() {
        return this.focusTicks;
    }

}
