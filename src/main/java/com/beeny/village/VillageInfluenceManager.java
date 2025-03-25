package com.beeny.village;

import com.beeny.village.event.PlayerEventParticipation;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.particle.ParticleTypes;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages player influence on village development and evolution
 */
public class VillageInfluenceManager {
    private static final VillageInfluenceManager INSTANCE = new VillageInfluenceManager();
    
    // Maps BlockPos of village centers to their development data
    private final Map<BlockPos, VillageDevelopmentData> villageDevelopment = new ConcurrentHashMap<>();
    
    // Maps player UUIDs to their current village contribution activities
    private final Map<UUID, PlayerContribution> activeContributions = new ConcurrentHashMap<>();
    
    // Maps pairs of villages to their relationship status
    private final Map<VillagePair, VillageRelationship> villageRelationships = new ConcurrentHashMap<>();
    
    // Development thresholds for village evolution stages
    private static final int[] DEVELOPMENT_THRESHOLDS = {100, 250, 500, 1000, 2000};
    
    /**
     * Get singleton instance
     */
    public static VillageInfluenceManager getInstance() {
        return INSTANCE;
    }
    
    /**
     * Record types for village relationships
     */
    public record VillagePair(BlockPos village1, BlockPos village2) {
        /**
         * Create a standardized pair regardless of which village is passed first
         */
        public static VillagePair of(BlockPos village1, BlockPos village2) {
            // Create consistent pair order for lookup regardless of order passed in
            if (compareBlockPos(village1, village2) <= 0) {
                return new VillagePair(village1, village2);
            } else {
                return new VillagePair(village2, village1);
            }
        }
        
        private static int compareBlockPos(BlockPos pos1, BlockPos pos2) {
            if (pos1.getX() != pos2.getX()) return Integer.compare(pos1.getX(), pos2.getX());
            if (pos1.getY() != pos2.getY()) return Integer.compare(pos1.getY(), pos2.getY());
            return Integer.compare(pos1.getZ(), pos2.getZ());
        }
    }
    
    public static class VillageRelationship {
        // Relationship status from -100 (enemies) to 100 (allies)
        private int relationshipScore;
        private RelationshipStatus status;
        private long lastInteractionTime;
        private final List<RelationshipEvent> recentEvents;
        private final Map<String, Integer> tradeCounts;
        
        public VillageRelationship() {
            this.relationshipScore = 0;
            this.status = RelationshipStatus.NEUTRAL;
            this.lastInteractionTime = System.currentTimeMillis();
            this.recentEvents = new ArrayList<>();
            this.tradeCounts = new HashMap<>();
        }
        
        public int getRelationshipScore() {
            return relationshipScore;
        }
        
        public RelationshipStatus getStatus() {
            return status;
        }
        
        public long getLastInteractionTime() {
            return lastInteractionTime;
        }
        
        public List<RelationshipEvent> getRecentEvents() {
            return Collections.unmodifiableList(recentEvents);
        }
        
        public void adjustRelationship(int amount, String reason) {
            this.relationshipScore = Math.max(-100, Math.min(100, relationshipScore + amount));
            updateRelationshipStatus();
            lastInteractionTime = System.currentTimeMillis();
            
            // Add event to history, keeping only the last 10 events
            recentEvents.add(0, new RelationshipEvent(amount, reason, System.currentTimeMillis()));
            if (recentEvents.size() > 10) {
                recentEvents.remove(recentEvents.size() - 1);
            }
        }
        
        public void recordTrade(String resourceType, int amount) {
            tradeCounts.merge(resourceType, amount, Integer::sum);
            adjustRelationship(Math.min(5, amount / 3), "Traded " + amount + " " + resourceType);
        }
        
        private void updateRelationshipStatus() {
            if (relationshipScore >= 70) {
                status = RelationshipStatus.ALLIED;
            } else if (relationshipScore >= 30) {
                status = RelationshipStatus.FRIENDLY;
            } else if (relationshipScore > -30) {
                status = RelationshipStatus.NEUTRAL;
            } else if (relationshipScore > -70) {
                status = RelationshipStatus.UNFRIENDLY;
            } else {
                status = RelationshipStatus.HOSTILE;
            }
        }
    }
    
