package com.beeny.villagesreborn.core.llm;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of LLMApiClient for handling async requests to LLM providers
 * with retry logic and timeout handling
 */
public class LLMApiClientImpl implements LLMApiClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(LLMApiClientImpl.class);
    
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService scheduler;
    private final long minRequestInterval = 100; // 100ms between requests
    private final int maxRetries;
    private final long baseDelayMs;
    private volatile long lastRequestTime = 0;

    public LLMApiClientImpl(HttpClient httpClient) {
        this(httpClient, 3, 100);
    }
    
    public LLMApiClientImpl(HttpClient httpClient, int maxRetries, long baseDelayMs) {
        this.httpClient = httpClient;
        this.objectMapper = new ObjectMapper();
        this.scheduler = Executors.newScheduledThreadPool(2);
        this.maxRetries = maxRetries;
        this.baseDelayMs = baseDelayMs;
    }

    @Override
    public CompletableFuture<String> fetchModels(LLMProvider provider) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String endpoint = getModelsEndpoint(provider);
                
                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .GET();
                
                // Add authorization header for non-local providers
                if (provider != LLMProvider.LOCAL) {
                    // For model fetching, we need to handle the case where API key might not be set yet
                    // This is called during setup, so we return basic model list if no key available
                    requestBuilder.header("Authorization", "Bearer " + "dummy-key-for-model-fetch");
                }
                
                // Provider-specific headers
                if (provider == LLMProvider.ANTHROPIC) {
                    requestBuilder.header("anthropic-version", "2023-06-01");
                }
                
                HttpRequest request = requestBuilder.build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() == 200) {
                    return parseModelsResponse(response.body(), provider);
                } else {
                    LOGGER.warn("Failed to fetch models for {}: HTTP {}", provider, response.statusCode());
                    return getDefaultModels(provider);
                }
                
            } catch (Exception e) {
                LOGGER.error("Error fetching models for provider {}", provider, e);
                return getDefaultModels(provider);
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> validateKey(LLMProvider provider, String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // For local providers, always return true
                if (provider == LLMProvider.LOCAL) {
                    return true;
                }
                
                String endpoint = getValidationEndpoint(provider);
                
                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofSeconds(10))
                    .GET();
                
                // Provider-specific headers
                if (provider == LLMProvider.ANTHROPIC) {
                    requestBuilder.header("anthropic-version", "2023-06-01");
                }
                
                HttpRequest request = requestBuilder.build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                // API key is valid if we get a 200 response or if it's a recognizable response format
                return response.statusCode() == 200; // 401 means key format is recognized but invalid
                
            } catch (Exception e) {
                LOGGER.error("Error validating API key for provider {}", provider, e);
                return false;
            }
        });
    }

    @Override
    public CompletableFuture<ConversationResponse> generateConversationResponse(ConversationRequest request) {
        return sendRequestAsync(request);
    }

    @Override
    public CompletableFuture<ConversationResponse> sendRequestAsync(ConversationRequest request) {
        long startTime = System.currentTimeMillis();
        
        return applyRateLimit()
            .thenCompose(unused -> {
                HttpRequest httpRequest = buildHttpRequest(request);
                return httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
                    .orTimeout(request.getTimeout().toMillis(), TimeUnit.MILLISECONDS)
                    .handle((response, throwable) -> {
                        long responseTime = System.currentTimeMillis() - startTime;
                        
                        if (throwable != null) {
                            return handleException(throwable, responseTime);
                        }
                        
                        return parseResponse(response, responseTime);
                    });
            });
    }

    @Override
    public CompletableFuture<ConversationResponse> generateWithRetry(ConversationRequest request, int maxRetries) {
        return attemptWithRetry(request, maxRetries, 0);
    }

    private CompletableFuture<ConversationResponse> attemptWithRetry(ConversationRequest request, int maxRetries, int currentAttempt) {
        return sendRequestAsync(request)
            .thenCompose(response -> {
                if (response.isSuccess()) {
                    return CompletableFuture.completedFuture(response);
                }
                
                if (currentAttempt >= maxRetries - 1) {
                    // Max retries exceeded - return failure with explicit message
                    String failureMessage = "Max retries exceeded after " + maxRetries + " attempts. Last error: " +
                                          (response.getErrorMessage() != null ? response.getErrorMessage() : "Unknown error");
                    return CompletableFuture.completedFuture(ConversationResponse.failure(failureMessage));
                }
                
                if (!isRetryableError(response)) {
                    // Non-retryable error - return immediately
                    return CompletableFuture.completedFuture(response);
                }
                
                // Exponential backoff: baseDelayMs * 2^attempt
                long delayMs = baseDelayMs * (1L << currentAttempt);
                
                CompletableFuture<ConversationResponse> retryFuture = new CompletableFuture<>();
                scheduler.schedule(() -> {
                    attemptWithRetry(request, maxRetries, currentAttempt + 1)
                        .whenComplete((retryResponse, throwable) -> {
                            if (throwable != null) {
                                retryFuture.completeExceptionally(throwable);
                            } else {
                                retryFuture.complete(retryResponse);
                            }
                        });
                }, delayMs, TimeUnit.MILLISECONDS);
                
                return retryFuture;
            });
    }

    private boolean isRetryableError(ConversationResponse response) {
        if (response.isSuccess()) {
            return false;
        }
        
        String errorMessage = response.getErrorMessage();
        if (errorMessage == null) {
            return false;
        }
        
        // Don't retry authentication errors
        if (errorMessage.contains("authentication") || errorMessage.contains("Invalid API key")) {
            return false;
        }
        
        // Retry rate limits and server errors
        return errorMessage.contains("Rate limit") || errorMessage.contains("500") ||
               errorMessage.contains("server error") || errorMessage.contains("timeout") ||
               errorMessage.contains("Internal server error");
    }

    private CompletableFuture<Void> applyRateLimit() {
        long currentTime = System.currentTimeMillis();
        long timeSinceLastRequest = currentTime - lastRequestTime;
        
        if (timeSinceLastRequest >= minRequestInterval) {
            lastRequestTime = currentTime;
            return CompletableFuture.completedFuture(null);
        }
        
        long delay = minRequestInterval - timeSinceLastRequest;
        CompletableFuture<Void> delayedFuture = new CompletableFuture<>();
        
        scheduler.schedule(() -> {
            lastRequestTime = System.currentTimeMillis();
            delayedFuture.complete(null);
        }, delay, TimeUnit.MILLISECONDS);
        
        return delayedFuture;
    }

    private HttpRequest buildHttpRequest(ConversationRequest request) {
        String endpoint = getEndpoint(request.getProvider());
        String requestBody = buildRequestBody(request);
        
        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(URI.create(endpoint))
            .header("Content-Type", "application/json")
            .timeout(request.getTimeout())
            .POST(HttpRequest.BodyPublishers.ofString(requestBody));
        
        // Add authorization header
        if (request.getProvider() != LLMProvider.LOCAL) {
            builder.header("Authorization", "Bearer " + request.getApiKey());
        }
        
        // Provider-specific headers
        if (request.getProvider() == LLMProvider.ANTHROPIC) {
            builder.header("anthropic-version", "2023-06-01");
        }
        
        return builder.build();
    }

    private String getEndpoint(LLMProvider provider) {
        return switch (provider) {
            case OPENAI -> "https://api.openai.com/v1/chat/completions";
            case ANTHROPIC -> "https://api.anthropic.com/v1/messages";
            case GROQ -> "https://api.groq.com/openai/v1/chat/completions";
            case OPENROUTER -> "https://openrouter.ai/api/v1/chat/completions";
            case LOCAL -> "http://localhost:11434/v1/chat/completions";
        };
    }

    private String buildRequestBody(ConversationRequest request) {
        try {
            if (request.getProvider() == LLMProvider.ANTHROPIC) {
                return objectMapper.writeValueAsString(java.util.Map.of(
                    "model", "claude-3-haiku-20240307",
                    "max_tokens", request.getMaxTokens(),
                    "messages", java.util.List.of(
                        java.util.Map.of("role", "user", "content", request.getPrompt())
                    )
                ));
            } else {
                // OpenAI-compatible format
                return objectMapper.writeValueAsString(java.util.Map.of(
                    "model", "gpt-3.5-turbo",
                    "max_tokens", request.getMaxTokens(),
                    "temperature", request.getTemperature(),
                    "messages", java.util.List.of(
                        java.util.Map.of("role", "user", "content", request.getPrompt())
                    )
                ));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to build request body", e);
        }
    }

    private ConversationResponse parseResponse(HttpResponse<String> response, long responseTime) {
        try {
            if (response.statusCode() == 200) {
                return parseSuccessResponse(response.body(), responseTime);
            } else {
                return parseErrorResponse(response, responseTime);
            }
        } catch (Exception e) {
            return ConversationResponse.failure("JSON parsing error: " + e.getMessage());
        }
    }

    private ConversationResponse parseSuccessResponse(String body, long responseTime) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        
        // Handle Anthropic format
        if (root.has("content")) {
            JsonNode content = root.get("content");
            if (content.isArray() && content.size() > 0) {
                String text = content.get(0).get("text").asText();
                int tokens = root.has("usage") ? root.get("usage").get("output_tokens").asInt() : 0;
                return ConversationResponse.success(text, tokens, responseTime);
            }
        }
        
        // Handle OpenAI format
        if (root.has("choices")) {
            JsonNode choices = root.get("choices");
            if (choices.isArray() && choices.size() > 0) {
                JsonNode choice = choices.get(0);
                if (choice.has("message") && choice.get("message").has("content")) {
                    String content = choice.get("message").get("content").asText();
                    int tokens = root.has("usage") ? root.get("usage").get("total_tokens").asInt() : 0;
                    return ConversationResponse.success(content, tokens, responseTime);
                }
            }
        }
        
        throw new RuntimeException("Unexpected response format - missing required fields in JSON: " + body);
    }

    private ConversationResponse parseErrorResponse(HttpResponse<String> response, long responseTime) throws Exception {
        String errorMessage = "HTTP " + response.statusCode();
        
        // Check for specific HTTP status codes
        if (response.statusCode() == 429) {
            errorMessage = "Rate limit exceeded";
        } else if (response.statusCode() == 500) {
            errorMessage = "Internal server error";
        } else if (response.statusCode() == 401) {
            errorMessage = "Invalid API key";
        }
        
        try {
            JsonNode root = objectMapper.readTree(response.body());
            if (root.has("error")) {
                JsonNode error = root.get("error");
                if (error.has("message")) {
                    errorMessage = error.get("message").asText();
                }
            }
        } catch (Exception e) {
            // Use default error message if JSON parsing fails
        }
        
        return ConversationResponse.failure(errorMessage);
    }

    private ConversationResponse handleException(Throwable throwable, long responseTime) {
        if (throwable instanceof TimeoutException) {
            return ConversationResponse.failure("Request timeout after " + responseTime + "ms");
        } else if (throwable.getCause() instanceof TimeoutException) {
            return ConversationResponse.failure("Request timeout: " + throwable.getMessage());
        } else if (throwable.getMessage() != null && throwable.getMessage().toLowerCase().contains("timeout")) {
            return ConversationResponse.failure("Request timeout: " + throwable.getMessage());
        } else if (throwable.getMessage() != null && throwable.getMessage().toLowerCase().contains("connection")) {
            return ConversationResponse.failure("Network connection error: " + throwable.getMessage());
        } else {
            return ConversationResponse.failure("Request failed: " + throwable.getMessage());
        }
    }

    public long getMinRequestInterval() {
        return minRequestInterval;
    }

    /**
     * Get models endpoint for different providers
     */
    private String getModelsEndpoint(LLMProvider provider) {
        return switch (provider) {
            case OPENAI -> "https://api.openai.com/v1/models";
            case ANTHROPIC -> "https://api.anthropic.com/v1/models";
            case GROQ -> "https://api.groq.com/openai/v1/models";
            case OPENROUTER -> "https://openrouter.ai/api/v1/models";
            case LOCAL -> "http://localhost:11434/api/tags";
        };
    }
    
    /**
     * Get validation endpoint for different providers
     */
    private String getValidationEndpoint(LLMProvider provider) {
        return switch (provider) {
            case OPENAI -> "https://api.openai.com/v1/models";
            case ANTHROPIC -> "https://api.anthropic.com/v1/models";
            case GROQ -> "https://api.groq.com/openai/v1/models";
            case OPENROUTER -> "https://openrouter.ai/api/v1/models";
            case LOCAL -> "http://localhost:11434/api/tags";
        };
    }
    
    /**
     * Parse models response from different providers
     */
    private String parseModelsResponse(String responseBody, LLMProvider provider) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            
            if (provider == LLMProvider.LOCAL) {
                // Ollama format: {"models": [{"name": "modelname", ...}]}
                if (root.has("models")) {
                    JsonNode models = root.get("models");
                    StringBuilder modelList = new StringBuilder("[");
                    for (int i = 0; i < models.size(); i++) {
                        if (i > 0) modelList.append(",");
                        JsonNode model = models.get(i);
                        String modelName = model.has("name") ? model.get("name").asText() : "unknown";
                        modelList.append("\"").append(modelName).append("\"");
                    }
                    modelList.append("]");
                    return modelList.toString();
                }
            } else {
                // OpenAI/Anthropic format: {"data": [{"id": "model-id", ...}]}
                if (root.has("data")) {
                    JsonNode data = root.get("data");
                    StringBuilder modelList = new StringBuilder("[");
                    for (int i = 0; i < data.size(); i++) {
                        if (i > 0) modelList.append(",");
                        JsonNode model = data.get(i);
                        String modelId = model.has("id") ? model.get("id").asText() : "unknown";
                        modelList.append("\"").append(modelId).append("\"");
                    }
                    modelList.append("]");
                    return modelList.toString();
                }
            }
            
            return getDefaultModels(provider);
        } catch (Exception e) {
            LOGGER.error("Failed to parse models response", e);
            return getDefaultModels(provider);
        }
    }
    
    /**
     * Get default models for providers when API call fails
     */
    private String getDefaultModels(LLMProvider provider) {
        return switch (provider) {
            case OPENAI -> "[\"gpt-3.5-turbo\", \"gpt-4\", \"gpt-4-turbo\"]";
            case ANTHROPIC -> "[\"claude-3-haiku-20240307\", \"claude-3-sonnet-20240229\", \"claude-3-opus-20240229\"]";
            case GROQ -> "[\"llama2-70b-4096\", \"mixtral-8x7b-32768\", \"gemma-7b-it\"]";
            case OPENROUTER -> "[\"openrouter/auto\", \"anthropic/claude-3-haiku\", \"openai/gpt-3.5-turbo\"]";
            case LOCAL -> "[\"llama2\", \"codellama\", \"mistral\"]";
        };
    }

    /**
     * Shutdown the executor service to prevent resource leaks.
     * Should be called when the client is no longer needed.
     */
    public void shutdown() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}