package com.beeny.villagesreborn.core.ai;

import com.beeny.villagesreborn.core.ai.memory.*;
import com.beeny.villagesreborn.core.common.NBTCompound;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Enhanced MemoryBank implementation supporting complex narratives and long-term memories
 * Stores detailed memories including events, interactions, relationships, and personal stories
 */
public class MemoryBank {
    private static final int MAX_SIGNIFICANT_MEMORIES = 50;
    private static final int MAX_VILLAGE_EVENTS = 30;
    private static final int MAX_PERSONAL_STORIES = 20;
    private static final int DEFAULT_MEMORY_LIMIT = 150;
    
    private int memoryLimit = DEFAULT_MEMORY_LIMIT;
    
    // Basic memory storage (legacy support)
    private final List<String> significantMemories = new ArrayList<>();
    
    // Enhanced memory structures
    private final List<VillageEvent> villageEvents = new ArrayList<>();
    private final Map<UUID, List<DetailedInteraction>> playerInteractions = new HashMap<>();
    private final Map<UUID, VillagerRelationshipMemory> villagerRelationships = new HashMap<>();
    private final List<PersonalStory> personalStories = new ArrayList<>();
    private final List<TraumaticEvent> traumaticEvents = new ArrayList<>();
    private final List<JoyfulMemory> joyfulMemories = new ArrayList<>();
    
    // Memory categorization
    private final Map<MemoryCategory, List<Memory>> categorizedMemories = new EnumMap<>(MemoryCategory.class);
    
    public MemoryBank() {
        // Initialize categorized memory storage
        for (MemoryCategory category : MemoryCategory.values()) {
            categorizedMemories.put(category, new ArrayList<>());
        }
    }

    public NBTCompound toNBT() {
        NBTCompound nbt = new NBTCompound();
        nbt.putInt("memoryLimit", memoryLimit);
        nbt.putInt("significantMemories_count", significantMemories.size());
        for (int i = 0; i < significantMemories.size(); i++) {
            nbt.putString("significantMemories_" + i, significantMemories.get(i));
        }
        return nbt;
    }

    public static MemoryBank fromNBT(NBTCompound nbt) {
        MemoryBank bank = new MemoryBank();
        if (nbt.contains("memoryLimit")) {
            bank.setMemoryLimit(nbt.getInt("memoryLimit"));
        }
        int memoriesCount = nbt.getInt("significantMemories_count");
        for (int i = 0; i < memoriesCount; i++) {
            if (nbt.contains("significantMemories_" + i)) {
                bank.addSignificantMemory(nbt.getString("significantMemories_" + i));
            }
        }
        return bank;
    }

    // Legacy support methods
    public List<String> getSignificantMemories() {
        return new ArrayList<>(significantMemories);
    }

    public void addSignificantMemory(String memory) {
        significantMemories.add(memory);
        if (significantMemories.size() > MAX_SIGNIFICANT_MEMORIES) {
            significantMemories.remove(0);
        }
    }
    
    // Enhanced memory methods
    
    /**
     * Records a significant village event that affects the entire community
     */
    public void recordVillageEvent(String eventType, String description, List<UUID> participantsInvolved, EventImpact impact) {
        VillageEvent event = new VillageEvent(
            eventType,
            description,
            LocalDateTime.now(),
            new ArrayList<>(participantsInvolved),
            impact
        );
        
        villageEvents.add(event);
        addCategorizedMemory(MemoryCategory.VILLAGE_EVENTS, new Memory(eventType, description, LocalDateTime.now(), impact.getIntensity()));
        
        // Trim old events
        if (villageEvents.size() > MAX_VILLAGE_EVENTS) {
            villageEvents.remove(0);
        }
    }
    
    /**
     * Records a detailed interaction with a specific player
     */
    public void recordPlayerInteraction(UUID playerUUID, String playerName, String interactionType, 
                                       String context, String villagerResponse, float sentimentChange) {
        DetailedInteraction interaction = new DetailedInteraction(
            playerUUID,
            playerName,
            interactionType,
            context,
            villagerResponse,
            LocalDateTime.now(),
            sentimentChange
        );
        
        playerInteractions.computeIfAbsent(playerUUID, k -> new ArrayList<>()).add(interaction);
        addCategorizedMemory(MemoryCategory.PLAYER_INTERACTIONS, 
            new Memory(interactionType, String.format("Spoke with %s: %s", playerName, context), 
                      LocalDateTime.now(), Math.abs(sentimentChange)));
    }
    
