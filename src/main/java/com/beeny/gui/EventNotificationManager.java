package com.beeny.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Manages and displays event notifications on the player's screen.
 * These notifications appear temporarily when cultural events occur.
 */
public class EventNotificationManager {
    private final List<EventNotification> activeNotifications = new ArrayList<>();
    private static final int MAX_NOTIFICATIONS = 3; // Maximum number of notifications visible at once
    private static final int NOTIFICATION_WIDTH = 220; // Wider notifications for better readability
    private static final int NOTIFICATION_HEIGHT = 60; // Taller notifications for more content
    
    /**
     * Adds a new notification to be displayed.
     *
     * @param title The title of the notification
     * @param description The description text
     * @param durationTicks How long to display the notification (in ticks)
     */
    public void addNotification(String title, String description, int durationTicks) {
        // Play notification sound
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.playSound(
                SoundEvents.BLOCK_NOTE_BLOCK_CHIME, 
                SoundCategory.MASTER, 
                0.5f, // Volume
                1.0f  // Pitch
            );
        }
        
        // Limit number of concurrent notifications
        if (activeNotifications.size() >= MAX_NOTIFICATIONS) {
            // Remove the oldest notification
            activeNotifications.remove(0);
        }
        
        // Add the new notification
        activeNotifications.add(new EventNotification(title, description, durationTicks));
    }
    
    /**
     * Renders all active notifications and updates their state.
     *
     * @param matrices The matrix stack
     * @param client The Minecraft client
     * @param tickDelta The partial tick
     */
    public void render(MatrixStack matrices, MinecraftClient client, float tickDelta) {
        if (activeNotifications.isEmpty()) {
            return;
        }
        
        TextRenderer textRenderer = client.textRenderer;
        int screenWidth = client.getWindow().getScaledWidth();
        int padding = 10; // Padding between notifications
        
        // Remove expired notifications
        Iterator<EventNotification> iterator = activeNotifications.iterator();
        while (iterator.hasNext()) {
            EventNotification notification = iterator.next();
            notification.update();
            if (notification.isExpired()) {
                iterator.remove();
            }
        }
        
        // Render active notifications from bottom to top
        int yPosition = client.getWindow().getScaledHeight() - padding - NOTIFICATION_HEIGHT;
        for (int i = activeNotifications.size() - 1; i >= 0; i--) {
            EventNotification notification = activeNotifications.get(i);
            
            // Calculate animation offset (slide in from right)
            float slideOffset = notification.getSlideOffset();
            int xOffset = (int)(slideOffset * NOTIFICATION_WIDTH);
            
            // Render with animation
            renderNotification(
                matrices, 
                textRenderer, 
                notification, 
                screenWidth + xOffset, 
                yPosition
            );
            
            yPosition -= (NOTIFICATION_HEIGHT + padding);
        }
    }
    
    /**
     * Renders a single notification.
     *
     * @param matrices The matrix stack
     * @param textRenderer The text renderer
     * @param notification The notification to render
     * @param xPosition The x position to render at
     * @param yPosition The y position to render at
     */
    private void renderNotification(MatrixStack matrices, TextRenderer textRenderer, 
                                   EventNotification notification, int xPosition, int yPosition) {
        // Calculate fade factor
        float alpha = notification.getFadeLevel();
        if (alpha <= 0.05f) return; // Don't render nearly invisible notifications
        
        int width = NOTIFICATION_WIDTH;
        int height = NOTIFICATION_HEIGHT;
        xPosition -= width; // Position from right edge
        
        // Background with alpha based on fade level
        int backgroundColor = ((int)(alpha * 192) << 24) | 0x000000;
        int borderColor = ((int)(alpha * 255) << 24) | 0xFFD700; // Gold border
        
        // Draw a glowing background effect
        float glowPulse = (float)(0.3 * Math.sin(notification.getLifeTimeProgress() * Math.PI * 6) + 0.7);
        int glowColor = ((int)(alpha * 64 * glowPulse) << 24) | 0xFFD700;
        fill(matrices, xPosition - 3, yPosition - 3, xPosition + width + 3, yPosition + height + 3, glowColor);
        
        // Draw background and border
        fill(matrices, xPosition - 1, yPosition - 1, xPosition + width + 1, yPosition + height + 1, borderColor);
        fill(matrices, xPosition, yPosition, xPosition + width, yPosition + height, backgroundColor);
        
        // Title
        int titleColor = ((int)(alpha * 255) << 24) | 0xFFD700; // Gold text for title
        textRenderer.drawWithShadow(
            matrices, 
            Text.literal("◆ " + notification.title + " ◆"),
            xPosition + width/2 - textRenderer.getWidth("◆ " + notification.title + " ◆")/2, // Center text
            yPosition + 10, 
            titleColor
        );
        
        // Divider line
        fill(
            matrices, 
            xPosition + 10, 
            yPosition + 22, 
            xPosition + width - 10, 
            yPosition + 23, 
            ((int)(alpha * 128) << 24) | 0xFFD700
        );
        
        // Description (with possible word wrap)
        int descriptionColor = ((int)(alpha * 255) << 24) | 0xFFFFFF; // White text for description
        String[] words = notification.description.split(" ");
        StringBuilder currentLine = new StringBuilder();
        int lineY = yPosition + 28;
        
        for (String word : words) {
            if (textRenderer.getWidth(currentLine + " " + word) > width - 20 && !currentLine.isEmpty()) {
                // Line would be too long, render current line and start a new one
                textRenderer.draw(
                    matrices,
                    currentLine.toString(),
                    xPosition + 10,
                    lineY,
                    descriptionColor
                );
                currentLine = new StringBuilder(word);
                lineY += textRenderer.fontHeight + 2;
                
                // Prevent overflow
                if (lineY >= yPosition + height - 5) break;
            } else {
                // Add word to current line
                if (!currentLine.isEmpty()) {
                    currentLine.append(" ");
                }
                currentLine.append(word);
            }
        }
        
        // Draw the last line
        if (currentLine.length() > 0 && lineY < yPosition + height - 5) {
            textRenderer.draw(
                matrices,
                currentLine.toString(),
                xPosition + 10,
                lineY,
                descriptionColor
            );
        }
    }
    
    /**
     * Fills a rectangular area with a solid color.
     */
    private void fill(MatrixStack matrices, int startX, int startY, int endX, int endY, int color) {
        int minX = Math.min(startX, endX);
        int minY = Math.min(startY, endY);
        int maxX = Math.max(startX, endX);
        int maxY = Math.max(startY, endY);
        
        matrices.push();
        net.minecraft.client.render.Tessellator tessellator = net.minecraft.client.render.Tessellator.getInstance();
        net.minecraft.client.render.BufferBuilder bufferBuilder = tessellator.getBuffer();
        
        net.minecraft.client.render.RenderSystem.enableBlend();
        net.minecraft.client.render.RenderSystem.disableTexture();
        net.minecraft.client.render.RenderSystem.defaultBlendFunc();
        
        bufferBuilder.begin(net.minecraft.client.render.VertexFormat.DrawMode.QUADS, net.minecraft.client.render.VertexFormats.POSITION_COLOR);
        
        float a = (color >> 24 & 255) / 255.0F;
        float r = (color >> 16 & 255) / 255.0F;
        float g = (color >> 8 & 255) / 255.0F;
        float b = (color & 255) / 255.0F;
        
        bufferBuilder.vertex(minX, maxY, 0).color(r, g, b, a).next();
        bufferBuilder.vertex(maxX, maxY, 0).color(r, g, b, a).next();
        bufferBuilder.vertex(maxX, minY, 0).color(r, g, b, a).next();
        bufferBuilder.vertex(minX, minY, 0).color(r, g, b, a).next();
        
        tessellator.draw();
        net.minecraft.client.render.RenderSystem.enableTexture();
        net.minecraft.client.render.RenderSystem.disableBlend();
        
        matrices.pop();
    }
    
    /**
     * Internal class representing a single notification.
     */
    private static class EventNotification {
        private final String title;
        private final String description;
        private int remainingTicks;
        private final int totalDuration;
        private static final int FADE_DURATION = 20; // Ticks to fade in/out
        private static final int SLIDE_DURATION = 15; // Ticks for slide animation
        
        public EventNotification(String title, String description, int durationTicks) {
            this.title = title;
            this.description = description;
            this.remainingTicks = durationTicks;
            this.totalDuration = durationTicks;
        }
        
        /**
         * Updates the notification state, decreasing its remaining time.
         */
        public void update() {
            if (remainingTicks > 0) {
                remainingTicks--;
            }
        }
        
        /**
         * Checks if this notification has expired and should be removed.
         */
        public boolean isExpired() {
            return remainingTicks <= 0;
        }
        
        /**
         * Gets the current fade level (0.0 to 1.0) based on the notification lifetime.
         */
        public float getFadeLevel() {
            // Fade in during first FADE_DURATION ticks
            if (totalDuration - remainingTicks < FADE_DURATION) {
                return (float)(totalDuration - remainingTicks) / FADE_DURATION;
            }
            
            // Fade out during last FADE_DURATION ticks
            if (remainingTicks < FADE_DURATION) {
                return (float)remainingTicks / FADE_DURATION;
            }
            
            // Fully visible between fade in/out
            return 1.0f;
        }
        
        /**
         * Gets the slide offset factor (-1.0 to 0.0) for horizontal animation.
         * -1.0 means fully off-screen, 0.0 means in final position.
         */
        public float getSlideOffset() {
            // Slide in during first SLIDE_DURATION ticks
            if (totalDuration - remainingTicks < SLIDE_DURATION) {
                float progress = (float)(totalDuration - remainingTicks) / SLIDE_DURATION;
                return progress - 1.0f; // -1.0 to 0.0 range
            }
            
            // Slide out during last SLIDE_DURATION ticks
            if (remainingTicks < SLIDE_DURATION) {
                float progress = (float)remainingTicks / SLIDE_DURATION;
                return -1.0f + progress; // -1.0 to 0.0 range
            }
            
            // Stable position in between
            return 0.0f;
        }
        
        /**
         * Gets the progress through the notification's lifetime (0.0 to 1.0)
         */
        public float getLifeTimeProgress() {
            return (float)(totalDuration - remainingTicks) / totalDuration;
        }
    }
}