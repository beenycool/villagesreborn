package com.beeny.villagesreborn.platform.fabric.gui;

import com.beeny.villagesreborn.platform.fabric.biome.BiomeDisplayInfo;
import com.beeny.villagesreborn.platform.fabric.biome.BiomeManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Widget for displaying and selecting a biome in the biome selector grid
 * Simplified version for testing compatibility
 */
@Environment(EnvType.CLIENT)
public class BiomeSelectionWidget extends ClickableWidget {
    private static final Logger LOGGER = LoggerFactory.getLogger(BiomeSelectionWidget.class);
    private static final int ICON_SIZE = 32;
    private static final int PADDING = 8;
    
    private final BiomeDisplayInfo biome;
    private final Consumer<BiomeDisplayInfo> onSelected;
    private boolean isSelected = false;
    private final Identifier biomeIcon;
    
    // Enhanced functionality fields
    private boolean creationMode = false;
    private Function<BiomeDisplayInfo, Text[]> tooltipProvider;
    private Consumer<BiomeDisplayInfo> previewHandler;
    private float hoverAnimationProgress = 0.0f;
    
    public BiomeSelectionWidget(BiomeDisplayInfo biome, int width, int height, 
                               Consumer<BiomeDisplayInfo> onSelected) {
        super(0, 0, width, height, biome.getDisplayName());
        this.biome = biome;
        this.onSelected = onSelected;
        this.biomeIcon = BiomeManager.getInstance().getBiomeIcon(biome.getRegistryKey());
    }
    
    
    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        this.appendDefaultNarrations(builder);
    }
    
    private void renderBiomeStats(DrawContext context) {
        int statsY = getY() + height - 15;
        int statsX = getX() + PADDING;
        
        // Temperature indicator (thermometer icon + color)
        int tempColor = BiomeManager.getTemperatureColor(biome.getTemperature());
        context.fill(statsX, statsY, statsX + 20, statsY + 3, tempColor);
        
        // Humidity indicator (water drop icon + color)
        int humidityColor = BiomeManager.getHumidityColor(biome.getHumidity());
        context.fill(statsX + 25, statsY, statsX + 45, statsY + 3, humidityColor);
    }
    
    @Override
    public void onClick(double mouseX, double mouseY) {
        this.onSelected.accept(biome);
    }
    
    public void setSelected(boolean selected) {
        this.isSelected = selected;
    }
    
    public BiomeDisplayInfo getBiome() {
        return biome;
    }
    
    public boolean isSelected() {
        return isSelected;
    }
    
    /**
     * Sets creation mode for enhanced rendering
     */
    public void setCreationMode(boolean isCreationMode) {
        this.creationMode = isCreationMode;
    }
    
    /**
     * Sets tooltip provider for enhanced tooltips
     */
    public void setTooltipProvider(Function<BiomeDisplayInfo, Text[]> provider) {
        this.tooltipProvider = provider;
    }
    
    /**
     * Sets preview handler for hover previews
     */
    public void setPreviewHandler(Consumer<BiomeDisplayInfo> handler) {
        this.previewHandler = handler;
    }
    
    /**
     * Updates hover animation progress
     */
    private void updateHoverAnimation(float delta) {
        float targetProgress = isHovered() ? 1.0f : 0.0f;
        float animationSpeed = 0.1f;
        
        // Simple linear interpolation for animation
        float difference = targetProgress - hoverAnimationProgress;
        hoverAnimationProgress += difference * animationSpeed * delta;
        
        // Clamp to valid range
        if (hoverAnimationProgress < 0.0f) hoverAnimationProgress = 0.0f;
        if (hoverAnimationProgress > 1.0f) hoverAnimationProgress = 1.0f;
    }
    
    /**
     * Renders creation mode indicators
     */
    private void renderCreationModeIndicators(DrawContext context) {
        // Render difficulty indicator using actual difficulty rating
        int difficultyColor = getDifficultyColor(biome.getDifficultyRating());
        context.fill(getX() + width - 8, getY() + 2, getX() + width - 2, getY() + 8, difficultyColor);
        
        // Render resource indicator
        int resourceColor = BiomeManager.getHumidityColor(biome.getHumidity());
        context.fill(getX() + width - 16, getY() + 2, getX() + width - 10, getY() + 8, resourceColor);
    }
    
    /**
     * Gets difficulty color based on difficulty rating
     */
    private int getDifficultyColor(int difficulty) {
        switch (difficulty) {
            case 1: return 0xFF50C878; // Easy - Green
            case 2: return 0xFFFFD700; // Normal - Gold
            case 3: return 0xFFFF8C00; // Hard - Orange
            case 4: return 0xFFFF4500; // Expert - Red
            default: return 0xFF808080; // Unknown - Gray
        }
    }
    
    /**
     * Renders hover effects with animation
     */
    private void renderHoverEffects(DrawContext context) {
        if (hoverAnimationProgress > 0.0f) {
            // Animated border highlight with improved opacity
            int alpha = Math.min(255, (int)(hoverAnimationProgress * 150));
            int highlightColor = 0x64FFFFFF | (alpha << 24);
            context.drawBorder(getX() - 1, getY() - 1, width + 2, height + 2, highlightColor);
        }
    }
    
    /**
     * Enhanced render method with animations and creation mode features
     */
    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        // Update animations
        updateHoverAnimation(delta);
        
        // Background with selection highlight
        int backgroundColor = isSelected ? 0xFF4A90E2 : 0xFF2C2C2C;
        int borderColor = isSelected ? 0xFFFFFFFF : 0xFF666666;
        
        context.fill(getX(), getY(), getX() + width, getY() + height, backgroundColor);
        context.drawBorder(getX(), getY(), width, height, borderColor);
        
        // Render biome name text
        renderBiomeName(context);
        
        // Render biome icon if available
        renderBiomeIcon(context);
        
        // Render biome description
        renderBiomeDescription(context);
        
        // Creation mode enhancements
        if (creationMode) {
            renderCreationModeIndicators(context);
        }
        
        // Hover effects
        if (isHovered()) {
            renderHoverEffects(context);
            if (previewHandler != null) {
                previewHandler.accept(biome);
            }
        }
        
        // Temperature and humidity indicators
        renderBiomeStats(context);
    }
    
    /**
     * Renders the biome name text
     */
    private void renderBiomeName(DrawContext context) {
        Text biomeName = biome.getDisplayName();
        int textColor = isSelected ? 0xFFFFFFFF : 0xFFCCCCCC;
        
        // Calculate text position (centered horizontally, near the top)
        int textX = getX() + (width / 2);
        int textY = getY() + PADDING;
        
        // Draw the text centered
        context.drawCenteredTextWithShadow(
            net.minecraft.client.MinecraftClient.getInstance().textRenderer,
            biomeName,
            textX,
            textY,
            textColor
        );
    }
    
    /**
     * Renders the biome icon
     */
    private void renderBiomeIcon(DrawContext context) {
        // Calculate icon position (centered horizontally, below the text)
        int iconX = getX() + (width - ICON_SIZE) / 2;
        int iconY = getY() + PADDING + 12; // Below the text
        
        // Try to render actual texture icon first
        if (biomeIcon != null && tryRenderTextureIcon(context, iconX, iconY)) {
            return; // Successfully rendered texture icon
        }
        
        // Fallback to colored rectangles based on biome properties
        renderColoredIcon(context, iconX, iconY);
    }
    
    /**
     * Attempts to render the biome texture icon
     * @return true if successful, false if texture not available
     */
    private boolean tryRenderTextureIcon(DrawContext context, int iconX, int iconY) {
        try {
            // For now, we'll skip texture rendering and always fall back to colored icons
            // This avoids texture loading complexity while still providing visual distinction
            // In a full implementation, this would properly load and render biome textures
            return false;
        } catch (Exception e) {
            // Texture not available or failed to load, fall back to colored icon
            LOGGER.debug("Failed to load biome texture {}: {}", biomeIcon, e.getMessage());
        }
        return false;
    }
    
    /**
     * Renders a colored icon as fallback when texture is not available
     */
    private void renderColoredIcon(DrawContext context, int iconX, int iconY) {
        // Use colored rectangles based on biome properties
        int backgroundColor = BiomeManager.getTemperatureColor(biome.getTemperature());
        int borderColor = BiomeManager.getHumidityColor(biome.getHumidity());
        
        // Draw the main icon background
        context.fill(iconX, iconY, iconX + ICON_SIZE, iconY + ICON_SIZE, backgroundColor);
        
        // Draw a border with humidity color
        context.drawBorder(iconX, iconY, ICON_SIZE, ICON_SIZE, borderColor);
        
        // Add a distinctive pattern based on biome type
        renderBiomePattern(context, iconX, iconY, borderColor);
    }
    
    /**
     * Renders a distinctive pattern inside the colored icon based on biome characteristics
     */
    private void renderBiomePattern(DrawContext context, int iconX, int iconY, int patternColor) {
        float temperature = biome.getTemperature();
        float humidity = biome.getHumidity();
        
        // Different patterns for different biome types
        if (temperature < 0.2f) {
            // Cold biomes: snowflake pattern
            renderSnowflakePattern(context, iconX, iconY, patternColor);
        } else if (humidity > 0.8f) {
            // Humid biomes: water droplet pattern
            renderDropletPattern(context, iconX, iconY, patternColor);
        } else if (humidity < 0.3f) {
            // Arid biomes: cactus/desert pattern
            renderDesertPattern(context, iconX, iconY, patternColor);
        } else {
            // Temperate biomes: tree/leaf pattern
            renderTreePattern(context, iconX, iconY, patternColor);
        }
    }
    
    /**
     * Renders a snowflake pattern for cold biomes
     */
    private void renderSnowflakePattern(DrawContext context, int iconX, int iconY, int color) {
        int centerX = iconX + ICON_SIZE / 2;
        int centerY = iconY + ICON_SIZE / 2;
        int size = ICON_SIZE / 6;
        
        // Draw cross pattern
        context.fill(centerX - size, centerY - 1, centerX + size, centerY + 1, color);
        context.fill(centerX - 1, centerY - size, centerX + 1, centerY + size, color);
        
        // Draw diagonal lines
        for (int i = -size/2; i <= size/2; i++) {
            context.fill(centerX + i - 1, centerY + i - 1, centerX + i + 1, centerY + i + 1, color);
            context.fill(centerX + i - 1, centerY - i - 1, centerX + i + 1, centerY - i + 1, color);
        }
    }
    
    /**
     * Renders a water droplet pattern for humid biomes
     */
    private void renderDropletPattern(DrawContext context, int iconX, int iconY, int color) {
        int centerX = iconX + ICON_SIZE / 2;
        int centerY = iconY + ICON_SIZE / 2;
        int size = ICON_SIZE / 4;
        
        // Draw droplet shape (circle with point at bottom)
        context.fill(centerX - size/2, centerY - size/2, centerX + size/2, centerY + size/2, color);
        context.fill(centerX - 1, centerY + size/2, centerX + 1, centerY + size/2 + 2, color);
    }
    
    /**
     * Renders a desert pattern for arid biomes
     */
    private void renderDesertPattern(DrawContext context, int iconX, int iconY, int color) {
        int centerX = iconX + ICON_SIZE / 2;
        int centerY = iconY + ICON_SIZE / 2;
        int size = ICON_SIZE / 6;
        
        // Draw cactus-like vertical lines
        context.fill(centerX - 1, centerY - size, centerX + 1, centerY + size, color);
        context.fill(centerX - size/2 - 1, centerY - 1, centerX - size/2 + 1, centerY + 1, color);
        context.fill(centerX + size/2 - 1, centerY - 1, centerX + size/2 + 1, centerY + 1, color);
    }
    
    /**
     * Renders a tree pattern for temperate biomes
     */
    private void renderTreePattern(DrawContext context, int iconX, int iconY, int color) {
        int centerX = iconX + ICON_SIZE / 2;
        int centerY = iconY + ICON_SIZE / 2;
        int size = ICON_SIZE / 6;
        
        // Draw tree trunk
        context.fill(centerX - 1, centerY, centerX + 1, centerY + size, color);
        
        // Draw tree crown (simple diamond shape)
        for (int i = 0; i < size/2; i++) {
            context.fill(centerX - i - 1, centerY - size/2 + i - 1, centerX + i + 1, centerY - size/2 + i + 1, color);
        }
    }
    
    /**
     * Renders the biome description text
     */
    private void renderBiomeDescription(DrawContext context) {
        // Only render description if there's enough space
        if (height > 60) {
            String descText = biome.getDescription().getString();
            // Truncate if too long
            if (descText.length() > 20) {
                descText = descText.substring(0, 17) + "...";
            }
            
            int textColor = isSelected ? 0xFFCCCCCC : 0xFF999999;
            int textX = getX() + (width / 2);
            int textY = getY() + height - 15; // Near the bottom
            
            // Draw the description text centered
            context.drawCenteredTextWithShadow(
                net.minecraft.client.MinecraftClient.getInstance().textRenderer,
                Text.literal(descText),
                textX,
                textY,
                textColor
            );
        }
    }
    
    /**
     * Renders enhanced tooltips if provider is set
     */
    public void renderTooltip(DrawContext context, int mouseX, int mouseY) {
        if (tooltipProvider != null && isHovered()) {
            Text[] tooltipLines = tooltipProvider.apply(biome);
            if (tooltipLines != null && tooltipLines.length > 0) {
                // Render the tooltip using Minecraft's built-in tooltip system
                java.util.List<Text> tooltipList = java.util.Arrays.asList(tooltipLines);
                context.drawTooltip(
                    net.minecraft.client.MinecraftClient.getInstance().textRenderer,
                    tooltipList,
                    mouseX,
                    mouseY
                );
            }
        } else if (isHovered()) {
            // Render default tooltip with biome information
            java.util.List<Text> defaultTooltip = createDefaultTooltip();
            context.drawTooltip(
                net.minecraft.client.MinecraftClient.getInstance().textRenderer,
                defaultTooltip,
                mouseX,
                mouseY
            );
        }
    }
    
    /**
     * Creates a default tooltip with biome information
     */
    private java.util.List<Text> createDefaultTooltip() {
        java.util.List<Text> tooltip = new java.util.ArrayList<>();
        
        // Biome name
        tooltip.add(biome.getDisplayName());
        
        // Temperature info
        String tempDesc = getTemperatureDescription(biome.getTemperature());
        tooltip.add(Text.literal("Temperature: " + tempDesc).formatted(net.minecraft.util.Formatting.GRAY));
        
        // Humidity info
        String humidityDesc = getHumidityDescription(biome.getHumidity());
        tooltip.add(Text.literal("Humidity: " + humidityDesc).formatted(net.minecraft.util.Formatting.GRAY));
        
        // Difficulty info
        String difficultyDesc = getDifficultyDescription(biome.getDifficultyRating());
        tooltip.add(Text.literal("Difficulty: " + difficultyDesc).formatted(net.minecraft.util.Formatting.YELLOW));
        
        // Description
        tooltip.add(Text.literal("").formatted(net.minecraft.util.Formatting.RESET)); // Empty line
        tooltip.add(biome.getDescription().copy().formatted(net.minecraft.util.Formatting.ITALIC));
        
        return tooltip;
    }
    
    /**
     * Gets a human-readable temperature description
     */
    private String getTemperatureDescription(float temperature) {
        if (temperature < 0.0f) return "Freezing";
        if (temperature < 0.3f) return "Cold";
        if (temperature < 0.7f) return "Moderate";
        if (temperature < 1.2f) return "Warm";
        return "Hot";
    }
    
    /**
     * Gets a human-readable humidity description
     */
    private String getHumidityDescription(float humidity) {
        if (humidity < 0.2f) return "Arid";
        if (humidity < 0.5f) return "Dry";
        if (humidity < 0.8f) return "Moderate";
        return "Humid";
    }
    
    /**
     * Gets a human-readable difficulty description
     */
    private String getDifficultyDescription(int difficulty) {
        switch (difficulty) {
            case 1: return "Easy";
            case 2: return "Normal";
            case 3: return "Hard";
            case 4: return "Expert";
            default: return "Unknown";
        }
    }
}