    /**
     * Records or updates a relationship with another villager
     */
    public void recordVillagerRelationship(UUID villagerUUID, String villagerName, 
                                         RelationshipType relationshipType, String lastInteraction, 
                                         float relationshipStrength) {
        VillagerRelationshipMemory relationship = new VillagerRelationshipMemory(
            villagerUUID,
            villagerName,
            relationshipType,
            lastInteraction,
            LocalDateTime.now(),
            relationshipStrength
        );
        
        villagerRelationships.put(villagerUUID, relationship);
        addCategorizedMemory(MemoryCategory.RELATIONSHIPS, 
            new Memory("villager_relationship", String.format("%s with %s: %s", 
                      relationshipType.name(), villagerName, lastInteraction), 
                      LocalDateTime.now(), relationshipStrength));
    }
    
    /**
     * Adds a personal story or significant life event to the villager's narrative
     */
    public void addPersonalStory(String title, String narrative, List<String> keyCharacters, 
                                EmotionalTone tone, boolean isOngoing) {
        PersonalStory story = new PersonalStory(
            title,
            narrative,
            LocalDateTime.now(),
            new ArrayList<>(keyCharacters),
            tone,
            isOngoing
        );
        
        personalStories.add(story);
        addCategorizedMemory(MemoryCategory.PERSONAL_STORIES, 
            new Memory("personal_story", title + ": " + narrative, LocalDateTime.now(), tone.getIntensity()));
        
        // Trim old stories
        if (personalStories.size() > MAX_PERSONAL_STORIES) {
            personalStories.remove(0);
        }
    }
    
    /**
     * Records a traumatic event that may influence future behavior
     */
    public void recordTrauma(String description, String trigger, float severity, List<UUID> involvedEntities) {
        TraumaticEvent trauma = new TraumaticEvent(
            description,
            trigger,
            LocalDateTime.now(),
            severity,
            new ArrayList<>(involvedEntities)
        );
        
        traumaticEvents.add(trauma);
        addCategorizedMemory(MemoryCategory.TRAUMATIC_EVENTS, 
            new Memory("trauma", description, LocalDateTime.now(), severity));
    }
    
    /**
     * Records a joyful memory that can improve mood and relationships
     */
    public void recordJoyfulMemory(String description, List<UUID> sharedWith, float happinessLevel) {
        JoyfulMemory joyfulMemory = new JoyfulMemory(
            description,
            LocalDateTime.now(),
            new ArrayList<>(sharedWith),
            happinessLevel
        );
        
        joyfulMemories.add(joyfulMemory);
        addCategorizedMemory(MemoryCategory.JOYFUL_MEMORIES, 
            new Memory("joy", description, LocalDateTime.now(), happinessLevel));
    }
    
    /**
     * Retrieves memories related to a specific entity (player or villager)
     */
    public List<Memory> getMemoriesAbout(UUID entityUUID) {
        List<Memory> memories = new ArrayList<>();
        
        // Player interactions
        List<DetailedInteraction> interactions = playerInteractions.get(entityUUID);
        if (interactions != null) {
            memories.addAll(interactions.stream()
                .map(i -> new Memory("interaction", i.getContext(), i.getTimestamp(), i.getSentimentChange()))
                .collect(Collectors.toList()));
        }
        
        // Villager relationships
        VillagerRelationshipMemory relationship = villagerRelationships.get(entityUUID);
        if (relationship != null) {
            memories.add(new Memory("relationship", relationship.getLastInteraction(), 
                         relationship.getLastUpdate(), relationship.getRelationshipStrength()));
        }
        
        return memories;
    }
    
    /**
     * Gets memories from a specific time period
     */
    public List<Memory> getMemoriesFromPeriod(LocalDateTime start, LocalDateTime end) {
        return categorizedMemories.values().stream()
            .flatMap(List::stream)
            .filter(memory -> memory.getTimestamp().isAfter(start) && memory.getTimestamp().isBefore(end))
            .sorted((m1, m2) -> m2.getTimestamp().compareTo(m1.getTimestamp()))
            .collect(Collectors.toList());
    }
    
    /**
     * Gets the most emotionally significant memories
     */
    public List<Memory> getMostSignificantMemories(int count) {
        return categorizedMemories.values().stream()
            .flatMap(List::stream)
            .sorted((m1, m2) -> Float.compare(m2.getEmotionalIntensity(), m1.getEmotionalIntensity()))
            .limit(count)
            .collect(Collectors.toList());
    }
    
    /**
     * Gets memories by category
     */
    public List<Memory> getMemoriesByCategory(MemoryCategory category) {
        return new ArrayList<>(categorizedMemories.getOrDefault(category, Collections.emptyList()));
    }
    
