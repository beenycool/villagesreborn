package com.beeny.util;

import com.beeny.data.VillagerData;
import com.beeny.constants.VillagerConstants;
import net.minecraft.entity.passive.VillagerEntity;
import org.jetbrains.annotations.NotNull;

/**
 * Utility class for villager personality-related operations.
 * Centralizes personality access patterns to eliminate code duplication.
 */
public class VillagerPersonalityUtils {
    
    /**
     * Gets the personality string for a villager.
     * 
     * @param villager The villager entity
     * @return The personality type as a string, defaults to "Friendly" if not available
     */
    @NotNull
    public static String getPersonality(@NotNull VillagerEntity villager) {
        VillagerData data = villager.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
        return data != null ? VillagerConstants.PersonalityType.toString(data.getPersonality()) : "Friendly";
    }
    
    /**
     * Gets the personality modifier for social actions based on personality type.
     * 
     * @param personality The personality string
     * @return Modifier value for social interactions
     */
    public static float getSocialModifier(@NotNull String personality) {
        return switch (personality) {
            case "Friendly", "Cheerful" -> 1.5f;
            case "Shy" -> 0.6f;
            case "Grumpy" -> 0.7f;
            case "Curious" -> 1.3f;
            default -> 1.0f;
        };
    }
    
    /**
     * Gets the personality modifier for work-related actions.
     * 
     * @param personality The personality string
     * @return Modifier value for work activities
     */
    public static float getWorkModifier(@NotNull String personality) {
        return switch (personality) {
            case "Energetic", "Serious" -> 1.3f;
            case "Lazy" -> 0.5f;
            default -> 1.0f;
        };
    }
    
    /**
     * Gets cost modifier for actions that are personality-dependent.
     * 
     * @param personality The personality string
     * @param actionType The type of action (e.g., "social", "work", "gossip")
     * @return Cost modifier for the action
     */
    public static float getActionCostModifier(@NotNull String personality, @NotNull String actionType) {
        return switch (actionType.toLowerCase()) {
            case "social" -> switch (personality) {
                case "Shy" -> 2.0f;
                case "Friendly", "Cheerful" -> 0.5f;
                default -> 1.0f;
            };
            case "work" -> switch (personality) {
                case "Lazy" -> 3.0f;
                case "Energetic", "Serious" -> 0.7f;
                default -> 1.0f;
            };
            case "gossip" -> switch (personality) {
                case "Shy" -> 2.5f;
                case "Curious", "Friendly" -> 0.5f;
                default -> 1.0f;
            };
            default -> 1.0f;
        };
    }
}