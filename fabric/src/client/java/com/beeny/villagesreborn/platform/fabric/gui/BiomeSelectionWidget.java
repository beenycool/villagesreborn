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
        // Render difficulty indicator
        int difficultyColor = getDifficultyColor(biome.getTemperature()); // Use temperature as difficulty proxy
        context.fill(getX() + width - 8, getY() + 2, getX() + width - 2, getY() + 8, difficultyColor);
        
        // Render resource indicator
        int resourceColor = BiomeManager.getHumidityColor(biome.getHumidity());
        context.fill(getX() + width - 16, getY() + 2, getX() + width - 10, getY() + 8, resourceColor);
    }
    
    /**
     * Gets difficulty color based on temperature
     */
    private int getDifficultyColor(float temperature) {
        if (temperature < 0.2f) return 0xFF4A90E2; // Cold - Blue
        if (temperature < 0.8f) return 0xFF50C878; // Moderate - Green
        return 0xFFFF6B6B; // Hot - Red
    }
    
    /**
     * Renders hover effects with animation
     */
    private void renderHoverEffects(DrawContext context) {
        if (hoverAnimationProgress > 0.0f) {
            // Animated border highlight
            int alpha = (int)(hoverAnimationProgress * 100);
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
     * Renders enhanced tooltips if provider is set
     */
    public void renderTooltip(DrawContext context, int mouseX, int mouseY) {
        if (tooltipProvider != null && isHovered()) {
            Text[] tooltipLines = tooltipProvider.apply(biome);
            if (tooltipLines != null && tooltipLines.length > 0) {
                // In a full implementation, this would render the tooltip
                // For now, we'll just log it
                LOGGER.debug("Would show tooltip for biome: {}", biome.getRegistryKey());
            }
        }
    }
}