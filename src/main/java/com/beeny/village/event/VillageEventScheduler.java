package com.beeny.village.event;

import com.beeny.village.VillageInfluenceManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Handles automatic scheduling and generation of cultural events for villages
 * Creates varied events based on templates, village state, and seasonal factors
 */
public class VillageEventScheduler {
    private static final VillageEventScheduler INSTANCE = new VillageEventScheduler();
    
    // Maps village BlockPos to their next scheduled event time (in system millis)
    private final Map<BlockPos, Long> nextEventTimes = new ConcurrentHashMap<>();
    
    // Maps village BlockPos to their event frequency in minutes
    private final Map<BlockPos, Integer> eventFrequencies = new ConcurrentHashMap<>();
    
    // Maps village BlockPos to their event radius
    private final Map<BlockPos, Integer> eventRadii = new ConcurrentHashMap<>();
    
    // Maps village BlockPos to their recent event history
    private final Map<BlockPos, List<String>> villageEventHistory = new ConcurrentHashMap<>();
    
    // Maps village BlockPos to their special conditions
    private final Map<BlockPos, Map<String, Object>> villageConditions = new ConcurrentHashMap<>();
    
    // Minimum time between event checks (in milliseconds)
    private static final long MIN_CHECK_INTERVAL = 1000 * 60; // 1 minute
    
    // Maximum events to track in history per village
    private static final int MAX_EVENT_HISTORY = 5;
    
    // Last time we checked for new events
    private long lastCheckTime = 0;
    
    // Random for event generation
    private final Random random = new Random();
    
    // Event types by culture
    private final Map<String, List<EventTemplate>> eventTemplatesByCulture = new HashMap<>();
    
    // Special event triggers by condition
    private final Map<String, List<EventTrigger>> eventTriggersByCondition = new HashMap<>();
    
    private VillageEventScheduler() {
        initializeEventTemplates();
        initializeEventTriggers();
    }
    
    public static VillageEventScheduler getInstance() {
        return INSTANCE;
    }
    
    /**
     * Event template for generating events
     */
    private static class EventTemplate {
        public final String type;
        public final String[] descriptions;
        public final int weight;
        public final int baseMinDuration;
        public final int baseMaxDuration;
        public final Map<Season, Integer> seasonalWeights;
        public final boolean requiresSpecialCondition;
        public final String specialCondition;
        
        public EventTemplate(String type, String[] descriptions, int weight, 
                           int baseMinDuration, int baseMaxDuration) {
            this(type, descriptions, weight, baseMinDuration, baseMaxDuration, null, false, null);
        }
        
        public EventTemplate(String type, String[] descriptions, int weight, 
                           int baseMinDuration, int baseMaxDuration,
                           Map<Season, Integer> seasonalWeights, 
                           boolean requiresSpecialCondition, 
                           String specialCondition) {
            this.type = type;
            this.descriptions = descriptions;
            this.weight = weight;
            this.baseMinDuration = baseMinDuration;
            this.baseMaxDuration = baseMaxDuration;
            this.seasonalWeights = seasonalWeights != null ? seasonalWeights : new EnumMap<>(Season.class);
            this.requiresSpecialCondition = requiresSpecialCondition;
            this.specialCondition = specialCondition;
        }
        
        /**
         * Get adjusted weight based on season and village conditions
         */
        public int getAdjustedWeight(Season currentSeason, Map<String, Object> conditions) {
            int baseWeight = weight;
            
            // Apply seasonal adjustment
            Integer seasonalWeight = seasonalWeights.get(currentSeason);
            if (seasonalWeight != null) {
                baseWeight = seasonalWeight;
            }
            
            // Check if this event requires a special condition
            if (requiresSpecialCondition) {
                if (conditions == null || !conditions.containsKey(specialCondition)) {
                    return 0; // Event can't happen without the required condition
                }
                
                // Boost the weight for special condition events when conditions are met
                baseWeight *= 2;
            }
            
            return baseWeight;
        }
    }
    
