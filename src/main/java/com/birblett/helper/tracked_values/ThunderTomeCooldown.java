package com.birblett.helper.tracked_values;

import com.birblett.helper.AttributeManager;
import com.birblett.helper.PlayerTicker;
import net.minecraft.server.network.ServerPlayerEntity;

public class ThunderTomeCooldown extends PlayerTicker {

    private int cooldown = 0;
    private int current = 0;

    public ThunderTomeCooldown(ServerPlayerEntity player, AttributeManager attributeManager) {
        super(player, attributeManager);
    }

    @Override
    public void tick() {
        if (this.cooldown > 0) {
            --this.cooldown;
        } else {
            this.current = 0;
        }
    }

    @Override
    public int get() {
        return this.current;
    }

    @Override
    public void set(int value) {
        this.cooldown = value;
        ++this.current;
    }
}
