package com.beeny.village;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.beeny.ai.LLMService;
import com.beeny.ai.CulturalPromptTemplates;
import net.minecraft.server.world.ServerWorld;

public class CulturalEvolution {
    private static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn");
    private final Map<String, CulturalState> culturalStates = new HashMap<>();
    private final List<CulturalEvent> historicalEvents = new ArrayList<>();
    
    public static class CulturalState {
        private final String baseCulture;
        private final Map<String, Integer> influences = new HashMap<>();
        private final Map<String, String> adaptations = new HashMap<>();
        private int evolutionStage = 0;
        
        public CulturalState(String baseCulture) {
            this.baseCulture = baseCulture;
        }
        
        public void addInfluence(String culture, int strength) {
            influences.merge(culture, strength, Integer::sum);
        }
        
        public void recordAdaptation(String aspect, String description) {
            adaptations.put(aspect, description);
            evolutionStage++;
        }
    }

    public CompletableFuture<String> evolveVillageCulture(String baseCulture, ServerWorld world, List<VillagerAI> villagers) {
        CulturalState state = culturalStates.computeIfAbsent(baseCulture, CulturalState::new);
        
        // Build rich context for cultural evolution
        Map<String, String> context = new HashMap<>();
        context.put("base_culture", baseCulture);
        context.put("evolution_stage", String.valueOf(state.evolutionStage));
        context.put("influences", state.influences.toString());
        context.put("population", String.valueOf(villagers.size()));
        context.put("historical_events", getRecentHistory());
        
        String prompt = String.format(
            "Analyze this Minecraft village's cultural evolution:\n" +
            "Base Culture: %s\n" +
            "Current Stage: %d\n" +
            "Recent Events: %s\n" +
            "Population: %d villagers\n\n" +
            "Suggest cultural adaptations in these areas:\n" +
            "1. Architecture\n" +
            "2. Social Structure\n" +
            "3. Traditions\n" +
            "4. Daily Life\n\n" +
            "Consider environmental factors, villager interactions, and historical events.\n" +
            "Focus on natural, gradual changes that blend existing elements.",
            baseCulture,
            state.evolutionStage,
            getRecentHistory(),
            villagers.size()
        );

        return LLMService.getInstance()
            .generateResponse(prompt, context)
            .thenApply(response -> {
                Map<String, String> adaptations = parseAdaptations(response);
                adaptations.forEach((aspect, description) -> {
                    state.recordAdaptation(aspect, description);
                    applyAdaptation(aspect, description, world, villagers);
                });
                return generateEvolutionSummary(state);
            });
    }

    private void applyAdaptation(String aspect, String description, ServerWorld world, List<VillagerAI> villagers) {
        switch (aspect.toLowerCase()) {
            case "architecture" -> applyArchitecturalChanges(description, world);
            case "social" -> applySocialChanges(description, villagers);
            case "traditions" -> addNewTraditions(description, villagers);
            case "daily_life" -> updateDailyRoutines(description, villagers);
            default -> LOGGER.warn("Unknown adaptation aspect: {}", aspect);
        }
    }

    private void applyArchitecturalChanges(String changes, ServerWorld world) {
        String prompt = CulturalPromptTemplates.getTemplate("building_style", 
            "evolved", "building_modification");
        
        LLMService.getInstance()
            .generateResponse(prompt)
            .thenAccept(response -> {
                Map<String, String> modifications = parseArchitecturalChanges(response);
                // Queue modifications for gradual application
                // This will be implemented in the building system
            });
    }

    private void applySocialChanges(String changes, List<VillagerAI> villagers) {
        villagers.forEach(villager -> {
            String prompt = CulturalPromptTemplates.getTemplate("social_interaction",
                "evolved",
                villager.getPersonality(),
                villager.getCurrentActivity(),
                "unknown",
                "unknown",
                "day",
                "adapting",
                changes);
                
            LLMService.getInstance()
                .generateResponse(prompt)
                .thenAccept(response -> {
                    Map<String, String> socialChanges = parseSocialChanges(response);
                    applySocialBehaviorChanges(villager, socialChanges);
                });
        });
    }

