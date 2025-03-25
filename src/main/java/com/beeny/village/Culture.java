package com.beeny.village;

import java.util.*;

public class Culture {
    private final CultureType type;
    private final CultureType secondaryType;
    private final float hybridRatio;
    private final Set<CulturalTrait> traits;
    private final Set<String> preferredBiomes;
    
    public Culture(CultureType type) {
        this(type, null, 0.0f);
    }
    
    public Culture(CultureType type, CultureType secondaryType, float hybridRatio) {
        this.type = type;
        this.secondaryType = secondaryType;
        this.hybridRatio = hybridRatio;
        this.traits = new HashSet<>();
        this.preferredBiomes = new HashSet<>();
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

    @Override
    public String toString() {
        return type.getDisplayName() + (isHybrid() ? "-" + secondaryType.getDisplayName() : "");
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
        SPIRITUAL("spiritual", "Spiritual");
        
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
