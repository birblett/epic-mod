package com.birblett;

import net.fabricmc.api.ModInitializer;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EpicMod implements ModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("epic-mod");
    public static final RegistryKey<Enchantment> ADAPTABILITY = RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.ofVanilla("adaptability"));
    public static final RegistryKey<Enchantment> ARROW_RAIN = RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.ofVanilla("arrow_rain"));
    public static final RegistryKey<Enchantment> BLASTING = RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.ofVanilla("blasting"));
    public static final RegistryKey<Enchantment> BLINK = RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.ofVanilla("blink"));
    public static final RegistryKey<Enchantment> BURST_FIRE = RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.ofVanilla("burst_fire"));
    public static final RegistryKey<Enchantment> CATALYST = RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.ofVanilla("catalyst"));
    public static final RegistryKey<Enchantment> DASH = RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.ofVanilla("air_dash"));
    public static final RegistryKey<Enchantment> DOUBLE_JUMP = RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.ofVanilla("double_jump"));
    public static final RegistryKey<Enchantment> FEATHERWEIGHT = RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.ofVanilla("featherweight"));
    public static final RegistryKey<Enchantment> FOCUS = RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.ofVanilla("focus"));
    public static final RegistryKey<Enchantment> GRAPPLING = RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.ofVanilla("grappling"));
    public static final RegistryKey<Enchantment> HEAVY_SHOT = RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.ofVanilla("heavy_shot"));
    public static final RegistryKey<Enchantment> AUTOAIM = RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.ofVanilla("autoaim"));
    public static final RegistryKey<Enchantment> HITSCAN = RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.ofVanilla("hitscan"));
    public static final RegistryKey<Enchantment> HOVERING = RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.ofVanilla("hovering"));
    public static final RegistryKey<Enchantment> LEAPING = RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.ofVanilla("leaping"));
    public static final RegistryKey<Enchantment> MAGIC_GUARD = RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.ofVanilla("magic_guard"));
    public static final RegistryKey<Enchantment> MIGHTY_WIND = RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.ofVanilla("mighty_wind"));
    public static final RegistryKey<Enchantment> RIDER = RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.ofVanilla("rider"));
    public static final RegistryKey<Enchantment> ROCKET = RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.ofVanilla("rocket"));
    public static final RegistryKey<Enchantment> SLIPSTREAM = RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.ofVanilla("slipstream"));
    public static final RegistryKey<Enchantment> SOULBOUND = RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.ofVanilla("soulbound"));
    public static final RegistryKey<Enchantment> THUNDERBOLT = RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.ofVanilla("thunderbolt"));
    public static final RegistryKey<Enchantment> WALLCLING = RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.ofVanilla("wallcling"));

    public static final TagKey<DamageType> INDIRECT_DAMAGE = TagKey.of(RegistryKeys.DAMAGE_TYPE, Identifier.ofVanilla("indirect"));

    @Override
    public void onInitialize() {}

}