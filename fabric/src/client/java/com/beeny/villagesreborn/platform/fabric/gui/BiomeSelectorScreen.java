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
        
        // Calculate GUI scaling
        double guiScale = client.getWindow().getScaleFactor();
        
        int scaledPadding = (int) Math.max(16, 20 / guiScale * 2);
        int scaledStartY = (int) Math.max(50, 60 / guiScale * 2);
        int scaledWidgetWidth = (int) Math.max(100, 120 / guiScale * 2);
        int scaledWidgetHeight = (int) Math.max(60, 80 / guiScale * 2);
        int scaledSpacing = (int) Math.max(8, 10 / guiScale * 2);
        
        // Adaptive column count based on screen width
        int availableWidth = this.width - (scaledPadding * 2);
        int widgetPlusSpacing = scaledWidgetWidth + scaledSpacing;
        int columns = Math.max(1, Math.min(4, availableWidth / widgetPlusSpacing));
        
        int startX = (this.width - (columns * scaledWidgetWidth + (columns - 1) * scaledSpacing)) / 2;
        
        for (int i = 0; i < availableBiomes.size(); i++) {
            BiomeDisplayInfo biome = availableBiomes.get(i);
            
            int row = i / columns;
            int col = i % columns;
            
            int x = startX + col * (scaledWidgetWidth + scaledSpacing);
            int y = scaledStartY + row * (scaledWidgetHeight + scaledSpacing);
            
            BiomeSelectionWidget widget = new BiomeSelectionWidget(
                biome, scaledWidgetWidth, scaledWidgetHeight, this::onBiomeSelected
            );
            widget.setX(x);
            widget.setY(y);
            
            biomeWidgets.add(widget);
            this.addDrawableChild(widget);
        }
    }
    
    private void initializeControlButtons() {
        // Calculate GUI scaling for buttons
        double guiScale = client.getWindow().getScaleFactor();
        int buttonHeight = (int) Math.max(18, 20 / guiScale * 2);
        int buttonWidth = Math.min(100, (this.width - 60) / 3);
        int bottomPadding = (int) Math.max(40, 50 / guiScale * 2);
        
        if (creationMode == WorldCreationMode.WORLD_CREATION) {
            // World creation mode buttons - three buttons layout
            int totalButtonWidth = buttonWidth * 3 + 20; // 10px spacing between buttons
            int startX = (this.width - totalButtonWidth) / 2;
            
            this.confirmButton = ButtonWidget.builder(
                Text.translatable("villagesreborn.spawn_biome.confirm_for_creation"),
                button -> confirmBiomeSelection()
            )
            .position(startX, this.height - bottomPadding)
            .size(buttonWidth, buttonHeight)
            .build();
            
            this.cancelButton = ButtonWidget.builder(
                Text.translatable("villagesreborn.spawn_biome.cancel_creation"),
                button -> cancelSelection()
            )
            .position(startX + buttonWidth + 10, this.height - bottomPadding)
            .size(buttonWidth, buttonHeight)
            .build();
            
            this.randomButton = ButtonWidget.builder(
                Text.translatable("villagesreborn.biome_selector.random"),
                button -> selectRandomBiome()
            )
            .position(startX + (buttonWidth + 10) * 2, this.height - bottomPadding)
            .size(buttonWidth, buttonHeight)
            .build();
            
            this.addDrawableChild(confirmButton);
            this.addDrawableChild(cancelButton);
            this.addDrawableChild(randomButton);
        } else {
            // Post-join mode buttons - two buttons layout
            int totalButtonWidth = buttonWidth * 2 + 10; // 10px spacing between buttons
            int startX = (this.width - totalButtonWidth) / 2;
            
            this.confirmButton = ButtonWidget.builder(
                Text.translatable("villagesreborn.biome_selector.confirm"),
                button -> confirmBiomeSelection()
            )
            .position(startX, this.height - bottomPadding)
            .size(buttonWidth, buttonHeight)
            .build();
            
            this.randomButton = ButtonWidget.builder(
                Text.translatable("villagesreborn.biome_selector.random"),
                button -> selectRandomBiome()
            )
            .position(startX + buttonWidth + 10, this.height - bottomPadding)
            .size(buttonWidth, buttonHeight)
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
                    // Try to find and call the resetBiomeSelectorState method
                    java.lang.reflect.Method resetMethod = null;
                    Class<?> currentClass = parentScreen.getClass();
                    while (currentClass != null && resetMethod == null) {
                        try {
                            resetMethod = currentClass.getDeclaredMethod("resetBiomeSelectorState");
                            resetMethod.setAccessible(true);
                            break;
                        } catch (NoSuchMethodException e) {
                            currentClass = currentClass.getSuperclass();
                        }
                    }
                    if (resetMethod != null) {
                        resetMethod.invoke(parentScreen);
                        LOGGER.debug("Successfully reset biome selector state on parent screen");
                    } else {
                        LOGGER.debug("Could not find resetBiomeSelectorState method on parent screen");
                    }
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
            
            // Store the spawn biome choice using the proper storage manager
            // Note: In a complete implementation, this would use SpawnBiomeStorageManager
            // For now, we'll just log the selection since the deprecated method was removed
            System.out.println("Selected spawn biome: " + selectedBiome.getRegistryKey().getValue());
            
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
        
        // Calculate GUI scaling for text positioning
        double guiScale = client.getWindow().getScaleFactor();
        int titleY = (int) Math.max(16, 20 / guiScale * 2);
        int descriptionY = (int) Math.max(32, 40 / guiScale * 2);
        
        // Render title
        context.drawCenteredTextWithShadow(this.textRenderer, this.title,
            this.width / 2, titleY, 0xFFFFFF);
        
        // Render description
        Text description = Text.translatable("villagesreborn.biome_selector.description");
        context.drawCenteredTextWithShadow(this.textRenderer, description,
            this.width / 2, descriptionY, 0xCCCCCC);
        
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