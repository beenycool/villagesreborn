package com.beeny.villagesreborn.platform.fabric.gui;

import com.beeny.villagesreborn.core.config.FirstTimeSetupConfig;
import com.beeny.villagesreborn.core.llm.LLMProvider;
import com.beeny.villagesreborn.core.llm.LLMProviderManager;
import com.beeny.villagesreborn.platform.fabric.gui.steps.ModelStep;
import com.beeny.villagesreborn.platform.fabric.gui.steps.ProviderStep;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ConfigManagerValidationTest {

    @Mock
    private FirstTimeSetupConfig setupConfig;

    @Mock
    private LLMProviderManager llmManager;

    private ConfigManager configManager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        configManager = new ConfigManager(setupConfig);
    }

    @Test
    void saveAllSteps_ValidInputs_SavesSuccessfully() {
        // Create valid wizard steps
        ModelStep modelStep = new ModelStep(llmManager);
        ProviderStep providerStep = new ProviderStep(llmManager);
        
        // Set valid selections
        setModelStepSelection(modelStep, "gpt-3.5-turbo");
        setProviderStepSelection(providerStep, LLMProvider.OPENAI);
        
        List<WizardStep> steps = Arrays.asList(modelStep, providerStep);
        
        // Should not throw exception
        assertDoesNotThrow(() -> configManager.saveAllSteps(steps));
    }

    @Test
    void saveAllSteps_InvalidModelName_ThrowsException() {
        ModelStep modelStep = new ModelStep(llmManager);
        
        // Set invalid model name with special characters
        setModelStepSelection(modelStep, "invalid@model#name");
        
        List<WizardStep> steps = Arrays.asList(modelStep);
        
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> configManager.saveAllSteps(steps));
        
        assertTrue(exception.getMessage().contains("Configuration save failed"));
        assertTrue(exception.getCause() instanceof IllegalArgumentException);
        assertTrue(exception.getCause().getMessage().contains("Invalid model name"));
    }

    @Test
    void saveAllSteps_EmptyModelName_ThrowsException() {
        ModelStep modelStep = new ModelStep(llmManager);
        
        // Set empty model name
        setModelStepSelection(modelStep, "");
        
        List<WizardStep> steps = Arrays.asList(modelStep);
        
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> configManager.saveAllSteps(steps));
        
        assertTrue(exception.getMessage().contains("Configuration save failed"));
        assertTrue(exception.getCause() instanceof IllegalArgumentException);
    }

    @Test
    void saveAllSteps_NullModelName_ThrowsException() {
        ModelStep modelStep = new ModelStep(llmManager);
        
        // Set null model name
        setModelStepSelection(modelStep, null);
        
        List<WizardStep> steps = Arrays.asList(modelStep);
        
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> configManager.saveAllSteps(steps));
        
        assertTrue(exception.getMessage().contains("Configuration save failed"));
        assertTrue(exception.getCause() instanceof IllegalArgumentException);
    }

    @Test
    void saveAllSteps_TooLongModelName_ThrowsException() {
        ModelStep modelStep = new ModelStep(llmManager);
        
        // Set model name that exceeds maximum length
        String longModelName = "a".repeat(101);
        setModelStepSelection(modelStep, longModelName);
        
        List<WizardStep> steps = Arrays.asList(modelStep);
        
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> configManager.saveAllSteps(steps));
        
        assertTrue(exception.getMessage().contains("Configuration save failed"));
        assertTrue(exception.getCause() instanceof IllegalArgumentException);
        assertTrue(exception.getCause().getMessage().contains("too long"));
    }

    @Test
    void saveAllSteps_ValidEdgeCaseInputs_SavesSuccessfully() {
        ModelStep modelStep = new ModelStep(llmManager);
        
        // Test edge cases that should be valid
        String[] validEdgeCases = {
            "gpt-4-turbo-preview",
            "claude-3-5-sonnet-20241022",
            "llama3.1:8b",
            "mixtral_8x7b",
            "model.name.with.dots",
            "simple_name",
            "UPPERCASE_MODEL",
            "openai:gpt-4o-mini"
        };
        
        for (String modelName : validEdgeCases) {
            setModelStepSelection(modelStep, modelName);
            List<WizardStep> steps = Arrays.asList(modelStep);
            
            assertDoesNotThrow(() -> configManager.saveAllSteps(steps), 
                "Should accept valid model name: " + modelName);
        }
    }

    @Test
    void saveAllSteps_InvalidSpecialCharacters_ThrowsException() {
        ModelStep modelStep = new ModelStep(llmManager);
        
        // Test various invalid special characters
        String[] invalidInputs = {
            "model with spaces",
            "model@symbol",
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
            "model<>angle",
            "model\"quote",
            "model'apostrophe"
        };
        
        for (String invalidInput : invalidInputs) {
            setModelStepSelection(modelStep, invalidInput);
            List<WizardStep> steps = Arrays.asList(modelStep);
            
            RuntimeException exception = assertThrows(RuntimeException.class, 
                () -> configManager.saveAllSteps(steps),
                "Should reject invalid model name: " + invalidInput);
            
            assertTrue(exception.getCause() instanceof IllegalArgumentException);
        }
    }

    @Test
    void saveAllSteps_MultipleStepsWithMixedValidity_ThrowsOnFirstInvalid() {
        ModelStep validModelStep = new ModelStep(llmManager);
        ModelStep invalidModelStep = new ModelStep(llmManager);
        
        setModelStepSelection(validModelStep, "gpt-3.5-turbo");
        setModelStepSelection(invalidModelStep, "invalid@model");
        
        List<WizardStep> steps = Arrays.asList(validModelStep, invalidModelStep);
        
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> configManager.saveAllSteps(steps));
        
        assertTrue(exception.getCause() instanceof IllegalArgumentException);
    }

    // Helper methods to set step selections using reflection or mock steps
    private void setModelStepSelection(ModelStep modelStep, String modelName) {
        try {
            java.lang.reflect.Field field = ModelStep.class.getDeclaredField("selectedModel");
            field.setAccessible(true);
            field.set(modelStep, modelName);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set model selection", e);
        }
    }

    private void setProviderStepSelection(ProviderStep providerStep, LLMProvider provider) {
        try {
            java.lang.reflect.Field field = ProviderStep.class.getDeclaredField("selectedProvider");
            field.setAccessible(true);
            field.set(providerStep, provider);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set provider selection", e);
        }
    }
}