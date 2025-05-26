package com.beeny.villagesreborn.core.llm;

import com.beeny.villagesreborn.core.hardware.HardwareTier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Enhanced unit tests for LLMProviderManager
 * Tests provider management, model recommendations, compatibility validation, and new methods
 */
@ExtendWith(MockitoExtension.class)
class LLMProviderManagerEnhancedTest {

    @Mock
    private LLMApiClient mockApiClient;
    
    private LLMProviderManager llmManager;
    
    @BeforeEach
    void setUp() {
        llmManager = new LLMProviderManager(mockApiClient);
    }
    
    @Test
    void testGetAllProviders() {
        // WHEN: All providers are requested
        List<LLMProvider> providers = llmManager.getAllProviders();
        
        // THEN: All enum values are returned
        assertThat(providers).containsExactlyInAnyOrder(LLMProvider.values());
    }
    
    @Test
    void testGetModelsWithValidProvider() {
        // WHEN: Models are requested for a valid provider
        List<String> models = llmManager.getModels("OPENAI");
        
        // THEN: OpenAI models are returned
        assertThat(models).contains("gpt-4", "gpt-3.5-turbo");
    }
    
    @Test
    void testGetModelsWithInvalidProvider() {
        // WHEN: Models are requested for an invalid provider
        List<String> models = llmManager.getModels("INVALID_PROVIDER");
        
        // THEN: Empty list is returned
        assertThat(models).isEmpty();
    }
    
    @Test
    void testRecommendedModelsForHighTierHardware() {
        // WHEN: Recommendations are requested for high-tier hardware
        LLMProviderManager.ModelRecommendation recommendations = 
            llmManager.getRecommendedModelsWithReasoning(LLMProvider.OPENAI, HardwareTier.HIGH);
        
        // THEN: All models are recommended with excellent performance
        assertThat(recommendations.getModels()).contains("gpt-4", "gpt-3.5-turbo");
        assertThat(recommendations.getPerformance()).isEqualTo("Excellent");
        assertThat(recommendations.getReason()).contains("can handle all available models");
        assertThat(recommendations.getWarnings()).isEmpty();
    }
    
    @Test
    void testRecommendedModelsForMediumTierHardware() {
        // WHEN: Recommendations are requested for medium-tier hardware
        LLMProviderManager.ModelRecommendation recommendations = 
            llmManager.getRecommendedModelsWithReasoning(LLMProvider.OPENAI, HardwareTier.MEDIUM);
        
        // THEN: Filtered models are recommended with good performance
        assertThat(recommendations.getModels()).contains("gpt-3.5-turbo");
        assertThat(recommendations.getModels()).doesNotContain("gpt-4"); // Filtered out for medium tier
        assertThat(recommendations.getPerformance()).isEqualTo("Good");
        assertThat(recommendations.getReason()).contains("Optimized for your hardware");
        assertThat(recommendations.getWarnings()).contains("Some heavy models may run slower");
    }
    
    @Test
    void testRecommendedModelsForLowTierHardware() {
        // WHEN: Recommendations are requested for low-tier hardware
        LLMProviderManager.ModelRecommendation recommendations = 
            llmManager.getRecommendedModelsWithReasoning(LLMProvider.OPENAI, HardwareTier.LOW);
        
        // THEN: Only lightweight models are recommended
        assertThat(recommendations.getModels()).contains("gpt-3.5-turbo");
        assertThat(recommendations.getModels()).doesNotContain("gpt-4");
        assertThat(recommendations.getPerformance()).isEqualTo("Basic");
        assertThat(recommendations.getReason()).contains("Lightweight models recommended");
        assertThat(recommendations.getWarnings()).contains("Limited to efficient models");
    }
    
    @Test
    void testRecommendedModelsForUnknownTierHardware() {
        // WHEN: Recommendations are requested for unknown-tier hardware
        LLMProviderManager.ModelRecommendation recommendations = 
            llmManager.getRecommendedModelsWithReasoning(LLMProvider.OPENAI, HardwareTier.UNKNOWN);
        
        // THEN: Conservative selection is made
        assertThat(recommendations.getModels()).contains("gpt-3.5-turbo");
        assertThat(recommendations.getModels()).doesNotContain("gpt-4");
        assertThat(recommendations.getPerformance()).isEqualTo("Basic");
        assertThat(recommendations.getReason()).contains("Conservative selection due to unknown hardware");
        assertThat(recommendations.getWarnings()).contains("Hardware detection failed");
    }
    
    @Test
    void testModelFilteringForMediumTier() {
        // WHEN: Models are filtered for medium-tier hardware
        List<String> filteredModels = llmManager.getRecommendedModels(LLMProvider.OPENAI, HardwareTier.MEDIUM);
        
        // THEN: High-end models are filtered out
        assertThat(filteredModels).contains("gpt-3.5-turbo");
        assertThat(filteredModels).doesNotContain("gpt-4");
    }
    
    @Test
    void testModelFilteringForLowTier() {
        // WHEN: Models are filtered for low-tier hardware
        List<String> filteredModels = llmManager.getRecommendedModels(LLMProvider.OPENAI, HardwareTier.LOW);
        
        // THEN: Only lightweight models are included
        assertThat(filteredModels).contains("gpt-3.5-turbo");
        assertThat(filteredModels).doesNotContain("gpt-4");
    }
    
