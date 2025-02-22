package com.birblett.helper.tracked_values;

import com.birblett.EpicMod;
import com.birblett.helper.AttributeManager;
import com.birblett.helper.PlayerTicker;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;

public class Featherweight extends PlayerTicker {

    public Featherweight(ServerPlayerEntity player, AttributeManager attributeManager) {
        super(player, attributeManager);
    }

    @Override
    public void tick() {
        if (this.player.isSneaking() && this.hasEnchant(EquipmentSlot.FEET, EpicMod.FEATHERWEIGHT)) {
            this.player.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 2, 1, false, false));
        }
    }

}