    /**
     * Represents a special event trigger based on village conditions
     */
    private static class EventTrigger {
        public final String triggerCondition;
        public final String eventType;
        public final String[] eventDescriptions;
        public final int durationMinutes;
        public final int priorityWeight;
        
        public EventTrigger(String triggerCondition, String eventType, 
                          String[] eventDescriptions, int durationMinutes, int priorityWeight) {
            this.triggerCondition = triggerCondition;
            this.eventType = eventType;
            this.eventDescriptions = eventDescriptions;
            this.durationMinutes = durationMinutes;
            this.priorityWeight = priorityWeight;
        }
    }
    
    /**
     * Seasons that can affect event occurrence and frequency
     */
    public enum Season {
        SPRING,
        SUMMER,
        AUTUMN,
        WINTER
    }
    
    /**
     * Initialize event triggers for special conditions
     */
    private void initializeEventTriggers() {
        // Drought condition triggers
        List<EventTrigger> droughtTriggers = new ArrayList<>();
        droughtTriggers.add(new EventTrigger(
            "drought",
            "ritual",
            new String[] {
                "Rain Prayer Ceremony", 
                "Drought Crisis Council", 
                "Water Conservation Ritual"
            },
            120, 200
        ));
        eventTriggersByCondition.put("drought", droughtTriggers);
        
        // Prosperity condition triggers
        List<EventTrigger> prosperityTriggers = new ArrayList<>();
        prosperityTriggers.add(new EventTrigger(
            "prosperity",
            "festival",
            new String[] {
                "Grand Harvest Festival", 
                "Prosperity Celebration", 
                "Golden Age Ceremony"
            },
            180, 150
        ));
        eventTriggersByCondition.put("prosperity", prosperityTriggers);
        
        // War condition triggers
        List<EventTrigger> warTriggers = new ArrayList<>();
        warTriggers.add(new EventTrigger(
            "war",
            "ceremony",
            new String[] {
                "War Council Assembly", 
                "Military Rally", 
                "Defense Preparation Ritual"
            },
            60, 250
        ));
        eventTriggersByCondition.put("war", warTriggers);
        
        // Trade boom condition triggers
        List<EventTrigger> tradeTriggers = new ArrayList<>();
        tradeTriggers.add(new EventTrigger(
            "trade_boom",
            "market",
            new String[] {
                "Grand Trade Fair", 
                "Merchant Guild Gathering", 
                "International Exchange Fair"
            },
            120, 180
        ));
        eventTriggersByCondition.put("trade_boom", tradeTriggers);
    }
    
