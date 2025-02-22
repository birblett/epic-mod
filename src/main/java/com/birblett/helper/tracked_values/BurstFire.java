package com.birblett.helper.tracked_values;

import com.birblett.helper.AttributeManager;
import com.birblett.helper.PlayerTicker;
import com.birblett.mixin.base.RangedWeaponAccessor;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;

import java.util.List;

public class BurstFire extends PlayerTicker {

    private ItemStack trackedStack = ItemStack.EMPTY;
    private List<ItemStack> ammo = List.of();
    private Hand hand = Hand.MAIN_HAND;
    private int progress;
    private float speed;
    private float divergence;

    public BurstFire(ServerPlayerEntity player, AttributeManager attributeManager) {
        super(player, attributeManager);
    }

    @Override
    public void tick() {
        if (this.progress > 0) {
            if (this.player.getStackInHand(this.hand) != this.trackedStack) {
                this.progress = 0;
            } else {
                if ((this.progress - 1) % 4 == 0 && this.trackedStack.getItem() instanceof CrossbowItem crossbowItem) {
                    ((RangedWeaponAccessor) crossbowItem).crossbowShootAll(this.world(), this.player, this.hand, this.trackedStack,
                            this.ammo, this.speed, this.divergence, true, null);
                }
                --this.progress;
            }
        }
    }

    @Override
    public void setUsing(ItemStack stack, Hand hand, int value) {
        this.trackedStack = stack;
        this.hand = hand;
        this.progress = value;
    }

    public void setShooting(List<ItemStack> stack, float speed, float divergence) {
        this.ammo = stack;
        this.speed = speed;
        this.divergence = divergence;
    }

}
