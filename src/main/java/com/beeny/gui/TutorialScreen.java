package com.beeny.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;

public class TutorialScreen extends Screen {
    private int currentStep = 0;
    private boolean isDismissed = false;
    // private long showTime; // Removed for manual dismissal

    private static class TutorialTip {
        final String message;
        final String context;
        // final int durationMs; // Removed for manual dismissal

        TutorialTip(String message, String context) { // Removed durationMs
            this.message = message;
            this.context = context;
            // this.durationMs = durationMs; // Removed
        }
    }

    private final TutorialTip[] tips = {
        new TutorialTip("Right-click a villager to talk to them!", "near_villager"),
        new TutorialTip("Press 'B' to see info about the village!", "in_village"),
        new TutorialTip("Hold Sneak + Right-click a villager to see their personality!", "targeting_villager"),
        new TutorialTip("Type /village events in chat to find activities!", "event_nearby"),
        new TutorialTip("Help villagers with tasks to become friends!", "first_task")
    };

    public TutorialScreen(Text title) {
        super(title);
        // this.showTime = System.currentTimeMillis(); // Removed
    }

    @Override
    protected void init() {
        super.init();
        // Tip box dimensions and position (top-right)
        int boxWidth = 200;
        int boxHeight = 60; // Unused in init, but kept for context
        int boxX = this.width - boxWidth - 10; // Use boxX for consistency with render
        int boxY = 10;

        // Add a close ('X') button inside the box
        int closeButtonSize = 15;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("X"), button -> dismiss())
            .dimensions(boxX + boxWidth - closeButtonSize - 3, boxY + 3, closeButtonSize, closeButtonSize) // Use boxX, boxY
            .build());
    }

    private void dismiss() {
        this.isDismissed = true;
        this.client.setScreen(null);
    }

    public static void showIfRelevant(MinecraftClient client, String context) {
        TutorialScreen screen = new TutorialScreen(Text.literal("Tutorial"));
        screen.currentStep = -1;
        
        for (int i = 0; i < screen.tips.length; i++) {
            if (screen.tips[i].context.equals(context)) {
                screen.currentStep = i;
                break;
            }
        }
        
        if (screen.currentStep != -1) {
            client.setScreen(screen);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Tip box constants
        int boxWidth = 200;
        int boxHeight = 60;
        int boxX = this.width - boxWidth - 10; // Keep names distinct from text coords
        int boxY = 10;
        
        // Draw semi-transparent background
        context.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0xC0000000); // Use boxX, boxY
        context.drawBorder(boxX, boxY, boxWidth, boxHeight, 0xFFFFFFFF); // Use boxX, boxY
        
        // Draw Title
        context.drawTextWithShadow(this.textRenderer, Text.literal("Quick Tip!"), boxX + 5, boxY + 5, 0xFFFFFF); // Use boxX, boxY

        // Draw progress indicator (below title)
        String progressText = String.format("Tip %d/%d", currentStep + 1, tips.length);
        context.drawTextWithShadow(this.textRenderer, Text.literal(progressText), boxX + 5, boxY + 18, 0xAAAAAA); // Use boxX, boxY

        // Draw tip message (adjust starting Y)
        String[] words = tips[currentStep].message.split(" ");
        StringBuilder currentLine = new StringBuilder();
        int lineY = boxY + 31; // Start message below progress text (Use boxY)
        
        for (String word : words) {
            // Add padding (10px left/right) for text wrapping check
            if (this.textRenderer.getWidth(currentLine + " " + word) > boxWidth - 20) {
                context.drawTextWithShadow(this.textRenderer,
                    Text.literal(currentLine.toString().trim()),
                    boxX + 10, lineY, 0xFFFFFF); // Use boxX + padding
                lineY += 10;
                currentLine = new StringBuilder(word + " ");
            } else {
                currentLine.append(word).append(" ");
            }
        }
        
        if (currentLine.length() > 0) {
            context.drawTextWithShadow(this.textRenderer,
                Text.literal(currentLine.toString().trim()),
                boxX + 10, lineY, 0xFFFFFF); // Use boxX + padding
        }

        // Auto-dismiss logic removed - user must click 'X'

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
