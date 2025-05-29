package com.beeny.villagesreborn.core.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class InputValidatorTest {

    @Test
    void validateModelName_ValidInputs_ReturnsValid() {
        // Valid model names
        String[] validNames = {
            "gpt-3.5-turbo",
            "claude-3-sonnet",
            "llama2-7b",
            "mixtral_8x7b",
            "openai:gpt-4",
            "anthropic:claude-3",
            "model.name.with.dots",
            "simple_model",
            "Model123"
        };
        
        for (String name : validNames) {
            InputValidator.ValidationResult result = InputValidator.validateModelName(name);
            assertTrue(result.isValid(), "Expected valid result for: " + name);
            assertNull(result.getErrorMessage());
        }
    }
    
    @Test
    void validateModelName_InvalidInputs_ReturnsInvalid() {
        // Test null input
        InputValidator.ValidationResult result = InputValidator.validateModelName(null);
        assertFalse(result.isValid());
        assertEquals("Model name cannot be null", result.getErrorMessage());
        
        // Test empty input
        result = InputValidator.validateModelName("");
        assertFalse(result.isValid());
        assertEquals("Model name cannot be empty", result.getErrorMessage());
        
        // Test whitespace only
        result = InputValidator.validateModelName("   ");
        assertFalse(result.isValid());
        assertEquals("Model name cannot be empty", result.getErrorMessage());
    }
    
    @Test
    void validateModelName_InvalidCharacters_ReturnsInvalid() {
        String[] invalidNames = {
            "model with spaces",
            "model@invalid",
            "model#hash",
            "model%percent",
            "model&ampersand",
            "model*asterisk",
            "model+plus",
            "model=equals",
            "model[bracket]",
            "model{brace}",
            "model|pipe",
            "model\\backslash",
            "model/slash",
            "model?question",
            "model<>angle"
        };
        
        for (String name : invalidNames) {
            InputValidator.ValidationResult result = InputValidator.validateModelName(name);
            assertFalse(result.isValid(), "Expected invalid result for: " + name);
            assertTrue(result.getErrorMessage().contains("invalid characters"));
        }
    }
    
    @Test
    void validateModelName_TooLong_ReturnsInvalid() {
        String longName = "a".repeat(101); // Exceeds MAX_MODEL_NAME_LENGTH
        InputValidator.ValidationResult result = InputValidator.validateModelName(longName);
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("too long"));
    }
    
    @Test
    void validateApiKey_ValidInputs_ReturnsValid() {
        String[] validKeys = {
            "sk-1234567890abcdef",
            "api_key_123",
            "token-with-dashes",
            "key.with.dots",
            "UPPERCASE_KEY",
            "mixedCaseKey123",
            "a".repeat(200) // At max length
        };
        
        for (String key : validKeys) {
            InputValidator.ValidationResult result = InputValidator.validateApiKey(key);
            assertTrue(result.isValid(), "Expected valid result for: " + key);
            assertNull(result.getErrorMessage());
        }
    }
    
    @Test
    void validateApiKey_InvalidInputs_ReturnsInvalid() {
        // Test null input
        InputValidator.ValidationResult result = InputValidator.validateApiKey(null);
        assertFalse(result.isValid());
        assertEquals("API key cannot be null", result.getErrorMessage());
        
        // Test empty input
        result = InputValidator.validateApiKey("");
        assertFalse(result.isValid());
        assertEquals("API key cannot be empty", result.getErrorMessage());
        
        // Test whitespace only
        result = InputValidator.validateApiKey("   ");
        assertFalse(result.isValid());
        assertEquals("API key cannot be empty", result.getErrorMessage());
        
        // Test too long
        String longKey = "a".repeat(201);
        result = InputValidator.validateApiKey(longKey);
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("too long"));
        
        // Test invalid characters
        result = InputValidator.validateApiKey("key with spaces");
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("invalid characters"));
    }
    
    @Test
    void validateProviderName_ValidInputs_ReturnsValid() {
        String[] validNames = {
            "OpenAI",
            "Anthropic",
            "Google",
            "Microsoft Azure",
            "AWS Bedrock",
            "a".repeat(50) // At max length
        };
        
        for (String name : validNames) {
            InputValidator.ValidationResult result = InputValidator.validateProviderName(name);
            assertTrue(result.isValid(), "Expected valid result for: " + name);
            assertNull(result.getErrorMessage());
        }
    }
    
    @Test
    void validateProviderName_InvalidInputs_ReturnsInvalid() {
        // Test null input
        InputValidator.ValidationResult result = InputValidator.validateProviderName(null);
        assertFalse(result.isValid());
        assertEquals("Provider name cannot be null", result.getErrorMessage());
        
        // Test empty input
        result = InputValidator.validateProviderName("");
        assertFalse(result.isValid());
        assertEquals("Provider name cannot be empty", result.getErrorMessage());
        
        // Test too long
        String longName = "a".repeat(51);
        result = InputValidator.validateProviderName(longName);
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("too long"));
    }
    
    @Test
    void validateTextInput_ValidInputs_ReturnsValid() {
        InputValidator.ValidationResult result = InputValidator.validateTextInput("valid text", "Field", 100, false);
        assertTrue(result.isValid());
        
        // Test empty allowed
        result = InputValidator.validateTextInput("", "Field", 100, true);
        assertTrue(result.isValid());
        
        // Test at max length
        result = InputValidator.validateTextInput("a".repeat(50), "Field", 50, false);
        assertTrue(result.isValid());
    }
    
    @Test
    void validateTextInput_InvalidInputs_ReturnsInvalid() {
        // Test null
        InputValidator.ValidationResult result = InputValidator.validateTextInput(null, "Field", 100, false);
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("cannot be null"));
        
        // Test empty not allowed
        result = InputValidator.validateTextInput("", "Field", 100, false);
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("cannot be empty"));
        
        // Test too long
        result = InputValidator.validateTextInput("a".repeat(101), "Field", 100, false);
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("too long"));
    }
    
    @Test
    void sanitizeInput_RemovesControlCharacters() {
        String input = "text\nwith\tcontrol\rcharacters\u0000";
        String sanitized = InputValidator.sanitizeInput(input);
        assertEquals("textwithcontrolcharacters", sanitized);
    }
    
    @Test
    void sanitizeInput_TrimsWhitespace() {
        String input = "  text with spaces  ";
        String sanitized = InputValidator.sanitizeInput(input);
        assertEquals("text with spaces", sanitized);
    }
    
    @Test
    void sanitizeInput_HandlesNull() {
        String sanitized = InputValidator.sanitizeInput(null);
        assertNull(sanitized);
    }
    
    @ParameterizedTest
    @ValueSource(strings = {
        "normal-text",
        "text_with_underscores",
        "text.with.dots",
        "text:with:colons"
    })
    void sanitizeInput_PreservesValidCharacters(String input) {
        String sanitized = InputValidator.sanitizeInput(input);
        assertEquals(input, sanitized);
    }
}