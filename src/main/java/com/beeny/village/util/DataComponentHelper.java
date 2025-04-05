package com.beeny.village.util;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.FireworksComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.item.FireworkRocketItem;
import net.minecraft.potion.Potion;
import net.minecraft.registry.RegistryWrapper;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;

public class DataComponentHelper {
    
    private static RegistryWrapper.WrapperLookup wrapperLookup = null;
    
    public static void setWrapperLookup(RegistryWrapper.WrapperLookup lookup) {
        wrapperLookup = lookup;
    }
    
    public static void setCustomData(ItemStack stack, Map<String, Object> data) {
        if (wrapperLookup == null) {
            LOGGER.error("WrapperLookup is null! Cannot proceed.");
            return;
        }
        NbtCompound nbt = new NbtCompound();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (entry.getValue() instanceof String) {
                nbt.putString(entry.getKey(), (String)entry.getValue());
            } else if (entry.getValue() instanceof Integer) {
                nbt.putInt(entry.getKey(), (Integer)entry.getValue());
            } else if (entry.getValue() instanceof Float) {
                nbt.putFloat(entry.getKey(), (Float)entry.getValue());
            } else if (entry.getValue() instanceof Boolean) {
                nbt.putBoolean(entry.getKey(), (Boolean)entry.getValue());
            } else if (entry.getValue() instanceof Long) {
                nbt.putLong(entry.getKey(), (Long)entry.getValue());
            }
        }
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }
    
    public static void setLore(ItemStack stack, List<Text> lore) {
        if (wrapperLookup == null) {
            LOGGER.error("WrapperLookup is null! Cannot proceed.");
            return;
        }
        stack.set(DataComponentTypes.LORE, new LoreComponent(lore));
    }

    public static void setEnchantments(ItemStack stack, Map<Enchantment, Integer> enchantments) {
        if (wrapperLookup == null) {
            LOGGER.error("WrapperLookup is null! Cannot proceed.");
            return;
        }
        ItemEnchantmentsComponent.Builder builder = ItemEnchantmentsComponent.builder();
        enchantments.forEach((enchant, level) -> {
            RegistryKey<Enchantment> registryKey = RegistryKey.of(
                RegistryKeys.ENCHANTMENT, 
                Registries.ENCHANTMENT.getId(enchant)
            );
            Optional<RegistryEntry.Reference<Enchantment>> enchantRef = 
                wrapperLookup.getWrapperOrThrow(RegistryKeys.ENCHANTMENT).getOptional(registryKey);
            
            enchantRef.ifPresent(ref -> builder.add(ref, level));
        });
        stack.set(DataComponentTypes.ENCHANTMENTS, builder.build());
    }

    public static void setFireworkEffect(ItemStack stack, byte flight, int[] colors) {
        if (wrapperLookup == null) {
            LOGGER.error("WrapperLookup is null! Cannot proceed.");
            return;
        }
        FireworksComponent.Builder builder = new FireworksComponent.Builder()
            .flight(flight);
        
        List<FireworksComponent.Explosion> explosions = new ArrayList<>();
        FireworksComponent.Explosion.Builder explosionBuilder = 
            new FireworksComponent.Explosion.Builder()
                .type(FireworksComponent.Explosion.Type.LARGE_BALL);
        
        for (int color : colors) {
            explosionBuilder.addColor(color);
        }
        
        explosions.add(explosionBuilder.build());
        builder.explosions(explosions);
        
        stack.set(DataComponentTypes.FIREWORKS, builder.build());
    }

    public static void setPotionEffect(ItemStack stack, String potionId) {
        if (wrapperLookup == null) {
            LOGGER.error("WrapperLookup is null! Cannot proceed.");
            return;
        }
        Identifier potionIdentifier = Identifier.of(potionId);
        RegistryKey<Potion> potionKey = RegistryKey.of(RegistryKeys.POTION, potionIdentifier);
        
        Optional<RegistryEntry.Reference<Potion>> potionRef = 
            wrapperLookup.getWrapperOrThrow(RegistryKeys.POTION).getOptional(potionKey);
        
        potionRef.ifPresent(ref -> {
            PotionContentsComponent potionContents = new PotionContentsComponent(ref);
            stack.set(DataComponentTypes.POTION_CONTENTS, potionContents);
        });
    }

    public static NbtCompound getCustomData(ItemStack stack) {
        Optional<NbtComponent> customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        return customData.map(NbtComponent::copyNbt).orElse(new NbtCompound());
    }
}