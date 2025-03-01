package com.beeny.village;

import net.minecraft.util.Identifier;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.Registry;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.biome.Biome;
import java.util.*;

public class Culture {
    private final CultureType type;
    private final Set<CulturalTrait> traits;
    private final Map<ProfessionType, Integer> professionWeights;
    private final List<String> structureTemplates;
    private final Set<String> preferredBiomes;
    private final Map<String, Float> eventWeights;
    private final List<String> craftingRecipes;
    private final List<String> artifacts;
    // For cultural hybrids
    private final CultureType secondaryType;
    private final float hybridRatio; // 0.0 = pure primary, 1.0 = pure secondary

    public enum CultureType {
        ROMAN("roman", Arrays.asList("plains", "savanna"), 
            Arrays.asList("forum", "bathhouse", "villa", "temple")),
        EGYPTIAN("egyptian", Arrays.asList("desert", "beach"), 
            Arrays.asList("pyramid", "temple", "marketplace", "oasis")),
        VICTORIAN("victorian", Arrays.asList("plains", "forest"), 
            Arrays.asList("mansion", "factory", "park", "station")),
        NYC("nyc", Arrays.asList("plains"), 
            Arrays.asList("skyscraper", "apartment", "store", "park")),
        MEDIEVAL("medieval", Arrays.asList("plains", "forest", "mountains"), 
            Arrays.asList("castle", "cottage", "blacksmith", "tavern")),
        GREEK("greek", Arrays.asList("beach", "plains", "mountains"), 
            Arrays.asList("temple", "agora", "theater", "stadium")),
        JAPANESE("japanese", Arrays.asList("forest", "mountains", "cherry_blossom"), 
            Arrays.asList("pagoda", "dojo", "garden", "shrine")),
        MAYAN("mayan", Arrays.asList("jungle", "forest"), 
            Arrays.asList("pyramid", "ballcourt", "observatory", "palace")),
        NORDIC("nordic", Arrays.asList("snowy_plains", "taiga", "frozen_peaks"), 
            Arrays.asList("longhouse", "mead_hall", "rune_stone", "port"));

        private final String id;
        private final List<String> biomes;
        private final List<String> structures;
        private final Map<Season, String> seasonalEvents;

        CultureType(String id, List<String> biomes, List<String> structures) {
            this.id = id;
            this.biomes = biomes;
            this.structures = structures;
            this.seasonalEvents = initializeSeasonalEvents(id);
        }

        private Map<Season, String> initializeSeasonalEvents(String id) {
            Map<Season, String> events = new HashMap<>();
            switch (id) {
                case "roman" -> {
                    events.put(Season.SPRING, "spring_planting_festival");
                    events.put(Season.SUMMER, "gladiator_games");
                    events.put(Season.AUTUMN, "harvest_celebration");
                    events.put(Season.WINTER, "saturnalia");
                }
                case "egyptian" -> {
                    events.put(Season.SPRING, "nile_flooding_ceremony");
                    events.put(Season.SUMMER, "sun_worship_festival");
                    events.put(Season.AUTUMN, "harvest_offering");
                    events.put(Season.WINTER, "rebirth_ritual");
                }
                case "victorian" -> {
                    events.put(Season.SPRING, "spring_fair");
                    events.put(Season.SUMMER, "garden_party");
                    events.put(Season.AUTUMN, "industrial_exhibition");
                    events.put(Season.WINTER, "christmas_ball");
                }
                case "nyc" -> {
                    events.put(Season.SPRING, "central_park_picnic");
                    events.put(Season.SUMMER, "street_fair");
                    events.put(Season.AUTUMN, "broadway_opening");
                    events.put(Season.WINTER, "new_years_celebration");
                }
                case "medieval" -> {
                    events.put(Season.SPRING, "spring_tournament");
                    events.put(Season.SUMMER, "midsummer_feast");
                    events.put(Season.AUTUMN, "harvest_festival");
                    events.put(Season.WINTER, "winter_solstice");
                }
                case "greek" -> {
                    events.put(Season.SPRING, "dionysia_festival");
                    events.put(Season.SUMMER, "olympic_games");
                    events.put(Season.AUTUMN, "thesmophoria");
                    events.put(Season.WINTER, "poseidonia");
                }
                case "japanese" -> {
                    events.put(Season.SPRING, "cherry_blossom_festival");
                    events.put(Season.SUMMER, "tanabata");
                    events.put(Season.AUTUMN, "moon_viewing");
                    events.put(Season.WINTER, "winter_solstice");
                }
                case "mayan" -> {
                    events.put(Season.SPRING, "rain_ceremony");
                    events.put(Season.SUMMER, "sun_festival");
                    events.put(Season.AUTUMN, "harvest_celebration");
                    events.put(Season.WINTER, "new_year_ritual");
                }
                case "nordic" -> {
                    events.put(Season.SPRING, "spring_blot");
                    events.put(Season.SUMMER, "midsummer");
                    events.put(Season.AUTUMN, "winter_nights");
                    events.put(Season.WINTER, "yule_feast");
                }
                default -> {
                    events.put(Season.SPRING, "spring_festival");
                    events.put(Season.SUMMER, "summer_fair");
                    events.put(Season.AUTUMN, "harvest_festival");
                    events.put(Season.WINTER, "winter_celebration");
                }
            }
            return events;
        }

