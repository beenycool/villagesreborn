package com.beeny.village;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.beeny.ai.LLMService;

public class InterCulturalManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn");
    private static final InterCulturalManager INSTANCE = new InterCulturalManager();
    private final Map<String, Set<String>> culturalConnections = new HashMap<>();
    private final Map<String, List<InteractionEvent>> interactionHistory = new HashMap<>();
    
    public static class InteractionEvent {
        final String culture1;
        final String culture2;
        final String type;
        final String outcome;
        final long timestamp;
        
        public InteractionEvent(String culture1, String culture2, String type, String outcome) {
            this.culture1 = culture1;
            this.culture2 = culture2;
            this.type = type;
            this.outcome = outcome;
            this.timestamp = System.currentTimeMillis();
        }
    }

    private InterCulturalManager() {}
    
    public static InterCulturalManager getInstance() {
        return INSTANCE;
    }

    public CompletableFuture<List<String>> generateCulturalInteraction(
            SpawnRegion region1, 
            SpawnRegion region2, 
            ServerWorld world) {
        
        String cultureKey = getCulturePairKey(region1.getCultureAsString(), region2.getCultureAsString());
        
        Map<String, String> context = new HashMap<>();
        context.put("culture1", region1.getCultureAsString());
        context.put("culture2", region2.getCultureAsString());
        context.put("distance", String.valueOf(getDistance(region1.getCenter(), region2.getCenter())));
        context.put("previous_interactions", getPreviousInteractions(cultureKey));
        
        String prompt = String.format(
            "Design an interaction between two Minecraft villages:\n" +
            "Village 1: %s culture\n" +
            "Village 2: %s culture\n" +
            "Distance: %d blocks\n" +
            "Previous interactions: %s\n\n" +
            "Generate potential cultural exchanges, conflicts, or collaborations.\n" +
            "Consider:\n" +
            "1. Trade opportunities\n" +
            "2. Architectural influences\n" +
            "3. Social customs\n" +
            "4. Shared celebrations\n\n" +
            "Format as:\n" +
            "TYPE: (trade/cultural/social/conflict)\n" +
            "DESCRIPTION: (interaction details)\n" +
            "OUTCOME: (resulting changes)\n" +
            "ADAPTATIONS: (specific modifications for each culture)",
            region1.getCulture(),
            region2.getCulture(),
            getDistance(region1.getCenter(), region2.getCenter()),
            getPreviousInteractions(cultureKey)
        );

        return LLMService.getInstance()
            .generateResponse(prompt, context)
            .thenApply(response -> {
                Map<String, String> interaction = parseInteraction(response);
                recordInteraction(region1.getCultureAsString(), region2.getCultureAsString(),
                    interaction.get("TYPE"), interaction.get("OUTCOME"));
                
                return generateAdaptations(interaction.get("ADAPTATIONS"));
            });
    }

    private String getCulturePairKey(String culture1, String culture2) {
        return culture1.compareTo(culture2) < 0 ?
            culture1 + "_" + culture2 :
            culture2 + "_" + culture1;
    }

    private int getDistance(BlockPos pos1, BlockPos pos2) {
        return (int) Math.sqrt(pos1.getSquaredDistance(pos2));
    }

    private String getPreviousInteractions(String culturePair) {
        List<InteractionEvent> events = interactionHistory.getOrDefault(culturePair, Collections.emptyList());
        if (events.isEmpty()) return "No previous interactions";
        
        return events.stream()
            .skip(Math.max(0, events.size() - 3))
            .map(e -> e.type + ": " + e.outcome)
            .reduce((a, b) -> a + "; " + b)
            .orElse("");
    }

    private Map<String, String> parseInteraction(String response) {
        return Arrays.stream(response.split("\n"))
            .map(line -> line.split(":", 2))
            .filter(parts -> parts.length == 2)
            .collect(HashMap::new,
                (m, parts) -> m.put(parts[0].trim(), parts[1].trim()),
                HashMap::putAll);
    }

    private void recordInteraction(String culture1, String culture2, String type, String outcome) {
        String key = getCulturePairKey(culture1, culture2);
        InteractionEvent event = new InteractionEvent(culture1, culture2, type, outcome);
        
        interactionHistory.computeIfAbsent(key, k -> new ArrayList<>()).add(event);
        
        culturalConnections.computeIfAbsent(culture1, k -> new HashSet<>()).add(culture2);
        culturalConnections.computeIfAbsent(culture2, k -> new HashSet<>()).add(culture1);
    }

    private List<String> generateAdaptations(String adaptationsStr) {
        return Arrays.asList(adaptationsStr.split(";"));
    }

    public void applyInteractions(ServerWorld world) {
        VillagerManager vm = VillagerManager.getInstance();
        List<SpawnRegion> regions = new ArrayList<>(vm.getSpawnRegions());
        
        for (int i = 0; i < regions.size(); i++) {
            for (int j = i + 1; j < regions.size(); j++) {
                SpawnRegion region1 = regions.get(i);
                SpawnRegion region2 = regions.get(j);
                
                if (shouldInteract(region1, region2)) {
                    generateCulturalInteraction(region1, region2, world)
                        .thenAccept(adaptations ->
                            applyAdaptations(adaptations, region1, region2, world));
                }
            }
        }
    }

    private boolean shouldInteract(SpawnRegion region1, SpawnRegion region2) {
        int distance = getDistance(region1.getCenter(), region2.getCenter());
        String culturePair = getCulturePairKey(region1.getCultureAsString(), region2.getCultureAsString());
        List<InteractionEvent> history = interactionHistory.getOrDefault(culturePair, Collections.emptyList());
        
        // More likely to interact if close or have history
        return distance < 500 || !history.isEmpty();
    }

    private void applyAdaptations(List<String> adaptations, SpawnRegion region1, SpawnRegion region2, ServerWorld world) {
        CulturalEvolution evolution = new CulturalEvolution();
        
        // Apply adaptations to both villages
        VillagerManager vm = VillagerManager.getInstance();
        List<VillagerAI> village1Villagers = getVillagersInRegion(region1);
        List<VillagerAI> village2Villagers = getVillagersInRegion(region2);
        
        evolution.evolveVillageCulture(region1.getCultureAsString(), world, village1Villagers)
            .thenAccept(summary -> LOGGER.info("Village 1 evolution: {}", summary));
            
        evolution.evolveVillageCulture(region2.getCultureAsString(), world, village2Villagers)
            .thenAccept(summary -> LOGGER.info("Village 2 evolution: {}", summary));
    }

    private List<VillagerAI> getVillagersInRegion(SpawnRegion region) {
        return VillagerManager.getInstance().getActiveVillagers().stream()
            .filter(ai -> region.isWithinRegion(ai.getVillager().getBlockPos()))
            .collect(java.util.stream.Collectors.toList());
    }

    public Set<String> getConnectedCultures(String culture) {
        return culturalConnections.getOrDefault(culture, Collections.emptySet());
    }

    public List<InteractionEvent> getInteractionHistory(String culture1, String culture2) {
        return interactionHistory.getOrDefault(getCulturePairKey(culture1, culture2), Collections.emptyList());
    }

    public void reset() {
        culturalConnections.clear();
        interactionHistory.clear();
    }
}