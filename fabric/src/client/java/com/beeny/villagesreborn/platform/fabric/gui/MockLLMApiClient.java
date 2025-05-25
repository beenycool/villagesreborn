package com.beeny.villagesreborn.platform.fabric.gui;

import com.beeny.villagesreborn.core.llm.LLMApiClient;
import com.beeny.villagesreborn.core.llm.LLMProvider;
import com.beeny.villagesreborn.core.llm.ConversationRequest;
import com.beeny.villagesreborn.core.llm.ConversationResponse;

import java.util.concurrent.CompletableFuture;

/**
 * Mock implementation of LLMApiClient for testing and development
 */
public class MockLLMApiClient implements LLMApiClient {
    
    @Override
    public CompletableFuture<String> fetchModels(LLMProvider provider) {
        // Return empty result for now - this will use static models from LLMProviderManager
        return CompletableFuture.completedFuture("{}");
    }
    
    @Override
    public CompletableFuture<Boolean> validateKey(LLMProvider provider, String apiKey) {
        // Return true for development - real validation would come later
        return CompletableFuture.completedFuture(true);
    }
    
    @Override
    public CompletableFuture<ConversationResponse> generateConversationResponse(ConversationRequest request) {
        // Return a mock response for development
        ConversationResponse mockResponse = ConversationResponse.success(
            "Hello, traveler! How may I help you today?",
            50,
            100
        );
        return CompletableFuture.completedFuture(mockResponse);
    }
    
    @Override
    public CompletableFuture<ConversationResponse> sendRequestAsync(ConversationRequest request) {
        // For mock implementation, just delegate to the basic method
        return generateConversationResponse(request);
    }
    
    @Override
    public CompletableFuture<ConversationResponse> generateWithRetry(ConversationRequest request, int maxRetries) {
        // For mock implementation, just delegate to the basic method
        return generateConversationResponse(request);
    }
}