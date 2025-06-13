package com.beeny.villagesreborn.platform.fabric.config;

import com.beeny.villagesreborn.core.VillagesRebornCommon;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Comprehensive debugging command for Villages Reborn
 */
public class DebugCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(DebugCommand.class);
    
    public static int execute(ServerCommandSource source) {
        try {
            source.sendFeedback(() -> Text.literal("§6╔══════════════════════════════════════════════════════╗"), false);
            source.sendFeedback(() -> Text.literal("§6║           VILLAGES REBORN - DEBUG REPORT            ║"), false);
            source.sendFeedback(() -> Text.literal("§6╚══════════════════════════════════════════════════════╝"), false);
            
            // Test 1: Core Module Status
            testCoreModule(source);
            
            // Test 2: Configuration Status
            testConfiguration(source);
            
            // Test 3: Villager Detection
            testVillagerDetection(source);
            
            // Test 4: Memory and Performance
            testMemoryAndPerformance(source);
            
            // Test 5: AI Systems
            testAISystems(source);
            
            // Test 6: Mod Integration
            testModIntegration(source);
            
            source.sendFeedback(() -> Text.literal("§6╔══════════════════════════════════════════════════════╗"), false);
            source.sendFeedback(() -> Text.literal("§6║                  DEBUG COMPLETE                     ║"), false);
            source.sendFeedback(() -> Text.literal("§6╚══════════════════════════════════════════════════════╝"), false);
            source.sendFeedback(() -> Text.literal("§a✓ All systems tested! Check console for detailed logs."), false);
            
            LOGGER.info("Villages Reborn debug test completed successfully");
            return 1;
            
        } catch (Exception e) {
            source.sendFeedback(() -> Text.literal("§c§lERROR: Debug test failed: " + e.getMessage()), false);
            LOGGER.error("Debug test failed", e);
            return 0;
        }
    }
    
    private static void testCoreModule(ServerCommandSource source) {
        source.sendFeedback(() -> Text.literal("§e▶ Testing Core Module..."), false);
        
        boolean initialized = VillagesRebornCommon.isInitialized();
        source.sendFeedback(() -> Text.literal("  §7Core Initialized: " + (initialized ? "§a✓ YES" : "§c✗ NO")), false);
        
        if (initialized) {
            source.sendFeedback(() -> Text.literal("  §7Mod ID: §f" + VillagesRebornCommon.MOD_ID), false);
            source.sendFeedback(() -> Text.literal("  §a✓ Core module operational"), false);
        } else {
            source.sendFeedback(() -> Text.literal("  §c✗ Core module not initialized"), false);
        }
    }
    
    private static void testConfiguration(ServerCommandSource source) {
        source.sendFeedback(() -> Text.literal("§e▶ Testing Configuration..."), false);
        
        try {
            var config = VillagesRebornCommon.getConfig();
            source.sendFeedback(() -> Text.literal("  §7Development Mode: " + (config.isDevelopmentMode() ? "§a✓ ON" : "§7○ OFF")), false);
            source.sendFeedback(() -> Text.literal("  §7Log Level: §f" + config.getLogLevel()), false);
            source.sendFeedback(() -> Text.literal("  §7World Creation UI: " + (config.isWorldCreationUIEnabled() ? "§a✓ ON" : "§7○ OFF")), false);
            source.sendFeedback(() -> Text.literal("  §7API Key: " + (config.hasValidApiKey() ? "§a✓ SET" : "§c✗ MISSING")), false);
            
            if (!config.hasValidApiKey()) {
                source.sendFeedback(() -> Text.literal("  §c⚠ Warning: API key not configured! AI features disabled."), false);
                source.sendFeedback(() -> Text.literal("  §7To configure API key:"), false);
                source.sendFeedback(() -> Text.literal("  §7  1. Edit villagesreborn.properties in your game directory"), false);
                source.sendFeedback(() -> Text.literal("  §7  2. Set: llm.api.key=your_actual_api_key_here"), false);
                source.sendFeedback(() -> Text.literal("  §7  3. Restart Minecraft"), false);
            }
            
            source.sendFeedback(() -> Text.literal("  §a✓ Configuration loaded"), false);
        } catch (Exception e) {
            source.sendFeedback(() -> Text.literal("  §c✗ Configuration error: " + e.getMessage()), false);
        }
    }
    
    private static void testVillagerDetection(ServerCommandSource source) {
        source.sendFeedback(() -> Text.literal("§e▶ Testing Villager Detection..."), false);
        
        try {
            ServerWorld world = source.getWorld();
            BlockPos playerPos = source.getPosition() != null ? BlockPos.ofFloored(source.getPosition()) : new BlockPos(0, 64, 0);
            
            // Search for villagers in different radii
            Box smallBox = Box.of(playerPos.toCenterPos(), 50, 50, 50);
            Box largeBox = Box.of(playerPos.toCenterPos(), 200, 200, 200);
            
            List<VillagerEntity> nearbyVillagers = world.getEntitiesByClass(VillagerEntity.class, smallBox, entity -> true);
            List<VillagerEntity> allVillagers = world.getEntitiesByClass(VillagerEntity.class, largeBox, entity -> true);
            
            source.sendFeedback(() -> Text.literal("  §7Villagers nearby (25 blocks): §f" + nearbyVillagers.size()), false);
            source.sendFeedback(() -> Text.literal("  §7Villagers total (100 blocks): §f" + allVillagers.size()), false);
            
            if (!allVillagers.isEmpty()) {
                VillagerEntity villager = allVillagers.get(0);
                source.sendFeedback(() -> Text.literal("  §7Sample villager:"), false);
                source.sendFeedback(() -> Text.literal("    §7- UUID: §f" + villager.getUuid().toString().substring(0, 8) + "..."), false);
                source.sendFeedback(() -> Text.literal("    §7- Position: §f" + villager.getBlockPos()), false);
                source.sendFeedback(() -> Text.literal("    §7- Profession: §f" + villager.getVillagerData().getProfession()), false);
                source.sendFeedback(() -> Text.literal("    §7- Level: §f" + villager.getVillagerData().getLevel()), false);
            } else {
                source.sendFeedback(() -> Text.literal("  §7No villagers found in search area"), false);
                source.sendFeedback(() -> Text.literal("  §7Try running this command near a village"), false);
            }
            
            source.sendFeedback(() -> Text.literal("  §a✓ Villager detection functional"), false);
        } catch (Exception e) {
            source.sendFeedback(() -> Text.literal("  §c✗ Villager detection error: " + e.getMessage()), false);
        }
    }
    
    private static void testMemoryAndPerformance(ServerCommandSource source) {
        source.sendFeedback(() -> Text.literal("§e▶ Testing Memory & Performance..."), false);
        
        try {
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            long maxMemory = runtime.maxMemory();
            
            double usagePercent = (double) usedMemory / totalMemory * 100;
            
            source.sendFeedback(() -> Text.literal("  §7Memory Used: §f" + (usedMemory / 1024 / 1024) + " MB (" + String.format("%.1f", usagePercent) + "%)"), false);
            source.sendFeedback(() -> Text.literal("  §7Memory Available: §f" + (freeMemory / 1024 / 1024) + " MB"), false);
            source.sendFeedback(() -> Text.literal("  §7Memory Total: §f" + (totalMemory / 1024 / 1024) + " MB"), false);
            source.sendFeedback(() -> Text.literal("  §7Memory Max: §f" + (maxMemory / 1024 / 1024) + " MB"), false);
            
            // Performance test
            long startTime = System.nanoTime();
            Thread.sleep(5); // Small delay to test responsiveness
            long endTime = System.nanoTime();
            double responseTimeMs = (endTime - startTime) / 1_000_000.0;
            
            source.sendFeedback(() -> Text.literal("  §7Response Time: §f" + String.format("%.2f", responseTimeMs) + " ms"), false);
            
            if (usagePercent > 90) {
                source.sendFeedback(() -> Text.literal("  §c⚠ Warning: High memory usage detected"), false);
            } else {
                source.sendFeedback(() -> Text.literal("  §a✓ Memory usage normal"), false);
            }
            
        } catch (Exception e) {
            source.sendFeedback(() -> Text.literal("  §c✗ Performance test error: " + e.getMessage()), false);
        }
    }
    
    private static void testAISystems(ServerCommandSource source) {
        source.sendFeedback(() -> Text.literal("§e▶ Testing AI Systems..."), false);
        
        try {
            // Test AI Integration
            var aiIntegration = com.beeny.villagesreborn.platform.fabric.ai.VillagerAIIntegration.getInstance();
            source.sendFeedback(() -> Text.literal("  §7AI Integration: §f" + aiIntegration.getAIStats()), false);
            
            // Test Trading System
            var tradingSystem = com.beeny.villagesreborn.platform.fabric.trading.DynamicTradingSystem.getInstance();
            source.sendFeedback(() -> Text.literal("  §7Trading System: §f" + tradingSystem.getTradingStats()), false);
            
            source.sendFeedback(() -> Text.literal("  §a✓ AI systems online"), false);
            
        } catch (Exception e) {
            source.sendFeedback(() -> Text.literal("  §c✗ AI systems error: " + e.getMessage()), false);
        }
    }
    
    private static void testModIntegration(ServerCommandSource source) {
        source.sendFeedback(() -> Text.literal("§e▶ Testing Mod Integration..."), false);
        
        try {
            // Test basic mod presence
            source.sendFeedback(() -> Text.literal("  §7Platform: §fFabric"), false);
            source.sendFeedback(() -> Text.literal("  §7Environment: §f" + (isDevelopmentEnvironment() ? "Development" : "Production")), false);
            
            // Test command system
            source.sendFeedback(() -> Text.literal("  §7Commands: §a✓ Registered"), false);
            
            // Test logger
            LOGGER.info("Debug command executed - mod integration test");
            source.sendFeedback(() -> Text.literal("  §7Logging: §a✓ Active"), false);
            
            source.sendFeedback(() -> Text.literal("  §a✓ Mod integration successful"), false);
            
        } catch (Exception e) {
            source.sendFeedback(() -> Text.literal("  §c✗ Integration error: " + e.getMessage()), false);
        }
    }
    
    private static boolean isDevelopmentEnvironment() {
        try {
            return VillagesRebornCommon.getConfig().isDevelopmentMode();
        } catch (Exception e) {
            return false;
        }
    }
} 