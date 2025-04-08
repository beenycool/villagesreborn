package com.beeny.village.event;

import com.beeny.village.VillageInfluenceManager;
import com.beeny.config.VillagesConfig; // Added import
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
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
    
    // Maps village BlockPos to their demographic data
    private final Map<BlockPos, VillageDemographics> villageDemographics = new ConcurrentHashMap<>();
    
    // Maps village BlockPos to their happiness level (0-100)
    private final Map<BlockPos, Integer> villageHappiness = new ConcurrentHashMap<>();
    
    // Event complexity level increases as players engage with more events
    private final Map<BlockPos, Integer> eventComplexityLevel = new ConcurrentHashMap<>();
    
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
        public final boolean isCultural; // Added flag
        public final boolean isOutdoor; // Added flag
        
        public EventTemplate(String type, String[] descriptions, int weight,
                           int baseMinDuration, int baseMaxDuration) {
            // Default outdoor based on type
            boolean outdoor = type.equalsIgnoreCase("festival") || type.equalsIgnoreCase("competition") || type.equalsIgnoreCase("market") || type.equalsIgnoreCase("parade");
            this(type, descriptions, weight, baseMinDuration, baseMaxDuration, null, false, null, true, outdoor);
        }
        
        public EventTemplate(String type, String[] descriptions, int weight, 
                           int baseMinDuration, int baseMaxDuration,
                           Map<Season, Integer> seasonalWeights, 
                           boolean requiresSpecialCondition,
                           String specialCondition,
                           boolean isCultural,
                           boolean isOutdoor) { // Added parameter
            this.type = type;
            this.descriptions = descriptions;
            this.weight = weight;
            this.baseMinDuration = baseMinDuration;
            this.baseMaxDuration = baseMaxDuration;
            this.seasonalWeights = seasonalWeights != null ? seasonalWeights : new EnumMap<>(Season.class);
            this.requiresSpecialCondition = requiresSpecialCondition;
            this.specialCondition = specialCondition;
            this.isCultural = isCultural;
            this.isOutdoor = isOutdoor; // Assign flag
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
     * Represents village demographic information
     */
    public static class VillageDemographics {
        private final int totalPopulation;
        private final int childPopulation;
        private final int elderlyPopulation;
        private final Map<String, Integer> professionCounts;
        private final boolean hasLeader;
        
        public VillageDemographics(int totalPopulation, int childPopulation, int elderlyPopulation,
                                  Map<String, Integer> professionCounts, boolean hasLeader) {
            this.totalPopulation = totalPopulation;
            this.childPopulation = childPopulation;
            this.elderlyPopulation = elderlyPopulation;
            this.professionCounts = new HashMap<>(professionCounts);
            this.hasLeader = hasLeader;
        }
        
        public int getTotalPopulation() {
            return totalPopulation;
        }
        
        public int getChildPopulation() {
            return childPopulation;
        }
        
        public int getElderlyPopulation() {
            return elderlyPopulation;
        }
        
        public Map<String, Integer> getProfessionCounts() {
            return Collections.unmodifiableMap(professionCounts);
        }
        
        public boolean hasLeader() {
            return hasLeader;
        }
        
        public int getPopulationByProfession(String profession) {
            return professionCounts.getOrDefault(profession, 0);
        }
        
        public boolean hasProfession(String profession) {
            return professionCounts.containsKey(profession) && professionCounts.get(profession) > 0;
        }
        
        public String getDominantProfession() {
            return professionCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("none");
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
        
        // Low happiness triggers
        List<EventTrigger> lowHappinessTriggers = new ArrayList<>();
        lowHappinessTriggers.add(new EventTrigger(
            "low_happiness",
            "celebration",
            new String[] {
                "Village Morale Boost Festival", 
                "Community Spirit Day", 
                "Harmony Restoration Ceremony"
            },
            150, 220
        ));
        eventTriggersByCondition.put("low_happiness", lowHappinessTriggers);
        
        // High child population triggers
        List<EventTrigger> childPopulationTriggers = new ArrayList<>();
        childPopulationTriggers.add(new EventTrigger(
            "high_child_population",
            "festival",
            new String[] {
                "Children's Day Celebration", 
                "Youth Games Tournament", 
                "Young Villagers Fair"
            },
            120, 150
        ));
        eventTriggersByCondition.put("high_child_population", childPopulationTriggers);
        
        // Scholarly population triggers
        List<EventTrigger> scholarTriggers = new ArrayList<>();
        scholarTriggers.add(new EventTrigger(
            "scholarly_population",
            "ceremony",
            new String[] {
                "Knowledge Symposium", 
                "Scholarly Debate Contest", 
                "Wisdom Exchange Conference"
            },
            90, 170
        ));
        eventTriggersByCondition.put("scholarly_population", scholarTriggers);
        
        // New leader triggers
        List<EventTrigger> newLeaderTriggers = new ArrayList<>();
        newLeaderTriggers.add(new EventTrigger(
            "new_leader",
            "ceremony",
            new String[] {
                "Leadership Inauguration", 
                "Authority Transfer Ceremony", 
                "Leader's Oath Ritual"
            },
            120, 250
        ));
        eventTriggersByCondition.put("new_leader", newLeaderTriggers);
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
        
        // Events for villages with many priests/clerics
        romanEvents.add(new EventTemplate(
            "religious_festival",
            new String[] {
                "Temple Dedication Festival", 
                "Divine Oracle Consultation",
                "Vestal Virgins Ceremony",
                "Jupiter's Blessing Ritual"
            },
            40, 60, 120, null, true, "high_priest_population"
        ));
        
        // Events for villages with many farmers
        romanEvents.add(new EventTemplate(
            "agricultural_ceremony",
            new String[] {
                "Ceres Harvest Ritual", 
                "Field Blessing Ceremony",
                "Seed Planting Festival",
                "First Fruits Offering"
            },
            50, 45, 90, null, true, "high_farmer_population"
        ));
        
        // Events for villages with military focus
        romanEvents.add(new EventTemplate(
            "military_parade",
            new String[] {
                "Legion Triumph Procession", 
                "Arms Display Ceremony",
                "Military Training Exhibition",
                "Victory Commemoration"
            },
            60, 75, 150, null, true, "high_smith_population"
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
                "Royal Hunt", 
                "Scribe Competition", 
                "Chariot Race (Egyptian Style)",
                "Archery Contest"
            },
            50, 60, 120
        ));
        
        // Special condition: Low Nile (Drought)
        egyptianEvents.add(new EventTemplate(
            "drought_ritual",
            new String[] {
                "Prayer to Hapi (Nile God)", 
                "Water Scarcity Ceremony",
                "Offering to Ra for Sun's Mercy"
            },
            30, 45, 90, null, true, "drought"
        ));
        
        // Special condition: High Priest Population
        egyptianEvents.add(new EventTemplate(
            "temple_ceremony",
            new String[] {
                "Grand Temple Procession", 
                "Sacred Animal Blessing",
                "Reading of Sacred Texts",
                "Oracle Consultation Ritual"
            },
            45, 60, 150, null, true, "high_priest_population"
        ));
        
        // Special condition: High Farmer Population
        egyptianEvents.add(new EventTemplate(
            "harvest_festival",
            new String[] {
                "First Harvest Offering", 
                "Nile Flood Thanksgiving",
                "Grain Storage Blessing",
                "Fertility Ritual for Fields"
            },
            55, 45, 120, null, true, "high_farmer_population"
        ));
        
        // Special condition: High Mason Population
        egyptianEvents.add(new EventTemplate(
            "construction_ceremony",
            new String[] {
                "Monument Foundation Ritual", 
                "Tool Blessing Ceremony",
                "Quarry Opening Rite",
                "Completion Celebration"
            },
            50, 75, 180, null, true, "high_mason_population"
        ));
        
        eventTemplatesByCulture.put("egyptian", egyptianEvents);
        
        // Victorian events
        List<EventTemplate> victorianEvents = new ArrayList<>();
        
        // Seasonal adjustments for Victorian events
        Map<Season, Integer> victorianFestivalSeasons = new EnumMap<>(Season.class);
        victorianFestivalSeasons.put(Season.WINTER, 140); // Christmas/Winter festivals
        victorianFestivalSeasons.put(Season.SUMMER, 120); // Summer fairs
        
        victorianEvents.add(new EventTemplate(
            "festival",
            new String[] {
                "Victorian Christmas Market", 
                "Summer Fair", 
                "Queen's Jubilee Celebration",
                "Harvest Festival",
                "May Day Fair"
            },
            100, 90, 240, victorianFestivalSeasons, false, null
        ));
        
        victorianEvents.add(new EventTemplate(
            "ceremony",
            new String[] {
                "Town Hall Meeting", 
                "Factory Opening Ceremony", 
                "Remembrance Day Service",
                "Royal Visit Preparation"
            },
            60, 60, 120
        ));
        
        // Victorian rituals often tied to social events
        Map<Season, Integer> victorianRitualSeasons = new EnumMap<>(Season.class);
        victorianRitualSeasons.put(Season.AUTUMN, 90); // Autumn social season
        
        victorianEvents.add(new EventTemplate(
            "ritual", // More like social rituals
            new String[] {
                "High Tea Gathering", 
                "Formal Ball", 
                "Sunday Church Service",
                "Debating Society Meeting"
            },
            70, 45, 90, victorianRitualSeasons, false, null
        ));
        
        victorianEvents.add(new EventTemplate(
            "competition",
            new String[] {
                "Cricket Match", 
                "Baking Competition", 
                "Scientific Invention Fair",
                "Flower Show"
            },
            50, 60, 150
        ));
        
        // Special condition: Industrial Boom
        victorianEvents.add(new EventTemplate(
            "industrial_expo",
            new String[] {
                "Factory Technology Showcase", 
                "Steam Power Exhibition",
                "New Invention Unveiling"
            },
            40, 90, 180, null, true, "industrial_boom"
        ));
        
        // Special condition: High Scholar Population
        victorianEvents.add(new EventTemplate(
            "academic_lecture",
            new String[] {
                "Scientific Discovery Lecture", 
                "Philosophical Debate",
                "Literary Society Reading",
                "Historical Society Meeting"
            },
            45, 60, 120, null, true, "scholarly_population"
        ));
        
        eventTemplatesByCulture.put("victorian", victorianEvents);
        
        // NYC events
        List<EventTemplate> nycEvents = new ArrayList<>();
        
        // Seasonal adjustments for NYC events
        Map<Season, Integer> nycFestivalSeasons = new EnumMap<>(Season.class);
        nycFestivalSeasons.put(Season.SUMMER, 130); // Summer street fairs, concerts
        nycFestivalSeasons.put(Season.WINTER, 130); // Holiday markets, New Year's
        
        nycEvents.add(new EventTemplate(
            "festival",
            new String[] {
                "Street Fair", 
                "Central Park Concert", 
                "Holiday Market",
                "Food Truck Festival",
                "Neighborhood Block Party"
            },
            100, 120, 300, nycFestivalSeasons, false, null
        ));
        
        nycEvents.add(new EventTemplate(
            "ceremony", // More like public gatherings
            new String[] {
                "Building Inauguration", 
                "Public Art Unveiling", 
                "Political Rally",
                "Memorial Service"
            },
            50, 60, 120
        ));
        
        nycEvents.add(new EventTemplate(
            "ritual", // Modern rituals/routines
            new String[] {
                "Rush Hour Commute", // :)
                "Weekend Brunch Gathering", 
                "Coffee Shop Meetup",
                "Gallery Opening Night"
            },
            60, 30, 60 // Shorter, more frequent 'rituals'
        ));
        
        nycEvents.add(new EventTemplate(
            "competition",
            new String[] {
                "Street Basketball Tournament", 
                "Hot Dog Eating Contest", 
                "Taxi Race (Conceptual)",
                "Stock Market Challenge"
            },
            40, 60, 180
        ));
        
        // Special condition: Financial Boom
        nycEvents.add(new EventTemplate(
            "financial_gala",
            new String[] {
                "Wall Street Gala", 
                "Investment Bankers Ball",
                "Stock Exchange Celebration"
            },
            30, 120, 240, null, true, "financial_boom"
        ));
        
        // Special condition: High Artist Population
        nycEvents.add(new EventTemplate(
            "art_exhibition",
            new String[] {
                "Gallery Opening Night", 
                "Street Art Festival",
                "Avant-Garde Performance",
                "Museum Exhibit Premiere"
            },
            50, 90, 180, null, true, "high_artist_population"
        ));
        
        eventTemplatesByCulture.put("nyc", nycEvents);
        
        // TODO: Add templates for Nether and End cultures
    }
    
    /**
     * Registers a village with the scheduler
     * @param center The center BlockPos of the village
     * @param frequency The base frequency of events in minutes
     * @param radius The radius of the village for event effects
     */
    public void registerVillage(BlockPos center, int frequency, int radius) {
        nextEventTimes.put(center, System.currentTimeMillis() + (long)frequency * 60 * 1000); // Schedule first event
        eventFrequencies.put(center, frequency);
        eventRadii.put(center, radius);
        villageEventHistory.put(center, new ArrayList<>());
        villageConditions.put(center, new HashMap<>());
        villageDemographics.put(center, new VillageDemographics(0, 0, 0, new HashMap<>(), false)); // Initial empty demographics
        villageHappiness.put(center, 50); // Start at neutral happiness
        eventComplexityLevel.put(center, 1); // Start at base complexity
    }
    
    /**
     * Updates the demographic information for a village
     * @param center The center BlockPos of the village
     * @param demographics The new demographic data
     */
    public void updateVillageDemographics(BlockPos center, VillageDemographics demographics) {
        villageDemographics.put(center, demographics);
        
        // Update conditions based on demographics
        Map<String, Object> conditions = villageConditions.computeIfAbsent(center, k -> new HashMap<>());
        
        // Example conditions based on demographics
        conditions.put("high_child_population", demographics.getChildPopulation() > demographics.getTotalPopulation() * 0.3);
        conditions.put("scholarly_population", demographics.getPopulationByProfession("librarian") > 2); // Example threshold
        conditions.put("high_farmer_population", demographics.getPopulationByProfession("farmer") > 5);
        conditions.put("high_priest_population", demographics.getPopulationByProfession("cleric") > 3);
        conditions.put("high_mason_population", demographics.getPopulationByProfession("mason") > 4);
        conditions.put("high_smith_population", 
            demographics.getPopulationByProfession("armorer") + 
            demographics.getPopulationByProfession("weaponsmith") + 
            demographics.getPopulationByProfession("toolsmith") > 5);
        
        // Add more conditions as needed
    }
    
    /**
     * Updates the happiness level for a village
     * @param center The center BlockPos of the village
     * @param happinessLevel The new happiness level (0-100)
     */
    public void updateVillageHappiness(BlockPos center, int happinessLevel) {
        int clampedHappiness = Math.max(0, Math.min(100, happinessLevel));
        villageHappiness.put(center, clampedHappiness);
        
        // Update conditions based on happiness
        Map<String, Object> conditions = villageConditions.computeIfAbsent(center, k -> new HashMap<>());
        conditions.put("low_happiness", clampedHappiness < 30);
        conditions.put("high_happiness", clampedHappiness > 75);
    }
    
    /**
     * Schedules the next event for a specific village
     */
    private void scheduleNextEvent(BlockPos center) {
        // Get the event frequency multiplier from config
        float frequencyMultiplier = VillagesConfig.getInstance().getGameplaySettings().getEventFrequencyMultiplier();
        if (frequencyMultiplier <= 0) frequencyMultiplier = 1.0f; // Avoid division by zero or negative delays

        // Get minimum days between events and convert to milliseconds
        int minDays = VillagesConfig.getInstance().getGameplaySettings().getMinDaysBetweenEvents();
        long minMillisBetweenEvents = TimeUnit.DAYS.toMillis(minDays);

        int baseFrequencyMinutes = eventFrequencies.getOrDefault(center, 60 * 24); // Default to once per Minecraft day
        // Adjust frequency based on multiplier (lower multiplier = longer delay)
        long baseDelayMillis = (long) (baseFrequencyMinutes * 60 * 1000 / frequencyMultiplier);
        long randomizedDelayMillis = (long) (baseDelayMillis * (0.8 + random.nextDouble() * 0.4)); // Add +/- 20% randomness

        long nextEventTime = System.currentTimeMillis() + randomizedDelayMillis;

        // Find the start time of the last event in this village
        // Assuming VillageEvent.findEventsNear can get recent events and their start times
        long lastEventStartTime = VillageEvent.findEventsNear(center, eventRadii.getOrDefault(center, 64))
                                      .stream()
                                      .mapToLong(VillageEvent::getStartTime) // Assuming getStartTime exists
                                      .max()
                                      .orElse(0); // 0 if no previous events

        // Ensure the next event time respects the minimum days constraint
        nextEventTime = Math.max(nextEventTime, lastEventStartTime + minMillisBetweenEvents);

        nextEventTimes.put(center, nextEventTime);

        // Log the scheduled time (optional)
        // System.out.println("Next event for village at " + center + " scheduled in " + 
        //                    (randomizedDelayMillis / 1000 / 60) + " minutes (adjusted for min days).");
    }
    
    /**
     * Checks villages and potentially triggers new events based on schedule and conditions
     */
    public void update(World world) {
        // Check max concurrent events globally first
        int maxConcurrent = VillagesConfig.getInstance().getGameplaySettings().getMaxConcurrentEvents();
        // Need a way to get the current number of active events.
        // Assuming VillageEvent.getActiveEventCount() provides the correct global count.
        int currentActiveEvents = VillageEvent.getActiveEventCount();
        if (currentActiveEvents >= maxConcurrent) {
            return; // Don't schedule new events if limit is reached
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCheckTime < MIN_CHECK_INTERVAL) {
            return; // Don't check too frequently
        }
        lastCheckTime = currentTime; // Update last check time

        // Iterate through registered villages
        for (BlockPos center : nextEventTimes.keySet()) {
            long nextEventTime = nextEventTimes.getOrDefault(center, 0L);
            if (currentTime >= nextEventTime) {
                // Time for a potential event in this village
                String culture = VillageInfluenceManager.getInstance().getCultureAt(center); // Assuming this exists

                // Re-check concurrent events before creating one for *this* village
                if (VillageEvent.getActiveEventCount() >= maxConcurrent) {
                    continue; // Skip this village if limit reached just now
                }

                if (culture != null) {
                    // Try triggering a special event first
                    boolean specialEventTriggered = checkAndTriggerSpecialEvent(world, center, culture, 
                        eventRadii.getOrDefault(center, 64), 
                        villageConditions.getOrDefault(center, new HashMap<>()));
                    
                    // If no special event, try a regular random event
                    if (!specialEventTriggered) {
                        createRandomEvent(world, center, culture, 
                            eventRadii.getOrDefault(center, 64), 
                            getCurrentSeason(world), 
                            villageConditions.getOrDefault(center, new HashMap<>()));
                    }
                    
                    // Schedule the next check for this village
                    scheduleNextEvent(center);
                }
            }
        }
    }
    
    /**
     * Check for and potentially trigger a special event based on village conditions
     * @return true if a special event was triggered
     */
    private boolean checkAndTriggerSpecialEvent(World world, BlockPos center, String culture, 
                                              int radius, Map<String, Object> conditions) {
        if (conditions.isEmpty()) return false;
        
        List<EventTrigger> possibleTriggers = new ArrayList<>();
        for (String conditionKey : conditions.keySet()) {
            if (eventTriggersByCondition.containsKey(conditionKey)) {
                // Check if the condition value is met (if applicable, e.g., boolean true)
                Object conditionValue = conditions.get(conditionKey);
                if (conditionValue instanceof Boolean && !(Boolean)conditionValue) {
                    continue; // Skip if boolean condition is false
                }
                possibleTriggers.addAll(eventTriggersByCondition.get(conditionKey));
            }
        }
        
        if (possibleTriggers.isEmpty()) return false;
        
        // Select trigger based on weight
        int totalWeight = possibleTriggers.stream().mapToInt(t -> t.priorityWeight).sum();
        if (totalWeight <= 0) return false;
        
        int roll = random.nextInt(totalWeight);
        int currentWeight = 0;
        EventTrigger selectedTrigger = null;
        for (EventTrigger trigger : possibleTriggers) {
            currentWeight += trigger.priorityWeight;
            if (roll < currentWeight) {
                selectedTrigger = trigger;
                break;
            }
        }
        
        if (selectedTrigger == null) return false; // Should not happen if totalWeight > 0
        
        // Create the event
        String description = selectedTrigger.eventDescriptions[random.nextInt(selectedTrigger.eventDescriptions.length)];
        long durationTicks = (long)selectedTrigger.durationMinutes * 60 * 20; // Convert minutes to ticks
        
        VillageEvent event = new VillageEvent(
            selectedTrigger.eventType + " (" + getConditionDisplayName(selectedTrigger.triggerCondition) + ")", 
            description, 
            center, 
            radius, 
            durationTicks, 
            culture
        );
        
        VillageEventManager.getInstance().addEvent(event);
        recordEventInHistory(center, selectedTrigger.eventType);
        
        // Notify players
        notifyNearbyPlayers(world, center, radius * 2, player -> {
            player.sendMessage(Text.literal("A special event has started nearby: " + event.getName()), false);
            player.sendMessage(Text.literal(event.getDescription()).fillStyle(Style.EMPTY.withItalic(true)), false);
        });
        
        return true;
    }
    
    /**
     * Gets a user-friendly display name for a condition ID
     */
    private String getConditionDisplayName(String conditionId) {
        return switch (conditionId) {
            case "drought" -> "Drought";
            case "prosperity" -> "Prosperity";
            case "war" -> "War Time";
            case "trade_boom" -> "Trade Boom";
            case "low_happiness" -> "Low Morale";
            case "high_child_population" -> "Many Children";
            case "scholarly_population" -> "Scholarly Focus";
            case "new_leader" -> "New Leader";
            default -> conditionId; // Fallback to ID
        };
    }
    
    /**
     * Creates a random event based on available templates for the culture and conditions
     */
    private void createRandomEvent(World world, BlockPos center, String culture,
                                 int radius, Season currentSeason, Map<String, Object> conditions) {
        List<EventTemplate> templates = eventTemplatesByCulture.get(culture.toLowerCase());
        if (templates == null || templates.isEmpty()) {
            // System.out.println("No event templates found for culture: " + culture);
            return; // No templates for this culture
        }
        
        // Select a template based on adjusted weights
        EventTemplate selectedTemplate = selectRandomTemplate(world, center, culture, templates, currentSeason, conditions);
        if (selectedTemplate == null) {
            // System.out.println("Could not select a valid event template for culture: " + culture);
            return; // No valid template could be selected
        }
        
        // Determine duration
        int minDuration = selectedTemplate.baseMinDuration;
        int maxDuration = selectedTemplate.baseMaxDuration;
        int durationMinutes = minDuration + random.nextInt(maxDuration - minDuration + 1);
        long durationTicks = (long)durationMinutes * 60 * 20; // Convert minutes to ticks
        
        // Select a random description
        String description = selectedTemplate.descriptions[random.nextInt(selectedTemplate.descriptions.length)];
        
        // Create the event
        VillageEvent event = new VillageEvent(
            selectedTemplate.type, 
            description, 
            center, 
            radius, 
            durationTicks, 
            culture
        );
        
        VillageEventManager.getInstance().addEvent(event);
        recordEventInHistory(center, selectedTemplate.type);
        
        // Notify players
        notifyNearbyPlayers(world, center, radius * 2, player -> {
            player.sendMessage(Text.literal("An event has started nearby: " + event.getName()), false);
            player.sendMessage(Text.literal(event.getDescription()).fillStyle(Style.EMPTY.withItalic(true)), false);
        });
        
        // System.out.println("Created event '" + event.getName() + "' for village at " + center);
    }
    
    /**
     * Records an event in the village's history
     */
    private void recordEventInHistory(BlockPos center, String eventType) {
        List<String> history = villageEventHistory.computeIfAbsent(center, k -> new ArrayList<>());
        history.add(eventType);
        if (history.size() > MAX_EVENT_HISTORY) {
            history.remove(0); // Keep history size limited
        }
    }
    
    /**
     * Selects a random event template based on weights adjusted for season and conditions
     */
    private EventTemplate selectRandomTemplate(World world, BlockPos center, String culture, List<EventTemplate> templates,
                                             Season currentSeason, Map<String, Object> conditions) {
        // Imports needed: com.beeny.Villagesreborn, net.minecraft.server.network.ServerPlayerEntity, java.util.UUID
        boolean checkReputation = VillagesConfig.getInstance().getGameplaySettings().isPlayerReputationAffectsEvents();
        double averageReputation = 0;
        int playerCount = 0;

        if (checkReputation && world instanceof ServerWorld serverWorld) {
            double radiusSq = 100 * 100; // Check players within 100 blocks for reputation influence
            List<ServerPlayerEntity> nearbyPlayers = serverWorld.getPlayers(player -> player.getBlockPos().getSquaredDistance(center) <= radiusSq);
            if (!nearbyPlayers.isEmpty()) {
                double totalReputation = 0;
                for (ServerPlayerEntity player : nearbyPlayers) {
                    // Ensure culture is uppercase for getPlayerReputation lookup if needed
                    totalReputation += Villagesreborn.getInstance().getPlayerReputation(player.getUuid(), culture.toUpperCase());
                }
                averageReputation = totalReputation / nearbyPlayers.size();
                playerCount = nearbyPlayers.size();
            }
        }

        // Calculate total adjusted weight
        int totalWeight = 0;
        Map<EventTemplate, Integer> adjustedWeights = new HashMap<>();
        
        for (EventTemplate template : templates) {
            int baseWeight = template.getAdjustedWeight(currentSeason, conditions);
            int adjustedWeight = baseWeight; // Start with base weight

            // Apply reputation modifier if enabled and players are nearby
            if (checkReputation && playerCount > 0 && baseWeight > 0) {
                final double HIGH_REP_THRESHOLD = 50.0;
                final double LOW_REP_THRESHOLD = -30.0;
                final float POSITIVE_EVENT_REP_MODIFIER = 1.3f; // 30% boost for high rep
                final float NEGATIVE_EVENT_REP_MODIFIER = 0.7f; // 30% reduction for low rep

                boolean isPositiveEvent = template.type.equalsIgnoreCase("festival") || template.type.equalsIgnoreCase("celebration");
                // Add other positive/negative event types here if needed

                if (averageReputation >= HIGH_REP_THRESHOLD && isPositiveEvent) {
                    adjustedWeight = (int) (baseWeight * POSITIVE_EVENT_REP_MODIFIER);
                    // System.out.println("Boosting weight for " + template.type + " due to high reputation (" + averageReputation + ")");
                } else if (averageReputation <= LOW_REP_THRESHOLD && isPositiveEvent) {
                    adjustedWeight = (int) (baseWeight * NEGATIVE_EVENT_REP_MODIFIER);
                    // System.out.println("Reducing weight for " + template.type + " due to low reputation (" + averageReputation + ")");
                }
                // Ensure weight doesn't become negative
                adjustedWeight = Math.max(1, adjustedWeight);
            }

            // Apply cultural event multiplier if applicable
            if (template.isCultural && baseWeight > 0) { // Check baseWeight > 0 to avoid multiplying zero
                float culturalMultiplier = VillagesConfig.getInstance().getGameplaySettings().getCulturalEventMultiplier();
                if (culturalMultiplier > 0) { // Avoid zero/negative multipliers
                    adjustedWeight = (int) (adjustedWeight * culturalMultiplier);
                    // Ensure weight doesn't become zero if multiplier is very small but > 0
                    adjustedWeight = Math.max(1, adjustedWeight);
                    // System.out.println("Applying cultural multiplier (" + culturalMultiplier + ") to " + template.type + ". New weight: " + adjustedWeight);
                }
            }

            // Apply weather modifier if enabled
            if (VillagesConfig.getInstance().getGameplaySettings().isWeatherAffectsEvents() && baseWeight > 0) {
                boolean isBadWeather = world.isRaining() || world.isThundering();
                if (isBadWeather && template.isOutdoor) {
                    final float WEATHER_PENALTY_MULTIPLIER = 0.1f; // Reduce weight significantly in bad weather
                    adjustedWeight = (int) (adjustedWeight * WEATHER_PENALTY_MULTIPLIER);
                    adjustedWeight = Math.max(1, adjustedWeight); // Ensure minimum weight
                    // System.out.println("Applying weather penalty to outdoor event " + template.type + ". New weight: " + adjustedWeight);
                }
            }
            if (adjustedWeight > 0) {
                adjustedWeights.put(template, adjustedWeight);
                totalWeight += adjustedWeight;
            }
        }
        
        if (totalWeight <= 0) {
            return null; // No valid templates to choose from
        }
        
        // Roll the dice
        int roll = random.nextInt(totalWeight);
        int currentWeight = 0;
        
        // Find the selected template
        for (Map.Entry<EventTemplate, Integer> entry : adjustedWeights.entrySet()) {
            currentWeight += entry.getValue();
            if (roll < currentWeight) {
                return entry.getKey();
            }
        }
        
        return null; // Should not happen if totalWeight > 0
    }
    
    /**
     * Determines the current season based on world time
     */
    private Season getCurrentSeason(World world) {
        // Minecraft day is 24000 ticks. Let's assume 30 days per season (720000 ticks)
        long day = world.getTime() / 24000;
        int seasonIndex = (int) ((day / 30) % 4); // 0=Spring, 1=Summer, 2=Autumn, 3=Winter
        
        return switch (seasonIndex) {
            case 0 -> Season.SPRING;
            case 1 -> Season.SUMMER;
            case 2 -> Season.AUTUMN;
            case 3 -> Season.WINTER;
            default -> Season.SPRING; // Fallback
        };
    }
    
    /**
     * Helper to notify players within a certain radius of an event
     */
    private void notifyNearbyPlayers(World world, BlockPos center, int radius, Consumer<ServerPlayerEntity> action) {
        // Check if notifications are enabled
        if (!VillagesConfig.getInstance().getGameplaySettings().isShowEventNotifications()) {
            return; // Don't send notifications if disabled
        }

        if (world instanceof ServerWorld serverWorld) {
            double radiusSq = radius * radius;
            for (ServerPlayerEntity player : serverWorld.getPlayers()) {
                if (player.getBlockPos().getSquaredDistance(center) <= radiusSq) {
                    action.accept(player);
                }
            }
        }
    }
    
    /**
     * Gets the estimated time until the next event for a village (in seconds)
     * Returns -1 if the village is not registered or no event is scheduled.
     */
    public int getTimeUntilNextEvent(BlockPos center) {
        Long nextTime = nextEventTimes.get(center);
        if (nextTime == null) {
            return -1;
        }
        
        long diff = nextTime - System.currentTimeMillis();
        return diff > 0 ? (int) (diff / 1000) : 0;
    }
    
    /**
     * Forces a specific event type to start immediately in a village
     * @param world The world
     * @param center The village center
     * @param eventType The type of event to force (e.g., "festival", "ceremony")
     */
    public void forceEvent(World world, BlockPos center, String eventType) {
        String culture = VillageInfluenceManager.getInstance().getCultureAt(center);
        if (culture == null) {
            System.out.println("Cannot force event: No culture found for village at " + center);
            return;
        }
        
        List<EventTemplate> templates = eventTemplatesByCulture.get(culture.toLowerCase());
        if (templates == null) {
            System.out.println("Cannot force event: No templates for culture " + culture);
            return;
        }
        
        // Find a template matching the type
        EventTemplate selectedTemplate = null;
        for (EventTemplate template : templates) {
            if (template.type.equalsIgnoreCase(eventType)) {
                selectedTemplate = template;
                break;
            }
        }
        
        if (selectedTemplate == null) {
            System.out.println("Cannot force event: No template found for type '" + eventType + "' in culture " + culture);
            return;
        }
        
        // Use the selected template to create the event
        int radius = eventRadii.getOrDefault(center, 64);
        int minDuration = selectedTemplate.baseMinDuration;
        int maxDuration = selectedTemplate.baseMaxDuration;
        int durationMinutes = minDuration + random.nextInt(maxDuration - minDuration + 1);
        long durationTicks = (long)durationMinutes * 60 * 20;
        String description = selectedTemplate.descriptions[random.nextInt(selectedTemplate.descriptions.length)];
        
        VillageEvent event = new VillageEvent(
            selectedTemplate.type, 
            description, 
            center, 
            radius, 
            durationTicks, 
            culture
        );
        
        VillageEventManager.getInstance().addEvent(event);
        recordEventInHistory(center, selectedTemplate.type);
        
        // Notify players
        notifyNearbyPlayers(world, center, radius * 2, player -> {
            player.sendMessage(Text.literal("A forced event has started nearby: " + event.getName()), false);
            player.sendMessage(Text.literal(event.getDescription()).fillStyle(Style.EMPTY.withItalic(true)), false);
        });
        
        // Reschedule next random event
        scheduleNextEvent(center);
        System.out.println("Forced event '" + event.getName() + "' for village at " + center);
    }
    
    /**
     * Increases the complexity level for events in a village
     * @param center The village center
     */
    public void increaseEventComplexity(BlockPos center) {
        int currentLevel = eventComplexityLevel.getOrDefault(center, 1);
        // Increase complexity, maybe cap it?
        eventComplexityLevel.put(center, Math.min(5, currentLevel + 1)); 
        // System.out.println("Increased event complexity for village at " + center + " to level " + (currentLevel + 1));
    }
    
    /**
     * Gets the current event complexity level for a village
     * @param center The village center
     * @return The complexity level (1-5, default 1)
     */
    // public int getEventComplexityLevel(BlockPos center) {
    //     return eventComplexityLevel.getOrDefault(center, 1);
    // }
    
    /**
     * Adds or updates a special condition for a village
     * @param center The village center
     * @param condition The condition ID (e.g., "drought", "prosperity")
     * @param value The value of the condition (often just true/false, but could be numeric)
     */
    public void addVillageCondition(BlockPos center, String condition, Object value) {
        villageConditions.computeIfAbsent(center, k -> new HashMap<>()).put(condition, value);
    }
    
    /**
     * Removes a special condition from a village
     * @param center The village center
     * @param condition The condition ID to remove
     */
    public void removeVillageCondition(BlockPos center, String condition) {
        Map<String, Object> conditions = villageConditions.get(center);
        if (conditions != null) {
            conditions.remove(condition);
        }
    }
    
    /**
     * Checks if a village has a specific condition
     * @param center The village center
     * @param condition The condition ID to check
     * @return true if the condition exists (and is typically true if boolean)
     */
    public boolean hasVillageCondition(BlockPos center, String condition) {
        Map<String, Object> conditions = villageConditions.get(center);
        if (conditions != null && conditions.containsKey(condition)) {
            Object value = conditions.get(condition);
            // Treat non-boolean or true boolean values as the condition being present
            return !(value instanceof Boolean) || (Boolean) value;
        }
        return false;
    }
    
    /**
     * Records player participation in an event, potentially increasing future complexity
     * @param villageCenter The center of the village where the event occurred
     * @param playerCount The number of players who participated
     */
    public void recordPlayerParticipation(BlockPos villageCenter, int playerCount) {
        if (playerCount > 0) {
            // Simple logic: if players participate, increase complexity slightly
            // More complex logic could depend on success/failure or specific actions
            if (random.nextInt(3) == 0) { // 1 in 3 chance to increase complexity per participation
                increaseEventComplexity(villageCenter);
            }
        }
    }
    
    // Add methods to load/save scheduler state if needed
}