    /**
     * Checks if the villager holds a grudge against a specific entity
     */
    public boolean holdsGrudge(UUID entityUUID) {
        return getMemoriesAbout(entityUUID).stream()
            .anyMatch(memory -> memory.getEmotionalIntensity() < -0.5f);
    }
    
    /**
     * Gets the relationship strength with a specific villager
     */
    public float getRelationshipStrength(UUID villagerUUID) {
        VillagerRelationshipMemory relationship = villagerRelationships.get(villagerUUID);
        return relationship != null ? relationship.getRelationshipStrength() : 0.0f;
    }
    
    /**
     * Generates a summary of the villager's life story for journal entries
     */
    public String generateLifeStorySummary() {
        StringBuilder summary = new StringBuilder();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        
        summary.append("My Life in the Village\n");
        summary.append("=====================\n\n");
        
        // Personal stories
        if (!personalStories.isEmpty()) {
            summary.append("Personal Chronicles:\n");
            for (PersonalStory story : personalStories.stream()
                    .sorted((s1, s2) -> s2.getCreatedAt().compareTo(s1.getCreatedAt()))
                    .limit(5)
                    .collect(Collectors.toList())) {
                summary.append(String.format("- [%s] %s: %s\n", 
                    story.getCreatedAt().format(formatter), story.getTitle(), story.getNarrative()));
            }
            summary.append("\n");
        }
        
        // Significant village events
        if (!villageEvents.isEmpty()) {
            summary.append("Village History I Witnessed:\n");
            for (VillageEvent event : villageEvents.stream()
                    .sorted((e1, e2) -> e2.getTimestamp().compareTo(e1.getTimestamp()))
                    .limit(3)
                    .collect(Collectors.toList())) {
                summary.append(String.format("- [%s] %s: %s\n", 
                    event.getTimestamp().format(formatter), event.getEventType(), event.getDescription()));
            }
            summary.append("\n");
        }
        
        // Important relationships
        if (!villagerRelationships.isEmpty()) {
            summary.append("Important People in My Life:\n");
            villagerRelationships.values().stream()
                .filter(rel -> Math.abs(rel.getRelationshipStrength()) > 0.3f)
                .sorted((r1, r2) -> Float.compare(Math.abs(r2.getRelationshipStrength()), Math.abs(r1.getRelationshipStrength())))
                .limit(3)
                .forEach(rel -> summary.append(String.format("- %s (%s): %s\n", 
                    rel.getVillagerName(), rel.getRelationshipType().name(), rel.getLastInteraction())));
        }
        
        return summary.toString();
    }
    
    private void addCategorizedMemory(MemoryCategory category, Memory memory) {
        List<Memory> categoryMemories = categorizedMemories.get(category);
        categoryMemories.add(memory);
        
        // Maintain a reasonable size for each category
        if (categoryMemories.size() > 20) {
            categoryMemories.remove(0);
        }
    }
    
    // Getters for complex memory structures
    public List<VillageEvent> getVillageEvents() { return new ArrayList<>(villageEvents); }
    public Map<UUID, List<DetailedInteraction>> getPlayerInteractions() { return new HashMap<>(playerInteractions); }
    public Map<UUID, VillagerRelationshipMemory> getVillagerRelationships() { return new HashMap<>(villagerRelationships); }
    public List<PersonalStory> getPersonalStories() { return new ArrayList<>(personalStories); }
    public List<TraumaticEvent> getTraumaticEvents() { return new ArrayList<>(traumaticEvents); }
    public List<JoyfulMemory> getJoyfulMemories() { return new ArrayList<>(joyfulMemories); }
    
    /**
     * Gets the current memory limit
     */
    public int getMemoryLimit() { 
        return memoryLimit; 
    }
    
    /**
     * Sets the memory limit and trims memories if necessary
     */
    public void setMemoryLimit(int memoryLimit) {
        this.memoryLimit = Math.max(50, Math.min(500, memoryLimit)); // Clamp to valid range
        trimMemoriesToLimit();
    }
    
    /**
     * Trims memories to stay within the memory limit
     */
    private void trimMemoriesToLimit() {
        // Calculate total memory usage (simplified approach)
        int totalMemories = significantMemories.size() + villageEvents.size() + personalStories.size();
        
        // If over limit, remove oldest memories first
        while (totalMemories > memoryLimit && !significantMemories.isEmpty()) {
            significantMemories.remove(0);
            totalMemories--;
        }
        
        while (totalMemories > memoryLimit && villageEvents.size() > 10) { // Keep at least 10 village events
            villageEvents.remove(0);
            totalMemories--;
        }
        
        while (totalMemories > memoryLimit && personalStories.size() > 5) { // Keep at least 5 personal stories
            personalStories.remove(0);
            totalMemories--;
        }
    }
}