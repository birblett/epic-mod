package com.birblett.mixin.extra;

import com.birblett.EpicMod;
import com.birblett.helper.Ability;
import com.birblett.interfaces.AbilityUser;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.item.ArmorStandItem;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import static net.minecraft.entity.attribute.EntityAttributes.*;

@Mixin(ArmorStandItem.class)
public class ArmorStandItemMixin {

    @ModifyExpressionValue(method = "useOnBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityType;create(Lnet/minecraft/server/world/ServerWorld;Ljava/util/function/Consumer;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/SpawnReason;ZZ)Lnet/minecraft/entity/Entity;"))
    private Entity applyDummy(@Nullable Entity original, @Local(argsOnly = true) ItemUsageContext context) {
        Text t;
        if (original instanceof ArmorStandEntity armorStand && (t = context.getStack().getCustomName()) != null &&
                t.asTruncatedString(20).equals("Dummy")) {
            ((AbilityUser) armorStand).addAbilities(Ability.DUMMY);
        }
        return original;
    }

}
