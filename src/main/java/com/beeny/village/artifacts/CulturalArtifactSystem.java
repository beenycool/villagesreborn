package com.beeny.village.artifacts;

import com.beeny.village.event.PlayerEventParticipation;
import com.beeny.village.util.DataComponentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.nbt.NbtElement; // Added import
import net.minecraft.registry.RegistryWrapper.WrapperLookup;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * System for handling cultural artifacts that players can discover and collect
 * to enhance village development and gain special bonuses
 */
public class CulturalArtifactSystem {
    private static final CulturalArtifactSystem INSTANCE = new CulturalArtifactSystem();
    
    // Maps player UUIDs to their artifact collections
    private final Map<UUID, Map<String, List<ArtifactInstance>>> playerArtifacts = new ConcurrentHashMap<>();
    
    // Available artifact types by culture
    private final Map<String, List<ArtifactDefinition>> artifactDefinitions = new HashMap<>();
    
    // Random for generating artifacts
    private final Random random = new Random();
    
    // WrapperLookup for NBT serialization
    private WrapperLookup wrapperLookup = null;
    
    private CulturalArtifactSystem() {
        initializeArtifactDefinitions();
    }
    
    public static CulturalArtifactSystem getInstance() {
        return INSTANCE;
    }
    
    public void setWrapperLookup(WrapperLookup lookup) {
        this.wrapperLookup = lookup;
        DataComponentHelper.setWrapperLookup(lookup);
    }
    
    /**
     * Represents an artifact definition
     */
    public static class ArtifactDefinition {
        private final String id;
        private final String name;
        private final String description;
        private final String culture;
        private final Item baseItem;
        private final Rarity rarity;
        private final int villageBonus;
        private final int reputationValue;
        
        public ArtifactDefinition(String id, String name, String description, String culture, 
                               Item baseItem, Rarity rarity, int villageBonus, int reputationValue) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.culture = culture;
            this.baseItem = baseItem;
            this.rarity = rarity;
            this.villageBonus = villageBonus;
            this.reputationValue = reputationValue;
        }
        
        public String getId() { return id; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getCulture() { return culture; }
        public Item getBaseItem() { return baseItem; }
        public Rarity getRarity() { return rarity; }
        public int getVillageBonus() { return villageBonus; }
        public int getReputationValue() { return reputationValue; }
    }
    
    /**
     * Represents a specific artifact instance owned by a player
     */
    public static class ArtifactInstance {
        private final String artifactId;
        private final UUID playerUUID;
        private final long discoveryTime;
        private boolean displayed;
        private BlockPos displayLocation;
        
        public ArtifactInstance(String artifactId, UUID playerUUID) {
            this.artifactId = artifactId;
            this.playerUUID = playerUUID;
            this.discoveryTime = System.currentTimeMillis();
            this.displayed = false;
            this.displayLocation = null;
        }
        
        public String getArtifactId() { return artifactId; }
        public UUID getPlayerUUID() { return playerUUID; }
        public long getDiscoveryTime() { return discoveryTime; }
        public boolean isDisplayed() { return displayed; }
        public BlockPos getDisplayLocation() { return displayLocation; }
        
        public void setDisplayed(boolean displayed) {
            this.displayed = displayed;
        }
        
        public void setDisplayLocation(BlockPos location) {
            this.displayLocation = location;
            this.displayed = location != null;
        }
    }
    
    /**
     * Artifact rarity levels
     */
    public enum Rarity {
        COMMON(0.6f, Formatting.WHITE, 1),
        UNCOMMON(0.3f, Formatting.GREEN, 2),
        RARE(0.08f, Formatting.BLUE, 4),
        EPIC(0.02f, Formatting.DARK_PURPLE, 8);
        
        private final float chance;
        private final Formatting color;
        private final int multiplier;
        
        Rarity(float chance, Formatting color, int multiplier) {
            this.chance = chance;
            this.color = color;
            this.multiplier = multiplier;
        }
        
