package com.beeny.ai;

import com.beeny.ai.VillagerAIManager;
import com.beeny.config.AIConfig;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class VillagerAIErrorHandlingTest {
    private VillagerAIManager aiManager;
    private AIConfig mockConfig;

    @BeforeEach
    void setUp() {
        aiManager = new VillagerAIManager();
        mockConfig = Mockito.mock(AIConfig.class);
    }

    @Test
    void testApiFailureFallback() {
        // Simulate API failure by passing null API client
        Exception exception = assertThrows(IllegalArgumentException.class, 
            () -> aiManager.initializeWithApiClient(null));
        assertEquals("API client cannot be null", exception.getMessage());
    }

    @Test
    void testInvalidInputHandling() {
        // Test with invalid input parameters
        Exception exception = assertThrows(IllegalArgumentException.class,
            () -> aiManager.processInput(""));
        assertEquals("Input cannot be empty", exception.getMessage());
    }

    @Test
    void testConfigFallbackWhenInvalid() {
        // Setup invalid config
        when(mockConfig.getMaxRetries()).thenReturn(-1);
        when(mockConfig.getTimeout()).thenReturn(0);
        
        aiManager.applyConfig(mockConfig);
        
        // Verify fallback to default values
        assertEquals(3, aiManager.getCurrentMaxRetries());
        assertEquals(5000, aiManager.getCurrentTimeout());
    }

    @Test
    void testErrorRecoveryAfterInvalidConfig() {
        // Apply invalid config then valid config
        when(mockConfig.getMaxRetries()).thenReturn(-1);
        aiManager.applyConfig(mockConfig);
        
        AIConfig validConfig = Mockito.mock(AIConfig.class);
        when(validConfig.getMaxRetries()).thenReturn(5);
        when(validConfig.getTimeout()).thenReturn(10000);
        
        aiManager.applyConfig(validConfig);
        
        assertEquals(5, aiManager.getCurrentMaxRetries());
        assertEquals(10000, aiManager.getCurrentTimeout());
    }

    @Test
    void testApiRateLimitHandling() {
        // Test basic rate limiting
        when(mockConfig.getRateLimit()).thenReturn(5);
        aiManager.applyConfig(mockConfig);
        
        // Use all allowed requests
        for (int i = 0; i < 5; i++) {
            assertDoesNotThrow(() -> aiManager.processInput("valid input " + i));
        }
        
        // Verify limit enforcement
        Exception exception = assertThrows(RuntimeException.class,
            () -> aiManager.processInput("blocked input"));
        assertTrue(exception.getMessage().contains("Rate limit exceeded"));
        
        // Test different rate limit configuration
        when(mockConfig.getRateLimit()).thenReturn(2);
        aiManager.applyConfig(mockConfig);
        
        assertDoesNotThrow(() -> aiManager.processInput("first"));
        assertDoesNotThrow(() -> aiManager.processInput("second"));
        assertThrows(RuntimeException.class, () -> aiManager.processInput("third"));
    }

    @Test
    void testChatVsApiRateLimiting() {
        // Test differential rate limits
        when(mockConfig.getChatRateLimit()).thenReturn(10);
        when(mockConfig.getApiRateLimit()).thenReturn(3);
        aiManager.applyConfig(mockConfig);
        
        // Use API quota
        for (int i = 0; i < 3; i++) {
            assertDoesNotThrow(() -> aiManager.processApiRequest("api-" + i));
        }
        assertThrows(RuntimeException.class, () -> aiManager.processApiRequest("api-blocked"));
        
        // Chat should still work
        for (int i = 0; i < 10; i++) {
            assertDoesNotThrow(() -> aiManager.processChat("chat-" + i));
        }
        assertThrows(RuntimeException.class, () -> aiManager.processChat("chat-blocked"));
    }
}