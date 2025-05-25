package com.beeny.villagesreborn.platform.fabric.welcome;

import com.beeny.villagesreborn.core.hardware.HardwareTier;
import com.beeny.villagesreborn.core.llm.LLMProvider;
import com.beeny.villagesreborn.core.llm.LLMProviderManager;
import com.beeny.villagesreborn.platform.fabric.gui.MockLLMApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for LLM Provider recommendations
 */
class WelcomeScreenHandlerTest {
    
    private LLMProviderManager manager;
    
    @BeforeEach
    void setUp() {
        manager = new LLMProviderManager(new MockLLMApiClient());
    }
    
    @Test
    void testLLMProviderRecommendations() {
        // Test each provider has appropriate recommendations
        for (LLMProvider provider : LLMProvider.values()) {
            List<String> models = manager.getRecommendedModels(provider, HardwareTier.MEDIUM);
            
            // Debug: Print provider and models for troubleshooting
            System.out.println("Provider: " + provider + ", Models: " + models);
            
            // All providers should have at least some recommended models
            // Note: MEDIUM tier should include most models except the highest-end ones
            assertFalse(models.isEmpty(), 
                "Provider " + provider + " should have recommended models available for MEDIUM tier hardware");
        }
    }
}