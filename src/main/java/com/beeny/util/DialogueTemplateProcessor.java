package com.beeny.util;

import com.beeny.Villagersreborn;
import com.beeny.data.VillagerData;
import com.beeny.system.VillagerDialogueSystem;
import com.beeny.system.VillagerScheduleManager;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class DialogueTemplateProcessor {
    
    private static final String[] DEFAULT_LOCATIONS = {
        "old oak tree", "village well", "market square", "forest edge", "river bend"
    };
    
    /**
     * Processes a dialogue template by replacing placeholder tokens with contextual values
     */
    public static String processTemplate(String template, VillagerDialogueSystem.DialogueContext context) {
        if (template == null || context == null) {
            return template;
        }
        
        Map<String, String> replacements = buildReplacementMap(context);
        return applyReplacements(template, replacements);
    }
    
    /**
     * Builds a map of template tokens to their replacement values
     */
    private static Map<String, String> buildReplacementMap(VillagerDialogueSystem.DialogueContext context) {
        Map<String, String> replacements = new HashMap<>();
        
        // Basic context replacements
        addBasicReplacements(replacements, context);
        
        // Activity replacement
        addActivityReplacement(replacements, context);
        
        // Hobby replacement
        addHobbyReplacement(replacements, context);
        
        // Family replacements
        addFamilyReplacements(replacements, context);
        
        // Nearby villager replacements
        addVillagerReplacements(replacements, context);
        
        // Location replacement
        addLocationReplacement(replacements);
        
        return replacements;
    }
    
    private static void addBasicReplacements(Map<String, String> replacements, VillagerDialogueSystem.DialogueContext context) {
        replacements.put("{player}", context.player.getName().getString());
        replacements.put("{timeOfDay}", context.timeOfDay.name.toLowerCase());
        replacements.put("{weather}", context.weather);
        replacements.put("{biome}", formatBiomeName(context.worldContextInfo.biome));
        replacements.put("{profession}", "villager");
    }
    
    private static void addActivityReplacement(Map<String, String> replacements, VillagerDialogueSystem.DialogueContext context) {
        VillagerScheduleManager.Activity currentActivity = VillagerScheduleManager.getCurrentActivity(context.villager);
        replacements.put("{activity}", currentActivity.description.toLowerCase());
    }
    
    private static void addHobbyReplacement(Map<String, String> replacements, VillagerDialogueSystem.DialogueContext context) {
        if (context.villagerData != null) {
            replacements.put("{hobby}", context.villagerData.getHobby().name().toLowerCase());
        } else {
            replacements.put("{hobby}", "exploring");
        }
    }
    
    private static void addFamilyReplacements(Map<String, String> replacements, VillagerDialogueSystem.DialogueContext context) {
        if (context.villagerData == null) {
            replacements.put("{familyMember}", "family");
            return;
        }
        
        if (!context.villagerData.getSpouseName().isEmpty()) {
            replacements.put("{spouse}", context.villagerData.getSpouseName());
            replacements.put("{familyMember}", context.villagerData.getSpouseName());
        } else if (!context.villagerData.getChildrenNames().isEmpty()) {
            String childName = context.villagerData.getChildrenNames().get(0);
            replacements.put("{child}", childName);
            replacements.put("{familyMember}", "child " + childName);
        } else {
            replacements.put("{familyMember}", "family");
        }
    }
    
    private static void addVillagerReplacements(Map<String, String> replacements, VillagerDialogueSystem.DialogueContext context) {
        List<VillagerEntity> nearbyVillagers = context.villager.getWorld().getEntitiesByClass(
            VillagerEntity.class,
            BoundingBoxUtils.fromBlocks(context.villager.getBlockPos(), context.villager.getBlockPos()).expand(20),
            v -> v != context.villager && v.isAlive()
        );
        
        if (!nearbyVillagers.isEmpty()) {
            VillagerEntity randomVillager1 = nearbyVillagers.get(ThreadLocalRandom.current().nextInt(nearbyVillagers.size()));
            VillagerData data1 = randomVillager1.getAttached(Villagersreborn.VILLAGER_DATA);
            String villager1Name = SafeValue.getOrElse(
                (data1 != null && !data1.getName().isEmpty()) ? data1.getName() : null,
                () -> "a villager"
            );
            replacements.put("{villager1}", villager1Name);
            
            if (nearbyVillagers.size() > 1) {
                VillagerEntity randomVillager2;
                do {
                    randomVillager2 = nearbyVillagers.get(ThreadLocalRandom.current().nextInt(nearbyVillagers.size()));
                } while (randomVillager2 == randomVillager1);
                
                VillagerData data2 = randomVillager2.getAttached(Villagersreborn.VILLAGER_DATA);
                String villager2Name = (data2 != null && !data2.getName().isEmpty()) ? data2.getName() : "another villager";
                replacements.put("{villager2}", villager2Name);
            } else {
                replacements.put("{villager2}", "someone");
            }
        } else {
            replacements.put("{villager1}", "someone");
            replacements.put("{villager2}", "another villager");
        }
    }
    
    private static void addLocationReplacement(Map<String, String> replacements) {
        String location = DEFAULT_LOCATIONS[ThreadLocalRandom.current().nextInt(DEFAULT_LOCATIONS.length)];
        replacements.put("{location}", location);
    }
    
    /**
     * Applies all replacements to the template string
     */
    private static String applyReplacements(String template, Map<String, String> replacements) {
        String processed = template;
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            processed = processed.replace(entry.getKey(), entry.getValue());
        }
        return processed;
    }
    
    /**
     * Formats biome names to be more readable
     */
    private static String formatBiomeName(String biomeName) {
        if (biomeName == null || biomeName.isEmpty()) {
            return "unknown";
        }
        
        String formatted = biomeName.replace("_", " ").toLowerCase();
        // Capitalize first letter of each word
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : formatted.toCharArray()) {
            if (Character.isWhitespace(c)) {
                capitalizeNext = true;
                result.append(c);
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
    
    /**
     * Creates a custom replacement map for specific use cases
     */
    public static Map<String, String> createCustomReplacements() {
        return new HashMap<>();
    }
    
    /**
     * Processes a template with additional custom replacements
     */
    public static String processTemplateWithCustom(String template, VillagerDialogueSystem.DialogueContext context, Map<String, String> customReplacements) {
        Map<String, String> replacements = buildReplacementMap(context);
        replacements.putAll(customReplacements);
        return applyReplacements(template, replacements);
    }
}