    /**
     * Initialize event templates for each culture
     */
    private void initializeEventTemplates() {
        // Roman events
        List<EventTemplate> romanEvents = new ArrayList<>();
        
        // Standard festivals with seasonal weights
        Map<Season, Integer> romanFestivalSeasons = new EnumMap<>(Season.class);
        romanFestivalSeasons.put(Season.SPRING, 150); // Spring festivals more common
        romanFestivalSeasons.put(Season.WINTER, 120); // Winter Saturnalia
        
        romanEvents.add(new EventTemplate(
            "festival",
            new String[] {
                "Festival of Jupiter", 
                "Roman Market Festival", 
                "Festival of Sol Invictus",
                "Roman Games",
                "Bacchanalia"
            },
            100, 60, 180, romanFestivalSeasons, false, null
        ));
        
        // Ceremonies
        Map<Season, Integer> romanCeremonySeasons = new EnumMap<>(Season.class);
        romanCeremonySeasons.put(Season.SUMMER, 100); // More ceremonies in summer
        
        romanEvents.add(new EventTemplate(
            "ceremony",
            new String[] {
                "Roman Military Triumph", 
                "Senate Ceremony", 
                "Vestal Rites",
                "Gladiatorial Ceremony"
            },
            70, 45, 120, romanCeremonySeasons, false, null
        ));
        
        // Rituals
        Map<Season, Integer> romanRitualSeasons = new EnumMap<>(Season.class);
        romanRitualSeasons.put(Season.WINTER, 80); // Winter rituals more common
        
        romanEvents.add(new EventTemplate(
            "ritual",
            new String[] {
                "Temple Offering Ritual", 
                "Feast of Venus", 
                "Saturnalia Celebration",
                "Votive Ceremony"
            },
            50, 30, 90, romanRitualSeasons, false, null
        ));
        
        // Competitions with seasonal weights
        Map<Season, Integer> romanCompetitionSeasons = new EnumMap<>(Season.class);
        romanCompetitionSeasons.put(Season.SUMMER, 130); // Summer competitions more common
        
        romanEvents.add(new EventTemplate(
            "competition",
            new String[] {
                "Gladiator Tournament", 
                "Chariot Race", 
                "Roman Athletic Contest",
                "Military Training Contest"
            },
            80, 60, 150, romanCompetitionSeasons, false, null
        ));
        
        // Special condition events
        romanEvents.add(new EventTemplate(
            "crisis_relief",
            new String[] {
                "Grain Distribution Ceremony", 
                "Public Works Project",
                "Imperial Relief Effort" 
            },
            20, 45, 90, null, true, "food_shortage"
        ));
        
        eventTemplatesByCulture.put("roman", romanEvents);
        
        // Egyptian events
        List<EventTemplate> egyptianEvents = new ArrayList<>();
        
        // Seasonal adjustments for Egyptian festivals
        Map<Season, Integer> egyptFestivalSeasons = new EnumMap<>(Season.class);
        egyptFestivalSeasons.put(Season.SUMMER, 150); // Summer Nile flood festivals
        
        egyptianEvents.add(new EventTemplate(
            "festival",
            new String[] {
                "Festival of the Nile", 
                "Opet Festival", 
                "Festival of Hathor",
                "Feast of Bastet",
                "Celebration of the Pharaoh"
            },
            100, 60, 180, egyptFestivalSeasons, false, null
        ));
        
        egyptianEvents.add(new EventTemplate(
            "ceremony",
            new String[] {
                "Temple Dedication", 
                "Pharaoh's Coronation", 
                "Mummification Ceremony",
                "Opening of the Mouth Ritual"
            },
            70, 45, 120
        ));
        
        // Egyptian rituals more common during transitions between seasons
        Map<Season, Integer> egyptRitualSeasons = new EnumMap<>(Season.class);
        egyptRitualSeasons.put(Season.SPRING, 80);
        egyptRitualSeasons.put(Season.AUTUMN, 80);
        
        egyptianEvents.add(new EventTemplate(
            "ritual",
            new String[] {
                "Rite of Osiris", 
                "Solar Deity Worship", 
                "Blessing of the Fields",
                "Ritual of the Afterlife"
            },
            60, 30, 90, egyptRitualSeasons, false, null
        ));
        
        egyptianEvents.add(new EventTemplate(
            "competition",
            new String[] {
                "Nile Boat Race", 
                "Egyptian Wrestling", 
                "Archery Contest",
                "Hunting Tournament"
            },
            70, 60, 150
        ));
        
        // Special condition events
        egyptianEvents.add(new EventTemplate(
            "flood_preparation",
            new String[] {
                "Nile Flood Preparation", 
                "Hapi Worship Ritual",
                "Levee Construction Effort" 
            },
            20, 45, 90, null, true, "flood_warning"
        ));
        
        eventTemplatesByCulture.put("egyptian", egyptianEvents);
        
        // Victorian events
        List<EventTemplate> victorianEvents = new ArrayList<>();
        
        // Seasonal weights for Victorian festivals
        Map<Season, Integer> victorianFestivalSeasons = new EnumMap<>(Season.class);
        victorianFestivalSeasons.put(Season.WINTER, 130); // Christmas markets and winter balls
        victorianFestivalSeasons.put(Season.SPRING, 110); // Spring fairs
        
        victorianEvents.add(new EventTemplate(
            "festival",
            new String[] {
                "Victorian Garden Party", 
                "Industrial Exhibition", 
                "Christmas Market",
                "Grand Ball",
                "Spring Fair"
            },
            100, 60, 180, victorianFestivalSeasons, false, null
        ));
        
        victorianEvents.add(new EventTemplate(
            "ceremony",
            new String[] {
                "Royal Visit", 
                "Knighting Ceremony", 
                "Factory Inauguration",
                "Town Hall Assembly"
            },
            80, 45, 120
        ));
        
        victorianEvents.add(new EventTemplate(
            "market",
            new String[] {
                "Street Market", 
                "Trade Exposition", 
                "Antique Auction",
                "Factory Goods Fair"
            },
            90, 60, 150
        ));
        
        // Victorian competitions vary by season
        Map<Season, Integer> victorianCompetitionSeasons = new EnumMap<>(Season.class);
        victorianCompetitionSeasons.put(Season.SUMMER, 100); // Summer sporting events
        victorianCompetitionSeasons.put(Season.AUTUMN, 80);  // Autumn competitions
        victorianCompetitionSeasons.put(Season.WINTER, 40);  // Fewer winter competitions
        
        victorianEvents.add(new EventTemplate(
            "competition",
            new String[] {
                "Cricket Match", 
                "Horse Race", 
                "Poetry Contest",
                "Scientific Demonstration"
            },
            70, 60, 150, victorianCompetitionSeasons, false, null
        ));
        
        // Special condition events
        victorianEvents.add(new EventTemplate(
            "industrial_showcase",
            new String[] {
                "Industrial Innovation Showcase", 
                "Engineering Marvel Exhibition",
                "Modern Machinery Demonstration" 
            },
            20, 60, 120, null, true, "industrial_boom"
        ));
        
        eventTemplatesByCulture.put("victorian", victorianEvents);
        
        // NYC events
        List<EventTemplate> nycEvents = new ArrayList<>();
        
        // Seasonal adjustments for NYC festivals
        Map<Season, Integer> nycFestivalSeasons = new EnumMap<>(Season.class);
        nycFestivalSeasons.put(Season.SUMMER, 120); // Summer street festivals
        nycFestivalSeasons.put(Season.WINTER, 110); // Winter holiday celebrations
        
        nycEvents.add(new EventTemplate(
            "festival",
            new String[] {
                "Broadway Street Party", 
                "Rooftop Celebration", 
                "Cultural Diversity Festival",
                "Downtown Music Festival",
                "Art Gallery Opening"
            },
            100, 60, 180, nycFestivalSeasons, false, null
        ));
        
        // NYC markets vary by season
        Map<Season, Integer> nycMarketSeasons = new EnumMap<>(Season.class);
        nycMarketSeasons.put(Season.SPRING, 120); // Spring markets
        nycMarketSeasons.put(Season.AUTUMN, 110); // Fall markets
        
        nycEvents.add(new EventTemplate(
            "market",
            new String[] {
                "Farmers Market", 
                "Stock Exchange Day", 
                "Fashion Week Market",
                "Street Food Fair"
            },
            90, 45, 120, nycMarketSeasons, false, null
        ));
        
        nycEvents.add(new EventTemplate(
            "celebration",
            new String[] {
                "New Year's Celebration", 
                "Independence Day Party", 
                "Central Park Concert",
                "Neighborhood Block Party"
            },
            80, 60, 150
        ));
        
        nycEvents.add(new EventTemplate(
            "competition",
            new String[] {
                "Marathon Race", 
                "Street Basketball Tournament", 
                "Hot Dog Eating Contest",
                "Subway Art Competition"
            },
            70, 60, 150
        ));
        
        // Special condition events
        nycEvents.add(new EventTemplate(
            "cleanup_initiative",
            new String[] {
                "Community Cleanup Drive", 
                "Green Space Restoration",
                "Urban Beautification Project" 
            },
            20, 45, 90, null, true, "pollution"
        ));
        
        eventTemplatesByCulture.put("nyc", nycEvents);
    }
    