        public String getId() { return id; }
        public List<String> getBiomes() { return biomes; }
        public List<String> getStructures() { return structures; }
        public String getSeasonalEvent(Season season) { return seasonalEvents.get(season); }
    }

    public enum Season {
        SPRING, SUMMER, AUTUMN, WINTER
    }

    public enum ProfessionType {
        // Roman professions
        LEGIONARY, SENATOR, MERCHANT, ARCHITECT,
        // Egyptian professions
        SCRIBE, PRIEST, CRAFTSMAN, FARMER,
        // Victorian professions
        INDUSTRIALIST, MERCHANT_VICTORIAN, SERVANT, ARTISAN,
        // NYC professions
        BUSINESSMAN, VENDOR, ARTIST, WORKER,
        // Medieval professions
        KNIGHT, BLACKSMITH, PEASANT, LORD,
        // Greek professions
        PHILOSOPHER, HOPLITE, ORACLE, ARTIST_GREEK,
        // Japanese professions
        SAMURAI, MERCHANT_JAPANESE, ARTISAN_JAPANESE, MONK,
        // Mayan professions
        PRIEST_MAYAN, WARRIOR, CRAFTSMAN_MAYAN, ASTRONOMER,
        // Nordic professions
        VIKING, SHIPBUILDER, HUNTER, SKALD
    }

    public enum CulturalTrait {
        // Personality traits
        CURIOUS, TRADITIONAL, ARTISTIC, MILITANT, SPIRITUAL, MERCANTILE,
        // Social traits
        HIERARCHICAL, EGALITARIAN, COMMUNAL, INDIVIDUALISTIC,
        // Work traits
        INDUSTRIOUS, SCHOLARLY, CRAFTING_FOCUSED, TRADING_FOCUSED,
        // Cultural values
        HONOR_BOUND, NATURE_FOCUSED, TECHNOLOGICAL, CEREMONIAL,
        // New traits for evolving personalities
        ADAPTABLE, INNOVATIVE, CONSERVATIVE, DIPLOMATIC, ISOLATIONIST
    }

    // Constructor for standard culture
    private Culture(CultureType type) {
        this.type = type;
        this.secondaryType = null;
        this.hybridRatio = 0.0f;
        this.traits = initializeTraits(type);
        this.professionWeights = initializeProfessions(type);
        this.structureTemplates = new ArrayList<>(type.getStructures());
        this.preferredBiomes = new HashSet<>(type.getBiomes());
        this.eventWeights = initializeEvents(type);
        this.craftingRecipes = initializeCraftingRecipes(type);
        this.artifacts = initializeArtifacts(type);
    }
    
    // Constructor for hybrid culture
    private Culture(CultureType primaryType, CultureType secondaryType, float hybridRatio) {
        this.type = primaryType;
        this.secondaryType = secondaryType;
        this.hybridRatio = Math.max(0.0f, Math.min(1.0f, hybridRatio)); // Clamp between 0.0 and 1.0
        
        // Initialize with traits from both cultures
        this.traits = initializeHybridTraits(primaryType, secondaryType, hybridRatio);
        this.professionWeights = initializeHybridProfessions(primaryType, secondaryType, hybridRatio);
        
        // Merge structure templates from both cultures
        List<String> allStructures = new ArrayList<>(primaryType.getStructures());
        allStructures.addAll(secondaryType.getStructures());
        this.structureTemplates = new ArrayList<>(new HashSet<>(allStructures)); // Remove duplicates
        
        // Merge preferred biomes
        Set<String> allBiomes = new HashSet<>(primaryType.getBiomes());
        allBiomes.addAll(secondaryType.getBiomes());
        this.preferredBiomes = allBiomes;
        
        // Merge events with appropriate weights
        this.eventWeights = initializeHybridEvents(primaryType, secondaryType, hybridRatio);
        
        // Merge crafting recipes and artifacts
        this.craftingRecipes = initializeHybridCraftingRecipes(primaryType, secondaryType, hybridRatio);
        this.artifacts = initializeHybridArtifacts(primaryType, secondaryType, hybridRatio);
    }

