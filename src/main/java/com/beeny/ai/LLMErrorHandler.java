package com.beeny.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;

import java.util.HashMap;
import java.util.Map;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * Central handler for LLM-related errors.
 * Provides user-friendly error reporting and manages error state.
 */
public class LLMErrorHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn");
    private static final LLMErrorHandler INSTANCE = new LLMErrorHandler();
    
    // Map to track when errors were last reported to avoid spamming
    private final Map<ErrorType, Instant> lastReportedErrors = new HashMap<>();
    private static final long ERROR_COOLDOWN_SECONDS = 30; // Only show each error type once per 30 seconds
    
    public enum ErrorType {
        INVALID_API_KEY("Invalid API Key", "The API key is missing or invalid. Please check your configuration."),
        CONNECTION_ERROR("Connection Error", "Could not connect to the AI provider. Please check your internet connection."),
        API_RATE_LIMIT("Rate Limit Exceeded", "The AI provider's rate limit has been exceeded. Please try again later."),
        PROVIDER_ERROR("AI Provider Error", "The AI provider reported an error. Please try a different provider or check your configuration."),
        GENERIC_ERROR("AI Error", "An unexpected error occurred with the AI service.");
        
        private final String title;
        private final String defaultMessage;
        
        ErrorType(String title, String defaultMessage) {
            this.title = title;
            this.defaultMessage = defaultMessage;
        }
        
        public String getTitle() {
            return title;
        }
        
        public String getDefaultMessage() {
            return defaultMessage;
        }
    }
    
    private LLMErrorHandler() {
        // Private constructor for singleton
    }
    
    public static LLMErrorHandler getInstance() {
        return INSTANCE;
    }
    
    /**
     * Report an error to the player via toast notification
     */
    public void reportErrorToClient(ErrorType errorType, String details) {
        LOGGER.error("LLM Error: {} - {}", errorType.getTitle(), details);
        
        if (shouldThrottleError(errorType)) {
            return; // Skip showing this error if it's being throttled
        }
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            client.execute(() -> {
                String message = details != null && !details.isEmpty() 
                    ? details 
                    : errorType.getDefaultMessage();
                
                SystemToast.show(
                    client.getToastManager(), 
                    SystemToast.Type.NARRATOR_TOGGLE, 
                    Text.literal(errorType.getTitle()).formatted(Formatting.RED),
                    Text.literal(message)
                );
            });
        }
    }
    
    /**
     * Send error message to the player in-game
     */
    public void reportErrorToPlayer(ServerPlayerEntity player, ErrorType errorType, String details) {
        if (player == null || shouldThrottleError(errorType)) {
            return;
        }
        
        String message = details != null && !details.isEmpty() 
            ? details 
            : errorType.getDefaultMessage();
            
        Text errorMessage = Text.literal("⚠ " + errorType.getTitle() + ": " + message)
            .formatted(Formatting.RED);
            
        player.sendMessage(errorMessage, false);
    }
    
    /**
     * Check if error is being reported too frequently
     */
    private boolean shouldThrottleError(ErrorType errorType) {
        Instant now = Instant.now();
        Instant lastReported = lastReportedErrors.get(errorType);
        
        if (lastReported != null) {
            long secondsSinceLastReport = now.getEpochSecond() - lastReported.getEpochSecond();
            if (secondsSinceLastReport < ERROR_COOLDOWN_SECONDS) {
                return true; // Throttle this error
            }
        }
        
        // Update the last reported time
        lastReportedErrors.put(errorType, now);
        return false;
    }
    
    /**
     * Parse error message to determine error type
     */
    public ErrorType determineErrorType(Throwable error) {
        String message = error.getMessage();
        if (message == null) {
            return ErrorType.GENERIC_ERROR;
        }
        
        message = message.toLowerCase();
        
        if (message.contains("api key") || message.contains("apikey") || 
            message.contains("authentication") || message.contains("auth") || 
            message.contains("401") || message.contains("unauthorized")) {
            return ErrorType.INVALID_API_KEY;
        }
        
        if (message.contains("connect") || message.contains("timeout") || 
            message.contains("network") || message.contains("unreachable") ||
            message.contains("connection")) {
            return ErrorType.CONNECTION_ERROR;
        }
        
        if (message.contains("rate") || message.contains("limit") || 
            message.contains("too many requests") || message.contains("429")) {
            return ErrorType.API_RATE_LIMIT;
        }
        
        return ErrorType.PROVIDER_ERROR;
    }
}