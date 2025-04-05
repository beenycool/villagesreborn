package com.beeny.gui;

import com.beeny.config.VillagesConfig;
import com.beeny.village.VillagerManager;
import com.beeny.village.VillagerAI;
import com.beeny.network.VillagesClientNetwork;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;

/**
 * Renders a HUD element showing which villager the player is currently speaking to.
 */
public class ConversationHud {
    private VillagerEntity currentVillager;
    private long conversationStartTime;
    private long lastInteractionTime;
    private static final long CONVERSATION_TIMEOUT = 30000; // 30 seconds of inactivity ends conversation
    private boolean isVisible = false;
    private HudPosition position = HudPosition.BOTTOM_RIGHT;
    // Use Identifier.of() instead of new Identifier() constructor
    private final Identifier ICON_TEXTURE = Identifier.of("villagesreborn", "textures/gui/conversation_icon.png");
    private final Identifier SPEECH_BUBBLE_TEXTURE = Identifier.of("villagesreborn", "textures/gui/speech_bubble.png");
    
    // Animation properties
    private float animationProgress = 0.0f;
    private static final float ANIMATION_SPEED = 0.1f;
    private boolean isAnimatingIn = false;
    private boolean isAnimatingOut = false;
    private long lastAnimationTime = 0;
    private long lastCultureFetchTime = 0;
    private static final long CULTURE_FETCH_COOLDOWN = 30000; // 30 seconds between fetches
    
    // Cached villager data
    private Map<UUID, String> cultureCache = new HashMap<>();
    private Map<UUID, Integer> recentInteractions = new HashMap<>();
    private String currentMood = "neutral";
    
    // Friendship colors for different levels
    private static final int[] FRIENDSHIP_COLORS = {
        0xFFFF3333, // Level 1-2 (Red)
        0xFFFF9933, // Level 3-4 (Orange)
        0xFFFFFF33, // Level 5-6 (Yellow)
        0xFF99FF33, // Level 7-8 (Light Green)
        0xFF33FF33  // Level 9-10 (Green)
    };
    
    // Singleton instance
    private static ConversationHud instance;
    
    /**
     * Gets the singleton instance of the ConversationHud
     */
    public static ConversationHud getInstance() {
        if (instance == null) {
            instance = new ConversationHud();
        }
        return instance;
    }
    
    private ConversationHud() {
        // Private constructor for singleton pattern
    }
    
    private int getFriendshipColor(int level) {
        int index = Math.min((level - 1) / 2, FRIENDSHIP_COLORS.length - 1);
        return FRIENDSHIP_COLORS[Math.max(0, index)];
    }
    
    private int getFriendshipLevel(VillagerEntity villager) {
        VillagerAI ai = VillagerManager.getInstance().getVillagerAI(villager.getUuid());
        return ai != null ? ai.getFriendshipLevel() : 0;
    }
    
    // Available positions for the conversation indicator
    public enum HudPosition {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT
    }
    
    /**
     * Start or update a conversation with a villager
     * 
     * @param villager The villager being spoken to
     */
    public void startConversation(VillagerEntity villager) {
        if (villager == null) return;
        
        // If we're showing a different villager, animate out first
        if (isVisible && currentVillager != null && !currentVillager.getUuid().equals(villager.getUuid())) {
            endConversation();
        }
        
        this.currentVillager = villager;
        this.isVisible = true;
        this.lastInteractionTime = System.currentTimeMillis();
        
        // Only update start time if this is a new villager conversation
        if (!isAnimatingIn) {
            this.conversationStartTime = System.currentTimeMillis();
            this.isAnimatingIn = true;
            this.isAnimatingOut = false;
            this.animationProgress = 0.0f;
            
            // Increment interaction count for this villager
            UUID villagerId = villager.getUuid();
            recentInteractions.put(villagerId, recentInteractions.getOrDefault(villagerId, 0) + 1);
            
            // Request culture data if needed
            fetchCultureData(villager);
        }
        
        // Update mood based on villager state
        updateMood(villager);
    }
    
    /**
     * End the current conversation with animation
     */
    public void endConversation() {
        if (!isVisible) return;
        
        this.isAnimatingOut = true;
        this.isAnimatingIn = false;
    }
    
    /**
     * Immediately end conversation without animation
     */
    public void forceEndConversation() {
        this.isVisible = false;
        this.currentVillager = null;
        this.isAnimatingIn = false;
        this.isAnimatingOut = false;
        this.animationProgress = 0.0f;
    }
    
    /**
     * Change the display position of the conversation indicator
     * 
     * @param position The new position
     */
    public void setPosition(HudPosition position) {
        this.position = position;
        
        // Also update the config
        VillagesConfig.UISettings uiSettings = VillagesConfig.getInstance().getUISettings();
        uiSettings.setConversationHudPosition(position.name());
        VillagesConfig.getInstance().save();
    }
    
