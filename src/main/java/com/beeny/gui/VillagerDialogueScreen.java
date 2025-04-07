package com.beeny.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class VillagerDialogueScreen extends Screen {
    private final VillagerEntity villager;
    private TextFieldWidget inputField;
    private final List<String> conversation = new ArrayList<>();
    private int scrollOffset = 0;
    private static final int MAX_MESSAGES = 50;
    private static final int LINE_HEIGHT = 12;
    private static final int CHAT_BOX_PADDING = 4;

    public VillagerDialogueScreen(VillagerEntity villager) {
        super(Text.literal("Conversation with " + villager.getName().getString()));
        this.villager = villager;
    }

    @Override
    protected void init() {
        inputField = new TextFieldWidget(
            this.textRenderer,
            width / 4,
            height - 40,
            width / 2,
            20,
            Text.literal("Type your message...")
        );
        inputField.setMaxLength(256);
        addDrawableChild(inputField);
        if (conversation.size() * LINE_HEIGHT > height - 80) {
            var scrollUpButton = ButtonWidget.builder(Text.literal("↑"), button -> {
                if (scrollOffset > 0) scrollOffset--;
            })
            .dimensions(width / 2 + width / 4 + 10, height / 2 - 40, 20, 20)
            .build();
            addDrawableChild(scrollUpButton);

            ButtonWidget scrollDownButton = ButtonWidget.builder(Text.literal("↓"), button -> {
                if ((scrollOffset + 1) * LINE_HEIGHT < conversation.size() * LINE_HEIGHT) scrollOffset++;
            })
            .dimensions(width / 2 + width / 4 + 10, height / 2 + 20, 20, 20)
            .build();
            addDrawableChild(scrollDownButton);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        
        // Draw title
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 10, 0xFFFFFF);

        // Calculate chat box dimensions
        int chatBoxX = width / 4;
        int chatBoxY = 30;
        int chatBoxWidth = width / 2;
        int chatBoxHeight = height - 80;

        // Draw chat box background
        context.fill(chatBoxX - CHAT_BOX_PADDING, 
                    chatBoxY - CHAT_BOX_PADDING,
                    chatBoxX + chatBoxWidth + CHAT_BOX_PADDING,
                    chatBoxY + chatBoxHeight + CHAT_BOX_PADDING,
                    0x80000000);

        // Draw messages
        int y = chatBoxY;
        int visibleLines = chatBoxHeight / LINE_HEIGHT;
        int startIndex = Math.max(0, conversation.size() - visibleLines - scrollOffset);
        int endIndex = Math.min(conversation.size(), startIndex + visibleLines);

        for (int i = startIndex; i < endIndex; i++) {
            String message = conversation.get(i);
            boolean isPlayerMessage = message.startsWith("You: ");
            int color = isPlayerMessage ? 0xFFFFFF : 0xFFE0A0;
            context.drawTextWithShadow(textRenderer, message, chatBoxX, y, color);
            y += LINE_HEIGHT;
        }

        inputField.render(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            String message = inputField.getText().trim();
            if (!message.isEmpty()) {
                // Add player message to conversation
                conversation.add("You: " + message);
                
                // Generate villager response
                String villagerName = villager.getName().getString();
                if (message.toLowerCase().startsWith("hi") || message.toLowerCase().startsWith("hello")) {
                    conversation.add(villagerName + ": Greetings! How may I assist you today?");
                } else {
                    // Send message to server for AI processing
                    // This will be handled by the networking system we'll implement
                    conversation.add(villagerName + ": *processing response*");
                }

                // Clear input field
                inputField.setText("");

                // Trim conversation if too long
                while (conversation.size() > MAX_MESSAGES) {
                    conversation.remove(0);
                }

                // Auto-scroll to bottom
                scrollOffset = 0;
            }
            return true;
        }

        return inputField.keyPressed(keyCode, scanCode, modifiers) || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}