package com.beeny.client.gui;

import com.beeny.network.VillagerTeleportPacket;
import com.beeny.util.VillagerNames;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VillagerJournalScreen extends Screen {
    // Constants for magic numbers
    private static final int BACKGROUND_WIDTH = 320;
    private static final int BACKGROUND_HEIGHT = 240;
    private static final int ENTRY_HEIGHT = 40;
    private static final int MARGIN = 10;
    private static final int MAX_VISIBLE_ENTRIES = 5;
    private static final int DISTANCE_UPDATE_INTERVAL = 20; // ticks
    private static final int PROFESSION_ICON_SIZE = 10;
    private static final int PULSE_DISTANCE_THRESHOLD = 10;
    
    // Profession color cache for consistent coloring
    private static final Map<String, Integer> PROFESSION_COLORS = new HashMap<>();
    static {
        PROFESSION_COLORS.put("farmer", 0xFF8B4513);
        PROFESSION_COLORS.put("librarian", 0xFF9932CC);
        PROFESSION_COLORS.put("priest", 0xFFFFD700);
        PROFESSION_COLORS.put("cleric", 0xFFFFD700);
        PROFESSION_COLORS.put("blacksmith", 0xFF708090);
        PROFESSION_COLORS.put("toolsmith", 0xFF708090);
        PROFESSION_COLORS.put("weaponsmith", 0xFF708090);
        PROFESSION_COLORS.put("armorer", 0xFF708090);
        PROFESSION_COLORS.put("butcher", 0xFFDC143C);
        PROFESSION_COLORS.put("nitwit", 0xFF808080);
    }
    
    private final List<VillagerEntry> villagerEntries;
    private final ButtonWidget[] villagerButtons; // Fixed-size button array
    private ButtonWidget scrollUpButton;
    private ButtonWidget scrollDownButton;
    private ButtonWidget closeButton;
    
    // Pre-calculated journal positions
    private int journalX;
    private int journalY;
    private int entriesStartX;
    private int entriesStartY;
    
    // State management
    private float animationTick = 0;
    private int scrollOffset = 0;
    private int ticksSinceLastUpdate = 0;
    
    public VillagerJournalScreen(List<VillagerEntity> villagers) {
        super(Text.literal("Villager Journal"));
        this.villagerEntries = new ArrayList<>();
        this.villagerButtons = new ButtonWidget[MAX_VISIBLE_ENTRIES];
        
        for (VillagerEntity villager : villagers) {
            villagerEntries.add(new VillagerEntry(villager));
        }
    }
    
    @Override
    protected void init() {
        super.init();
        calculateJournalPositions();
        createStaticButtons();
        updateVillagerButtons();
    }
    
    private void calculateJournalPositions() {
        journalX = (width - BACKGROUND_WIDTH) / 2;
        journalY = (height - BACKGROUND_HEIGHT) / 2;
        entriesStartX = journalX + MARGIN;
        entriesStartY = journalY + 40;
    }
    
    private void createStaticButtons() {
        // Create fixed button array without recreation
        for (int i = 0; i < MAX_VISIBLE_ENTRIES; i++) {
            final int buttonIndex = i;
            villagerButtons[i] = ButtonWidget.builder(
                Text.empty(),
                btn -> teleportToVillager(scrollOffset + buttonIndex)
            ).dimensions(entriesStartX, entriesStartY + (i * ENTRY_HEIGHT), 
                        BACKGROUND_WIDTH - 2 * MARGIN, 30).build();
        }
        
        // Persistent scroll buttons with state management
        scrollUpButton = ButtonWidget.builder(
            Text.literal("↑"),
            btn -> scrollUp()
        ).dimensions(width / 2 + BACKGROUND_WIDTH / 2 - 20, entriesStartY - 25, 20, 20).build();
        
        scrollDownButton = ButtonWidget.builder(
            Text.literal("↓"),
            btn -> scrollDown()
        ).dimensions(width / 2 + BACKGROUND_WIDTH / 2 - 20, entriesStartY + MAX_VISIBLE_ENTRIES * ENTRY_HEIGHT, 20, 20).build();
        
        closeButton = ButtonWidget.builder(
            Text.literal("Close"),
            btn -> close()
        ).dimensions(width / 2 - 50, (height + BACKGROUND_HEIGHT) / 2 - 30, 100, 20).build();
        
        addDrawableChild(closeButton);
    }
    
    private void updateVillagerButtons() {
        // Clear existing buttons
        for (ButtonWidget button : villagerButtons) {
            remove(button);
        }
        remove(scrollUpButton);
        remove(scrollDownButton);
        
        // Add visible villager buttons with efficient position-based indexing
        int visibleCount = Math.min(villagerEntries.size() - scrollOffset, MAX_VISIBLE_ENTRIES);
        for (int i = 0; i < visibleCount; i++) {
            VillagerEntry entry = villagerEntries.get(i + scrollOffset);
            villagerButtons[i].setMessage(Text.literal(entry.name).formatted(Formatting.AQUA));
            addDrawableChild(villagerButtons[i]);
        }
        
        // Add scroll buttons if needed
        if (scrollOffset > 0) {
            addDrawableChild(scrollUpButton);
        }
        if (scrollOffset + MAX_VISIBLE_ENTRIES < villagerEntries.size()) {
            addDrawableChild(scrollDownButton);
        }
    }
    
    // Dedicated scroll methods with bounds checking
    private void scrollUp() {
        if (scrollOffset > 0) {
            scrollOffset = Math.max(0, scrollOffset - 1);
            updateVillagerButtons();
            playClickSound();
        }
    }
    
    private void scrollDown() {
        if (scrollOffset + MAX_VISIBLE_ENTRIES < villagerEntries.size()) {
            scrollOffset = Math.min(villagerEntries.size() - MAX_VISIBLE_ENTRIES, scrollOffset + 1);
            updateVillagerButtons();
            playClickSound();
        }
    }
    
    @Override
    public void tick() {
        super.tick();
        ticksSinceLastUpdate++;
        
        // Distance update throttling (every 20 ticks / 1 second)
        if (ticksSinceLastUpdate >= DISTANCE_UPDATE_INTERVAL) {
            updateVillagerDistances();
            ticksSinceLastUpdate = 0;
        }
    }
    
    private void updateVillagerDistances() {
        if (client == null || client.player == null) return;
        
        for (VillagerEntry entry : villagerEntries) {
            // Safe villager entity lookup with error handling
            Entity entity = client.world.getEntityById(entry.villagerId);
            if (entity instanceof VillagerEntity villager) {
                entry.distance = (int) villager.getPos().distanceTo(client.player.getPos());
            } else {
                entry.distance = -1; // Handling of missing or non-villager entities
            }
        }
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        animationTick += delta;
        
        // Better background glow animation
        renderJournalBackground(context);
        renderAnimatedTitle(context);
        renderVillagerEntries(context, mouseX, mouseY);
        
        super.render(context, mouseX, mouseY, delta);
        
        // Single tooltip rendering optimization
        renderSingleTooltip(context, mouseX, mouseY);
    }
    
    private void renderJournalBackground(DrawContext context) {
        float glowIntensity = (MathHelper.sin(animationTick * 0.1f) + 1) * 0.1f + 0.8f;
        int glowColor = (int)(255 * glowIntensity) << 24 | 0x4A90E2;
        
        context.fill(journalX - 2, journalY - 2, journalX + BACKGROUND_WIDTH + 2, journalY + BACKGROUND_HEIGHT + 2, glowColor);
        context.fill(journalX, journalY, journalX + BACKGROUND_WIDTH, journalY + BACKGROUND_HEIGHT, 0xFF2C3E50);
    }
    
    private void renderAnimatedTitle(DrawContext context) {
        float titleHue = (animationTick * 2) % 360;
        int titleColor = MathHelper.hsvToRgb(titleHue / 360f, 0.7f, 1.0f) | 0xFF000000;
        
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("📖 Villager Journal 📖"),
            width / 2, journalY + 10, titleColor);
    }
    
    private void renderVillagerEntries(DrawContext context, int mouseX, int mouseY) {
        int visibleCount = Math.min(villagerEntries.size() - scrollOffset, MAX_VISIBLE_ENTRIES);
        
        for (int i = 0; i < visibleCount; i++) {
            VillagerEntry entry = villagerEntries.get(i + scrollOffset);
            int entryY = entriesStartY + (i * ENTRY_HEIGHT);
            
            // Improved entry hover detection
            boolean isHovered = isMouseOverEntry(mouseX, mouseY, i);
            
            int backgroundColor = isHovered ? 0x55FFFFFF : 0x33FFFFFF;
            context.fill(entriesStartX, entryY, entriesStartX + BACKGROUND_WIDTH - 2 * MARGIN, entryY + 30, backgroundColor);
            
            // Profession icon with cached colors
            int professionColor = PROFESSION_COLORS.getOrDefault(entry.profession.toLowerCase(), 0xFF4169E1);
            context.fill(entriesStartX + 5, entryY + 5, entriesStartX + 5 + PROFESSION_ICON_SIZE, 
                        entryY + 5 + PROFESSION_ICON_SIZE, professionColor);
            
            // Visual pulse effect for nearby villagers
            if (entry.distance != -1 && entry.distance <= PULSE_DISTANCE_THRESHOLD) {
                float pulse = (MathHelper.sin(animationTick * 0.3f) + 1) * 0.5f;
                int pulseColor = (int)(255 * pulse) << 24 | 0x00FF00;
                context.fill(entriesStartX + BACKGROUND_WIDTH - 2 * MARGIN - 15, entryY + 5, 
                           entriesStartX + BACKGROUND_WIDTH - 2 * MARGIN - 5, entryY + 15, pulseColor);
            }
        }
    }
    
    // Helper method for improved entry hover detection
    private boolean isMouseOverEntry(int mouseX, int mouseY, int entryIndex) {
        int entryY = entriesStartY + (entryIndex * ENTRY_HEIGHT);
        return mouseX >= entriesStartX && mouseX <= entriesStartX + BACKGROUND_WIDTH - 2 * MARGIN &&
               mouseY >= entryY && mouseY <= entryY + 30;
    }
    
    // Single tooltip rendering optimization
    private void renderSingleTooltip(DrawContext context, int mouseX, int mouseY) {
        int visibleCount = Math.min(villagerEntries.size() - scrollOffset, MAX_VISIBLE_ENTRIES);
        
        for (int i = 0; i < visibleCount; i++) {
            if (isMouseOverEntry(mouseX, mouseY, i)) {
                VillagerEntry entry = villagerEntries.get(i + scrollOffset);
                
                // Efficient tooltip building
                List<Text> tooltip = buildTooltip(entry);
                context.drawTooltip(textRenderer, tooltip, mouseX, mouseY);
                break; // Single tooltip only
            }
        }
    }
    
    // Efficient tooltip building
    private List<Text> buildTooltip(VillagerEntry entry) {
        List<Text> tooltip = new ArrayList<>(5);
        tooltip.add(Text.literal("Name: " + entry.name).formatted(Formatting.AQUA));
        tooltip.add(Text.literal("Profession: " + capitalize(entry.profession)).formatted(Formatting.YELLOW));
        tooltip.add(Text.literal("Level: " + entry.level).formatted(Formatting.GREEN));
        
        if (entry.distance == -1) {
            tooltip.add(Text.literal("Distance: Unknown (villager missing)").formatted(Formatting.RED));
        } else {
            tooltip.add(Text.literal("Distance: " + entry.distance + " blocks").formatted(Formatting.GRAY));
        }
        
        tooltip.add(Text.literal("Click to teleport!").formatted(Formatting.LIGHT_PURPLE));
        return tooltip;
    }
    
    // Helper method for capitalized profession names
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
    
    private void teleportToVillager(int entryIndex) {
        if (entryIndex >= 0 && entryIndex < villagerEntries.size() && client != null && client.player != null) {
            VillagerEntry entry = villagerEntries.get(entryIndex);
            
            // Null checks for client state
            if (entry.distance == -1) {
                // Don't teleport to missing villagers
                return;
            }
            
            playClickSound();
            VillagerTeleportPacket.sendToServer(entry.villagerId);
            close();
        }
    }
    
    private void playClickSound() {
        if (client != null) {
            client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0f, 0.8f));
        }
    }
    
    @Override
    public boolean shouldPause() {
        return false;
    }
    
    private static class VillagerEntry {
        final String name;
        final String profession;
        final int level;
        final int villagerId;
        int distance; // Mutable for periodic updates
        
        VillagerEntry(VillagerEntity villager) {
            this.name = getVillagerName(villager);
            this.profession = villager.getVillagerData().getProfession().toString().toLowerCase();
            this.level = villager.getVillagerData().getLevel();
            this.villagerId = villager.getId();
            this.distance = 0; // Will be calculated during updates
        }
        
        private String getVillagerName(VillagerEntity villager) {
            return VillagerNames.getVillagerName(villager);
        }
    }
}