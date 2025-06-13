package com.beeny.villagesreborn.platform.fabric.gui;

import com.beeny.villagesreborn.core.VillagesRebornCommon;
import com.beeny.villagesreborn.core.llm.LLMApiClient;
import com.beeny.villagesreborn.core.llm.LLMProvider;
import com.beeny.villagesreborn.core.llm.ConversationRequest;
import com.beeny.villagesreborn.core.llm.ConversationResponse;
import com.beeny.villagesreborn.core.config.ModConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Factory for creating appropriate LLM client instances based on configuration.
 * Prevents production code from defaulting to mock implementations.
 */
public class LLMClientFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(LLMClientFactory.class);
    
    /**
     * Creates an appropriate LLM client based on current configuration
     */
    public static LLMApiClient createLLMClient() {
        try {
            ModConfig config = VillagesRebornCommon.getConfig();
            
            // Check if we have a valid API key
            if (!config.hasValidApiKey()) {
                LOGGER.warn("No valid API key configured. LLM features will be disabled.");
                return new DisabledLLMApiClient();
            }
            
            // For now, default to test/mock mode if in development
            if (config.isDevelopmentMode()) {
                LOGGER.warn("Development mode - using mock LLM client");
                return new MockLLMApiClient();
            }
            
            // Default to disabled client until specific providers are implemented
            LOGGER.info("Using disabled LLM client - providers not yet implemented");
            return new DisabledLLMApiClient("LLM providers not yet implemented");
            
        } catch (Exception e) {
            LOGGER.error("Failed to create LLM client, using disabled client", e);
            return new DisabledLLMApiClient();
        }
    }
    
    /**
     * Creates an OpenAI client (placeholder - would need actual implementation)
     */
    private static LLMApiClient createOpenAIClient(ModConfig config) {
        LOGGER.info("Creating OpenAI LLM client");
        // TODO: Implement actual OpenAI client creation
        return new DisabledLLMApiClient("OpenAI client not yet implemented");
    }
    
    /**
     * Creates an Anthropic client (placeholder - would need actual implementation)
     */
    private static LLMApiClient createAnthropicClient(ModConfig config) {
        LOGGER.info("Creating Anthropic LLM client");
        // TODO: Implement actual Anthropic client creation
        return new DisabledLLMApiClient("Anthropic client not yet implemented");
    }
    
    /**
     * Creates an Ollama client (placeholder - would need actual implementation)
     */
    private static LLMApiClient createOllamaClient(ModConfig config) {
        LOGGER.info("Creating Ollama LLM client");
        // TODO: Implement actual Ollama client creation
        return new DisabledLLMApiClient("Ollama client not yet implemented");
    }
    
    /**
     * LLM client that gracefully handles disabled state
     */
    private static class DisabledLLMApiClient implements LLMApiClient {
        private final String reason;
        
        public DisabledLLMApiClient() {
            this.reason = "LLM features disabled";
        }
        
        public DisabledLLMApiClient(String reason) {
            this.reason = reason;
        }
        
        @Override
        public CompletableFuture<String> fetchModels(LLMProvider provider) {
            LOGGER.debug("LLM fetch models requested but disabled: {}", reason);
            return CompletableFuture.completedFuture("[]");
        }
        
        @Override
        public CompletableFuture<Boolean> validateKey(LLMProvider provider, String apiKey) {
            LOGGER.debug("LLM key validation requested but disabled: {}", reason);
            return CompletableFuture.completedFuture(false);
        }
        
        @Override
        public CompletableFuture<ConversationResponse> generateConversationResponse(ConversationRequest request) {
            LOGGER.debug("LLM conversation generation requested but disabled: {}", reason);
            ConversationResponse response = ConversationResponse.failure("[LLM Disabled: " + reason + "]");
            return CompletableFuture.completedFuture(response);
        }
        
        @Override
        public CompletableFuture<ConversationResponse> sendRequestAsync(ConversationRequest request) {
            return generateConversationResponse(request);
        }
        
        @Override
        public CompletableFuture<ConversationResponse> generateWithRetry(ConversationRequest request, int maxRetries) {
            return generateConversationResponse(request);
        }
    }
} 