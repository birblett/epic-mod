package com.birblett.helper;

import com.birblett.interfaces.AbilityUser;
import com.birblett.interfaces.PhysicsProjectile;
import com.birblett.interfaces.PhysicsUser;
import com.birblett.interfaces.ServerPlayerEntityInterface;
import com.mojang.datafixers.util.Function4;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.dragon.EnderDragonPart;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.List;

public class CustomItems {

    public static final String USE = "EpicModUse";

    private static final int ARCHMAGE_ID = 0;
    private static final int BOT_ID = 1;
    private static final int DASHMASTER_ID = 2;
    private static final int DULLAHAN_ID = 3;
    private static final int LUDFRU_ID = 4;
    private static final int MINER_ID = 5;
    private static final int GUNNER_ID = 6;
    private static final int SKELATOM_ID = 7;

    private static final EntityAttributeModifier THUNDER_TOME_SPEED_MODIFIER = new EntityAttributeModifier(Identifier
            .of("thunder_tome"), -0.6, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

    private static final EntityAttributeModifier THUNDER_TOME_JUMP_MODIFIER = new EntityAttributeModifier(Identifier
            .of("thunder_tome"), -10, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

    public record EventResult(int cooldown, ActionResult result) {}
    public static final HashMap<Integer, Function4<PlayerEntity, ItemStack, Hand, ServerWorld, EventResult>> USE_EVENTS = new HashMap<>();
    public static final HashMap<Integer, Function4<PlayerEntity, ItemStack, Hand, ServerWorld, EventResult>> USE_TICK_EVENTS = new HashMap<>();
    public static final HashMap<Integer, Function4<PlayerEntity, ItemStack, BlockPos, ServerWorld, EventResult>> USE_ON_BLOCK_EVENTS = new HashMap<>();
    public static final HashMap<Integer, Function4<PlayerEntity, ItemStack, Boolean, ServerWorld, EventResult>> TICK_EVENTS = new HashMap<>();

    private static <T> String withEvent(String s, int id, HashMap<Integer, T> map, T event) {
        map.put(id, event);
        return String.format(s.substring(0, s.length() - 1) + ",custom_data={EpicModUse:%d}]", id);
    }

    private static <T> String withEventNoMod(String s, int id, HashMap<Integer, T> map, T event) {
        map.put(id, event);
        return s;
    }

    private static String withCustomModel(String s, String id) {
        return String.format(s.substring(0, s.length() - 1) + ",custom_model_data={strings:[\"%s\"]}]", id);
    }

    public static final String ARCHMAGE_TOME = withCustomModel(withEvent("book[item_model=\"minecraft:enchanted_book\",enchantment_glint_override=true,custom_name='{\"color\":\"dark_red\",\"italic\":false,\"text\":\"Thunder Tome\"}',max_stack_size=1]", ARCHMAGE_ID, USE_EVENTS, (player, stack, pos, world) -> {
        var tracker = ((ServerPlayerEntityInterface) player).getTickers(PlayerTicker.ID.THUNDER_TOME);
        Vec3d dir = player.getRotationVector().multiply(2);
        Vec3d pos2 = player.getPos().add(0, player.getHeight() / 1.5, 0).add(dir.multiply(0.2));
        Util.playSound(world, player, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 0.4f, 2.0f);
        boolean crit = tracker.get() >= 5;
        int selfDamage = Math.max(0, tracker.get() - 10);
        if (selfDamage > 0) {
            player.hurtTime = 0;
            player.timeUntilRegen = 1;
            player.damage(world, world.getDamageSources().indirectMagic(player, null), selfDamage);
        }
        for (int i = 0; i < 5; ++i) {
            if (crit) {
                Util.damagingRaycast(world, player, pos2, dir.multiply(0.05), 20, 0.05, -1,
                        ParticleTypes.CRIT, false, false, null, false, 0.08);
            }
            var hit = Util.damagingRaycast(world, player, pos2, dir.multiply(0.05), 20, 0.05, crit ? 9 : 4,
                    ParticleTypes.ELECTRIC_SPARK, false, true, world.getDamageSources().playerAttack(player), true, 0.01);
            if (hit != null) {
                if (crit && hit instanceof EntityHitResult e && e.getEntity() instanceof LivingEntity entity) {
                    Util.playSound(world, entity, SoundEvents.ENTITY_ARROW_HIT_PLAYER, 2, 1.2f);
                }
                break;
            } else {
                pos2 = pos2.add(dir);
                dir = Util.applyDivergence(dir, 0.2).multiply(2);
                LivingEntity target = world.getClosestEntity(LivingEntity.class, TargetPredicate.DEFAULT, player, pos2.x, pos2.y, pos2.z,
                        Box.of(pos2, 16, 16, 16));
                if (target != null) {
                    Vec3d targetDir = target.getPos().add(0, target.getHeight() / 2, 0).subtract(pos2);
                    dir = Util.rotateTowards(dir, targetDir, 0.3).normalize().multiply(2);
                }
            }
        }
        tracker.set(30);
        player.swingHand(player.getMainHandStack() == stack ? Hand.MAIN_HAND : Hand.OFF_HAND, true);
        ((ServerPlayerEntityInterface) player).addTickedAttribute(EntityAttributes.MOVEMENT_SPEED, THUNDER_TOME_SPEED_MODIFIER, 10);
        ((ServerPlayerEntityInterface) player).addTickedAttribute(EntityAttributes.JUMP_STRENGTH, THUNDER_TOME_JUMP_MODIFIER, 10);
        return new EventResult(3, ActionResult.SUCCESS);
    }), "thunder_tome");

    public static final String BOT_SWORD = withCustomModel(withEvent(withEventNoMod("netherite_sword[custom_name='{\"color\":\"dark_red\",\"italic\":false,\"text\":\"Physics Blade\"}',max_damage=6000,enchantments={levels:{\"minecraft:knockback\":2,\"minecraft:sharpness\":4},show_in_tooltip:true}]", BOT_ID, USE_ON_BLOCK_EVENTS, (player, stack, pos, world) -> {
        if (!((PhysicsUser) player).hasProjectile()) {
            BlockState b = world.getBlockState(pos);
            if (b.getBlock().getHardness() > 0 && !b.hasBlockEntity()) {
                DisplayEntity.BlockDisplayEntity e = new DisplayEntity.BlockDisplayEntity(EntityType.BLOCK_DISPLAY, world);
                PhysicsProjectile p = (PhysicsProjectile) e;
                ((AbilityUser) e).addAbilities(Ability.PHYSICS);
                p.setBlock(b, world, pos);
                p.setProjectileOwner(player);
                e.setPos(pos.getX(), pos.getY(), pos.getZ());
                world.spawnEntity(e);
                world.setBlockState(pos, Blocks.AIR.getDefaultState());
                ((PhysicsUser) player).setProjectile(p);
                return new EventResult(5, ActionResult.SUCCESS_SERVER);
            }
        }
        return new EventResult(-1, null);
    }), BOT_ID, USE_EVENTS, (player, stack, held, world) -> {
        if (player instanceof PhysicsUser user && user.hasProjectile()) {
            BlockState b = user.getProjectile().projectileBlockState();
            user.getProjectile().releaseProjectile();
            user.setProjectile(null);
            return new EventResult((int) Math.min(200, Math.max(5, b.getBlock().getBlastResistance() * 5)), ActionResult.SUCCESS_SERVER);
        }
        return new EventResult(-1, null);
    }), "physics_blade");

    public static final String DASHMASTER_SHARD = withCustomModel(withEvent("prismarine_shard[attribute_modifiers=[{id:\"base_attack_damage\",type:\"attack_damage\",amount:2,operation:\"add_value\",slot:\"mainhand\"}],enchantment_glint_override=true,custom_name='{\"color\":\"aqua\",\"italic\":false,\"text\":\"Dashmaster\"}',max_stack_size=1]", DASHMASTER_ID, USE_EVENTS, (player, stack, hand, world) -> {
        player.addVelocity(player.getRotationVector());
        player.velocityModified = true;
        return new EventResult(50, ActionResult.SUCCESS_SERVER);
    }), "dashmaster");

    public static final String DRUNKARD_BEER = withCustomModel("potion[custom_name='{\"italic\":false,\"text\":\"Beer\"}',potion_contents={custom_color:3876109,custom_effects:[{id:\"minecraft:nausea\",amplifier:0,duration:3600,show_particles:0b}]},max_stack_size=64]", "beer");
    public static final String DRUNKARD_MOONSHINE = withCustomModel("potion[custom_name='{\"italic\":false,\"text\":\"Moonshine\"}',potion_contents={custom_color:11662047,custom_effects:[{id:\"minecraft:nausea\",amplifier:0,duration:7200,show_particles:0b},{id:\"minecraft:blindness\",amplifier:0,duration:7200},{id:\"minecraft:poison\",amplifier:0,duration:7200}]},max_stack_size=64]", "moonshine");
    public static final String DRUNKARD_QMARKS = withCustomModel("potion[custom_name='{\"italic\":false,\"text\":\"???\"}',potion_contents={custom_color:16763904},max_stack_size=64]", "qmarks");
    public static final String DRUNKARD_RED_WINE = withCustomModel("potion[custom_name='{\"italic\":false,\"text\":\"Red Wine\"}',potion_contents={custom_color:6690853,custom_effects:[{id:\"minecraft:nausea\",amplifier:0,duration:1200},{id:\"minecraft:unluck\",amplifier:0,duration:1200}]},max_stack_size=64]", "red_wine");
    public static final String DRUNKARD_RUM = withCustomModel("potion[custom_name='{\"italic\":false,\"text\":\"???\"}',potion_contents={custom_color:8733212,custom_effects:[{id:\"minecraft:nausea\",amplifier:0,duration:4800}]},max_stack_size=64", "rum");

    public static final String DULLAHAN_KNIFE = withCustomModel(withEvent("iron_sword[enchantments={levels:{\"minecraft:looting\":4,\"minecraft:sharpness\":6}},custom_name='{\"color\":\"dark_red\",\"italic\":false,\"text\":\"Occultic Dagger\"}']", DULLAHAN_ID, USE_EVENTS, (user, stack, hand, world) -> {
        if (!user.hasStatusEffect(StatusEffects.SPEED) || !user.hasStatusEffect(StatusEffects.STRENGTH) ||
                !user.hasStatusEffect(StatusEffects.RESISTANCE)) {
            user.damage(world, world.getDamageSources().magic(), 6);
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 120, 0), user);
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 120, 0), user);
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 120, 0), user);
            return new EventResult(80, ActionResult.SUCCESS_SERVER);
        }
        return new EventResult(-1, null);
    }), "occultic_dagger");
    public static final String DULLAHAN_HEAD = withCustomModel("chainmail_helmet[death_protection={death_effects:[{type:\"minecraft:clear_all_effects\"},{type:\"minecraft:apply_effects\",effects:[{id:\"minecraft:absorption\",amplifier:1,duration:300},{id:\"minecraft:fire_resistance\",amplifier:1,duration:300},{id:\"minecraft:regeneration\",amplifier:3,duration:100},{id:\"minecraft:resistance\",amplifier:2,duration:100}]},{type:\"minecraft:play_sound\",sound:\"item.totem.use\"}]},equippable={slot:\"offhand\",dispensable:true,swappable:true},custom_name='{\"color\":\"yellow\",\"italic\":false,\"text\":\"Dullahan Head\"}',item_model=\"minecraft:zombie_head\",attribute_modifiers=[{id:\"armor\",type:\"armor\",amount:2,operation:\"add_value\",slot:\"offhand\"},{id:\"armor_toughness\",type:\"armor_toughness\",amount:2,operation:\"add_value\",slot:\"offhand\"}]]", "dullahan_head");

    public static final String ENFORCER_BATON = withCustomModel("stick[enchantments={levels:{\"minecraft:knockback\":3,\"minecraft:looting\":3,\"minecraft:sharpness\":4,\"minecraft:mending\":1}},custom_name='{\"color\":\"dark_red\",\"italic\":false,\"text\":\"Baton\"}',max_stack_size=1]", "baton");
    public static final String ENFORCER_RIOT_SHIELD = withCustomModel("shield[enchantments={levels:{\"minecraft:mending\":1,\"minecraft:stalwart\":1,\"minecraft:unbreaking\":3}},custom_name='{\"color\":\"dark_red\",\"italic\":false,\"text\":\"Riot Shield\"}']", "riot_shield");

    public static final String FARMER_HOE = withCustomModel("diamond_hoe[custom_name='{\"color\":\"aqua\",\"italic\":false,\"text\":\"Bountiful Harvest\"}',enchantments={levels:{\"minecraft:fortune\":5}}]", "bountiful_harvest");
    public static final String FARMER_BOOTS = withCustomModel("leather_boots[custom_name='{\"color\":\"aqua\",\"italic\":false,\"text\":\"I RIDE MY HORSE\"}',enchantments={levels:{\"minecraft:rider\":1,\"minecraft:depth_strider\":3}}]", "ride_my_horse");

    public static final String GUNNER_HELMET = "turtle_helmet[unbreakable={}]";
    public static final String GUNNER_TRIDENT = withCustomModel("trident[custom_name='{\"color\":\"dark_red\",\"italic\":false,\"text\":\"Blaster Bolt\"}',max_damage=1000,enchantments={levels:{\"minecraft:blasting\":1}}]", "blaster_bolt");
    private static final EntityAttributeModifier GUNNER_MODIFIER = new EntityAttributeModifier(Identifier.of("guardian_plate"), 1,
            EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    public static final String GUNNER_PLATE = withCustomModel(withEvent("diamond_chestplate[custom_name='{\"color\":\"aqua\",\"italic\":false,\"text\":\"Guardian Plate\"}',max_damage=1500]", GUNNER_ID, TICK_EVENTS, (player, stack, held, world) -> {
        if (player.getEquippedStack(EquipmentSlot.CHEST) == stack && player.getHealth() * 2.5f < player.getMaxHealth()) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 200, 4));
            ((ServerPlayerEntityInterface) player).addTickedAttribute(EntityAttributes.KNOCKBACK_RESISTANCE, GUNNER_MODIFIER, 200);
            ((ServerPlayerEntityInterface) player).addTickedAttribute(EntityAttributes.ATTACK_KNOCKBACK, GUNNER_MODIFIER, 200);
            return new EventResult(6000, null);
        }
        return new EventResult(0, null);
    }), "guardian_plate");

    public static final String KILLER_PILLAR_ASS = withCustomModel("stone_sword[custom_name='{\"color\":\"dark_red\",\"italic\":false,\"text\":\"Ancient Stone Sword\"}',attribute_modifiers=[{id:\"base_attack_damage\",type:\"attack_damage\",amount:16,operation:\"add_value\",slot:\"mainhand\"},{id:\"base_attack_speed\",type:\"attack_speed\",amount:-3.4,operation:\"add_value\",slot:\"mainhand\"},{id:\"entity_interaction_range\",type:\"entity_interaction_range\",amount:1,operation:\"add_value\",slot:\"mainhand\"},{id:\"sweeping_damage_ratio\",type:\"sweeping_damage_ratio\",amount:0.25,operation:\"add_value\",slot:\"mainhand\"}],max_damage=1800]", "ancient_stone_sword");
    public static final String KILLER_PILLAR_COAL_HELMET = withCustomModel("iron_helmet[equippable={slot:\"head\",equip_sound:\"item.armor.equip_generic\",camera_overlay:\"block/tinted_glass\",dispensable:true,swappable:true,damage_on_hurt:false},item_model=\"minecraft:coal_block\",custom_name='{\"italic\":false,\"text\":\"Coal Helmet\"}',rarity=\"epic\",attribute_modifiers=[{id:\"helmet_armor\",type:\"armor\",amount:3,operation:\"add_value\",slot:\"head\"},{id:\"helmet_armor_toughness\",type:\"armor_toughness\",amount:3,operation:\"add_value\",slot:\"head\"},{id:\"helmet_attack_damage\",type:\"attack_damage\",amount:2,operation:\"add_value\",slot:\"head\"},{id:\"helmet_attack_speed\",type:\"attack_speed\",amount:0.2,operation:\"add_multiplied_total\",slot:\"head\"}],enchantments={levels:{\"minecraft:blindfold\":1}},max_damage=600,repair_cost=100]", "coal_helmet");
    public static final String KILLER_PILLAR_CHEST = "leather_chestplate[dyed_color=1908001,unbreakable={}]";
    public static final String KILLER_PILLAR_LEGS = "leather_leggings[dyed_color=1908001,unbreakable={}]";
    public static final String KILLER_PILLAR_FEET = "leather_boots[dyed_color=1908001,unbreakable={}]";

    public static final String LUDFRU_ASTRONAUT_HELMET = withCustomModel("iron_helmet[equippable={slot:\"head\",equip_sound:\"item.armor.equip_generic\",camera_overlay:\"block/glass\",dispensable:true,swappable:true,damage_on_hurt:false},item_model=\"minecraft:glass\",custom_name='{\"italic\":false,\"text\":\"Astronaut Helmet\"}',rarity=\"epic\",attribute_modifiers=[{id:\"helmet_armor\",type:\"armor\",amount:2,operation:\"add_value\",slot:\"head\"},{id:\"helmet_burn_time\",type:\"burning_time\",amount:-0.2,operation:\"add_multiplied_total\",slot:\"head\"}],enchantments={levels:{\"minecraft:protection\":4,\"minecraft:aqua_affinity\":1,\"minecraft:respiration\":3,\"minecraft:mending\":1}},max_damage=800,repair_cost=100]", "astronaut_helmet");
    public static final String LUDFRU_SHIT = withCustomModel(withEvent("cocoa_beans[max_stack_size=1,item_name='{\"color\":\"yellow\",\"italic\":false,\"text\":\"Ludfru\\'s Shit\"}',enchantment_glint_override=true]", LUDFRU_ID, TICK_EVENTS, (player, stack, held, world) -> {
        List<Entity> entities = world.getOtherEntities(player, new Box(player.getX() - 4, player.getY() - 4,
                        player.getZ() - 4, player.getX() + 4, player.getY() + 4, player.getZ() + 4),
                e -> e.squaredDistanceTo(player) <= 9);
        for (Entity e : entities) if (e instanceof LivingEntity || e instanceof EnderDragonPart) {
            if (e instanceof EnderDragonPart) {
                e = ((EnderDragonPart) e).owner;
            }
            Vec3d vel = e.getPos().subtract(player.getPos()).normalize().multiply(0.5);
            e.addVelocity(vel.x, vel.y, vel.z);
            if (e instanceof PlayerEntity) {
                e.velocityModified = true;
            }
        }
        held = held || player.getOffHandStack() == stack;
        return new EventResult(held ? 30 : 100, null);
    }), "ludfru_shit");

    public static final String MINER_HELMET = withCustomModel("chainmail_helmet[max_damage=600,custom_name='{\"italic\":false,\"text\":\"Miner\\'s Helmet\"}',rarity=\"epic\",damage=750,attribute_modifiers=[{id:\"helmet_armor\",type:\"armor\",amount:1,operation:\"add_value\",slot:\"head\"},{id:\"helmet_armor_toughness\",type:\"armor_toughness\",amount:1,operation:\"add_value\",slot:\"head\"},{id:\"helmet_break_speed\",type:\"block_break_speed\",amount:0.05,operation:\"add_multiplied_total\",slot:\"head\"},{id:\"helmet_submerged_speed\",type:\"submerged_mining_speed\",amount:0.3,operation:\"add_value\",slot:\"head\"},{id:\"helmet_oxy_bonus\",type:\"oxygen_bonus\",amount:1,operation:\"add_value\",slot:\"head\"}]]", "miner_helmet");
    public static final String MINER_CHESTPLATE = withCustomModel("chainmail_chestplate[max_damage=750,enchantments={levels:{\"minecraft:machine_assist\":1}},attribute_modifiers={modifiers:[{id:\"chest_armor\",type:\"armor\",amount:5,operation:\"add_value\",slot:\"chest\"},{id:\"chest_armor_toughness\",type:\"armor_toughness\",amount:2,operation:\"add_value\",slot:\"chest\"}],show_in_tooltip:true},item_name='{\"color\":\"aqua\",\"italic\":false,\"text\":\"Miner\\'s Mail\"}']", "miner_mail");
    public static final String MINER_LEGGINGS = withCustomModel("chainmail_leggings[max_damage=700,attribute_modifiers={modifiers:[{id:\"leggings_armor\",type:\"armor\",amount:4,operation:\"add_value\",slot:\"legs\"},{id:\"leggings_armor_toughness\",type:\"armor_toughness\",amount:2,operation:\"add_value\",slot:\"legs\"},{id:\"leggings_block_interaction_range\",type:\"block_interaction_range\",amount:1,operation:\"add_value\",slot:\"legs\"}],show_in_tooltip:true},item_name='{\"color\":\"aqua\",\"italic\":false,\"text\":\"Miner\\'s Pants\"}',enchantments={levels:{\"minecraft:swift_sneak\":3}}]", "miner_pants");
    public static final String MINER_BOOTS = withCustomModel("chainmail_boots[max_damage=600,attribute_modifiers={modifiers:[{id:\"feet_armor\",type:\"armor\",amount:2,operation:\"add_value\",slot:\"feet\"},{id:\"feet_armor_toughness\",type:\"armor_toughness\",amount:2,operation:\"add_value\",slot:\"feet\"},{id:\"feet_block_break_speed\",type:\"block_break_speed\",amount:0.05,operation:\"add_multiplied_total\",slot:\"feet\"}],show_in_tooltip:true},item_name='{\"color\":\"aqua\",\"italic\":false,\"text\":\"Miner\\'s Greaves\"}']", "miner_greaves");
    public static final String MINER_GRAVITY_PICKAXE = withCustomModel(withEvent("diamond_pickaxe[max_damage=4000,item_name='{\"color\":\"yellow\",\"text\":\"Gravity Pickaxe\"}']", MINER_ID, USE_ON_BLOCK_EVENTS, (player, stack, pos, world) -> {
        BlockState b = world.getBlockState(pos);
        if (b.getBlock().getHardness() > 0 && !b.hasBlockEntity()) {
            FallingBlockEntity.spawnFromBlock(world, pos, b);
            return new EventResult((int) Math.min(200, Math.max(5, b.getBlock().getBlastResistance() * 5)), ActionResult.SUCCESS_SERVER);
        }
        return new EventResult(-1, null);
    }), "gravity_pickaxe");

    public static final String ZOMBIE_BOSS_ARMOR = "netherite_chestplate[enchantments={levels:{\"minecraft:projectile_protection\":6,\"minecraft:magic_guard\":1}}]";

    public static final String ZOMBIE_KING_WITHER_SWORD = withCustomModel("stone_sword[enchantments={levels:{\"minecraft:withering\":1}}]", "wither_sword");
    public static final String ZOMBIE_KING_TOILET_PAPER = withCustomModel("paper[attribute_modifiers=[{id:\"offhand_attack_damage\",type:\"attack_damage\",amount:7,operation:\"add_value\",slot:\"offhand\"},{id:\"offhand_attack_speed\",type:\"attack_speed\",amount:-0.6,operation:\"add_multiplied_total\",slot:\"offhand\"},{id:\"offhand_armor\",type:\"armor\",amount:5,operation:\"add_value\",slot:\"offhand\"},{id:\"offhand_armor_toughness\",type:\"armor_toughness\",amount:6,operation:\"add_value\",slot:\"offhand\"},{id:\"offhand_movement_speed\",type:\"movement_speed\",amount:-0.6,operation:\"add_multiplied_total\",slot:\"offhand\"},{id:\"offhand_max_health\",type:\"max_health\",amount:-2,operation:\"add_value\",slot:\"offhand\"}],item_name='{\"color\":\"#401804\",\"italic\":false,\"text\":\"Ludfru\\'s Toilet Paper\"}',max_stack_size=1]", "ludfru_toilet_paper");

    public static final String GIANT_SKELETON_BOW = "bow[enchantments={levels:{\"minecraft:power\":4}}]";
    public static final String GIANT_CHESTPLATE = withCustomModel("iron_chestplate[trim={material:\"minecraft:netherite\",pattern:\"minecraft:wayfinder\",show_in_tooltip:false},attribute_modifiers=[{id:\"chest_armor\",type:\"armor\",amount:7,operation:\"add_value\",slot:\"chest\"},{id:\"chest_armor_toughness\",type:\"armor_toughness\",amount:5,operation:\"add_value\",slot:\"chest\"},{id:\"chest_scale\",type:\"scale\",amount:0.5,operation:\"add_multiplied_total\",slot:\"chest\"},{id:\"chest_block_interaction_range\",type:\"block_interaction_range\",amount:0.5,operation:\"add_value\",slot:\"chest\"},{id:\"chest_entity_interaction_range\",type:\"entity_interaction_range\",amount:0.5,operation:\"add_value\",slot:\"chest\"}],custom_name='{\"color\":\"aqua\",\"italic\":false,\"text\":\"Giant\\'s Chestplate\"}',max_damage=700]", "giant_chestplate");

    public static final String DAEDALUS_HELMET = withCustomModel("diamond_helmet[custom_name='{\"color\":\"aqua\",\"italic\":false,\"text\":\"Scoped Helmet\"}',max_damage=600,enchantments={levels:{\"minecraft:sniper\":1}}]", "scoped_helmet");
    public static final String DAEDALUS_STORMBOW = withCustomModel("bow[enchantments={levels:{\"minecraft:arrow_rain\":1,\"minecraft:flak\":1}}]", "daedalus_stormbow");
    public static final String DAEDALUS_STORMBOW_DROPPED = withCustomModel("bow[custom_name='{\"color\":\"dark_red\",\"italic\":false,\"text\":\"Daedalus Stormbow\"}',enchantments={levels:{\"minecraft:arrow_rain\":1}},unbreakable={}]", "daedalus_stormbow");
    public static final String DAEDALUS_BOOTS = withCustomModel("chainmail_boots[trim={material:\"minecraft:redstone\",pattern:\"minecraft:raiser\",show_in_tooltip:false},enchantments={levels:{\"minecraft:rider\":1,\"minecraft:blink\":1,\"minecraft:windstep\":1}},custom_name='{\"color\":\"aqua\",\"italic\":false,\"text\":\"Daedalus Boots\"}',attribute_modifiers=[{id:\"feet_armor\",type:\"armor\",amount:1,operation:\"add_value\",slot:\"feet\"}],max_damage=4000]", "daedalus_boots");

    public static final String POTIONEER_CROSSBOW = withCustomModel("crossbow[enchantments={levels:{\"minecraft:quick_charge\":1,\"minecraft:multishot\":1,\"minecraft:adaptability\":1}},custom_name='{\"color\":\"dark_red\",\"italic\":false,\"text\":\"Adaptabow\"}',max_damage=600]", "adaptabow");
    public static final String POTIONEER_POTION = "splash_potion[potion_contents={potion:\"minecraft:strong_harming\"}]";
    public static final String POTIONEER_CHESTPLATE = withCustomModel("chainmail_chestplate[glider={},max_damage=120,custom_name='{\"color\":\"aqua\",\"italic\":false,\"text\":\"Broken Floaty Plate\"}',attribute_modifiers=[{id:\"chest_max_health\",type:\"max_health\",amount:2,operation:\"add_value\",slot:\"chest\"},{id:\"chest_armor\",type:\"armor\",amount:3,operation:\"add_value\",slot:\"chest\"},{id:\"chest_armor_toughness\",type:\"armor_toughness\",amount:2,operation:\"add_value\",slot:\"chest\"}],enchantments={levels:{\"minecraft:magic_guard\":1}}]", "broken_floaty_plate");

    public static final String SKELATOM_BOW = withCustomModel("bow[custom_name='{\"color\":\"dark_red\",\"italic\":false,\"text\":\"SUPER PUNCH BOW\"}',max_damage=120,enchantments={levels:{\"minecraft:punch\":4}}]", "super_punch_bow");
    public static final String SKELATOM_BONE_ZONE = withCustomModel(withEvent("bone[custom_name='{\"color\":\"yellow\",\"italic\":false,\"text\":\"Bone Zone\"}',enchantment_glint_override=true,max_stack_size=1]", SKELATOM_ID, TICK_EVENTS, (player, stack, held, world) -> {
        if (player.getHungerManager().getFoodLevel() > 0 && player.getHealth() <= (player.getMaxHealth() / 2) - 1) {
            player.getHungerManager().setSaturationLevel(0);
            player.getHungerManager().addExhaustion(8);
            player.heal(1);
            return new EventResult(held ? 80 : 200, null);
        }
        return new EventResult(0, null);
    }), "bone_zone");
    public static final String SKELATOM_PLATE = withCustomModel("chainmail_chestplate[custom_name='{\"color\":\"aqua\",\"italic\":false,\"text\":\"Atom Plate\"}',attribute_modifiers=[{id:\"atom_armor\",type:\"armor\",amount:-1,operation:\"add_multiplied_total\",slot:\"chest\"},{id:\"atom_armor_toughness\",type:\"armor_toughness\",amount:-1,operation:\"add_multiplied_total\",slot:\"chest\"},{id:\"atom_gravity\",type:\"gravity\",amount:-0.7,operation:\"add_multiplied_total\",slot:\"chest\"},{id:\"atom_attack_damage\",type:\"attack_damage\",amount:-0.2,operation:\"add_multiplied_total\",slot:\"chest\"},{id:\"atom_safe_fall_distance\",type:\"safe_fall_distance\",amount:40,operation:\"add_value\",slot:\"chest\"},{id:\"atom_max_health\",type:\"max_health\",amount:-20,operation:\"add_value\",slot:\"chest\"},{id:\"atom_scale\",type:\"scale\",amount:-0.95,operation:\"add_multiplied_total\",slot:\"chest\"}]]", "atom_plate");

    public static final String SKELEPANT_HANDPANT = withCustomModel("iron_leggings[custom_name='{\"color\":\"dark_red\",\"italic\":false,\"text\":\"Handpants\"}',max_damage=400,attribute_modifiers=[{id:\"base_attack_damage\",type:\"attack_damage\",amount:6,operation:\"add_value\",slot:\"mainhand\"},{id:\"base_attack_speed\",type:\"attack_speed\",amount:-2,operation:\"add_value\",slot:\"mainhand\"}],equippable={slot:\"mainhand\",swappable:false}]", "headpants");
    public static final String SKELEPANT_HEADPANT = withCustomModel("iron_leggings[custom_name='{\"color\":\"aqua\",\"italic\":false,\"text\":\"Headpants\"}',max_damage=400,attribute_modifiers=[{id:\"head_armor\",type:\"armor\",amount:3,operation:\"add_value\",slot:\"head\"},{id:\"head_toughness\",type:\"armor_toughness\",amount:2,operation:\"add_value\",slot:\"head\"},{id:\"max_health\",type:\"max_health\",amount:2,operation:\"add_value\",slot:\"head\"}],equippable={slot:\"head\"}]", "handpants");
    public static final String SKELEPANT_OFFHANDPANT = withCustomModel("iron_leggings[custom_name='{\"color\":\"yellow\",\"italic\":false,\"text\":\"Offhandpants\"}',max_damage=400,attribute_modifiers=[{id:\"offhand_block_break_speed\",type:\"block_break_speed\",amount:0.1,operation:\"add_multiplied_total\",slot:\"offhand\"},{id:\"offhand_block_interaction_range\",type:\"block_interaction_range\",amount:0.5,operation:\"add_value\",slot:\"offhand\"},{id:\"offhand_movement_speed\",type:\"movement_speed\",amount:0.1,operation:\"add_multiplied_total\",slot:\"offhand\"},{id:\"offhand_entity_interaction_range\",type:\"entity_interaction_range\",amount:0.5,operation:\"add_value\",slot:\"offhand\"}],equippable={slot:\"offhand\",swappable:false}]", "offhandpants");

    public static final String STRANGE_SKELETON_CROSSBOW = withCustomModel("crossbow[custom_name='{\"color\":\"dark_red\",\"italic\":false,\"text\":\"Perfectly Legal Crossbow\"}',enchantments={levels:{\"minecraft:quick_charge\":3,\"minecraft:multishot\":1,\"minecraft:piercing\":4}}]", "legal_crossbow");
    public static final String STRANGE_SKELETON_HAT = withCustomModel("turtle_helmet[trim={material:\"minecraft:gold\",pattern:\"minecraft:vex\",show_in_tooltip:false},custom_name='{\"color\":\"aqua\",\"italic\":false,\"text\":\"Prestigious Hat\"}',enchantments={levels:{\"minecraft:heroic\":1}}]", "prestigious_hat");

    public static final String THUNDER_SKELETON_BOW = withCustomModel("bow[custom_name='{\"color\":\"dark_red\",\"italic\":false,\"text\":\"THIS BOW IS FULL OF EVIL\"}',enchantments={levels:{\"minecraft:thunderbolt\":1}}]", "evil_bow");
    public static final String THUNDER_SKELETON_CHESTPLATE = withCustomModel("golden_chestplate[custom_name='{\"color\":\"aqua\",\"italic\":false,\"text\":\"Perfection\"}',max_damage=80,attribute_modifiers=[{id:\"chest_armor\",type:\"armor\",amount:20,operation:\"add_value\",slot:\"chest\"},{id:\"chest_armor_toughness\",type:\"armor_toughness\",amount:20,operation:\"add_value\",slot:\"chest\"}]]", "perfection");

    public static final String UNDEAD_PILLAR_BANNER = "white_banner[rarity=\"uncommon\",item_name='{\"translate\":\"block.minecraft.ominous_banner\"}',hide_additional_tooltip={},banner_patterns=[{pattern:\"minecraft:rhombus\",color:\"cyan\"},{pattern:\"minecraft:stripe_bottom\",color:\"light_gray\"},{pattern:\"minecraft:stripe_center\",color:\"gray\"},{pattern:\"minecraft:border\",color:\"light_gray\"},{pattern:\"minecraft:stripe_middle\",color:\"black\"},{pattern:\"minecraft:half_horizontal\",color:\"light_gray\"},{pattern:\"minecraft:circle\",color:\"light_gray\"},{pattern:\"minecraft:border\",color:\"black\"}]]";
    public static final String UNDEAD_PILLAGER_CROSSBOW = "crossbow[enchantments={levels:{\"minecraft:quick_charge\":4,\"minecraft:arrow_rain\":1}}]";
    public static final String UNDEAD_PILLAGER_CROSSBOW_LOOT = withCustomModel("crossbow[enchantments={levels:{\"minecraft:arrow_rain\":1,\"minecraft:flame\":1}},custom_name='{\"color\":\"dark_red\",\"italic\":false,\"text\":\"Hellstorm\"}']", "hellstorm");
    public static final String UNDEAD_PILLAGER_VEST = withCustomModel("leather_chestplate[enchantments={levels:{\"minecraft:projectile_protection\":8}},dyed_color=0,trim={material:\"minecraft:diamond\",pattern:\"minecraft:wild\",show_in_tooltip:false},custom_name='{\"color\":\"aqua\",\"italic\":false,\"text\":\"Pillager Vest\"}',attribute_modifiers=[{id:\"chest_armor\",type:\"armor\",amount:6,operation:\"add_value\",slot:\"chest\"},{id:\"chest_toughness\",type:\"armor_toughness\",amount:4,operation:\"add_value\",slot:\"chest\"},{id:\"chest_knockback_resistance\",type:\"knockback_resistance\",amount:0.4,operation:\"add_value\",slot:\"chest\"},{id:\"chest_movement_speed\",type:\"movement_speed\",amount:0.15,operation:\"add_multiplied_base\",slot:\"chest\"}],max_damage=500]", "pillager_vest");

}
