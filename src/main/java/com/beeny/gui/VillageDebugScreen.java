package com.beeny.gui;

import com.beeny.Villagesreborn;
import com.beeny.village.Culture.CulturalTrait;
import com.beeny.config.VillagesConfig;
import com.beeny.village.Culture;
import com.beeny.village.Villager;
import com.beeny.village.VillageEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Debug screen for Villages Reborn mod.
 * 
 * This screen displays technical information about villages, cultures,
 * and other internal data to help with debugging.
 */
public class VillageDebugScreen extends Screen {
    private final List<String> debugInfo = new ArrayList<>();
    private final MinecraftClient client;
    private int scroll = 0;
    private static final int LINE_HEIGHT = 12;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    // Timestamp of when debug data was collected
    private final LocalDateTime timestamp = LocalDateTime.now();
    
    // Performance tracking
    private long collectionStartTime;
    private long collectionEndTime;
    
    public VillageDebugScreen() {
        super(Text.of("Village Debug Information"));
        this.client = MinecraftClient.getInstance();
        collectDebugInfo();
    }

    @Override
    protected void init() {
        super.init();
    }

    /**
     * Collects all debug information to be displayed
     */
    private void collectDebugInfo() {
        debugInfo.clear();
        collectionStartTime = System.nanoTime();
        
        // Header
        debugInfo.add("§e=== Villages Reborn Debug Information ===");
        debugInfo.add("§7Generated: " + timestamp.format(TIME_FORMATTER));
        debugInfo.add("");
        
        // Player position and world data
        collectPlayerAndWorldInfo();
        
        // Village information (if in a village)
        collectVillageInfo();
        
        // Villager data
        collectVillagerData();
        
        // Technical information
        collectTechnicalInfo();
        
        // Mod configuration
        collectConfigInfo();
        
        // Instructions for navigation
        debugInfo.add("");
        debugInfo.add("§7Scroll to view more information.");
        debugInfo.add("§7Press ESC to exit this screen.");
        
        collectionEndTime = System.nanoTime();
        // Add data collection time as the first technical stat
        debugInfo.add(13 + debugInfo.indexOf("§6=== Technical Information ==="), 
                 "§aData Collection Time: §f" + String.format("%.2f", (collectionEndTime - collectionStartTime) / 1_000_000.0) + " ms");
    }
    
    /**
     * Collects player and world information
     */
    private void collectPlayerAndWorldInfo() {
        if (client.player == null || client.world == null) return;
        
        World world = client.world;
        BlockPos playerPos = client.player.getBlockPos();
        Vec3d exactPos = client.player.getPos();
        
        debugInfo.add("§6=== Player & World Information ===");
        debugInfo.add("§aPlayer: §f" + client.player.getName().getString());
        debugInfo.add("§aBlock Position: §f" + playerPos.getX() + ", " + playerPos.getY() + ", " + playerPos.getZ());
        debugInfo.add("§aExact Position: §f" + String.format("%.2f, %.2f, %.2f", exactPos.x, exactPos.y, exactPos.z));
        debugInfo.add("§aDimension: §f" + world.getRegistryKey().getValue());
        debugInfo.add("§aBiome: §f" + world.getBiome(playerPos).getKey().map(key -> key.getValue().toString()).orElse("Unknown"));
        debugInfo.add("§aIn-game Time: §f" + world.getTimeOfDay() + " (Day " + (world.getTimeOfDay() / 24000) + ")");
        debugInfo.add("");
    }
    
