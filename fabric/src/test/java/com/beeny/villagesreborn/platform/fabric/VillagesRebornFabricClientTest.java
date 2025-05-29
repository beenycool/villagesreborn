package com.beeny.villagesreborn.platform.fabric;

import com.beeny.villagesreborn.platform.fabric.error.PermanentError;
import com.beeny.villagesreborn.platform.fabric.error.TransientError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for VillagesRebornFabricClient error handling functionality.
 * Tests the error categorization and recovery mechanisms.
 */
@ExtendWith(MockitoExtension.class)
class VillagesRebornFabricClientTest {

    private VillagesRebornFabricClient client;

    @BeforeEach
    void setUp() {
        client = new VillagesRebornFabricClient();
    }

    @Test
    void shouldCategorizeNetworkErrorsAsTransient() throws Exception {
        // Test socket timeout
        SocketTimeoutException timeoutException = new SocketTimeoutException("Connection timeout");
        Exception categorized = invokeCategorizeError(timeoutException);
        
        assertInstanceOf(TransientError.class, categorized);
        TransientError transientError = (TransientError) categorized;
        assertTrue(transientError.shouldRetry());
        assertTrue(transientError.getMessage().contains("Network or temporary failure"));
    }

    @Test
    void shouldCategorizeConnectionRefusedAsTransient() throws Exception {
        ConnectException connectException = new ConnectException("Connection refused");
        Exception categorized = invokeCategorizeError(connectException);
        
        assertInstanceOf(TransientError.class, categorized);
        TransientError transientError = (TransientError) categorized;
        assertTrue(transientError.shouldRetry());
    }

    @Test
    void shouldCategorizeAPIKeyErrorsAsPermanent() throws Exception {
        RuntimeException apiKeyException = new RuntimeException("Invalid API key provided");
        Exception categorized = invokeCategorizeError(apiKeyException);
        
        assertInstanceOf(PermanentError.class, categorized);
        PermanentError permanentError = (PermanentError) categorized;
        assertTrue(permanentError.isAllowDegradedMode());
        assertEquals(PermanentError.ErrorSeverity.MEDIUM, permanentError.getSeverity());
    }

    @Test
    void shouldCategorizeClassNotFoundAsHighSeverityPermanent() throws Exception {
        ClassNotFoundException classException = new ClassNotFoundException("Required class not found");
        Exception categorized = invokeCategorizeError(classException);
        
        assertInstanceOf(PermanentError.class, categorized);
        PermanentError permanentError = (PermanentError) categorized;
        assertEquals(PermanentError.ErrorSeverity.HIGH, permanentError.getSeverity());
        assertFalse(permanentError.isAllowDegradedMode());
    }

    @Test
    void shouldCategorizeConfigurationErrorsAsPermanent() throws Exception {
        RuntimeException configException = new RuntimeException("Missing configuration file");
        Exception categorized = invokeCategorizeError(configException);
        
        assertInstanceOf(PermanentError.class, categorized);
        PermanentError permanentError = (PermanentError) categorized;
        assertTrue(permanentError.isAllowDegradedMode());
    }

    @Test
    void shouldCalculateExponentialBackoffCorrectly() throws Exception {
        Method backoffMethod = VillagesRebornFabricClient.class
            .getDeclaredMethod("calculateBackoffDelay", int.class);
        backoffMethod.setAccessible(true);
        
        // Test exponential backoff: 1s, 2s, 4s, max 8s
        assertEquals(1000L, backoffMethod.invoke(client, 0));
        assertEquals(2000L, backoffMethod.invoke(client, 1));
        assertEquals(4000L, backoffMethod.invoke(client, 2));
        assertEquals(8000L, backoffMethod.invoke(client, 3)); // Capped at 8s
        assertEquals(8000L, backoffMethod.invoke(client, 10)); // Still capped at 8s
    }

    @Test
    void shouldDetermineSeverityCorrectly() throws Exception {
        Method severityMethod = VillagesRebornFabricClient.class
            .getDeclaredMethod("determineSeverity", Throwable.class);
        severityMethod.setAccessible(true);
        
        // High severity
        ClassNotFoundException highSeverityException = new ClassNotFoundException("Critical class missing");
        assertEquals(PermanentError.ErrorSeverity.HIGH, 
                    severityMethod.invoke(client, highSeverityException));
        
        // Medium severity
        RuntimeException mediumSeverityException = new RuntimeException("API key issue");
        assertEquals(PermanentError.ErrorSeverity.MEDIUM, 
                    severityMethod.invoke(client, mediumSeverityException));
        
        // Low severity
        RuntimeException lowSeverityException = new RuntimeException("Minor issue");
        assertEquals(PermanentError.ErrorSeverity.LOW, 
                    severityMethod.invoke(client, lowSeverityException));
    }

    @Test
    void shouldTrackRetryCount() {
        assertFalse(VillagesRebornFabricClient.isDegradedMode());
        
        // Test retry counting through transient error handling
        SocketTimeoutException timeoutException = new SocketTimeoutException("timeout");
        TransientError transientError = new TransientError("Test error", timeoutException, 0, 1000);
        
        assertTrue(transientError.shouldRetry());
        assertEquals(0, transientError.getRetryCount());
        assertEquals(1000L, transientError.getNextRetryDelay());
    }

    @Test
    void shouldNotRetryAfterMaxAttempts() {
        TransientError maxRetriesError = new TransientError("Max retries", null, 3, 1000);
        assertFalse(maxRetriesError.shouldRetry());
    }

    @Test
    void shouldHandleDegradedModeCorrectly() {
        // Initially not in degraded mode
        assertFalse(VillagesRebornFabricClient.isDegradedMode());
        
        // After enabling degraded mode through permanent error
        PermanentError allowDegradedError = new PermanentError(
            "Non-critical error", 
            PermanentError.ErrorSeverity.LOW, 
            true
        );
        
        assertTrue(allowDegradedError.isAllowDegradedMode());
        assertEquals(PermanentError.ErrorSeverity.LOW, allowDegradedError.getSeverity());
    }

    @Test
    void shouldRejectHighSeverityErrorsForDegradedMode() {
        PermanentError criticalError = new PermanentError(
            "Critical system error", 
            PermanentError.ErrorSeverity.HIGH, 
            false
        );
        
        assertFalse(criticalError.isAllowDegradedMode());
        assertEquals(PermanentError.ErrorSeverity.HIGH, criticalError.getSeverity());
    }

    // Helper method to invoke private categorizeError method
    private Exception invokeCategorizeError(Throwable throwable) throws Exception {
        Method categorizeMethod = VillagesRebornFabricClient.class
            .getDeclaredMethod("categorizeError", Throwable.class);
        categorizeMethod.setAccessible(true);
        return (Exception) categorizeMethod.invoke(client, throwable);
    }
}