    public enum RelationshipStatus {
        ALLIED("Allied", "§2"), // Strong green - full alliance with shared resources
        FRIENDLY("Friendly", "§a"), // Light green - cooperative but independent
        NEUTRAL("Neutral", "§e"), // Yellow - no significant relations
        UNFRIENDLY("Unfriendly", "§6"), // Gold/Orange - minor tensions
        HOSTILE("Hostile", "§c"); // Red - open hostility
        
        private final String displayName;
        private final String colorCode;
        
        RelationshipStatus(String displayName, String colorCode) {
            this.displayName = displayName;
            this.colorCode = colorCode;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public String getColoredDisplayName() {
            return colorCode + displayName + "§r";
        }
    }
    
    public record RelationshipEvent(int scoreChange, String description, long timestamp) {}
    
    /**
     * Village development data class
     */
    public static class VillageDevelopmentData {
        private final BlockPos center;
        private final String culture;
        private int developmentPoints;
        private int villageLevel;
        private final Map<String, Integer> buildingCounts = new HashMap<>();
        private final List<String> unlockedFeatures = new ArrayList<>();
        private final List<String> pendingUpgrades = new ArrayList<>();
        private UUID founderUUID;
        private final List<UUID> contributors = new ArrayList<>();
        // List of allied villages
        private final List<BlockPos> allies = new ArrayList<>();
        // List of rival/enemy villages
        private final List<BlockPos> rivals = new ArrayList<>();
        
        public VillageDevelopmentData(BlockPos center, String culture) {
            this.center = center;
            this.culture = culture;
            this.developmentPoints = 0;
            this.villageLevel = 1;
            initBasicBuildings();
        }
        
        private void initBasicBuildings() {
            buildingCounts.put("house", 3);
            buildingCounts.put("farm", 1);
            
            switch (culture.toLowerCase()) {
                case "roman" -> {
                    buildingCounts.put("forum", 1);
                }
                case "egyptian" -> {
                    buildingCounts.put("shrine", 1);
                }
                case "victorian" -> {
                    buildingCounts.put("market", 1);
                }
                case "nyc" -> {
                    buildingCounts.put("plaza", 1);
                }
                case "nether" -> {
                    buildingCounts.put("altar", 1);
                }
                case "end" -> {
                    buildingCounts.put("observatory", 1);
                }
            }
        }
        
        public BlockPos getCenter() {
            return center;
        }
        
        public String getCulture() {
            return culture;
        }
        
        public int getDevelopmentPoints() {
            return developmentPoints;
        }
        
        public int getVillageLevel() {
            return villageLevel;
        }
        
        public Map<String, Integer> getBuildingCounts() {
            return Collections.unmodifiableMap(buildingCounts);
        }
        
        public List<String> getUnlockedFeatures() {
            return Collections.unmodifiableList(unlockedFeatures);
        }
        
        public List<String> getPendingUpgrades() {
            return Collections.unmodifiableList(pendingUpgrades);
        }
        
        public UUID getFounderUUID() {
            return founderUUID;
        }
        
        public void setFounder(UUID playerUUID) {
            if (this.founderUUID == null) {
                this.founderUUID = playerUUID;
                if (!contributors.contains(playerUUID)) {
                    contributors.add(playerUUID);
                }
            }
        }
        
        public List<UUID> getContributors() {
            return Collections.unmodifiableList(contributors);
        }
        
        public List<BlockPos> getAllies() {
            return Collections.unmodifiableList(allies);
        }
        
        public List<BlockPos> getRivals() {
            return Collections.unmodifiableList(rivals);
        }
        
        public void updateAllyList(BlockPos otherVillage, boolean isAlly) {
            if (isAlly) {
                if (!allies.contains(otherVillage)) {
                    allies.add(otherVillage);
                }
                rivals.remove(otherVillage);
            } else {
                allies.remove(otherVillage);
                if (!rivals.contains(otherVillage)) {
                    rivals.add(otherVillage);
                }
            }
        }
        
        public void addDevelopmentPoints(int points) {
            this.developmentPoints += points;
            checkForLevelUp();
        }
        
        /**
         * Check if village should level up based on development points
         */
        private void checkForLevelUp() {
            int newLevel = 1;
            for (int threshold : DEVELOPMENT_THRESHOLDS) {
                if (developmentPoints >= threshold) {
                    newLevel++;
                } else {
                    break;
                }
            }
            
            if (newLevel > villageLevel) {
                int levelsGained = newLevel - villageLevel;
                villageLevel = newLevel;
                
                // Add pending upgrades based on level gain
                for (int i = 0; i < levelsGained; i++) {
                    addRandomUpgrade();
                }
            }
        }
        
