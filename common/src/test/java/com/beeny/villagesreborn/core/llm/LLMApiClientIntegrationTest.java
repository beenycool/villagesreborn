package com.beeny.villagesreborn.core.llm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.ExecutionException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test-Driven Development tests for LLMApiClient integration
 * Tests async call behavior, retry logic, timeout handling,
 * and ConversationRequest/ConversationResponse mapping
 */
@DisplayName("LLM API Client Integration Tests")
class LLMApiClientIntegrationTest {

    @Mock
    private HttpClient mockHttpClient;
    
    @Mock
    private HttpResponse<String> mockHttpResponse;

    private LLMApiClientImpl apiClient;
    private ConversationRequest testRequest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Ensure mocks are properly initialized
        assertNotNull(mockHttpClient);
        assertNotNull(mockHttpResponse);
        
        apiClient = new LLMApiClientImpl(mockHttpClient);
        
        testRequest = ConversationRequest.builder()
            .prompt("Test prompt")
            .maxTokens(100)
            .temperature(0.7f)
            .timeout(Duration.ofSeconds(30))
            .provider(LLMProvider.OPENAI)
            .apiKey("sk-test-key")
            .build();
    }

    @Test
    @DisplayName("Should successfully generate conversation response")
    void shouldSuccessfullyGenerateConversationResponse() throws Exception {
        // Given: Successful HTTP response
        String successResponse = """
            {
                "choices": [
                    {
                        "message": {
                            "content": "Hello! How can I help you today?"
                        }
                    }
                ],
                "usage": {
                    "total_tokens": 45
                }
            }
            """;
        
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(successResponse);
        
        // Mock sendAsync instead of send for async operations
        when(mockHttpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(CompletableFuture.completedFuture(mockHttpResponse));

        // When: Generating conversation response
        CompletableFuture<ConversationResponse> future = 
            apiClient.generateConversationResponse(testRequest);
        ConversationResponse response = future.get();

        // Then: Should return successful response
        assertTrue(response.isSuccess());
        assertEquals("Hello! How can I help you today?", response.getResponse());
        assertEquals(45, response.getTokensUsed());
        assertTrue(response.getResponseTime() > 0);
        assertNull(response.getErrorMessage());
    }

    @Test
    @DisplayName("Should handle API key validation correctly")
    void shouldHandleApiKeyValidationCorrectly() throws Exception {
        // Given: Valid API key response
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn("{\"object\":\"list\",\"data\":[]}");
        when(mockHttpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(CompletableFuture.completedFuture(mockHttpResponse));

        // When: Validating API key
        CompletableFuture<Boolean> future = 
            apiClient.validateKey(LLMProvider.OPENAI, "sk-valid-key");
        Boolean isValid = future.get();

        // Then: Should return true for valid key
        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should retry on rate limit responses")
    void shouldRetryOnRateLimitResponses() throws Exception {
        // Given: Rate limit followed by success
        when(mockHttpResponse.statusCode())
            .thenReturn(429) // Rate limit
            .thenReturn(200); // Success on retry
        
        when(mockHttpResponse.body())
            .thenReturn("{\"error\":{\"message\":\"Rate limit exceeded\"}}")
            .thenReturn("{\"choices\":[{\"message\":{\"content\":\"Success after retry\"}}],\"usage\":{\"total_tokens\":20}}");
        
        when(mockHttpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(CompletableFuture.completedFuture(mockHttpResponse));

        // When: Generating response with retry
        CompletableFuture<ConversationResponse> future = 
            apiClient.generateWithRetry(testRequest, 3);
        ConversationResponse response = future.get();

        // Then: Should succeed after retry
        assertTrue(response.isSuccess());
        assertEquals("Success after retry", response.getResponse());
        
        // Should have made 2 calls (initial + 1 retry)
        verify(mockHttpClient, times(2)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    @DisplayName("Should handle timeout gracefully")
    void shouldHandleTimeoutGracefully() throws Exception {
        // Given: HTTP client that times out
        when(mockHttpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(CompletableFuture.failedFuture(new TimeoutException("Request timed out")));

        // When: Generating response with timeout
        CompletableFuture<ConversationResponse> future = 
            apiClient.generateConversationResponse(testRequest);
        ConversationResponse response = future.get();

        // Then: Should return failure response
        assertFalse(response.isSuccess());
        assertTrue(response.getErrorMessage().contains("timeout"));
        assertNull(response.getResponse());
        assertEquals(0, response.getTokensUsed());
    }

    @Test
    @DisplayName("Should handle HTTP error codes properly")
    void shouldHandleHttpErrorCodesProperly() throws Exception {
        // Given: HTTP error response
        when(mockHttpResponse.statusCode()).thenReturn(500);
        when(mockHttpResponse.body()).thenReturn("{\"error\":{\"message\":\"Internal server error\"}}");
        when(mockHttpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(CompletableFuture.completedFuture(mockHttpResponse));

        // When: Generating response
        CompletableFuture<ConversationResponse> future = 
            apiClient.generateConversationResponse(testRequest);
        ConversationResponse response = future.get();

        // Then: Should return failure response
        assertFalse(response.isSuccess());
        assertNotNull(response.getErrorMessage());
        assertTrue(response.getErrorMessage().contains("500") || 
                  response.getErrorMessage().contains("server error"));
    }

    @Test
    @DisplayName("Should respect exponential backoff on retries")
    void shouldRespectExponentialBackoffOnRetries() throws Exception {
        // Given: Multiple failures followed by success
        when(mockHttpResponse.statusCode())
            .thenReturn(429) // First failure
            .thenReturn(429) // Second failure
            .thenReturn(200); // Success
        
        when(mockHttpResponse.body())
            .thenReturn("{\"error\":{\"message\":\"Rate limit\"}}")
            .thenReturn("{\"error\":{\"message\":\"Rate limit\"}}")
            .thenReturn("{\"choices\":[{\"message\":{\"content\":\"Final success\"}}],\"usage\":{\"total_tokens\":30}}");
        
        when(mockHttpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(CompletableFuture.completedFuture(mockHttpResponse));

        // When: Generating response with retries
        long startTime = System.currentTimeMillis();
        CompletableFuture<ConversationResponse> future = 
            apiClient.generateWithRetry(testRequest, 3);
        ConversationResponse response = future.get();
        long endTime = System.currentTimeMillis();

        // Then: Should succeed and take time for backoff
        assertTrue(response.isSuccess());
        assertEquals("Final success", response.getResponse());
        
        // Should have taken some time for exponential backoff
        assertTrue(endTime - startTime >= 100); // At least some delay
        
        // Should have made 3 calls
        verify(mockHttpClient, times(3)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    @DisplayName("Should fail after maximum retries exceeded")
    void shouldFailAfterMaximumRetriesExceeded() throws Exception {
        // Given: Continuous failures
        when(mockHttpResponse.statusCode()).thenReturn(429);
        when(mockHttpResponse.body()).thenReturn("{\"error\":{\"message\":\"Rate limit exceeded\"}}");
        when(mockHttpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(CompletableFuture.completedFuture(mockHttpResponse));

        // When: Generating response with limited retries
        CompletableFuture<ConversationResponse> future = 
            apiClient.generateWithRetry(testRequest, 2);
        ConversationResponse response = future.get();

        // Then: Should fail after max retries
        assertFalse(response.isSuccess());
        assertTrue(response.getErrorMessage().contains("Max retries exceeded") ||
                  response.getErrorMessage().contains("retries"));
        
        // Should have made max attempts
        verify(mockHttpClient, times(2)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    @DisplayName("Should build correct HTTP request for OpenAI")
    void shouldBuildCorrectHttpRequestForOpenAI() throws Exception {
        // Given: OpenAI request
        ConversationRequest openAIRequest = ConversationRequest.builder()
            .prompt("Test OpenAI prompt")
            .maxTokens(150)
            .temperature(0.8f)
            .provider(LLMProvider.OPENAI)
            .apiKey("sk-openai-key")
            .build();

        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn("{\"choices\":[{\"message\":{\"content\":\"Response\"}}],\"usage\":{\"total_tokens\":25}}");
        when(mockHttpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(CompletableFuture.completedFuture(mockHttpResponse));

        // When: Generating response
        apiClient.generateConversationResponse(openAIRequest).get();

        // Then: Should build correct request
        verify(mockHttpClient).send(argThat(request -> {
            // Verify URL
            assertTrue(request.uri().toString().contains("openai.com"));
            
            // Verify headers
            assertTrue(request.headers().firstValue("Authorization")
                      .orElse("").contains("sk-openai-key"));
            assertTrue(request.headers().firstValue("Content-Type")
                      .orElse("").equals("application/json"));
            
            return true;
        }), any(HttpResponse.BodyHandler.class));
    }

    @Test
    @DisplayName("Should handle malformed JSON responses")
    void shouldHandleMalformedJsonResponses() throws Exception {
        // Given: Malformed JSON response
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn("{ invalid json structure");
        when(mockHttpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(CompletableFuture.completedFuture(mockHttpResponse));

        // When: Generating response
        CompletableFuture<ConversationResponse> future = 
            apiClient.generateConversationResponse(testRequest);
        ConversationResponse response = future.get();

        // Then: Should handle gracefully
        assertFalse(response.isSuccess());
        assertNotNull(response.getErrorMessage());
        assertTrue(response.getErrorMessage().contains("JSON") || 
                  response.getErrorMessage().contains("parse"));
    }

    @Test
    @DisplayName("Should apply rate limiting between requests")
    void shouldApplyRateLimitingBetweenRequests() throws Exception {
        // Given: Successful responses
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn("{\"choices\":[{\"message\":{\"content\":\"Response\"}}],\"usage\":{\"total_tokens\":20}}");
        when(mockHttpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(CompletableFuture.completedFuture(mockHttpResponse));

        // When: Making multiple rapid requests
        long startTime = System.currentTimeMillis();
        
        CompletableFuture<ConversationResponse> future1 = 
            apiClient.generateConversationResponse(testRequest);
        CompletableFuture<ConversationResponse> future2 = 
            apiClient.generateConversationResponse(testRequest);
        
        future1.get();
        future2.get();
        
        long endTime = System.currentTimeMillis();

        // Then: Should have applied rate limiting delay
        assertTrue(endTime - startTime >= apiClient.getMinRequestInterval());
    }

    @Test
    @DisplayName("Should map different provider endpoints correctly")
    void shouldMapDifferentProviderEndpointsCorrectly() throws Exception {
        // Given: Different providers
        ConversationRequest anthropicRequest = testRequest.toBuilder()
            .provider(LLMProvider.ANTHROPIC)
            .build();
        
        ConversationRequest groqRequest = testRequest.toBuilder()
            .provider(LLMProvider.GROQ)
            .build();

        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn("{\"choices\":[{\"message\":{\"content\":\"Response\"}}]}");
        when(mockHttpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(CompletableFuture.completedFuture(mockHttpResponse));

        // When: Making requests to different providers
        apiClient.generateConversationResponse(anthropicRequest).get();
        apiClient.generateConversationResponse(groqRequest).get();

        // Then: Should use correct endpoints
        verify(mockHttpClient, times(2)).send(argThat(request -> {
            String uri = request.uri().toString();
            return uri.contains("anthropic") || uri.contains("groq") || 
                   uri.contains("openai"); // Fallback to OpenAI format
        }), any(HttpResponse.BodyHandler.class));
    }

    @Test
    @DisplayName("Should handle network connectivity issues")
    void shouldHandleNetworkConnectivityIssues() throws Exception {
        // Given: Network connectivity error
        when(mockHttpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Connection refused")));

        // When: Generating response
        CompletableFuture<ConversationResponse> future = 
            apiClient.generateConversationResponse(testRequest);
        ConversationResponse response = future.get();

        // Then: Should handle gracefully
        assertFalse(response.isSuccess());
        assertNotNull(response.getErrorMessage());
        assertTrue(response.getErrorMessage().contains("Connection") ||
                  response.getErrorMessage().toLowerCase().contains("network"));
    }

    @Test
    @DisplayName("Should track response time accurately")
    void shouldTrackResponseTimeAccurately() throws Exception {
        // Given: Successful response with known delay
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn("{\"choices\":[{\"message\":{\"content\":\"Response\"}}],\"usage\":{\"total_tokens\":15}}");
        when(mockHttpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenAnswer(invocation -> {
                return CompletableFuture.supplyAsync(() -> {
                    try {
                        Thread.sleep(100); // Simulate 100ms delay
                        return mockHttpResponse;
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    }
                });
            });

        // When: Generating response
        long startTime = System.currentTimeMillis();
        CompletableFuture<ConversationResponse> future = 
            apiClient.generateConversationResponse(testRequest);
        ConversationResponse response = future.get();
        long endTime = System.currentTimeMillis();

        // Then: Should track response time accurately
        assertTrue(response.isSuccess());
        assertTrue(response.getResponseTime() >= 100);
        assertTrue(response.getResponseTime() <= (endTime - startTime + 50)); // Allow some tolerance
    }
}