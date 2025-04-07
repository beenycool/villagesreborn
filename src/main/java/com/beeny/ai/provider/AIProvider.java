package com.beeny.ai.provider;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** AI language model provider interface */
public interface AIProvider {
    /**
     * Generate a response asynchronously
     * @param prompt prompt text
     * @param context additional context
     * @return future with response
     */
    CompletableFuture<String> generateResponse(String prompt, Map<String, String> context);
    
    /**
     * Initialize provider with configuration
     * @param config provider config
     * @throws IllegalArgumentException if invalid
     */
    void initialize(Map<String, String> config);
    
    /**
     * @return provider name
     */
    String getName();
    
    /**
     * @return true if available
     */
    boolean isAvailable();
    
    /** Shut down and release resources */
    void shutdown();
    
    /**
     * Validate API key and connectivity
     * @return future with true if valid
     */
    default CompletableFuture<Boolean> validateAccess() {
        return CompletableFuture.supplyAsync(() -> isAvailable());
    }
}