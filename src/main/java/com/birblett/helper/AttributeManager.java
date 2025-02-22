package com.birblett.helper;

import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;

public class AttributeManager {

    private final ServerPlayerEntity player;

    public AttributeManager(ServerPlayerEntity player) {
        this.player = player;
    }

    record Attribute(EntityAttributeInstance instance, EntityAttributeModifier mod) {

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Attribute a && a.instance.equals(this.instance) && a.mod.id().equals(this.mod.id());
        }

        @Override
        public int hashCode() {
            return this.instance.hashCode() + this.mod.id().hashCode();
        }

    }

    private final HashMap<Attribute, Integer> tickedAttributes = new HashMap<>();

    public void tick() {
        for (var entry : this.tickedAttributes.entrySet()) {
            EntityAttributeInstance instance = entry.getKey().instance();
            if (instance.hasModifier(entry.getKey().mod.id())) {
                instance.removeModifier(entry.getKey().mod.id());
            }
            if (entry.getValue() > 0) {
                instance.addPersistentModifier(entry.getKey().mod);
                this.tickedAttributes.put(entry.getKey(), entry.getValue() - 1);
            }
        }
    }

    public void addAttribute(RegistryEntry<EntityAttribute> key, EntityAttributeModifier modifier, int ticks) {
        this.addAttribute(player.getAttributeInstance(key), modifier, ticks);
    }

    public void addAttribute(EntityAttributeInstance i, EntityAttributeModifier modifier, int ticks) {
        if (i != null) {
            this.tickedAttributes.put(new Attribute(i, modifier), ticks);
        }
    }


}
