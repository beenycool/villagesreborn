package com.beeny.ai.provider;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for AI language model providers
 */
public interface AIProvider {
    /**
     * Generate a response to the given prompt
     * 
     * @param prompt the prompt to respond to
     * @param context additional context for the prompt
     * @return a CompletableFuture that will contain the response when available
     */
    CompletableFuture<String> generateResponse(String prompt, Map<String, String> context);
    
    /**
     * Initialize the provider with the given configuration
     * Should validate the configuration and throw appropriate exceptions if invalid
     * 
     * @param config the provider configuration
     * @throws IllegalArgumentException if the configuration is invalid
     */
    void initialize(Map<String, String> config);
    
    /**
     * Get the name of this provider
     * 
     * @return the provider name
     */
    String getName();
    
    /**
     * Check if this provider is available for use
     * 
     * @return true if the provider is available, false otherwise
     */
    boolean isAvailable();
    
    /**
     * Shut down the provider, releasing any resources
     */
    void shutdown();
    
    /**
     * Validate API key and connectivity to the provider
     * 
     * @return CompletableFuture that completes with true if valid, false otherwise
     */
    default CompletableFuture<Boolean> validateAccess() {
        return CompletableFuture.supplyAsync(() -> isAvailable());
    }
}