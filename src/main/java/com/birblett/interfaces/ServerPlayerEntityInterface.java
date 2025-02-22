package com.birblett.interfaces;

import com.birblett.helper.PlayerTicker;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;

public interface ServerPlayerEntityInterface {

    PlayerTicker getTickers(PlayerTicker.ID id);
    boolean setReloading(ItemStack i, Item ammo, int reload, int reloadAmount, int capacity);
    boolean isReloading(ItemStack i);
    void addTickedAttribute(RegistryEntry<EntityAttribute> key, EntityAttributeModifier modifier, int ticks);

}
