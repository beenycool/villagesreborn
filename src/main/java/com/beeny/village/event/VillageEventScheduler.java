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

/**
 * Handles automatic scheduling and generation of cultural events for villages
 */
public class VillageEventScheduler {
    private static final VillageEventScheduler INSTANCE = new VillageEventScheduler();
    
    // Maps village BlockPos to their next scheduled event time (in system millis)
    private final Map<BlockPos, Long> nextEventTimes = new ConcurrentHashMap<>();
    
    // Maps village BlockPos to their event frequency in minutes
    private final Map<BlockPos, Integer> eventFrequencies = new ConcurrentHashMap<>();
    
    // Maps village BlockPos to their event radius
    private final Map<BlockPos, Integer> eventRadii = new ConcurrentHashMap<>();
    
    // Minimum time between event checks (in milliseconds)
    private static final long MIN_CHECK_INTERVAL = 1000 * 60; // 1 minute
    
    // Last time we checked for new events
    private long lastCheckTime = 0;
    
    // Random for event generation
    private final Random random = new Random();
    
    // Event types by culture
    private final Map<String, List<EventTemplate>> eventTemplatesByCulture = new HashMap<>();
    
    private VillageEventScheduler() {
        initializeEventTemplates();
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
        
        public EventTemplate(String type, String[] descriptions, int weight, int baseMinDuration, int baseMaxDuration) {
            this.type = type;
            this.descriptions = descriptions;
            this.weight = weight;
            this.baseMinDuration = baseMinDuration;
            this.baseMaxDuration = baseMaxDuration;
        }
    }
    
    /**
     * Initialize event templates for each culture
     */
    private void initializeEventTemplates() {
        // Roman events
        List<EventTemplate> romanEvents = new ArrayList<>();
        romanEvents.add(new EventTemplate(
            "festival",
            new String[] {
                "Festival of Jupiter", 
                "Roman Market Festival", 
                "Festival of Sol Invictus",
                "Roman Games",
                "Bacchanalia"
            },
            100, 60, 180
        ));
        romanEvents.add(new EventTemplate(
            "ceremony",
            new String[] {
                "Roman Military Triumph", 
                "Senate Ceremony", 
                "Vestal Rites",
                "Gladiatorial Ceremony"
            },
            70, 45, 120
        ));
        romanEvents.add(new EventTemplate(
            "ritual",
            new String[] {
                "Temple Offering Ritual", 
                "Feast of Venus", 
                "Saturnalia Celebration",
                "Votive Ceremony"
            },
            50, 30, 90
        ));
        romanEvents.add(new EventTemplate(
            "competition",
            new String[] {
                "Gladiator Tournament", 
                "Chariot Race", 
                "Roman Athletic Contest",
                "Military Training Contest"
            },
            80, 60, 150
        ));
        eventTemplatesByCulture.put("roman", romanEvents);
        
        // Egyptian events
        List<EventTemplate> egyptianEvents = new ArrayList<>();
        egyptianEvents.add(new EventTemplate(
            "festival",
            new String[] {
                "Festival of the Nile", 
                "Opet Festival", 
                "Festival of Hathor",
                "Feast of Bastet",
                "Celebration of the Pharaoh"
            },
            100, 60, 180
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
        egyptianEvents.add(new EventTemplate(
            "ritual",
            new String[] {
                "Rite of Osiris", 
                "Solar Deity Worship", 
                "Blessing of the Fields",
                "Ritual of the Afterlife"
            },
            60, 30, 90
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
        eventTemplatesByCulture.put("egyptian", egyptianEvents);
        
        // Victorian events
        List<EventTemplate> victorianEvents = new ArrayList<>();
        victorianEvents.add(new EventTemplate(
            "festival",
            new String[] {
                "Victorian Garden Party", 
                "Industrial Exhibition", 
                "Christmas Market",
                "Grand Ball",
                "Spring Fair"
            },
            100, 60, 180
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
        victorianEvents.add(new EventTemplate(
            "competition",
            new String[] {
                "Cricket Match", 
                "Horse Race", 
                "Poetry Contest",
                "Scientific Demonstration"
            },
            70, 60, 150
        ));
        eventTemplatesByCulture.put("victorian", victorianEvents);
        
        // NYC events
        List<EventTemplate> nycEvents = new ArrayList<>();
        nycEvents.add(new EventTemplate(
            "festival",
            new String[] {
                "Broadway Street Party", 
                "Rooftop Celebration", 
                "Cultural Diversity Festival",
                "Downtown Music Festival",
                "Art Gallery Opening"
            },
            100, 60, 180
        ));
        nycEvents.add(new EventTemplate(
            "market",
            new String[] {
                "Farmers Market", 
                "Stock Exchange Day", 
                "Fashion Week Market",
                "Street Food Fair"
            },
            90, 45, 120
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
        
        // Schedule the first event
        scheduleNextEvent(center);
    }
    
    /**
     * Schedule the next event for a village
     */
    private void scheduleNextEvent(BlockPos center) {
        int frequency = eventFrequencies.getOrDefault(center, 180); // Default to 3 hours
        
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
                    
                    // Create an event
                    createRandomEvent(world, villageCenter, culture, radius);
                    
                    // Schedule next event
                    scheduleNextEvent(villageCenter);
                }
            }
        }
        
        // Update all existing events
        VillageEvent.updateAllEvents(world);
    }
    
    /**
     * Create a random event for a village
     */
    private void createRandomEvent(World world, BlockPos center, String culture, int radius) {
        // Get event templates for this culture
        List<EventTemplate> templates = eventTemplatesByCulture.get(culture.toLowerCase());
        if (templates == null || templates.isEmpty()) {
            return;
        }
        
        // Select a random event template based on weights
        EventTemplate selectedTemplate = selectRandomTemplate(templates);
        
        // Pick a random description
        String description = selectedTemplate.descriptions[random.nextInt(selectedTemplate.descriptions.length)];
        
        // Calculate duration based on village development level
        VillageInfluenceManager.VillageDevelopmentData village = 
            VillageInfluenceManager.getInstance().getVillageDevelopment(center);
        
        int durationMinutes = selectedTemplate.baseMinDuration;
        if (village != null) {
            int villageLevel = village.getVillageLevel();
            // Larger duration for higher level villages
            durationMinutes += (villageLevel - 1) * 30;
            durationMinutes = Math.min(durationMinutes, selectedTemplate.baseMaxDuration);
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
        
        // Notify nearby players about the new event
        notifyNearbyPlayers(world, center, radius * 2, player -> {
            player.sendMessage(Text.of("§6§lNew Event: " + description + "§r"), false);
            player.sendMessage(Text.of("§eA " + culture + " " + 
                selectedTemplate.type + " has begun nearby!§r"), false);
            player.sendMessage(Text.of("§eJoin in to earn reputation and rewards.§r"), false);
        });
    }
    
    /**
     * Select a random event template based on weights
     */
    private EventTemplate selectRandomTemplate(List<EventTemplate> templates) {
        int totalWeight = 0;
        for (EventTemplate template : templates) {
            totalWeight += template.weight;
        }
        
        int randomValue = random.nextInt(totalWeight);
        int currentWeight = 0;
        
        for (EventTemplate template : templates) {
            currentWeight += template.weight;
            if (randomValue < currentWeight) {
                return template;
            }
        }
        
        // Fallback
        return templates.get(0);
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
}