package com.beeny.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;
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
     * Renders the village info HUD if it's currently visible.
     * 
     * @param matrices The matrix stack
     * @param client The Minecraft client instance
     */
    public void render(MatrixStack matrices, MinecraftClient client) {
        // Auto-hide HUD after duration expires
        if (System.currentTimeMillis() > displayEndTime) {
            showHud = false;
        }
        
        // Only render if there's info to show
        if (!showHud || cultureName.isEmpty()) {
            return;
        }
        
        TextRenderer textRenderer = client.textRenderer;
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        
        // Position in the top-right corner with some padding
        int xPos = screenWidth - 150;
        int yPos = 10;
        int lineHeight = textRenderer.fontHeight + 2;
        
        // Semi-transparent background
        fill(matrices, xPos - 5, yPos - 5, xPos + 145, yPos + (lineHeight * 4) + 5, 0x80000000);
        
        // Village name/culture with color based on prosperity
        Formatting prosperityColor = getColorForValue(prosperity);
        textRenderer.drawWithShadow(matrices, 
            Text.literal("Village: ").append(Text.literal(cultureName).formatted(prosperityColor)),
            xPos, yPos, 0xFFFFFF);
        
        // Prosperity indicator
        textRenderer.drawWithShadow(matrices, 
            Text.literal("Prosperity: ").append(Text.literal(getBarString(prosperity)).formatted(prosperityColor)),
            xPos, yPos + lineHeight, 0xFFFFFF);
        
        // Safety indicator with color
        Formatting safetyColor = getColorForValue(safety);
        textRenderer.drawWithShadow(matrices, 
            Text.literal("Safety: ").append(Text.literal(getBarString(safety)).formatted(safetyColor)),
            xPos, yPos + lineHeight * 2, 0xFFFFFF);
        
        // Population count
        textRenderer.drawWithShadow(matrices, 
            Text.literal("Population: ").append(Text.literal(String.valueOf(population))),
            xPos, yPos + lineHeight * 3, 0xFFFFFF);
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
    
    /**
     * Fill a rectangle with color (helper method).
     */
    private void fill(MatrixStack matrices, int startX, int startY, int endX, int endY, int color) {
        int minX = Math.min(startX, endX);
        int minY = Math.min(startY, endY);
        int maxX = Math.max(startX, endX);
        int maxY = Math.max(startY, endY);
        
        matrices.push();
        net.minecraft.client.render.Tessellator tessellator = net.minecraft.client.render.Tessellator.getInstance();
        net.minecraft.client.render.BufferBuilder bufferBuilder = tessellator.getBuffer();
        
        net.minecraft.client.render.RenderSystem.enableBlend();
        net.minecraft.client.render.RenderSystem.disableTexture();
        net.minecraft.client.render.RenderSystem.defaultBlendFunc();
        
        bufferBuilder.begin(net.minecraft.client.render.VertexFormat.DrawMode.QUADS, net.minecraft.client.render.VertexFormats.POSITION_COLOR);
        
        float a = (color >> 24 & 255) / 255.0F;
        float r = (color >> 16 & 255) / 255.0F;
        float g = (color >> 8 & 255) / 255.0F;
        float b = (color & 255) / 255.0F;
        
        bufferBuilder.vertex(minX, maxY, 0).color(r, g, b, a).next();
        bufferBuilder.vertex(maxX, maxY, 0).color(r, g, b, a).next();
        bufferBuilder.vertex(maxX, minY, 0).color(r, g, b, a).next();
        bufferBuilder.vertex(minX, minY, 0).color(r, g, b, a).next();
        
        tessellator.draw();
        net.minecraft.client.render.RenderSystem.enableTexture();
        net.minecraft.client.render.RenderSystem.disableBlend();
        
        matrices.pop();
    }
}