    @Test
    void testProviderCompatibilityValidationLocal() {
        // WHEN: Compatibility is checked for LOCAL provider on LOW hardware
        LLMProviderManager.ValidationResult result = 
            llmManager.validateProviderCompatibility(LLMProvider.LOCAL, HardwareTier.LOW);
        
        // THEN: Warning is returned
        assertThat(result.getType()).isEqualTo(LLMProviderManager.ValidationResult.Type.WARNING);
        assertThat(result.getMessage()).contains("may be slow");
    }
    
    @Test
    void testProviderCompatibilityValidationLocalMedium() {
        // WHEN: Compatibility is checked for LOCAL provider on MEDIUM hardware
        LLMProviderManager.ValidationResult result = 
            llmManager.validateProviderCompatibility(LLMProvider.LOCAL, HardwareTier.MEDIUM);
        
        // THEN: Compatible result is returned
        assertThat(result.getType()).isEqualTo(LLMProviderManager.ValidationResult.Type.COMPATIBLE);
    }
    
    @Test
    void testProviderCompatibilityValidationOpenAI() {
        // WHEN: Compatibility is checked for OpenAI provider
        LLMProviderManager.ValidationResult result = 
            llmManager.validateProviderCompatibility(LLMProvider.OPENAI, HardwareTier.LOW);
        
        // THEN: Compatible result is returned (cloud providers work on any hardware)
        assertThat(result.getType()).isEqualTo(LLMProviderManager.ValidationResult.Type.COMPATIBLE);
    }
    
    @Test
    void testProviderCompatibilityValidationGroqHigh() {
        // WHEN: Compatibility is checked for GROQ provider on HIGH hardware
        LLMProviderManager.ValidationResult result = 
            llmManager.validateProviderCompatibility(LLMProvider.GROQ, HardwareTier.HIGH);
        
        // THEN: Optimal result is returned
        assertThat(result.getType()).isEqualTo(LLMProviderManager.ValidationResult.Type.OPTIMAL);
    }
    
    @Test
    void testProviderCompatibilityValidationGroqMedium() {
        // WHEN: Compatibility is checked for GROQ provider on MEDIUM hardware
        LLMProviderManager.ValidationResult result = 
            llmManager.validateProviderCompatibility(LLMProvider.GROQ, HardwareTier.MEDIUM);
        
        // THEN: Compatible result is returned
        assertThat(result.getType()).isEqualTo(LLMProviderManager.ValidationResult.Type.COMPATIBLE);
    }
    
    @Test
    void testLocalProviderModels() {
        // WHEN: Models are requested for LOCAL provider
        List<String> models = llmManager.getAvailableModels(LLMProvider.LOCAL);
        
        // THEN: Local models are returned
        assertThat(models).contains("llama2:7b", "mistral:7b");
    }
    
    @Test
    void testGroqProviderModels() {
        // WHEN: Models are requested for GROQ provider
        List<String> models = llmManager.getAvailableModels(LLMProvider.GROQ);
        
        // THEN: Groq models are returned
        assertThat(models).contains("llama2-70b-4096", "mixtral-8x7b-32768", "gemma-7b-it");
    }
    
    @Test
    void testAnthropicProviderModels() {
        // WHEN: Models are requested for ANTHROPIC provider
        List<String> models = llmManager.getAvailableModels(LLMProvider.ANTHROPIC);
        
        // THEN: Anthropic models are returned
        assertThat(models).contains(
            "claude-3-opus-20240229", 
            "claude-3-sonnet-20240229", 
            "claude-3-haiku-20240307"
        );
    }
    
    @Test
    void testModelCacheClearing() {
        // GIVEN: Models are cached
        llmManager.getAvailableModels(LLMProvider.OPENAI);
        
        // WHEN: Cache is cleared
        llmManager.clearModelCache();
        
        // THEN: Models can still be retrieved (re-initialized)
        List<String> models = llmManager.getAvailableModels(LLMProvider.OPENAI);
        assertThat(models).isNotEmpty();
    }
    
    @Test
    void testValidationResultTypes() {
        // Test all validation result factory methods
        var compatible = LLMProviderManager.ValidationResult.compatible();
        var optimal = LLMProviderManager.ValidationResult.optimal();
        var warning = LLMProviderManager.ValidationResult.warning("Test warning");
        var incompatible = LLMProviderManager.ValidationResult.incompatible("Test reason");
        
        assertThat(compatible.getType()).isEqualTo(LLMProviderManager.ValidationResult.Type.COMPATIBLE);
        assertThat(optimal.getType()).isEqualTo(LLMProviderManager.ValidationResult.Type.OPTIMAL);
        assertThat(warning.getType()).isEqualTo(LLMProviderManager.ValidationResult.Type.WARNING);
        assertThat(incompatible.getType()).isEqualTo(LLMProviderManager.ValidationResult.Type.INCOMPATIBLE);
        
        assertThat(warning.getMessage()).isEqualTo("Test warning");
        assertThat(incompatible.getMessage()).isEqualTo("Test reason");
    }
    
    @Test
    void testModelRecommendationData() {
        // GIVEN: Model recommendation with test data
        List<String> models = Arrays.asList("test-model");
        List<String> warnings = Arrays.asList("test-warning");
        var recommendation = new LLMProviderManager.ModelRecommendation(
            models, "test-reason", "test-performance", warnings
        );
        
        // THEN: All data is accessible
        assertThat(recommendation.getModels()).isEqualTo(models);
        assertThat(recommendation.getReason()).isEqualTo("test-reason");
        assertThat(recommendation.getPerformance()).isEqualTo("test-performance");
        assertThat(recommendation.getWarnings()).isEqualTo(warnings);
    }
}