    /**
     * Collects information about the current village (if any)
     */
    private void collectVillageInfo() {
        if (client.player == null) return;
        
        BlockPos playerPos = client.player.getBlockPos();
        Optional<Culture> culture = Optional.ofNullable(Villagesreborn.getInstance().getNearestVillageCulture(playerPos));
        
        if (culture.isPresent()) {
            Culture villageCulture = culture.get();
            debugInfo.add("§6=== Current Village Information ===");
            
            // Core village data
            debugInfo.add("§aCulture: §f" + villageCulture.getType().getId());
            debugInfo.add("§aType: §f" + villageCulture.getType());
            debugInfo.add("§aFoundation Date: §f" + villageCulture.getFoundationDate());
            debugInfo.add("§aAge: §f" + villageCulture.getAgeInDays() + " days");
            
            // Village metrics
            collectVillageMetrics(villageCulture, playerPos);
            
            // Village structures
            collectVillageStructures(villageCulture, playerPos);
            
            // Village events
            collectVillageEvents(villageCulture, playerPos);
            
            // Cultural traits
            debugInfo.add("§aTraits: §f" + villageCulture.getTraits().size() + " cultural traits");
            int traitCounter = 0;
            for (CulturalTrait trait : villageCulture.getTraits()) {
                if (traitCounter++ < 5) { // Limit to 5 traits to avoid clutter
                    debugInfo.add("  §7- §f" + trait);
                } else {
                    debugInfo.add("  §7- §f... and " + (villageCulture.getTraits().size() - 5) + " more");
                    break;
                }
            }
            
            // Biome preferences
            debugInfo.add("§aPreferred Biomes: §f" + villageCulture.getPreferredBiomes());
            
            // Village boundaries
            BlockPos center = Villagesreborn.getInstance().getVillageCenterPos(playerPos);
            if (center != null) {
                int distance = (int) Math.sqrt(playerPos.getSquaredDistance(center));
                debugInfo.add("§aVillage Center: §f" + center.getX() + ", " + center.getY() + ", " + center.getZ());
                debugInfo.add("§aDistance from Center: §f" + distance + " blocks");
                
                int radius = Villagesreborn.getInstance().getVillageRadius(playerPos);
                debugInfo.add("§aRadius: §f" + radius + " blocks");
                debugInfo.add("§aArea: §f" + (Math.PI * radius * radius) + " square blocks");
                
                // Village boundary status
                double distancePercent = (double) distance / radius * 100.0;
                String boundaryInfo = String.format("%.1f%% from boundary", 100 - distancePercent);
                if (distance > radius) {
                    boundaryInfo = String.format("§c%.1f blocks outside boundary", distance - radius);
                }
                debugInfo.add("§aBoundary Status: §f" + boundaryInfo);
            } else {
                debugInfo.add("§aVillage Center: §fUnknown");
                debugInfo.add("§aRadius: §fUnknown");
            }
        } else {
            debugInfo.add("§6=== Village Information ===");
            debugInfo.add("§cNo village detected at current position.");
            
            // Find the nearest village if any exist
            if (!Villagesreborn.getInstance().getAllVillages().isEmpty()) {
                Culture nearestVillage = null;
                double nearestDistance = Double.MAX_VALUE;
                
                for (Culture village : Villagesreborn.getInstance().getAllVillages()) {
                    BlockPos villageCenter = village.getCenterPos();
                    if (villageCenter != null) {
                        double distance = villageCenter.getSquaredDistance(playerPos);
                        if (distance < nearestDistance) {
                            nearestDistance = distance;
                            nearestVillage = village;
                        }
                    }
                }
                
                if (nearestVillage != null) {
                    BlockPos nearestCenter = nearestVillage.getCenterPos();
                    double distance = Math.sqrt(nearestCenter.getSquaredDistance(playerPos));
                    debugInfo.add("§aNearest Village: §f" + nearestVillage.getType().getId());
                    debugInfo.add("§aDistance: §f" + String.format("%.1f blocks", distance));
                    debugInfo.add("§aDirection: §f" + getDirection(playerPos, nearestCenter));
                }
            }
        }
        
        debugInfo.add("");
    }
    
    /**
     * Collects metrics about the village such as prosperity, safety, etc.
     */
    private void collectVillageMetrics(Culture culture, BlockPos playerPos) {
        // Prosperity metrics
        int prosperity = culture.getProsperity();
        int safety = culture.getSafety();
        int happiness = culture.getHappiness();
        int reputation = Villagesreborn.getInstance().getPlayerReputation(client.player.getUuid(), culture.getId());
        
        debugInfo.add("§a=== Village Metrics ===");
        debugInfo.add("§aProsperity: §f" + prosperity + "/100" + getMetricBar(prosperity));
        debugInfo.add("§aSafety: §f" + safety + "/100" + getMetricBar(safety));
        debugInfo.add("§aHappiness: §f" + happiness + "/100" + getMetricBar(happiness));
        debugInfo.add("§aYour Reputation: §f" + reputation + getReputationLabel(reputation));
        
        // Resources and economy
        Map<String, Integer> resources = culture.getResourceLevels();
        debugInfo.add("§aResource Levels:");
        resources.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .forEach(entry -> 
                    debugInfo.add("  §7- §f" + entry.getKey() + ": " + entry.getValue() + getMetricBar(entry.getValue()))
                );
        
        // Growth metrics
        debugInfo.add("§aGrowth Rate: §f" + culture.getGrowthRate() + "%");
        debugInfo.add("§aTrade Activity: §f" + culture.getTradeActivity() + " trades/day");
    }
    
