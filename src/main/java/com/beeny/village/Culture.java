package com.beeny.village;

import java.util.*;
import java.time.LocalDate;
import net.minecraft.util.math.BlockPos;
import java.time.temporal.ChronoUnit;

public class Culture {
    private final CultureType type;
    private final CultureType secondaryType;
    private final float hybridRatio;
    private final Set<CulturalTrait> traits;
    private final Set<String> preferredBiomes;
    private final Map<Season, String> seasonalEvents;
    
    // Added fields to fix compilation errors
    private String id; // Unique identifier for the culture
    private BlockPos centerPos; // Center position of the village
    private BlockPos townHallPos; // Town hall position
    private int prosperity = 50; // Default prosperity value
    private int safety = 50; // Default safety value
    private int happiness = 50; // Default happiness value
    private float growthRate = 1.0f; // Default growth rate
    private int tradeActivity = 5; // Default trade activity
    private Map<String, Integer> resourceLevels = new HashMap<>();
    private Map<String, Integer> structures = new HashMap<>();
    private List<VillageEvent> activeEvents = new ArrayList<>();
    private List<VillageEvent> upcomingEvents = new ArrayList<>();
    private List<Villager> villagers = new ArrayList<>();
    private LocalDate foundationDate = LocalDate.now(); // Default foundation date
    
    public Culture(CultureType type) {
        this(type, null, 0.0f);
    }
    
    public Culture(CultureType type, CultureType secondaryType, float hybridRatio) {
        this.type = type;
        this.secondaryType = secondaryType;
        this.hybridRatio = hybridRatio;
        this.traits = new HashSet<>();
        this.preferredBiomes = new HashSet<>();
        this.seasonalEvents = new HashMap<>();
        this.id = UUID.randomUUID().toString(); // Generate a unique ID
    }

    public CultureType getType() {
        return type;
    }

    public CultureType getSecondaryType() {
        return secondaryType;
    }

    public boolean isHybrid() {
        return secondaryType != null;
    }

    public float getHybridRatio() {
        return hybridRatio;
    }

    public Set<CulturalTrait> getTraits() {
        return Collections.unmodifiableSet(traits);
    }

    public void addTrait(CulturalTrait trait) {
        traits.add(trait);
    }

    public void removeTrait(CulturalTrait trait) {
        traits.remove(trait);
    }

    public Set<String> getPreferredBiomes() {
        return Collections.unmodifiableSet(preferredBiomes);
    }

    public void addPreferredBiome(String biome) {
        preferredBiomes.add(biome);
    }
    
    public String getSeasonalEvent(Season season) {
        return seasonalEvents.getOrDefault(season, "Festival");
    }
    
    public void setSeasonalEvent(Season season, String eventName) {
        seasonalEvents.put(season, eventName);
    }
    
    /**
     * Returns the culture as a string for compatibility purposes
     */
    public String getName() {
        return toString();
    }

    @Override
    public String toString() {
        return type.getDisplayName() + (isHybrid() ? "-" + secondaryType.getDisplayName() : "");
    }
    
    /**
     * Returns a string representation of the culture in lowercase for string comparisons
     */
    public String toLowerCase() {
        return toString().toLowerCase();
    }

    /**
     * Get the unique ID of this culture
     */
    public String getId() {
        return id;
    }
    
    /**
     * Set the unique ID of this culture
     */
    public void setId(String id) {
        this.id = id;
    }
    
    /**
     * Get the center position of this village
     */
    public BlockPos getCenterPos() {
        return centerPos;
    }
    
    /**
     * Set the center position of this village
     */
    public void setCenterPos(BlockPos pos) {
        this.centerPos = pos;
    }
    
    /**
     * Get the town hall position
     */
    public BlockPos getTownHallPos() {
        return townHallPos;
    }
    
    /**
     * Set the town hall position
     */
    public void setTownHallPos(BlockPos pos) {
        this.townHallPos = pos;
    }
    
    /**
     * Get the prosperity level of the village (0-100)
     */
    public int getProsperity() {
        return prosperity;
    }
    
    /**
     * Set the prosperity level of the village
     */
    public void setProsperity(int prosperity) {
        this.prosperity = Math.max(0, Math.min(100, prosperity));
    }
    
    /**
     * Get the safety level of the village (0-100)
     */
    public int getSafety() {
        return safety;
    }
    
    /**
     * Set the safety level of the village
     */
    public void setSafety(int safety) {
        this.safety = Math.max(0, Math.min(100, safety));
    }
    
    /**
     * Get the happiness level of the village (0-100)
     */
    public int getHappiness() {
        return happiness;
    }
    
    /**
     * Set the happiness level of the village
     */
    public void setHappiness(int happiness) {
        this.happiness = Math.max(0, Math.min(100, happiness));
    }
    
    /**
     * Get the growth rate of the village
     */
    public float getGrowthRate() {
        return growthRate;
    }
    
    /**
     * Set the growth rate of the village
     */
    public void setGrowthRate(float growthRate) {
        this.growthRate = growthRate;
    }
    