    private void addNewTraditions(String traditions, List<VillagerAI> villagers) {
        String prompt = CulturalPromptTemplates.getTemplate("cultural_event", "evolved");
        
        LLMService.getInstance()
            .generateResponse(prompt)
            .thenAccept(response -> {
                VillagerManager.CulturalEvent event = parseNewEvent(response);
                if (event != null) {
                    historicalEvents.add(event);
                    notifyVillagersOfNewTradition(villagers, event);
                }
            });
    }

    private void updateDailyRoutines(String changes, List<VillagerAI> villagers) {
        villagers.forEach(villager -> {
            String prompt = CulturalPromptTemplates.getTemplate("daily_schedule",
                villager.getPersonality(),
                villager.getVillager().getVillagerData().getProfession().toString(),
                "evolved");
                
            LLMService.getInstance()
                .generateResponse(prompt)
                .thenAccept(response -> {
                    Map<Integer, VillagerAI.ActivitySchedule> newSchedule = parseSchedule(response);
                    villager.updateSchedule(newSchedule);
                });
        });
    }

    private Map<String, String> parseAdaptations(String response) {
        Map<String, String> adaptations = new HashMap<>();
        String[] sections = response.split("\n\n");
        for (String section : sections) {
            String[] parts = section.split(":", 2);
            if (parts.length == 2) {
                adaptations.put(parts[0].trim(), parts[1].trim());
            }
        }
        return adaptations;
    }

    private String getRecentHistory() {
        return historicalEvents.stream()
            .skip(Math.max(0, historicalEvents.size() - 5))
            .map(event -> event.name)
            .reduce((a, b) -> a + ", " + b)
            .orElse("No recent events");
    }

    private String generateEvolutionSummary(CulturalState state) {
        return String.format(
            "Cultural Evolution Stage %d:\n" +
            "Base: %s\n" +
            "Influences: %s\n" +
            "Recent Adaptations: %s",
            state.evolutionStage,
            state.baseCulture,
            state.influences,
            state.adaptations.values().stream()
                .reduce((a, b) -> a + "; " + b)
                .orElse("None")
        );
    }

    private void applySocialBehaviorChanges(VillagerAI villager, Map<String, String> changes) {
        changes.forEach((behavior, description) -> {
            villager.addBehaviorModifier(behavior, description);
            LOGGER.info("Applied new behavior '{}' to villager {}", 
                behavior, villager.getVillager().getName().getString());
        });
    }

    private void notifyVillagersOfNewTradition(List<VillagerAI> villagers, VillagerManager.CulturalEvent event) {
        villagers.forEach(villager -> {
            String situation = String.format("Learning about new tradition: %s", event.name);
            villager.generateBehavior(situation)
                .thenAccept(response -> 
                    LOGGER.info("Villager {} responds to new tradition: {}", 
                        villager.getVillager().getName().getString(), response));
        });
    }

    private Map<String, String> parseArchitecturalChanges(String response) {
        Map<String, String> changes = new HashMap<>();
        Arrays.stream(response.split("\n"))
            .filter(line -> line.contains(":"))
            .forEach(line -> {
                String[] parts = line.split(":", 2);
                changes.put(parts[0].trim(), parts[1].trim());
            });
        return changes;
    }

    private Map<String, String> parseSocialChanges(String response) {
        return parseArchitecturalChanges(response); // Same parsing logic
    }

    private VillagerManager.CulturalEvent parseNewEvent(String response) {
        Map<String, String> eventDetails = parseArchitecturalChanges(response);
        return new VillagerManager.CulturalEvent(
            eventDetails.getOrDefault("NAME", "Unknown Event"),
            eventDetails.getOrDefault("DESCRIPTION", "A new tradition"),
            Integer.parseInt(eventDetails.getOrDefault("DURATION", "24000")),
            System.currentTimeMillis()
        );
    }

    private Map<Integer, VillagerAI.ActivitySchedule> parseSchedule(String response) {
        Map<Integer, VillagerAI.ActivitySchedule> schedule = new HashMap<>();
        Arrays.stream(response.split("\n"))
            .filter(line -> line.contains(":"))
            .forEach(line -> {
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    String[] details = parts[1].split(",");
                    if (details.length == 3) {
                        schedule.put(
                            Integer.parseInt(parts[0].trim()),
                            new VillagerAI.ActivitySchedule(
                                details[0].trim(),
                                null, // Location will be determined when applied
                                Integer.parseInt(details[2].trim())
                            )
                        );
                    }
                }
            });
        return schedule;
    }
}