package com.beeny.village.event;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement; // Added import
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/**
 * Manages player participation in cultural events and tracks reputation with different cultures
 */
public class PlayerEventParticipation {
    private static final PlayerEventParticipation INSTANCE = new PlayerEventParticipation();
    
    // Maps player UUIDs to their event participation data
    private final Map<UUID, Set<EventParticipationData>> playerEvents = new ConcurrentHashMap<>();
    
    // Maps player UUIDs to their reputation with each culture
    private final Map<UUID, Map<String, Integer>> playerReputations = new ConcurrentHashMap<>();
    
    // Constants
    private static final int MAX_ACTIVE_EVENTS = 3;
    private static final int EVENT_COMPLETION_THRESHOLD = 2; // Number of activities needed to complete an event
    
    /**
     * Get singleton instance
     */
    public static PlayerEventParticipation getInstance() {
        return INSTANCE;
    }
    
    /**
     * Event participation data
     */
    public static class EventParticipationData {
        public final String eventId;
        public final String culture;
        public final String eventType;
        public final BlockPos eventLocation;
        public final long joinTime;
        public final Set<String> completedActivities = new HashSet<>();
        public boolean completed;
        
        public EventParticipationData(String eventId, String culture, String eventType, BlockPos location) {
            this.eventId = eventId;
            this.culture = culture;
            this.eventType = eventType;
            this.eventLocation = location;
            this.joinTime = System.currentTimeMillis();
            this.completed = false;
        }
        
        /**
         * Check if a specific activity has been completed for this event
         */
        public boolean hasCompletedActivity(String activity) {
            return completedActivities.contains(activity);
        }
    }
    
    /**
     * Join a cultural event
     */
    public void joinEvent(ServerPlayerEntity player, VillageEvent event) {
        UUID playerUUID = player.getUuid();
        
        // Get or create player's events set
        Set<EventParticipationData> events = playerEvents.computeIfAbsent(playerUUID, k -> new HashSet<>());
        
        // Check if player is already participating in this event
        for (EventParticipationData existingEvent : events) {
            if (existingEvent.eventId.equals(event.getId())) {
                player.sendMessage(Text.of("§eYou are already participating in this event.§r"), false);
                return;
            }
        }
        
        // Check if player has too many active events
        int activeCount = 0;
        for (EventParticipationData existingEvent : events) {
            if (!existingEvent.completed) {
                activeCount++;
            }
        }
        
        if (activeCount >= MAX_ACTIVE_EVENTS) {
            player.sendMessage(Text.of("§cYou are already participating in too many events. Complete some first.§r"), false);
            return;
        }
        
        // Create new participation data
        EventParticipationData participation = new EventParticipationData(
            event.getId(),
            event.getCulture(),
            event.getType(),
            event.getLocation()
        );
        
        // Add to player's events
        events.add(participation);
        
        // Send notification
        player.sendMessage(Text.of("§a§lYou've joined: " + event.getDescription() + "§r"), false);
        player.sendMessage(Text.of("§eComplete activities in this area to earn reputation and rewards!§r"), false);
    }
    
    /**
     * Record completion of an activity for an event
     */
    public void recordActivityCompletion(ServerPlayerEntity player, String eventId, String activity) {
        UUID playerUUID = player.getUuid();
        
        // Find matching event participation
        Set<EventParticipationData> events = playerEvents.get(playerUUID);
        if (events == null) {
            return;
        }
        
        EventParticipationData eventData = null;
        for (EventParticipationData data : events) {
            if (data.eventId.equals(eventId)) {
                eventData = data;
                break;
            }
        }
        
        if (eventData == null) {
            return;
        }
        
        // Record activity completion
        if (!eventData.completedActivities.contains(activity)) {
            eventData.completedActivities.add(activity);
            
            // Grant reputation bonus for activity
            addReputationPoints(player, eventData.culture, 2);
            
            // Check if event is now completed
            if (eventData.completedActivities.size() >= EVENT_COMPLETION_THRESHOLD && !eventData.completed) {
                completeEvent(player, eventData);
            }
        }
    }
    
    /**
     * Mark an event as completed and grant rewards
     */
    private void completeEvent(ServerPlayerEntity player, EventParticipationData eventData) {
        eventData.completed = true;
        
        // Calculate reputation gain based on activities completed and culture
        int reputationGain = 5 + (2 * eventData.completedActivities.size());
        
        // Grant reputation
        addReputationPoints(player, eventData.culture, reputationGain);
        
        // Notify player
        player.sendMessage(Text.of("§a§l✓ Event completed: " + getEventDisplayName(eventData) + "§r"), false);
        player.sendMessage(Text.of("§b+" + reputationGain + " Reputation with " + eventData.culture + " culture§r"), false);
    }
    
