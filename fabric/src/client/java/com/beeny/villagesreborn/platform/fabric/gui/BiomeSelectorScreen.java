package com.beeny.villagesreborn.platform.fabric.gui;

import com.beeny.villagesreborn.core.world.WorldCreationSettingsCapture;
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
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Screen for selecting spawn biome when joining a new world
 * Simplified version for testing compatibility
 */
@Environment(EnvType.CLIENT)
public class BiomeSelectorScreen extends Screen {
    private static final Logger LOGGER = LoggerFactory.getLogger(BiomeSelectorScreen.class);
    
    public enum WorldCreationMode {
        WORLD_CREATION,    // Called from Create World screen
        POST_JOIN          // Called after joining world
    }
    
    private final MinecraftClient client;
    private final List<BiomeDisplayInfo> availableBiomes;
    private final List<BiomeSelectionWidget> biomeWidgets = new ArrayList<>();
    private BiomeDisplayInfo selectedBiome;
    private ButtonWidget confirmButton;
    private ButtonWidget randomButton;
    private ButtonWidget cancelButton;
    
    // World creation mode fields
    private WorldCreationMode creationMode = WorldCreationMode.POST_JOIN;
    private CreateWorldScreen parentScreen;
    private Function<BiomeDisplayInfo, Text[]> tooltipProvider;
    private Consumer<BiomeDisplayInfo> previewHandler;
    
    public BiomeSelectorScreen() {
        super(Text.translatable("villagesreborn.biome_selector.title"));
        this.client = MinecraftClient.getInstance();
        this.availableBiomes = BiomeManager.getInstance().getSelectableBiomes();
    }
    
    private BiomeSelectorScreen(WorldCreationMode mode, CreateWorldScreen parent) {
        super(Text.translatable("villagesreborn.biome_selector.title"));
        this.client = MinecraftClient.getInstance();
        this.creationMode = mode;
        this.parentScreen = parent;
        
        if (mode == WorldCreationMode.WORLD_CREATION) {
            this.availableBiomes = BiomeManager.getInstance().getSelectableBiomes();
        } else {
            this.availableBiomes = BiomeManager.getInstance().getSelectableBiomes();
        }
    }
    
    public static BiomeSelectorScreen create() {
        return new BiomeSelectorScreen();
    }
    
    public static BiomeSelectorScreen createForWorldCreation(CreateWorldScreen parentScreen) {
        BiomeSelectorScreen screen = new BiomeSelectorScreen(WorldCreationMode.WORLD_CREATION, parentScreen);
        LOGGER.info("Created BiomeSelectorScreen in world creation mode");
        return screen;
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
        if (creationMode == WorldCreationMode.WORLD_CREATION) {
            // World creation mode buttons
            this.confirmButton = ButtonWidget.builder(
                Text.translatable("villagesreborn.spawn_biome.confirm_for_creation"),
                button -> confirmBiomeSelection()
            )
            .position(this.width / 2 - 155, this.height - 50)
            .size(100, 20)
            .build();
            
            this.cancelButton = ButtonWidget.builder(
                Text.translatable("villagesreborn.spawn_biome.cancel_creation"),
                button -> cancelSelection()
            )
            .position(this.width / 2 - 50, this.height - 50)
            .size(100, 20)
            .build();
            
            this.randomButton = ButtonWidget.builder(
                Text.translatable("villagesreborn.biome_selector.random"),
                button -> selectRandomBiome()
            )
            .position(this.width / 2 + 55, this.height - 50)
            .size(100, 20)
            .build();
            
            this.addDrawableChild(confirmButton);
            this.addDrawableChild(cancelButton);
            this.addDrawableChild(randomButton);
        } else {
            // Post-join mode buttons (existing)
            this.confirmButton = ButtonWidget.builder(
                Text.translatable("villagesreborn.biome_selector.confirm"),
                button -> confirmBiomeSelection()
            )
            .position(this.width / 2 - 100, this.height - 50)
            .size(95, 20)
            .build();
            
            this.randomButton = ButtonWidget.builder(
                Text.translatable("villagesreborn.biome_selector.random"),
                button -> selectRandomBiome()
            )
            .position(this.width / 2 + 5, this.height - 50)
            .size(95, 20)
            .build();
            
            this.addDrawableChild(confirmButton);
            this.addDrawableChild(randomButton);
        }
        
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
        
        if (creationMode == WorldCreationMode.WORLD_CREATION) {
            // Store choice for world creation
            SpawnBiomeChoiceData choiceData = new SpawnBiomeChoiceData(
                selectedBiome.getRegistryKey(),
                System.currentTimeMillis()
            );
            
            // Store in world creation settings capture
            WorldCreationSettingsCapture.setSpawnBiomeChoice(selectedBiome);
            LOGGER.info("Stored spawn biome choice for world creation: {}", selectedBiome.getRegistryKey());
            
            // Return to parent screen
            if (parentScreen != null) {
                client.setScreen(parentScreen);
                // Reset the parent screen's biome selector open state
                try {
                    parentScreen.getClass().getMethod("resetBiomeSelectorState").invoke(parentScreen);
                } catch (Exception e) {
                    LOGGER.debug("Could not reset biome selector state on parent screen", e);
                }
            } else {
                this.close();
            }
        } else {
            // Post-join mode (existing logic)
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
    }
    
    private void cancelSelection() {
        LOGGER.info("Player cancelled biome selection");
        
        if (creationMode == WorldCreationMode.WORLD_CREATION && parentScreen != null) {
            // Return to parent screen without storing choice
            client.setScreen(parentScreen);
            // Reset the parent screen's biome selector open state
            try {
                parentScreen.getClass().getMethod("resetBiomeSelectorState").invoke(parentScreen);
            } catch (Exception e) {
                LOGGER.debug("Could not reset biome selector state on parent screen", e);
            }
        } else {
            this.close();
        }
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