    /**
     * Get the current position setting
     */
    public HudPosition getPosition() {
        return this.position;
    }
    
    /**
     * Renders the conversation indicator if active
     * 
     * @param context The draw context
     * @param client The Minecraft client instance
     */
    public void render(DrawContext context, MinecraftClient client) {
        // Process animations
        processAnimations();
        
        // Check for timeout
        if (isVisible && !isAnimatingOut && System.currentTimeMillis() - lastInteractionTime > CONVERSATION_TIMEOUT) {
            endConversation();
        }
        
        if ((!isVisible && !isAnimatingOut) || currentVillager == null) {
            return;
        }
        
        // If animation complete on exit, clean up
        if (isAnimatingOut && animationProgress <= 0.0f) {
            forceEndConversation();
            return;
        }
        
        // Get UI settings
        VillagesConfig.UISettings uiSettings = VillagesConfig.getInstance().getUISettings();
        
        // Set position from config if different
        HudPosition configPosition = HudPosition.valueOf(uiSettings.getConversationHudPosition());
        if (configPosition != position) {
            position = configPosition;
        }
        
        TextRenderer textRenderer = client.textRenderer;
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        
        // Get villager information
        String villagerName = currentVillager.getName().getString();
        String culture = getCulture(currentVillager);
        String profession = currentVillager.getVillagerData().getProfession().toString();
        profession = formatProfession(profession);

        // Get friendship information
        VillagerAI villagerAI = VillagerManager.getInstance().getVillagerAI(currentVillager.getUuid());
        int friendshipLevel = villagerAI != null ? villagerAI.getFriendshipLevel() : 0;
        String friendshipText = String.format("Friendship: %d/10", friendshipLevel);
        
        // Format the label with villager name
        String labelFormat = uiSettings.getConversationLabelFormat();
        String label = labelFormat.replace("{name}", villagerName);
        
        // Calculate text dimensions
        int textWidth = textRenderer.getWidth(label);
        int detailsWidth = 0;
        
        // Calculate details width if shown
        if (uiSettings.isShowCulture() && uiSettings.isShowProfession()) {
            detailsWidth = textRenderer.getWidth(culture + " " + profession);
        } else if (uiSettings.isShowCulture()) {
            detailsWidth = textRenderer.getWidth(culture);
        } else if (uiSettings.isShowProfession()) {
            detailsWidth = textRenderer.getWidth(profession);
        }

        int friendshipWidth = textRenderer.getWidth(friendshipText);
        int boxWidth = Math.max(Math.max(textWidth, detailsWidth), friendshipWidth) + 30; // Extra space for icon
        int boxHeight = (uiSettings.isShowCulture() || uiSettings.isShowProfession()) ? 52 : 36; // Extra height for details
        
        // Apply animation scaling
        boxWidth = (int)(boxWidth * animationProgress);
        boxHeight = (int)(boxHeight * animationProgress);
        
        if (boxWidth <= 0 || boxHeight <= 0) return;
        
        // Calculate position based on user preference
        int x, y;
        switch (position) {
            case TOP_LEFT:
                x = 10;
                y = 10;
                break;
            case TOP_RIGHT:
                x = screenWidth - boxWidth - 10;
                y = 10;
                break;
            case BOTTOM_LEFT:
                x = 10;
                y = screenHeight - boxHeight - 10;
                break;
            case BOTTOM_RIGHT:
            default:
                x = screenWidth - boxWidth - 10;
                y = screenHeight - boxHeight - 10;
                break;
        }
        
        // Slide animation based on position
        if (isAnimatingIn || isAnimatingOut) {
            switch (position) {
                case TOP_LEFT:
                    x = (int)(x - (1.0f - animationProgress) * boxWidth);
                    break;
                case TOP_RIGHT:
                    x = (int)(x + (1.0f - animationProgress) * boxWidth);
                    break;
                case BOTTOM_LEFT:
                    y = (int)(y + (1.0f - animationProgress) * boxHeight);
                    break;
                case BOTTOM_RIGHT:
                    y = (int)(y + (1.0f - animationProgress) * boxHeight);
                    break;
            }
        }
        
        // Draw background with configured colors
        int backgroundColor = uiSettings.getBackgroundColor();
        int borderColor = uiSettings.getBorderColor();
        
        // Apply alpha based on animation
        backgroundColor = applyAlpha(backgroundColor, (int)(255 * animationProgress));
        borderColor = applyAlpha(borderColor, (int)(255 * animationProgress));
        
        // Create a rounded or shaped background
        drawRoundedRect(context, x, y, boxWidth, boxHeight, 6, backgroundColor, borderColor);
        
        // Draw text with dynamic opacity based on animation
        int textX = x + 10;
        int textY = y + 5;
        
        // Try to draw conversation icon if texture exists (16x16 pixels)
        try {
            boolean hasIcon = MinecraftClient.getInstance().getResourceManager().getResource(ICON_TEXTURE).isPresent();
            if (hasIcon) {
                // Updated ICON_TEXTURE rendering
                context.drawGuiTexture(ICON_TEXTURE, x + 5, y + 5, 16, 16);
                textX = x + 26;    // Move text to right of icon
            }
        } catch (Exception e) {
            // Texture not found, continue without icon
        }
        
        // Draw formatted label (includes villager name) in configured color
        int labelColor = applyAlpha(uiSettings.getLabelColor(), (int)(255 * animationProgress));
        context.drawTextWithShadow(textRenderer, 
            Text.literal(label).formatted(Formatting.WHITE), 
            textX, textY, labelColor);
        
        // Draw status icon if villager is speaking
        if (currentMood.equals("speaking")) {
            try {
                boolean hasBubble = MinecraftClient.getInstance().getResourceManager().getResource(SPEECH_BUBBLE_TEXTURE).isPresent();
                if (hasBubble) {
                    int bubbleSize = 10;
                    // Updated SPEECH_BUBBLE_TEXTURE rendering
                    context.drawGuiTexture(SPEECH_BUBBLE_TEXTURE, textX + textWidth + 4, textY + 3, bubbleSize, bubbleSize);
                }
            } catch (Exception e) {
                // Texture not found, continue without icon
            }
        }
        
        // Draw culture and profession on second line if enabled
        if (animationProgress > 0.5f) { // Only show details after animation is halfway
            int detailsY = textY + 16;
            int detailsX = textX;
            
            if (uiSettings.isShowCulture()) {
                int cultureColor = applyAlpha(0xFFFFFFFF, (int)(255 * animationProgress));
                context.drawTextWithShadow(textRenderer, 
                    Text.literal(culture).formatted(getCultureFormatting(culture)), 
                    detailsX, detailsY, cultureColor);
                
                detailsX += textRenderer.getWidth(culture);
            }
                
            if (uiSettings.isShowProfession()) {
                int professionColor = applyAlpha(0xFFAAAAAA, (int)(255 * animationProgress));
                context.drawTextWithShadow(textRenderer,
                    Text.literal((uiSettings.isShowCulture() ? " " : "") + profession).formatted(Formatting.GRAY),
                    detailsX, detailsY, professionColor);
            }

            // Draw friendship bar if AI is available
            if (villagerAI != null) {
                int barWidth = boxWidth - 20;
                int barHeight = 4;
                int barY = detailsY + 16;
                int barX = x + 10;

                // Draw background bar
                context.fill(barX, barY, barX + barWidth, barY + barHeight, 
                    applyAlpha(0x80000000, (int)(192 * animationProgress)));

                // Draw filled portion
                int filledWidth = (int)((friendshipLevel / 10.0f) * barWidth);
                int friendshipColor = applyAlpha(getFriendshipColor(friendshipLevel), 
                    (int)(255 * animationProgress));
                context.fill(barX, barY, barX + filledWidth, barY + barHeight, friendshipColor);

                // Draw friendship level text
                int friendshipTextColor = applyAlpha(0xFFFFFFFF, (int)(255 * animationProgress));
                context.drawTextWithShadow(textRenderer,
                    Text.literal(friendshipText).formatted(Formatting.WHITE),
                    barX, barY - 10, friendshipTextColor);
            }
        }
    }
    
