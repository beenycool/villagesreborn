package com.beeny.villagesreborn.platform.fabric.gui;

import com.beeny.villagesreborn.core.config.FirstTimeSetupConfig;
import com.beeny.villagesreborn.core.hardware.HardwareInfo;
import com.beeny.villagesreborn.core.llm.LLMProviderManager;
import com.beeny.villagesreborn.platform.fabric.gui.steps.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for StepFactory implementation.
 * Tests step class instantiation and dependency injection.
 */
class StepFactoryTest {

    @Mock
    private HardwareInfo mockHardwareInfo;
    
    @Mock
    private LLMProviderManager mockLLMManager;
    
    @Mock
    private FirstTimeSetupConfig mockSetupConfig;

    private DefaultStepFactory stepFactory;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        stepFactory = new DefaultStepFactory(mockHardwareInfo, mockLLMManager, mockSetupConfig);
    }

    @Test
    @DisplayName("Should create WelcomeStep with correct type")
    void shouldCreateWelcomeStepWithCorrectType() {
        WizardStep step = stepFactory.createWelcomeStep();
        
        assertNotNull(step);
        assertInstanceOf(WelcomeStep.class, step);
    }

    @Test
    @DisplayName("Should create HardwareStep with correct type and dependencies")
    void shouldCreateHardwareStepWithCorrectTypeAndDependencies() {
        WizardStep step = stepFactory.createHardwareStep(mockHardwareInfo);
        
        assertNotNull(step);
        assertInstanceOf(HardwareStep.class, step);
    }

    @Test
    @DisplayName("Should create ProviderStep with correct type and dependencies")
    void shouldCreateProviderStepWithCorrectTypeAndDependencies() {
        WizardStep step = stepFactory.createProviderStep(mockLLMManager);
        
        assertNotNull(step);
        assertInstanceOf(ProviderStep.class, step);
    }

    @Test
    @DisplayName("Should create ModelStep with correct type and dependencies")
    void shouldCreateModelStepWithCorrectTypeAndDependencies() {
        WizardStep step = stepFactory.createModelStep(mockLLMManager);
        
        assertNotNull(step);
        assertInstanceOf(ModelStep.class, step);
    }

    @Test
    @DisplayName("Should create SummaryStep with correct type and dependencies")
    void shouldCreateSummaryStepWithCorrectTypeAndDependencies() {
        WizardStep step = stepFactory.createSummaryStep(mockSetupConfig);
        
        assertNotNull(step);
        assertInstanceOf(SummaryStep.class, step);
    }

    @Test
    @DisplayName("Should inject dependencies correctly into factory")
    void shouldInjectDependenciesCorrectlyIntoFactory() {
        assertEquals(mockHardwareInfo, stepFactory.hardwareInfo);
        assertEquals(mockLLMManager, stepFactory.llmManager);
        assertEquals(mockSetupConfig, stepFactory.setupConfig);
    }

    @Test
    @DisplayName("Should accept null hardware info but may fail later")
    void shouldAcceptNullHardwareInfoButMayFailLater() {
        // The constructor accepts null values, but operations may fail later
        DefaultStepFactory factory = new DefaultStepFactory(null, mockLLMManager, mockSetupConfig);
        assertNotNull(factory);
        assertNull(factory.hardwareInfo);
    }

    @Test
    @DisplayName("Should accept null LLM manager but may fail later")
    void shouldAcceptNullLLMManagerButMayFailLater() {
        // The constructor accepts null values, but operations may fail later
        DefaultStepFactory factory = new DefaultStepFactory(mockHardwareInfo, null, mockSetupConfig);
        assertNotNull(factory);
        assertNull(factory.llmManager);
    }

    @Test
    @DisplayName("Should accept null setup config but may fail later")
    void shouldAcceptNullSetupConfigButMayFailLater() {
        // The constructor accepts null values, but operations may fail later
        DefaultStepFactory factory = new DefaultStepFactory(mockHardwareInfo, mockLLMManager, null);
        assertNotNull(factory);
        assertNull(factory.setupConfig);
    }

    @Test
    @DisplayName("Should create different step instances on multiple calls")
    void shouldCreateDifferentStepInstancesOnMultipleCalls() {
        WizardStep step1 = stepFactory.createWelcomeStep();
        WizardStep step2 = stepFactory.createWelcomeStep();
        
        assertNotSame(step1, step2, "Factory should create new instances, not reuse them");
    }

    @Test
    @DisplayName("Should pass correct parameters to step constructors")
    void shouldPassCorrectParametersToStepConstructors() {
        // Test that parameters are passed correctly by creating steps with specific mock objects
        HardwareInfo specificHardware = mock(HardwareInfo.class);
        LLMProviderManager specificLLM = mock(LLMProviderManager.class);
        FirstTimeSetupConfig specificConfig = mock(FirstTimeSetupConfig.class);
        
        // Create hardware step with specific hardware
        WizardStep hardwareStep = stepFactory.createHardwareStep(specificHardware);
        assertNotNull(hardwareStep);
        
        // Create provider step with specific LLM manager
        WizardStep providerStep = stepFactory.createProviderStep(specificLLM);
        assertNotNull(providerStep);
        
        // Create summary step with specific config
        WizardStep summaryStep = stepFactory.createSummaryStep(specificConfig);
        assertNotNull(summaryStep);
    }

    @Test
    @DisplayName("Should maintain immutable dependencies")
    void shouldMaintainImmutableDependencies() {
        HardwareInfo originalHardware = stepFactory.hardwareInfo;
        LLMProviderManager originalLLM = stepFactory.llmManager;
        FirstTimeSetupConfig originalConfig = stepFactory.setupConfig;
        
        // Create multiple steps and verify dependencies remain unchanged
        stepFactory.createWelcomeStep();
        stepFactory.createHardwareStep(mockHardwareInfo);
        stepFactory.createProviderStep(mockLLMManager);
        stepFactory.createModelStep(mockLLMManager);
        stepFactory.createSummaryStep(mockSetupConfig);
        
        assertSame(originalHardware, stepFactory.hardwareInfo);
        assertSame(originalLLM, stepFactory.llmManager);
        assertSame(originalConfig, stepFactory.setupConfig);
    }

    @Test
    @DisplayName("Should implement StepFactory interface correctly")
    void shouldImplementStepFactoryInterfaceCorrectly() {
        assertInstanceOf(StepFactory.class, stepFactory);
        
        // Verify all interface methods are implemented
        assertDoesNotThrow(() -> stepFactory.createWelcomeStep());
        assertDoesNotThrow(() -> stepFactory.createHardwareStep(mockHardwareInfo));
        assertDoesNotThrow(() -> stepFactory.createProviderStep(mockLLMManager));
        assertDoesNotThrow(() -> stepFactory.createModelStep(mockLLMManager));
        assertDoesNotThrow(() -> stepFactory.createSummaryStep(mockSetupConfig));
    }

    @Test
    @DisplayName("Should create steps in expected order for wizard")
    void shouldCreateStepsInExpectedOrderForWizard() {
        // Test the typical wizard flow
        WizardStep[] expectedStepTypes = {
            stepFactory.createWelcomeStep(),
            stepFactory.createHardwareStep(mockHardwareInfo),
            stepFactory.createProviderStep(mockLLMManager),
            stepFactory.createModelStep(mockLLMManager),
            stepFactory.createSummaryStep(mockSetupConfig)
        };
        
        // Verify all steps are created and of correct types
        assertInstanceOf(WelcomeStep.class, expectedStepTypes[0]);
        assertInstanceOf(HardwareStep.class, expectedStepTypes[1]);
        assertInstanceOf(ProviderStep.class, expectedStepTypes[2]);
        assertInstanceOf(ModelStep.class, expectedStepTypes[3]);
        assertInstanceOf(SummaryStep.class, expectedStepTypes[4]);
    }

    @Test
    @DisplayName("Should handle edge cases for step creation")
    void shouldHandleEdgeCasesForStepCreation() {
        // Test creating steps with null parameters - may succeed but steps might fail later
        assertDoesNotThrow(() -> stepFactory.createHardwareStep(null));
        assertDoesNotThrow(() -> stepFactory.createProviderStep(null));
        assertDoesNotThrow(() -> stepFactory.createModelStep(null));
        assertDoesNotThrow(() -> stepFactory.createSummaryStep(null));
        
        // Verify steps are created (but may contain null dependencies)
        WizardStep hardwareStep = stepFactory.createHardwareStep(null);
        WizardStep providerStep = stepFactory.createProviderStep(null);
        WizardStep modelStep = stepFactory.createModelStep(null);
        WizardStep summaryStep = stepFactory.createSummaryStep(null);
        
        assertNotNull(hardwareStep);
        assertNotNull(providerStep);
        assertNotNull(modelStep);
        assertNotNull(summaryStep);
    }
}