    /**
     * Register a village for event scheduling
     * 
     * @param center The village center position
     * @param frequency How often events should occur (in minutes)
     * @param radius The radius for village events
     */
    public void registerVillage(BlockPos center, int frequency, int radius) {
        eventFrequencies.put(center, frequency);
        eventRadii.put(center, radius);
        villageEventHistory.put(center, new ArrayList<>());
        villageConditions.put(center, new HashMap<>());
        
        // Schedule the first event
        scheduleNextEvent(center);
    }
    
    /**
     * Schedule the next event for a village
     */
    private void scheduleNextEvent(BlockPos center) {
        int frequency = eventFrequencies.getOrDefault(center, 180); // Default to 3 hours
        
        // Get village development level to adjust frequency
        VillageInfluenceManager.VillageDevelopmentData village = 
            VillageInfluenceManager.getInstance().getVillageDevelopment(center);
        
        if (village != null) {
            int level = village.getVillageLevel();
            // Higher level villages have more frequent events
            frequency = Math.max(30, frequency - ((level - 1) * 30));
        }
        
        // Adjust frequency based on active conditions
        Map<String, Object> conditions = villageConditions.getOrDefault(center, Collections.emptyMap());
        if (conditions.containsKey("festival_season")) {
            frequency = (int)(frequency * 0.7); // More frequent during festival seasons
        }
        
        // Add some randomness to the frequency (±20%)
        int randomizedFrequency = (int)(frequency * (0.8 + random.nextFloat() * 0.4));
        
        // Calculate next event time
        long nextEventTime = System.currentTimeMillis() + (randomizedFrequency * 60 * 1000L);
        nextEventTimes.put(center, nextEventTime);
    }
    