    /**
     * Process animations based on current state
     */
    private void processAnimations() {
        long currentTime = System.currentTimeMillis();
        long delta = currentTime - lastAnimationTime;
        
        if (delta < 10) return; // Limit animation updates for performance
        
        lastAnimationTime = currentTime;
        
        if (isAnimatingIn) {
            animationProgress += ANIMATION_SPEED * (delta / 50.0f);
            if (animationProgress >= 1.0f) {
                animationProgress = 1.0f;
                isAnimatingIn = false;
            }
        } else if (isAnimatingOut) {
            animationProgress -= ANIMATION_SPEED * (delta / 50.0f);
            if (animationProgress <= 0.0f) {
                animationProgress = 0.0f;
                isAnimatingOut = false;
                isVisible = false;
            }
        }
    }
    
    /**
     * Apply alpha value to an ARGB color
     */
    private int applyAlpha(int color, int alpha) {
        alpha = Math.max(0, Math.min(255, alpha));
        return (color & 0x00FFFFFF) | (alpha << 24);
    }
    
    /**
     * Draw a rounded rectangle with the given dimensions
     */
    private void drawRoundedRect(DrawContext context, int x, int y, int width, int height, int radius, int fillColor, int borderColor) {
        // Simple implementation - could be improved with actual rounded corners
        context.fill(x, y, x + width, y + height, fillColor);
        context.drawBorder(x, y, width, height, borderColor);
    }
    
