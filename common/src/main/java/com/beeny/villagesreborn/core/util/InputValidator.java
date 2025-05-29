package com.beeny.villagesreborn.core.util;

import java.util.regex.Pattern;

/**
 * Utility class for validating user inputs across the application
 */
public class InputValidator {
    
    // Pattern for safe model names (alphanumeric, hyphens, underscores, dots, colons for namespaces)
    private static final Pattern MODEL_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._:-]+$");
    
    // Pattern for API keys/tokens (alphanumeric and common special chars, no spaces)
    private static final Pattern API_KEY_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]+$");
    
    // Maximum reasonable lengths for various inputs
    private static final int MAX_MODEL_NAME_LENGTH = 100;
    private static final int MAX_API_KEY_LENGTH = 200;
    private static final int MAX_PROVIDER_NAME_LENGTH = 50;
    
    /**
     * Validates a model name input
     * @param modelName the model name to validate
     * @return ValidationResult containing validation status and error message
     */
    public static ValidationResult validateModelName(String modelName) {
        if (modelName == null) {
            return ValidationResult.invalid("Model name cannot be null");
        }
        
        String trimmed = modelName.trim();
        if (trimmed.isEmpty()) {
            return ValidationResult.invalid("Model name cannot be empty");
        }
        
        if (trimmed.length() > MAX_MODEL_NAME_LENGTH) {
            return ValidationResult.invalid("Model name too long (max " + MAX_MODEL_NAME_LENGTH + " characters)");
        }
        
        if (!MODEL_NAME_PATTERN.matcher(trimmed).matches()) {
            return ValidationResult.invalid("Model name contains invalid characters (only alphanumeric, dots, hyphens, underscores, colons allowed)");
        }
        
        return ValidationResult.valid();
    }
    
    /**
     * Validates an API key input
     * @param apiKey the API key to validate
     * @return ValidationResult containing validation status and error message
     */
    public static ValidationResult validateApiKey(String apiKey) {
        if (apiKey == null) {
            return ValidationResult.invalid("API key cannot be null");
        }
        
        String trimmed = apiKey.trim();
        if (trimmed.isEmpty()) {
            return ValidationResult.invalid("API key cannot be empty");
        }
        
        if (trimmed.length() > MAX_API_KEY_LENGTH) {
            return ValidationResult.invalid("API key too long (max " + MAX_API_KEY_LENGTH + " characters)");
        }
        
        if (!API_KEY_PATTERN.matcher(trimmed).matches()) {
            return ValidationResult.invalid("API key contains invalid characters");
        }
        
        return ValidationResult.valid();
    }
    
    /**
     * Validates a provider name input
     * @param providerName the provider name to validate
     * @return ValidationResult containing validation status and error message
     */
    public static ValidationResult validateProviderName(String providerName) {
        if (providerName == null) {
            return ValidationResult.invalid("Provider name cannot be null");
        }
        
        String trimmed = providerName.trim();
        if (trimmed.isEmpty()) {
            return ValidationResult.invalid("Provider name cannot be empty");
        }
        
        if (trimmed.length() > MAX_PROVIDER_NAME_LENGTH) {
            return ValidationResult.invalid("Provider name too long (max " + MAX_PROVIDER_NAME_LENGTH + " characters)");
        }
        
        return ValidationResult.valid();
    }
    
    /**
     * Validates a general text input with length constraints
     * @param input the input to validate
     * @param fieldName the name of the field being validated
     * @param maxLength maximum allowed length
     * @param allowEmpty whether empty values are allowed
     * @return ValidationResult containing validation status and error message
     */
    public static ValidationResult validateTextInput(String input, String fieldName, int maxLength, boolean allowEmpty) {
        if (input == null) {
            return ValidationResult.invalid(fieldName + " cannot be null");
        }
        
        String trimmed = input.trim();
        if (!allowEmpty && trimmed.isEmpty()) {
            return ValidationResult.invalid(fieldName + " cannot be empty");
        }
        
        if (trimmed.length() > maxLength) {
            return ValidationResult.invalid(fieldName + " too long (max " + maxLength + " characters)");
        }
        
        return ValidationResult.valid();
    }
    
    /**
     * Sanitizes input by trimming whitespace and removing potentially dangerous characters
     * @param input the input to sanitize
     * @return sanitized input string
     */
    public static String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }
        
        return input.trim()
                .replaceAll("[\r\n\t]", "") // Remove control characters
                .replaceAll("[\\x00-\\x1F\\x7F]", ""); // Remove other control characters
    }
    
    /**
     * Result of input validation
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;
        
        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }
        
        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }
        
        public static ValidationResult invalid(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
    }
}