        /**
         * Add a random upgrade to the pending upgrades list
         */
        private void addRandomUpgrade() {
            List<String> possibleUpgrades = new ArrayList<>();
            
            // Generic upgrades available to all cultures
            possibleUpgrades.add("additional_villager");
            possibleUpgrades.add("enhanced_farms");
            possibleUpgrades.add("better_housing");
            
            // Culture specific upgrades
            switch (culture.toLowerCase()) {
                case "roman" -> {
                    possibleUpgrades.add("bathhouse");
                    possibleUpgrades.add("aqueduct");
                    possibleUpgrades.add("colosseum");
                    possibleUpgrades.add("temple");
                }
                case "egyptian" -> {
                    possibleUpgrades.add("obelisk");
                    possibleUpgrades.add("pyramid");
                    possibleUpgrades.add("sphinx");
                    possibleUpgrades.add("temple_complex");
                }
                case "victorian" -> {
                    possibleUpgrades.add("factory");
                    possibleUpgrades.add("train_station");
                    possibleUpgrades.add("park");
                    possibleUpgrades.add("townhall");
                }
                case "nyc" -> {
                    possibleUpgrades.add("skyscraper");
                    possibleUpgrades.add("subway");
                    possibleUpgrades.add("cafe");
                    possibleUpgrades.add("art_gallery");
                }
                case "nether" -> {
                    possibleUpgrades.add("forge");
                    possibleUpgrades.add("soul_chamber");
                    possibleUpgrades.add("fire_temple");
                    possibleUpgrades.add("netherbrick_fortress");
                }
                case "end" -> {
                    possibleUpgrades.add("chorus_garden");
                    possibleUpgrades.add("void_portal");
                    possibleUpgrades.add("ender_spire");
                    possibleUpgrades.add("crystal_chamber");
                }
            }
            
            // Remove already unlocked features
            possibleUpgrades.removeAll(unlockedFeatures);
            possibleUpgrades.removeAll(pendingUpgrades);
            
            // Add a random upgrade if possible
            if (!possibleUpgrades.isEmpty()) {
                Random random = new Random();
                String upgrade = possibleUpgrades.get(random.nextInt(possibleUpgrades.size()));
                pendingUpgrades.add(upgrade);
            }
        }
        
        /**
         * Complete a pending village upgrade
         */
        public void completeUpgrade(String upgradeName) {
            if (pendingUpgrades.contains(upgradeName)) {
                pendingUpgrades.remove(upgradeName);
                unlockedFeatures.add(upgradeName);
                
                // Update building counts
                if (upgradeName.equals("additional_villager")) {
                    // This is not a building but a special feature
                } else if (upgradeName.equals("enhanced_farms")) {
                    buildingCounts.put("farm", buildingCounts.getOrDefault("farm", 0) + 1);
                } else if (upgradeName.equals("better_housing")) {
                    buildingCounts.put("house", buildingCounts.getOrDefault("house", 0) + 2);
                } else {
                    // It's a specific cultural building
                    buildingCounts.put(upgradeName, buildingCounts.getOrDefault(upgradeName, 0) + 1);
                }
            }
        }
    }
    
    /**
     * Player contribution activity
     */
    public static class PlayerContribution {
        private final UUID playerUUID;
        private final BlockPos villageCenter;
        private final String contributionType;
        private int progress;
        private final int requiredProgress;
        private final String buildingType;
        
        public PlayerContribution(UUID playerUUID, BlockPos villageCenter, String contributionType, 
                                 int requiredProgress, String buildingType) {
            this.playerUUID = playerUUID;
            this.villageCenter = villageCenter;
            this.contributionType = contributionType;
            this.progress = 0;
            this.requiredProgress = requiredProgress;
            this.buildingType = buildingType;
        }
        
        public UUID getPlayerUUID() {
            return playerUUID;
        }
        
        public BlockPos getVillageCenter() {
            return villageCenter;
        }
        
        public String getContributionType() {
            return contributionType;
        }
        
        public int getProgress() {
            return progress;
        }
        
        public int getRequiredProgress() {
            return requiredProgress;
        }
        
        public String getBuildingType() {
            return buildingType;
        }
        
        public void addProgress(int amount) {
            this.progress += amount;
        }
        
        public boolean isComplete() {
            return progress >= requiredProgress;
        }
    }
    