    /**
     * Collects information about village structures
     */
    private void collectVillageStructures(Culture culture, BlockPos playerPos) {
        Map<String, Integer> structures = culture.getStructures();
        
        debugInfo.add("§a=== Village Structures ===");
        debugInfo.add("§aTotal Buildings: §f" + structures.values().stream().mapToInt(Integer::intValue).sum());
        
        // List most common structures
        structures.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(7)  // Show top 7 structures
                .forEach(entry -> 
                    debugInfo.add("  §7- §f" + entry.getKey() + ": " + entry.getValue())
                );
                
        // Special buildings
        BlockPos townHall = culture.getTownHallPos();
        if (townHall != null) {
            double distanceToTownHall = Math.sqrt(playerPos.getSquaredDistance(townHall));
            debugInfo.add("§aTown Hall: §f" + townHall.getX() + ", " + townHall.getY() + ", " + townHall.getZ() + 
                         " (" + String.format("%.1f", distanceToTownHall) + " blocks away)");
        }
    }
    
    /**
     * Collects information about active and upcoming village events
     */
    private void collectVillageEvents(Culture culture, BlockPos playerPos) {
        List<VillageEvent> events = culture.getActiveEvents();
        
        debugInfo.add("§a=== Village Events ===");
        debugInfo.add("§aActive Events: §f" + events.size());
        
        if (!events.isEmpty()) {
            for (int i = 0; i < Math.min(events.size(), 3); i++) {
                VillageEvent event = events.get(i);
                debugInfo.add("  §7- §f" + event.getName() + " - " + event.getDescription());
            }
            
            if (events.size() > 3) {
                debugInfo.add("  §7- §f... and " + (events.size() - 3) + " more");
            }
        }
        
        // Upcoming events
        List<VillageEvent> upcoming = culture.getUpcomingEvents();
        if (!upcoming.isEmpty()) {
            debugInfo.add("§aUpcoming Events: §f" + upcoming.size());
            for (int i = 0; i < Math.min(upcoming.size(), 2); i++) {
                VillageEvent event = upcoming.get(i);
                debugInfo.add("  §7- §f" + event.getName() + " (in " + event.getTimeUntilStart() + ")");
            }
        }
    }
    
