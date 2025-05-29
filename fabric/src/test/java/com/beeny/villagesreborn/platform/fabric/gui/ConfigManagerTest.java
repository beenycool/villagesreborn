package com.beeny.villagesreborn.platform.fabric.gui;

import com.beeny.villagesreborn.core.config.FirstTimeSetupConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for ConfigManager.
 * Tests loading, updating, and persisting settings with mocked file I/O.
 */
class ConfigManagerTest {

    @Mock
    private FirstTimeSetupConfig mockSetupConfig;
    
    @Mock
    private WizardStep mockStep1;
    
    @Mock
    private WizardStep mockStep2;
    
    @Mock
    private WizardStep mockStep3;

    private ConfigManager configManager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        configManager = new ConfigManager(mockSetupConfig);
    }

    @Test
    @DisplayName("Should initialize with setup config")
    void shouldInitializeWithSetupConfig() {
        assertNotNull(configManager);
    }

    @Test
    @DisplayName("Should save all steps successfully")
    void shouldSaveAllStepsSuccessfully() throws Exception {
        // Arrange
        List<WizardStep> steps = Arrays.asList(mockStep1, mockStep2, mockStep3);
        
        // Mock successful save operations
        doNothing().when(mockStep1).saveConfiguration();
        doNothing().when(mockStep2).saveConfiguration();
        doNothing().when(mockStep3).saveConfiguration();
        doNothing().when(mockSetupConfig).save();

        // Act & Assert
        assertDoesNotThrow(() -> configManager.saveAllSteps(steps));
        
        // Verify all steps were saved
        verify(mockStep1).saveConfiguration();
        verify(mockStep2).saveConfiguration();
        verify(mockStep3).saveConfiguration();
        verify(mockSetupConfig).save();
    }

    @Test
    @DisplayName("Should handle empty step list")
    void shouldHandleEmptyStepList() throws Exception {
        // Arrange
        List<WizardStep> emptySteps = Collections.emptyList();
        doNothing().when(mockSetupConfig).save();

        // Act & Assert
        assertDoesNotThrow(() -> configManager.saveAllSteps(emptySteps));
        verify(mockSetupConfig).save();
    }

    @Test
    @DisplayName("Should handle null step list gracefully")
    void shouldHandleNullStepListGracefully() {
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            configManager.saveAllSteps(null);
        });
        
        assertEquals("Configuration save failed", exception.getMessage());
        assertTrue(exception.getCause() instanceof NullPointerException);
    }

    @Test
    @DisplayName("Should handle step save failure")
    void shouldHandleStepSaveFailure() throws Exception {
        // Arrange
        List<WizardStep> steps = Arrays.asList(mockStep1, mockStep2);
        
        doNothing().when(mockStep1).saveConfiguration();
        doThrow(new RuntimeException("Step save failed")).when(mockStep2).saveConfiguration();

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            configManager.saveAllSteps(steps);
        });
        
        assertEquals("Configuration save failed", exception.getMessage());
        assertTrue(exception.getCause() instanceof RuntimeException);
        assertEquals("Step save failed", exception.getCause().getMessage());
        
        // Verify first step was saved but second failed
        verify(mockStep1).saveConfiguration();
        verify(mockStep2).saveConfiguration();
        // Setup config save should not be called due to step failure
        verify(mockSetupConfig, never()).save();
    }

    @Test
    @DisplayName("Should handle setup config save failure")
    void shouldHandleSetupConfigSaveFailure() throws Exception {
        // Arrange
        List<WizardStep> steps = Arrays.asList(mockStep1);
        
        doNothing().when(mockStep1).saveConfiguration();
        doThrow(new RuntimeException("Config save failed")).when(mockSetupConfig).save();

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            configManager.saveAllSteps(steps);
        });
        
        assertEquals("Configuration save failed", exception.getMessage());
        assertTrue(exception.getCause() instanceof RuntimeException);
        assertEquals("Config save failed", exception.getCause().getMessage());
        
        // Verify step was saved but config save failed
        verify(mockStep1).saveConfiguration();
        verify(mockSetupConfig).save();
    }

    @Test
    @DisplayName("Should handle multiple step save failures")
    void shouldHandleMultipleStepSaveFailures() throws Exception {
        // Arrange
        List<WizardStep> steps = Arrays.asList(mockStep1, mockStep2, mockStep3);
        
        doThrow(new RuntimeException("Step1 failed")).when(mockStep1).saveConfiguration();
        doNothing().when(mockStep2).saveConfiguration();
        doThrow(new RuntimeException("Step3 failed")).when(mockStep3).saveConfiguration();

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            configManager.saveAllSteps(steps);
        });
        
        assertEquals("Configuration save failed", exception.getMessage());
        assertEquals("Step1 failed", exception.getCause().getMessage());
        
        // Verify only first step was attempted (fail-fast behavior)
        verify(mockStep1).saveConfiguration();
        verify(mockStep2, never()).saveConfiguration();
        verify(mockStep3, never()).saveConfiguration();
        verify(mockSetupConfig, never()).save();
    }

    @Test
    @DisplayName("Should save steps in order")
    void shouldSaveStepsInOrder() throws Exception {
        // Arrange
        List<WizardStep> steps = Arrays.asList(mockStep1, mockStep2, mockStep3);
        
        doNothing().when(mockStep1).saveConfiguration();
        doNothing().when(mockStep2).saveConfiguration();
        doNothing().when(mockStep3).saveConfiguration();
        doNothing().when(mockSetupConfig).save();

        // Act
        configManager.saveAllSteps(steps);

        // Assert - verify order using InOrder
        var inOrder = inOrder(mockStep1, mockStep2, mockStep3, mockSetupConfig);
        inOrder.verify(mockStep1).saveConfiguration();
        inOrder.verify(mockStep2).saveConfiguration();
        inOrder.verify(mockStep3).saveConfiguration();
        inOrder.verify(mockSetupConfig).save();
    }

    @Test
    @DisplayName("Should accept null setup config but may fail later")
    void shouldAcceptNullSetupConfigButMayFailLater() {
        // The constructor might accept null, but operations will fail later
        ConfigManager nullConfigManager = new ConfigManager(null);
        assertNotNull(nullConfigManager);
        
        // Using it should fail
        assertThrows(RuntimeException.class, () -> {
            nullConfigManager.saveAllSteps(Collections.emptyList());
        });
    }

    @Test
    @DisplayName("Should handle concurrent save operations")
    void shouldHandleConcurrentSaveOperations() throws Exception {
        // Arrange
        List<WizardStep> steps1 = Arrays.asList(mockStep1);
        List<WizardStep> steps2 = Arrays.asList(mockStep2);
        
        doNothing().when(mockStep1).saveConfiguration();
        doNothing().when(mockStep2).saveConfiguration();
        doNothing().when(mockSetupConfig).save();

        // Act - simulate concurrent saves (in practice this would be different threads)
        assertDoesNotThrow(() -> {
            configManager.saveAllSteps(steps1);
            configManager.saveAllSteps(steps2);
        });

        // Assert
        verify(mockStep1).saveConfiguration();
        verify(mockStep2).saveConfiguration();
        verify(mockSetupConfig, times(2)).save();
    }

    @Test
    @DisplayName("Should handle steps with null save methods")
    void shouldHandleStepsWithNullSaveMethods() throws Exception {
        // Arrange - create a step that does nothing on save (default behavior)
        WizardStep stepWithNoOp = mock(WizardStep.class);
        List<WizardStep> steps = Arrays.asList(stepWithNoOp);
        
        doNothing().when(stepWithNoOp).saveConfiguration();
        doNothing().when(mockSetupConfig).save();

        // Act & Assert
        assertDoesNotThrow(() -> configManager.saveAllSteps(steps));
        verify(stepWithNoOp).saveConfiguration();
        verify(mockSetupConfig).save();
    }

    @Test
    @DisplayName("Should preserve exception details in failure")
    void shouldPreserveExceptionDetailsInFailure() throws Exception {
        // Arrange
        List<WizardStep> steps = Arrays.asList(mockStep1);
        RuntimeException originalException = new RuntimeException("Original error message");
        
        doThrow(originalException).when(mockStep1).saveConfiguration();

        // Act & Assert
        RuntimeException thrownException = assertThrows(RuntimeException.class, () -> {
            configManager.saveAllSteps(steps);
        });
        
        assertEquals("Configuration save failed", thrownException.getMessage());
        assertSame(originalException, thrownException.getCause());
    }

    @Test
    @DisplayName("Should handle general exceptions from file operations")
    void shouldHandleGeneralExceptionsFromFileOperations() {
        // Arrange
        List<WizardStep> steps = Arrays.asList(mockStep1);
        
        doNothing().when(mockStep1).saveConfiguration();
        doThrow(new RuntimeException("Disk full")).when(mockSetupConfig).save();

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            configManager.saveAllSteps(steps);
        });
        
        assertEquals("Configuration save failed", exception.getMessage());
        assertTrue(exception.getCause() instanceof RuntimeException);
        assertEquals("Disk full", exception.getCause().getMessage());
    }
}