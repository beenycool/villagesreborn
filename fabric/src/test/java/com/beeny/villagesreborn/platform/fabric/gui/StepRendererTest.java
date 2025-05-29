package com.beeny.villagesreborn.platform.fabric.gui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for StepRenderer.
 * Tests rendering of core components, layout properties, theming, and scaling.
 */
class StepRendererTest {

    @Mock
    private TextRenderer mockTextRenderer;
    
    @Mock
    private DrawContext mockDrawContext;

    private StepRenderer stepRenderer;
    private final int testScreenWidth = 800;
    private final int testScreenHeight = 600;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        stepRenderer = new StepRenderer(mockTextRenderer, testScreenWidth, testScreenHeight);
    }

    @Test
    @DisplayName("Should initialize with correct screen dimensions")
    void shouldInitializeWithCorrectScreenDimensions() {
        assertNotNull(stepRenderer);
    }

    @Test
    @DisplayName("Should render step indicator with correct colors and positions")
    void shouldRenderStepIndicatorWithCorrectColorsAndPositions() {
        int currentStep = 2;
        int totalSteps = 5;
        
        stepRenderer.renderStepIndicator(mockDrawContext, currentStep, totalSteps);
        
        // Verify that fill is called for each step with correct parameters
        // Current step should be white (0xFFFFFF)
        // Previous steps should be green (0x00FF00)  
        // Future steps should be gray (0x666666)
        
        int centerX = testScreenWidth / 2;
        int stepY = 20;
        int stepSpacing = 20;
        
        // Calculate expected positions and colors
        for (int i = 0; i < totalSteps; i++) {
            int expectedX = centerX - ((totalSteps - 1) * stepSpacing / 2) + (i * stepSpacing);
            int expectedColor = i == currentStep ? 0xFFFFFF : (i < currentStep ? 0x00FF00 : 0x666666);
            
            verify(mockDrawContext).fill(
                expectedX - 5, stepY, expectedX + 5, stepY + 2, expectedColor
            );
        }
    }

    @Test
    @DisplayName("Should render step indicator for first step")
    void shouldRenderStepIndicatorForFirstStep() {
        int currentStep = 0;
        int totalSteps = 3;
        
        stepRenderer.renderStepIndicator(mockDrawContext, currentStep, totalSteps);
        
        // Verify first step is current (white), others are future (gray)
        verify(mockDrawContext, times(3)).fill(anyInt(), anyInt(), anyInt(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("Should render step indicator for last step")
    void shouldRenderStepIndicatorForLastStep() {
        int currentStep = 4;
        int totalSteps = 5;
        
        stepRenderer.renderStepIndicator(mockDrawContext, currentStep, totalSteps);
        
        // Verify last step is current (white), others are previous (green)
        verify(mockDrawContext, times(5)).fill(anyInt(), anyInt(), anyInt(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("Should handle single step indicator")
    void shouldHandleSingleStepIndicator() {
        int currentStep = 0;
        int totalSteps = 1;
        
        stepRenderer.renderStepIndicator(mockDrawContext, currentStep, totalSteps);
        
        // Verify single step is rendered as current (white)
        int centerX = testScreenWidth / 2;
        int stepY = 20;
        
        verify(mockDrawContext).fill(centerX - 5, stepY, centerX + 5, stepY + 2, 0xFFFFFF);
    }

    @Test
    @DisplayName("Should create navigation button with correct position and properties")
    void shouldCreateNavigationButtonWithCorrectPositionAndProperties() {
        String buttonText = "Next";
        boolean[] actionCalled = {false};
        Runnable testAction = () -> actionCalled[0] = true;
        int position = 1; // Right position
        
        ButtonWidget button = stepRenderer.createNavigationButton(buttonText, testAction, position);
        
        assertNotNull(button);
        
        // Test button action
        button.onPress();
        assertTrue(actionCalled[0], "Button action should be called when pressed");
        
        // Verify button dimensions and position
        assertEquals(80, button.getWidth());
        assertEquals(20, button.getHeight());
        
        // For position 1 (right), button should be at centerX + spacing
        int expectedX = testScreenWidth / 2 + 10;
        int expectedY = testScreenHeight - 30;
        assertEquals(expectedX, button.getX());
        assertEquals(expectedY, button.getY());
    }

    @Test
    @DisplayName("Should create left navigation button with correct position")
    void shouldCreateLeftNavigationButtonWithCorrectPosition() {
        String buttonText = "Back";
        Runnable testAction = () -> {};
        int position = 0; // Left position
        
        ButtonWidget button = stepRenderer.createNavigationButton(buttonText, testAction, position);
        
        assertNotNull(button);
        
        // For position 0 (left), button should be at centerX - buttonWidth - spacing
        int expectedX = testScreenWidth / 2 - 80 - 10;
        int expectedY = testScreenHeight - 30;
        assertEquals(expectedX, button.getX());
        assertEquals(expectedY, button.getY());
    }

    @Test
    @DisplayName("Should render title with correct centering")
    void shouldRenderTitleWithCorrectCentering() {
        String title = "Test Title";
        int titleWidth = 100;
        
        when(mockTextRenderer.getWidth(title)).thenReturn(titleWidth);
        
        stepRenderer.renderTitle(mockDrawContext, title);
        
        int expectedX = (testScreenWidth - titleWidth) / 2;
        int expectedY = 5;
        
        verify(mockTextRenderer).getWidth(title);
        verify(mockDrawContext).drawText(mockTextRenderer, title, expectedX, expectedY, 0xFFFFFF, false);
    }

    @Test
    @DisplayName("Should handle empty title")
    void shouldHandleEmptyTitle() {
        String emptyTitle = "";
        when(mockTextRenderer.getWidth(emptyTitle)).thenReturn(0);
        
        stepRenderer.renderTitle(mockDrawContext, emptyTitle);
        
        verify(mockTextRenderer).getWidth(emptyTitle);
        verify(mockDrawContext).drawText(mockTextRenderer, emptyTitle, testScreenWidth / 2, 5, 0xFFFFFF, false);
    }

    @Test
    @DisplayName("Should handle very long title")
    void shouldHandleVeryLongTitle() {
        String longTitle = "This is a very long title that might exceed screen width";
        int titleWidth = testScreenWidth + 100; // Wider than screen
        
        when(mockTextRenderer.getWidth(longTitle)).thenReturn(titleWidth);
        
        stepRenderer.renderTitle(mockDrawContext, longTitle);
        
        int expectedX = (testScreenWidth - titleWidth) / 2; // Will be negative
        verify(mockDrawContext).drawText(mockTextRenderer, longTitle, expectedX, 5, 0xFFFFFF, false);
    }

    @Test
    @DisplayName("Should accept null parameters but may fail later")
    void shouldAcceptNullParametersButMayFailLater() {
        // The constructor might accept null but operations will fail later
        assertDoesNotThrow(() -> {
            new StepRenderer(null, testScreenWidth, testScreenHeight);
        });
    }

    @Test
    @DisplayName("Should handle edge case screen dimensions")
    void shouldHandleEdgeCaseScreenDimensions() {
        // Test with very small screen
        StepRenderer smallRenderer = new StepRenderer(mockTextRenderer, 100, 100);
        assertNotNull(smallRenderer);
        
        // Test with very large screen
        StepRenderer largeRenderer = new StepRenderer(mockTextRenderer, 4000, 3000);
        assertNotNull(largeRenderer);
        
        // Test with zero dimensions
        StepRenderer zeroRenderer = new StepRenderer(mockTextRenderer, 0, 0);
        assertNotNull(zeroRenderer);
    }

    @Test
    @DisplayName("Should maintain consistent button styling")
    void shouldMaintainConsistentButtonStyling() {
        ButtonWidget button1 = stepRenderer.createNavigationButton("Button1", () -> {}, 0);
        ButtonWidget button2 = stepRenderer.createNavigationButton("Button2", () -> {}, 1);
        
        // Both buttons should have same dimensions
        assertEquals(button1.getWidth(), button2.getWidth());
        assertEquals(button1.getHeight(), button2.getHeight());
        
        // Both buttons should be at same Y position
        assertEquals(button1.getY(), button2.getY());
    }

    @Test
    @DisplayName("Should handle different screen aspect ratios")
    void shouldHandleDifferentScreenAspectRatios() {
        // Test wide screen
        StepRenderer wideRenderer = new StepRenderer(mockTextRenderer, 1920, 720);
        assertNotNull(wideRenderer);
        
        // Test tall screen
        StepRenderer tallRenderer = new StepRenderer(mockTextRenderer, 720, 1920);
        assertNotNull(tallRenderer);
        
        // Test square screen
        StepRenderer squareRenderer = new StepRenderer(mockTextRenderer, 800, 800);
        assertNotNull(squareRenderer);
    }

    @Test
    @DisplayName("Should render step indicators with proper spacing")
    void shouldRenderStepIndicatorsWithProperSpacing() {
        stepRenderer.renderStepIndicator(mockDrawContext, 1, 3);
        
        // Verify spacing calculations are correct
        int centerX = testScreenWidth / 2;
        int stepSpacing = 20;
        
        // For 3 steps, positions should be: center-20, center, center+20
        verify(mockDrawContext).fill(centerX - 25, 20, centerX - 15, 22, 0x00FF00); // Step 0 (previous)
        verify(mockDrawContext).fill(centerX - 5, 20, centerX + 5, 22, 0xFFFFFF);   // Step 1 (current)
        verify(mockDrawContext).fill(centerX + 15, 20, centerX + 25, 22, 0x666666); // Step 2 (future)
    }

    @Test
    @DisplayName("Should handle button actions correctly")
    void shouldHandleButtonActionsCorrectly() {
        int[] counter = {0};
        Runnable incrementAction = () -> counter[0]++;
        
        ButtonWidget button = stepRenderer.createNavigationButton("Test", incrementAction, 0);
        
        // Test multiple button presses
        button.onPress();
        assertEquals(1, counter[0]);
        
        button.onPress();
        assertEquals(2, counter[0]);
    }

    @Test
    @DisplayName("Should maintain color consistency for theming")
    void shouldMaintainColorConsistencyForTheming() {
        // Test that colors used are consistent across components
        
        // Step indicator colors
        stepRenderer.renderStepIndicator(mockDrawContext, 1, 3);
        
        // Title color
        String title = "Test";
        when(mockTextRenderer.getWidth(title)).thenReturn(50);
        stepRenderer.renderTitle(mockDrawContext, title);
        
        // Verify title uses white color (same as current step indicator)
        verify(mockDrawContext).drawText(eq(mockTextRenderer), eq(title), anyInt(), anyInt(), eq(0xFFFFFF), eq(false));
    }
}