package com.birblett.ai;

import net.minecraft.entity.ai.goal.Goal;

public class BotSummoningGoal extends Goal {


    @Override
    public boolean canStart() {
        return false;
    }
}
