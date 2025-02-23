package com.birblett.helper.tracked_values;

import com.birblett.EpicMod;
import com.birblett.helper.AttributeManager;
import com.birblett.helper.PlayerTicker;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class Sniper extends PlayerTicker {

    private final EntityAttributeModifier penalty = new EntityAttributeModifier(Identifier.of("sniper_penalty"), -10,
            EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

    public Sniper(ServerPlayerEntity player, AttributeManager attributeManager) {
        super(player, attributeManager);
    }

    @Override
    public void tick() {
        if (this.player.isSneaking() && this.player.isOnGround() && this.hasEnchant(EquipmentSlot.HEAD, EpicMod.SNIPER)) {
            this.attributeManager.addAttribute(EntityAttributes.MOVEMENT_SPEED, penalty, 1);
            this.attributeManager.addAttribute(EntityAttributes.JUMP_STRENGTH, penalty, 1);
        }
    }

}