    private Set<CulturalTrait> initializeHybridTraits(CultureType primary, CultureType secondary, float hybridRatio) {
        Set<CulturalTrait> primaryTraits = initializeTraits(primary);
        Set<CulturalTrait> secondaryTraits = initializeTraits(secondary);
        Set<CulturalTrait> hybridTraits = new HashSet<>();
        
        // Add all primary traits
        hybridTraits.addAll(primaryTraits);
        
        // Add some secondary traits based on hybridRatio
        for (CulturalTrait trait : secondaryTraits) {
            if (Math.random() < hybridRatio) {
                hybridTraits.add(trait);
            }
        }
        
        // Always add ADAPTABLE trait for hybrid cultures
        hybridTraits.add(CulturalTrait.ADAPTABLE);
        
        return hybridTraits;
    }
    
    private Map<ProfessionType, Integer> initializeHybridProfessions(CultureType primary, CultureType secondary, float hybridRatio) {
        Map<ProfessionType, Integer> primaryWeights = initializeProfessions(primary);
        Map<ProfessionType, Integer> secondaryWeights = initializeProfessions(secondary);
        Map<ProfessionType, Integer> hybridWeights = new HashMap<>(primaryWeights);
        
        // Add secondary professions with weights adjusted by hybrid ratio
        for (Map.Entry<ProfessionType, Integer> entry : secondaryWeights.entrySet()) {
            int adjustedWeight = Math.round(entry.getValue() * hybridRatio);
            if (adjustedWeight > 0) {
                hybridWeights.put(entry.getKey(), adjustedWeight);
            }
        }
        
        return hybridWeights;
    }
    
    private Map<String, Float> initializeHybridEvents(CultureType primary, CultureType secondary, float hybridRatio) {
        Map<String, Float> primaryEvents = initializeEvents(primary);
        Map<String, Float> secondaryEvents = initializeEvents(secondary);
        Map<String, Float> hybridEvents = new HashMap<>(primaryEvents);
        
        // Add secondary events with weights adjusted by hybrid ratio
        for (Map.Entry<String, Float> entry : secondaryEvents.entrySet()) {
            float adjustedWeight = entry.getValue() * hybridRatio;
            if (adjustedWeight > 0.05f) { // Minimum threshold for events
                hybridEvents.put(entry.getKey(), adjustedWeight);
            }
        }
        
        // Add a special "cultural_exchange" event for hybrid cultures
        hybridEvents.put("cultural_exchange_festival", 0.4f);
        
        return hybridEvents;
    }
    
    private List<String> initializeHybridCraftingRecipes(CultureType primary, CultureType secondary, float hybridRatio) {
        List<String> primaryRecipes = initializeCraftingRecipes(primary);
        List<String> secondaryRecipes = initializeCraftingRecipes(secondary);
        List<String> hybridRecipes = new ArrayList<>(primaryRecipes);
        
        // Add some secondary recipes based on hybridRatio
        for (String recipe : secondaryRecipes) {
            if (Math.random() < hybridRatio && !hybridRecipes.contains(recipe)) {
                hybridRecipes.add(recipe);
            }
        }
        
        // Add a unique hybrid recipe
        hybridRecipes.add("fusion_" + primary.getId() + "_" + secondary.getId() + "_artifact");
        
        return hybridRecipes;
    }
    
