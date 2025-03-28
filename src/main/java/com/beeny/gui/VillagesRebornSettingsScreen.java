// File: src/main/java/com/beeny/gui/VillagesRebornSettingsScreen.java
package com.beeny.gui;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.tooltip.Tooltip; // Import Tooltip
import net.minecraft.text.Text;
import com.beeny.config.VillagesConfig;
import com.beeny.config.ClothConfigProvider;

public class VillagesRebornSettingsScreen extends Screen {
    private final Screen parent;
    private VillagesConfig config;

    public VillagesRebornSettingsScreen(Screen parent) {
        super(Text.literal("Villages Reborn Settings"));
        this.parent = parent;
        this.config = VillagesConfig.getInstance();
    }

    @Override
    protected void init() {
        int y = this.height / 4 + 24; // Start buttons lower
        int buttonWidth = 200;
        int spacing = 24;

        // Advanced Configuration (Cloth Config)
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Advanced Configuration"),
                button -> this.client.setScreen(new ClothConfigProvider().createConfigScreen(this)))
            .tooltip(Tooltip.of(Text.literal("Configure all settings in detail using a comprehensive interface.")))
            .dimensions(this.width / 2 - buttonWidth / 2, y, buttonWidth, 20)
            .build());
        y += spacing;

        // --- Simplified Main Settings ---

        // Village Spawn Rate (Keep as cycle for simplicity here)
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Village Spawn Rate: " + config.getVillageSpawnRate()),
                button -> {
                    config.cycleVillageSpawnRate();
                    this.client.setScreen(new VillagesRebornSettingsScreen(this.parent)); // Re-initialize screen
                })
            .tooltip(Tooltip.of(Text.literal("Adjusts how often Villages Reborn villages generate.")))
            .dimensions(this.width / 2 - buttonWidth / 2, y, buttonWidth, 20)
            .build());
        y += spacing;

        // AI Settings Button
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("AI Settings"),
                button -> this.client.setScreen(new AISettingsScreen(this))) // Link to new AI screen
            .tooltip(Tooltip.of(Text.literal("Configure AI provider, API keys, and model settings.")))
            .dimensions(this.width / 2 - buttonWidth / 2, y, buttonWidth, 20)
            .build());
        y += spacing;

        // Gameplay Settings Button
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Gameplay Settings"),
                button -> this.client.setScreen(new GameplaySettingsScreen(this))) // Link to new Gameplay screen
            .tooltip(Tooltip.of(Text.literal("Configure features like Villager PvP and Theft Detection.")))
            .dimensions(this.width / 2 - buttonWidth / 2, y, buttonWidth, 20)
            .build());
        y += spacing;

        // Configure Cultures Button (Assuming CulturesConfigScreen exists and works)
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Configure Cultures"),
                button -> this.client.setScreen(new CulturesConfigScreen(this, null))) // null for activeCulture for now
            .tooltip(Tooltip.of(Text.literal("Enable/disable specific village cultures.")))
            .dimensions(this.width / 2 - buttonWidth / 2, y, buttonWidth, 20)
            .build());
        y += spacing;

        // UI Settings Button
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("UI Settings"),
                button -> this.client.setScreen(new VillageUISettingsScreen(this))) // Link to UI settings screen
            .tooltip(Tooltip.of(Text.literal("Customize HUD elements and UI display.")))
            .dimensions(this.width / 2 - buttonWidth / 2, y, buttonWidth, 20)
            .build());
        y += spacing;

        // Done button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Done"),
                button -> close())
            .dimensions(this.width / 2 - 100, this.height - 40, 200, 20) // Move Done button up slightly
            .build());
    }

    // Removed cycleAIProvider, toggleVillagerPvP, toggleTheftDetection - moved to sub-screens

    @Override
    public void close() {
        config.save(); // Save config when closing the main settings screen
        this.client.setScreen(parent);
    }

     @Override
    public void render(net.minecraft.client.gui.DrawContext context, int mouseX, int mouseY, float delta) {
        // Render background darkens the screen slightly
        this.renderBackground(context, mouseX, mouseY, delta);
        // Draw the title centered
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);
        // Render widgets (buttons, etc.)
        super.render(context, mouseX, mouseY, delta);
    }
}