package com.birblett.helper.tracked_values;

import com.birblett.EpicMod;
import com.birblett.helper.AttributeManager;
import com.birblett.helper.PlayerTicker;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class Catalyst extends PlayerTicker {

    private static final Identifier CATALYST_ID = Identifier.of("catalyst");

    public Catalyst(ServerPlayerEntity player, AttributeManager attributeManager) {
        super(player, attributeManager);
    }

    @Override
    public void tick() {
        EntityAttributeInstance[] instances = {
                this.player.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED),
                this.player.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE),
                this.player.getAttributeInstance(EntityAttributes.ATTACK_SPEED),
                this.player.getAttributeInstance(EntityAttributes.ATTACK_KNOCKBACK)
        };
        if (this.hasEnchant(EquipmentSlot.CHEST, EpicMod.CATALYST)) {
            int scale = this.player.getStatusEffects().size();
            for (EntityAttributeInstance in : instances) {
                this.attributeManager.addAttribute(in, new EntityAttributeModifier(CATALYST_ID, 0.05 * scale,
                        EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL), 1);
            }
        }
    }

}