    /**
     * Get display name for an event
     */
    private String getEventDisplayName(EventParticipationData eventData) {
        // Try to get the actual event
        VillageEvent event = VillageEvent.getEvent(eventData.eventId);
        if (event != null) {
            return event.getDescription();
        }
        
        // Fallback if event is no longer active
        return eventData.culture + " " + eventData.eventType;
    }
    
    /**
     * Add reputation points with a culture
     */
    public void addReputationPoints(ServerPlayerEntity player, String culture, int points) {
        UUID playerUUID = player.getUuid();
        
        // Get or create reputation map
        Map<String, Integer> reputations = playerReputations.computeIfAbsent(playerUUID, k -> new HashMap<>());
        
        // Update reputation
        String cultureLower = culture.toLowerCase();
        int currentRep = reputations.getOrDefault(cultureLower, 0);
        reputations.put(cultureLower, currentRep + points);
        
        // Notify player of significant reputation changes
        if (points >= 5) {
            player.sendMessage(Text.of("§b+" + points + " Reputation with " + culture + " culture§r"), false);
        }
    }
    
    /**
     * Get a player's reputation with a culture
     */
    public int getReputation(PlayerEntity player, String culture) {
        UUID playerUUID = player.getUuid();
        Map<String, Integer> reputations = playerReputations.get(playerUUID);
        
        if (reputations == null) {
            return 0;
        }
        
        return reputations.getOrDefault(culture.toLowerCase(), 0);
    }
    
    /**
     * Get all player events
     */
    public Collection<EventParticipationData> getPlayerEvents(PlayerEntity player) {
        UUID playerUUID = player.getUuid();
        return Collections.unmodifiableSet(playerEvents.getOrDefault(playerUUID, Collections.emptySet()));
    }
    
    /**
     * Get player's active (incomplete) events
     */
    public Collection<EventParticipationData> getActivePlayerEvents(PlayerEntity player) {
        UUID playerUUID = player.getUuid();
        Set<EventParticipationData> events = playerEvents.get(playerUUID);
        
        if (events == null) {
            return Collections.emptyList();
        }
        
        List<EventParticipationData> activeEvents = new ArrayList<>();
        for (EventParticipationData data : events) {
            if (!data.completed) {
                activeEvents.add(data);
            }
        }
        
        return activeEvents;
    }
    
