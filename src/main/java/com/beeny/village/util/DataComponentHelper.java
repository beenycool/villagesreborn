package com.beeny.village.util;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.FireworksComponent;
import net.minecraft.component.type.FireworkExplosionComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.item.FireworkRocketItem;
import net.minecraft.potion.Potion;
import net.minecraft.registry.RegistryWrapper;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;

public class DataComponentHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataComponentHelper.class);
    
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

    // Renamed and modified to accept String IDs
    public static void setEnchantmentsById(ItemStack stack, Map<String, Integer> enchantmentIds) {
        if (wrapperLookup == null) {
            LOGGER.error("WrapperLookup is null! Cannot set enchantments by ID.");
            return;
        }
        ItemEnchantmentsComponent.Builder builder = new ItemEnchantmentsComponent.Builder(ItemEnchantmentsComponent.DEFAULT);

        enchantmentIds.forEach((idStr, level) -> {
            Identifier id = Identifier.tryParse(idStr);
            if (id == null) {
                LOGGER.warn("Invalid enchantment identifier string: {}", idStr);
                return; // Skip invalid IDs
            }

            RegistryKey<Enchantment> key = RegistryKey.of(RegistryKeys.ENCHANTMENT, id);

            // Use flatMap pattern similar to setPotionEffect to get the entry reference
            Optional<RegistryEntry.Reference<Enchantment>> enchantRefOpt =
                wrapperLookup.getOptional(RegistryKeys.ENCHANTMENT)
                             .flatMap(wrapper -> wrapper.getOptional(key));

            enchantRefOpt.ifPresentOrElse(
                ref -> builder.add(ref, level), // Add if present
                () -> LOGGER.warn("Could not find registry entry reference for enchantment key: {}", key)
            );
        });
        ItemEnchantmentsComponent finalEnchantments = builder.build();
        // Only apply if the built component is different from the default (meaning enchantments were added)
        if (!finalEnchantments.equals(ItemEnchantmentsComponent.DEFAULT)) {
             stack.set(DataComponentTypes.ENCHANTMENTS, finalEnchantments);
        } else if (!enchantmentIds.isEmpty()) {
            LOGGER.warn("Attempted to set enchantments by ID, but none were resolved or added: {}", enchantmentIds.keySet());
        }
    }

    public static void setFireworkEffect(ItemStack stack, byte flight, int[] colors) {
        if (wrapperLookup == null) {
            LOGGER.error("WrapperLookup is null! Cannot proceed.");
            return;
        }
        // Create the explosion component directly
        FireworkExplosionComponent explosion = new FireworkExplosionComponent(
            FireworkExplosionComponent.Type.LARGE_BALL, // Shape (Assuming Shape was renamed to Type)
            new IntArrayList(colors), // Colors (converted to IntList)
            new IntArrayList(),       // Fade Colors (empty IntList)
            false,           // Twinkle
            false            // Trail
        );

        // Create the fireworks component with flight duration and the explosion
        FireworksComponent fireworks = new FireworksComponent(
            flight,          // Flight duration
            List.of(explosion) // List of explosions
        );
        
        stack.set(DataComponentTypes.FIREWORKS, fireworks);
    }

    public static void setPotionEffect(ItemStack stack, String potionId) {
        if (wrapperLookup == null) {
            LOGGER.error("WrapperLookup is null! Cannot proceed.");
            return;
        }
        Identifier potionIdentifier = Identifier.of(potionId);
        RegistryKey<Potion> potionKey = RegistryKey.of(RegistryKeys.POTION, potionIdentifier);
        
        Optional<RegistryEntry.Reference<Potion>> potionRef = 
            wrapperLookup.getOptional(RegistryKeys.POTION).flatMap(wrapper -> wrapper.getOptional(potionKey)); // Use getOptional and flatMap
        
        potionRef.ifPresent(ref -> {
            PotionContentsComponent potionContents = new PotionContentsComponent(ref);
            stack.set(DataComponentTypes.POTION_CONTENTS, potionContents);
        });
    }

    public static NbtCompound getCustomData(ItemStack stack) {
        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA); // Get component directly
        return customData != null ? customData.copyNbt() : new NbtCompound(); // Check for null and return NBT or new compound
    }
}