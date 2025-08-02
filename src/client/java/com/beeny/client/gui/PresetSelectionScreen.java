package com.beeny.client.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class PresetSelectionScreen extends Screen {
    private static final int PANEL_WIDTH = 500;
    private static final int PANEL_HEIGHT = 400;
    
    private final Screen parent;
    private final Consumer<DialogueConfigScreen.PresetConfig> onPresetSelected;
    
    private final List<DialogueConfigScreen.PresetConfig> presets = Arrays.asList(
        new DialogueConfigScreen.PresetConfig(
            "Google Gemini (Recommended)",
            "gemini",
            "gemini-1.5-flash",
            "",
            "Free tier available, fast responses, good for beginners"
        ),
        new DialogueConfigScreen.PresetConfig(
            "OpenRouter - GPT-3.5 Turbo",
            "openrouter", 
            "openai/gpt-3.5-turbo",
            "",
            "Reliable, well-tested model with good dialogue quality"
        ),
        new DialogueConfigScreen.PresetConfig(
            "OpenRouter - Claude 3 Haiku",
            "openrouter",
            "anthropic/claude-3-haiku",
            "",
            "Anthropic's fast model, excellent for conversations"
        ),
        new DialogueConfigScreen.PresetConfig(
            "OpenRouter - Llama 3.1",
            "openrouter",
            "meta-llama/llama-3.1-8b-instruct:free",
            "",
            "Free open-source model, good performance"
        )
    );
    
    public PresetSelectionScreen(Screen parent, Consumer<DialogueConfigScreen.PresetConfig> onPresetSelected) {
        super(Text.literal("Quick Setup - Choose a Preset"));
        this.parent = parent;
        this.onPresetSelected = onPresetSelected;
    }
    
    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int panelLeft = centerX - PANEL_WIDTH / 2;
        int panelTop = centerY - PANEL_HEIGHT / 2;
        
        int currentY = panelTop + 50;
        
        // Add preset buttons
        for (DialogueConfigScreen.PresetConfig preset : presets) {
            ButtonWidget presetButton = ButtonWidget.builder(
                Text.literal(preset.name), 
                button -> selectPreset(preset)
            ).dimensions(panelLeft + 20, currentY, PANEL_WIDTH - 40, 25).build();
            
            this.addDrawableChild(presetButton);
            currentY += 80; // Extra space for description
        }
        
        // Cancel button
        ButtonWidget cancelButton = ButtonWidget.builder(
            Text.literal("Cancel"), 
            button -> this.close()
        ).dimensions(centerX - 50, panelTop + PANEL_HEIGHT - 40, 100, 20).build();
        
        this.addDrawableChild(cancelButton);
    }
    
    private void selectPreset(DialogueConfigScreen.PresetConfig preset) {
        onPresetSelected.accept(preset);
        this.close();
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int panelLeft = centerX - PANEL_WIDTH / 2;
        int panelTop = centerY - PANEL_HEIGHT / 2;
        
        // Draw panel background
        context.fill(panelLeft, panelTop, panelLeft + PANEL_WIDTH, panelTop + PANEL_HEIGHT, 0x88000000);
        context.drawBorder(panelLeft, panelTop, PANEL_WIDTH, PANEL_HEIGHT, 0xFFFFFFFF);
        
        // Draw title
        Text title = Text.literal("Quick Setup - Choose a Preset").formatted(Formatting.BOLD);
        int titleWidth = this.textRenderer.getWidth(title);
        context.drawText(this.textRenderer, title, centerX - titleWidth / 2, panelTop + 15, 0xFFFFFF, false);
        
        // Draw subtitle
        Text subtitle = Text.literal("Select a configuration to get started quickly").formatted(Formatting.GRAY);
        int subtitleWidth = this.textRenderer.getWidth(subtitle);
        context.drawText(this.textRenderer, subtitle, centerX - subtitleWidth / 2, panelTop + 30, 0xAAAAAA, false);
        
        // Draw preset descriptions
        int currentY = panelTop + 75;
        for (DialogueConfigScreen.PresetConfig preset : presets) {
            // Draw description text below each button
            context.drawText(this.textRenderer, 
                Text.literal(preset.description).formatted(Formatting.GRAY), 
                panelLeft + 25, currentY, 0x888888, false);
            
            // Draw API key requirement note
            String keyNote = preset.provider.equals("gemini") ? 
                "API Key: Get free key from ai.google.dev" : 
                "API Key: Get key from openrouter.ai";
            
            context.drawText(this.textRenderer, 
                Text.literal(keyNote).formatted(Formatting.DARK_AQUA), 
                panelLeft + 25, currentY + 12, 0x0088AA, false);
            
            currentY += 80;
        }
        
        super.render(context, mouseX, mouseY, delta);
    }
    
    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }
}