        public float getChance() { return chance; }
        public Formatting getColor() { return color; }
        public int getMultiplier() { return multiplier; }
        
        public static Rarity getRandomRarity() {
            float roll = new Random().nextFloat();
            float cumulativeChance = 0;
            
            for (Rarity rarity : values()) {
                cumulativeChance += rarity.getChance();
                if (roll <= cumulativeChance) {
                    return rarity;
                }
            }
            
            return COMMON; // Default fallback
        }
    }
    
    /**
     * Initialize all artifact definitions
     */
    private void initializeArtifactDefinitions() {
        // Roman artifacts
        List<ArtifactDefinition> romanArtifacts = new ArrayList<>();
        romanArtifacts.add(new ArtifactDefinition(
            "roman_coin", "Ancient Roman Coin", "A well-preserved coin from the Roman Empire",
            "roman", Items.GOLD_NUGGET, Rarity.COMMON, 5, 3
        ));
        romanArtifacts.add(new ArtifactDefinition(
            "roman_statue", "Roman Marble Statue", "A small marble statue depicting a Roman deity",
            "roman", Items.CLAY, Rarity.UNCOMMON, 10, 5
        ));
        romanArtifacts.add(new ArtifactDefinition(
            "gladiator_helmet", "Gladiator Helmet", "A decorative helmet worn by Roman gladiators",
            "roman", Items.IRON_HELMET, Rarity.RARE, 20, 10
        ));
        romanArtifacts.add(new ArtifactDefinition(
            "caesar_bust", "Caesar's Bust", "An incredibly rare bust of Julius Caesar",
            "roman", Items.GOLD_BLOCK, Rarity.EPIC, 40, 20
        ));
        artifactDefinitions.put("roman", romanArtifacts);
        
        // Egyptian artifacts
        List<ArtifactDefinition> egyptianArtifacts = new ArrayList<>();
        egyptianArtifacts.add(new ArtifactDefinition(
            "egyptian_faience", "Egyptian Faience", "A small glazed ceramic piece from ancient Egypt",
            "egyptian", Items.LIGHT_BLUE_GLAZED_TERRACOTTA, Rarity.COMMON, 5, 3
        ));
        egyptianArtifacts.add(new ArtifactDefinition(
            "papyrus_scroll", "Ancient Papyrus Scroll", "A preserved scroll with hieroglyphics",
            "egyptian", Items.PAPER, Rarity.UNCOMMON, 10, 5
        ));
        egyptianArtifacts.add(new ArtifactDefinition(
            "ankh_amulet", "Ankh Amulet", "A gold amulet symbolizing life in ancient Egypt",
            "egyptian", Items.GOLD_INGOT, Rarity.RARE, 20, 10
        ));
        egyptianArtifacts.add(new ArtifactDefinition(
            "pharaohs_mask", "Pharaoh's Mask", "An incredibly rare golden death mask of a pharaoh",
            "egyptian", Items.GOLDEN_HELMET, Rarity.EPIC, 40, 20
        ));
        artifactDefinitions.put("egyptian", egyptianArtifacts);
        
        // Victorian artifacts
        List<ArtifactDefinition> victorianArtifacts = new ArrayList<>();
        victorianArtifacts.add(new ArtifactDefinition(
            "victorian_gear", "Victorian Gear", "A small brass gear from a Victorian machine",
            "victorian", Items.IRON_NUGGET, Rarity.COMMON, 5, 3
        ));
        victorianArtifacts.add(new ArtifactDefinition(
            "pocket_watch", "Victorian Pocket Watch", "An ornate pocket watch from the Victorian era",
            "victorian", Items.CLOCK, Rarity.UNCOMMON, 10, 5
        ));
        victorianArtifacts.add(new ArtifactDefinition(
            "steam_engine", "Miniature Steam Engine", "A working miniature steam engine model",
            "victorian", Items.PISTON, Rarity.RARE, 20, 10
        ));
        victorianArtifacts.add(new ArtifactDefinition(
            "royal_scepter", "Royal Scepter", "An incredibly rare ceremonial scepter from Victorian royalty",
            "victorian", Items.BLAZE_ROD, Rarity.EPIC, 40, 20
        ));
        artifactDefinitions.put("victorian", victorianArtifacts);
        
        // NYC artifacts
        List<ArtifactDefinition> nycArtifacts = new ArrayList<>();
        nycArtifacts.add(new ArtifactDefinition(
            "subway_token", "NYC Subway Token", "A small token used for the NYC subway",
            "nyc", Items.IRON_NUGGET, Rarity.COMMON, 5, 3
        ));
        nycArtifacts.add(new ArtifactDefinition(
            "broadway_ticket", "Broadway Ticket", "A preserved ticket to a famous Broadway show",
            "nyc", Items.PAPER, Rarity.UNCOMMON, 10, 5
        ));
        nycArtifacts.add(new ArtifactDefinition(
            "liberty_torch", "Liberty Torch Model", "A small model of the Statue of Liberty's torch",
            "nyc", Items.TORCH, Rarity.RARE, 20, 10
        ));
        nycArtifacts.add(new ArtifactDefinition(
            "empire_state_model", "Empire State Building Model", "An incredibly detailed model of the Empire State Building",
            "nyc", Items.QUARTZ_BLOCK, Rarity.EPIC, 40, 20
        ));
        artifactDefinitions.put("nyc", nycArtifacts);
    }
    