    /**
     * Register a new village for development tracking
     */
    public void registerVillage(BlockPos center, String culture) {
        if (!villageDevelopment.containsKey(center)) {
            villageDevelopment.put(center, new VillageDevelopmentData(center, culture));
            
            // Initialize relationships with existing villages
            for (BlockPos otherVillage : villageDevelopment.keySet()) {
                if (!otherVillage.equals(center)) {
                    VillagePair pair = VillagePair.of(center, otherVillage);
                    if (!villageRelationships.containsKey(pair)) {
                        VillageRelationship relationship = createInitialRelationship(
                            villageDevelopment.get(center).getCulture(),
                            villageDevelopment.get(otherVillage).getCulture()
                        );
                        villageRelationships.put(pair, relationship);
                    }
                }
            }
        }
    }
    
    /**
     * Create an initial relationship between two villages based on their cultures
     */
    private VillageRelationship createInitialRelationship(String culture1, String culture2) {
        VillageRelationship relationship = new VillageRelationship();
        
        // Different cultures may have predisposed relationships
        if (culture1.equals(culture2)) {
            // Same culture tends to be friendly
            relationship.adjustRelationship(30, "Cultural kinship");
        } else if ((culture1.equals("nether") && !culture2.equals("end")) || 
                 (culture2.equals("nether") && !culture1.equals("end"))) {
            // Nether villages tend to be hostile to non-End villages
            relationship.adjustRelationship(-40, "Cultural tensions");
        } else if ((culture1.equals("end") && culture2.equals("nether")) || 
                 (culture2.equals("end") && culture1.equals("nether"))) {
            // End and Nether have an ancient understanding
            relationship.adjustRelationship(10, "Dimensional understanding");
        } else if (culture1.equals("roman") && culture2.equals("egyptian") || 
                 culture2.equals("roman") && culture1.equals("egyptian")) {
            // Historical rivalry
            relationship.adjustRelationship(-20, "Historical tensions");
        } else if (culture1.equals("victorian") && culture2.equals("nyc") || 
                 culture2.equals("victorian") && culture1.equals("nyc")) {
            // Modern cultures understand each other
            relationship.adjustRelationship(20, "Modern cultural similarities");
        }
        
        return relationship;
    }
    
    /**
     * Get development data for a village
     */
    public VillageDevelopmentData getVillageDevelopment(BlockPos villageCenter) {
        return villageDevelopment.get(villageCenter);
    }
    
    /**
     * Find the nearest village to a position
     */
    public BlockPos findNearestVillage(BlockPos pos) {
        if (villageDevelopment.isEmpty()) {
            return null;
        }
        
        BlockPos nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        
        for (BlockPos center : villageDevelopment.keySet()) {
            double distance = center.getSquaredDistance(pos);
            if (distance < nearestDistance) {
                nearest = center;
                nearestDistance = distance;
            }
        }
        
        return nearest;
    }
    
    /**
     * Get all villages registered for development
     */
    public Collection<VillageDevelopmentData> getAllVillages() {
        return Collections.unmodifiableCollection(villageDevelopment.values());
    }
    
    /**
     * Get the relationship between two villages
     */
    public VillageRelationship getVillageRelationship(BlockPos village1, BlockPos village2) {
        VillagePair pair = VillagePair.of(village1, village2);
        return villageRelationships.get(pair);
    }
    
    /**
     * Adjust the relationship between two villages
     * @param village1 First village position
     * @param village2 Second village position
     * @param amount Amount to adjust (-100 to +100)
     * @param reason Description of why the relationship changed
     * @return The new relationship status
     */
    public RelationshipStatus adjustVillageRelationship(BlockPos village1, BlockPos village2, int amount, String reason) {
        VillagePair pair = VillagePair.of(village1, village2);
        VillageRelationship relationship = villageRelationships.get(pair);
        
        if (relationship == null) {
            if (!villageDevelopment.containsKey(village1) || !villageDevelopment.containsKey(village2)) {
                return RelationshipStatus.NEUTRAL; // One or both villages don't exist
            }
            
            relationship = createInitialRelationship(
                villageDevelopment.get(village1).getCulture(),
                villageDevelopment.get(village2).getCulture()
            );
            villageRelationships.put(pair, relationship);
        }
        
        // Adjust the relationship
        relationship.adjustRelationship(amount, reason);
        
        // Update the ally/rival lists in the village data
        updateVillageRelationshipLists(village1, village2, relationship.getStatus());
        
        return relationship.getStatus();
    }
    
