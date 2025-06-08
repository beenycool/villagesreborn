package com.beeny.villagesreborn.core.expansion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * AI-driven generator for dynamic expansion events.
 * Creates emergent gameplay through procedurally generated events that influence village expansion.
 */
public class ExpansionEventGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExpansionEventGenerator.class);
    private static final Random RANDOM = new Random();
    
    // Base probabilities for different event types (can be modified by AI)
    private final Map<ExpansionEvent.EventType, Float> eventProbabilities = new HashMap<>();
    
    // AI-influenced narrative templates for event descriptions
    private final Map<ExpansionEvent.EventType, List<String>> eventNarratives = new HashMap<>();
    
    public ExpansionEventGenerator() {
        initializeEventProbabilities();
        initializeEventNarratives();
    }
    
    /**
     * Initializes base event probabilities (can be overridden by AI).
     */
    private void initializeEventProbabilities() {
        eventProbabilities.put(ExpansionEvent.EventType.POPULATION_BOOM, 0.15f);
        eventProbabilities.put(ExpansionEvent.EventType.RESOURCE_DISCOVERY, 0.12f);
        eventProbabilities.put(ExpansionEvent.EventType.TRADE_ROUTE_OPENED, 0.10f);
        eventProbabilities.put(ExpansionEvent.EventType.THREAT_DETECTED, 0.08f);
        eventProbabilities.put(ExpansionEvent.EventType.CULTURAL_SHIFT, 0.10f);
        eventProbabilities.put(ExpansionEvent.EventType.SEASONAL_MIGRATION, 0.05f);
        eventProbabilities.put(ExpansionEvent.EventType.TECHNOLOGICAL_ADVANCE, 0.08f);
        eventProbabilities.put(ExpansionEvent.EventType.ENVIRONMENTAL_CHANGE, 0.06f);
        eventProbabilities.put(ExpansionEvent.EventType.DIPLOMATIC_EVENT, 0.07f);
        eventProbabilities.put(ExpansionEvent.EventType.ECONOMIC_BOOM, 0.09f);
    }
    
    /**
     * Initializes AI-influenced narrative templates for immersive event descriptions.
     */
    private void initializeEventNarratives() {
        eventNarratives.put(ExpansionEvent.EventType.POPULATION_BOOM, Arrays.asList(
            "A group of refugees has arrived seeking shelter and new opportunities.",
            "Young families are drawn to the village's growing prosperity.",
            "Word of the village's success has spread, attracting new settlers.",
            "A successful harvest celebration has encouraged more people to stay."
        ));
        
        eventNarratives.put(ExpansionEvent.EventType.RESOURCE_DISCOVERY, Arrays.asList(
            "Villagers have discovered a rich vein of ore in the nearby hills.",
            "A new source of fresh water has been found, enabling expansion.",
            "Rare herbs with medicinal properties have been discovered in the forest.",
            "An abandoned mine has been restored and is producing valuable materials."
        ));
        
        eventNarratives.put(ExpansionEvent.EventType.TRADE_ROUTE_OPENED, Arrays.asList(
            "Merchants have established a new trade route through the village.",
            "A delegation from a distant city has proposed regular trade exchanges.",
            "Caravans are now stopping regularly, bringing exotic goods and opportunities.",
            "The village's reputation for quality crafts has attracted foreign traders."
        ));
        
        eventNarratives.put(ExpansionEvent.EventType.THREAT_DETECTED, Arrays.asList(
            "Scouts report increased monster activity in the surrounding area.",
            "A rival village has been making threatening gestures towards expansion.",
            "Bandit activity has increased on the trade routes near the village.",
            "Strange omens suggest supernatural threats may be approaching."
        ));
        
        eventNarratives.put(ExpansionEvent.EventType.CULTURAL_SHIFT, Arrays.asList(
            "The villagers have developed a new appreciation for artistic endeavors.",
            "A charismatic leader has inspired the community towards different goals.",
            "Contact with foreign traders has introduced new customs and preferences.",
            "A generation of young villagers is showing different interests than their parents."
        ));
        
        eventNarratives.put(ExpansionEvent.EventType.TECHNOLOGICAL_ADVANCE, Arrays.asList(
            "A brilliant inventor has developed new construction techniques.",
            "Ancient blueprints have been discovered in forgotten ruins.",
            "Trade with advanced civilizations has brought new knowledge.",
            "Experimentation with local materials has yielded surprising results."
        ));
    }
    
    /**
     * Generates a random expansion event based on current village state.
     */
    public ExpansionEvent generateRandomEvent(VillageResources resources, String biomeName, 
                                            int villageSize, long currentTime) {
        
        // AI-influenced event type selection
        ExpansionEvent.EventType eventType = selectEventType(resources, villageSize, biomeName);
        if (eventType == null) {
            return null; // No event generated this time
        }
        
        // Generate AI-influenced event characteristics
        ExpansionEvent.Priority priority = generateEventPriority(eventType, resources);
        String description = generateEventDescription(eventType, biomeName);
        Map<String, Object> parameters = generateEventParameters(eventType, resources);
        List<String> triggeredBuildings = generateTriggeredBuildings(eventType, biomeName);
        float expansionModifier = generateExpansionModifier(eventType, priority);
        long duration = generateEventDuration(eventType, priority);
        
        ExpansionEvent event = new ExpansionEvent(eventType, priority, description, parameters,
                                                triggeredBuildings, expansionModifier, duration);
        
        LOGGER.info("Generated expansion event: {}", event);
        return event;
    }
    
    /**
     * AI-influenced event type selection based on village characteristics.
     */
    private ExpansionEvent.EventType selectEventType(VillageResources resources, int villageSize, String biomeName) {
        Map<ExpansionEvent.EventType, Float> adjustedProbabilities = new HashMap<>(eventProbabilities);
        
        // Adjust probabilities based on village state
        if (villageSize < 20) {
            // Small villages more likely to have population events
            adjustedProbabilities.put(ExpansionEvent.EventType.POPULATION_BOOM, 0.25f);
            adjustedProbabilities.put(ExpansionEvent.EventType.THREAT_DETECTED, 0.05f);
        } else if (villageSize > 100) {
            // Large villages more likely to have complex events
            adjustedProbabilities.put(ExpansionEvent.EventType.DIPLOMATIC_EVENT, 0.15f);
            adjustedProbabilities.put(ExpansionEvent.EventType.ECONOMIC_BOOM, 0.18f);
        }
        
        // Adjust based on resources
        if (resources.getFood() < 50) {
            adjustedProbabilities.put(ExpansionEvent.EventType.RESOURCE_DISCOVERY, 0.20f);
        }
        if (resources.getWood() > 200) {
            adjustedProbabilities.put(ExpansionEvent.EventType.TECHNOLOGICAL_ADVANCE, 0.15f);
        }
        
        // Biome-specific adjustments
        adjustProbabilitiesForBiome(adjustedProbabilities, biomeName);
        
        // Select event type based on adjusted probabilities
        float totalProbability = adjustedProbabilities.values().stream().reduce(0f, Float::sum);
        float roll = RANDOM.nextFloat() * totalProbability;
        
        float cumulative = 0f;
        for (Map.Entry<ExpansionEvent.EventType, Float> entry : adjustedProbabilities.entrySet()) {
            cumulative += entry.getValue();
            if (roll <= cumulative) {
                return entry.getKey();
            }
        }
        
        return null; // No event selected
    }
    
    /**
     * Adjusts event probabilities based on biome characteristics.
     */
    private void adjustProbabilitiesForBiome(Map<ExpansionEvent.EventType, Float> probabilities, String biomeName) {
        if (biomeName == null) return;
        
        switch (biomeName.toLowerCase()) {
            case "desert", "badlands" -> {
                probabilities.put(ExpansionEvent.EventType.RESOURCE_DISCOVERY, 0.18f);
                probabilities.put(ExpansionEvent.EventType.ENVIRONMENTAL_CHANGE, 0.12f);
            }
            case "forest", "jungle" -> {
                probabilities.put(ExpansionEvent.EventType.RESOURCE_DISCOVERY, 0.15f);
                probabilities.put(ExpansionEvent.EventType.THREAT_DETECTED, 0.12f);
            }
            case "mountain", "hills" -> {
                probabilities.put(ExpansionEvent.EventType.RESOURCE_DISCOVERY, 0.20f);
                probabilities.put(ExpansionEvent.EventType.TECHNOLOGICAL_ADVANCE, 0.12f);
            }
            case "plains", "meadow" -> {
                probabilities.put(ExpansionEvent.EventType.TRADE_ROUTE_OPENED, 0.15f);
                probabilities.put(ExpansionEvent.EventType.POPULATION_BOOM, 0.18f);
            }
            case "swamp" -> {
                probabilities.put(ExpansionEvent.EventType.ENVIRONMENTAL_CHANGE, 0.15f);
                probabilities.put(ExpansionEvent.EventType.CULTURAL_SHIFT, 0.12f);
            }
        }
    }
    
    /**
     * Generates AI-influenced event priority based on type and village state.
     */
    private ExpansionEvent.Priority generateEventPriority(ExpansionEvent.EventType eventType, VillageResources resources) {
        float basePriority = switch (eventType) {
            case THREAT_DETECTED -> 0.8f;
            case POPULATION_BOOM, RESOURCE_DISCOVERY -> 0.6f;
            case TRADE_ROUTE_OPENED, ECONOMIC_BOOM -> 0.5f;
            case TECHNOLOGICAL_ADVANCE, DIPLOMATIC_EVENT -> 0.4f;
            case CULTURAL_SHIFT, ENVIRONMENTAL_CHANGE -> 0.3f;
            case SEASONAL_MIGRATION -> 0.2f;
        };
        
        // Adjust based on resource scarcity
        if (resources.getFood() < 30 && (eventType == ExpansionEvent.EventType.RESOURCE_DISCOVERY ||
                                        eventType == ExpansionEvent.EventType.TRADE_ROUTE_OPENED)) {
            basePriority += 0.3f;
        }
        
        // Add randomness for immersion
        basePriority += (RANDOM.nextFloat() - 0.5f) * 0.2f;
        basePriority = Math.max(0.1f, Math.min(1.0f, basePriority));
        
        if (basePriority >= 0.8f) return ExpansionEvent.Priority.URGENT;
        if (basePriority >= 0.6f) return ExpansionEvent.Priority.HIGH;
        if (basePriority >= 0.3f) return ExpansionEvent.Priority.MEDIUM;
        return ExpansionEvent.Priority.LOW;
    }
    
    /**
     * Generates immersive event description using AI-influenced narratives.
     */
    private String generateEventDescription(ExpansionEvent.EventType eventType, String biomeName) {
        List<String> narratives = eventNarratives.get(eventType);
        if (narratives == null || narratives.isEmpty()) {
            return "An unexpected event has occurred in the village.";
        }
        
        String baseNarrative = narratives.get(RANDOM.nextInt(narratives.size()));
        
        // Add biome-specific flavor text
        String biomeContext = generateBiomeContext(biomeName);
        if (!biomeContext.isEmpty()) {
            baseNarrative += " " + biomeContext;
        }
        
        return baseNarrative;
    }
    
    /**
     * Generates biome-specific context for event descriptions.
     */
    private String generateBiomeContext(String biomeName) {
        if (biomeName == null) return "";
        
        return switch (biomeName.toLowerCase()) {
            case "desert" -> "The harsh desert conditions make this development particularly significant.";
            case "forest" -> "The dense woodland provides both opportunities and challenges.";
            case "mountain" -> "The mountainous terrain requires careful planning and adaptation.";
            case "swamp" -> "The wetland environment influences how the village must respond.";
            case "plains" -> "The open landscape offers excellent opportunities for growth.";
            case "taiga" -> "The cold climate adds urgency to expansion decisions.";
            default -> "";
        };
    }
    
    /**
     * Generates event-specific parameters for AI processing.
     */
    private Map<String, Object> generateEventParameters(ExpansionEvent.EventType eventType, VillageResources resources) {
        Map<String, Object> parameters = new HashMap<>();
        
        switch (eventType) {
            case POPULATION_BOOM -> {
                parameters.put("newVillagers", 5 + RANDOM.nextInt(15));
                parameters.put("housingNeeded", true);
            }
            case RESOURCE_DISCOVERY -> {
                String resourceType = generateDiscoveredResource();
                parameters.put("resourceType", resourceType);
                parameters.put("abundance", 0.5f + RANDOM.nextFloat() * 0.5f);
            }
            case TRADE_ROUTE_OPENED -> {
                parameters.put("tradeValue", 100 + RANDOM.nextInt(500));
                parameters.put("infrastructureNeeded", true);
            }
            case THREAT_DETECTED -> {
                parameters.put("threatLevel", 1 + RANDOM.nextInt(5));
                parameters.put("defensesNeeded", true);
            }
            case TECHNOLOGICAL_ADVANCE -> {
                parameters.put("technologyType", generateTechnologyType());
                parameters.put("buildingUnlocked", true);
            }
        }
        
        return parameters;
    }
    
    /**
     * Generates type of discovered resource.
     */
    private String generateDiscoveredResource() {
        String[] resources = {"iron", "gold", "stone", "clay", "herbs", "gemstones", "oil", "coal"};
        return resources[RANDOM.nextInt(resources.length)];
    }
    
    /**
     * Generates type of technological advance.
     */
    private String generateTechnologyType() {
        String[] technologies = {"construction", "agriculture", "metallurgy", "textiles", "medicine", "engineering"};
        return technologies[RANDOM.nextInt(technologies.length)];
    }
    
    /**
     * Generates buildings that should be prioritized due to this event.
     */
    private List<String> generateTriggeredBuildings(ExpansionEvent.EventType eventType, String biomeName) {
        List<String> buildings = new ArrayList<>();
        
        switch (eventType) {
            case POPULATION_BOOM -> buildings.addAll(Arrays.asList("house", "dormitory", "apartment"));
            case RESOURCE_DISCOVERY -> buildings.addAll(Arrays.asList("mine", "quarry", "refinery", "storage"));
            case TRADE_ROUTE_OPENED -> buildings.addAll(Arrays.asList("market", "warehouse", "inn", "stable"));
            case THREAT_DETECTED -> buildings.addAll(Arrays.asList("watchtower", "barracks", "walls", "fortress"));
            case CULTURAL_SHIFT -> buildings.addAll(Arrays.asList("library", "theater", "temple", "art_gallery"));
            case TECHNOLOGICAL_ADVANCE -> buildings.addAll(Arrays.asList("workshop", "laboratory", "university", "forge"));
            case ECONOMIC_BOOM -> buildings.addAll(Arrays.asList("bank", "luxury_housing", "plaza", "monument"));
            case DIPLOMATIC_EVENT -> buildings.addAll(Arrays.asList("embassy", "council_hall", "guest_house"));
            case ENVIRONMENTAL_CHANGE -> buildings.addAll(generateEnvironmentalBuildings(biomeName));
            case SEASONAL_MIGRATION -> buildings.addAll(Arrays.asList("temporary_housing", "camp", "shelter"));
        }
        
        return buildings;
    }
    
    /**
     * Generates buildings appropriate for environmental changes in specific biomes.
     */
    private List<String> generateEnvironmentalBuildings(String biomeName) {
        if (biomeName == null) return Arrays.asList("weather_station", "storage");
        
        return switch (biomeName.toLowerCase()) {
            case "desert" -> Arrays.asList("oasis", "shade_structure", "water_storage");
            case "mountain" -> Arrays.asList("avalanche_barrier", "weather_station", "heated_shelter");
            case "swamp" -> Arrays.asList("stilted_platform", "drainage", "bog_bridge");
            case "forest" -> Arrays.asList("fire_break", "ranger_station", "tree_farm");
            default -> Arrays.asList("weather_station", "emergency_shelter");
        };
    }
    
    /**
     * Generates expansion rate modifier based on event type and priority.
     */
    private float generateExpansionModifier(ExpansionEvent.EventType eventType, ExpansionEvent.Priority priority) {
        float baseModifier = switch (eventType) {
            case POPULATION_BOOM, ECONOMIC_BOOM -> 1.5f + RANDOM.nextFloat() * 0.5f; // 1.5-2.0x
            case RESOURCE_DISCOVERY, TRADE_ROUTE_OPENED -> 1.3f + RANDOM.nextFloat() * 0.4f; // 1.3-1.7x
            case TECHNOLOGICAL_ADVANCE -> 1.2f + RANDOM.nextFloat() * 0.3f; // 1.2-1.5x
            case THREAT_DETECTED -> 1.8f + RANDOM.nextFloat() * 0.4f; // 1.8-2.2x (urgent expansion)
            case CULTURAL_SHIFT, DIPLOMATIC_EVENT -> 1.1f + RANDOM.nextFloat() * 0.2f; // 1.1-1.3x
            case ENVIRONMENTAL_CHANGE -> 0.8f + RANDOM.nextFloat() * 0.4f; // 0.8-1.2x (may slow or accelerate)
            case SEASONAL_MIGRATION -> 1.0f + (RANDOM.nextFloat() - 0.5f) * 0.6f; // 0.7-1.3x (variable)
        };
        
        return baseModifier * priority.getMultiplier();
    }
    
    /**
     * Generates event duration based on type and priority.
     */
    private long generateEventDuration(ExpansionEvent.EventType eventType, ExpansionEvent.Priority priority) {
        long baseDuration = switch (eventType) {
            case THREAT_DETECTED -> 300000L; // 5 minutes - urgent
            case POPULATION_BOOM -> 600000L; // 10 minutes
            case RESOURCE_DISCOVERY, TECHNOLOGICAL_ADVANCE -> 900000L; // 15 minutes
            case TRADE_ROUTE_OPENED, ECONOMIC_BOOM -> 1200000L; // 20 minutes
            case CULTURAL_SHIFT, DIPLOMATIC_EVENT -> 1800000L; // 30 minutes
            case ENVIRONMENTAL_CHANGE -> 2400000L; // 40 minutes - long-term
            case SEASONAL_MIGRATION -> 1800000L; // 30 minutes
        };
        
        // Add randomness and priority influence
        float variation = 0.7f + RANDOM.nextFloat() * 0.6f; // 0.7 to 1.3 multiplier
        baseDuration = (long)(baseDuration * variation);
        
        // Priority can extend or shorten duration
        if (priority == ExpansionEvent.Priority.URGENT) {
            baseDuration *= 0.7f; // Shorter but more intense
        } else if (priority == ExpansionEvent.Priority.LOW) {
            baseDuration *= 1.5f; // Longer but less intense
        }
        
        return baseDuration;
    }
    
    /**
     * Checks if an event should be generated based on village state and time.
     */
    public boolean shouldGenerateEvent(VillageResources resources, int villageSize, long timeSinceLastEvent) {
        // Base probability increases over time since last event
        float baseProbability = Math.min(0.3f, timeSinceLastEvent / 600000f); // Max 30% after 10 minutes
        
        // Adjust based on village size (larger villages have more events)
        float sizeFactor = 1.0f + (villageSize / 100f) * 0.5f;
        
        // Adjust based on resource stress (low resources increase event probability)
        float stressFactor = 1.0f;
        if (resources.getFood() < 50 || resources.getWood() < 30) {
            stressFactor = 1.5f;
        }
        
        float finalProbability = baseProbability * sizeFactor * stressFactor;
        return RANDOM.nextFloat() < finalProbability;
    }
} 