    /**
     * Check for villages with scheduled events and create them if it's time
     */
    public void update(World world) {
        long currentTime = System.currentTimeMillis();
        
        // Only check periodically to reduce overhead
        if (currentTime - lastCheckTime < MIN_CHECK_INTERVAL) {
            return;
        }
        
        lastCheckTime = currentTime;
        
        // Calculate current season based on world time
        Season currentSeason = getCurrentSeason(world);
        
        // Process each village with scheduled events
        for (Map.Entry<BlockPos, Long> entry : nextEventTimes.entrySet()) {
            BlockPos villageCenter = entry.getKey();
            long scheduledTime = entry.getValue();
            
            // Check if it's time for the event
            if (currentTime >= scheduledTime) {
                // Get village culture
                VillageInfluenceManager.VillageDevelopmentData village = 
                    VillageInfluenceManager.getInstance().getVillageDevelopment(villageCenter);
                
                if (village != null) {
                    String culture = village.getCulture();
                    int radius = eventRadii.getOrDefault(villageCenter, 32);
                    
                    // Check for special condition triggers first
                    Map<String, Object> conditions = villageConditions.getOrDefault(villageCenter, new HashMap<>());
                    boolean triggeredSpecialEvent = checkAndTriggerSpecialEvent(world, villageCenter, culture, radius, conditions);
                    
                    // If no special event was triggered, create a regular random event
                    if (!triggeredSpecialEvent) {
                        createRandomEvent(world, villageCenter, culture, radius, currentSeason, conditions);
                    }
                    
                    // Schedule next event
                    scheduleNextEvent(villageCenter);
                }
            }
        }
        
        // Update all existing events
        VillageEvent.updateAllEvents(world);
    }
    
