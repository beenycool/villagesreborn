package com.beeny.village;

public enum Culture {
    ROMAN("roman", "Roman"),
    EGYPTIAN("egyptian", "Egyptian"),
    VICTORIAN("victorian", "Victorian"),
    NYC("nyc", "NYC"),
    NETHER("nether", "Nether"),
    END("end", "End");

    private final String id;
    private final String displayName;

    Culture(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }
    
    // Missing nested types that are referenced by other classes
    
    /**
     * Represents different cultural types for resource generation and crafting
     */
    public enum CultureType {
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
        PROGRESSIVE("progressive", "Progressive"),
        PEACEFUL("peaceful", "Peaceful"),
        AGGRESSIVE("aggressive", "Aggressive"),
        DIPLOMATIC("diplomatic", "Diplomatic"),
        ISOLATIONIST("isolationist", "Isolationist"),
        INDUSTRIOUS("industrious", "Industrious"),
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
