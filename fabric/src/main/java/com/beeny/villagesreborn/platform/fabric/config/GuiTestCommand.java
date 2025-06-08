package com.beeny.villagesreborn.platform.fabric.config;

import com.beeny.villagesreborn.core.VillagesRebornCommon;
import com.beeny.villagesreborn.core.config.WorldSettingsManager;
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
 * Specialized command for testing GUI and AI integration issues
 */
public class GuiTestCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(GuiTestCommand.class);
    
    public static int execute(ServerCommandSource source) {
        try {
            source.sendFeedback(() -> Text.literal("§6╔══════════════════════════════════════════════════════╗"), false);
            source.sendFeedback(() -> Text.literal("§6║          VILLAGES REBORN - GUI & AI TEST            ║"), false);
            source.sendFeedback(() -> Text.literal("§6╚══════════════════════════════════════════════════════╝"), false);
            
            // Test GUI Translation Keys
            testTranslationKeys(source);
            
            // Test AI Integration
            testAIIntegration(source);
            
            // Test ModMenu Settings Access
            testModMenuAccess(source);
            
            // Test Villager AI Features
            testVillagerAIFeatures(source);
            
            source.sendFeedback(() -> Text.literal("§6╔══════════════════════════════════════════════════════╗"), false);
            source.sendFeedback(() -> Text.literal("§6║                GUI & AI TEST COMPLETE               ║"), false);
            source.sendFeedback(() -> Text.literal("§6╚══════════════════════════════════════════════════════╝"), false);
            
            LOGGER.info("GUI and AI test completed");
            return 1;
            
        } catch (Exception e) {
            source.sendFeedback(() -> Text.literal("§c§lERROR: GUI/AI test failed: " + e.getMessage()), false);
            LOGGER.error("GUI/AI test failed", e);
            return 0;
        }
    }
    
    private static void testTranslationKeys(ServerCommandSource source) {
        source.sendFeedback(() -> Text.literal("§e▶ Testing Translation Keys..."), false);
        
        try {
            // Test key translation keys
            var titleText = Text.translatable("villagesreborn.config.title");
            var aggressionText = Text.translatable("villagesreborn.config.ai.aggression");
            var aggressionLevelText = Text.translatable("villagesreborn.config.ai.aggression.level", "Medium");
            
            source.sendFeedback(() -> Text.literal("  §7Config Title: §f" + titleText.getString()), false);
            source.sendFeedback(() -> Text.literal("  §7Aggression Key: §f" + aggressionText.getString()), false);
            source.sendFeedback(() -> Text.literal("  §7Aggression Level: §f" + aggressionLevelText.getString()), false);
            
            // Check if translations are working or showing keys
            if (titleText.getString().contains("villagesreborn.config")) {
                source.sendFeedback(() -> Text.literal("  §c✗ Translation keys not resolving properly"), false);
                source.sendFeedback(() -> Text.literal("  §7Issue: Language files may not be loaded correctly"), false);
            } else {
                source.sendFeedback(() -> Text.literal("  §a✓ Translation keys working"), false);
            }
            
        } catch (Exception e) {
            source.sendFeedback(() -> Text.literal("  §c✗ Translation test error: " + e.getMessage()), false);
        }
    }
    
    private static void testAIIntegration(ServerCommandSource source) {
        source.sendFeedback(() -> Text.literal("§e▶ Testing AI Integration..."), false);
        
        try {
            var config = VillagesRebornCommon.getConfig();
            
            source.sendFeedback(() -> Text.literal("  §7API Key Valid: " + (config.hasValidApiKey() ? "§a✓ YES" : "§c✗ NO")), false);
            
            if (config.hasValidApiKey()) {
                source.sendFeedback(() -> Text.literal("  §7API Key Length: §f" + config.getLlmApiKey().length() + " characters"), false);
                
                // Test if AI classes are accessible
                try {
                    // Try to access AI-related classes
                    Class.forName("com.beeny.villagesreborn.core.ai.VillagerBrain");
                    source.sendFeedback(() -> Text.literal("  §a✓ VillagerBrain class accessible"), false);
                } catch (ClassNotFoundException e) {
                    source.sendFeedback(() -> Text.literal("  §c✗ VillagerBrain class not found"), false);
                }
                
                try {
                    Class.forName("com.beeny.villagesreborn.core.ai.social.SocialMemory");
                    source.sendFeedback(() -> Text.literal("  §a✓ SocialMemory class accessible"), false);
                } catch (ClassNotFoundException e) {
                    source.sendFeedback(() -> Text.literal("  §c✗ SocialMemory class not found"), false);
                }
                
            } else {
                source.sendFeedback(() -> Text.literal("  §c⚠ AI features disabled - no valid API key"), false);
            }
            
        } catch (Exception e) {
            source.sendFeedback(() -> Text.literal("  §c✗ AI integration test error: " + e.getMessage()), false);
        }
    }
    
    private static void testModMenuAccess(ServerCommandSource source) {
        source.sendFeedback(() -> Text.literal("§e▶ Testing ModMenu Integration..."), false);
        
        try {
            // Test if ModMenu integration class exists
            try {
                Class.forName("com.beeny.villagesreborn.platform.fabric.ModMenuIntegration");
                source.sendFeedback(() -> Text.literal("  §a✓ ModMenu integration class found"), false);
            } catch (ClassNotFoundException e) {
                source.sendFeedback(() -> Text.literal("  §c✗ ModMenu integration class not found"), false);
            }
            
            // Test if config screen class exists
            try {
                Class.forName("com.beeny.villagesreborn.platform.fabric.gui.VillagesRebornConfigScreen");
                source.sendFeedback(() -> Text.literal("  §a✓ Config screen class found"), false);
            } catch (ClassNotFoundException e) {
                source.sendFeedback(() -> Text.literal("  §c✗ Config screen class not found"), false);
            }
            
            source.sendFeedback(() -> Text.literal("  §7Known Issues:"), false);
            source.sendFeedback(() -> Text.literal("    §c• Done button not working"), false);
            source.sendFeedback(() -> Text.literal("    §c• Sliders not draggable"), false);
            source.sendFeedback(() -> Text.literal("    §c• Translation keys showing in UI"), false);
            
        } catch (Exception e) {
            source.sendFeedback(() -> Text.literal("  §c✗ ModMenu test error: " + e.getMessage()), false);
        }
    }
    
    private static void testVillagerAIFeatures(ServerCommandSource source) {
        source.sendFeedback(() -> Text.literal("§e▶ Testing Villager AI Features..."), false);
        
        try {
            ServerWorld world = source.getWorld();
            BlockPos playerPos = source.getPosition() != null ? BlockPos.ofFloored(source.getPosition()) : new BlockPos(0, 64, 0);
            
            Box searchBox = Box.of(playerPos.toCenterPos(), 100, 100, 100);
            List<VillagerEntity> villagers = world.getEntitiesByClass(VillagerEntity.class, searchBox, entity -> true);
            
            source.sendFeedback(() -> Text.literal("  §7Villagers found: §f" + villagers.size()), false);
            
            if (!villagers.isEmpty()) {
                VillagerEntity villager = villagers.get(0);
                
                // Test villager data access
                source.sendFeedback(() -> Text.literal("  §7Sample villager tests:"), false);
                source.sendFeedback(() -> Text.literal("    §7- Has custom data: " + (villager.hasCustomName() ? "§a✓" : "§7○")), false);
                source.sendFeedback(() -> Text.literal("    §7- Profession: §f" + villager.getVillagerData().getProfession()), false);
                source.sendFeedback(() -> Text.literal("    §7- Level: §f" + villager.getVillagerData().getLevel()), false);
                
                // Test AI features
                source.sendFeedback(() -> Text.literal("  §7AI Feature Status:"), false);
                source.sendFeedback(() -> Text.literal("    §c• Memory system: Not integrated"), false);
                source.sendFeedback(() -> Text.literal("    §c• Social relationships: Not working"), false);
                source.sendFeedback(() -> Text.literal("    §c• Personality traits: Not implemented"), false);
                source.sendFeedback(() -> Text.literal("    §c• Dynamic trading: Not functional"), false);
                
                source.sendFeedback(() -> Text.literal("  §c⚠ AI features need implementation/connection"), false);
                
            } else {
                source.sendFeedback(() -> Text.literal("  §7No villagers nearby to test AI features"), false);
                source.sendFeedback(() -> Text.literal("  §7Move closer to a village and run this test again"), false);
            }
            
        } catch (Exception e) {
            source.sendFeedback(() -> Text.literal("  §c✗ Villager AI test error: " + e.getMessage()), false);
        }
    }
} 