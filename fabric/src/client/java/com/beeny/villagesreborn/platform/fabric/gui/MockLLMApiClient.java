package com.beeny.villagesreborn.platform.fabric.gui;

import com.beeny.villagesreborn.core.llm.LLMApiClient;
import com.beeny.villagesreborn.core.llm.LLMProvider;
import com.beeny.villagesreborn.core.llm.ConversationRequest;
import com.beeny.villagesreborn.core.llm.ConversationResponse;

import java.util.concurrent.CompletableFuture;
import java.util.Random;
import java.util.Arrays;
import java.util.List;

/**
 * Mock implementation of LLMApiClient for testing and development
 */
public class MockLLMApiClient implements LLMApiClient {
    private final Random random = new Random();
    
    // Sample responses for different conversation contexts
    private final List<String> greetingResponses = Arrays.asList(
        "Hello, traveler! How may I help you today?",
        "Greetings! What brings you to our village?",
        "Welcome, friend! Is there something you need?",
        "Good day to you! How can I assist?"
    );
    
    private final List<String> tradeResponses = Arrays.asList(
        "I have some fine goods to trade. What are you looking for?",
        "These emeralds won't spend themselves! What can I get you?",
        "Business is good today. What would you like to purchase?",
        "I've got the best deals in the village. Take a look!"
    );
    
    private final List<String> questResponses = Arrays.asList(
        "There's been trouble with monsters near the wheat fields. Could you help?",
        "I've lost my precious ring somewhere in the forest. Would you find it?",
        "The village needs more iron. Could you gather some from the mines?",
        "Strange noises have been coming from the old ruins. Investigate, will you?"
    );
    
    @Override
    public CompletableFuture<String> fetchModels(LLMProvider provider) {
        // Return mock model list based on provider
        String mockModels = switch (provider) {
            case OPENAI -> "{\"models\": [\"gpt-3.5-turbo\", \"gpt-4\", \"gpt-4-turbo\"]}";
            case ANTHROPIC -> "{\"models\": [\"claude-3-sonnet\", \"claude-3-opus\", \"claude-3-haiku\"]}";
            case LOCAL -> "{\"models\": [\"llama2\", \"mistral\", \"codellama\"]}";
            default -> "{\"models\": [\"default-model\"]}";
        };
        return CompletableFuture.completedFuture(mockModels);
    }
    
    @Override
    public CompletableFuture<Boolean> validateKey(LLMProvider provider, String apiKey) {
        // Simulate validation with some realistic behavior
        if (apiKey == null || apiKey.trim().isEmpty() || apiKey.equals("YOUR_API_KEY_HERE")) {
            return CompletableFuture.completedFuture(false);
        }
        
        // Simulate network delay
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(500 + random.nextInt(1000)); // 0.5-1.5 second delay
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // Mock validation - accept keys that look realistic
            return apiKey.length() > 10 && (apiKey.startsWith("sk-") || apiKey.startsWith("claude-") || apiKey.contains("api"));
        });
    }
    
    @Override
    public CompletableFuture<ConversationResponse> generateConversationResponse(ConversationRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            // Simulate processing time
            try {
                Thread.sleep(200 + random.nextInt(800)); // 0.2-1.0 second delay
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // Generate contextual response based on request content
            String response = generateContextualResponse(request.getPrompt());
            int inputTokens = estimateTokens(request.getPrompt());
            int outputTokens = estimateTokens(response);
            
            return ConversationResponse.success(response, inputTokens, outputTokens);
        });
    }
    
    private String generateContextualResponse(String message) {
        if (message == null || message.trim().isEmpty()) {
            return getRandomResponse(greetingResponses);
        }
        
        String lowerMessage = message.toLowerCase();
        
        // Context-aware response generation
        if (lowerMessage.contains("trade") || lowerMessage.contains("buy") || lowerMessage.contains("sell")) {
            return getRandomResponse(tradeResponses);
        } else if (lowerMessage.contains("quest") || lowerMessage.contains("help") || lowerMessage.contains("task")) {
            return getRandomResponse(questResponses);
        } else if (lowerMessage.contains("hello") || lowerMessage.contains("hi") || lowerMessage.contains("greet")) {
            return getRandomResponse(greetingResponses);
        } else {
            // Default conversational responses
            List<String> defaultResponses = Arrays.asList(
                "That's interesting! Tell me more about that.",
                "I see. The village has been quite busy lately.",
                "Indeed, these are changing times in our village.",
                "Hmm, I hadn't thought of it that way before.",
                "The elders might have wisdom about such matters."
            );
            return getRandomResponse(defaultResponses);
        }
    }
    
    private String getRandomResponse(List<String> responses) {
        return responses.get(random.nextInt(responses.size()));
    }
    
    private int estimateTokens(String text) {
        // Rough token estimation (approximately 4 characters per token)
        return Math.max(1, text.length() / 4);
    }
    
    @Override
    public CompletableFuture<ConversationResponse> sendRequestAsync(ConversationRequest request) {
        // For mock implementation, just delegate to the basic method
        return generateConversationResponse(request);
    }
    
    @Override
    public CompletableFuture<ConversationResponse> generateWithRetry(ConversationRequest request, int maxRetries) {
        // Simulate occasional failures for testing retry logic
        if (random.nextInt(10) < 2 && maxRetries > 0) { // 20% chance of failure
            // Simulate a failure that would trigger retry
            return CompletableFuture.completedFuture(ConversationResponse.failure("Mock API temporarily unavailable"));
        }
        
        // Otherwise, delegate to normal response generation
        return generateConversationResponse(request);
    }
}