    /**
     * Collects information about villagers in the current village
     */
    private void collectVillagerData() {
        if (client.player == null || client.world == null) return;
        
        BlockPos playerPos = client.player.getBlockPos();
        World world = client.world;
        
        debugInfo.add("§6=== Villager Information ===");
        
        // Get all nearby villagers (vanilla + custom)
        List<VillagerEntity> nearbyVillagers = world.getEntitiesByClass(
            VillagerEntity.class, 
            client.player.getBoundingBox().expand(50), 
            villager -> true
        );
        
        debugInfo.add("§aNearby Villagers: §f" + nearbyVillagers.size());
        
        // Group villagers by profession
        Map<String, Long> professionCounts = nearbyVillagers.stream()
            .collect(Collectors.groupingBy(
                villager -> villager.getVillagerData().getProfession().toString(), // Corrected based on user info
                Collectors.counting()
            ));
        
        professionCounts.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(5)
            .forEach(entry -> 
                debugInfo.add("  §7- §f" + entry.getKey().replace("minecraft:", "") + ": " + entry.getValue())
            );
        
        // Custom villager data if available
        Culture culture = Villagesreborn.getInstance().getNearestVillageCulture(playerPos);
        if (culture != null) {
            List<Villager> customVillagers = culture.getVillagers();
            
            debugInfo.add("§aTotal Village Population: §f" + customVillagers.size());
            debugInfo.add("§aAdults: §f" + customVillagers.stream().filter(v -> !v.isChild()).count());
            debugInfo.add("§aChildren: §f" + customVillagers.stream().filter(Villager::isChild).count());
            
            // Happiness distribution
            long happyVillagers = customVillagers.stream().filter(v -> v.getHappiness() > 75).count();
            long contentVillagers = customVillagers.stream().filter(v -> v.getHappiness() > 40 && v.getHappiness() <= 75).count();
            long unhappyVillagers = customVillagers.stream().filter(v -> v.getHappiness() <= 40).count();
            
            debugInfo.add("§aHappiness Distribution: §f");
            debugInfo.add("  §7- §aHappy (>75): §f" + happyVillagers + " (" + 
                        String.format("%.1f%%", (double) happyVillagers / customVillagers.size() * 100) + ")");
            debugInfo.add("  §7- §eContent (40-75): §f" + contentVillagers + " (" + 
                        String.format("%.1f%%", (double) contentVillagers / customVillagers.size() * 100) + ")");
            debugInfo.add("  §7- §cUnhappy (<40): §f" + unhappyVillagers + " (" + 
                        String.format("%.1f%%", (double) unhappyVillagers / customVillagers.size() * 100) + ")");
            
            // Notable villagers
            List<Villager> notableVillagers = customVillagers.stream()
                .filter(v -> v.isLeader() || v.getInfluence() > 70)
                .sorted(Comparator.comparingInt(Villager::getInfluence).reversed())
                .limit(3)
                .collect(Collectors.toList());
            
            if (!notableVillagers.isEmpty()) {
                debugInfo.add("§aNotable Villagers:");
                for (Villager notable : notableVillagers) {
                    String role = notable.isLeader() ? "Leader" : "Influential Citizen";
                    debugInfo.add("  §7- §f" + notable.getName() + " (" + role + ", Influence: " + notable.getInfluence() + ")");
                }
            }
        }
        
        debugInfo.add("");
    }
    
    /**
     * Collects technical information about the mod
     */
    private void collectTechnicalInfo() {
        debugInfo.add("§6=== Technical Information ===");
        
        // Mod version
        debugInfo.add("§aMod Version: §f" + Villagesreborn.VERSION);
        
        // Village data
        debugInfo.add("§aAll Villages: §f" + Villagesreborn.getInstance().getAllVillages().size());
        debugInfo.add("§aActive Village Events: §f" + Villagesreborn.getInstance().getTotalActiveEvents());
        
        // Game performance
        debugInfo.add("§aFPS: §f" + MinecraftClient.getInstance().getCurrentFps());
        
        // Memory usage
        long usedMemory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024;
        long totalMemory = Runtime.getRuntime().totalMemory() / 1024 / 1024;
        long maxMemory = Runtime.getRuntime().maxMemory() / 1024 / 1024;
        
        debugInfo.add("§aMemory Usage: §f" + usedMemory + "MB / " + totalMemory + "MB");
        debugInfo.add("§aMax Memory: §f" + maxMemory + "MB");
        
        // Thread information
        debugInfo.add("§aActive Threads: §f" + Thread.activeCount());
        
        // Tick time information if available
        float averageTickTime = Villagesreborn.getInstance().getAverageTickTime();
        if (averageTickTime > 0) {
            debugInfo.add("§aAvg Tick Time: §f" + String.format("%.2f", averageTickTime) + " ms");
        }
        
        // Network stats
        int pendingRequests = Villagesreborn.getInstance().getPendingNetworkRequests();
        debugInfo.add("§aPending Network Requests: §f" + pendingRequests);
        
        debugInfo.add("");
    }
    
