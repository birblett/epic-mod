package com.birblett.mixin.base;

import com.birblett.helper.Ability;
import com.birblett.interfaces.AbilityUser;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;
import java.util.HashSet;

@Mixin(Entity.class)
public class EntityMixin implements AbilityUser {

    @Unique
    HashSet<Ability> abilities = null;

    @Override
    public void addAbilities(Ability... abilities) {
        if (this.abilities == null) {
            this.abilities = new HashSet<>();
        }
        this.abilities.addAll(Arrays.asList(abilities));
    }

    @Override
    public boolean hasAbility(Ability ability) {
        return this.abilities != null && this.abilities.contains(ability);
    }

    @Override
    public boolean hasAbilities() {
        return this.abilities != null && !this.abilities.isEmpty();
    }

    @Override
    public Ability[] getAbilities() {
        return this.abilities == null ? new Ability[]{} : this.abilities.toArray(new Ability[]{});
    }

    @Inject(method = "writeNbt", at = @At("TAIL"))
    private void writeAbilityNbt(NbtCompound nbt, CallbackInfoReturnable<NbtCompound> cir) {
        if (this.abilities != null) {
            NbtCompound compound = new NbtCompound();
            for (Ability j : this.abilities) {
                compound.putBoolean(j.name(), true);
            }
            nbt.put("EpicAbilities", compound);
        }
    }

    @Inject(method = "readNbt", at = @At("TAIL"))
    private void readAbilityNbt(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.contains("EpicAbilities")) {
        NbtCompound compound = nbt.getCompound("EpicAbilities");
            this.abilities = new HashSet<>();
            for (String keys : compound.getKeys()) {
                try {
                    this.abilities.add(Ability.valueOf(keys));
                } catch(Exception ignored) {}
            }
            this.updateGoals();
        }
    }

}