    /**
     * Update the ally and rival lists in village development data
     */
    private void updateVillageRelationshipLists(BlockPos village1, BlockPos village2, RelationshipStatus status) {
        VillageDevelopmentData data1 = villageDevelopment.get(village1);
        VillageDevelopmentData data2 = villageDevelopment.get(village2);
        
        if (data1 != null && data2 != null) {
            boolean areAllies = (status == RelationshipStatus.ALLIED || status == RelationshipStatus.FRIENDLY);
            boolean areRivals = (status == RelationshipStatus.UNFRIENDLY || status == RelationshipStatus.HOSTILE);
            
            if (areAllies) {
                data1.updateAllyList(village2, true);
                data2.updateAllyList(village1, true);
            } else if (areRivals) {
                data1.updateAllyList(village2, false);
                data2.updateAllyList(village1, false);
            }
        }
    }
    
    /**
     * Record theft event and impact on inter-village relations
     * @param thiefUUID The UUID of the player who stole
     * @param victimUUID The UUID of the villager who was stolen from
     * @param itemCount Number of items stolen
     */
    public void recordTheft(UUID thiefUUID, UUID victimUUID, int itemCount) {
        // First determine which villages the thief and victim belong to
        BlockPos thiefVillage = findVillageOfPlayer(thiefUUID);
        BlockPos victimVillage = findVillageOfVillager(victimUUID);
        
        if (thiefVillage != null && victimVillage != null && !thiefVillage.equals(victimVillage)) {
            // Calculate relationship penalty based on theft severity
            int penalty = Math.min(20, 5 + (itemCount * 2));
            
            // Apply relationship penalty
            adjustVillageRelationship(
                thiefVillage,
                victimVillage,
                -penalty,
                "Theft by village member"
            );
        }
    }
    
    /**
     * Find which village a player is associated with (as founder or major contributor)
     */
    public BlockPos findVillageOfPlayer(UUID playerUUID) {
        // Check for villages where the player is founder
        for (Map.Entry<BlockPos, VillageDevelopmentData> entry : villageDevelopment.entrySet()) {
            if (playerUUID.equals(entry.getValue().getFounderUUID())) {
                return entry.getKey();
            }
        }
        
        // If not a founder, check where they have contributed the most
        Map<BlockPos, Integer> contributionCount = new HashMap<>();
        for (Map.Entry<BlockPos, VillageDevelopmentData> entry : villageDevelopment.entrySet()) {
            if (entry.getValue().getContributors().contains(playerUUID)) {
                contributionCount.merge(entry.getKey(), 1, Integer::sum);
            }
        }
        
        if (!contributionCount.isEmpty()) {
            return Collections.max(contributionCount.entrySet(), Map.Entry.comparingByValue()).getKey();
        }
        
        return null;
    }
    
