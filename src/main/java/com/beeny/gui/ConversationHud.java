package com.beeny.gui;

import com.beeny.config.VillagesConfig;
import com.beeny.village.VillagerManager;
import com.beeny.village.VillagerAI;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

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
    private final Identifier ICON_TEXTURE = Identifier.of("villagesreborn", "textures/gui/conversation_icon.png");
    
    // Friendship colors for different levels
    private static final int[] FRIENDSHIP_COLORS = {
        0xFFFF3333, // Level 1-2 (Red)
        0xFFFF9933, // Level 3-4 (Orange)
        0xFFFFFF33, // Level 5-6 (Yellow)
        0xFF99FF33, // Level 7-8 (Light Green)
        0xFF33FF33  // Level 9-10 (Green)
    };
    
    private int getFriendshipColor(int level) {
        int index = Math.min((level - 1) / 2, FRIENDSHIP_COLORS.length - 1);
        return FRIENDSHIP_COLORS[Math.max(0, index)];
    }
    
    private int getFriendshipLevel(VillagerEntity villager) {
        return VillagerManager.getInstance().getVillagerAI(villager.getUuid()).getFriendshipLevel();
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
        
        this.currentVillager = villager;
        this.isVisible = true;
        this.lastInteractionTime = System.currentTimeMillis();
        
        // Only update start time if this is a new villager
        if (currentVillager != villager) {
            this.conversationStartTime = System.currentTimeMillis();
        }
    }
    
    /**
     * End the current conversation
     */
    public void endConversation() {
        this.isVisible = false;
        this.currentVillager = null;
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
        uiSettings.conversationHudPosition = position.name();
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
        // Check for timeout
        if (isVisible && System.currentTimeMillis() - lastInteractionTime > CONVERSATION_TIMEOUT) {
            endConversation();
        }
        
        if (!isVisible || currentVillager == null) {
            return;
        }
        
        // Get UI settings
        VillagesConfig.UISettings uiSettings = VillagesConfig.getInstance().getUISettings();
        
        // Set position from config if different
        HudPosition configPosition = HudPosition.valueOf(uiSettings.conversationHudPosition);
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
        String labelFormat = uiSettings.conversationLabelFormat;
        String label = labelFormat.replace("{name}", villagerName);
        
        // Calculate text dimensions
        int textWidth = textRenderer.getWidth(label);
        int detailsWidth = 0;
        
        // Calculate details width if shown
        if (uiSettings.showCulture && uiSettings.showProfession) {
            detailsWidth = textRenderer.getWidth(culture + " " + profession);
        } else if (uiSettings.showCulture) {
            detailsWidth = textRenderer.getWidth(culture);
        } else if (uiSettings.showProfession) {
            detailsWidth = textRenderer.getWidth(profession);
        }

        int friendshipWidth = textRenderer.getWidth(friendshipText);
        int boxWidth = Math.max(Math.max(textWidth, detailsWidth), friendshipWidth) + 30; // Extra space for icon
        int boxHeight = (uiSettings.showCulture || uiSettings.showProfession) ? 52 : 36; // Extra height for friendship bar
        
        
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
        
        // Draw background with configured colors
        int backgroundColor = uiSettings.backgroundColor;
        int borderColor = uiSettings.borderColor;
        
        context.fill(x, y, x + boxWidth, y + boxHeight, backgroundColor);
        context.drawBorder(x, y, boxWidth, boxHeight, borderColor);
        
        // Draw conversation icon (16x16 pixels) - if texture exists
        // context.drawTexture(ICON_TEXTURE, x + 5, y + 10, 0, 0, 16, 16, 16, 16);
        
        // Draw text
        int textX = x + 5;
        int textY = y + 5;
        
        // Draw formatted label (includes villager name) in configured color
        context.drawText(textRenderer, 
            Text.literal(label).formatted(Formatting.WHITE), 
            textX, textY, uiSettings.labelColor, true);
        
        // Draw culture and profession on second line if enabled
        int detailsY = textY + 16;
        int detailsX = textX;
        
        if (uiSettings.showCulture) {
            context.drawText(textRenderer, 
                Text.literal(culture).formatted(getCultureFormatting(culture)), 
                detailsX, detailsY, 0xFFFFFFFF, true);
            
            detailsX += textRenderer.getWidth(culture);
        }
            
        if (uiSettings.showProfession) {
            context.drawText(textRenderer,
                Text.literal((uiSettings.showCulture ? " " : "") + profession).formatted(Formatting.GRAY),
                detailsX, detailsY, 0xFFFFFFFF, true);
        }

        // Draw friendship bar if AI is available
        if (villagerAI != null) {
            int barWidth = boxWidth - 10;
            int barHeight = 4;
            int barY = detailsY + 16;
            int barX = x + 5;

            // Draw background bar
            context.fill(barX, barY, barX + barWidth, barY + barHeight, 0x80000000);

            // Draw filled portion
            int filledWidth = (int)((friendshipLevel / 10.0f) * barWidth);
            int friendshipColor = getFriendshipColor(friendshipLevel);
            context.fill(barX, barY, barX + filledWidth, barY + barHeight, friendshipColor);

            // Draw friendship level text
            context.drawText(textRenderer,
                Text.literal(friendshipText).formatted(Formatting.WHITE),
                barX, barY - 10, 0xFFFFFFFF, true);
        }
    }
    
    /**
     * Update the last interaction time to keep conversation active
     */
    public void updateInteraction() {
        this.lastInteractionTime = System.currentTimeMillis();
    }
    
    private String getCulture(VillagerEntity villager) {
        // This would ideally come from the VillagerManager, 
        // but for client-side rendering we'll determine from location/biome
        // For now, use a basic implementation
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