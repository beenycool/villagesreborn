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
 * Tests async request/response mapping, retry logic, timeout handling,
 * and ConversationRequest/ConversationResponse mapping
 */
@DisplayName("LLM API Client Integration Tests")
class LLMApiClientIntegrationTest {

    @Mock
    private HttpClient mockHttpClient;
    
    @Mock
    private HttpResponse<String> mockHttpResponse;

    private LLMApiClient apiClient;
    private ConversationRequest testRequest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // This will fail initially as LLMApiClientImpl doesn't exist yet
        apiClient = new LLMApiClientImpl(mockHttpClient);
        
        testRequest = ConversationRequest.builder()
            .prompt("Test prompt for villager conversation")
            .maxTokens(150)
            .temperature(0.7f)
            .timeout(Duration.ofSeconds(30))
            .provider(LLMProvider.OPENAI)
            .apiKey("sk-test-key")
            .build();
    }

    @Test
    @DisplayName("Should successfully map ConversationRequest to ConversationResponse async")
    void shouldSuccessfullyMapRequestToResponseAsync() throws Exception {
        // Given: Successful HTTP response mimicking OpenAI format
        String successResponse = """
            {
                "choices": [
                    {
                        "message": {
                            "content": "Hello! I'm a villager who loves to trade. How can I help you today?"
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
        when(mockHttpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(CompletableFuture.completedFuture(mockHttpResponse));

        // When: Sending async request
        CompletableFuture<ConversationResponse> future = 
            apiClient.sendRequestAsync(testRequest);
        ConversationResponse response = future.get();

        // Then: Should successfully map to ConversationResponse
        assertTrue(response.isSuccess());
        assertEquals("Hello! I'm a villager who loves to trade. How can I help you today?", response.getResponse());
        assertEquals(45, response.getTokensUsed());
        assertTrue(response.getResponseTime() > 0);
        assertNull(response.getErrorMessage());
    }

    @Test
    @DisplayName("Should retry on transient failures with exponential backoff")
    void shouldRetryOnTransientFailuresWithExponentialBackoff() throws Exception {
        // Given: Rate limit (429) followed by server error (500) then success
        when(mockHttpResponse.statusCode())
            .thenReturn(429) // Rate limit - transient failure
            .thenReturn(500) // Server error - transient failure
            .thenReturn(200); // Success on third attempt
        
        when(mockHttpResponse.body())
            .thenReturn("{\"error\":{\"message\":\"Rate limit exceeded\",\"type\":\"rate_limit_exceeded\"}}")
            .thenReturn("{\"error\":{\"message\":\"Internal server error\",\"type\":\"server_error\"}}")
            .thenReturn("{\"choices\":[{\"message\":{\"content\":\"Success after retries\"}}],\"usage\":{\"total_tokens\":25}}");
        
        when(mockHttpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(CompletableFuture.completedFuture(mockHttpResponse));

        // When: Generating response with retry logic
        long startTime = System.currentTimeMillis();
        CompletableFuture<ConversationResponse> future = 
            apiClient.generateWithRetry(testRequest, 3);
        ConversationResponse response = future.get();
        long endTime = System.currentTimeMillis();

        // Then: Should succeed after retries with exponential backoff
        assertTrue(response.isSuccess());
        assertEquals("Success after retries", response.getResponse());
        assertEquals(25, response.getTokensUsed());
        
        // Should demonstrate exponential backoff timing (at least some delay)
        assertTrue(endTime - startTime >= 100, "Should have exponential backoff delay");
        
        // Should have made 3 attempts (initial + 2 retries)
        verify(mockHttpClient, times(3)).sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    @DisplayName("Should handle timeout properly and return appropriate response")
    void shouldHandleTimeoutProperlyAndReturnResponse() throws Exception {
        // Given: Request that exceeds timeout limit
        ConversationRequest timeoutRequest = testRequest.toBuilder()
            .timeout(Duration.ofMillis(100)) // Very short timeout
            .build();
            
        // Simulate timeout by returning a future that times out
        CompletableFuture<HttpResponse<String>> timeoutFuture = new CompletableFuture<>();
        timeoutFuture.completeExceptionally(new TimeoutException("Request timed out after 100ms"));
        
        when(mockHttpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(timeoutFuture);

        // When: Sending request that times out
        CompletableFuture<ConversationResponse> future = 
            apiClient.sendRequestAsync(timeoutRequest);
        ConversationResponse response = future.get();

        // Then: Should return failure response with timeout error
        assertFalse(response.isSuccess());
        assertNotNull(response.getErrorMessage());
        assertTrue(response.getErrorMessage().toLowerCase().contains("timeout"));
        assertNull(response.getResponse());
        assertEquals(0, response.getTokensUsed());
        assertEquals(0, response.getResponseTime());
    }

    @Test
    @DisplayName("Should properly map different request parameters to HTTP request")
    void shouldProperlyMapRequestParametersToHttpRequest() throws Exception {
        // Given: Request with specific parameters
        ConversationRequest parameterizedRequest = ConversationRequest.builder()
            .prompt("You are a helpful villager NPC. Respond to the player's greeting.")
            .maxTokens(200)
            .temperature(0.9f)
            .timeout(Duration.ofSeconds(45))
            .provider(LLMProvider.ANTHROPIC)
            .apiKey("sk-ant-test-key")
            .build();

        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn("{\"content\":[{\"text\":\"Greetings, traveler!\"}],\"usage\":{\"output_tokens\":15}}");
        when(mockHttpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(CompletableFuture.completedFuture(mockHttpResponse));

        // When: Sending request
        apiClient.sendRequestAsync(parameterizedRequest).get();

        // Then: Should build HTTP request with correct parameters
        verify(mockHttpClient).sendAsync(argThat(request -> {
            // Verify URL contains Anthropic endpoint
            String uri = request.uri().toString();
            assertTrue(uri.contains("anthropic.com"), "Should use Anthropic endpoint");
            
            // Verify headers
            assertTrue(request.headers().firstValue("Authorization")
                      .orElse("").contains("sk-ant-test-key"), "Should include API key");
            assertEquals("application/json", 
                        request.headers().firstValue("Content-Type").orElse(""),
                        "Should have JSON content type");
            
            return true;
        }), any(HttpResponse.BodyHandler.class));
    }

    @Test
    @DisplayName("Should fail after maximum retries and return failure response")
    void shouldFailAfterMaximumRetriesAndReturnFailure() throws Exception {
        // Given: Continuous rate limiting (non-recoverable within retry limit)
        when(mockHttpResponse.statusCode()).thenReturn(429);
        when(mockHttpResponse.body()).thenReturn("{\"error\":{\"message\":\"Rate limit exceeded\",\"type\":\"rate_limit_exceeded\"}}");
        when(mockHttpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(CompletableFuture.completedFuture(mockHttpResponse));

        // When: Attempting with limited retries
        CompletableFuture<ConversationResponse> future = 
            apiClient.generateWithRetry(testRequest, 2);
        ConversationResponse response = future.get();

        // Then: Should fail after exhausting retries
        assertFalse(response.isSuccess());
        assertNotNull(response.getErrorMessage());
        assertTrue(response.getErrorMessage().contains("retries") || 
                  response.getErrorMessage().contains("attempts"),
                  "Error message should indicate retry exhaustion");
        assertNull(response.getResponse());
        assertEquals(0, response.getTokensUsed());
        
        // Should have made exactly 2 attempts
        verify(mockHttpClient, times(2)).sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    @DisplayName("Should handle malformed JSON response gracefully")
    void shouldHandleMalformedJsonResponseGracefully() throws Exception {
        // Given: Response with malformed JSON
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn("{ malformed json structure without proper closing");
        when(mockHttpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(CompletableFuture.completedFuture(mockHttpResponse));

        // When: Processing malformed response
        CompletableFuture<ConversationResponse> future = 
            apiClient.sendRequestAsync(testRequest);
        ConversationResponse response = future.get();

        // Then: Should handle parsing error gracefully
        assertFalse(response.isSuccess());
        assertNotNull(response.getErrorMessage());
        assertTrue(response.getErrorMessage().toLowerCase().contains("json") || 
                  response.getErrorMessage().toLowerCase().contains("parse"),
                  "Should indicate JSON parsing error");
        assertNull(response.getResponse());
    }

    @Test
    @DisplayName("Should differentiate between retryable and non-retryable errors")
    void shouldDifferentiateBetweenRetryableAndNonRetryableErrors() throws Exception {
        // Given: Non-retryable error (401 Unauthorized)
        when(mockHttpResponse.statusCode()).thenReturn(401);
        when(mockHttpResponse.body()).thenReturn("{\"error\":{\"message\":\"Invalid API key\",\"type\":\"authentication_error\"}}");
        when(mockHttpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(CompletableFuture.completedFuture(mockHttpResponse));

        // When: Attempting retry on non-retryable error
        CompletableFuture<ConversationResponse> future = 
            apiClient.generateWithRetry(testRequest, 3);
        ConversationResponse response = future.get();

        // Then: Should fail immediately without retries
        assertFalse(response.isSuccess());
        assertTrue(response.getErrorMessage().contains("Invalid API key") ||
                  response.getErrorMessage().contains("authentication"));
        
        // Should only make 1 attempt (no retries for auth errors)
        verify(mockHttpClient, times(1)).sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }
}