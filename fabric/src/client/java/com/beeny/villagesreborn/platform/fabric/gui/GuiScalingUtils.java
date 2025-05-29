package com.beeny.villagesreborn.platform.fabric.gui;

import net.minecraft.client.MinecraftClient;

/**
 * Utility class for consistent GUI scaling across all UI components
 */
public class GuiScalingUtils {
    
    /**
     * Gets the current GUI scale factor from Minecraft client
     */
    public static double getGuiScale() {
        return MinecraftClient.getInstance().getWindow().getScaleFactor();
    }
    
    /**
     * Calculates scaled spacing with minimum value
     */
    public static int getScaledSpacing(int originalSpacing) {
        double guiScale = getGuiScale();
        return (int) Math.max(originalSpacing * 0.8, originalSpacing / guiScale * 2);
    }
    
    /**
     * Calculates scaled padding with minimum value
     */
    public static int getScaledPadding(int originalPadding) {
        double guiScale = getGuiScale();
        return (int) Math.max(originalPadding * 0.7, originalPadding / guiScale * 2);
    }
    
    /**
     * Calculates scaled widget height with minimum value
     */
    public static int getScaledWidgetHeight(int originalHeight) {
        double guiScale = getGuiScale();
        return (int) Math.max(originalHeight * 0.8, originalHeight / guiScale * 2);
    }
    
    /**
     * Calculates scaled widget width with maximum and minimum constraints
     */
    public static int getScaledWidgetWidth(int originalWidth, int maxWidth) {
        double guiScale = getGuiScale();
        return Math.min(maxWidth, (int) Math.max(originalWidth * 0.6, originalWidth / guiScale * 2));
    }
    
    /**
     * Calculates adaptive column count based on available width
     */
    public static int getAdaptiveColumnCount(int availableWidth, int widgetWidth, int spacing, int maxColumns) {
        int widgetPlusSpacing = widgetWidth + spacing;
        return Math.max(1, Math.min(maxColumns, availableWidth / widgetPlusSpacing));
    }
    
    /**
     * Calculates centered position for a widget of given width
     */
    public static int getCenteredX(int containerWidth, int widgetWidth) {
        return (containerWidth - widgetWidth) / 2;
    }
    
    /**
     * Calculates scaled button width with container constraints
     */
    public static int getScaledButtonWidth(int originalWidth, int containerWidth, int buttonCount) {
        int maxButtonWidth = Math.min(originalWidth, (containerWidth - 60) / buttonCount);
        return getScaledWidgetWidth(originalWidth, maxButtonWidth);
    }
    
    /**
     * Standard scaled dimensions for common UI elements
     */
    public static class StandardDimensions {
        public static final int WIDGET_HEIGHT = 20;
        public static final int WIDGET_WIDTH = 200;
        public static final int SLIDER_WIDTH = 180;
        public static final int LABEL_HEIGHT = 15;
        public static final int SECTION_SPACING = 25;
        public static final int BUTTON_WIDTH = 100;
        
        public static int getScaledWidgetHeight() {
            return GuiScalingUtils.getScaledWidgetHeight(WIDGET_HEIGHT);
        }
        
        public static int getScaledWidgetWidth() {
            return GuiScalingUtils.getScaledWidgetWidth(WIDGET_WIDTH, WIDGET_WIDTH);
        }
        
        public static int getScaledSliderWidth() {
            return GuiScalingUtils.getScaledWidgetWidth(SLIDER_WIDTH, SLIDER_WIDTH);
        }
        
        public static int getScaledLabelHeight() {
            return GuiScalingUtils.getScaledWidgetHeight(LABEL_HEIGHT);
        }
        
        public static int getScaledSectionSpacing() {
            return GuiScalingUtils.getScaledSpacing(SECTION_SPACING);
        }
        
        public static int getScaledButtonWidth() {
            return GuiScalingUtils.getScaledWidgetWidth(BUTTON_WIDTH, BUTTON_WIDTH);
        }
    }
}