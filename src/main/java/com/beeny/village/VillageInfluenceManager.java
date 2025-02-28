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
    
    // Development thresholds for village evolution stages
    private static final int[] DEVELOPMENT_THRESHOLDS = {100, 250, 500, 1000, 2000};
    
    /**
     * Get singleton instance
     */
    public static VillageInfluenceManager getInstance() {
        return INSTANCE;
    }
    
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
        
        public void addContributor(UUID playerUUID) {
            if (!contributors.contains(playerUUID)) {
                contributors.add(playerUUID);
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
        }
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