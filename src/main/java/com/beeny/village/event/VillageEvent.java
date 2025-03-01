package com.beeny.village.event;

import com.beeny.village.VillageInfluenceManager;
import com.beeny.village.artifacts.CulturalArtifactSystem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.server.world.ServerWorld;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a cultural village event that players can participate in
 */
public class VillageEvent {
    // Map of active events by ID
    private static final Map<String, VillageEvent> activeEvents = new ConcurrentHashMap<>();
    
    private final String id;
    private final String type;
    private final String culture;
    private final String description;
    private final BlockPos location;
    private final int radius;
    private final long startTime;
    private final long duration; // in milliseconds
    private final Set<UUID> participants = new HashSet<>();
    private boolean completed = false;
    private final Map<String, Object> eventData = new HashMap<>();
    private String outcome; // Added outcome field

    /**
     * Create a new village event
     */
    public VillageEvent(String id, String type, String culture, String description, 
                       BlockPos location, int radius, long duration) {
        this.id = id;
        this.type = type;
        this.culture = culture;
        this.description = description;
        this.location = location;
        this.radius = radius;
        this.startTime = System.currentTimeMillis();
        this.duration = duration;
    }
    
    public String getId() { return id; }
    public String getType() { return type; }
    public String getCulture() { return culture; }
    public String getDescription() { return description; }
    public BlockPos getLocation() { return location; }
    public int getRadius() { return radius; }
    public long getStartTime() { return startTime; }
    public long getDuration() { return duration; }
    public boolean isCompleted() { return completed; }
    
    /**
     * Get the outcome of this event
     * @return Event outcome description
     */
    public String getOutcome() {
        return outcome;
    }
    