    /**
     * Generate a random artifact for a player during an event
     */
    public ItemStack generateEventArtifact(ServerPlayerEntity player, String culture, String eventType) {
        List<ArtifactDefinition> artifacts = artifactDefinitions.get(culture);
        if (artifacts == null || artifacts.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ArtifactDefinition selectedArtifact = artifacts.get(random.nextInt(artifacts.size()));
        ItemStack result = new ItemStack(selectedArtifact.getBaseItem());

        // Set custom name with appropriate color
        result.set(DataComponentTypes.CUSTOM_NAME, 
            Text.literal(selectedArtifact.getName())
                .setStyle(Style.EMPTY.withColor(selectedArtifact.getRarity().getColor())));

        // Create lore list
        List<Text> lore = new ArrayList<>();
        
        // Culture info
        lore.add(Text.literal("Culture: " + capitalize(culture))
            .setStyle(Style.EMPTY.withColor(Formatting.BLUE)));
        
        // Event info
        lore.add(Text.literal("Discovered during: " + capitalize(eventType))
            .setStyle(Style.EMPTY.withColor(Formatting.GREEN)));
        
        // Development value
        lore.add(Text.literal("Village Development Value: +" + selectedArtifact.getVillageBonus())
            .setStyle(Style.EMPTY.withColor(Formatting.GREEN)));
        
        // Reputation value
        lore.add(Text.literal("Reputation Value: +" + selectedArtifact.getReputationValue())
            .setStyle(Style.EMPTY.withColor(Formatting.AQUA)));

        DataComponentHelper.setLore(result, lore);

        // Add hidden artifact data
        Map<String, Object> customData = new HashMap<>();
        customData.put("id", selectedArtifact.getId());
        customData.put("culture", culture);
        customData.put("discoveryTime", System.currentTimeMillis());
        customData.put("discoveredBy", player.getUuid().toString());
        customData.put("eventType", eventType);
        customData.put("villageBonus", selectedArtifact.getVillageBonus());
        customData.put("reputationValue", selectedArtifact.getReputationValue());
        
        DataComponentHelper.setCustomData(result, customData);

        return result;
    }
    
    /**
     * Register an artifact to a player's collection
     */
    public void registerArtifact(UUID playerUUID, String artifactId) {
        ArtifactDefinition definition = getArtifactDefinitionById(artifactId);
        if (definition == null) {
            return;
        }
        
        // Create the artifact instance
        ArtifactInstance artifact = new ArtifactInstance(artifactId, playerUUID);
        
        // Add to player's collection
        Map<String, List<ArtifactInstance>> culturalCollections = 
            playerArtifacts.computeIfAbsent(playerUUID, k -> new HashMap<>());
            
        List<ArtifactInstance> cultureArtifacts = 
            culturalCollections.computeIfAbsent(definition.getCulture(), k -> new ArrayList<>());
            
        cultureArtifacts.add(artifact);
    }
    
    /**
     * Display an artifact in a village
     */
    public boolean displayArtifact(PlayerEntity player, String artifactId, BlockPos location) {
        UUID playerUUID = player.getUuid();
        ArtifactInstance artifact = findPlayerArtifactInstance(playerUUID, artifactId);
        
        if (artifact == null) {
            return false;
        }
        
        // Set the display location
        artifact.setDisplayLocation(location);
        return true;
    }
    
    /**
     * Find an artifact instance owned by a player
     */
    private ArtifactInstance findPlayerArtifactInstance(UUID playerUUID, String artifactId) {
        Map<String, List<ArtifactInstance>> culturalCollections = playerArtifacts.get(playerUUID);
        if (culturalCollections == null) {
            return null;
        }
        
        for (List<ArtifactInstance> artifacts : culturalCollections.values()) {
            for (ArtifactInstance artifact : artifacts) {
                if (artifact.getArtifactId().equals(artifactId)) {
                    return artifact;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Get artifact definition by ID
     */
    public ArtifactDefinition getArtifactDefinitionById(String artifactId) {
        for (List<ArtifactDefinition> culturalArtifacts : artifactDefinitions.values()) {
            for (ArtifactDefinition definition : culturalArtifacts) {
                if (definition.getId().equals(artifactId)) {
                    return definition;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Get all artifacts owned by a player
     */
    public Map<String, List<ArtifactInstance>> getPlayerArtifacts(UUID playerUUID) {
        return playerArtifacts.getOrDefault(playerUUID, Collections.emptyMap());
    }
    
    /**
     * Get artifacts owned by a player for a specific culture
     */
    public List<ArtifactInstance> getPlayerCultureArtifacts(UUID playerUUID, String culture) {
        Map<String, List<ArtifactInstance>> culturalCollections = playerArtifacts.get(playerUUID);
        if (culturalCollections == null) {
            return Collections.emptyList();
        }
        
        return culturalCollections.getOrDefault(culture.toLowerCase(), Collections.emptyList());
    }
    
    /**
     * Get available artifact definitions for a culture
     */
    public List<ArtifactDefinition> getArtifactDefinitionsForCulture(String culture) {
        return artifactDefinitions.getOrDefault(culture.toLowerCase(), Collections.emptyList());
    }
    
    /**
     * Determine if a player has collected all artifacts for a culture
     */
    public boolean hasCompleteCollection(UUID playerUUID, String culture) {
        List<ArtifactInstance> collected = getPlayerCultureArtifacts(playerUUID, culture);
        List<ArtifactDefinition> available = getArtifactDefinitionsForCulture(culture);
        
        if (collected.isEmpty() || available.isEmpty()) {
            return false;
        }
        
        // Check if player has at least one of each artifact type
        Set<String> collectedIds = new HashSet<>();
        for (ArtifactInstance artifact : collected) {
            collectedIds.add(artifact.getArtifactId());
        }
        
        for (ArtifactDefinition definition : available) {
            if (!collectedIds.contains(definition.getId())) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Calculate village development bonus from a player's artifacts
     */
    public int calculateVillageDevelopmentBonus(UUID playerUUID, String culture) {
        List<ArtifactInstance> artifacts = getPlayerCultureArtifacts(playerUUID, culture);
        int totalBonus = 0;
        
        for (ArtifactInstance instance : artifacts) {
            ArtifactDefinition definition = getArtifactDefinitionById(instance.getArtifactId());
            if (definition != null && definition.getCulture().equalsIgnoreCase(culture)) {
                // Displayed artifacts provide full bonus, undisplayed provide half
                if (instance.isDisplayed()) {
                    totalBonus += definition.getVillageBonus();
                } else {
                    totalBonus += definition.getVillageBonus() / 2;
                }
            }
        }
        
        // Bonus for complete collection
        if (hasCompleteCollection(playerUUID, culture)) {
            totalBonus *= 1.5; // 50% bonus for complete collection
        }
        
        return totalBonus;
    }
    
    /**
     * Save player artifact data to NBT
     */
    public NbtCompound savePlayerArtifacts(UUID playerUUID) {
        NbtCompound nbt = new NbtCompound();
        Map<String, List<ArtifactInstance>> collections = playerArtifacts.get(playerUUID);
        
        if (collections != null) {
            for (Map.Entry<String, List<ArtifactInstance>> entry : collections.entrySet()) {
                String culture = entry.getKey();
                List<ArtifactInstance> artifacts = entry.getValue();
                
                NbtCompound cultureData = new NbtCompound();
                for (ArtifactInstance artifact : artifacts) {
                    NbtCompound artifactData = new NbtCompound();
                    artifactData.putString("id", artifact.getArtifactId());
                    artifactData.putBoolean("displayed", artifact.isDisplayed());
                    
                    if (artifact.getDisplayLocation() != null) {
                        BlockPos pos = artifact.getDisplayLocation();
                        NbtCompound posData = new NbtCompound();
                        posData.putInt("x", pos.getX());
                        posData.putInt("y", pos.getY());
                        posData.putInt("z", pos.getZ());
                        artifactData.put("displayPos", posData);
                    }
                    
                    cultureData.put(artifact.getArtifactId(), artifactData);
                }
                
                nbt.put("artifacts_" + culture, cultureData);
            }
        }
        
        return nbt;
    }
    
    /**
     * Load player artifact data from NBT
     */
    public void loadPlayerArtifacts(UUID playerUUID, NbtCompound data) {
        Map<String, List<ArtifactInstance>> collections = new HashMap<>();
        
        for (String key : data.getKeys()) {
            if (key.startsWith("artifacts_")) {
                String culture = key.substring(10);
                NbtCompound cultureData = data.contains(key, NbtElement.COMPOUND_TYPE) ? data.getCompound(key) : new NbtCompound();
                
                List<ArtifactInstance> artifacts = new ArrayList<>();
                for (String artifactId : cultureData.getKeys()) {
                    NbtCompound artifactData = cultureData.contains(artifactId, NbtElement.COMPOUND_TYPE) ? cultureData.getCompound(artifactId) : new NbtCompound();
                    ArtifactInstance artifact = new ArtifactInstance(artifactId, playerUUID);
                    
                    // Load display status
                    boolean displayed = artifactData.contains("displayed", NbtElement.BYTE_TYPE) ? artifactData.getBoolean("displayed") : false;
                    artifact.setDisplayed(displayed);
                    
                    // Load display location if available
                    if (displayed && artifactData.contains("displayPos")) {
                        NbtCompound posData = artifactData.contains("displayPos", NbtElement.COMPOUND_TYPE) ? artifactData.getCompound("displayPos") : new NbtCompound();
                        artifact.setDisplayLocation(new BlockPos(
                            posData.contains("x", NbtElement.INT_TYPE) ? posData.getInt("x") : 0,
                            posData.contains("y", NbtElement.INT_TYPE) ? posData.getInt("y") : 0,
                            posData.contains("z", NbtElement.INT_TYPE) ? posData.getInt("z") : 0
                        ));
                    }
                    
                    artifacts.add(artifact);
                }
                
                collections.put(culture, artifacts);
            }
        }
        
        if (!collections.isEmpty()) {
            playerArtifacts.put(playerUUID, collections);
        }
    }
    
    /**
     * Capitalize the first letter of a string
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}
