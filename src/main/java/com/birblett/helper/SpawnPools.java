package com.birblett.helper;

import com.birblett.EpicMod;
import com.birblett.interfaces.AbilityUser;
import com.birblett.interfaces.Lootable;
import com.mojang.brigadier.StringReader;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.*;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootTable;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.apache.commons.lang3.function.TriFunction;
import org.apache.logging.log4j.util.TriConsumer;
import oshi.util.tuples.Pair;

import java.util.*;

import static com.birblett.helper.Ability.*;
import static com.birblett.helper.CustomItems.*;
import static com.birblett.helper.SpawnPools.Modifier.*;
import static net.minecraft.entity.attribute.EntityAttributes.*;

public class SpawnPools {

    public static class WeightedRandomPool<T> {

        private final Random random = new Random();
        private final LinkedHashMap<Integer, T> entries = new LinkedHashMap<>();
        private Integer[] keys = new Integer[]{};

        public void addModifier(int weight, T value) {
            this.entries.put(this.entries.lastEntry() == null ? weight : weight + this.entries.lastEntry().getKey(), value);
            this.keys = this.entries.keySet().toArray(new Integer[0]);
        }

        public T getRandomEntry() {
            int left = 0, right = this.keys.length, target = this.random.nextInt(0, this.entries.lastEntry().getKey()), mid;
            while (left != right) {
                if (target >= this.keys[mid = ((right + left) >> 1)]) {
                    left = right - left == 1 ? right : mid;
                } else {
                    right = mid;
                }
            }
            return this.entries.get(this.keys[left]);
        }

        public void resetEntries() {
            this.entries.clear();
            this.keys = new Integer[]{};
        }

        @Override
        public String toString() {
            return this.entries.toString();
        }

    }

    public static class Modifier<T extends MobEntity> {

        public static final int MAINHAND = 0;
        public static final int OFFHAND = 1;
        public static final int HEAD = 2;
        public static final int CHEST = 3;
        public static final int LEGS = 4;
        public static final int FEET = 5;
        public static final int ADD = 0;
        public static final int MUL = 1;
        private static final EquipmentSlot[] SLOTS = new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND, EquipmentSlot.HEAD,
                EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

        public static CommandRegistryAccess access = null;

        private final ArrayList<Pair<RegistryEntry<EntityAttribute>, Double>> attributes = new ArrayList<>();
        private final HashMap<RegistryEntry<EntityAttribute>, Pair<Integer, Double>> attributeMods = new HashMap<>();
        private final ItemStack[] equips = new ItemStack[6];
        private final float[] dropChance = new float[6];

        private ItemStackArgumentType stackGenerator = null;
        private int maxHealth = 0;
        private Ability[] abilities = null;
        private TriConsumer<T, ServerWorld, Modifier<T>> onSpawn = null;
        private TriFunction<T, ServerWorld, Modifier<T>, Entity> redirect = null;
        private Text name = null;
        private int lastSlot = -1;
        private RegistryKey<LootTable> lootTable = null;

        public Modifier() {
            if (access != null) {
                this.stackGenerator = ItemStackArgumentType.itemStack(access);
            }
        }

        public Modifier<T> setName(Text text) {
            this.name = text;
            return this;
        }

        public Modifier<T> setName(String name) {
            return this.setName(Text.literal(name));
        }

        public Modifier<T> setMaxHealth(int maxHealth) {
            this.maxHealth = maxHealth;
            return this;
        }

        public Modifier<T> baseAttribute(RegistryEntry<EntityAttribute> e, double d) {
            this.attributes.add(new Pair<>(e, d));
            return this;
        }

        public Modifier<T> modAttribute(RegistryEntry<EntityAttribute> e, int t, double d) {
            this.attributeMods.put(e, new Pair<>(t, d));
            return this;
        }

        public Modifier<T> setStack(int slot, ItemStack stack, int count) {
            this.lastSlot = slot;
            stack.setCount(count);
            this.equips[slot] = stack;
            return this;
        }

        public Modifier<T> setStack(int slot, Item item) {
            return this.setStack(slot, item.getDefaultStack(), 1);
        }