    /**
     * Get the trade activity of the village
     */
    public int getTradeActivity() {
        return tradeActivity;
    }
    
    /**
     * Set the trade activity of the village
     */
    public void setTradeActivity(int tradeActivity) {
        this.tradeActivity = tradeActivity;
    }
    
    /**
     * Get the resource levels of the village
     */
    public Map<String, Integer> getResourceLevels() {
        return Collections.unmodifiableMap(resourceLevels);
    }
    
    /**
     * Set a resource level for the village
     */
    public void setResourceLevel(String resource, int level) {
        resourceLevels.put(resource, level);
    }
    
    /**
     * Get the structures in the village
     */
    public Map<String, Integer> getStructures() {
        return Collections.unmodifiableMap(structures);
    }
    
    /**
     * Add a structure to the village
     */
    public void addStructure(String structureType) {
        structures.put(structureType, structures.getOrDefault(structureType, 0) + 1);
    }
    
    /**
     * Get the active events in the village
     */
    public List<VillageEvent> getActiveEvents() {
        return Collections.unmodifiableList(activeEvents);
    }
    
    /**
     * Add an active event to the village
     */
    public void addActiveEvent(VillageEvent event) {
        activeEvents.add(event);
    }
    
    /**
     * Get the upcoming events in the village
     */
    public List<VillageEvent> getUpcomingEvents() {
        return Collections.unmodifiableList(upcomingEvents);
    }
    
    /**
     * Add an upcoming event to the village
     */
    public void addUpcomingEvent(VillageEvent event) {
        upcomingEvents.add(event);
    }
    
    /**
     * Get the villagers in the village
     */
    public List<Villager> getVillagers() {
        return Collections.unmodifiableList(villagers);
    }
    
    /**
     * Add a villager to the village
     */
    public void addVillager(Villager villager) {
        villagers.add(villager);
    }
    
    /**
     * Get the foundation date of the village
     */
    public LocalDate getFoundationDate() {
        return foundationDate;
    }
    
    /**
     * Set the foundation date of the village
     */
    public void setFoundationDate(LocalDate date) {
        this.foundationDate = date;
    }
    
    /**
     * Get the age of the village in days
     */
    public long getAgeInDays() {
        return ChronoUnit.DAYS.between(foundationDate, LocalDate.now());
    }

    /**
     * Represents different cultural types for resource generation and crafting
     */
    public enum CultureType {
        MEDIEVAL("medieval", "Medieval"),
        GREEK("greek", "Greek"),
        JAPANESE("japanese", "Japanese"),
        MAYAN("mayan", "Mayan"),
        ROMAN("roman", "Roman"),
        EGYPTIAN("egyptian", "Egyptian"),
        VICTORIAN("victorian", "Victorian"),
        MODERN("modern", "Modern"),
        NETHER("nether", "Nether"),
        END("end", "End"),
        HYBRID("hybrid", "Hybrid");
        
        private final String id;
        private final String displayName;
        
        CultureType(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }
        
        public String getId() {
            return id;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * Represents villager professions and skill types
     */
    public enum ProfessionType {
        FARMING("farming", "Farming"),
        MINING("mining", "Mining"),
        FISHING("fishing", "Fishing"),
        CRAFTING("crafting", "Crafting"),
        BREWING("brewing", "Brewing"),
        SMITHING("smithing", "Smithing"),
        TRADING("trading", "Trading"),
        COMBAT("combat", "Combat"),
        LEADERSHIP("leadership", "Leadership"),
        DIPLOMACY("diplomacy", "Diplomacy");
        
        private final String id;
        private final String displayName;
        
        ProfessionType(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }
        
        public String getId() {
            return id;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * Represents cultural personality traits that affect villager behavior
     */
    public enum CulturalTrait {
        TRADITIONAL("traditional", "Traditional"),
        INNOVATIVE("innovative", "Innovative"),
        COMMUNAL("communal", "Communal"),
        INDIVIDUALISTIC("individualistic", "Individualistic"),
        CONSERVATIVE("conservative", "Conservative"),
        ADAPTABLE("adaptable", "Adaptable"),
        CRAFTING_FOCUSED("crafting_focused", "Crafting Focused"),
        ARTISTIC("artistic", "Artistic"),
        SCHOLARLY("scholarly", "Scholarly"),
        SPIRITUAL("spiritual", "Spiritual"),
        // Added missing traits referenced in code
        DIPLOMATIC("diplomatic", "Diplomatic"),
        ISOLATIONIST("isolationist", "Isolationist"),
        INDUSTRIOUS("industrious", "Industrious");
        
        private final String id;
        private final String displayName;
        
        CulturalTrait(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }
        
        public String getId() {
            return id;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * Represents seasons for events and festivals
     */
    public enum Season {
        SPRING("spring", "Spring"),
        SUMMER("summer", "Summer"),
        AUTUMN("autumn", "Autumn"),
        WINTER("winter", "Winter");
        
        private final String id;
        private final String displayName;
        
        Season(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }
        
        public String getId() {
            return id;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
}
