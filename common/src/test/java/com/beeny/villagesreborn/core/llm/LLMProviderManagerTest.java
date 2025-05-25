package com.beeny.villagesreborn.core.llm;

import com.beeny.villagesreborn.core.hardware.HardwareTier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test-Driven Development tests for LLMProviderManager
 * Tests provider management, API key validation, and model fetching
 */
@DisplayName("LLM Provider Manager Tests")
class LLMProviderManagerTest {

    @Mock
    private LLMApiClient apiClient;

    private LLMProviderManager providerManager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        providerManager = new LLMProviderManager(apiClient);
    }

    @Test
    @DisplayName("Should validate OpenAI API key format")
    void shouldValidateOpenAIApiKeyFormat() {
        // Given: Valid OpenAI API key format
        String validKey = "sk-1234567890abcdef1234567890abcdef1234567890abcdef";

        // When: Validating API key
        boolean isValid = providerManager.validateApiKey(LLMProvider.OPENAI, validKey);

        // Then: Should be valid
        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should reject invalid OpenAI API key format")
    void shouldRejectInvalidOpenAIApiKeyFormat() {
        // Given: Invalid OpenAI API key format
        String invalidKey = "invalid-key-format";

        // When: Validating API key
        boolean isValid = providerManager.validateApiKey(LLMProvider.OPENAI, invalidKey);

        // Then: Should be invalid
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should validate Anthropic API key format")
    void shouldValidateAnthropicApiKeyFormat() {
        // Given: Valid Anthropic API key format
        String validKey = "sk-ant-api03-1234567890abcdef1234567890abcdef1234567890abcdef-1234567890abcdef";

        // When: Validating API key
        boolean isValid = providerManager.validateApiKey(LLMProvider.ANTHROPIC, validKey);

        // Then: Should be valid
        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should validate OpenRouter API key format")
    void shouldValidateOpenRouterApiKeyFormat() {
        // Given: Valid OpenRouter API key format
        String validKey = "sk-or-v1-1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef";

        // When: Validating API key
        boolean isValid = providerManager.validateApiKey(LLMProvider.OPENROUTER, validKey);

        // Then: Should be valid
        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should validate Groq API key format")
    void shouldValidateGroqApiKeyFormat() {
        // Given: Valid Groq API key format
        String validKey = "gsk_1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef";

        // When: Validating API key
        boolean isValid = providerManager.validateApiKey(LLMProvider.GROQ, validKey);

        // Then: Should be valid
        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should get available models for provider")
    void shouldGetAvailableModelsForProvider() {
        // Given: OpenAI provider
        LLMProvider provider = LLMProvider.OPENAI;

        // When: Getting available models
        List<String> models = providerManager.getAvailableModels(provider);

        // Then: Should return OpenAI models
        assertNotNull(models);
        assertFalse(models.isEmpty());
        assertTrue(models.contains("gpt-4"));
        assertTrue(models.contains("gpt-3.5-turbo"));
    }

    @Test
    @DisplayName("Should fetch dynamic models from OpenRouter")
    void shouldFetchDynamicModelsFromOpenRouter() {
        // Given: OpenRouter API response
        String apiResponse = """
            {
                "data": [
                    {"id": "openai/gpt-4", "name": "GPT-4"},
                    {"id": "anthropic/claude-3-opus", "name": "Claude 3 Opus"},
                    {"id": "meta-llama/llama-2-70b-chat", "name": "Llama 2 70B Chat"}
                ]
            }
            """;
        when(apiClient.fetchModels(LLMProvider.OPENROUTER)).thenReturn(CompletableFuture.completedFuture(apiResponse));

        // When: Fetching dynamic models
        CompletableFuture<List<String>> future = providerManager.fetchDynamicModels(LLMProvider.OPENROUTER);
        List<String> models = future.join();

        // Then: Should return parsed models
        assertNotNull(models);
        assertEquals(3, models.size());
        assertTrue(models.contains("openai/gpt-4"));
        assertTrue(models.contains("anthropic/claude-3-opus"));
        assertTrue(models.contains("meta-llama/llama-2-70b-chat"));
    }

    @Test
    @DisplayName("Should handle API errors gracefully")
    void shouldHandleApiErrorsGracefully() {
        // Given: API call that fails
        when(apiClient.fetchModels(LLMProvider.OPENROUTER))
            .thenReturn(CompletableFuture.failedFuture(new RuntimeException("API Error")));

        // When: Fetching dynamic models
        CompletableFuture<List<String>> future = providerManager.fetchDynamicModels(LLMProvider.OPENROUTER);

        // Then: Should complete exceptionally
        assertThrows(RuntimeException.class, future::join);
    }

    @Test
    @DisplayName("Should validate API key with actual API call")
    void shouldValidateApiKeyWithActualApiCall() {
        // Given: Valid API key format and successful API response
        String apiKey = "sk-1234567890abcdef1234567890abcdef1234567890abcdef";
        when(apiClient.validateKey(LLMProvider.OPENAI, apiKey))
            .thenReturn(CompletableFuture.completedFuture(true));

        // When: Validating with API call
        CompletableFuture<Boolean> future = providerManager.validateApiKeyWithApi(LLMProvider.OPENAI, apiKey);
        boolean isValid = future.join();

        // Then: Should be valid
        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should cache model lists for performance")
    void shouldCacheModelListsForPerformance() {
        // Given: Provider with cacheable models
        LLMProvider provider = LLMProvider.ANTHROPIC;

        // When: Getting models twice
        List<String> models1 = providerManager.getAvailableModels(provider);
        List<String> models2 = providerManager.getAvailableModels(provider);

        // Then: Should return same cached instance
        assertSame(models1, models2);
    }

    @Test
    @DisplayName("Should clear model cache when requested")
    void shouldClearModelCacheWhenRequested() {
        // Given: Cached models
        LLMProvider provider = LLMProvider.GROQ;
        List<String> models1 = providerManager.getAvailableModels(provider);

        // When: Clearing cache and getting models again
        providerManager.clearModelCache();
        List<String> models2 = providerManager.getAvailableModels(provider);

        // Then: Should return different instance
        assertNotSame(models1, models2);
    }

    @Test
    @DisplayName("Should get recommended models based on hardware tier")
    void shouldGetRecommendedModelsBasedOnHardwareTier() {
        // Given: High-tier hardware
        HardwareTier highTier = HardwareTier.HIGH;

        // When: Getting recommended models
        List<String> recommended = providerManager.getRecommendedModels(LLMProvider.OPENAI, highTier);

        // Then: Should include high-end models
        assertNotNull(recommended);
        assertFalse(recommended.isEmpty());
        assertTrue(recommended.contains("gpt-4"));
    }

    @Test
    @DisplayName("Should filter models for low-tier hardware")
    void shouldFilterModelsForLowTierHardware() {
        // Given: Low-tier hardware
        HardwareTier lowTier = HardwareTier.LOW;

        // When: Getting recommended models
        List<String> recommended = providerManager.getRecommendedModels(LLMProvider.OPENAI, lowTier);

        // Then: Should only include lighter models
        assertNotNull(recommended);
        assertFalse(recommended.isEmpty());
        assertTrue(recommended.contains("gpt-3.5-turbo"));
        assertFalse(recommended.contains("gpt-4")); // Too heavy for low-tier
    }

    @Test
    @DisplayName("Should handle null or empty API keys")
    void shouldHandleNullOrEmptyApiKeys() {
        // Given: Null and empty API keys
        String nullKey = null;
        String emptyKey = "";

        // When: Validating API keys
        boolean nullValid = providerManager.validateApiKey(LLMProvider.OPENAI, nullKey);
        boolean emptyValid = providerManager.validateApiKey(LLMProvider.OPENAI, emptyKey);

        // Then: Should be invalid
        assertFalse(nullValid);
        assertFalse(emptyValid);
    }
}