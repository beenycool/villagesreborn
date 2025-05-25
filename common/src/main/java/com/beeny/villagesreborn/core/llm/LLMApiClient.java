package com.beeny.villagesreborn.core.llm;

import java.util.concurrent.CompletableFuture;

/**
 * Interface for LLM API client operations
 */
public interface LLMApiClient {
    
    /**
     * Fetch available models from the provider
     * 
     * @param provider The LLM provider
     * @return CompletableFuture containing the API response as JSON string
     */
    CompletableFuture<String> fetchModels(LLMProvider provider);
    
    /**
     * Validate an API key by making a test request
     * 
     * @param provider The LLM provider
     * @param apiKey The API key to validate
     * @return CompletableFuture containing validation result
     */
    CompletableFuture<Boolean> validateKey(LLMProvider provider, String apiKey);
    
    /**
     * Generate conversation response from LLM
     * 
     * @param request The conversation request
     * @return CompletableFuture containing the conversation response
     */
    CompletableFuture<ConversationResponse> generateConversationResponse(ConversationRequest request);
    
    /**
     * Send async request to LLM API
     *
     * @param request The conversation request
     * @return CompletableFuture containing the conversation response
     */
    CompletableFuture<ConversationResponse> sendRequestAsync(ConversationRequest request);
    
    /**
     * Generate conversation response with retry logic
     *
     * @param request The conversation request
     * @param maxRetries Maximum number of retries
     * @return CompletableFuture containing the conversation response
     */
    CompletableFuture<ConversationResponse> generateWithRetry(ConversationRequest request, int maxRetries);
}