    private List<String> initializeHybridArtifacts(CultureType primary, CultureType secondary, float hybridRatio) {
        List<String> primaryArtifacts = initializeArtifacts(primary);
        List<String> secondaryArtifacts = initializeArtifacts(secondary);
        List<String> hybridArtifacts = new ArrayList<>(primaryArtifacts);
        
        // Add some secondary artifacts based on hybridRatio
        for (String artifact : secondaryArtifacts) {
            if (Math.random() < hybridRatio && !hybridArtifacts.contains(artifact)) {
                hybridArtifacts.add(artifact);
            }
        }
        
        // Add a unique hybrid artifact
        hybridArtifacts.add("hybrid_" + primary.getId() + "_" + secondary.getId() + "_relic");
        
        return hybridArtifacts;
    }

    private Set<CulturalTrait> initializeTraits(CultureType type) {
        Set<CulturalTrait> traits = new HashSet<>();
        switch (type) {
            case ROMAN -> {
                traits.add(CulturalTrait.MILITANT);
                traits.add(CulturalTrait.HIERARCHICAL);
                traits.add(CulturalTrait.HONOR_BOUND);
            }
            case EGYPTIAN -> {
                traits.add(CulturalTrait.SPIRITUAL);
                traits.add(CulturalTrait.CEREMONIAL);
                traits.add(CulturalTrait.TRADITIONAL);
            }
            case VICTORIAN -> {
                traits.add(CulturalTrait.INDUSTRIOUS);
                traits.add(CulturalTrait.TECHNOLOGICAL);
                traits.add(CulturalTrait.HIERARCHICAL);
            }
            case NYC -> {
                traits.add(CulturalTrait.TRADING_FOCUSED);
                traits.add(CulturalTrait.INDIVIDUALISTIC);
                traits.add(CulturalTrait.TECHNOLOGICAL);
            }
            case MEDIEVAL -> {
                traits.add(CulturalTrait.HONOR_BOUND);
                traits.add(CulturalTrait.HIERARCHICAL);
                traits.add(CulturalTrait.CRAFTING_FOCUSED);
            }
            case GREEK -> {
                traits.add(CulturalTrait.SCHOLARLY);
                traits.add(CulturalTrait.ARTISTIC);
                traits.add(CulturalTrait.EGALITARIAN);
            }
            case JAPANESE -> {
                traits.add(CulturalTrait.HONOR_BOUND);
                traits.add(CulturalTrait.TRADITIONAL);
                traits.add(CulturalTrait.CRAFTING_FOCUSED);
            }
            case MAYAN -> {
                traits.add(CulturalTrait.SPIRITUAL);
                traits.add(CulturalTrait.SCHOLARLY);
                traits.add(CulturalTrait.NATURE_FOCUSED);
            }
        }
        return traits;
    }

    private Map<ProfessionType, Integer> initializeProfessions(CultureType type) {
        Map<ProfessionType, Integer> weights = new HashMap<>();
        switch (type) {
            case ROMAN -> {
                weights.put(ProfessionType.LEGIONARY, 30);
                weights.put(ProfessionType.SENATOR, 10);
                weights.put(ProfessionType.MERCHANT, 30);
                weights.put(ProfessionType.ARCHITECT, 30);
            }
            case EGYPTIAN -> {
                weights.put(ProfessionType.SCRIBE, 20);
                weights.put(ProfessionType.PRIEST, 20);
                weights.put(ProfessionType.CRAFTSMAN, 30);
                weights.put(ProfessionType.FARMER, 30);
            }
            case VICTORIAN -> {
                weights.put(ProfessionType.INDUSTRIALIST, 20);
                weights.put(ProfessionType.MERCHANT_VICTORIAN, 30);
                weights.put(ProfessionType.SERVANT, 30);
                weights.put(ProfessionType.ARTISAN, 20);
            }
            case NYC -> {
                weights.put(ProfessionType.BUSINESSMAN, 25);
                weights.put(ProfessionType.VENDOR, 25);
                weights.put(ProfessionType.ARTIST, 25);
                weights.put(ProfessionType.WORKER, 25);
            }
            case MEDIEVAL -> {
                weights.put(ProfessionType.KNIGHT, 20);
                weights.put(ProfessionType.BLACKSMITH, 30);
                weights.put(ProfessionType.PEASANT, 40);
                weights.put(ProfessionType.LORD, 10);
            }
            case GREEK -> {
                weights.put(ProfessionType.PHILOSOPHER, 25);
                weights.put(ProfessionType.HOPLITE, 25);
                weights.put(ProfessionType.ORACLE, 20);
                weights.put(ProfessionType.ARTIST_GREEK, 30);
            }
            case JAPANESE -> {
                weights.put(ProfessionType.SAMURAI, 20);
                weights.put(ProfessionType.MERCHANT_JAPANESE, 30);
                weights.put(ProfessionType.ARTISAN_JAPANESE, 30);
                weights.put(ProfessionType.MONK, 20);
            }
            case MAYAN -> {
                weights.put(ProfessionType.PRIEST_MAYAN, 25);
                weights.put(ProfessionType.WARRIOR, 25);
                weights.put(ProfessionType.CRAFTSMAN_MAYAN, 25);
                weights.put(ProfessionType.ASTRONOMER, 25);
            }
        }
        return weights;
    }