    /**
     * Find which village a villager belongs to
     */
    public BlockPos findVillageOfVillager(UUID villagerUUID) {
        VillagerEntity villager = null;
        
        // This would be more efficient with a direct lookup, but we'll search all villages
        for (Map.Entry<BlockPos, VillageDevelopmentData> entry : villageDevelopment.entrySet()) {
            BlockPos center = entry.getKey();
            // Find server world for this village center
            for (ServerWorld world : Objects.requireNonNull(VillagerManager.getInstance().getServer()).getWorlds()) {
                // Check if world contains this village center
                if (world.isChunkLoaded(center.getX() >> 4, center.getZ() >> 4)) {
                    // Look for villager in a reasonable radius around the village center
                    List<VillagerEntity> villagers = world.getEntitiesByClass(
                        VillagerEntity.class,
                        new Box(center).expand(64),
                        v -> v.getUuid().equals(villagerUUID)
                    );
                    
                    if (!villagers.isEmpty()) {
                        return center;
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * Start a building contribution for a player
     */
    public void startBuildingContribution(ServerPlayerEntity player, BlockPos villageCenter, String buildingType) {
        UUID playerUUID = player.getUuid();
        VillageDevelopmentData village = getVillageDevelopment(villageCenter);
        
        // Check if player already has an active contribution
        if (activeContributions.containsKey(playerUUID)) {
            player.sendMessage(Text.of("§cYou're already working on a village contribution.§r"), false);
            return;
        }
        
        // Check if village exists
        if (village == null) {
            player.sendMessage(Text.of("§cNo village found at this location.§r"), false);
            return;
        }
        
        // Add player as contributor
        village.addContributor(playerUUID);
        
        // Determine the required progress based on building type and player reputation
        int baseProgress = getBaseProgressForBuilding(buildingType);
        int reputation = PlayerEventParticipation.getInstance().getReputation(player, village.getCulture());
        int requiredProgress = adjustProgressByReputation(baseProgress, reputation);
        
        // Create the contribution
        PlayerContribution contribution = new PlayerContribution(
            playerUUID, 
            villageCenter,
            "building",
            requiredProgress,
            buildingType
        );
        
        activeContributions.put(playerUUID, contribution);
        
        // Notify the player
        player.sendMessage(Text.of("§6You've started work on a " + buildingType + " for the village.§r"), false);
        player.sendMessage(Text.of("§eProgress required: " + requiredProgress + "§r"), false);
        
        if (reputation > 0) {
            player.sendMessage(Text.of("§aYour reputation reduces the required effort!§r"), false);
        }
    }
    
    /**
     * Start a diplomatic mission between two villages
     * @param player The player undertaking the mission
     * @param sourceVillage The village the player is representing
     * @param targetVillage The village the player is visiting
     */
    public void startDiplomaticMission(ServerPlayerEntity player, BlockPos sourceVillage, BlockPos targetVillage) {
        UUID playerUUID = player.getUuid();
        VillageDevelopmentData source = getVillageDevelopment(sourceVillage);
        VillageDevelopmentData target = getVillageDevelopment(targetVillage);
        
        // Check if player already has an active contribution
        if (activeContributions.containsKey(playerUUID)) {
            player.sendMessage(Text.of("§cYou're already working on a village contribution.§r"), false);
            return;
        }
        
        // Check if villages exist
        if (source == null || target == null) {
            player.sendMessage(Text.of("§cOne or both villages don't exist.§r"), false);
            return;
        }
        
        // Check if player is a contributor to source village
        if (!source.getContributors().contains(playerUUID) && !playerUUID.equals(source.getFounderUUID())) {
            player.sendMessage(Text.of("§cYou need to be a contributor to " + source.getCulture() + 
                                     " village to represent them diplomatically.§r"), false);
            return;
        }
        
        // Determine the required progress based on current relationship
        VillageRelationship relationship = getVillageRelationship(sourceVillage, targetVillage);
        int requiredProgress = getDiplomacyRequiredProgress(relationship);
        
        // Create the contribution
        PlayerContribution contribution = new PlayerContribution(
            playerUUID, 
            targetVillage,
            "diplomacy",
            requiredProgress,
            relationship.getStatus().name()
        );
        
        activeContributions.put(playerUUID, contribution);
        
        // Notify the player
        player.sendMessage(Text.of("§6You've started a diplomatic mission to the " + 
                                  target.getCulture() + " village.§r"), false);
        player.sendMessage(Text.of("§eCurrent relations: " + relationship.getStatus().getColoredDisplayName()), false);
        player.sendMessage(Text.of("§eProgress required: " + requiredProgress + "§r"), false);
    }
    
    /**
     * Get the progress required for a diplomatic mission based on current relationship
     */
    private int getDiplomacyRequiredProgress(VillageRelationship relationship) {
        if (relationship == null) return 50;
        
        return switch (relationship.getStatus()) {
            case ALLIED -> 20;      // Easy to maintain alliance
            case FRIENDLY -> 30;    // Relatively easy
            case NEUTRAL -> 50;     // Medium difficulty
            case UNFRIENDLY -> 70;  // More challenging
            case HOSTILE -> 100;    // Very difficult to improve
        };
    }
    
    /**
     * Record progress for a player's active contribution
     */
    public void recordContributionProgress(ServerPlayerEntity player, int amount) {
        UUID playerUUID = player.getUuid();
        PlayerContribution contribution = activeContributions.get(playerUUID);
        
        if (contribution == null) {
            return;
        }
        
        contribution.addProgress(amount);
        
        // Check if completed
        if (contribution.isComplete()) {
            completeContribution(player, contribution);
        } else {
            // Notify of progress
            if (contribution.getProgress() % 5 == 0 || contribution.getProgress() + amount >= contribution.getRequiredProgress()) { 
                player.sendMessage(Text.of("§aContribution progress: " + contribution.getProgress() + 
                    "/" + contribution.getRequiredProgress() + "§r"), false);
            }
        }
    }
    
    /**
     * Complete a player's contribution
     */
    private void completeContribution(ServerPlayerEntity player, PlayerContribution contribution) {
        // Get the village data
        VillageDevelopmentData village = getVillageDevelopment(contribution.getVillageCenter());
        if (village == null) {
            return;
        }
        
        switch (contribution.getContributionType()) {
            case "building" -> {
                // Check if it's a special upgrade
                if (village.pendingUpgrades.contains(contribution.getBuildingType())) {
                    // It's a village upgrade
                    village.completeUpgrade(contribution.getBuildingType());
                    player.sendMessage(Text.of("§6Village upgrade completed: " + contribution.getBuildingType() + "!§r"), false);
                    
                    // Add development points
                    int points = getDevelopmentPointsForUpgrade(contribution.getBuildingType());
                    village.addDevelopmentPoints(points);
                    player.sendMessage(Text.of("§aVillage development: +" + points + " points!§r"), false);
                } else {
                    // It's a regular building
                    String buildingType = contribution.getBuildingType();
                    village.buildingCounts.put(buildingType, village.buildingCounts.getOrDefault(buildingType, 0) + 1);
                    player.sendMessage(Text.of("§6Building completed: " + buildingType + "!§r"), false);
                    
                    // Add development points
                    int points = getDevelopmentPointsForBuilding(buildingType);
                    village.addDevelopmentPoints(points);
                    player.sendMessage(Text.of("§aVillage development: +" + points + " points!§r"), false);
                }
                
                // Add reputation for the player
                PlayerEventParticipation.getInstance().addReputationPoints(
                    player, 
                    village.getCulture(), 
                    getReputationForBuilding(contribution.getBuildingType())
                );
                
                // Special effects for completion
                World world = player.getWorld();
                if (world instanceof ServerWorld serverWorld) {
                    BlockPos pos = player.getBlockPos();
                    serverWorld.spawnParticles(
                        ParticleTypes.HAPPY_VILLAGER,
                        pos.getX(), pos.getY() + 1, pos.getZ(),
                        20, 0.5, 0.5, 0.5, 0.1
                    );
                }
            }
            case "diplomacy" -> {
                // Find the other village
                BlockPos sourceVillage = findVillageOfPlayer(player.getUuid());
                BlockPos targetVillage = contribution.getVillageCenter();
                
                if (sourceVillage != null && !sourceVillage.equals(targetVillage)) {
                    // Improve the relationship
                    RelationshipStatus previousStatus = getVillageRelationship(sourceVillage, targetVillage).getStatus();
                    RelationshipStatus newStatus = adjustVillageRelationship(
                        sourceVillage, targetVillage, 15, "Diplomatic mission by " + player.getName().getString());
                    
                    player.sendMessage(Text.of("§6Diplomatic mission complete!§r"), false);
                    player.sendMessage(Text.of("§eRelationship improved from " + previousStatus.getColoredDisplayName() + 
                                           " to " + newStatus.getColoredDisplayName()), false);
                    
                    // Add reputation for being a successful diplomat
                    PlayerEventParticipation.getInstance().addReputationPoints(
                        player, 
                        getVillageDevelopment(sourceVillage).getCulture(), 
                        15
                    );
                    
                    // Special effects for completion
                    World world = player.getWorld();
                    if (world instanceof ServerWorld serverWorld) {
                        BlockPos pos = player.getBlockPos();
                        serverWorld.spawnParticles(
                            ParticleTypes.HAPPY_VILLAGER,
                            pos.getX(), pos.getY() + 1, pos.getZ(),
                            20, 0.5, 0.5, 0.5, 0.1
                        );
                    }
                }
            }
        }
        
        // Remove the active contribution
        activeContributions.remove(player.getUuid());
        
        // Check if village leveled up
        int newLevel = village.getVillageLevel();
        if (newLevel > 1) {
            player.sendMessage(Text.of("§6§lVillage level: " + newLevel + "§r"), false);
            player.sendMessage(Text.of("§eNew upgrades available!§r"), false);
        }
    }
    
    /**
     * Get a player's active contribution
     */
    public PlayerContribution getPlayerContribution(UUID playerUUID) {
        return activeContributions.get(playerUUID);
    }
    
    /**
     * Cancel a player's active contribution
     */
    public void cancelContribution(ServerPlayerEntity player) {
        UUID playerUUID = player.getUuid();
        if (activeContributions.containsKey(playerUUID)) {
            activeContributions.remove(playerUUID);
            player.sendMessage(Text.of("§cYour village contribution has been canceled.§r"), false);
        }
    }
    
    /**
     * Get the list of pending upgrades for a village
     */
    public List<String> getAvailableUpgrades(BlockPos villageCenter) {
        VillageDevelopmentData data = getVillageDevelopment(villageCenter);
        return data != null ? data.getPendingUpgrades() : Collections.emptyList();
    }
    
    /**
     * Get base progress required for different building types
     */
    private int getBaseProgressForBuilding(String buildingType) {
        return switch (buildingType.toLowerCase()) {
            case "house" -> 30;
            case "farm" -> 25;
            case "shop" -> 40;
            case "bathhouse", "temple", "obelisk", "factory", "skyscraper" -> 60;
            case "aqueduct", "pyramid", "train_station", "subway" -> 80;
            case "colosseum", "sphinx", "townhall", "art_gallery" -> 100;
            case "forge", "soul_chamber", "fire_temple" -> 70;
            case "netherbrick_fortress", "void_portal", "ender_spire", "crystal_chamber" -> 90;
            default -> 50;
        };
    }
    
    /**
     * Adjust required progress based on player reputation with the culture
     */
    private int adjustProgressByReputation(int baseProgress, int reputation) {
        if (reputation <= 0) {
            return baseProgress;
        }
        
        // Reduce progress requirement by up to 50% based on reputation
        float reputationFactor = Math.min(0.5f, reputation / 200.0f);
        return (int) (baseProgress * (1.0 - reputationFactor));
    }
    
    /**
     * Get development points awarded for different building types
     */
    private int getDevelopmentPointsForBuilding(String buildingType) {
        return switch (buildingType.toLowerCase()) {
            case "house" -> 10;
            case "farm" -> 15;
            case "shop" -> 20;
            case "bathhouse", "temple", "obelisk", "factory", "skyscraper" -> 30;
            case "aqueduct", "pyramid", "train_station", "subway" -> 50;
            case "colosseum", "sphinx", "townhall", "art_gallery" -> 70;
            case "forge", "soul_chamber" -> 35;
            case "fire_temple", "netherbrick_fortress", "void_portal", "ender_spire", "crystal_chamber" -> 60;
            default -> 20;
        };
    }
    
    /**
     * Get development points for special village upgrades
     */
    private int getDevelopmentPointsForUpgrade(String upgrade) {
        return switch (upgrade) {
            case "additional_villager" -> 30;
            case "enhanced_farms" -> 40;
            case "better_housing" -> 35;
            default -> getDevelopmentPointsForBuilding(upgrade);
        };
    }
    
    /**
     * Get reputation points awarded for completing buildings
     */
    private int getReputationForBuilding(String buildingType) {
        return switch (buildingType.toLowerCase()) {
            case "house" -> 5;
            case "farm" -> 7;
            case "shop" -> 8;
            case "bathhouse", "temple", "obelisk", "factory", "skyscraper" -> 15;
            case "aqueduct", "pyramid", "train_station", "subway" -> 20;
            case "colosseum", "sphinx", "townhall", "art_gallery" -> 25;
            case "additional_villager", "enhanced_farms", "better_housing" -> 12;
            case "forge", "soul_chamber", "fire_temple", "netherbrick_fortress" -> 18;
            case "void_portal", "ender_spire", "crystal_chamber" -> 22;
            default -> 10;
        };
    }
    
    /**
     * Register a founding player for a village (first player to interact significantly)
     */
    public void registerVillageFounder(PlayerEntity player, BlockPos villageCenter) {
        VillageDevelopmentData data = getVillageDevelopment(villageCenter);
        if (data != null && data.getFounderUUID() == null) {
            data.setFounder(player.getUuid());
            
            if (player instanceof ServerPlayerEntity serverPlayer) {
                serverPlayer.sendMessage(Text.of("§6§lYou are now recognized as the founder of this " + 
                    data.getCulture() + " village!§r"), false);
                
                // Give bonus development points for founding
                data.addDevelopmentPoints(50);
                
                // Give bonus reputation
                PlayerEventParticipation.getInstance().addReputationPoints(
                    serverPlayer, data.getCulture(), 25);
            }
        }
    }
}