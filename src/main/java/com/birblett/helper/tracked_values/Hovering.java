package com.birblett.helper.tracked_values;

import com.birblett.EpicMod;
import com.birblett.helper.AttributeManager;
import com.birblett.helper.InputManager;
import com.birblett.helper.PlayerTicker;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

public class Hovering extends PlayerTicker {

    private static final Identifier HOVERING_ID = Identifier.of("hovering");

    private boolean hovering = false;
    private int hoveringTicks = 0;
    private InputManager last = new InputManager(false, false, false, false, false, false, false);

    public Hovering(ServerPlayerEntity player, AttributeManager attributeManager) {
        super(player, attributeManager);
    }

    @Override
    public void onGroundTick() {
        this.hoveringTicks = 60;
    }

    @Override
    public void tick() {
        if (this.hovering && this.hoveringTicks > 0) {
            this.attributeManager.addAttribute(this.player.getAttributeInstance(EntityAttributes.GRAVITY),
                    new EntityAttributeModifier(HOVERING_ID, -1, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL), 1);
            this.player.setVelocity(0, 0, 0);
            this.player.fallDistance = 0;
            if (this.last.forward() || this.last.backward() || this.last.left() || this.last.right()) {
                Vec3d base = this.player.getRotationVector().multiply(1e17, 0, 1e17).normalize().multiply(0.5);
                if (this.last.forward()) {
                    this.player.addVelocity(base);
                }
                if (this.last.backward()) {
                    this.player.addVelocity(new Vec3d(-base.x, 0, -base.z));
                }
                if (this.last.left()) {
                    this.player.addVelocity(new Vec3d(base.z, 0, -base.x));
                }
                if (this.last.right()) {
                    this.player.addVelocity(new Vec3d(-base.z, 0, base.x));
                }
                --this.hoveringTicks;
            }
            this.player.velocityDirty = true;
            this.player.velocityModified = true;
        } else {
            this.hovering = false;
        }
    }

    @Override
    public void onInput(InputManager pressed, InputManager last) {
        this.last = last;
        if (pressed.sneak() && !this.player.isOnGround() && this.hoveringTicks > 0 && this.hasEnchant(EquipmentSlot.LEGS, EpicMod.HOVERING)) {
            this.hovering = !this.hovering;
        }
    }

}
