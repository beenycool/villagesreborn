package com.beeny.gui;

import com.beeny.village.SpawnRegion;
import com.beeny.village.VillagerAI;
import com.beeny.village.VillagerManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Debug screen for Villages Reborn mod.
 * Shows detailed information about villages and villagers.
 */
public class VillageDebugScreen extends Screen {
    private static final int BACKGROUND_COLOR = 0x88000000;
    private static final int BORDER_COLOR = 0xFF444444;
    private static final int TITLE_COLOR = 0xFFFFDD00;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    
    private List<String> villageStats = new ArrayList<>();
    private int scrollOffset = 0;
    private int maxScroll = 0;

    public VillageDebugScreen() {
        super(Text.literal("Village Debug"));
    }

    @Override
    protected void init() {
        super.init();
        updateVillageStats();
        maxScroll = Math.max(0, villageStats.size() - (height / 12));
    }
    
    private void updateVillageStats() {
        villageStats.clear();
        villageStats.add("=== Villages Reborn Debug Information ===");
        villageStats.add("");
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            villageStats.add("Player or world not available");
            return;
        }
        
        BlockPos playerPos = client.player.getBlockPos();
        villageStats.add("Player Position: " + playerPos.getX() + ", " + playerPos.getY() + ", " + playerPos.getZ());
        
        // Add server-side information (this will be empty in single-player preview)
        villageStats.add("");
        villageStats.add("=== Village Regions ===");
        
        Collection<SpawnRegion> regions = VillagerManager.getInstance().getSpawnRegions();
        if (regions.isEmpty()) {
            villageStats.add("No village regions registered");
        } else {
            for (SpawnRegion region : regions) {
                BlockPos center = region.getCenter();
                double distance = Math.sqrt(center.getSquaredDistance(playerPos));
                villageStats.add(String.format(
                    "Village: %s (at %d,%d,%d, radius: %d, distance: %.1f)",
                    region.getCulture(), center.getX(), center.getY(), center.getZ(), 
                    region.getRadius(), distance
                ));
                
                VillagerManager.VillageStats stats = VillagerManager.getInstance().getVillageStats(center);
                if (stats != null) {
                    villageStats.add(String.format("  Prosperity: %d, Safety: %d", stats.prosperity, stats.safety));
                }
                
                villageStats.add(String.format("  Structures: %d, POIs: %d", 
                    region.getCulturalStructures().size(),
                    region.getPointsOfInterest().size()));
            }
        }
        
        // Add villager information
        villageStats.add("");
        villageStats.add("=== Active Villagers ===");
        Collection<VillagerAI> villagers = VillagerManager.getInstance().getActiveVillagers();
        int villagerCount = villagers.size();
        villageStats.add(String.format("Total count: %d", villagerCount));
        
        if (!villagers.isEmpty()) {
            int nearby = 0;
            for (VillagerAI ai : villagers) {
                if (ai.getVillager().getBlockPos().isWithinDistance(playerPos, 50)) {
                    nearby++;
                    villageStats.add(String.format("  %s (%s) - %s",
                        ai.getVillager().getName().getString(),
                        ai.getVillager().getVillagerData().getProfession().toString(),
                        ai.getCurrentActivity()));
                }
            }
            
            if (nearby == 0) {
                villageStats.add("  No villagers within 50 blocks");
            }
        }
        
        // Add cultural event information
        villageStats.add("");
        villageStats.add("=== Current Cultural Events ===");
        List<VillagerManager.CulturalEvent> events = VillagerManager.getInstance()
            .getCurrentEvents(playerPos, null); // Changed to only pass null as server world from client is not possible
            
        if (events.isEmpty()) {
            villageStats.add("No active cultural events");
        } else {
            for (VillagerManager.CulturalEvent event : events) {
                villageStats.add(String.format("  %s - %s", event.name, event.description));
            }
        }
        
        // Add performance metrics
        villageStats.add("");
        villageStats.add("=== Performance Metrics ===");
        villageStats.add(String.format("FPS: %d", MinecraftClient.getInstance().getCurrentFps()));
        
        // Add controls help
        villageStats.add("");
        villageStats.add("=== Controls ===");
        villageStats.add("Scroll: Mouse Wheel / Arrow Keys");
        villageStats.add("Close: ESC / Shift+V");
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Draw the default dark background with the standard method
        this.renderBackgroundTexture(context);
        
        // Calculate dimensions
        int screenWidth = this.width;
        int screenHeight = this.height;
        int panelWidth = Math.min(600, screenWidth - 40);
        int panelHeight = Math.min(400, screenHeight - 40);
        int panelX = (screenWidth - panelWidth) / 2;
        int panelY = (screenHeight - panelHeight) / 2;
        
        // Draw panel background with border
        context.fill(panelX - 2, panelY - 2, panelX + panelWidth + 2, panelY + panelHeight + 2, BORDER_COLOR);
        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, BACKGROUND_COLOR);
        
        // Draw title
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        context.drawTextWithShadow(textRenderer, 
            "Villages Reborn Debug", 
            panelX + (panelWidth - textRenderer.getWidth("Villages Reborn Debug")) / 2, 
            panelY + 10, 
            TITLE_COLOR);
        
        // Draw stats content with scrolling
        int lineY = panelY + 30;
        int lineHeight = 12;
        int visibleLines = (panelHeight - 40) / lineHeight;
        
        for (int i = scrollOffset; i < Math.min(scrollOffset + visibleLines, villageStats.size()); i++) {
            String line = villageStats.get(i);
            int color = line.startsWith("===") ? TITLE_COLOR : TEXT_COLOR;
            context.drawTextWithShadow(textRenderer, line, panelX + 10, lineY, color);
            lineY += lineHeight;
        }
        
        // Draw scroll indicator if needed
        if (villageStats.size() > visibleLines) {
            int scrollBarHeight = panelHeight - 40;
            int scrollThumbHeight = Math.max(20, scrollBarHeight * visibleLines / villageStats.size());
            int scrollThumbY = panelY + 20 + (scrollBarHeight - scrollThumbHeight) * scrollOffset / maxScroll;
            
            // Draw scroll track
            context.fill(
                panelX + panelWidth - 12,
                panelY + 20,
                panelX + panelWidth - 8,
                panelY + panelHeight - 20,
                0x44FFFFFF
            );
            
            // Draw scroll thumb
            context.fill(
                panelX + panelWidth - 12,
                scrollThumbY,
                panelX + panelWidth - 8,
                scrollThumbY + scrollThumbHeight,
                0xCCFFFFFF
            );
        }
        
        super.render(context, mouseX, mouseY, delta);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (maxScroll > 0) {
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int)verticalAmount * 3));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Handle arrow keys for scrolling
        if (keyCode == 264) { // Down arrow
            if (maxScroll > 0) {
                scrollOffset = Math.min(maxScroll, scrollOffset + 1);
                return true;
            }
        } else if (keyCode == 265) { // Up arrow
            scrollOffset = Math.max(0, scrollOffset - 1);
            return true;
        }
        
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void tick() {
        super.tick();
        
        // Update stats periodically - using frame count instead of tick counter
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.world != null && client.world.getTime() % 20 == 0) {
            updateVillageStats();
            maxScroll = Math.max(0, villageStats.size() - (height / 12));
            
            // Keep scrollOffset within bounds after update
            if (maxScroll > 0 && scrollOffset > maxScroll) {
                scrollOffset = maxScroll;
            }
        }
    }
    
    @Override
    public boolean shouldPause() {
        // Don't pause the game when debug screen is open
        return false;
    }
}