package com.birblett.mixin.recipe;

import com.birblett.EpicMod;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.recipe.SmithingTransformRecipe;
import net.minecraft.recipe.input.SmithingRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static net.minecraft.entity.attribute.EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
import static net.minecraft.entity.attribute.EntityAttributeModifier.Operation.ADD_VALUE;
import static net.minecraft.entity.attribute.EntityAttributes.*;

@Mixin(SmithingTransformRecipe.class)
public class SmithingTransformRecipeMixin {

    @Inject(method = "craft(Lnet/minecraft/recipe/input/SmithingRecipeInput;Lnet/minecraft/registry/RegistryWrapper$WrapperLookup;)Lnet/minecraft/item/ItemStack;", at = @At("RETURN"), cancellable = true)
    private void rejectInvalidSpecialRecipe(SmithingRecipeInput smithingRecipeInput, RegistryWrapper.WrapperLookup wrapperLookup, CallbackInfoReturnable<ItemStack> cir, @Local ItemStack stack) {
        NbtComponent nbtComponent;
        if (!stack.isEmpty() && smithingRecipeInput.addition().isOf(Items.PRISMARINE_SHARD) && (smithingRecipeInput.base()
                .isIn(EpicMod.CAN_AUGMENT)) && smithingRecipeInput.template().isOf(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE)) {
            if((nbtComponent = smithingRecipeInput.base().get(DataComponentTypes.CUSTOM_DATA)) != null && nbtComponent.contains("Upgrade")
                || (nbtComponent = smithingRecipeInput.addition().get(DataComponentTypes.CUSTOM_DATA)) == null || !nbtComponent.contains("Augment")) {
                cir.setReturnValue(ItemStack.EMPTY);
                return;
            }
            nbtComponent = smithingRecipeInput.addition().get(DataComponentTypes.CUSTOM_DATA);
            NbtCompound nbt = nbtComponent.getNbt();
            if (stack.get(DataComponentTypes.EQUIPPABLE) != null) {
                EquipmentSlot slot = smithingRecipeInput.base().get(DataComponentTypes.EQUIPPABLE).slot();
                Identifier id = Identifier.of("augment_" + slot.asString().toLowerCase());
                int aug = nbt.getInt("Augment");
                List<Pair<RegistryEntry<EntityAttribute>, EntityAttributeModifier>> attributeList = switch(aug) {
                    default -> List.of(getAttr(stack, ARMOR, id, 2, ADD_VALUE),
                            getAttr(stack, ARMOR_TOUGHNESS, id, 0.5, ADD_VALUE));
                    case 1 -> List.of(getAttr(stack, ATTACK_DAMAGE, id, 1, ADD_VALUE),
                            getAttr(stack, ATTACK_KNOCKBACK, id, 0.1, ADD_MULTIPLIED_TOTAL));
                    case 2 -> List.of(getAttr(stack, ATTACK_SPEED, id, 0.075, ADD_MULTIPLIED_TOTAL),
                            getAttr(stack, BLOCK_BREAK_SPEED, id, 0.05, ADD_MULTIPLIED_TOTAL));
                    case 3 -> List.of(getAttr(stack, BLOCK_INTERACTION_RANGE, id, 0.2, ADD_VALUE),
                            getAttr(stack, ENTITY_INTERACTION_RANGE, id, 0.2, ADD_VALUE));
                    case 4 -> List.of(getAttr(stack, MOVEMENT_SPEED, id, 0.075, ADD_MULTIPLIED_TOTAL),
                            getAttr(stack, STEP_HEIGHT, id, 0.15, ADD_VALUE));
                    case 5 -> List.of(getAttr(stack, JUMP_STRENGTH, id, 0.075, ADD_MULTIPLIED_TOTAL),
                            getAttr(stack, SAFE_FALL_DISTANCE, id, 1, ADD_VALUE));
                    case 6 -> List.of(getAttr(stack, MAX_HEALTH, id, 2, ADD_VALUE),
                            getAttr(stack, OXYGEN_BONUS, id, 0.5, ADD_VALUE),
                            getAttr(stack, KNOCKBACK_RESISTANCE, id, 0.1, ADD_VALUE));
                    case 7 -> List.of(getAttr(stack, MINING_EFFICIENCY, id, 0.1, ADD_MULTIPLIED_TOTAL),
                            getAttr(stack, MOVEMENT_EFFICIENCY, id, 0.5, ADD_VALUE),
                            getAttr(stack, SNEAKING_SPEED, id, 0.07, ADD_VALUE));
                };
                AttributeModifiersComponent.Builder builder = AttributeModifiersComponent.builder();
                AttributeModifiersComponent a = stack.getOrDefault(DataComponentTypes.ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.DEFAULT);
                HashSet<RegistryEntry<EntityAttribute>> e = HashSet.newHashSet(1);
                for (AttributeModifiersComponent.Entry entry : a.modifiers()) {
                    boolean cont = true;
                    for (var pair : attributeList) {
                        if (entry.matches(pair.getLeft(), entry.modifier().id())) {
                            builder.add(pair.getLeft(), pair.getRight(), AttributeModifierSlot.forEquipmentSlot(slot));
                            e.add(pair.getLeft());
                            cont = false;
                            break;
                        }
                    }
                    if (cont) {
                        builder.add(entry.attribute(), entry.modifier(), AttributeModifierSlot.forEquipmentSlot(slot));
                    }
                }
                for (var mod : attributeList) {
                    if (!e.contains(mod.getLeft())) {
                        builder.add(mod.getLeft(), mod.getRight(), AttributeModifierSlot.forEquipmentSlot(slot));
                    }
                }
                stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, builder.build());
                ArrayList<Float> floats = new ArrayList<>();
                ArrayList<Boolean> flags = new ArrayList<>();
                ArrayList<String> strings = new ArrayList<>();
                ArrayList<Integer> ints = new ArrayList<>();
                strings.add("augmented");
                if (stack.get(DataComponentTypes.CUSTOM_MODEL_DATA) instanceof CustomModelDataComponent c) {
                    floats.addAll(c.floats());
                    flags.addAll(c.flags());
                    strings.addAll(c.strings());
                    ints.addAll(c.colors());
                }
                stack.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(floats, flags, strings, ints));
                NbtComponent n = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(new NbtCompound()));
                NbtCompound nbtCompound = n.copyNbt();
                nbtCompound.putInt("Upgrade", aug);
                stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbtCompound));
            }
        } else if (!stack.isEmpty() && smithingRecipeInput.addition().getItem().equals(Items.NETHERITE_INGOT) && smithingRecipeInput.base()
                .get(DataComponentTypes.EQUIPPABLE) != null && smithingRecipeInput.template().getItem()
                .equals(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE)) {
            AttributeModifiersComponent c = stack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
            if (c != null) {
                AttributeModifiersComponent.Builder builder = AttributeModifiersComponent.builder();
                for (AttributeModifiersComponent.Entry entry : c.modifiers()) {
                    EntityAttributeModifier newModifier = entry.modifier();
                    if (entry.attribute().equals(ARMOR)) {
                         newModifier = new EntityAttributeModifier(entry.modifier().id(), Math.max(8,
                                entry.modifier().value()), entry.modifier().operation());
                    } else if (entry.attribute().equals(ARMOR_TOUGHNESS)) {
                        newModifier = new EntityAttributeModifier(entry.modifier().id(), Math.max(3,
                                entry.modifier().value()), entry.modifier().operation());
                    }
                    builder.add(entry.attribute(), newModifier, entry.slot());
                }
                stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, builder.build());
            }
        }
    }

    private static List<Pair<RegistryEntry<EntityAttribute>, EntityAttributeModifier>> getSingle(ItemStack stack, RegistryEntry<EntityAttribute> e, Identifier id, double value, EntityAttributeModifier.Operation op) {
        return List.of(getAttr(stack, e, id, value, op));
    }

    private static Pair<RegistryEntry<EntityAttribute>, EntityAttributeModifier> getAttr(ItemStack stack, RegistryEntry<EntityAttribute> e, Identifier id, double value, EntityAttributeModifier.Operation op) {
        AttributeModifiersComponent a = stack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
        if (a != null) {
            for (AttributeModifiersComponent.Entry entry : a.modifiers()) {
                if (entry.attribute() == e) {
                    EntityAttributeModifier m = entry.modifier();
                    return new Pair<>(e, new EntityAttributeModifier(m.id(), op == ADD_VALUE ? m.value() + value : m.value() * (1 + value), m.operation()));
                }
            }
        }
        return new Pair<>(e, new EntityAttributeModifier(id, value, op));
    }

}