    /**
     * Check if player is near any of their active events
     */
    public boolean isNearActiveEvent(PlayerEntity player) {
        UUID playerUUID = player.getUuid();
        Set<EventParticipationData> events = playerEvents.get(playerUUID);
        
        if (events == null) {
            return false;
        }
        
        BlockPos playerPos = player.getBlockPos();
        
        for (EventParticipationData data : events) {
            if (!data.completed) {
                // Check if player is near this event location
                VillageEvent event = VillageEvent.getEvent(data.eventId);
                
                if (event != null && event.isInEventArea(playerPos)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Clean up expired events for a player
     */
    public void cleanupPlayerEvents(PlayerEntity player) {
        UUID playerUUID = player.getUuid();
        Set<EventParticipationData> events = playerEvents.get(playerUUID);
        
        if (events == null) {
            return;
        }
        
        // Find expired events (events that no longer exist)
        Set<EventParticipationData> toRemove = new HashSet<>();
        for (EventParticipationData data : events) {
            if (data.completed) {
                continue; // Keep completed events in history
            }
            
            VillageEvent event = VillageEvent.getEvent(data.eventId);
            if (event == null || event.isExpired() || event.isCompleted()) {
                toRemove.add(data);
            }
        }
        
        // Remove expired events
        events.removeAll(toRemove);
    }
    
    /**
     * Get player's reputation with all cultures
     */
    public Map<String, Integer> getAllReputations(PlayerEntity player) {
        UUID playerUUID = player.getUuid();
        Map<String, Integer> reputations = playerReputations.get(playerUUID);
        
        if (reputations == null) {
            return Collections.emptyMap();
        }
        
        return Collections.unmodifiableMap(reputations);
    }
    
    /**
     * Get cultural title based on reputation level
     */
    public String getReputationTitle(String culture, int reputation) {
        if (reputation < 0) {
            return "Outsider";
        } else if (reputation < 10) {
            return "Visitor";
        } else if (reputation < 25) {
            return "Guest";
        } else if (reputation < 50) {
            return "Friend";
        } else if (reputation < 100) {
            return "Ally";
        } else if (reputation < 200) {
            return "Trusted";
        } else if (reputation < 500) {
            return "Honored";
        } else {
            return getCultureSpecificTitle(culture);
        }
    }
    
    /**
     * Get culture-specific highest reputation title
     */
    private String getCultureSpecificTitle(String culture) {
        return switch (culture.toLowerCase()) {
            case "roman" -> "Consul";
            case "egyptian" -> "Pharaoh's Advisor";
            case "victorian" -> "Noble";
            case "nyc" -> "City Legend";
            default -> "Cultural Icon";
        };
    }
    
    /**
     * Save player event participation data
     */
    public NbtCompound savePlayerData(UUID playerUUID) {
        NbtCompound nbt = new NbtCompound();
        
        // Save reputations
        Map<String, Integer> reputations = playerReputations.get(playerUUID);
        if (reputations != null) {
            NbtCompound repNbt = new NbtCompound();
            for (Map.Entry<String, Integer> entry : reputations.entrySet()) {
                repNbt.putInt(entry.getKey(), entry.getValue());
            }
            nbt.put("reputations", repNbt);
        }
        
        // Save events
        Set<EventParticipationData> events = playerEvents.get(playerUUID);
        if (events != null) {
            NbtCompound eventsNbt = new NbtCompound();
            int i = 0;
            
            for (EventParticipationData data : events) {
                NbtCompound eventNbt = new NbtCompound();
                eventNbt.putString("id", data.eventId);
                eventNbt.putString("culture", data.culture);
                eventNbt.putString("type", data.eventType);
                eventNbt.putInt("locX", data.eventLocation.getX());
                eventNbt.putInt("locY", data.eventLocation.getY());
                eventNbt.putInt("locZ", data.eventLocation.getZ());
                eventNbt.putLong("joinTime", data.joinTime);
                eventNbt.putBoolean("completed", data.completed);
                
                // Save completed activities
                int j = 0;
                for (String activity : data.completedActivities) {
                    eventNbt.putString("activity" + j, activity);
                    j++;
                }
                eventNbt.putInt("activityCount", j);
                
                eventsNbt.put("event" + i, eventNbt);
                i++;
            }
            
            eventsNbt.putInt("count", i);
            nbt.put("events", eventsNbt);
        }
        
        return nbt;
    }
    
    /**
     * Load player event participation data
     */
    public void loadPlayerData(UUID playerUUID, NbtCompound nbt) {
        // Load reputations
        if (nbt.contains("reputations")) {
            NbtCompound repNbt = nbt.contains("reputations", NbtElement.COMPOUND_TYPE) ? nbt.getCompound("reputations") : new NbtCompound();
            Map<String, Integer> reputations = new HashMap<>();
            
            for (String culture : repNbt.getKeys()) {
                reputations.put(culture, repNbt.contains(culture, NbtElement.INT_TYPE) ? repNbt.getInt(culture) : 0);
            }
            
            playerReputations.put(playerUUID, reputations);
        }
        
        // Load events
        if (nbt.contains("events")) {
            NbtCompound eventsNbt = nbt.contains("events", NbtElement.COMPOUND_TYPE) ? nbt.getCompound("events") : new NbtCompound();
            int count = eventsNbt.contains("count", NbtElement.INT_TYPE) ? eventsNbt.getInt("count") : 0;
            Set<EventParticipationData> events = new HashSet<>();
            
            for (int i = 0; i < count; i++) {
                NbtCompound eventNbt = eventsNbt.contains("event" + i, NbtElement.COMPOUND_TYPE) ? eventsNbt.getCompound("event" + i) : new NbtCompound();
                
                String id = eventNbt.contains("id", NbtElement.STRING_TYPE) ? eventNbt.getString("id") : "";
                String culture = eventNbt.contains("culture", NbtElement.STRING_TYPE) ? eventNbt.getString("culture") : "";
                String type = eventNbt.contains("type", NbtElement.STRING_TYPE) ? eventNbt.getString("type") : "";
                int x = eventNbt.contains("locX", NbtElement.INT_TYPE) ? eventNbt.getInt("locX") : 0;
                int y = eventNbt.contains("locY", NbtElement.INT_TYPE) ? eventNbt.getInt("locY") : 0;
                int z = eventNbt.contains("locZ", NbtElement.INT_TYPE) ? eventNbt.getInt("locZ") : 0;
                BlockPos location = new BlockPos(x, y, z);
                
                EventParticipationData data = new EventParticipationData(id, culture, type, location);
                data.completed = eventNbt.contains("completed", NbtElement.BYTE_TYPE) ? eventNbt.getBoolean("completed") : false;
                
                // Load completed activities
                int activityCount = eventNbt.contains("activityCount", NbtElement.INT_TYPE) ? eventNbt.getInt("activityCount") : 0;
                for (int j = 0; j < activityCount; j++) {
                    String activity = eventNbt.contains("activity" + j, NbtElement.STRING_TYPE) ? eventNbt.getString("activity" + j) : "";
                    data.completedActivities.add(activity);
                }
                
                // Only add if not too old (over 7 days)
                long joinTime = eventNbt.contains("joinTime", NbtElement.LONG_TYPE) ? eventNbt.getLong("joinTime") : 0L;
                if (System.currentTimeMillis() - joinTime < 7 * 24 * 60 * 60 * 1000L) {
                    events.add(data);
                }
            }
            
            playerEvents.put(playerUUID, events);
        }
    }
}