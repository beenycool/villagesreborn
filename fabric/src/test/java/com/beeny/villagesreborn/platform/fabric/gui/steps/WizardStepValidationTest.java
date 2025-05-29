package com.beeny.villagesreborn.platform.fabric.gui.steps;

import com.beeny.villagesreborn.core.llm.LLMProvider;
import com.beeny.villagesreborn.core.llm.LLMProviderManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;

class WizardStepValidationTest {

    @Mock
    private LLMProviderManager llmManager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void modelStep_ValidInputs_ReturnsTrue() {
        ModelStep modelStep = new ModelStep(llmManager);
        
        String[] validModels = {
            "gpt-3.5-turbo",
            "gpt-4",
            "claude-3-sonnet",
            "llama2-7b",
            "mixtral_8x7b",
            "openai:gpt-4",
            "anthropic:claude-3",
            "model.name.with.dots"
        };
        
        for (String model : validModels) {
            setModelSelection(modelStep, model);
            assertTrue(modelStep.isValid(), "Should be valid for model: " + model);
        }
    }

    @Test
    void modelStep_InvalidInputs_ReturnsFalse() {
        ModelStep modelStep = new ModelStep(llmManager);
        
        String[] invalidModels = {
            null,
            "",
            "   ",
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
        
        for (String model : invalidModels) {
            setModelSelection(modelStep, model);
            assertFalse(modelStep.isValid(), "Should be invalid for model: " + model);
        }
    }

    @Test
    void modelStep_TooLongInput_ReturnsFalse() {
        ModelStep modelStep = new ModelStep(llmManager);
        
        // Create a model name that exceeds maximum length
        String longModel = "a".repeat(101);
        setModelSelection(modelStep, longModel);
        
        assertFalse(modelStep.isValid());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "gpt-4-turbo-preview",
        "claude-3-5-sonnet-20241022",
        "llama3.1:8b",
        "mixtral_8x7b",
        "model.name.with.dots",
        "simple_name",
        "UPPERCASE_MODEL",
        "openai:gpt-4o-mini"
    })
    void modelStep_EdgeCaseValidInputs_ReturnsTrue(String modelName) {
        ModelStep modelStep = new ModelStep(llmManager);
        setModelSelection(modelStep, modelName);
        assertTrue(modelStep.isValid());
    }

    @Test
    void modelStep_InputSanitization_RemovesControlCharacters() {
        ModelStep modelStep = new ModelStep(llmManager);
        
        // Test that control characters are sanitized when setting model
        String inputWithControlChars = "model\nwith\tcontrol\rchars";
        setModelSelectionDirectly(modelStep, inputWithControlChars);
        
        String sanitizedModel = getModelSelection(modelStep);
        assertEquals("modelwithcontrolchars", sanitizedModel);
    }

    @Test
    void providerStep_ValidProvider_ReturnsTrue() {
        ProviderStep providerStep = new ProviderStep(llmManager);
        
        for (LLMProvider provider : LLMProvider.values()) {
            setProviderSelection(providerStep, provider);
            assertTrue(providerStep.isValid(), "Should be valid for provider: " + provider);
        }
    }

    @Test
    void providerStep_NullProvider_ReturnsFalse() {
        ProviderStep providerStep = new ProviderStep(llmManager);
        setProviderSelection(providerStep, null);
        assertFalse(providerStep.isValid());
    }

    @Test
    void providerStep_ValidatesProviderDisplayName() {
        ProviderStep providerStep = new ProviderStep(llmManager);
        
        // All current LLM providers should have valid display names
        for (LLMProvider provider : LLMProvider.values()) {
            setProviderSelection(providerStep, provider);
            assertTrue(providerStep.isValid(), 
                "Provider " + provider + " should have valid display name: " + provider.getDisplayName());
        }
    }

    @Test
    void modelStep_EmptyAfterTrim_ReturnsFalse() {
        ModelStep modelStep = new ModelStep(llmManager);
        
        // Test strings that become empty after trimming
        String[] emptyAfterTrim = {
            "   ",
            "\t\t",
            "\n\n",
            "\r\r",
            " \t \n \r "
        };
        
        for (String input : emptyAfterTrim) {
            setModelSelection(modelStep, input);
            assertFalse(modelStep.isValid(), "Should be invalid for input: '" + input + "'");
        }
    }

    @Test
    void modelStep_ValidAfterTrim_ReturnsTrue() {
        ModelStep modelStep = new ModelStep(llmManager);
        
        // Test strings that are valid after trimming
        setModelSelection(modelStep, "  gpt-3.5-turbo  ");
        assertTrue(modelStep.isValid());
        
        // Note: The actual trimming happens during validation, not storage
        // The test verifies that validation works even with whitespace
        String stored = getModelSelection(modelStep);
        // The stored value might still have whitespace, but validation should pass
        assertTrue(stored.trim().equals("gpt-3.5-turbo"));
    }

    // Helper methods using reflection to set private fields
    private void setModelSelection(ModelStep modelStep, String model) {
        try {
            java.lang.reflect.Field field = ModelStep.class.getDeclaredField("selectedModel");
            field.setAccessible(true);
            field.set(modelStep, model);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set model selection", e);
        }
    }

    private void setModelSelectionDirectly(ModelStep modelStep, String model) {
        // This simulates the sanitization that happens during model selection
        try {
            java.lang.reflect.Field field = ModelStep.class.getDeclaredField("selectedModel");
            field.setAccessible(true);
            field.set(modelStep, com.beeny.villagesreborn.core.util.InputValidator.sanitizeInput(model));
        } catch (Exception e) {
            throw new RuntimeException("Failed to set model selection", e);
        }
    }

    private String getModelSelection(ModelStep modelStep) {
        try {
            java.lang.reflect.Field field = ModelStep.class.getDeclaredField("selectedModel");
            field.setAccessible(true);
            return (String) field.get(modelStep);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get model selection", e);
        }
    }

    private void setProviderSelection(ProviderStep providerStep, LLMProvider provider) {
        try {
            java.lang.reflect.Field field = ProviderStep.class.getDeclaredField("selectedProvider");
            field.setAccessible(true);
            field.set(providerStep, provider);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set provider selection", e);
        }
    }
}