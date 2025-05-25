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

/**
 * Implementation of LLMApiClient for handling async requests to LLM providers
 * with retry logic and timeout handling
 */
public class LLMApiClientImpl implements LLMApiClient {
    
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
        return CompletableFuture.completedFuture("[]");
    }

    @Override
    public CompletableFuture<Boolean> validateKey(LLMProvider provider, String apiKey) {
        return CompletableFuture.completedFuture(true);
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
}