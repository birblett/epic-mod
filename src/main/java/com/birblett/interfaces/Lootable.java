package com.birblett.interfaces;

import net.minecraft.loot.LootTable;
import net.minecraft.registry.RegistryKey;

public interface Lootable {

    void setLootTable(RegistryKey<LootTable> id);

}
