package com.beeny.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Formatting;
import net.minecraft.text.Text;

/**
 * Renders information about the current village on the player's HUD.
 */
public class VillageInfoHud {
    private String cultureName = "";
    private int prosperity = 0;
    private int safety = 0;
    private int population = 0;
    private boolean showHud = false;
    private long displayEndTime = 0;
    private static final long DISPLAY_DURATION = 5000; // Show for 5 seconds after update
    private boolean forcedDisplay = false;
    
    /**
     * Updates the village information to be displayed.
     * 
     * @param cultureName The name of the village culture
     * @param prosperity The prosperity value (0-100)
     * @param safety The safety value (0-100)
     * @param population The current population count
     */
    public void update(String cultureName, int prosperity, int safety, int population) {
        this.cultureName = cultureName;
        this.prosperity = prosperity;
        this.safety = safety;
        this.population = population;
        
        // Show the HUD for a few seconds after update
        this.showHud = true;
        this.displayEndTime = System.currentTimeMillis() + DISPLAY_DURATION;
    }
    
    /**
     * Force the HUD to display for a specific duration, regardless of auto-hide settings.
     * 
     * @param durationMillis Duration to show in milliseconds
     */
    public void forceDisplay(long durationMillis) {
        this.showHud = true;
        this.forcedDisplay = true;
        this.displayEndTime = System.currentTimeMillis() + durationMillis;
    }
    
    /**
     * Renders the village info HUD if it's currently visible.
     * 
     * @param context The draw context
     * @param client The Minecraft client instance
     */
    public void render(DrawContext context, MinecraftClient client) {
        long currentTime = System.currentTimeMillis();
        
        // Auto-hide HUD after duration expires
        if (currentTime > displayEndTime) {
            showHud = false;
            forcedDisplay = false;
        }
        
        // Only render if there's info to show
        if (!showHud || cultureName.isEmpty()) {
            return;
        }
        
        TextRenderer textRenderer = client.textRenderer;
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        
        // Calculate animation factor for smooth transitions
        float animationFactor = calculateAnimationFactor(currentTime);
        
        // Position in the top-right corner with some padding
        int xPos = screenWidth - 150;
        int baseYPos = 10;
        int yPos = baseYPos - (int)((1.0f - animationFactor) * 50); // Slide down animation
        int lineHeight = textRenderer.fontHeight + 2;
        
        // Background opacity based on animation
        int bgAlpha = (int)(128 * animationFactor);
        int backgroundColor = (bgAlpha << 24); // ARGB: alpha, 0, 0, 0
        int borderColor = ((int)(animationFactor * 255) << 24) | (68 << 16) | (68 << 8) | 68; // ARGB
        
        // Semi-transparent background with border
        context.fill(xPos - 6, yPos - 6, xPos + 146, yPos + (lineHeight * 4) + 6, borderColor);
        context.fill(xPos - 5, yPos - 5, xPos + 145, yPos + (lineHeight * 4) + 5, backgroundColor);
        
        // Village name/culture with color based on prosperity
        Formatting prosperityColor = getColorForValue(prosperity);
        context.drawTextWithShadow(textRenderer, 
            Text.literal("Village: ").append(Text.literal(cultureName).formatted(prosperityColor)),
            xPos, yPos, 
            0xFF_FFFFFF | ((int)(animationFactor * 255) << 24));
        
        // Prosperity indicator
        context.drawTextWithShadow(textRenderer, 
            Text.literal("Prosperity: ").append(Text.literal(getBarString(prosperity)).formatted(prosperityColor)),
            xPos, yPos + lineHeight, 
            0xFF_FFFFFF | ((int)(animationFactor * 255) << 24));
        
        // Safety indicator with color
        Formatting safetyColor = getColorForValue(safety);
        context.drawTextWithShadow(textRenderer, 
            Text.literal("Safety: ").append(Text.literal(getBarString(safety)).formatted(safetyColor)),
            xPos, yPos + lineHeight * 2, 
            0xFF_FFFFFF | ((int)(animationFactor * 255) << 24));
        
        // Population count
        context.drawTextWithShadow(textRenderer, 
            Text.literal("Population: ").append(Text.literal(String.valueOf(population))),
            xPos, yPos + lineHeight * 3, 
            0xFF_FFFFFF | ((int)(animationFactor * 255) << 24));
    }
    
    /**
     * Calculate animation factor (0.0 to 1.0) for smooth fade in/out
     */
    private float calculateAnimationFactor(long currentTime) {
        // Time remaining until hide
        long timeRemaining = displayEndTime - currentTime;
        
        // If forced display, maintain full visibility until near the end
        if (forcedDisplay && timeRemaining > 500) {
            return 1.0f;
        }
        
        // During the first 500ms, fade in
        if (displayEndTime - DISPLAY_DURATION + 500 > currentTime) {
            long elapsed = currentTime - (displayEndTime - DISPLAY_DURATION);
            return Math.min(1.0f, elapsed / 500.0f);
        }
        
        // During the last 500ms, fade out
        if (timeRemaining < 500) {
            return Math.max(0.0f, timeRemaining / 500.0f);
        }
        
        // Otherwise fully visible
        return 1.0f;
    }
    
    /**
     * Creates a visual bar representation of a value between 0-100.
     * 
     * @param value The value to represent (0-100)
     * @return A string containing filled and empty characters
     */
    private String getBarString(int value) {
        int filledBars = value / 10;
        StringBuilder bar = new StringBuilder();
        
        for (int i = 0; i < 10; i++) {
            bar.append(i < filledBars ? "■" : "□");
        }
        
        return bar.toString();
    }
    
    /**
     * Returns an appropriate color formatting based on the value.
     * 
     * @param value The value (0-100)
     * @return Color formatting from red (low) to green (high)
     */
    private Formatting getColorForValue(int value) {
        if (value < 30) return Formatting.RED;
        if (value < 60) return Formatting.YELLOW;
        return Formatting.GREEN;
    }
}