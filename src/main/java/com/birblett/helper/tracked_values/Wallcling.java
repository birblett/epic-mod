package com.birblett.helper.tracked_values;

import com.birblett.EpicMod;
import com.birblett.helper.AttributeManager;
import com.birblett.helper.PlayerTicker;
import com.birblett.helper.Util;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class Wallcling extends PlayerTicker {

    private static final Identifier WALLCLING_ID = Identifier.of("wallcling");
    private int wallclingJumps = 0;

    public Wallcling(ServerPlayerEntity player, AttributeManager attributeManager) {
        super(player, attributeManager);
    }

    @Override
    public void onGroundTick() {
        this.wallclingJumps = 4 * this.getEnchantLevel(EquipmentSlot.FEET, EpicMod.WALLCLING);
    }

    @Override
    public void tick() {
        EntityAttributeInstance i = this.player.getAttributeInstance(EntityAttributes.GRAVITY);
        if (this.wallclingJumps > 0 && i != null) {
            if (!this.player.isOnGround() && this.player.isSneaking() && Util.isTouchingBlock(this.player, 0.02, 0, 0.02)) {
                this.attributeManager.addAttribute(i, new EntityAttributeModifier(WALLCLING_ID, -1,
                        EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL), 1);
                this.player.setVelocity(0, 0, 0);
                this.player.fallDistance = 0;
                this.player.velocityModified = true;
            } else if ((!this.player.isSneaking()) && i.hasModifier(WALLCLING_ID)) {
                this.player.setVelocity(this.player.getRotationVector().multiply(-0.8));
                this.player.velocityModified = true;
                this.wallclingJumps--;
            } else if (i.hasModifier(WALLCLING_ID)) {
                i.removeModifier(WALLCLING_ID);
                this.wallclingJumps--;
            }
        }
    }

}