    private Map<String, Float> initializeEvents(CultureType type) {
        Map<String, Float> weights = new HashMap<>();
        switch (type) {
            case ROMAN -> {
                weights.put("gladiator_games", 0.3f);
                weights.put("senate_election", 0.2f);
                weights.put("market_festival", 0.3f);
                weights.put("military_parade", 0.2f);
            }
            case EGYPTIAN -> {
                weights.put("nile_festival", 0.3f);
                weights.put("pyramid_ceremony", 0.2f);
                weights.put("harvest_celebration", 0.3f);
                weights.put("temple_ritual", 0.2f);
            }
            case VICTORIAN -> {
                weights.put("industrial_fair", 0.3f);
                weights.put("garden_party", 0.3f);
                weights.put("scientific_exhibition", 0.2f);
                weights.put("social_ball", 0.2f);
            }
            case NYC -> {
                weights.put("street_fair", 0.3f);
                weights.put("art_gallery_opening", 0.2f);
                weights.put("food_festival", 0.3f);
                weights.put("business_conference", 0.2f);
            }
            case MEDIEVAL -> {
                weights.put("tournament", 0.3f);
                weights.put("harvest_festival", 0.3f);
                weights.put("royal_ceremony", 0.2f);
                weights.put("guild_fair", 0.2f);
            }
            case GREEK -> {
                weights.put("olympic_games", 0.3f);
                weights.put("philosophical_debate", 0.2f);
                weights.put("theater_performance", 0.3f);
                weights.put("oracle_consultation", 0.2f);
            }
            case JAPANESE -> {
                weights.put("cherry_blossom_festival", 0.3f);
                weights.put("tea_ceremony", 0.2f);
                weights.put("martial_arts_tournament", 0.3f);
                weights.put("lantern_festival", 0.2f);
            }
            case MAYAN -> {
                weights.put("ball_game", 0.3f);
                weights.put("astronomical_ceremony", 0.2f);
                weights.put("harvest_ritual", 0.3f);
                weights.put("sacrifice_ceremony", 0.2f);
            }
        }
        return weights;
    }

    private List<String> initializeCraftingRecipes(CultureType type) {
        List<String> recipes = new ArrayList<>();
        switch (type) {
            case ROMAN -> {
                recipes.add("gladius");
                recipes.add("roman_armor");
                recipes.add("wine_amphora");
                recipes.add("mosaic_block");
            }
            case EGYPTIAN -> {
                recipes.add("khopesh");
                recipes.add("papyrus_scroll");
                recipes.add("scarab_amulet");
                recipes.add("hieroglyph_block");
            }
            case VICTORIAN -> {
                recipes.add("pocket_watch");
                recipes.add("steam_engine");
                recipes.add("top_hat");
                recipes.add("ornate_furniture");
            }
            case NYC -> {
                recipes.add("hot_dog_cart");
                recipes.add("newspaper_stand");
                recipes.add("traffic_light");
                recipes.add("subway_token");
            }
            case MEDIEVAL -> {
                recipes.add("longsword");
                recipes.add("chainmail");
                recipes.add("shield");
                recipes.add("forge_tools");
            }
            case GREEK -> {
                recipes.add("bronze_spear");
                recipes.add("philosopher_scroll");
                recipes.add("amphora");
                recipes.add("marble_column");
            }
            case JAPANESE -> {
                recipes.add("katana");
                recipes.add("kimono");
                recipes.add("tea_set");
                recipes.add("paper_lantern");
            }
            case MAYAN -> {
                recipes.add("obsidian_blade");
                recipes.add("jade_mask");
                recipes.add("calendar_stone");
                recipes.add("ceremonial_headdress");
            }
        }
        return recipes;
    }