    /**
     * Check for and potentially trigger a special event based on village conditions
     * @return true if a special event was triggered
     */
    private boolean checkAndTriggerSpecialEvent(World world, BlockPos center, String culture, 
                                              int radius, Map<String, Object> conditions) {
        if (conditions.isEmpty()) {
            return false;
        }
        
        // Get all conditions that have active triggers
        List<String> activeConditions = conditions.keySet().stream()
            .filter(eventTriggersByCondition::containsKey)
            .collect(Collectors.toList());
        
        if (activeConditions.isEmpty()) {
            return false;
        }
        
        // Sort by priority potential triggers
        String selectedCondition = activeConditions.get(random.nextInt(activeConditions.size()));
        List<EventTrigger> potentialTriggers = eventTriggersByCondition.get(selectedCondition);
        
        // Random trigger selection weighted by priority
        int totalPriority = potentialTriggers.stream().mapToInt(t -> t.priorityWeight).sum();
        int randomValue = random.nextInt(totalPriority);
        int currentPriority = 0;
        
        EventTrigger selectedTrigger = null;
        for (EventTrigger trigger : potentialTriggers) {
            currentPriority += trigger.priorityWeight;
            if (randomValue < currentPriority) {
                selectedTrigger = trigger;
                break;
            }
        }
        
        if (selectedTrigger == null) {
            selectedTrigger = potentialTriggers.get(0);
        }
        
        // Select a random description
        String description = selectedTrigger.eventDescriptions[
            random.nextInt(selectedTrigger.eventDescriptions.length)];
        
        // Create the special event
        VillageEvent event = VillageEvent.createEvent(
            selectedTrigger.eventType,
            culture,
            description,
            center,
            radius,
            selectedTrigger.durationMinutes
        );
        
        // Store event type in history
        recordEventInHistory(center, selectedTrigger.eventType);
        
        // Mark this condition as handled (optionally clear it if it's a one-time event)
        if (conditions.get(selectedCondition) instanceof Boolean && (Boolean)conditions.get(selectedCondition)) {
            conditions.remove(selectedCondition);
        }
        
        // Notify nearby players about the special event
        notifyNearbyPlayers(world, center, radius * 2, player -> {
            player.sendMessage(Text.of("§c§lSpecial Event: " + description + "§r"), false);
            player.sendMessage(Text.of("§6A " + culture + " " + 
                selectedTrigger.eventType + " is happening in response to " + 
                getConditionDisplayName(selectedCondition) + "!§r"), false);
            player.sendMessage(Text.of("§eJoin in to help the village and earn special rewards.§r"), false);
        });
        
        return true;
    }
    
    /**
     * Get user-friendly display name for a condition
     */
    private String getConditionDisplayName(String conditionId) {
        return switch (conditionId) {
            case "drought" -> "severe drought";
            case "prosperity" -> "village prosperity";
            case "war" -> "ongoing conflict";
            case "flood_warning" -> "flood warnings";
            case "trade_boom" -> "a trade boom";
            case "industrial_boom" -> "rapid industrialization";
            case "pollution" -> "urban pollution";
            case "food_shortage" -> "food shortages";
            case "festival_season" -> "the festival season";
            default -> conditionId.replace('_', ' ');
        };
    }
    
    /**
     * Create a random event for a village based on templates and current conditions
     */
    private void createRandomEvent(World world, BlockPos center, String culture, 
                                 int radius, Season currentSeason, Map<String, Object> conditions) {
        // Get event templates for this culture
        List<EventTemplate> templates = eventTemplatesByCulture.get(culture.toLowerCase());
        if (templates == null || templates.isEmpty()) {
            return;
        }
        
        // Get village event history to avoid repetition
        List<String> recentEvents = villageEventHistory.getOrDefault(center, new ArrayList<>());
        
        // Filter templates to avoid recent repeats if we have enough history
        List<EventTemplate> availableTemplates;
        if (recentEvents.size() >= 2) {
            String lastEventType = recentEvents.get(recentEvents.size() - 1);
            availableTemplates = templates.stream()
                .filter(t -> !t.type.equals(lastEventType) || random.nextFloat() < 0.2f) // 20% chance to repeat
                .collect(Collectors.toList());
        } else {
            availableTemplates = new ArrayList<>(templates);
        }
        
        // Select a random event template based on weights adjusted for season and conditions
        EventTemplate selectedTemplate = selectRandomTemplate(availableTemplates, currentSeason, conditions);
        
        // Pick a random description
        String description = selectedTemplate.descriptions[random.nextInt(selectedTemplate.descriptions.length)];
        
        // Calculate duration based on village development level and conditions
        VillageInfluenceManager.VillageDevelopmentData village = 
            VillageInfluenceManager.getInstance().getVillageDevelopment(center);
        
        int durationMinutes = selectedTemplate.baseMinDuration;
        if (village != null) {
            int villageLevel = village.getVillageLevel();
            // Larger duration for higher level villages
            durationMinutes += (villageLevel - 1) * 30;
            durationMinutes = Math.min(durationMinutes, selectedTemplate.baseMaxDuration);
            
            // Adjust duration based on player participation history (longer events if popular)
            if (conditions.containsKey("high_participation")) {
                durationMinutes = (int)(durationMinutes * 1.5);
            }
        }
        
        // Create the event
        VillageEvent event = VillageEvent.createEvent(
            selectedTemplate.type,
            culture,
            description, 
            center,
            radius,
            durationMinutes
        );
        
        // Record this event type in history
        recordEventInHistory(center, selectedTemplate.type);
        
        // Notify nearby players about the new event
        notifyNearbyPlayers(world, center, radius * 2, player -> {
            player.sendMessage(Text.of("§6§lNew Event: " + description + "§r"), false);
            player.sendMessage(Text.of("§eA " + culture + " " + 
                selectedTemplate.type + " has begun nearby!§r"), false);
            player.sendMessage(Text.of("§eJoin in to earn reputation and rewards.§r"), false);
        });
    }
    
