package com.birblett.mixin.base;

import com.birblett.interfaces.ItemSpoofer;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.c2s.play.ClientTickEndC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.SetPlayerInventoryS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerCommonNetworkHandler;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin extends ServerCommonNetworkHandler implements ItemSpoofer {

    @Shadow public ServerPlayerEntity player;
    @Unique
    private boolean usedOffhand;
    @Unique
    private int spoofTick = 0;
    @Unique
    private ItemStack spoofed;

    public ServerPlayNetworkHandlerMixin(MinecraftServer server, ClientConnection connection, ConnectedClientData clientData) {
        super(server, connection, clientData);
    }

    @Override
    public void mainHand() {
        ItemStack stack = this.player.getOffHandStack();
        if (!stack.isEmpty()) {
            ItemStack stack2 = Items.WOODEN_SWORD.getDefaultStack();
            stack2.set(DataComponentTypes.ITEM_MODEL, Registries.ITEM.getId((this.spoofed = stack).getItem()));
            stack2.set(DataComponentTypes.ENCHANTMENTS, stack.get(DataComponentTypes.ENCHANTMENTS));
            stack2.set(DataComponentTypes.DAMAGE, stack.get(DataComponentTypes.DAMAGE));
            stack2.set(DataComponentTypes.MAX_DAMAGE, stack.get(DataComponentTypes.MAX_DAMAGE));
            stack2.set(DataComponentTypes.MAX_STACK_SIZE, stack.get(DataComponentTypes.MAX_STACK_SIZE));
            stack2.setCount(stack.getCount());
            this.player.networkHandler.sendPacket(new SetPlayerInventoryS2CPacket(40, stack2));
            this.player.networkHandler.sendPacket(new ScreenHandlerSlotUpdateS2CPacket(this.player.playerScreenHandler.syncId,
                    this.player.playerScreenHandler.nextRevision(), this.player.getInventory().selectedSlot,
                    this.player.getInventory().getStack(this.player.getInventory().selectedSlot)));
            this.spoofTick = 4;
        }
    }

    @ModifyExpressionValue(method = "onPlayerInteractItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayerInteractionManager;interactItem(Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/world/World;Lnet/minecraft/item/ItemStack;Lnet/minecraft/util/Hand;)Lnet/minecraft/util/ActionResult;"))
    private ActionResult captureItemInteraction(ActionResult original, @Local ItemStack stack, @Local Hand hand, @Local ServerWorld world) {
        this.usedOffhand = hand == Hand.OFF_HAND;
        return original;
    }

    @Inject(method = "onClientTickEnd", at = @At("TAIL"))
    private void updateClientActions(ClientTickEndC2SPacket packet, CallbackInfo ci) {
        if (this.usedOffhand && this.spoofed != null) {
            TrackedData<Byte> f = LivingEntityAccessor.getLIVING_FLAGS();
            this.sendPacket(new EntityTrackerUpdateS2CPacket(this.player.getId(), List.of(DataTracker.SerializedEntry.of(f, this.player.getDataTracker().get(f)))));
        }
        this.usedOffhand = false;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void removeSpoof(CallbackInfo ci) {
        if (this.spoofed != null && this.spoofTick-- <= 0) {
            this.player.networkHandler.sendPacket(new SetPlayerInventoryS2CPacket(40, this.spoofed));
            this.spoofed = null;
        }
    }

}
