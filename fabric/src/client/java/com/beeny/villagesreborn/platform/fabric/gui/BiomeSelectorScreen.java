package com.beeny.villagesreborn.platform.fabric.gui;

import com.beeny.villagesreborn.platform.fabric.biome.BiomeDisplayInfo;
import com.beeny.villagesreborn.platform.fabric.biome.BiomeManager;
import com.beeny.villagesreborn.platform.fabric.spawn.SpawnBiomeChoiceData;
import com.beeny.villagesreborn.platform.fabric.spawn.SpawnPointManager;
import com.beeny.villagesreborn.platform.fabric.spawn.VillagesRebornWorldSettingsExtensions;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Screen for selecting spawn biome when joining a new world
 * Simplified version for testing compatibility
 */
@Environment(EnvType.CLIENT)
public class BiomeSelectorScreen extends Screen {
    private static final Logger LOGGER = LoggerFactory.getLogger(BiomeSelectorScreen.class);
    
    private final MinecraftClient client;
    private final List<BiomeDisplayInfo> availableBiomes;
    private final List<BiomeSelectionWidget> biomeWidgets = new ArrayList<>();
    private BiomeDisplayInfo selectedBiome;
    private ButtonWidget confirmButton;
    private ButtonWidget randomButton;
    
    public BiomeSelectorScreen() {
        super(Text.translatable("villagesreborn.biome_selector.title"));
        this.client = MinecraftClient.getInstance();
        this.availableBiomes = BiomeManager.getInstance().getSelectableBiomes();
    }
    
    public static BiomeSelectorScreen create() {
        return new BiomeSelectorScreen();
    }
    
    @Override
    protected void init() {
        // Control buttons
        initializeControlButtons();
        
        // Main biome selection area
        initializeBiomeWidgets();
        
        // Pre-select a recommended biome
        selectRecommendedBiome();
    }
    
    private void initializeBiomeWidgets() {
        biomeWidgets.clear();
        
        int startX = 20;
        int startY = 60;
        int widgetWidth = 120;
        int widgetHeight = 80;
        int columns = 4;
        int padding = 10;
        
        for (int i = 0; i < availableBiomes.size(); i++) {
            BiomeDisplayInfo biome = availableBiomes.get(i);
            
            int row = i / columns;
            int col = i % columns;
            
            int x = startX + col * (widgetWidth + padding);
            int y = startY + row * (widgetHeight + padding);
            
            BiomeSelectionWidget widget = new BiomeSelectionWidget(
                biome, widgetWidth, widgetHeight, this::onBiomeSelected
            );
            widget.setX(x);
            widget.setY(y);
            
            biomeWidgets.add(widget);
            this.addDrawableChild(widget);
        }
    }
    
    private void initializeControlButtons() {
        // Confirm button
        this.confirmButton = ButtonWidget.builder(
            Text.translatable("villagesreborn.biome_selector.confirm"),
            button -> confirmBiomeSelection()
        )
        .position(this.width / 2 - 100, this.height - 50)
        .size(95, 20)
        .build();
        
        // Random selection button
        this.randomButton = ButtonWidget.builder(
            Text.translatable("villagesreborn.biome_selector.random"),
            button -> selectRandomBiome()
        )
        .position(this.width / 2 + 5, this.height - 50)
        .size(95, 20)
        .build();
        
        this.addDrawableChild(confirmButton);
        this.addDrawableChild(randomButton);
        
        // Disable confirm until biome is selected
        this.confirmButton.active = false;
    }
    
    private void onBiomeSelected(BiomeDisplayInfo biome) {
        this.selectedBiome = biome;
        this.confirmButton.active = (biome != null);
        
        // Update visual selection state
        updateBiomeSelectionVisuals();
        
        if (biome != null) {
            LOGGER.debug("Selected biome: {}", biome.getRegistryKey().getValue());
        }
    }
    
    private void selectRecommendedBiome() {
        BiomeDisplayInfo recommended = BiomeManager.getInstance().getRecommendedSpawnBiome();
        if (recommended != null) {
            onBiomeSelected(recommended);
        }
    }
    
    private void selectRandomBiome() {
        if (!availableBiomes.isEmpty()) {
            BiomeDisplayInfo random = availableBiomes.get(
                (int) (Math.random() * availableBiomes.size())
            );
            onBiomeSelected(random);
        }
    }
    
    private void confirmBiomeSelection() {
        if (selectedBiome == null) return;
        
        LOGGER.info("Player confirmed biome selection: {}", 
                   selectedBiome.getRegistryKey().getValue());
        
        // Store selection in world data
        SpawnBiomeChoiceData choiceData = new SpawnBiomeChoiceData(
            selectedBiome.getRegistryKey(),
            System.currentTimeMillis()
        );
        
        VillagesRebornWorldSettingsExtensions.setSpawnBiomeChoice(choiceData);
        
        // Request teleportation (simplified for testing)
        if (client.player != null) {
            SpawnPointManager.getInstance().teleportToSpawnBiome(
                client.player, selectedBiome.getRegistryKey()
            );
        }
        
        // Close screen
        this.close();
    }
    
    private void updateBiomeSelectionVisuals() {
        // Update widget selection states
        for (BiomeSelectionWidget widget : biomeWidgets) {
            widget.setSelected(widget.getBiome().equals(selectedBiome));
        }
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Render background
        this.renderBackground(context, mouseX, mouseY, delta);
        
        // Render title
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, 
            this.width / 2, 20, 0xFFFFFF);
        
        // Render description
        Text description = Text.translatable("villagesreborn.biome_selector.description");
        context.drawCenteredTextWithShadow(this.textRenderer, description, 
            this.width / 2, 40, 0xCCCCCC);
        
        // Render widgets and buttons
        super.render(context, mouseX, mouseY, delta);
    }
    
    @Override
    public void close() {
        this.client.setScreen(null);
    }
    
    @Override
    public boolean shouldPause() {
        return false; // Don't pause the game
    }
    
    // Getters for testing
    public List<BiomeDisplayInfo> getAvailableBiomes() {
        return availableBiomes;
    }
    
    public BiomeDisplayInfo getSelectedBiome() {
        return selectedBiome;
    }
    
    public boolean isConfirmButtonActive() {
        return confirmButton != null && confirmButton.active;
    }
    
    public List<BiomeSelectionWidget> getBiomeWidgets() {
        return biomeWidgets;
    }
    
    // Test methods
    public void testInit() {
        this.width = 800;
        this.height = 600;
        init();
    }
    
    public void testOnBiomeSelected(BiomeDisplayInfo biome) {
        onBiomeSelected(biome);
    }
    
    public void testSelectRandomBiome() {
        selectRandomBiome();
    }
}