    /**
     * Record an event type in the village's history
     */
    private void recordEventInHistory(BlockPos center, String eventType) {
        List<String> history = villageEventHistory.computeIfAbsent(center, k -> new ArrayList<>());
        history.add(eventType);
        
        // Trim history if it's too long
        if (history.size() > MAX_EVENT_HISTORY) {
            history.remove(0);
        }
    }
    
    /**
     * Select a random event template based on weights adjusted for season and conditions
     */
    private EventTemplate selectRandomTemplate(List<EventTemplate> templates, 
                                             Season currentSeason, 
                                             Map<String, Object> conditions) {
        int totalWeight = 0;
        int[] adjustedWeights = new int[templates.size()];
        
        for (int i = 0; i < templates.size(); i++) {
            EventTemplate template = templates.get(i);
            adjustedWeights[i] = template.getAdjustedWeight(currentSeason, conditions);
            totalWeight += adjustedWeights[i];
        }
        
        // If no valid templates (all weights zero), use default weights
        if (totalWeight <= 0) {
            totalWeight = 0;
            for (int i = 0; i < templates.size(); i++) {
                adjustedWeights[i] = templates.get(i).weight;
                totalWeight += adjustedWeights[i];
            }
        }
        
        int randomValue = random.nextInt(totalWeight);
        int currentWeight = 0;
        
        for (int i = 0; i < templates.size(); i++) {
            currentWeight += adjustedWeights[i];
            if (randomValue < currentWeight) {
                return templates.get(i);
            }
        }
        
        // Fallback
        return templates.get(0);
    }
    
    /**
     * Get the current season based on world time
     */
    private Season getCurrentSeason(World world) {
        // Calculate day of the year (0-365)
        long worldTime = world.getTimeOfDay();
        long dayCount = worldTime / 24000L;
        long dayOfYear = dayCount % 365L;
        
        // Divide the year into 4 seasons
        if (dayOfYear < 91) {
            return Season.SPRING;
        } else if (dayOfYear < 182) {
            return Season.SUMMER;
        } else if (dayOfYear < 273) {
            return Season.AUTUMN;
        } else {
            return Season.WINTER;
        }
    }
    
    /**
     * Notify players near a position about something
     */
    private void notifyNearbyPlayers(World world, BlockPos center, int radius, Consumer<ServerPlayerEntity> action) {
        if (!(world instanceof ServerWorld serverWorld)) {
            return;
        }
        
        Vec3d centerVec = new Vec3d(center.getX(), center.getY(), center.getZ());
        
        for (ServerPlayerEntity player : serverWorld.getPlayers()) {
            if (player.getPos().squaredDistanceTo(centerVec) <= radius * radius) {
                action.accept(player);
            }
        }
    }
    
    /**
     * Get the time until the next event for a village (in minutes)
     */
    public int getTimeUntilNextEvent(BlockPos center) {
        Long nextTime = nextEventTimes.get(center);
        if (nextTime == null) {
            return -1;
        }
        
        long remainingMs = nextTime - System.currentTimeMillis();
        if (remainingMs <= 0) {
            return 0;
        }
        
        return (int)(remainingMs / (1000 * 60)); // Convert ms to minutes
    }
    