    private List<String> initializeArtifacts(CultureType type) {
        List<String> artifacts = new ArrayList<>();
        switch (type) {
            case ROMAN -> {
                artifacts.add("imperial_seal");
                artifacts.add("legion_standard");
                artifacts.add("senator_ring");
                artifacts.add("victory_laurel");
            }
            case EGYPTIAN -> {
                artifacts.add("pharaoh_mask");
                artifacts.add("ankh_amulet");
                artifacts.add("sun_disk");
                artifacts.add("sacred_scarab");
            }
            case VICTORIAN -> {
                artifacts.add("royal_scepter");
                artifacts.add("industrial_blueprint");
                artifacts.add("family_crest");
                artifacts.add("mechanical_wonder");
            }
            case NYC -> {
                artifacts.add("liberty_torch");
                artifacts.add("stock_certificate");
                artifacts.add("city_key");
                artifacts.add("bridge_cornerstone");
            }
            case MEDIEVAL -> {
                artifacts.add("royal_crown");
                artifacts.add("holy_grail");
                artifacts.add("knight_seal");
                artifacts.add("ancient_sword");
            }
            case GREEK -> {
                artifacts.add("olympic_wreath");
                artifacts.add("philosopher_stone");
                artifacts.add("oracle_token");
                artifacts.add("parthenon_fragment");
            }
            case JAPANESE -> {
                artifacts.add("shogun_seal");
                artifacts.add("ancient_scroll");
                artifacts.add("samurai_banner");
                artifacts.add("imperial_jade");
            }
            case MAYAN -> {
                artifacts.add("crystal_skull");
                artifacts.add("golden_mask");
                artifacts.add("astronomical_disc");
                artifacts.add("sacred_calendar");
            }
        }
        return artifacts;
    }

    public CultureType getType() { return type; }
    public CultureType getSecondaryType() { return secondaryType; }
    public boolean isHybrid() { return secondaryType != null; }
    public float getHybridRatio() { return hybridRatio; }
    public Set<CulturalTrait> getTraits() { return traits; }
    public Map<ProfessionType, Integer> getProfessionWeights() { return professionWeights; }
    public List<String> getStructureTemplates() { return structureTemplates; }
    public Set<String> getPreferredBiomes() { return preferredBiomes; }
    public Map<String, Float> getEventWeights() { return eventWeights; }
    public List<String> getCraftingRecipes() { return craftingRecipes; }
    public List<String> getArtifacts() { return artifacts; }

    public static Culture create(CultureType type) {
        return new Culture(type);
    }
    
    public static Culture createHybrid(CultureType primaryType, CultureType secondaryType, float hybridRatio) {
        return new Culture(primaryType, secondaryType, hybridRatio);
    }

    public ProfessionType getRandomProfession(Random random) {
        int totalWeight = professionWeights.values().stream().mapToInt(Integer::intValue).sum();
        int selection = random.nextInt(totalWeight);
        int currentWeight = 0;
        
        for (Map.Entry<ProfessionType, Integer> entry : professionWeights.entrySet()) {
            currentWeight += entry.getValue();
            if (selection < currentWeight) {
                return entry.getKey();
            }
        }
        
        return professionWeights.keySet().iterator().next();
    }

    public String getRandomEvent(Random random) {
        float totalWeight = (float) eventWeights.values().stream().mapToDouble(Float::doubleValue).sum();
        float selection = random.nextFloat() * totalWeight;
        float currentWeight = 0;
        
        for (Map.Entry<String, Float> entry : eventWeights.entrySet()) {
            currentWeight += entry.getValue();
            if (selection < currentWeight) {
                return entry.getKey();
            }
        }
        
        return eventWeights.keySet().iterator().next();
    }

    public boolean isPreferredBiome(Biome biome) {
        Identifier biomeId = Registries.BIOME.getEntrySet().stream()
            .filter(entry -> entry.getValue().equals(biome))
            .findFirst()
            .map(entry -> entry.getKey().getValue())
            .orElse(null);
            
        if (biomeId == null) return false;
        return preferredBiomes.contains(biomeId.getPath());
    }
    
    // Method to get seasonal events
    public String getSeasonalEvent(Season season) {
        if (isHybrid()) {
            // For hybrid cultures, randomly choose between primary and secondary based on hybridRatio
            return Math.random() < hybridRatio ? 
                secondaryType.getSeasonalEvent(season) : type.getSeasonalEvent(season);
        }
        return type.getSeasonalEvent(season);
    }
}
