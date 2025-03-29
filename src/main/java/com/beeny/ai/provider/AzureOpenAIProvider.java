package com.beeny.ai.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.ai.openai.models.*;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.util.Context;
import com.beeny.ai.LLMErrorHandler;
import com.azure.core.exception.HttpResponseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AzureOpenAIProvider implements AIProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn");
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Map<String, String> cache = new ConcurrentHashMap<>();
    private String apiKey, endpoint, modelName;
    private boolean initialized = false;
    private OpenAIClient client;
    private final LLMErrorHandler errorHandler = LLMErrorHandler.getInstance();

    @Override
    public void initialize(Map<String, String> config) {
        this.apiKey = config.get("apiKey");
        this.endpoint = config.get("endpoint");
        this.modelName = config.get("modelName");
        
        if (apiKey == null || apiKey.isEmpty() || endpoint == null || endpoint.isEmpty()) {
            String errorMsg = "Azure OpenAI provider initialization failed: missing required configuration";
            LOGGER.error(errorMsg);
            errorHandler.reportErrorToClient(LLMErrorHandler.ErrorType.INVALID_API_KEY, 
                "Please provide both a valid Azure OpenAI API key and endpoint in the configuration.");
            throw new IllegalArgumentException(errorMsg);
        }
        
        try {
            this.client = new OpenAIClientBuilder()
                .endpoint(endpoint)
                .credential(new AzureKeyCredential(apiKey))
                .buildClient();
            
            initialized = true;
            LOGGER.info("Azure OpenAI provider initialized with model: {}", modelName);
        } catch (Exception e) {
            LOGGER.error("Failed to initialize Azure OpenAI client", e);
            
            LLMErrorHandler.ErrorType errorType = errorHandler.determineErrorType(e);
            errorHandler.reportErrorToClient(errorType, "Failed to initialize Azure OpenAI client: " + e.getMessage());
            
            throw new IllegalStateException("Failed to initialize Azure OpenAI client", e);
        }
    }

    public CompletableFuture<String> generateResponse(String prompt, Map<String, String> context) {
        if (!initialized) {
            String error = "Azure OpenAI provider not initialized";
            errorHandler.reportErrorToClient(LLMErrorHandler.ErrorType.PROVIDER_ERROR, error);
            return CompletableFuture.failedFuture(new IllegalStateException(error));
        }
        
        String cacheKey = generateCacheKey(prompt, context);
        if (cache.containsKey(cacheKey)) {
            return CompletableFuture.completedFuture(cache.get(cacheKey));
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                String response = callAzureOpenAI(prompt, context);
                cache.put(cacheKey, response);
                return response;
            } catch (Exception e) {
                LOGGER.error("Error generating response from Azure OpenAI", e);
                
                LLMErrorHandler.ErrorType errorType = errorHandler.determineErrorType(e);
                errorHandler.reportErrorToClient(errorType, e.getMessage());
                
                if (errorType == LLMErrorHandler.ErrorType.INVALID_API_KEY) {
                    throw new RuntimeException("Your Azure OpenAI API key appears to be invalid. Please check your API key in the mod settings.", e);
                } else if (errorType == LLMErrorHandler.ErrorType.CONNECTION_ERROR) {
                    throw new RuntimeException("Could not connect to Azure OpenAI. Please check your internet connection and endpoint URL.", e);
                } else if (errorType == LLMErrorHandler.ErrorType.API_RATE_LIMIT) {
                    throw new RuntimeException("Azure OpenAI API rate limit exceeded. Please try again later or switch to a different provider.", e);
                } else {
                    String mockResp = mockResponse(prompt);
                    cache.put(cacheKey, mockResp);
                    return mockResp;
                }
            }
        }, executor);
    }

    private String callAzureOpenAI(String prompt, Map<String, String> context) {
        try {
            List<ChatMessage> messages = new ArrayList<>();
            if (context != null && !context.isEmpty()) {
                StringBuilder systemContent = new StringBuilder("You are a helpful assistant for a Minecraft villager AI.");
                context.forEach((key, value) -> {
                    if (value != null && !value.isEmpty()) systemContent.append(" ").append(key).append(": ").append(value).append(".");
                });
                messages.add(new ChatMessage(ChatRole.SYSTEM, systemContent.toString()));
            }
            messages.add(new ChatMessage(ChatRole.USER, prompt));
            ChatCompletionsOptions options = new ChatCompletionsOptions(messages);
            options.setMaxTokens(300);
            options.setTemperature(0.7);
            options.setModel(modelName);
            ChatCompletions completions = client.getChatCompletions(modelName, options);
            if (completions != null && !completions.getChoices().isEmpty()) {
                ChatChoice choice = completions.getChoices().get(0);
                return choice.getMessage().getContent();
            } else {
                LOGGER.warn("Empty response received from Azure OpenAI");
                throw new RuntimeException("Empty response received from Azure OpenAI");
            }
        } catch (HttpResponseException e) {
            int statusCode = e.getResponse().getStatusCode();
            
            if (statusCode == 401 || statusCode == 403) {Code == 403) {
                throw new RuntimeException("Authentication error: Invalid Azure OpenAI API key or insufficient permissions", e);
            } else if (statusCode == 429) {
                throw new RuntimeException("Rate limit exceeded: Azure OpenAI API quota has been reached", e);
            } else if (statusCode >= 500) {
                throw new RuntimeException("Azure OpenAI service is currently unavailable: " + statusCode, e);
            } else {
                throw new RuntimeException("Azure OpenAI API error: " + statusCode + " - " + e.getMessage(), e);
            }
        } catch (Exception e) {
            LOGGER.error("Exception during Azure OpenAI API call", e);
            throw e;
        }
    }

    @Override
    public CompletableFuture<Boolean> validateAccess() {
        if (!initialized || apiKey == null || apiKey.isEmpty() || endpoint == null || endpoint.isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<ChatMessage> messages = new ArrayList<>();
                messages.add(new ChatMessage(ChatRole.USER, "Validation ping"));
                ChatCompletionsOptions options = new ChatCompletionsOptions(messages);
                options.setMaxTokens(1);
                options.setModel(modelName);
                
                client.getChatCompletions(modelName, options);
                return true;
            } catch (HttpResponseException e) {
                int statusCode = e.getResponse().getStatusCode();
                
                if (statusCode == 401 || statusCode == 403) {
                    errorHandler.reportErrorToClient(LLMErrorHandler.ErrorType.INVALID_API_KEY,
                        "Azure OpenAI rejected your API key. Please check it is correct.");
                } else if (statusCode == 429) {
                    errorHandler.reportErrorToClient(LLMErrorHandler.ErrorType.API_RATE_LIMIT,
                        "Azure OpenAI API rate limit exceeded. Please try again later.");
                } else {
                    errorHandler.reportErrorToClient(LLMErrorHandler.ErrorType.PROVIDER_ERROR,
                        "Azure OpenAI API error: " + statusCode + " - " + e.getMessage());
                }
                return false;
            } catch (Exception e) {
                LOGGER.error("Error validating Azure OpenAI access", e);
                errorHandler.reportErrorToClient(LLMErrorHandler.ErrorType.CONNECTION_ERROR,
                    "Error connecting to Azure OpenAI: " + e.getMessage());
                return false;
            }
        });
    }

    private String mockResponse(String prompt) {
        if (prompt.toLowerCase().contains("name")) return "John the Wise Trader";
        else if (prompt.toLowerCase().contains("personality")) return "Friendly, wise, and generous merchant";
        else return "I understand, let me help you with that.";
    }

    private String generateCacheKey(String prompt, Map<String, String> context) {
        StringBuilder key = new StringBuilder(prompt);
        context.forEach((k, v) -> key.append("|").append(k).append("=").append(v));
        return key.toString();
    }

    @Override
    public String getName() {
        return "Azure OpenAI";
    }

    @Override
    public boolean isAvailable() {
        return initialized;
    }

    @Override
    public void shutdown() {
        executor.shutdown();
        cache.clear();
        initialized = false;
    }
}
