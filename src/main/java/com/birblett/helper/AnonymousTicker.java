package com.birblett.helper;

import com.birblett.interfaces.TickedEntity;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;

public class AnonymousTicker {

    protected final LivingEntity entity;

    public AnonymousTicker(LivingEntity entity) {
        ((TickedEntity) (this.entity = entity)).addTicker(this);
    }

    public void onGroundTick() {}

    public void tick() {}

    public void set(int value) {}

    public int get() {
        return 0;
    }

    public boolean shouldRemove() {
        return true;
    }

    protected World world() {
        return this.entity.getWorld();
    }

    protected boolean hasEnchant(EquipmentSlot slot, RegistryKey<Enchantment> key) {
        return Util.hasEnchant(this.entity.getEquippedStack(slot), key, this.world());
    }

    protected int getEnchantLevel(EquipmentSlot slot, RegistryKey<Enchantment> key) {
        return Util.getEnchantLevel(this.entity.getEquippedStack(slot), key, this.world());
    }

}
