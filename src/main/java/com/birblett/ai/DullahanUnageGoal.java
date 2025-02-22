package com.birblett.ai;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.mob.ZombieHorseEntity;

public class DullahanUnageGoal extends Goal {

    private final ZombieEntity zombie;

    public DullahanUnageGoal(ZombieEntity zombie) {
        this.zombie = zombie;
    }

    @Override
    public boolean canStart() {
        return this.zombie.hasVehicle();
    }

    @Override
    public void tick() {
        if (this.zombie.getVehicle() instanceof ZombieHorseEntity z) {
            z.setBreedingAge(-25000);
        }
    }

}
