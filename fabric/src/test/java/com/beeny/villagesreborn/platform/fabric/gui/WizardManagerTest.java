package com.beeny.villagesreborn.platform.fabric.gui;

import com.beeny.villagesreborn.core.config.FirstTimeSetupConfig;
import com.beeny.villagesreborn.core.hardware.HardwareInfo;
import com.beeny.villagesreborn.core.llm.LLMProviderManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for WizardManager following TDD principles.
 * Tests step lifecycle, navigation, validation failures, and state transitions.
 */
class WizardManagerTest {

    @Mock
    private StepFactory mockStepFactory;
    
    @Mock
    private WizardStep mockWelcomeStep;
    
    @Mock
    private WizardStep mockHardwareStep;
    
    @Mock
    private WizardStep mockProviderStep;
    
    @Mock
    private WizardStep mockModelStep;
    
    @Mock
    private WizardStep mockSummaryStep;
    
    @Mock
    private HardwareInfo mockHardwareInfo;
    
    @Mock
    private LLMProviderManager mockLLMManager;
    
    @Mock
    private FirstTimeSetupConfig mockSetupConfig;

    private WizardManager wizardManager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        setupMockStepFactory();
        wizardManager = new WizardManager(mockStepFactory);
    }

    private void setupMockStepFactory() {
        // Create a real DefaultStepFactory with mocked dependencies for testing
        // This is necessary because WizardManager casts to DefaultStepFactory
        mockStepFactory = new DefaultStepFactory(mockHardwareInfo, mockLLMManager, mockSetupConfig) {
            @Override
            public WizardStep createWelcomeStep() {
                return mockWelcomeStep;
            }
            
            @Override
            public WizardStep createHardwareStep(HardwareInfo hardwareInfo) {
                return mockHardwareStep;
            }
            
            @Override
            public WizardStep createProviderStep(LLMProviderManager llmManager) {
                return mockProviderStep;
            }
            
            @Override
            public WizardStep createModelStep(LLMProviderManager llmManager) {
                return mockModelStep;
            }
            
            @Override
            public WizardStep createSummaryStep(FirstTimeSetupConfig setupConfig) {
                return mockSummaryStep;
            }
        };
    }

    @Test
    @DisplayName("Should initialize with correct number of steps")
    void shouldInitializeWithCorrectStepCount() {
        assertEquals(5, wizardManager.getTotalSteps());
    }

    @Test
    @DisplayName("Should start at first step (index 0)")
    void shouldStartAtFirstStep() {
        assertEquals(0, wizardManager.getCurrentStepIndex());
        assertEquals(mockWelcomeStep, wizardManager.getCurrentStep());
    }

    @Test
    @DisplayName("Should return all wizard steps")
    void shouldReturnAllSteps() {
        List<WizardStep> steps = wizardManager.getSteps();
        assertEquals(5, steps.size());
        assertEquals(mockWelcomeStep, steps.get(0));
        assertEquals(mockHardwareStep, steps.get(1));
        assertEquals(mockProviderStep, steps.get(2));
        assertEquals(mockModelStep, steps.get(3));
        assertEquals(mockSummaryStep, steps.get(4));
    }

    @Test
    @DisplayName("Should navigate forward correctly")
    void shouldNavigateForwardCorrectly() {
        // Initially at step 0
        assertTrue(wizardManager.hasNext());
        assertFalse(wizardManager.hasPrevious());
        assertFalse(wizardManager.isLastStep());

        // Move to step 1
        wizardManager.nextStep();
        assertEquals(1, wizardManager.getCurrentStepIndex());
        assertEquals(mockHardwareStep, wizardManager.getCurrentStep());
        assertTrue(wizardManager.hasNext());
        assertTrue(wizardManager.hasPrevious());
        assertFalse(wizardManager.isLastStep());

        // Move to last step
        wizardManager.setCurrentStepIndex(4);
        assertEquals(4, wizardManager.getCurrentStepIndex());
        assertEquals(mockSummaryStep, wizardManager.getCurrentStep());
        assertFalse(wizardManager.hasNext());
        assertTrue(wizardManager.hasPrevious());
        assertTrue(wizardManager.isLastStep());
    }

    @Test
    @DisplayName("Should navigate backward correctly")
    void shouldNavigateBackwardCorrectly() {
        // Start at step 2
        wizardManager.setCurrentStepIndex(2);
        assertEquals(2, wizardManager.getCurrentStepIndex());
        
        // Move backward
        wizardManager.previousStep();
        assertEquals(1, wizardManager.getCurrentStepIndex());
        assertEquals(mockHardwareStep, wizardManager.getCurrentStep());
        
        // Move to first step
        wizardManager.previousStep();
        assertEquals(0, wizardManager.getCurrentStepIndex());
        assertEquals(mockWelcomeStep, wizardManager.getCurrentStep());
        
        // Try to move before first step (should stay at 0)
        wizardManager.previousStep();
        assertEquals(0, wizardManager.getCurrentStepIndex());
    }

    @Test
    @DisplayName("Should handle boundary conditions for next navigation")
    void shouldHandleBoundaryConditionsForNext() {
        // Move to last step
        wizardManager.setCurrentStepIndex(4);
        assertTrue(wizardManager.isLastStep());
        assertFalse(wizardManager.hasNext());
        
        // Try to move past last step
        wizardManager.nextStep();
        assertEquals(4, wizardManager.getCurrentStepIndex()); // Should remain at last step
    }

    @Test
    @DisplayName("Should handle boundary conditions for previous navigation")
    void shouldHandleBoundaryConditionsForPrevious() {
        // Start at first step
        assertEquals(0, wizardManager.getCurrentStepIndex());
        assertFalse(wizardManager.hasPrevious());
        
        // Try to move before first step
        wizardManager.previousStep();
        assertEquals(0, wizardManager.getCurrentStepIndex()); // Should remain at first step
    }

    @Test
    @DisplayName("Should clamp step index to valid range")
    void shouldClampStepIndexToValidRange() {
        // Test negative index
        wizardManager.setCurrentStepIndex(-5);
        assertEquals(0, wizardManager.getCurrentStepIndex());
        
        // Test index beyond last step
        wizardManager.setCurrentStepIndex(10);
        assertEquals(4, wizardManager.getCurrentStepIndex());
        
        // Test valid index
        wizardManager.setCurrentStepIndex(2);
        assertEquals(2, wizardManager.getCurrentStepIndex());
    }

    @Test
    @DisplayName("Should maintain step state during navigation")
    void shouldMaintainStepStateDuringNavigation() {
        // Configure step validation states
        when(mockWelcomeStep.isValid()).thenReturn(true);
        when(mockHardwareStep.isValid()).thenReturn(false);
        when(mockProviderStep.isValid()).thenReturn(true);
        
        // Navigate and verify step states are maintained
        wizardManager.nextStep(); // Move to hardware step
        WizardStep currentStep = wizardManager.getCurrentStep();
        assertEquals(mockHardwareStep, currentStep);
        assertFalse(currentStep.isValid());
        
        wizardManager.nextStep(); // Move to provider step
        currentStep = wizardManager.getCurrentStep();
        assertEquals(mockProviderStep, currentStep);
        assertTrue(currentStep.isValid());
    }

    @Test
    @DisplayName("Should handle step factory with proper dependency injection")
    void shouldHandleStepFactoryWithDependencyInjection() {
        // Verify that the factory creates the expected steps
        DefaultStepFactory factory = (DefaultStepFactory) mockStepFactory;
        
        // Verify dependencies are correctly injected
        assertEquals(mockHardwareInfo, factory.hardwareInfo);
        assertEquals(mockLLMManager, factory.llmManager);
        assertEquals(mockSetupConfig, factory.setupConfig);
        
        // Verify correct steps are created
        assertEquals(mockWelcomeStep, factory.createWelcomeStep());
        assertEquals(mockHardwareStep, factory.createHardwareStep(mockHardwareInfo));
        assertEquals(mockProviderStep, factory.createProviderStep(mockLLMManager));
        assertEquals(mockModelStep, factory.createModelStep(mockLLMManager));
        assertEquals(mockSummaryStep, factory.createSummaryStep(mockSetupConfig));
    }

    @Test
    @DisplayName("Should handle null step factory gracefully")
    void shouldHandleNullStepFactoryGracefully() {
        assertThrows(NullPointerException.class, () -> {
            new WizardManager(null);
        });
    }

    @Test
    @DisplayName("Should provide correct navigation states for each step")
    void shouldProvideCorrectNavigationStatesForEachStep() {
        // Step 0 (Welcome)
        wizardManager.setCurrentStepIndex(0);
        assertFalse(wizardManager.hasPrevious());
        assertTrue(wizardManager.hasNext());
        assertFalse(wizardManager.isLastStep());
        
        // Step 1 (Hardware)
        wizardManager.setCurrentStepIndex(1);
        assertTrue(wizardManager.hasPrevious());
        assertTrue(wizardManager.hasNext());
        assertFalse(wizardManager.isLastStep());
        
        // Step 2 (Provider)
        wizardManager.setCurrentStepIndex(2);
        assertTrue(wizardManager.hasPrevious());
        assertTrue(wizardManager.hasNext());
        assertFalse(wizardManager.isLastStep());
        
        // Step 3 (Model)
        wizardManager.setCurrentStepIndex(3);
        assertTrue(wizardManager.hasPrevious());
        assertTrue(wizardManager.hasNext());
        assertFalse(wizardManager.isLastStep());
        
        // Step 4 (Summary)
        wizardManager.setCurrentStepIndex(4);
        assertTrue(wizardManager.hasPrevious());
        assertFalse(wizardManager.hasNext());
        assertTrue(wizardManager.isLastStep());
    }

    @Test
    @DisplayName("Should simulate user navigation flow")
    void shouldSimulateUserNavigationFlow() {
        // Simulate a typical user flow
        
        // Start at welcome step
        assertEquals(0, wizardManager.getCurrentStepIndex());
        
        // User clicks "Next" to go to hardware step
        wizardManager.nextStep();
        assertEquals(1, wizardManager.getCurrentStepIndex());
        
        // User clicks "Next" to go to provider step
        wizardManager.nextStep();
        assertEquals(2, wizardManager.getCurrentStepIndex());
        
        // User clicks "Back" to return to hardware step
        wizardManager.previousStep();
        assertEquals(1, wizardManager.getCurrentStepIndex());
        
        // User clicks "Next" twice to reach model step
        wizardManager.nextStep();
        wizardManager.nextStep();
        assertEquals(3, wizardManager.getCurrentStepIndex());
        
        // User clicks "Next" to reach summary step
        wizardManager.nextStep();
        assertEquals(4, wizardManager.getCurrentStepIndex());
        assertTrue(wizardManager.isLastStep());
    }
}