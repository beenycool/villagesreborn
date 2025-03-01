package com.beeny.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;

public class TutorialScreen extends Screen {
    private int currentStep = 0;
    private boolean isDismissed = false;
    private long showTime;

    private static class TutorialTip {
        final String message;
        final String context;
        final int durationMs;

        TutorialTip(String message, String context, int durationMs) {
            this.message = message;
            this.context = context;
            this.durationMs = durationMs;
        }
    }

    private final TutorialTip[] tips = {
        new TutorialTip("Right-click a villager to start a conversation!", "near_villager", 5000),
        new TutorialTip("Press B to view village info!", "in_village", 4000),
        new TutorialTip("Sneak + Right-click to view villager personality!", "targeting_villager", 4000),
        new TutorialTip("Use /village events to see upcoming activities!", "event_nearby", 5000),
        new TutorialTip("Help villagers with tasks to increase friendship!", "first_task", 5000)
    };

    public TutorialScreen(Text title) {
        super(title);
        this.showTime = System.currentTimeMillis();
    }

    @Override
    protected void init() {
        super.init();
        int buttonWidth = 60;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Got it!"), button -> dismiss())
            .dimensions(this.width / 2 - buttonWidth / 2, this.height - 30, buttonWidth, 20)
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
        int boxWidth = 200;
        int boxHeight = 60;
        int x = (this.width - boxWidth) / 2;
        int y = 30;
        
        // Draw semi-transparent background
        context.fill(x, y, x + boxWidth, y + boxHeight, 0xC0000000);
        context.drawBorder(x, y, boxWidth, boxHeight, 0xFFFFFFFF);
        
        // Draw tip message
        String[] words = tips[currentStep].message.split(" ");
        StringBuilder currentLine = new StringBuilder();
        int lineY = y + 10;
        
        for (String word : words) {
            if (this.textRenderer.getWidth(currentLine + " " + word) > boxWidth - 20) {
                context.drawTextWithShadow(this.textRenderer,
                    Text.literal(currentLine.toString().trim()),
                    x + 10, lineY, 0xFFFFFF);
                lineY += 10;
                currentLine = new StringBuilder(word + " ");
            } else {
                currentLine.append(word).append(" ");
            }
        }
        
        if (currentLine.length() > 0) {
            context.drawTextWithShadow(this.textRenderer,
                Text.literal(currentLine.toString().trim()),
                x + 10, lineY, 0xFFFFFF);
        }

        // Check if tip should auto-dismiss
        if (System.currentTimeMillis() - showTime > tips[currentStep].durationMs) {
            dismiss();
        }

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