    /**
     * Collects configuration information
     */
    private void collectConfigInfo() {
        VillagesConfig config = VillagesConfig.getInstance();
        
        debugInfo.add("§6=== Configuration Information ===");
        debugInfo.add("§aVillage Spawn Rate: §f" + config.getVillageSpawnRate());
        debugInfo.add("§aAI Provider: §f" + config.getLLMSettings().getProvider());
        debugInfo.add("§aModel Type: §f" + config.getLLMSettings().getModel());
        debugInfo.add("§aVillager PvP: §f" + (config.isVillagerPvPEnabled() ? "Enabled" : "Disabled"));
        debugInfo.add("§aTheft Detection: §f" + (config.isTheftDetectionEnabled() ? "Enabled" : "Disabled"));
        
        // UI settings
        debugInfo.add("§aConversation HUD Position: §f" + config.getUISettings().getConversationHudPosition());
        debugInfo.add("§aShow Culture in HUD: §f" + (config.getUISettings().isShowCulture() ? "Yes" : "No"));
        debugInfo.add("§aShow Profession in HUD: §f" + (config.getUISettings().isShowProfession() ? "Yes" : "No"));
        
        debugInfo.add("");
    }
    
    /**
     * Generates a text-based progress bar for metrics
     */
    private String getMetricBar(int value) {
        StringBuilder bar = new StringBuilder(" [");
        int bars = value / 10;
        
        for (int i = 0; i < 10; i++) {
            if (i < bars) {
                bar.append("§a|");
            } else {
                bar.append("§8|");
            }
        }
        
        bar.append("§r]");
        return bar.toString();
    }
    
    /**
     * Returns a label for player reputation
     */
    private String getReputationLabel(int reputation) {
        if (reputation >= 90) return " §a(Revered)";
        if (reputation >= 75) return " §a(Honored)";
        if (reputation >= 50) return " §2(Friendly)";
        if (reputation >= 25) return " §e(Neutral)";
        if (reputation >= 0) return " §6(Unfriendly)";
        if (reputation >= -25) return " §c(Hostile)";
        return " §4(Hated)";
    }
    
    /**
     * Calculates a cardinal direction between two points
     */
    private String getDirection(BlockPos from, BlockPos to) {
        int dx = to.getX() - from.getX();
        int dz = to.getZ() - from.getZ();
        
        double angle = Math.toDegrees(Math.atan2(dz, dx));
        if (angle < 0) angle += 360;
        
        if (angle >= 337.5 || angle < 22.5) return "East";
        if (angle >= 22.5 && angle < 67.5) return "Southeast";
        if (angle >= 67.5 && angle < 112.5) return "South";
        if (angle >= 112.5 && angle < 157.5) return "Southwest";
        if (angle >= 157.5 && angle < 202.5) return "West";
        if (angle >= 202.5 && angle < 247.5) return "Northwest";
        if (angle >= 247.5 && angle < 292.5) return "North";
        return "Northeast";
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Use the proper method signature for renderBackground in 1.21.4
        this.renderBackground(context, mouseX, mouseY, delta);
        
        // Draw heading with timestamp
        context.drawCenteredTextWithShadow(
            client.textRenderer, 
            "Villages Reborn Debug Info - " + timestamp.format(TIME_FORMATTER), 
            this.width / 2, 
            5, 
            0xFFFFFF
        );
        
        // Draw scroll indicator if there's more content
        if (debugInfo.size() * LINE_HEIGHT > height) {
            float scrollPercentage = (float) scroll / (debugInfo.size() * LINE_HEIGHT - height + 40);
            int scrollbarHeight = Math.max(15, height * height / (debugInfo.size() * LINE_HEIGHT));
            int scrollbarY = (int) (10 + scrollPercentage * (height - 20 - scrollbarHeight));
            
            // Draw scrollbar track
            context.fill(width - 10, 10, width - 8, height - 10, 0x30FFFFFF);
            // Draw scrollbar thumb
            context.fill(width - 10, scrollbarY, width - 8, scrollbarY + scrollbarHeight, 0xAAFFFFFF);
        }
        
        // Draw debug text
        int y = 20 - scroll;
        for (String line : debugInfo) {
            if (y >= 10 && y <= height - 10) {
                context.drawText(client.textRenderer, line, 20, y, 0xFFFFFF, true);
            }
            y += LINE_HEIGHT;
        }
        
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scroll -= verticalAmount * 4 * LINE_HEIGHT;
        scroll = Math.max(0, Math.min(scroll, debugInfo.size() * LINE_HEIGHT - height + 40));
        return true;
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { // ESC key
            this.client.setScreen(null);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    @Override
    public boolean shouldPause() {
        return false;
    }
}