        public Modifier<T> setStack(int slot, String string, int count) {
            try {
                return access != null ? this.setStack(slot, this.stackGenerator.parse(new StringReader(string)).createStack(count, false), count) : this;
            } catch(Exception e) {
                EpicMod.LOGGER.info("Modifier failed to parse item string \"{}\"", string);
                return this;
            }
        }

        public Modifier<T> setStack(int slot, String string) {
            return this.setStack(slot, string, 1);
        }

        public Modifier<T> setDropChance(int slot, float dropChance) {
            this.dropChance[slot] = dropChance;
            return this;
        }

        public Modifier<T> setDropChance(float dropChance) {
            return this.lastSlot > -1 ? this.setDropChance(this.lastSlot, dropChance) : this;
        }

        public Modifier<T> setAbilities(Ability... abilities) {
            this.abilities = abilities;
            return this;
        }
        public Modifier<T> setLootTable(String id) {
            this.lootTable = RegistryKey.of(RegistryKeys.LOOT_TABLE, Identifier.ofVanilla(id));
            return this;
        }

        public Modifier<T> eventOnSpawn(TriConsumer<T, ServerWorld, Modifier<T>> apply) {
            this.onSpawn = apply;
            return this;
        }

        public Modifier<T> entityRedirect(TriFunction<T, ServerWorld, Modifier<T>, Entity> apply) {
            this.redirect = apply;
            return this;
        }

