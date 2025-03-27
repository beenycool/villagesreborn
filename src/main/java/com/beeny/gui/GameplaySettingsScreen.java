// File: src/main/java/com/beeny/gui/GameplaySettingsScreen.java
// NEW FILE
package com.beeny.gui;

import com.beeny.config.VillagesConfig;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.text.Text;

public class GameplaySettingsScreen extends Screen {
    private final Screen parent;
    private final VillagesConfig config;

    public GameplaySettingsScreen(Screen parent) {
        super(Text.literal("Gameplay Settings"));
        this.parent = parent;
        this.config = VillagesConfig.getInstance();
    }

    @Override
    protected void init() {
        int y = this.height / 4 + 24;
        int buttonWidth = 200;
        int spacing = 24;

        // Villager PvP Toggle
        this.addDrawableChild(CyclingButtonWidget.<Boolean>builder(value -> Text.literal(value ? "ON" : "OFF"))
            .values(true, false)
            .initially(config.isVillagerPvPEnabled())
            .tooltip(value -> Tooltip.of(Text.literal("Allows villagers to engage in combat with players or other mobs.")))
            .build(this.width / 2 - buttonWidth / 2, y, buttonWidth, 20, Text.literal("Villager PvP"),
                (button, value) -> config.toggleVillagerPvP()) // Action on change
        );
        y += spacing;

        // Theft Detection Toggle
        this.addDrawableChild(CyclingButtonWidget.<Boolean>builder(value -> Text.literal(value ? "ON" : "OFF"))
            .values(true, false)
            .initially(config.isTheftDetectionEnabled())
            .tooltip(value -> Tooltip.of(Text.literal("Villagers will react negatively if they see you stealing.")))
            .build(this.width / 2 - buttonWidth / 2, y, buttonWidth, 20, Text.literal("Theft Detection"),
                (button, value) -> config.toggleTheftDetection()) // Action on change
        );
        y += spacing;


        // Back button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Back"), button -> close())
            .dimensions(this.width / 2 - 100, this.height - 40, 200, 20)
            .build());
    }

     @Override
    public void close() {
        config.save(); // Save changes when closing this screen
        this.client.setScreen(parent);
    }

     @Override
    public void render(net.minecraft.client.gui.DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }
}