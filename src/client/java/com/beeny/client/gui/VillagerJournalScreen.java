package com.beeny.client.gui;

import com.beeny.network.VillagerTeleportPacket;
import com.beeny.network.RequestVillagerListPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
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
    
    private static final int BACKGROUND_WIDTH = 320;
    private static final int BACKGROUND_HEIGHT = 240;
    private static final int ENTRY_HEIGHT = 50;
    private static final int MARGIN = 10;
    private static final int MAX_VISIBLE_ENTRIES = 5;
    private static final int DISTANCE_UPDATE_INTERVAL = 20; 
    private static final int PROFESSION_ICON_SIZE = 10;
    private static final int PULSE_DISTANCE_THRESHOLD = 10;
    
    
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
    private final ButtonWidget[] villagerButtons; 
    private ButtonWidget scrollUpButton;
    private ButtonWidget scrollDownButton;
    private ButtonWidget closeButton;
    
    
    private int journalX;
    private int journalY;
    private int entriesStartX;
    private int entriesStartY;
    
    
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
    
    public static VillagerJournalScreen createFromPacketData(List<RequestVillagerListPacket.VillagerDataPacket> villagerDataList) {
        VillagerJournalScreen screen = new VillagerJournalScreen();
        
        for (RequestVillagerListPacket.VillagerDataPacket data : villagerDataList) {
            screen.villagerEntries.add(new VillagerEntry(data));
        }
        
        return screen;
    }
    
    private VillagerJournalScreen() {
        super(Text.literal("Villager Journal"));
        this.villagerEntries = new ArrayList<>();
        this.villagerButtons = new ButtonWidget[MAX_VISIBLE_ENTRIES];
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
        
        for (int i = 0; i < MAX_VISIBLE_ENTRIES; i++) {
            final int buttonIndex = i;
            villagerButtons[i] = ButtonWidget.builder(
                Text.empty(),
                btn -> teleportToVillager(scrollOffset + buttonIndex)
            ).dimensions(entriesStartX, entriesStartY + (i * ENTRY_HEIGHT), 
                        BACKGROUND_WIDTH - 2 * MARGIN, 30).build();
        }
        
        
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
        
        for (ButtonWidget button : villagerButtons) {
            remove(button);
        }
        remove(scrollUpButton);
        remove(scrollDownButton);
        
        
        int visibleCount = Math.min(villagerEntries.size() - scrollOffset, MAX_VISIBLE_ENTRIES);
        for (int i = 0; i < visibleCount; i++) {
            VillagerEntry entry = villagerEntries.get(i + scrollOffset);
            String buttonText = formatVillagerInfo(entry);
            villagerButtons[i].setMessage(Text.literal(buttonText));
            addDrawableChild(villagerButtons[i]);
        }
        
        
        if (scrollOffset > 0) {
            addDrawableChild(scrollUpButton);
        }
        if (scrollOffset + MAX_VISIBLE_ENTRIES < villagerEntries.size()) {
            addDrawableChild(scrollDownButton);
        }
    }
    
    
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
        
        
        if (ticksSinceLastUpdate >= DISTANCE_UPDATE_INTERVAL) {
            updateVillagerDistances();
            ticksSinceLastUpdate = 0;
        }
    }
    
    private void updateVillagerDistances() {
        if (client == null || client.player == null) return;
        
        for (VillagerEntry entry : villagerEntries) {
            // Calculate distance based on stored coordinates
            double dx = entry.x - client.player.getX();
            double dy = entry.y - client.player.getY();
            double dz = entry.z - client.player.getZ();
            entry.distance = (int) Math.sqrt(dx * dx + dy * dy + dz * dz);
        }
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        animationTick += delta;
        
        
        renderJournalBackground(context);
        renderAnimatedTitle(context);
        renderVillagerEntries(context, mouseX, mouseY);
        
        super.render(context, mouseX, mouseY, delta);
        
        
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
            
            
            boolean isHovered = isMouseOverEntry(mouseX, mouseY, i);
            
            int backgroundColor = isHovered ? 0x55FFFFFF : 0x33FFFFFF;
            context.fill(entriesStartX, entryY, entriesStartX + BACKGROUND_WIDTH - 2 * MARGIN, entryY + 40, backgroundColor);
            
            
            int professionColor = PROFESSION_COLORS.getOrDefault(entry.profession.toLowerCase(), 0xFF4169E1);
            context.fill(entriesStartX + 5, entryY + 5, entriesStartX + 5 + PROFESSION_ICON_SIZE, 
                        entryY + 5 + PROFESSION_ICON_SIZE, professionColor);
            
            
            context.drawTextWithShadow(textRenderer, Text.literal(entry.name).formatted(Formatting.AQUA),
                entriesStartX + 20, entryY + 5, 0xFFFFFF);
            
            
            String professionText = capitalize(entry.profession) + " (Lv." + entry.level + ")";
            context.drawTextWithShadow(textRenderer, Text.literal(professionText).formatted(Formatting.YELLOW),
                entriesStartX + 20, entryY + 18, 0xFFFFFF);
            
            
            String distanceText = entry.distance == -1 ? "Missing" : entry.distance + " blocks";
            Formatting distanceColor = entry.distance == -1 ? Formatting.RED : 
                (entry.distance <= PULSE_DISTANCE_THRESHOLD ? Formatting.GREEN : Formatting.GRAY);
            context.drawTextWithShadow(textRenderer, Text.literal(distanceText).formatted(distanceColor),
                entriesStartX + 20, entryY + 30, 0xFFFFFF);
            
            
            if (entry.distance != -1 && entry.distance <= PULSE_DISTANCE_THRESHOLD) {
                float pulse = (MathHelper.sin(animationTick * 0.3f) + 1) * 0.5f;
                int pulseColor = (int)(255 * pulse) << 24 | 0x00FF00;
                context.fill(entriesStartX + BACKGROUND_WIDTH - 2 * MARGIN - 15, entryY + 5, 
                           entriesStartX + BACKGROUND_WIDTH - 2 * MARGIN - 5, entryY + 15, pulseColor);
            }
            
            
            if (client != null && client.player != null && !client.player.isCreative()) {
                context.drawTextWithShadow(textRenderer, Text.literal("🔒").formatted(Formatting.RED),
                    entriesStartX + BACKGROUND_WIDTH - 2 * MARGIN - 25, entryY + 5, 0xFFFFFF);
            }
            
            // Family tree icon (hint for shift+right-click)
            context.drawTextWithShadow(textRenderer, Text.literal("🌳").formatted(Formatting.GREEN),
                entriesStartX + BACKGROUND_WIDTH - 2 * MARGIN - 40, entryY + 20, 0xFFFFFF);
        }
    }
    
    
    private boolean isMouseOverEntry(int mouseX, int mouseY, int entryIndex) {
        int entryY = entriesStartY + (entryIndex * ENTRY_HEIGHT);
        return mouseX >= entriesStartX && mouseX <= entriesStartX + BACKGROUND_WIDTH - 2 * MARGIN &&
               mouseY >= entryY && mouseY <= entryY + 40;
    }
    
    
    private void renderSingleTooltip(DrawContext context, int mouseX, int mouseY) {
        int visibleCount = Math.min(villagerEntries.size() - scrollOffset, MAX_VISIBLE_ENTRIES);
        
        for (int i = 0; i < visibleCount; i++) {
            if (isMouseOverEntry(mouseX, mouseY, i)) {
                VillagerEntry entry = villagerEntries.get(i + scrollOffset);
                
                
                List<Text> tooltip = buildTooltip(entry);
                context.drawTooltip(textRenderer, tooltip, mouseX, mouseY);
                break; 
            }
        }
    }
    
    
    private List<Text> buildTooltip(VillagerEntry entry) {
        List<Text> tooltip = new ArrayList<>(3);
        
        if (client != null && client.player != null) {
            if (client.player.isCreative()) {
                tooltip.add(Text.literal("Click to teleport!").formatted(Formatting.LIGHT_PURPLE));
            } else {
                tooltip.add(Text.literal("Teleport only available in Creative Mode").formatted(Formatting.RED));
            }
            
            tooltip.add(Text.literal("🌳 Shift+Right-click villager for family tree").formatted(Formatting.GREEN));
        }
        
        return tooltip;
    }
    
    
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
    
    private String formatVillagerInfo(VillagerEntry entry) {
        return entry.name + " | " + capitalize(entry.profession) + " Lv." + entry.level;
    }
    
    private void teleportToVillager(int entryIndex) {
        if (entryIndex >= 0 && entryIndex < villagerEntries.size() && client != null && client.player != null) {
            VillagerEntry entry = villagerEntries.get(entryIndex);
            
            
            if (!client.player.isCreative()) {
                
                return;
            }
            
            playClickSound();
            ClientPlayNetworking.send(new VillagerTeleportPacket(entry.villagerId));
            close();
        }
    }
    
    private void playClickSound() {
        if (client != null) {
            client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f, 0.8f));
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
        final int x, y, z;
        int distance;
        
        VillagerEntry(VillagerEntity villager) {
            this.name = getVillagerName(villager);
            this.profession = villager.getVillagerData().profession().toString().toLowerCase();
            this.level = villager.getVillagerData().level();
            this.villagerId = villager.getId();
            this.x = (int) villager.getX();
            this.y = (int) villager.getY();
            this.z = (int) villager.getZ();
            this.distance = 0;
        }
        
        VillagerEntry(RequestVillagerListPacket.VillagerDataPacket data) {
            this.name = data.getName();
            this.profession = data.getProfession().toLowerCase();
            this.level = 1; // Default level since we don't have this info from server
            this.villagerId = data.getEntityId();
            this.x = data.getX();
            this.y = data.getY();
            this.z = data.getZ();
            this.distance = 0;
        }
        
        private String getVillagerName(VillagerEntity villager) {
            return VillagerNames.getVillagerName(villager);
        }
    }
}