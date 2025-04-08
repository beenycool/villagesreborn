// File: src/main/java/com/beeny/gui/GameplaySettingsScreen.java
package com.beeny.gui;

import com.beeny.config.VillagesConfig;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Settings screen for gameplay-related options in Villages Reborn mod.
 * Controls features like villager PvP, theft detection, and other gameplay mechanics.
 */
public class GameplaySettingsScreen extends Screen {
    private static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn");
    private final Screen parent;
    private final VillagesConfig config;

    public GameplaySettingsScreen(Screen parent) {
        super(Text.literal("Gameplay Settings"));
        this.parent = parent;
        this.config = VillagesConfig.getInstance();
    }

    @Override
    protected void init() {
        int y = this.height / 4;
        int buttonWidth = 200;
        int spacing = 24;

        // Villager PvP Toggle
        this.addDrawableChild(CyclingButtonWidget.<Boolean>builder(value -> Text.literal(value ? "ON" : "OFF"))
            .values(true, false)
            .initially(config.getGameplaySettings().isVillagerPvPEnabled())
            .tooltip(value -> Tooltip.of(Text.literal("Allows villagers to engage in combat with players or other mobs")))
            .build(this.width / 2 - buttonWidth / 2, y, buttonWidth, 20, Text.literal("Villager PvP"),
                (button, value) -> config.getGameplaySettings().toggleVillagerPvP()) // Action on change
        );
        y += spacing;

        // Theft Detection Toggle
        this.addDrawableChild(CyclingButtonWidget.<Boolean>builder(value -> Text.literal(value ? "ON" : "OFF"))
            .values(true, false)
            .initially(config.getGameplaySettings().isTheftDetectionEnabled())
            .tooltip(value -> Tooltip.of(Text.literal("Villagers will react negatively if they see you stealing")))
            .build(this.width / 2 - buttonWidth / 2, y, buttonWidth, 20, Text.literal("Theft Detection"),
                (button, value) -> config.getGameplaySettings().toggleTheftDetection()) // Action on change
        );
        y += spacing;

        // Villager Memory Duration
        int memoryDuration = config.getGameplaySettings().getVillagerMemoryDuration();
        SliderWidget memorySlider = new SliderWidget(
            this.width / 2 - buttonWidth / 2, y, buttonWidth, 20,
            Text.literal("Memory Duration: " + formatDuration(memoryDuration)),
            (double) (memoryDuration - 1) / 6) {
            @Override
            protected void updateMessage() {
                int value = (int) (this.value * 6) + 1;
                this.setMessage(Text.literal("Memory Duration: " + formatDuration(value)));
            }

            @Override
            protected void applyValue() {
                int value = (int) (this.value * 6) + 1;
                config.getGameplaySettings().setVillagerMemoryDuration(value);
            }
        };
        memorySlider.setTooltip(Tooltip.of(Text.literal("How long villagers remember player actions (in Minecraft days)")));
        this.addDrawableChild(memorySlider);
        y += spacing;

        // Villager Trading Boost
        this.addDrawableChild(CyclingButtonWidget.<Boolean>builder(value -> Text.literal(value ? "ON" : "OFF"))
            .values(true, false)
            .initially(config.getGameplaySettings().isVillagerTradingBoostEnabled())
            .tooltip(value -> Tooltip.of(Text.literal("Better trades with villagers of cultures you have good relations with")))
            .build(this.width / 2 - buttonWidth / 2, y, buttonWidth, 20, Text.literal("Cultural Trading Bonus"),
                (button, value) -> config.getGameplaySettings().toggleVillagerTradingBoost()) // Action on change
        );
        y += spacing;

        // Unique Crafting Recipes
        this.addDrawableChild(CyclingButtonWidget.<Boolean>builder(value -> Text.literal(value ? "ON" : "OFF"))
            .values(true, false)
            .initially(config.getGameplaySettings().isUniqueCraftingRecipesEnabled())
            .tooltip(value -> Tooltip.of(Text.literal("Enable culture-specific crafting recipes")))
            .build(this.width / 2 - buttonWidth / 2, y, buttonWidth, 20, Text.literal("Cultural Crafting"),
                (button, value) -> config.getGameplaySettings().toggleUniqueCraftingRecipes()) // Action on change
        );
        y += spacing;

        // Cultural Gift Value Modifier
        int giftModifier = config.getGameplaySettings().getCulturalGiftModifier();
        SliderWidget giftSlider = new SliderWidget(
            this.width / 2 - buttonWidth / 2, y, buttonWidth, 20,
            Text.literal("Gift Value Modifier: " + giftModifier + "%"),
            (double) (giftModifier - 100) / 100) {
            @Override
            protected void updateMessage() {
                int value = (int) (this.value * 100) + 100;
                this.setMessage(Text.literal("Gift Value Modifier: " + value + "%"));
            }

            @Override
            protected void applyValue() {
                int value = (int) (this.value * 100) + 100;
                config.getGameplaySettings().setCulturalGiftModifier(value);
            }
        };
        giftSlider.setTooltip(Tooltip.of(Text.literal("How much cultural preferences affect gift values")));
        this.addDrawableChild(giftSlider);
        y += spacing;

        // Back button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Back"), button -> close())
            .dimensions(this.width / 2 - 100, this.height - 40, 200, 20)
            .build());
    }

    /**
     * Formats the memory duration into a readable string
     */
    private String formatDuration(int days) {
        if (days == 1) return "1 Day";
        else if (days <= 7) return days + " Days";
        else return (days / 7) + " Weeks";
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
        
        // Draw description text
        String description = "Configure gameplay mechanics for Villages Reborn";
        context.drawCenteredTextWithShadow(
            this.textRenderer, 
            description,
            this.width / 2, 
            35, 
            0xAAAAAA
        );
        
        super.render(context, mouseX, mouseY, delta);
    }
    
    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}