    /**
     * Force an immediate event at a village
     */
    public void forceEvent(World world, BlockPos center, String eventType) {
        VillageInfluenceManager.VillageDevelopmentData village = 
            VillageInfluenceManager.getInstance().getVillageDevelopment(center);
        
        if (village == null) {
            return;
        }
        
        String culture = village.getCulture();
        int radius = eventRadii.getOrDefault(center, 32);
        
        // Find matching event template
        List<EventTemplate> templates = eventTemplatesByCulture.get(culture.toLowerCase());
        if (templates == null || templates.isEmpty()) {
            return;
        }
        
        EventTemplate matchingTemplate = null;
        for (EventTemplate template : templates) {
            if (template.type.equalsIgnoreCase(eventType)) {
                matchingTemplate = template;
                break;
            }
        }
        
        // If no matching template, use first available
        if (matchingTemplate == null) {
            matchingTemplate = templates.get(0);
        }
        
        // Pick a random description
        String description = matchingTemplate.descriptions[random.nextInt(matchingTemplate.descriptions.length)];
        
        // Create the event with a standard duration
        VillageEvent.createEvent(
            matchingTemplate.type,
            culture,
            description,
            center,
            radius,
            90  // 90 minutes duration
        );
        
        // Record in history
        recordEventInHistory(center, matchingTemplate.type);
        
        // Store needed data in final variables before using in lambda
        final String eventType_final = matchingTemplate.type;
        
        // Notify nearby players
        notifyNearbyPlayers(world, center, radius * 2, player -> {
            player.sendMessage(Text.of("§6§lSpecial Event: " + description + "§r"), false);
            player.sendMessage(Text.of("§eA " + culture + " " + 
                eventType_final + " has begun nearby!§r"), false);
            player.sendMessage(Text.of("§eJoin in to earn reputation and rewards.§r"), false);
        });
        
        // Schedule next normal event
        scheduleNextEvent(center);
    }
    
    /**
     * Add a special condition to a village that may trigger special events
     */
    public void addVillageCondition(BlockPos center, String condition, Object value) {
        Map<String, Object> conditions = villageConditions.computeIfAbsent(center, k -> new HashMap<>());
        conditions.put(condition, value);
    }
    
    /**
     * Remove a special condition from a village
     */
    public void removeVillageCondition(BlockPos center, String condition) {
        Map<String, Object> conditions = villageConditions.get(center);
        if (conditions != null) {
            conditions.remove(condition);
        }
    }
    
    /**
     * Check if a village has a specific condition
     */
    public boolean hasVillageCondition(BlockPos center, String condition) {
        Map<String, Object> conditions = villageConditions.get(center);
        return conditions != null && conditions.containsKey(condition);
    }
    
    /**
     * Get all village conditions
     */
    public Map<String, Object> getVillageConditions(BlockPos center) {
        return new HashMap<>(villageConditions.getOrDefault(center, Collections.emptyMap()));
    }
    
    /**
     * Get recently occurred event types for a village
     */
    public List<String> getRecentEventTypes(BlockPos center) {
        return new ArrayList<>(villageEventHistory.getOrDefault(center, Collections.emptyList()));
    }
    
    /**
     * Update the event frequency for a village
     */
    public void setEventFrequency(BlockPos center, int frequencyMinutes) {
        eventFrequencies.put(center, frequencyMinutes);
    }
    
    /**
     * Update the event radius for a village
     */
    public void setEventRadius(BlockPos center, int radius) {
        eventRadii.put(center, radius);
    }
    
    /**
     * Record player participation in events to adjust future events
     */
    public void recordPlayerParticipation(BlockPos villageCenter, int playerCount) {
        Map<String, Object> conditions = villageConditions.computeIfAbsent(villageCenter, k -> new HashMap<>());
        
        // Mark as high participation if more than 3 players participated
        if (playerCount > 3) {
            conditions.put("high_participation", true);
        } else if (playerCount == 0) {
            // If no players participated, consider increasing the frequency
            int currentFrequency = eventFrequencies.getOrDefault(villageCenter, 180);
            eventFrequencies.put(villageCenter, Math.min(currentFrequency + 30, 240));
        }
    }
}