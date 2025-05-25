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

import java.util.function.Consumer;

/**
 * Widget for displaying and selecting a biome in the biome selector grid
 * Simplified version for testing compatibility
 */
@Environment(EnvType.CLIENT)
public class BiomeSelectionWidget extends ClickableWidget {
    private static final int ICON_SIZE = 32;
    private static final int PADDING = 8;
    
    private final BiomeDisplayInfo biome;
    private final Consumer<BiomeDisplayInfo> onSelected;
    private boolean isSelected = false;
    private final Identifier biomeIcon;
    
    public BiomeSelectionWidget(BiomeDisplayInfo biome, int width, int height, 
                               Consumer<BiomeDisplayInfo> onSelected) {
        super(0, 0, width, height, biome.getDisplayName());
        this.biome = biome;
        this.onSelected = onSelected;
        this.biomeIcon = BiomeManager.getInstance().getBiomeIcon(biome.getRegistryKey());
    }
    
    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        // Background with selection highlight
        int backgroundColor = isSelected ? 0xFF4A90E2 : 0xFF2C2C2C;
        int borderColor = isSelected ? 0xFFFFFFFF : 0xFF666666;
        
        context.fill(getX(), getY(), getX() + width, getY() + height, backgroundColor);
        context.drawBorder(getX(), getY(), width, height, borderColor);
        
        // Biome name
        Text biomeName = biome.getDisplayName();
        int textColor = isSelected ? 0xFFFFFFFF : 0xFFAAAAAA;
        
        // Simplified text rendering without textRenderer dependency
        // In a real implementation, this would use proper text rendering
        
        // Temperature and humidity indicators
        renderBiomeStats(context);
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
}