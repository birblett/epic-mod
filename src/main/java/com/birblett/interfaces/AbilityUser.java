package com.birblett.interfaces;

import com.birblett.helper.Ability;

public interface AbilityUser {

    void addAbilities(Ability... abilities);
    void removeAbility(Ability a);
    boolean hasAbility(Ability ability);
    boolean hasAbilities();
    Ability[] getAbilities();
    default void updateGoals() {};
    default void setCooldown(int i) {}
    default boolean isCooledDown() { return false; }

}