    /**
     * Update the last interaction time to keep conversation active
     */
    public void updateInteraction() {
        this.lastInteractionTime = System.currentTimeMillis();
        
        // Also update the mood
        if (currentVillager != null) {
            updateMood(currentVillager);
        }
    }
    
    /**
     * Update the mood display based on villager state
     */
    private void updateMood(VillagerEntity villager) {
        if (villager == null) return;
        
        // In a real implementation, this would check various factors:
        // - Is the villager actively speaking (animation playing)
        // - Is the villager damaged or in danger
        // - Time of day, weather, etc.
        
        // For now, rotate between moods based on interaction count as a placeholder
        int interactions = recentInteractions.getOrDefault(villager.getUuid(), 0);
        
        if (villager.hurtTime > 0) {
            currentMood = "hurt";
        } else if (interactions % 3 == 0) {
            currentMood = "neutral";
        } else if (interactions % 3 == 1) {
            currentMood = "speaking";
        } else {
            currentMood = "happy";
        }
    }
    
    /**
     * Update the villager's culture data from the network
     * 
     * @param villagerUuid The UUID of the villager
     * @param culture The culture name from the server
     */
    public void updateVillagerCulture(UUID villagerUuid, String culture) {
        if (culture != null && !culture.isEmpty()) {
            cultureCache.put(villagerUuid, culture);
        }
    }
    
    /**
     * Update the villager's mood from the network
     * 
     * @param mood The mood name from the server
     */
    public void updateVillagerMood(String mood) {
        if (mood != null && !mood.isEmpty()) {
            this.currentMood = mood;
        }
    }
    
    /**
     * Fetch culture data from server or cache
     */
    private void fetchCultureData(VillagerEntity villager) {
        if (villager == null) return;
        
        UUID villagerId = villager.getUuid();
        long currentTime = System.currentTimeMillis();
        
        // Check if we've recently fetched culture data
        if (currentTime - lastCultureFetchTime < CULTURE_FETCH_COOLDOWN && cultureCache.containsKey(villagerId)) {
            return;
        }
        
        // Reset the timer and try to fetch from server
        lastCultureFetchTime = currentTime;
        
        // Request data from server if multiplayer
        if (MinecraftClient.getInstance().getNetworkHandler() != null) {
            VillagesClientNetwork.requestVillagerCultureData(villagerId);
        }
        
        // If not already cached, use fallback calculation while waiting for server response
        if (!cultureCache.containsKey(villagerId)) {
            cultureCache.put(villagerId, getCultureFallback(villager));
        }
    }
    
    /**
     * Get culture from cache or compute fallback
     */
    private String getCulture(VillagerEntity villager) {
        if (villager == null) return "Unknown";
        
        UUID villagerId = villager.getUuid();
        
        // Check cache first
        if (cultureCache.containsKey(villagerId)) {
            return cultureCache.get(villagerId);
        }
        
        // Fallback to local calculation
        return getCultureFallback(villager);
    }
    
    /**
     * Fallback method when server data isn't available
     */
    private String getCultureFallback(VillagerEntity villager) {
        // Basic implementation based on location hash
        int posHash = villager.getBlockPos().hashCode();
        
        // Determine culture based on hash to be consistent for same locations
        switch (Math.abs(posHash) % 4) {
            case 0: return "Roman";
            case 1: return "Egyptian";
            case 2: return "Victorian";
            case 3: return "NYC";
            default: return "Unknown";
        }
    }
    
    private Formatting getCultureFormatting(String culture) {
        switch (culture) {
            case "Roman": return Formatting.RED;
            case "Egyptian": return Formatting.YELLOW;
            case "Victorian": return Formatting.DARK_PURPLE;
            case "NYC": return Formatting.AQUA;
            default: return Formatting.WHITE;
        }
    }
    
    private String formatProfession(String profession) {
        // Convert SNAKE_CASE to Title Case
        if (profession == null || profession.isEmpty()) {
            return "Villager";
        }
        
        // Remove the "minecraft:" prefix if present
        if (profession.contains(":")) {
            profession = profession.split(":")[1];
        }
        
        // Convert SNAKE_CASE to Title Case
        String[] words = profession.toLowerCase().split("_");
        StringBuilder result = new StringBuilder();
        
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                      .append(word.substring(1))
                      .append(" ");
            }
        }
        
        return result.toString().trim();
    }
    
    /**
     * Check if a conversation is currently active
     */
    public boolean isConversationActive() {
        return isVisible && currentVillager != null;
    }
    
    /**
     * Get the current villager being spoken to
     */
    public VillagerEntity getCurrentVillager() {
        return currentVillager;
    }
}