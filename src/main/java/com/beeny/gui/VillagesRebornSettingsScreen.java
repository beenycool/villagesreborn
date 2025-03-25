package com.beeny.gui;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import com.beeny.config.VillagesConfig;

public class VillagesRebornSettingsScreen extends Screen {
    private final Screen parent;
    private VillagesConfig config;

    public VillagesRebornSettingsScreen(Screen parent) {
        super(Text.of("Villages Reborn Settings"));
        this.parent = parent;
        this.config = VillagesConfig.getInstance();
    }

    @Override
    protected void init() {
        int y = this.height / 4;

        // Village generation frequency
        this.addDrawableChild(ButtonWidget.builder(Text.of("Village Spawn Rate: " + config.getVillageSpawnRate()), 
            button -> cycleVillageSpawnRate()).dimensions(this.width / 2 - 100, y, 200, 20).build());

        // AI Provider selection
        this.addDrawableChild(ButtonWidget.builder(Text.of("AI Provider: " + config.getAIProvider()), 
            button -> cycleAIProvider()).dimensions(this.width / 2 - 100, y + 24, 200, 20).build());

        // Cultures selection
        this.addDrawableChild(ButtonWidget.builder(Text.of("Configure Cultures"), 
            button -> openCulturesScreen()).dimensions(this.width / 2 - 100, y + 48, 200, 20).build());

        // Villager PvP
        this.addDrawableChild(ButtonWidget.builder(Text.of("Villager PvP: " + (config.isVillagerPvPEnabled() ? "ON" : "OFF")), 
            button -> toggleVillagerPvP()).dimensions(this.width / 2 - 100, y + 72, 200, 20).build());

        // Theft Detection
        this.addDrawableChild(ButtonWidget.builder(Text.of("Theft Detection: " + (config.isTheftDetectionEnabled() ? "ON" : "OFF")), 
            button -> toggleTheftDetection()).dimensions(this.width / 2 - 100, y + 96, 200, 20).build());

        // Done button
        this.addDrawableChild(ButtonWidget.builder(Text.of("Done"), 
            button -> close()).dimensions(this.width / 2 - 100, this.height - 28, 200, 20).build());
    }

    private void cycleVillageSpawnRate() {
        config.cycleVillageSpawnRate();
        init();
    }

    private void cycleAIProvider() {
        config.cycleAIProvider();
        init();
    }

    private void openCulturesScreen() {
        client.setScreen(new CulturesConfigScreen(this, null));
    }

    private void toggleVillagerPvP() {
        config.toggleVillagerPvP();
        init();
    }

    private void toggleTheftDetection() {
        config.toggleTheftDetection();
        init();
    }

@Override
public void close() {
        config.save();
        this.client.setScreen(parent);
    }
}
