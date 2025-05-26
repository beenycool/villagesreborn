package com.beeny.villagesreborn.platform.fabric.gui;

import com.beeny.villagesreborn.core.config.FirstTimeSetupConfig;
import com.beeny.villagesreborn.core.hardware.HardwareInfo;
import com.beeny.villagesreborn.core.hardware.HardwareInfoManager;
import com.beeny.villagesreborn.core.hardware.HardwareTier;
import com.beeny.villagesreborn.core.llm.LLMProvider;
import com.beeny.villagesreborn.core.llm.LLMProviderManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WelcomeScreen functionality
 * Tests provider selection, model updates, validation, and UI interactions
 */
@ExtendWith(MockitoExtension.class)
class WelcomeScreenTest {

    @Mock
    private HardwareInfoManager mockHardwareManager;
    
    @Mock
    private LLMProviderManager mockLLMManager;
    
    @Mock
    private FirstTimeSetupConfig mockSetupConfig;
    
    private WelcomeScreen welcomeScreen;
    
    @BeforeEach
    void setUp() {
        // Setup default mocks
        when(mockHardwareManager.getHardwareInfo()).thenReturn(createHighTierHardware());
        when(mockLLMManager.getRecommendedModels(any(), any())).thenReturn(Arrays.asList("gpt-4", "gpt-3.5-turbo"));
    }
    
    @Test
    void testProviderSelectionUpdatesModelDropdown() {
        // GIVEN: Welcome screen with mocked dependencies
        when(mockLLMManager.getRecommendedModels(LLMProvider.OPENAI, HardwareTier.HIGH))
            .thenReturn(Arrays.asList("gpt-4", "gpt-3.5-turbo"));
        
        welcomeScreen = new WelcomeScreen(mockHardwareManager, mockLLMManager, mockSetupConfig);
        
        // WHEN: Provider is changed to OpenAI
        welcomeScreen.setSelectedProvider(LLMProvider.OPENAI);
        
        // THEN: Model dropdown is updated with recommended models
        assertThat(welcomeScreen.getAvailableModels()).containsExactly("gpt-4", "gpt-3.5-turbo");
        assertThat(welcomeScreen.getSelectedModel()).isEqualTo("gpt-4");
        
        verify(mockLLMManager).getRecommendedModels(LLMProvider.OPENAI, HardwareTier.HIGH);
    }
    
    @Test
    void testContinueButtonDisabledWithoutSelection() {
        // GIVEN: Welcome screen with no provider selection
        welcomeScreen = new WelcomeScreen(mockHardwareManager, mockLLMManager, mockSetupConfig);
        welcomeScreen.setSelectedProvider(null);
        
        // THEN: Continue button is disabled
        assertThat(welcomeScreen.getContinueButton().active).isFalse();
        assertThat(welcomeScreen.getValidationErrors()).contains("Provider must be selected");
    }
    
    @Test
    void testContinueButtonDisabledWithoutModel() {
        // GIVEN: Welcome screen with provider but no model
        welcomeScreen = new WelcomeScreen(mockHardwareManager, mockLLMManager, mockSetupConfig);
        welcomeScreen.setSelectedProvider(LLMProvider.OPENAI);
        welcomeScreen.setSelectedModel(null);
        
        // THEN: Continue button is disabled
        assertThat(welcomeScreen.getContinueButton().active).isFalse();
        assertThat(welcomeScreen.getValidationErrors()).contains("Model must be selected");
    }
    
    @Test
    void testContinueButtonEnabledWithValidSelection() {
        // GIVEN: Welcome screen with valid selections
        welcomeScreen = new WelcomeScreen(mockHardwareManager, mockLLMManager, mockSetupConfig);
        welcomeScreen.setSelectedProvider(LLMProvider.OPENAI);
        welcomeScreen.setSelectedModel("gpt-3.5-turbo");
        
        // THEN: Continue button is enabled
        assertThat(welcomeScreen.getContinueButton().active).isTrue();
        assertThat(welcomeScreen.getValidationErrors()).isEmpty();
    }
    
    @Test
    void testValidationErrorsPreventSave() {
        // GIVEN: Welcome screen with invalid configuration
        welcomeScreen = new WelcomeScreen(mockHardwareManager, mockLLMManager, mockSetupConfig);
        welcomeScreen.setSelectedProvider(null);
        
        // WHEN: Validation is performed
        List<String> errors = welcomeScreen.getValidationErrors();
        
        // THEN: Validation errors are present
        assertThat(errors).isNotEmpty();
        assertThat(errors).contains("Provider must be selected");
    }
    
    @Test
    void testHardwareInfoDisplayed() {
        // GIVEN: Welcome screen with hardware info
        HardwareInfo hardwareInfo = createHighTierHardware();
        when(mockHardwareManager.getHardwareInfo()).thenReturn(hardwareInfo);
        
        welcomeScreen = new WelcomeScreen(mockHardwareManager, mockLLMManager, mockSetupConfig);
        
        // THEN: Hardware info is accessible
        assertThat(welcomeScreen.getHardwareInfo()).isEqualTo(hardwareInfo);
        assertThat(welcomeScreen.getHardwareInfo().getHardwareTier()).isEqualTo(HardwareTier.HIGH);
    }
    
    @Test
    void testProviderCompatibilityValidation() {
        // GIVEN: Welcome screen with compatibility validation
        var compatibilityResult = LLMProviderManager.ValidationResult.warning("Test warning");
        when(mockLLMManager.validateProviderCompatibility(LLMProvider.LOCAL, HardwareTier.LOW))
            .thenReturn(compatibilityResult);
        when(mockHardwareManager.getHardwareInfo()).thenReturn(createLowTierHardware());
        
        welcomeScreen = new WelcomeScreen(mockHardwareManager, mockLLMManager, mockSetupConfig);
        welcomeScreen.setSelectedProvider(LLMProvider.LOCAL);
        welcomeScreen.setSelectedModel("llama2:7b");
        
        // WHEN: Validation is performed
        // The validation should not add errors for warnings, just log them
        
        // THEN: No validation errors for compatibility warnings
        assertThat(welcomeScreen.getValidationErrors()).isEmpty();
        verify(mockLLMManager).validateProviderCompatibility(LLMProvider.LOCAL, HardwareTier.LOW);
    }
    
    private HardwareInfo createHighTierHardware() {
        return new HardwareInfo(32, 16, true, HardwareTier.HIGH);
    }
    
    private HardwareInfo createLowTierHardware() {
        return new HardwareInfo(4, 2, false, HardwareTier.LOW);
    }
}