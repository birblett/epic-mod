package com.birblett.helper;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;

public class PlayerTicker {

    public enum ID {
        BLINK,
        BURST_FIRE,
        CATALYST,
        DASH,
        DOUBLE_JUMP,
        FEATHERWEIGHT,
        FOCUS,
        HOMING,
        HOVERING,
        LEAPING,
        ROCKET,
        SLIPSTREAM,
        SNIPER,
        WALLCLING,

        THUNDER_TOME
    }

    protected final ServerPlayerEntity player;
    protected final AttributeManager attributeManager;

    public PlayerTicker(ServerPlayerEntity player, AttributeManager attributeManager) {
        this.player = player;
        this.attributeManager = attributeManager;
    }

    public void onGroundTick() {}

    public void tick() {}

    public void onInput(InputManager pressed, InputManager last) {}

    public void set(int value) {}

    public void setUsing(ItemStack stack, Hand hand, int value) {};

    public int get() {
        return 0;
    }

    protected ServerWorld world() {
        return this.player.getServerWorld();
    }

    protected boolean hasEnchant(EquipmentSlot slot, RegistryKey<Enchantment> key) {
        return Util.hasEnchant(this.player.getEquippedStack(slot), key, this.world());
    }

    protected int getEnchantLevel(EquipmentSlot slot, RegistryKey<Enchantment> key) {
        return Util.getEnchantLevel(this.player.getEquippedStack(slot), key, this.world());
    }

}
