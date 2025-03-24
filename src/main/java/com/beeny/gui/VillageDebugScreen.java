package com.beeny.gui;

import com.beeny.Villagesreborn;
import com.beeny.village.Culture;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Debug screen for Villages Reborn mod.
 * 
 * This screen displays technical information about villages, cultures,
 * and other internal data to help with debugging.
 */
public class VillageDebugScreen extends Screen {
    private final List<String> debugInfo = new ArrayList<>();
    private final MinecraftClient client;
    private int scroll = 0;
    private static final int LINE_HEIGHT = 12;
    
    public VillageDebugScreen() {
        super(Text.of("Village Debug Information"));
        this.client = MinecraftClient.getInstance();
        collectDebugInfo();
    }

    @Override
    protected void init() {
        super.init();
    }

    /**
     * Collects all debug information to be displayed
     */
    private void collectDebugInfo() {
        debugInfo.clear();
        
        // Header
        debugInfo.add("§e=== Villages Reborn Debug Information ===");
        debugInfo.add("");
        
        // Player position
        if (client.player != null) {
            BlockPos playerPos = client.player.getBlockPos();
            debugInfo.add("§aPlayer Position: " + playerPos.getX() + ", " + playerPos.getY() + ", " + playerPos.getZ());
            
            // Get village info for current position
            Optional<Culture> culture = Optional.ofNullable(Villagesreborn.getInstance().getNearestVillageCulture(playerPos));
            
            if (culture.isPresent()) {
                Culture villageCulture = culture.get();
                debugInfo.add("");
                debugInfo.add("§6=== Current Village Information ===");
                debugInfo.add("§aCulture: §f" + villageCulture.getType().getId());
                // Using the culture type's ID as the name
                
                // Get village stats from the appropriate methods
                debugInfo.add("§aType: §f" + villageCulture.getType());
                
                // If these properties aren't directly accessible, you might need to add methods to access them
                // or derive them from other properties
                debugInfo.add("§aTraits: §f" + villageCulture.getTraits().size() + " cultural traits");
                debugInfo.add("§aBiomes: §f" + villageCulture.getPreferredBiomes());
                
                // For center and radius, we'll need placeholders or other ways to access this info
                BlockPos center = Villagesreborn.getInstance().getVillageCenterPos(playerPos);
                if (center != null) {
                    debugInfo.add("§aVillage Center: §f" + center.getX() + ", " + 
                                center.getY() + ", " + 
                                center.getZ());
                    
                    int radius = Villagesreborn.getInstance().getVillageRadius(playerPos);
                    debugInfo.add("§aRadius: §f" + radius + " blocks");
                } else {
                    debugInfo.add("§aVillage Center: §fUnknown");
                    debugInfo.add("§aRadius: §fUnknown");
                }
                
                // For events
                int activeEvents = Villagesreborn.getInstance().getActiveEventCount(playerPos);
                debugInfo.add("§aActive Events: §f" + activeEvents);
            } else {
                debugInfo.add("");
                debugInfo.add("§cNo village detected at current position.");
            }
        }
        
        debugInfo.add("");
        debugInfo.add("§6=== System Information ===");
        debugInfo.add("§aAll Villages: §f" + Villagesreborn.getInstance().getAllVillages().size());
        debugInfo.add("§aMemory Usage: §f" + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024 + "MB / " + 
                     Runtime.getRuntime().totalMemory() / 1024 / 1024 + "MB");
        debugInfo.add("");
        debugInfo.add("§7Press ESC to exit this screen.");
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Use the proper method signature for renderBackground in 1.21.4
        this.renderBackground(context, mouseX, mouseY, delta);
        
        int y = 20 - scroll;
        for (String line : debugInfo) {
            if (y >= 10 && y <= height - 10) {
                context.drawText(client.textRenderer, line, 20, y, 0xFFFFFF, true);
            }
            y += LINE_HEIGHT;
        }
        
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scroll -= verticalAmount * 4 * LINE_HEIGHT;
        scroll = Math.max(0, Math.min(scroll, debugInfo.size() * LINE_HEIGHT - height + 40));
        return true;
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { // ESC key
            this.client.setScreen(null);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    @Override
    public boolean shouldPause() {
        return false;
    }
}