    /**
     * Set the outcome of this event
     * @param outcome Event outcome description
     */
    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }
    
    /**
     * Check if the event has expired based on its duration
     */
    public boolean isExpired() {
        return System.currentTimeMillis() > startTime + duration;
    }
    
    /**
     * Check if a position is within this event's area
     */
    public boolean isInEventArea(BlockPos pos) {
        return pos.isWithinDistance(location, radius);
    }
    
    /**
     * Get all participants of this event
     */
    public Set<UUID> getParticipants() {
        return Collections.unmodifiableSet(participants);
    }
    
    /**
     * Add a participant to this event
     */
    public void addParticipant(ServerPlayerEntity player) {
        UUID playerId = player.getUuid();
        if (!participants.contains(playerId)) {
            participants.add(playerId);
            // Register player with event participation system
            PlayerEventParticipation.getInstance().joinEvent(player, this);
        }
    }
    
    /**
     * Get the amount of time remaining for this event (in milliseconds)
     */
    public long getTimeRemaining() {
        long endTime = startTime + duration;
        long currentTime = System.currentTimeMillis();
        return Math.max(0, endTime - currentTime);
    }
    
    /**
     * Get the progress of this event (0.0 to 1.0)
     */
    public double getProgress() {
        return 1.0 - ((double)getTimeRemaining() / duration);
    }
    
    /**
     * Store additional data for this event
     */
    public void setEventData(String key, Object value) {
        eventData.put(key, value);
    }
    
    /**
     * Get additional data for this event
     */
    public Object getEventData(String key) {
        return eventData.get(key);
    }
    
    /**
     * Mark this event as completed (before its natural expiration)
     */
    public void complete() {
        if (!completed) {
            completed = true;
            
            // Additional completion logic could go here
        }
    }
    
    /**
     * Update this event (called periodically)
     */
    public void update(World world) {
        // Check if event should end
        if (isExpired() && !completed) {
            completed = true;
        }
        
        // Spawn ambient particles around the event area
        if (world instanceof ServerWorld serverWorld && !completed) {
            spawnEventParticles(serverWorld);
        }
    }
    
    /**
     * Spawn ambient particles to mark the event area
     */
    private void spawnEventParticles(ServerWorld world) {
        // Only spawn particles occasionally to reduce load
        if (random.nextInt(10) != 0) {
            return;
        }
        
        // Spawn particles at random locations around the event center
        for (int i = 0; i < 3; i++) {
            double offsetX = (random.nextDouble() - 0.5) * radius * 2;
            double offsetZ = (random.nextDouble() - 0.5) * radius * 2;
            
            // Keep within radius
            if (offsetX * offsetX + offsetZ * offsetZ > radius * radius) {
                continue;
            }
            
            // Fixed: Convert double to int before adding to BlockPos
            BlockPos particlePos = location.add((int)offsetX, 0, (int)offsetZ);
            
            // Find the ground level
            BlockPos groundPos = world.getTopPosition(
                net.minecraft.world.Heightmap.Type.WORLD_SURFACE, 
                new BlockPos(particlePos.getX(), 0, particlePos.getZ())
            );
            
            world.spawnParticles(
                getCultureParticle(),
                groundPos.getX() + 0.5, 
                groundPos.getY() + 1.5, 
                groundPos.getZ() + 0.5,
                1, 0.2, 0.2, 0.2, 0.02
            );
        }
    }
    
    // Random for various randomized aspects of events
    private final Random random = new Random();
    
    /**
     * Get appropriate particle type based on culture
     */
    private ParticleEffect getCultureParticle() {
        // Fixed: Return appropriate ParticleEffect types
        return switch(culture.toLowerCase()) {
            case "roman" -> ParticleTypes.COMPOSTER; // Changed from ENTITY_EFFECT to COMPOSTER
            case "egyptian" -> ParticleTypes.ENCHANT;
            case "victorian" -> ParticleTypes.SMOKE;
            case "nyc" -> ParticleTypes.FIREWORK;
            default -> ParticleTypes.END_ROD;
        };
    }
    
    /**
     * Reward players for completing this event with bonus rewards
     */
    public void giveCompletionRewards() {
        for (UUID playerId : participants) {
            ServerPlayerEntity player = getPlayerById(playerId);
            if (player != null) {
                // Check if player has completed the event
                Collection<PlayerEventParticipation.EventParticipationData> playerEvents = 
                    PlayerEventParticipation.getInstance().getPlayerEvents(player);
                
                boolean completedThisEvent = false;
                for (PlayerEventParticipation.EventParticipationData data : playerEvents) {
                    if (data.eventId.equals(id) && data.completed) {
                        completedThisEvent = true;
                        break;
                    }
                }
                
                if (completedThisEvent) {
                    // Chance for bonus artifact reward
                    if (random.nextFloat() < 0.3f) { // 30% chance
                        ItemStack artifact = CulturalArtifactSystem.getInstance()
                            .generateEventArtifact(player, culture, type);
                            
                        if (artifact != null) {
                            if (!player.giveItemStack(artifact)) {
                                player.dropItem(artifact, false);
                            }
                            
                            player.sendMessage(Text.of("§d§lYou discovered a cultural artifact: " + 
                                artifact.getName().getString() + "!§r"), false);
                        }
                    }
                    
                    // Register village development contribution
                    BlockPos nearestVillage = VillageInfluenceManager.getInstance().findNearestVillage(location);
                    if (nearestVillage != null) {
                        VillageInfluenceManager.VillageDevelopmentData village = 
                            VillageInfluenceManager.getInstance().getVillageDevelopment(nearestVillage);
                        
                        if (village != null && village.getCulture().equalsIgnoreCase(culture)) {
                            int developmentPoints = getDevelopmentPointsForEvent();
                            village.addDevelopmentPoints(developmentPoints);
                            
                            player.sendMessage(Text.of("§aYour event participation added " + 
                                developmentPoints + " development points to the " + 
                                culture + " village!§r"), false);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Get development points awarded for completing this event
     */
    private int getDevelopmentPointsForEvent() {
        int basePoints = switch(type.toLowerCase()) {
            case "festival", "celebration", "feast" -> 15;
            case "ritual", "ceremony" -> 20;
            case "market", "fair", "trading" -> 12;
            case "competition", "game", "contest" -> 18;
            default -> 10;
        };
        
        // Adjust based on event duration
        if (duration > 7_200_000) { // Events longer than 2 hours
            basePoints *= 1.5;
        }
        
        return (int)basePoints;
    }
    
    /**
     * Get a player by UUID from any server
     */
    private ServerPlayerEntity getPlayerById(UUID playerId) {
        // This would need to be implemented properly in a real environment
        // Typically would use ServerWorld.getServer().getPlayerManager().getPlayer(playerId)
        return null;
    }
    
    /**
     * Create a new cultural event
     */
    public static VillageEvent createEvent(String type, String culture, String description, 
                                        BlockPos location, int radius, long durationMinutes) {
        String eventId = generateEventId(culture, type);
        long durationMs = durationMinutes * 60 * 1000; // Convert minutes to milliseconds
        
        VillageEvent event = new VillageEvent(
            eventId, type, culture, description, location, radius, durationMs
        );
        
        registerEvent(event);
        return event;
    }
    
    /**
     * Generate a unique event ID
     */
    private static String generateEventId(String culture, String type) {
        return culture.toLowerCase() + "_" + type.toLowerCase() + "_" + System.currentTimeMillis();
    }
    
    /**
     * Register an event in the active events map
     */
    public static void registerEvent(VillageEvent event) {
        activeEvents.put(event.getId(), event);
    }
    
    /**
     * Unregister an event from the active events map
     */
    public static void unregisterEvent(String eventId) {
        activeEvents.remove(eventId);
    }
    
    /**
     * Get an event by its ID
     */
    public static VillageEvent getEvent(String eventId) {
        return activeEvents.get(eventId);
    }
    
    /**
     * Get all active events
     */
    public static Collection<VillageEvent> getActiveEvents() {
        return Collections.unmodifiableCollection(activeEvents.values());
    }
    
    /**
     * Find events near a position
     */
    public static List<VillageEvent> findEventsNear(BlockPos pos, int maxDistance) {
        List<VillageEvent> nearbyEvents = new ArrayList<>();
        
        for (VillageEvent event : activeEvents.values()) {
            if (!event.isCompleted() && !event.isExpired() && 
                event.getLocation().getSquaredDistance(pos) <= maxDistance * maxDistance) {
                nearbyEvents.add(event);
            }
        }
        
        return nearbyEvents;
    }
    
    /**
     * Update all active events
     */
    public static void updateAllEvents(World world) {
        List<String> expiredEventIds = new ArrayList<>();
        
        for (VillageEvent event : activeEvents.values()) {
            event.update(world);
            
            if (event.isCompleted() || event.isExpired()) {
                // Handle event completion
                event.giveCompletionRewards();
                expiredEventIds.add(event.getId());
            }
        }
        
        // Remove expired events
        for (String eventId : expiredEventIds) {
            unregisterEvent(eventId);
        }
    }
    
    /**
     * Find events for a specific culture
     */
    public static List<VillageEvent> findEventsByCulture(String culture) {
        List<VillageEvent> cultureEvents = new ArrayList<>();
        
        for (VillageEvent event : activeEvents.values()) {
            if (!event.isCompleted() && !event.isExpired() && 
                event.getCulture().equalsIgnoreCase(culture)) {
                cultureEvents.add(event);
            }
        }
        
        return cultureEvents;
    }

    /**
     * Check if the event is active based on the given world time
     * 
     * @param currentTime The current world time in ticks
     * @return Whether the event is still active
     */
    public boolean isActive(long currentTime) {
        return !isCompleted() && !isExpired();
    }

    /**
     * Builder class for creating VillageEvent objects
     */
    public static class Builder {
        private String id = UUID.randomUUID().toString();
        private String type;
        private String culture;
        private String description;
        private BlockPos location;
        private int radius = 20; // Default radius
        private long startTime;
        private long duration = 60 * 60 * 1000; // 1 hour default
        private final Set<UUID> participants = new HashSet<>();
        private String district;
        private String outcome;

        /**
         * Set the event type
         */
        public Builder type(String type) {
            this.type = type;
            return this;
        }

        /**
         * Set the event culture
         */
        public Builder culture(String culture) {
            this.culture = culture;
            return this;
        }

        /**
         * Set the event description
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Set the event location
         */
        public Builder location(BlockPos location) {
            this.location = location;
            return this;
        }

        /**
         * Set the event radius
         */
        public Builder radius(int radius) {
            this.radius = radius;
            return this;
        }

        /**
         * Set the event start time in world ticks
         */
        public Builder startTime(long startTime) {
            this.startTime = startTime;
            return this;
        }

        /**
         * Set the event duration in milliseconds
         */
        public Builder duration(long duration) {
            this.duration = duration;
            return this;
        }

        /**
         * Add participants by UUID list
         */
        public Builder participants(List<UUID> participants) {
            this.participants.addAll(participants);
            return this;
        }

        /**
         * Set the event district
         */
        public Builder district(String district) {
            this.district = district;
            return this;
        }

        /**
         * Set the event outcome
         */
        public Builder outcome(String outcome) {
            this.outcome = outcome;
            return this;
        }

        /**
         * Build the VillageEvent object
         */
        public VillageEvent build() {
            // Use existing eventId with culture and type if available
            if (culture != null && type != null) {
                this.id = generateEventId(culture, type);
            }
            
            VillageEvent event = new VillageEvent(
                id, type, culture, description, location, radius, duration
            );
            
            // Add participants
            // Static method can't access instance methods directly, use VillageEvent's static method
            for (UUID uuid : participants) {
                // We can't add participants directly here since we can't access instance method getPlayerById
                // Instead, we'll store the UUIDs and let the caller handle participant addition
                event.participants.add(uuid);
            }
            
            // Set additional data
            if (district != null) {
                event.setEventData("district", district);
            }
            if (outcome != null) {
                event.setOutcome(outcome);
            }
            
            // Register event
            registerEvent(event);
            
            return event;
        }
    }
}
