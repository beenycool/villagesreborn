package com.beeny.villagesreborn.core.governance;

/**
 * Result of applying a village policy
 */
public class PolicyResult {
    private final boolean successful;
    private final String message;
    
    public PolicyResult(boolean successful, String message) {
        this.successful = successful;
        this.message = message;
    }
    
    public boolean isSuccessful() {
        return successful;
    }
    
    public String getMessage() {
        return message;
    }
}