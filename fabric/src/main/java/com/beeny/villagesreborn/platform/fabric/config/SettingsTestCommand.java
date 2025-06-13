package com.beeny.villagesreborn.platform.fabric.config;

import com.beeny.villagesreborn.core.config.WorldSettingsManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple command to test and demonstrate world settings functionality
 */
public class SettingsTestCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(SettingsTestCommand.class);
    
    /**
     * Executes the settings test command
     */
    public static int execute(ServerCommandSource source) {
        try {
            ServerWorld world = source.getWorld();
            WorldSettingsManager settingsManager = WorldSettingsManager.getInstance();
            
            // Send header
            source.sendFeedback(() -> Text.literal("§6=== Villages Reborn Settings Test ==="), false);
            
            // Test AI Settings
            int memoryLimit = settingsManager.getVillagerMemoryLimit(world);
            float aggressionLevel = settingsManager.getAiAggressionLevel(world);
            boolean advancedAI = settingsManager.isAdvancedAIEnabled(world);
            
            source.sendFeedback(() -> Text.literal("§e--- AI Settings ---"), false);
            source.sendFeedback(() -> Text.literal("§7Villager Memory Limit: §f" + memoryLimit), false);
            source.sendFeedback(() -> Text.literal("§7AI Aggression Level: §f" + String.format("%.2f", aggressionLevel)), false);
            source.sendFeedback(() -> Text.literal("§7Advanced AI Enabled: §f" + advancedAI), false);
            
            // Test Expansion Settings
            boolean autoExpansion = settingsManager.isAutoExpansionEnabled(world);
            int maxVillageSize = settingsManager.getMaxVillageSize(world);
            float expansionRate = settingsManager.getExpansionRate(world);
            
            source.sendFeedback(() -> Text.literal("§e--- Expansion Settings ---"), false);
            source.sendFeedback(() -> Text.literal("§7Auto Expansion: §f" + autoExpansion), false);
            source.sendFeedback(() -> Text.literal("§7Max Village Size: §f" + maxVillageSize), false);
            source.sendFeedback(() -> Text.literal("§7Expansion Rate: §f" + String.format("%.2f", expansionRate)), false);
            
            // Test Feature Settings
            boolean elections = settingsManager.isElectionsEnabled(world);
            boolean dynamicTrading = settingsManager.isDynamicTradingEnabled(world);
            boolean relationships = settingsManager.isVillagerRelationshipsEnabled(world);
            
            source.sendFeedback(() -> Text.literal("§e--- Feature Settings ---"), false);
            source.sendFeedback(() -> Text.literal("§7Elections: §f" + elections), false);
            source.sendFeedback(() -> Text.literal("§7Dynamic Trading: §f" + dynamicTrading), false);
            source.sendFeedback(() -> Text.literal("§7Relationships: §f" + relationships), false);
            
            // Test Performance Settings
            boolean adaptivePerformance = settingsManager.isAdaptivePerformanceEnabled(world);
            int tickOptimization = settingsManager.getTickOptimizationLevel(world);
            
            source.sendFeedback(() -> Text.literal("§e--- Performance Settings ---"), false);
            source.sendFeedback(() -> Text.literal("§7Adaptive Performance: §f" + adaptivePerformance), false);
            source.sendFeedback(() -> Text.literal("§7Tick Optimization: §f" + tickOptimization), false);
            
            // Send footer
            source.sendFeedback(() -> Text.literal("§6=== End Settings Test ==="), false);
            source.sendFeedback(() -> Text.literal("§aSettings are working! Change them in the mod menu config."), false);
            
            LOGGER.info("Settings test command executed successfully for world: {}", world.getRegistryKey().getValue());
            return 1;
            
        } catch (Exception e) {
            LOGGER.error("Error executing settings test command", e);
            source.sendError(Text.literal("§cError testing settings: " + e.getMessage()));
            return 0;
        }
    }
} 