        public Entity apply(T entity, boolean applyConsumer) {
            if (this.name != null) {
                entity.setCustomName(this.name);
                entity.setCustomNameVisible(true);
            }
            EntityAttributeInstance e;
            if (this.maxHealth != 0 && (e = entity.getAttributeInstance(MAX_HEALTH)) != null) {
                e.setBaseValue(this.maxHealth);
                entity.setHealth(this.maxHealth);
            }
            for (var p : this.attributes) {
                if ((e = entity.getAttributeInstance(p.getA())) != null) {
                    e.setBaseValue(p.getB());
                }
            }
            for (var k : this.attributeMods.keySet()) {
                if ((e = entity.getAttributeInstance(k)) != null) {
                    var p = this.attributeMods.get(k);
                    e.setBaseValue(p.getA() == 0 ? e.getBaseValue() + p.getB() : e.getBaseValue() * p.getB());
                }
            }
            for (int i = 0; i < 6; ++i) if (this.equips[i] != null) {
                entity.equipStack(SLOTS[i], this.equips[i].copy());
                entity.setEquipmentDropChance(SLOTS[i], this.dropChance[i]);
            }
            if (this.abilities != null) {
                ((AbilityUser) entity).addAbilities(this.abilities);
            }
            if (this.lootTable != null) {
                ((Lootable) entity).setLootTable(this.lootTable);
            }
            entity.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 300));
            if (applyConsumer && entity.getWorld() instanceof ServerWorld w) {
                if (this.onSpawn != null) {
                    this.onSpawn.accept(entity, w, this);
                }
                if (this.abilities != null) {
                    if (List.of(this.abilities).contains(BOSS_FLAG)){
                        w.getServer().getPlayerManager().broadcast(SignedMessage.ofUnsigned(String.format("%s spawned at [%d %d %d]!",
                                        this.name.getLiteralString(), (int) entity.getX(), (int) entity.getY(), (int) entity.getZ())),
                                w.getServer().getCommandSource(), MessageType.params(MessageType.SAY_COMMAND, w.getServer().getCommandSource()));
                    } else if (List.of(this.abilities).contains(ELITE)) {
                        for (var player : w.getServer().getPlayerManager().getPlayerList()) {
                            if (player.squaredDistanceTo(entity) < (96 * 96)) {
                                player.sendMessage(Text.of(String.format("%s spawned at [%d %d %d]!", this.name.getLiteralString(), (int)
                                        entity.getX(), (int) entity.getY(), (int) entity.getZ())));
                            }
                        }
                    }
                }
            }
            if (this.redirect != null && entity.getWorld() instanceof ServerWorld w) {
                return this.redirect.apply(entity, w, this);
            }
            entity.setCanPickUpLoot(false);
            return entity;
        }

        public Entity apply(T entity) {
            return this.apply(entity, true);
        }

        public static <R extends MobEntity> Modifier<R> noOp() {
            return new Modifier<>() {
                @Override
                public Entity apply(R entity) { return entity; }
            };
        }

    }

    public static final WeightedRandomPool<Modifier<ZombieEntity>> ZOMBIES = new WeightedRandomPool<>();
    private static final TriConsumer<ZombieEntity, ServerWorld, Modifier<ZombieEntity>> SET_BABY = (z, w, m) -> z.setBaby(true);
    private static final TriConsumer<ZombieEntity, ServerWorld, Modifier<ZombieEntity>> SET_NOT_BABY = (z, w, m) -> z.setBaby(false);
    private static final TriConsumer<ZombieEntity, ServerWorld, Modifier<ZombieEntity>> UPDATE_GOALS = (z, w, m) -> ((AbilityUser) z).updateGoals();

    public static void zombiePoolInit() {
        ZOMBIES.resetEntries();
        ZOMBIES.addModifier(50, new Modifier<ZombieEntity>().setName("Drunkard")
                .setStack(HEAD, Items.IRON_HELMET)
                .setStack(MAINHAND, DRUNKARD_BEER).setDropChance(1.0f)
                .setMaxHealth(34)
                .modAttribute(MOVEMENT_SPEED, MUL, 0.8)
                .eventOnSpawn(SET_NOT_BABY));
        ZOMBIES.addModifier(50, new Modifier<ZombieEntity>().setName("Drunkard")
                .setStack(HEAD, Items.IRON_HELMET)
                .setStack(MAINHAND, DRUNKARD_MOONSHINE).setDropChance(1.0f)
                .setMaxHealth(34)
                .modAttribute(MOVEMENT_SPEED, MUL, 0.8)
                .eventOnSpawn(SET_NOT_BABY));
        ZOMBIES.addModifier(50, new Modifier<ZombieEntity>().setName("Drunkard")
                .setStack(HEAD, Items.IRON_HELMET)
                .setStack(MAINHAND, DRUNKARD_QMARKS).setDropChance(1.0f)
                .setMaxHealth(34)
                .modAttribute(MOVEMENT_SPEED, MUL, 0.8)
                .eventOnSpawn(SET_NOT_BABY));
        ZOMBIES.addModifier(50, new Modifier<ZombieEntity>().setName("Drunkard")
                .setStack(HEAD, Items.IRON_HELMET)
                .setStack(MAINHAND, DRUNKARD_RED_WINE).setDropChance(1.0f)
                .setMaxHealth(34)
                .modAttribute(MOVEMENT_SPEED, MUL, 0.8)
                .eventOnSpawn(SET_NOT_BABY));
        ZOMBIES.addModifier(50, new Modifier<ZombieEntity>().setName("Drunkard")
                .setStack(HEAD, Items.IRON_HELMET)
                .setStack(MAINHAND, DRUNKARD_RUM).setDropChance(1.0f)
                .setMaxHealth(34)
                .modAttribute(MOVEMENT_SPEED, MUL, 0.8)
                .eventOnSpawn(SET_NOT_BABY));
        ZOMBIES.addModifier(60, new Modifier<ZombieEntity>().setName("Farmer")
                .setStack(MAINHAND, FARMER_HOE).setDropChance(0.2f)
                .setStack(HEAD, Items.LEATHER_HELMET)
                .setStack(FEET, FARMER_BOOTS).setDropChance(0.2f)
                .setMaxHealth(25)
                .baseAttribute(ATTACK_DAMAGE, 6)
                .eventOnSpawn(SET_NOT_BABY));
        ZOMBIES.addModifier(55, new Modifier<ZombieEntity>().setName("Miner")
                .setStack(MAINHAND, MINER_GRAVITY_PICKAXE).setDropChance(0.2f)
                .setStack(HEAD, MINER_HELMET).setDropChance(0.1f)
                .setStack(CHEST, MINER_CHESTPLATE).setDropChance(0.1f)
                .setStack(LEGS, MINER_LEGGINGS).setDropChance(0.1f)
                .setStack(LEGS, MINER_BOOTS).setDropChance(0.1f)
                .setMaxHealth(25)
                .eventOnSpawn(SET_NOT_BABY));
        ZOMBIES.addModifier(20, new Modifier<ZombieEntity>().setName("Dashmaster")
                .setStack(MAINHAND, DASHMASTER_SHARD).setDropChance(0.25f)
                .setMaxHealth(25)
                .baseAttribute(ATTACK_DAMAGE, 8)
                .modAttribute(STEP_HEIGHT, ADD, 0.5)
                .modAttribute(MOVEMENT_SPEED, MUL, 1.6)
                .modAttribute(KNOCKBACK_RESISTANCE, ADD,10)
                .setAbilities(DASHMASTER, IGNORE_FIRE, ZOMBIE_IGNORE_WATER, ELITE)
                .setLootTable("entities/custom/zombie_elite")
                .eventOnSpawn(UPDATE_GOALS));
        ZOMBIES.addModifier(20, new Modifier<ZombieEntity>().setName("Dullahan")
                .setStack(MAINHAND, DULLAHAN_KNIFE).setDropChance(0.3f)
                .setStack(OFFHAND, DULLAHAN_HEAD)
                .setStack(HEAD, Items.CARVED_PUMPKIN)
                .setStack(CHEST, Items.DIAMOND_CHESTPLATE)
                .setMaxHealth(70)
                .baseAttribute(ARMOR, 20)
                .modAttribute(MOVEMENT_SPEED, MUL,0.8)
                .modAttribute(KNOCKBACK_RESISTANCE, ADD,10)
                .setAbilities(DULLAHAN, IGNORE_SUFFOCATION, IGNORE_FALL, ZOMBIE_IGNORE_WATER)
                .setLootTable("entities/custom/dullahan")
                .entityRedirect(((zombie, world, modifier) -> {
                    ZombieHorseEntity horse = new ZombieHorseEntity(EntityType.ZOMBIE_HORSE, world);
                    horse.setTame(true);
                    horse.setSilent(true);
                    horse.setPos(zombie.getX(), zombie.getY(), zombie.getZ());
                    Util.setAttributeBase(horse, MAX_HEALTH, 80);
                    Util.setAttributeBase(horse, MOVEMENT_SPEED, 0.35);
                    horse.saddle(Items.SADDLE.getDefaultStack(), null);
                    horse.setBaby(true);
                    ((AbilityUser) horse).addAbilities(CAN_DESPAWN_WITH_PARENT, IGNORE_FALL, ELITE);
                    zombie.startRiding(horse);
                    return horse;
                })));
        ZOMBIES.addModifier(20, new Modifier<ZombieEntity>().setName("Enforcer")
                .setStack(MAINHAND, ENFORCER_BATON).setDropChance(0.25f)
                .setStack(OFFHAND, ENFORCER_RIOT_SHIELD).setDropChance(0.25f)
                .setStack(HEAD, Items.CHAINMAIL_HELMET)
                .setStack(CHEST, Items.CHAINMAIL_CHESTPLATE)
                .setStack(LEGS, Items.CHAINMAIL_LEGGINGS)
                .setStack(FEET, Items.CHAINMAIL_BOOTS)
                .setMaxHealth(80)
                .baseAttribute(ATTACK_DAMAGE, 10)
                .modAttribute(ARMOR_TOUGHNESS, ADD, 12)
                .modAttribute(MOVEMENT_SPEED, MUL, 0.8)
                .modAttribute(KNOCKBACK_RESISTANCE, ADD, 10)
                .setAbilities(ZOMBIE_IGNORE_WATER, ELITE)
                .setLootTable("entities/custom/zombie_elite")
                .eventOnSpawn(SET_NOT_BABY));
        ZOMBIES.addModifier(20, new Modifier<ZombieEntity>().setName("Killer Pillar")
                .setStack(MAINHAND, KILLER_PILLAR_ASS).setDropChance(0.09f)
                .setStack(HEAD, KILLER_PILLAR_COAL_HELMET).setDropChance(0.09f)
                .setStack(CHEST, KILLER_PILLAR_CHEST)
                .setStack(LEGS, KILLER_PILLAR_LEGS)
                .setStack(FEET, KILLER_PILLAR_FEET)
                .setMaxHealth(50)
                .modAttribute(MOVEMENT_SPEED, MUL, 0.4)
                .modAttribute(KNOCKBACK_RESISTANCE, ADD, 10)
                .setAbilities(KILLER_PILLAR, IGNORE_FIRE, IGNORE_FALL, IGNORE_SUFFOCATION, ZOMBIE_IGNORE_WATER, ELITE)
                .setLootTable("entities/custom/killer_pillar")
                .eventOnSpawn((zombie, world, modifier) -> {
                    zombie.setBaby(true);
                    for (int i = 0; i < 2; ++i) {
                        ZombieEntity next = new ZombieEntity(world);
                        next.startRiding(zombie);
                        modifier.apply(next, false);
                        next.setBaby(true);
                        next.setPosition(zombie.getPos());
                        zombie = next;
                    }
                    SkeletonEntity skel = new SkeletonEntity(EntityType.SKELETON, world);
                    skel.setPosition(zombie.getPos());
                    skel.setCustomName(Text.literal("Killer Pillar"));
                    ItemStack stack = Items.BOW.getDefaultStack();
                    stack.addEnchantment(Util.getEntry(world, Enchantments.POWER), 4);
                    skel.equipStack(EquipmentSlot.MAINHAND, stack);
                    skel.getAttributeInstance(MAX_HEALTH).setBaseValue(60);
                    skel.setHealth(60);
                    ((AbilityUser) skel).addAbilities(IGNORE_FIRE, IGNORE_FALL, IGNORE_SUFFOCATION, ELITE);
                    for (EquipmentSlot e : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                        skel.equipStack(e, zombie.getEquippedStack(e).copy());
                        skel.setEquipmentDropChance(e, e == EquipmentSlot.HEAD ? 0.18f : 0);
                    }
                    skel.startRiding(zombie);
                }));
        ZOMBIES.addModifier(20, new Modifier<ZombieEntity>().setName("Ludfru")
                .setStack(MAINHAND, LUDFRU_SHIT).setDropChance(0.3f)
                .setStack(HEAD, LUDFRU_ASTRONAUT_HELMET).setDropChance(0.25f)
                .setStack(CHEST, Items.CHAINMAIL_CHESTPLATE)
                .setStack(LEGS, Items.CHAINMAIL_LEGGINGS)
                .setStack(FEET, Items.CHAINMAIL_BOOTS)
                .setMaxHealth(200)
                .modAttribute(MOVEMENT_SPEED, MUL, 0.3)
                .modAttribute(KNOCKBACK_RESISTANCE, ADD,10)
                .setAbilities(LUDFRU, ZOMBIE_IGNORE_WATER, ELITE)
                .setLootTable("entities/custom/zombie_elite")
                .eventOnSpawn(SET_BABY));
        ZOMBIES.addModifier(20, new Modifier<ZombieEntity>().setName("Zombie King")
                .setStack(MAINHAND, ZOMBIE_KING_WITHER_SWORD).setDropChance(0.3f)
                .setStack(OFFHAND, ZOMBIE_KING_TOILET_PAPER).setDropChance(0.2f)
                .setStack(HEAD, Items.GOLDEN_HELMET)
                .setStack(CHEST, Items.GOLDEN_CHESTPLATE)
                .setMaxHealth(150)
                .modAttribute(ATTACK_DAMAGE, ADD, 8)
                .modAttribute(ATTACK_KNOCKBACK, ADD,1)
                .modAttribute(KNOCKBACK_RESISTANCE, ADD,10)
                .setAbilities(ZOMBIE_KING, IGNORE_FIRE, IGNORE_FALL, ZOMBIE_IGNORE_WATER, ELITE)
                .eventOnSpawn(SET_NOT_BABY));
        ZOMBIES.addModifier(5, new Modifier<ZombieEntity>().setName("Archmage")
                .setStack(MAINHAND, ARCHMAGE_TOME).setDropChance(0.5f)
                .setStack(OFFHAND, Items.STICK)
                .setStack(HEAD, Items.CREAKING_HEART)
                .setStack(CHEST, ZOMBIE_BOSS_ARMOR)
                .setStack(LEGS, Items.LEATHER_LEGGINGS)
                .setStack(FEET, Items.LEATHER_BOOTS)
                .setMaxHealth(250)
                .baseAttribute(ATTACK_DAMAGE, 10)
                .baseAttribute(MOVEMENT_SPEED, 0)
                .baseAttribute(GRAVITY, 0)
                .baseAttribute(ARMOR, 14)
                .baseAttribute(ARMOR_TOUGHNESS, 6)
                .baseAttribute(KNOCKBACK_RESISTANCE,10)
                .setAbilities(ZOMBIE_MAGE, IGNORE_EXPLOSION, IGNORE_FALL, IGNORE_FAR_DAMAGE, IGNORE_LIGHTNING, IGNORE_SUFFOCATION,
                        BOSS_FLAG, IMPORTANT, LINE_OF_SIGHT_DAMAGE)
                .setLootTable("entities/custom/archmage")
                .eventOnSpawn((z, w, m) -> {
                    UPDATE_GOALS.accept(z, w, m);
                    SET_NOT_BABY.accept(z, w, m);
                }));
        ZOMBIES.addModifier(5, new Modifier<ZombieEntity>().setName("Bot")
                .setStack(MAINHAND, BOT_SWORD).setDropChance(0.5f)
                .setStack(OFFHAND, Items.ENDER_PEARL)
                .setStack(HEAD, Items.OBSERVER)
                .setStack(CHEST, ZOMBIE_BOSS_ARMOR)
                .setStack(LEGS, Items.IRON_LEGGINGS)
                .setStack(FEET, Items.DIAMOND_BOOTS)
                .setMaxHealth(270)
                .baseAttribute(ATTACK_DAMAGE, 10)
                .modAttribute(STEP_HEIGHT, ADD, 0.5)
                .modAttribute(MOVEMENT_SPEED, MUL, 1.2)
                .baseAttribute(ARMOR, 16)
                .baseAttribute(ARMOR_TOUGHNESS, 8)
                .modAttribute(KNOCKBACK_RESISTANCE, ADD,10)
                .setAbilities(BOT, IGNORE_EXPLOSION, IGNORE_FALL, IGNORE_FAR_DAMAGE, IGNORE_LIGHTNING, IGNORE_SUFFOCATION, BOSS_FLAG,
                        IMPORTANT, LINE_OF_SIGHT_DAMAGE)
                .setLootTable("entities/custom/bot")
                .eventOnSpawn((z, w, m) -> {
                    UPDATE_GOALS.accept(z, w, m);
                    SET_NOT_BABY.accept(z, w, m);
                }));
        ZOMBIES.addModifier(5, new Modifier<ZombieEntity>().setName("Gunner")
                .setStack(MAINHAND, Items.WOODEN_HOE)
                .setStack(OFFHAND, GUNNER_TRIDENT).setDropChance(0.5f)
                .setStack(HEAD, GUNNER_HELMET)
                .setStack(CHEST, ZOMBIE_BOSS_ARMOR)
                .setStack(LEGS, Items.DIAMOND_LEGGINGS)
                .setStack(FEET, Items.DIAMOND_BOOTS)
                .setMaxHealth(250)
                .baseAttribute(ATTACK_DAMAGE, 10)
                .modAttribute(STEP_HEIGHT, ADD, 0.5)
                .modAttribute(MOVEMENT_SPEED, MUL, 0.8)
                .baseAttribute(ARMOR, 15)
                .baseAttribute(ARMOR_TOUGHNESS, 7)
                .modAttribute(KNOCKBACK_RESISTANCE, ADD,10)
                .setAbilities(GUNNER, IGNORE_EXPLOSION, IGNORE_FALL, IGNORE_FAR_DAMAGE, IGNORE_LIGHTNING, IGNORE_SUFFOCATION, BOSS_FLAG,
                        IMPORTANT, LINE_OF_SIGHT_DAMAGE)
                .setLootTable("entities/custom/gunner")
                .eventOnSpawn((z, w, m) -> {
                    UPDATE_GOALS.accept(z, w, m);
                    SET_NOT_BABY.accept(z, w, m);
                }));
        ZOMBIES.addModifier(12500 - 250 - 250, noOp());
        EpicMod.LOGGER.info("Loaded zombie spawn pools: {}", ZOMBIES);
    }

    public static final WeightedRandomPool<Modifier<SkeletonEntity>> SKELETONS = new WeightedRandomPool<>();

    public static void skeletonPoolInit() {
        SKELETONS.resetEntries();
        SKELETONS.addModifier(40, new Modifier<SkeletonEntity>().setName("Giant Skeleton")
                .setStack(MAINHAND, GIANT_SKELETON_BOW)
                .setStack(HEAD, Items.IRON_HELMET)
                .setStack(CHEST, GIANT_CHESTPLATE).setDropChance(0.2f)
                .setStack(LEGS, Items.IRON_LEGGINGS)
                .setStack(FEET, Items.IRON_BOOTS)
                .setMaxHealth(35)
                .setAbilities(IGNORE_FIRE, IGNORE_SUFFOCATION, SKELETON_IGNORE_SNOW));
        SKELETONS.addModifier(40, new Modifier<SkeletonEntity>().setName("Skelepant")
                .setStack(MAINHAND, SKELEPANT_HANDPANT).setDropChance(0.2f)
                .setStack(OFFHAND, SKELEPANT_OFFHANDPANT).setDropChance(0.08f)
                .setStack(HEAD, SKELEPANT_HEADPANT).setDropChance(0.2f)
                .setMaxHealth(35)
                .modAttribute(MOVEMENT_SPEED, MUL, 1.3)
                .baseAttribute(WATER_MOVEMENT_EFFICIENCY, 4)
                .baseAttribute(SCALE, 0.75)
                .setAbilities(IGNORE_FIRE, SKELETON_IGNORE_SNOW));
        SKELETONS.addModifier(40, new Modifier<SkeletonEntity>().setName("Strange Skeleton")
                .setStack(MAINHAND, STRANGE_SKELETON_CROSSBOW).setDropChance(0.1f)
                .setStack(HEAD, STRANGE_SKELETON_HAT).setDropChance(0.03f)
                .setStack(FEET, Items.IRON_BOOTS)
                .setMaxHealth(35)
                .setAbilities(IGNORE_FIRE, SKELETON_IGNORE_SNOW));
        SKELETONS.addModifier(20, new Modifier<SkeletonEntity>().setName("Creature")
                .setStack(HEAD, Items.WITHER_SKELETON_SKULL)
                .setStack(MAINHAND, CREATURE_CROSSBOW)
                .setStack(LEGS, CREATURE_PANT).setDropChance(0.2f)
                .setStack(FEET, CREATURE_CROSSBOW_DROPPED).setDropChance(0.2f)
                .setMaxHealth(160)
                .baseAttribute(ARMOR, 4)
                .baseAttribute(ARMOR_TOUGHNESS, 4)
                .setAbilities(IGNORE_FIRE, IGNORE_WATER, SKELETON_IGNORE_SNOW, ELITE)
                .setLootTable("entities/custom/skeleton_elite"));
        SKELETONS.addModifier(20, new Modifier<SkeletonEntity>().setName("Daedalus")
                .setStack(HEAD, DAEDALUS_HELMET).setDropChance(0.12f)
                .setStack(MAINHAND, DAEDALUS_STORMBOW)
                .setStack(LEGS, DAEDALUS_STORMBOW_DROPPED).setDropChance(0.12f)
                .setStack(FEET, DAEDALUS_BOOTS).setDropChance(0.12f)
                .setMaxHealth(90)
                .baseAttribute(ARMOR, 8)
                .baseAttribute(ARMOR_TOUGHNESS, 4)
                .baseAttribute(SCALE, 0.55)
                .modAttribute(MOVEMENT_SPEED, MUL,1.1)
                .setAbilities(IGNORE_FIRE, IGNORE_WATER, SKELETON_IGNORE_SNOW, ELITE)
                .setLootTable("entities/custom/skeleton_elite"));
        SKELETONS.addModifier(20, new Modifier<SkeletonEntity>().setName("Potioneer")
                .setStack(HEAD, Items.TURTLE_HELMET)
                .setStack(MAINHAND, POTIONEER_CROSSBOW).setDropChance(0.12f)
                .setStack(OFFHAND, POTIONEER_POTION, 2)
                .setStack(CHEST, POTIONEER_CHESTPLATE).setDropChance(0.12f)
                .setStack(LEGS, POTIONEER_CROSSBOW_DROPPED).setDropChance(0.12f)
                .setMaxHealth(60)
                .baseAttribute(ARMOR, 10)
                .baseAttribute(ARMOR_TOUGHNESS, 4)
                .baseAttribute(SCALE, 0.5)
                .modAttribute(MOVEMENT_SPEED, MUL,1.2)
                .setAbilities(IGNORE_FIRE, SKELETON_IGNORE_SNOW, ELITE)
                .setLootTable("entities/custom/skeleton_elite"));
        SKELETONS.addModifier(20, new Modifier<SkeletonEntity>().setName("Skelatom")
                .setStack(MAINHAND, SKELATOM_BOW).setDropChance(0.15f)
                .setStack(OFFHAND, SKELATOM_BONE_ZONE).setDropChance(0.15f)
                .setStack(FEET, SKELATOM_PLATE).setDropChance(0.15f)
                .setMaxHealth(60)
                .modAttribute(MOVEMENT_SPEED, MUL, 2.5)
                .modAttribute(STEP_HEIGHT, ADD, 0.5)
                .baseAttribute(SCALE, 0.2)
                .setAbilities(IGNORE_FIRE, SKELETON_IGNORE_SNOW, ELITE)
                .setLootTable("entities/custom/skeleton_elite"));
        SKELETONS.addModifier(20, new Modifier<SkeletonEntity>().setName("Thunderbone")
                .setStack(MAINHAND, THUNDER_SKELETON_BOW).setDropChance(0.04f)
                .setStack(HEAD, Items.RAW_COPPER_BLOCK).setDropChance(1)
                .setStack(CHEST, THUNDER_SKELETON_CHESTPLATE).setDropChance(0.25f)
                .setStack(FEET, Items.IRON_BOOTS)
                .setMaxHealth(80)
                .baseAttribute(ARMOR, -10)
                .baseAttribute(ARMOR_TOUGHNESS, -10)
                .setAbilities(IGNORE_FIRE, IGNORE_LIGHTNING, SKELETON_IGNORE_SNOW, ELITE)
                .setLootTable("entities/custom/skeleton_elite"));
        SKELETONS.addModifier(20, new Modifier<SkeletonEntity>().setName("Undead Pillager")
                .setStack(HEAD, UNDEAD_PILLAR_BANNER).setDropChance(1)
                .setStack(MAINHAND, UNDEAD_PILLAGER_CROSSBOW)
                .setStack(CHEST, UNDEAD_PILLAGER_VEST).setDropChance(0.25f)
                .setStack(FEET, UNDEAD_PILLAGER_CROSSBOW_DROPPED).setDropChance(0.2f)
                .setMaxHealth(70)
                .baseAttribute(ARMOR, 4)
                .baseAttribute(ARMOR_TOUGHNESS, 4)
                .baseAttribute(SCALE, 0.75)
                .setAbilities(IGNORE_FIRE, SKELETON_IGNORE_SNOW, ELITE)
                .setLootTable("entities/custom/skeleton_elite"));
        SKELETONS.addModifier(12500 - 240, noOp());
        EpicMod.LOGGER.info("Loaded skeleton spawn pools: {}", SKELETONS);
    }

}
