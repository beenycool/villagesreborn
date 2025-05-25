package com.beeny.villagesreborn.platform.fabric.gui;

import com.beeny.villagesreborn.core.config.FirstTimeSetupConfig;
import com.beeny.villagesreborn.core.hardware.HardwareInfo;
import com.beeny.villagesreborn.core.hardware.HardwareTier;
import com.beeny.villagesreborn.core.llm.LLMProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for WelcomeScreenHandler functionality
 */
class WelcomeScreenHandlerTest {
    
    private WelcomeScreenHandler handler;
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() {
        // Set system property to use temp directory for config files
        System.setProperty("user.dir", tempDir.toString());
        handler = new WelcomeScreenHandler();
    }
    
    @Test
    void testInitialization() {
        assertNotNull(handler);
        assertNotNull(handler.getSetupConfig());
        assertNotNull(handler.getHardwareManager());
        assertNotNull(handler.getLLMManager());
    }
    
    @Test
    void testInitialSetupState() {
        // On first run, setup should not be completed
        assertFalse(handler.isSetupCompleted());
    }
    
    @Test
    void testResetSetup() {
        // First, complete setup
        FirstTimeSetupConfig config = handler.getSetupConfig();
        config.completeSetup(LLMProvider.OPENAI, "gpt-3.5-turbo");
        
        // Verify setup is completed
        assertTrue(handler.isSetupCompleted());
        
        // Reset setup
        handler.resetSetup();
        
        // Verify setup is reset
        assertFalse(handler.isSetupCompleted());
    }
    
    @Test
    void testHardwareDetection() {
        HardwareInfo hardwareInfo = handler.getHardwareManager().getHardwareInfo();
        
        assertNotNull(hardwareInfo);
        assertTrue(hardwareInfo.getRamGB() >= 0);
        assertTrue(hardwareInfo.getCpuCores() >= 0);
        assertNotNull(hardwareInfo.getHardwareTier());
    }
    
    @Test
    void testLLMProviderRecommendations() {
        // Test each provider has appropriate recommendations
        HardwareInfo hardwareInfo = handler.getHardwareManager().getHardwareInfo();
        
        for (LLMProvider provider : LLMProvider.values()) {
            var models = handler.getLLMManager().getRecommendedModels(provider, hardwareInfo.getHardwareTier());
            
            // Debug: Print provider and models for troubleshooting
            System.out.println("Provider: " + provider + ", Models: " + models);
            
            // All providers should have at least some recommended models
            // Note: The hardware tier affects which models are available
            assertFalse(models.isEmpty(), 
                "Provider " + provider + " should have recommended models available for hardware tier: " + hardwareInfo.getHardwareTier());
        }
    }
    
    @Test
    void testWorldJoinHandling() {
        // This test ensures the method doesn't throw exceptions
        assertDoesNotThrow(() -> handler.onWorldJoin());
        assertDoesNotThrow(() -> handler.onWorldLeave());
    }
    
    @Test
    void testConfigReloading() {
        FirstTimeSetupConfig originalConfig = handler.getSetupConfig();
        
        // Complete setup
        originalConfig.completeSetup(LLMProvider.ANTHROPIC, "claude-3-haiku");
        assertTrue(handler.isSetupCompleted());
        
        // Reload config
        handler.reloadSetupConfig();
        
        // Should still be completed after reload
        assertTrue(handler.isSetupCompleted());
        assertEquals(LLMProvider.ANTHROPIC, handler.getSetupConfig().getSelectedProvider());
        assertEquals("claude-3-haiku", handler.getSetupConfig().getSelectedModel());
    }
    
    @Test
    void testHardwareTierClassification() {
        HardwareInfo hardwareInfo = handler.getHardwareManager().getHardwareInfo();
        HardwareTier tier = hardwareInfo.getHardwareTier();
        
        // Ensure tier is one of the expected values
        assertTrue(tier == HardwareTier.HIGH || 
                  tier == HardwareTier.MEDIUM || 
                  tier == HardwareTier.LOW || 
                  tier == HardwareTier.UNKNOWN);
    }
    
    @Test
    void testModelFilteringByTier() {
        var llmManager = handler.getLLMManager();
        
        // Test that different tiers get different model recommendations
        var highTierModels = llmManager.getRecommendedModels(LLMProvider.OPENAI, HardwareTier.HIGH);
        var lowTierModels = llmManager.getRecommendedModels(LLMProvider.OPENAI, HardwareTier.LOW);
        
        assertNotNull(highTierModels);
        assertNotNull(lowTierModels);
        assertFalse(highTierModels.isEmpty());
        assertFalse(lowTierModels.isEmpty());
        
        // High tier should have at least as many models as low tier
        assertTrue(highTierModels.size() >= lowTierModels.size());
    }
}