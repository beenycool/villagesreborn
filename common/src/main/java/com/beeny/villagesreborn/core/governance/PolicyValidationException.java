package com.beeny.villagesreborn.core.governance;

/**
 * Exception thrown when a policy has invalid parameters
 */
public class PolicyValidationException extends RuntimeException {
    
    public PolicyValidationException(String message) {
        super(message);
    }
    
    public PolicyValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}