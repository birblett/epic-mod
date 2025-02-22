package com.birblett.mixin.base;

import com.birblett.EpicMod;
import com.birblett.helper.*;
import com.birblett.helper.tracked_values.*;
import com.birblett.interfaces.ServerPlayerEntityInterface;
import com.mojang.authlib.GameProfile;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.common.SyncedClientOptions;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.LinkedHashMap;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends PlayerEntity implements ServerPlayerEntityInterface {

    @Unique private InputManager last = new InputManager(false, false, false, false, false, false, false);
    @Unique private final GunHoe.ReloadManager reloadManager = new GunHoe.ReloadManager((ServerPlayerEntity) (Object) this);
    @Unique private final AttributeManager attributeManager = new AttributeManager((ServerPlayerEntity) (Object) this);
    @Unique private final LinkedHashMap<PlayerTicker.ID, PlayerTicker> tickers = new LinkedHashMap<>();

    public ServerPlayerEntityMixin(World world, BlockPos pos, float yaw, GameProfile gameProfile) {
        super(world, pos, yaw, gameProfile);
    }

    @Override
    public PlayerTicker getTickers(PlayerTicker.ID id) {
        return this.tickers.get(id);
    }

    @Override
    public boolean setReloading(ItemStack i, Item ammo, int reload, int reloadAmount, int capacity) {
        return this.reloadManager.setReload(i, ammo, reload, reloadAmount, capacity);
    }

    @Override
    public boolean isReloading(ItemStack i) {
        return this.reloadManager.isReloading(i);
    }

    @Override
    public void addTickedAttribute(RegistryEntry<EntityAttribute>key, EntityAttributeModifier modifier, int ticks) {
        this.attributeManager.addAttribute(key, modifier, ticks);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(MinecraftServer server, ServerWorld world, GameProfile profile, SyncedClientOptions clientOptions, CallbackInfo ci) {
        ServerPlayerEntity instance = (ServerPlayerEntity) (Object) this;
        this.tickers.put(PlayerTicker.ID.BLINK, new Blink(instance, this.attributeManager));
        this.tickers.put(PlayerTicker.ID.BURST_FIRE, new BurstFire(instance, this.attributeManager));
        this.tickers.put(PlayerTicker.ID.CATALYST, new Catalyst(instance, this.attributeManager));
        this.tickers.put(PlayerTicker.ID.DASH, new Dash(instance, this.attributeManager));
        this.tickers.put(PlayerTicker.ID.DOUBLE_JUMP, new DoubleJump(instance, this.attributeManager));
        this.tickers.put(PlayerTicker.ID.FEATHERWEIGHT, new Featherweight(instance, this.attributeManager));
        this.tickers.put(PlayerTicker.ID.FOCUS, new Focus(instance, this.attributeManager));
        this.tickers.put(PlayerTicker.ID.HOMING, new Homing(instance, this.attributeManager));
        this.tickers.put(PlayerTicker.ID.HOVERING, new Hovering(instance, this.attributeManager));
        this.tickers.put(PlayerTicker.ID.LEAPING, new Leaping(instance, this.attributeManager));
        this.tickers.put(PlayerTicker.ID.ROCKET, new Rocket(instance, this.attributeManager));
        this.tickers.put(PlayerTicker.ID.SLIPSTREAM, new Slipstream(instance, this.attributeManager));
        this.tickers.put(PlayerTicker.ID.WALLCLING, new Wallcling(instance, this.attributeManager));
        this.tickers.put(PlayerTicker.ID.THUNDER_TOME, new ThunderTomeCooldown(instance, this.attributeManager));
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void tickEffects(CallbackInfo ci) {
        this.reloadManager.tick();
        if (this.isOnGround()) {
            this.tickers.values().forEach(PlayerTicker::onGroundTick);
        }
        this.tickers.values().forEach(PlayerTicker::tick);
        this.attributeManager.tick();
    }

    @Inject(method = "setPlayerInput", at = @At("TAIL"))
    private void inputHandler(PlayerInput playerInput, CallbackInfo ci) {
        InputManager pressed = new InputManager(
                playerInput.forward() && !this.last.forward(), playerInput.backward() && !this.last.backward(),
                playerInput.left() && !this.last.left(), playerInput.right() && !this.last.right(), playerInput.jump() &&
                !this.last.jump(), playerInput.sneak() && !this.last.sneak(), playerInput.sprint() && !this.last.sprint()
        );
        this.last = new InputManager(playerInput.forward(), playerInput.backward(), playerInput.left(), playerInput.right(),
                playerInput.jump(), playerInput.sneak(), playerInput.sprint());
        this.tickers.values().forEach(t -> t.onInput(pressed, this.last));
    }

    @Inject(method = "copyFrom", at = @At("HEAD"))
    private void saveSoulboundItems(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo ci) {
        if (!alive) {
            ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;
            PlayerInventory inventory = oldPlayer.getInventory();
            for (int slot = 0; slot < inventory.size(); slot++) {
                if (Util.hasEnchant(inventory.getStack(slot), EpicMod.SOULBOUND, self.getWorld())) {
                    switch (slot) {
                        case 36, 37, 38, 39 -> self.getInventory().armor.set(slot - 36, inventory.getStack(slot));
                        case 40 -> self.getInventory().offHand.set(0, inventory.getStack(40));
                        default -> self.getInventory().insertStack(inventory.getStack(slot));
                    }
                }
            }
        }
    }

}
