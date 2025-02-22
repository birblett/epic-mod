package com.birblett.helper;

import com.birblett.interfaces.ServerPlayerEntityInterface;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.dragon.EnderDragonPart;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Set;
import java.util.function.BiFunction;

import static com.birblett.helper.GunHoe.ReloadManager.AMMO_KEY;

public class GunHoe {

    public static HashMap<RegistryKey<Enchantment>, BiFunction<ServerPlayerEntity, ItemStack, Integer>> GUN_HOES = new HashMap<>();
    private static final Identifier GUN_HOE_ID = Identifier.of("gun_hoe_recoil");

    static {
        GUN_HOES.put(gunHoeKey("auto"), ((player, stack) -> {
            if (!((ServerPlayerEntityInterface) player).isReloading(stack) && GunHoe.getAmmo(stack) > 0) {
                ServerWorld world = player.getServerWorld();
                Util.shootProjectile(world, player, Items.GOLD_NUGGET, 5, player.getRotationVector(), 1.5, 0.05,
                        36, true);
                Util.playSound(world, player, SoundEvents.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 2.0f, 1.8f);
                Util.playSound(world, player, SoundEvents.ENTITY_WITHER_BREAK_BLOCK, 0.2f, 2.0f);
                Util.sendActionBarMessage(player, removeAmmo(stack, 20) + "/" + 20);
                applyRecoil(player, 0.4, 8, 0.06);
                return 3;
            } else {
                boolean b = ((ServerPlayerEntityInterface) player).setReloading(stack, Items.GOLD_NUGGET, 60, 20, 20);
                return b ? 0 : -1;
            }
        }));
        GUN_HOES.put(gunHoeKey("bolt"), ((player, stack) -> {
            if (!((ServerPlayerEntityInterface) player).isReloading(stack) && GunHoe.getAmmo(stack) > 0) {
                ServerWorld world = player.getServerWorld();
                Util.damagingRaycast(world, player, player.getEyePos(), player.getRotationVector().multiply(0.125),
                        480, 0.1, 20, ParticleTypes.CRIT, false, true);
                Util.playSound(world, player, SoundEvents.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 0.6f, 2.0f);
                Util.playSound(world, player, SoundEvents.ITEM_TOTEM_USE, 0.35f, 2.0f);
                Util.playSound(world, player, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 2.0f, 2.0f);
                Util.playSound(world, player, SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT, 1.0f, 1.8f);
                Util.sendActionBarMessage(player, removeAmmo(stack, 5) + "/" + 5);
                applyRecoil(player, 0.75, 15, 0);
                return 35;
            } else {
                boolean b = ((ServerPlayerEntityInterface) player).setReloading(stack, Items.IRON_INGOT, 70, 5, 5);
                return b ? 0 : -1;
            }
        }));
        GUN_HOES.put(gunHoeKey("sawed_off"), ((player, stack) -> {
            if (!((ServerPlayerEntityInterface) player).isReloading(stack) && GunHoe.getAmmo(stack) > 0) {
                ServerWorld world = player.getServerWorld();
                for (int i = 0; i < 12; ++i) {
                    Util.shootProjectile(world, player, Items.GOLD_NUGGET, 4, player.getRotationVector(), 1.0,
                            0.2, 14, true);
                }
                Util.playSound(world, player, SoundEvents.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 0.6f, 2.0f);
                Util.playSound(world, player, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 1.0f, 1.5f);
                Util.playSound(world, player, SoundEvents.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 2.0f, 1.4f);
                Util.playSound(world, player, SoundEvents.ENTITY_WITHER_SHOOT, 0.3f, 1.5f);
                Util.playSound(world, player, SoundEvents.ENTITY_SHULKER_BULLET_HIT, 2.0f, 0.4f);
                Util.sendActionBarMessage(player, removeAmmo(stack, 2) + "/" + 2);
                applyRecoil(player, 0.4, 8, 1);
                return 2;
            } else {
                boolean b = ((ServerPlayerEntityInterface) player).setReloading(stack, Items.GOLD_INGOT, 50, 2, 2);
                return b ? 0 : -1;
            }
        }));
        GUN_HOES.put(gunHoeKey("snipe"), ((player, stack) -> {
            if (!((ServerPlayerEntityInterface) player).isReloading(stack) && GunHoe.getAmmo(stack) > 0) {
                ServerWorld world = player.getServerWorld();
                Util.shootProjectile(world, player, Items.IRON_NUGGET, 11, player.getRotationVector(), 1.8, 0.01,
                        45, true);
                Util.playSound(world, player, SoundEvents.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 2.0f, 1.8f);
                Util.playSound(world, player, SoundEvents.ENTITY_WITHER_BREAK_BLOCK, 0.2f, 2.0f);
                Util.sendActionBarMessage(player, removeAmmo(stack, 10) + "/" + 10);
                applyRecoil(player, 0.5, 10, 0);
                return 8;
            } else {
                boolean b = ((ServerPlayerEntityInterface) player).setReloading(stack, Items.IRON_NUGGET, 60, 10, 10);
                return b ? 0 : -1;
            }
        }));
        GUN_HOES.put(gunHoeKey("tactical"), ((player, stack) -> {
            if (!((ServerPlayerEntityInterface) player).isReloading(stack) && GunHoe.getAmmo(stack) > 0) {
                ServerWorld world = player.getServerWorld();
                for (int i = 0; i < 8; ++i) {
                    Util.shootProjectile(world, player, Items.GOLD_NUGGET, 2, player.getRotationVector(), 0.9,
                            0.05, 24, true);
                }
                Util.playSound(world, player, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 0.6f, 1.5f);
                Util.playSound(world, player, SoundEvents.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 2.0f, 1.4f);
                Util.playSound(world, player, SoundEvents.ENTITY_WITHER_SHOOT, 0.2f, 1.5f);
                Util.playSound(world, player, SoundEvents.ENTITY_SHULKER_BULLET_HIT, 2.0f, 0.4f);
                Util.sendActionBarMessage(player, removeAmmo(stack, 9) + "/" + 9);
                applyRecoil(player, 0.2, 4, 0.1);
                return 16;
            } else {
                boolean b = ((ServerPlayerEntityInterface) player).setReloading(stack, Items.GOLD_INGOT, 12, 1, 9);
                return b ? 0 : -1;
            }
        }));
    }

