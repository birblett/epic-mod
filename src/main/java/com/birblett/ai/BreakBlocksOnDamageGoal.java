package com.birblett.ai;

import com.birblett.helper.Util;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.MobEntity;

public class BreakBlocksOnDamageGoal extends Goal {

    private final MobEntity mob;
    private LivingEntity target = null;
    private int cooldownTicks = 20;
    private int hitsLeft = 6;
    private final int bx;
    private final int by;

    public BreakBlocksOnDamageGoal(MobEntity mob, int bx, int by) {
        this.bx = bx;
        this.by = by;
        this.mob = mob;
    }

    public BreakBlocksOnDamageGoal(MobEntity mob) {
        this(mob, 0, 0);
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }

    @Override
    public boolean canStart() {
        if (this.cooldownTicks > 0) {
            --this.cooldownTicks;
        } else if (this.mob.getTarget() != null && this.mob.hurtTime == 9) {
            if (this.hitsLeft > 0) {
                --this.hitsLeft;
                this.cooldownTicks = 10;
            } else {
                Util.mobBreakBlocks(this.mob, this.bx, this.by);
                this.hitsLeft = 6;
                this.cooldownTicks = 20;
            }
        }
        return false;
    }

}
