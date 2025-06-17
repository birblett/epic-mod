package com.birblett.helper.tracked_values;

import com.birblett.EpicMod;
import com.birblett.helper.AttributeManager;
import com.birblett.helper.PlayerTicker;
import com.birblett.helper.Util;
import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.HashSet;
import java.util.List;

public class Assault extends PlayerTicker {

    private static final EntityAttributeModifier ASSAULT_GRAVITY = new EntityAttributeModifier(Identifier.of("assault_gravity"), 0,
            EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    private Vec3d dashDirection;
    private Vec3d hitOffset;
    private Vec3d hitVelocity;
    private int canUseTicks = 0;

    public Assault(ServerPlayerEntity player, AttributeManager attributeManager) {
        super(player, attributeManager);
    }

    @Override
    public void tick() {
        ItemStack stack;
        if ((stack = this.player.getActiveItem()).isOf(Items.SHIELD) && Util.hasEnchant(stack, EpicMod.ASSAULT, this.world()) &&
                this.canUseTicks > 0) {
            this.attributeManager.addAttribute(EntityAttributes.GRAVITY, new EntityAttributeModifier(Identifier.of("assault_gravity"), -1,
                    EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL), 1);
            if (this.dashDirection == null) {
                this.dashDirection = this.player.getRotationVector().multiply(1, 0, 1).normalize().multiply(0.9);
                this.hitOffset = this.dashDirection.multiply(0.2222);
                this.hitVelocity = this.dashDirection.multiply(2).add(0, 0.2, 0);
            }
            this.player.setVelocity(this.dashDirection);
            Box box = this.player.getBoundingBox().expand(0.15).offset(this.dashDirection.multiply(0.3));
            HashSet<Entity> set = new HashSet<>();
            for (int i = 0; i < 5; ++i) {
                List<Entity> e = this.world().getOtherEntities(this.player, box);
                for (Entity entity : e) {
                    if (!set.contains(entity) && !(entity instanceof ItemFrameEntity) && entity.damage(this.world(),
                            this.world().getDamageSources().playerAttack(this.player), 3)) {
                        entity.addVelocity(this.hitVelocity);
                        entity.velocityModified = true;
                        stack.damage(1, this.player);
                    }
                    set.add(entity);
                }
                box = box.offset(this.hitOffset);
            }
            this.player.velocityModified = true;
            --this.canUseTicks;
        } else if (this.canUseTicks != 7 && !this.player.isUsingItem()) {
            this.player.getItemCooldownManager().set(Items.SHIELD.getDefaultStack(), this.player.isGliding() ? 60 : 15);
            this.dashDirection = null;
            this.canUseTicks = 7;
        }
    }

    @Override
    public int get() {
        return this.canUseTicks;
    }
}