    private static void applyRecoil(ServerPlayerEntity player, double slowdown, int duration, double pushback) {
        ((ServerPlayerEntityInterface) player).addTickedAttribute(EntityAttributes.MOVEMENT_SPEED,
                new EntityAttributeModifier(GUN_HOE_ID, -slowdown, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL), duration);
        ((ServerPlayerEntityInterface) player).addTickedAttribute(EntityAttributes.JUMP_STRENGTH,
                new EntityAttributeModifier(GUN_HOE_ID, -10, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL), duration);
        if (pushback > 0) {
            pushback = player.isSneaking() ? pushback * 0.5 : pushback;
            Vec3d rot = player.getRotationVector().multiply(-pushback);
            player.addVelocity(rot.x, rot.y, rot.z);
            player.velocityModified = true;
        }
    }

    private static RegistryKey<Enchantment> gunHoeKey(String name) {
        return RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.ofVanilla("gun_hoe_" + name));
    }

    public static void updateAmmoDamage(ItemStack stack, int capacity) {
        NbtComponent nbt = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (nbt == null) {
            stack.setDamage(stack.getMaxDamage() - 1);
        } else {
            stack.setDamage((int) ((stack.getMaxDamage() - 1) * (Math.max(0, capacity - nbt.getNbt().getInt(AMMO_KEY))) /
                    (double) capacity));
        }
    }

    private static int getAmmo(ItemStack stack) {
        var comp = stack.get(DataComponentTypes.CUSTOM_DATA);
        return comp == null ? 0 : comp.getNbt().getInt(AMMO_KEY);
    }

    private static int removeAmmo(ItemStack stack, int capacity) {
        var comp = stack.get(DataComponentTypes.CUSTOM_DATA);
        int current;
        if (comp != null && (current = comp.getNbt().getInt(AMMO_KEY)) > 0) {
            comp.getNbt().putInt(AMMO_KEY, current = current - 1);
        } else {
            comp.getNbt().putInt(AMMO_KEY, current = 0);
        }
        updateAmmoDamage(stack, capacity);
        return current;
    }

    public static void applyGunDamage(ServerWorld world, Entity e, float damage, boolean ignoreIframes, DamageSource damageSource) {
        if (e instanceof EnderDragonPart ed) {
            if (ignoreIframes) {
                ((EnderDragonPart) e).owner.hurtTime = 0;
                ((EnderDragonPart) e).owner.timeUntilRegen = 1;
            }
            ((EnderDragonPart) e).owner.damagePart(world, (EnderDragonPart) e, damageSource, damage);
        } else {
            if (ignoreIframes) {
                if (e instanceof LivingEntity) ((LivingEntity) e).hurtTime = 0;
                e.timeUntilRegen = 1;
            }
            e.damage(world, damageSource, damage);
        }
    }

    public static class ReloadManager {

        static final String AMMO_KEY = "GunHoeAmmo";

        private final ServerPlayerEntity player;
        private int reload;
        private int cooldown;
        private int reloadAmount;
        private int capacity;
        private ItemStack reloadingItem;
        private Item ammoType;

        public ReloadManager(ServerPlayerEntity player) {
            this.player = player;
        }

        public boolean isReloading(ItemStack item) {
            return item == this.reloadingItem;
        }

        public boolean setReload(ItemStack reloadingItem, Item ammoType, int reload, int reloadAmount, int capacity) {
            if (this.reloadingItem == reloadingItem) {
                return false;
            }
            if (!this.player.getInventory().containsAny(Set.of(ammoType))) {
                Util.sendActionBarMessage(this.player, "No ammo left!");
                return false;
            }
            this.reloadingItem = reloadingItem;
            this.ammoType = ammoType;
            this.cooldown = this.reload = reload;
            this.reloadAmount = reloadAmount;
            this.capacity = capacity;
            Util.sendActionBarMessage(this.player, "Reloading...");
            return true;
        }

        public void tick() {
            if (this.reloadingItem != null) {
                if (this.player.isHolding(e -> e == this.reloadingItem)) {
                    if (this.cooldown <= 0) {
                        int i;
                        if ((i = Inventories.remove(this.player.getInventory(), it -> it.isOf(this.ammoType), this.reloadAmount, false)) > 0) {
                            if (this.addAmmo(i)) {
                                this.reloadingItem = null;
                                return;
                            }
                            this.cooldown = this.reload;
                        } else {
                            this.reloadingItem = null;
                        }
                    } else {
                        --this.cooldown;
                    }
                } else {
                    this.reloadingItem = null;
                }
            }
        }

        private boolean addAmmo(int max) {
            var comp = this.reloadingItem.get(DataComponentTypes.CUSTOM_DATA);
            int current;
            if (comp == null) {
                NbtCompound tmp = new NbtCompound();
                tmp.putInt(AMMO_KEY, current = Math.min(max, this.reloadAmount));
                this.reloadingItem.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(tmp));
            } else {
                current = comp.getNbt().getInt(AMMO_KEY) + Math.min(max, this.reloadAmount);
                comp.getNbt().putInt(AMMO_KEY, Math.min(current, this.capacity));
            }
            Util.sendActionBarMessage(this.player, current + "/" + this.capacity);
            updateAmmoDamage(this.reloadingItem, this.capacity);
            return current == this.capacity;
        }

    }

}
