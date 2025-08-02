package com.beeny.client.gui;

import com.beeny.data.VillagerData;
import com.beeny.network.VillagerTeleportPacket;
import com.beeny.network.UpdateVillagerNotesPacket;
import com.beeny.Villagersreborn;
import com.beeny.util.VillagerNames;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;

import java.util.*;
import java.util.stream.Collectors;

public class EnhancedVillagerJournalScreen extends Screen {
    
    private static final int SCREEN_WIDTH = 400;
    private static final int SCREEN_HEIGHT = 300;
    private static final int TAB_HEIGHT = 30;
    private static final int CONTENT_MARGIN = 10;
    
    
    private enum Tab {
        LIST("List", 0xFF4A90E2),
        DETAILS("Details", 0xFF7B68EE),
        FAMILY("Family", 0xFFFF69B4),
        STATS("Statistics", 0xFF32CD32),
        NOTES("Notes", 0xFFFFD700);
        
        final String name;
        final int color;
        
        Tab(String name, int color) {
            this.name = name;
            this.color = color;
        }
    }
    
    private Tab currentTab = Tab.LIST;
    private final Map<Tab, ButtonWidget> tabButtons = new HashMap<>();
    
    
    private final List<VillagerEntity> villagers;
    private VillagerEntity selectedVillager;
    private VillagerData selectedVillagerData;
    
    
    private int scrollOffset = 0;
    private static final int ENTRIES_PER_PAGE = 8;
    private ButtonWidget scrollUpButton;
    private ButtonWidget scrollDownButton;
    private final List<ButtonWidget> villagerButtons = new ArrayList<>();
    
    
    private ButtonWidget teleportButton;
    private ButtonWidget refreshButton;
    
    
    private TextFieldWidget notesField;
    private ButtonWidget saveNotesButton;
    
    
    private TextFieldWidget searchField;
    private String searchQuery = "";
    private List<VillagerEntity> filteredVillagers;
    
    
    private float animationTick = 0;
    
    public EnhancedVillagerJournalScreen(List<VillagerEntity> villagers) {
        super(Text.literal("Villager Journal"));
        this.villagers = villagers;
        this.filteredVillagers = new ArrayList<>(villagers);
        
        
        this.filteredVillagers.sort((v1, v2) -> {
            String name1 = VillagerNames.getVillagerName(v1);
            String name2 = VillagerNames.getVillagerName(v2);
            return Comparator.nullsFirst(String::compareTo).compare(name1, name2);
        });
    }
    
    @Override
    protected void init() {
        super.init();
        
        int screenX = (width - SCREEN_WIDTH) / 2;
        int screenY = (height - SCREEN_HEIGHT) / 2;
        
        
        int tabWidth = SCREEN_WIDTH / Tab.values().length;
        int tabX = screenX;
        for (Tab tab : Tab.values()) {
            ButtonWidget tabButton = ButtonWidget.builder(
                Text.literal(tab.name),
                btn -> switchTab(tab)
            ).dimensions(tabX, screenY, tabWidth - 2, TAB_HEIGHT).build();
            
            tabButtons.put(tab, tabButton);
            addDrawableChild(tabButton);
            tabX += tabWidth;
        }
        
        
        searchField = new TextFieldWidget(textRenderer, screenX + CONTENT_MARGIN,
            screenY + TAB_HEIGHT + 5, 150, 20, Text.literal("Search"));
        searchField.setChangedListener(this::updateSearch);
        searchField.setPlaceholder(Text.translatable("villagersreborn.journal.search_placeholder"));
        addDrawableChild(searchField);
        
        
        switchTab(currentTab);
    }
    
    private void switchTab(Tab newTab) {
        
        clearTabWidgets();
        
        currentTab = newTab;
        
        int screenX = (width - SCREEN_WIDTH) / 2;
        int screenY = (height - SCREEN_HEIGHT) / 2;
        int contentY = screenY + TAB_HEIGHT + 35;
        
        switch (currentTab) {
            case LIST -> initListTab(screenX, contentY);
            case DETAILS -> initDetailsTab(screenX, contentY);
            case FAMILY -> initFamilyTab(screenX, contentY);
            case STATS -> initStatsTab(screenX, contentY);
            case NOTES -> initNotesTab(screenX, contentY);
        }
    }
    
    private void initListTab(int screenX, int contentY) {
        
        scrollUpButton = ButtonWidget.builder(
            Text.literal("▲"),
            btn -> scrollList(-1)
        ).dimensions(screenX + SCREEN_WIDTH - 30, contentY, 20, 20).build();
        addDrawableChild(scrollUpButton);
        
        scrollDownButton = ButtonWidget.builder(
            Text.literal("▼"),
            btn -> scrollList(1)
        ).dimensions(screenX + SCREEN_WIDTH - 30, contentY + 200, 20, 20).build();
        addDrawableChild(scrollDownButton);
        
        
        villagerButtons.clear();
        for (int i = 0; i < ENTRIES_PER_PAGE; i++) {
            final int index = i;
            ButtonWidget button = ButtonWidget.builder(
                Text.empty(),
                btn -> selectVillager(scrollOffset + index)
            ).dimensions(screenX + CONTENT_MARGIN, contentY + (i * 25), SCREEN_WIDTH - 50, 20).build();
            
            villagerButtons.add(button);
            addDrawableChild(button);
        }
        
        updateListButtons();
    }
    
    private void initDetailsTab(int screenX, int contentY) {
        if (selectedVillager == null) return;
        
        teleportButton = ButtonWidget.builder(
            Text.literal("Teleport to Villager"),
            btn -> teleportToVillager()
        ).dimensions(screenX + CONTENT_MARGIN, contentY + 180, 150, 20).build();
        addDrawableChild(teleportButton);
        
        refreshButton = ButtonWidget.builder(
            Text.literal("Refresh Data"),
            btn -> refreshVillagerData()
        ).dimensions(screenX + CONTENT_MARGIN + 160, contentY + 180, 100, 20).build();
        addDrawableChild(refreshButton);
    }
    
    private void initFamilyTab(int screenX, int contentY) {
        
    }
    
    private void initStatsTab(int screenX, int contentY) {
        
    }
    
    private void initNotesTab(int screenX, int contentY) {
        if (selectedVillager == null) return;
        
        notesField = new TextFieldWidget(textRenderer, screenX + CONTENT_MARGIN,
            contentY + 10, SCREEN_WIDTH - 2 * CONTENT_MARGIN, 150, Text.literal("Notes"));
        notesField.setMaxLength(500);
        
        if (selectedVillagerData != null) {
            notesField.setText(selectedVillagerData.getNotes());
        }
        
        addDrawableChild(notesField);
        
        saveNotesButton = ButtonWidget.builder(
            Text.literal("Save Notes"),
            btn -> saveVillagerNotes()
        ).dimensions(screenX + SCREEN_WIDTH / 2 - 50, contentY + 170, 100, 20).build();
        addDrawableChild(saveNotesButton);
    }
    
    private void clearTabWidgets() {
        
        villagerButtons.forEach(this::remove);
        villagerButtons.clear();
        
        if (scrollUpButton != null) remove(scrollUpButton);
        if (scrollDownButton != null) remove(scrollDownButton);
        if (teleportButton != null) remove(teleportButton);
        if (refreshButton != null) remove(refreshButton);
        if (notesField != null) remove(notesField);
        if (saveNotesButton != null) remove(saveNotesButton);
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        animationTick += delta;
        
        int screenX = (width - SCREEN_WIDTH) / 2;
        int screenY = (height - SCREEN_HEIGHT) / 2;
        
        
        context.fill(screenX, screenY, screenX + SCREEN_WIDTH, screenY + SCREEN_HEIGHT, 0xFF2C3E50);
        
        
        int tabWidth = SCREEN_WIDTH / Tab.values().length;
        int tabX = screenX;
        for (Tab tab : Tab.values()) {
            int color = tab == currentTab ? tab.color : 0xFF34495E;
            context.fill(tabX, screenY, tabX + tabWidth - 2, screenY + TAB_HEIGHT, color);
            tabX += tabWidth;
        }
        
        
        int contentY = screenY + TAB_HEIGHT + 35;
        context.fill(screenX + 5, contentY - 5, screenX + SCREEN_WIDTH - 5, 
            screenY + SCREEN_HEIGHT - 5, 0xFF1A252F);
        
        
        switch (currentTab) {
            case LIST -> renderListTab(context, screenX, contentY);
            case DETAILS -> renderDetailsTab(context, screenX, contentY);
            case FAMILY -> renderFamilyTab(context, screenX, contentY);
            case STATS -> renderStatsTab(context, screenX, contentY);
            case NOTES -> renderNotesTab(context, screenX, contentY);
        }
        
        super.render(context, mouseX, mouseY, delta);
    }
    
    private void renderListTab(DrawContext context, int screenX, int contentY) {
        
        if (scrollUpButton != null) {
            scrollUpButton.visible = scrollOffset > 0;
        }
        if (scrollDownButton != null) {
            scrollDownButton.visible = scrollOffset + ENTRIES_PER_PAGE < filteredVillagers.size();
        }
    }
    
    private void renderDetailsTab(DrawContext context, int screenX, int contentY) {
        if (selectedVillager == null || selectedVillagerData == null) {
            context.drawCenteredTextWithShadow(textRenderer, 
                Text.literal("Select a villager from the List tab"),
                screenX + SCREEN_WIDTH / 2, contentY + 50, 0xFFFFFF);
            return;
        }
        
        int y = contentY + 10;
        int lineHeight = 15;
        
        
        context.drawTextWithShadow(textRenderer, 
            Text.literal("Name: ").formatted(Formatting.GRAY).append(
                Text.literal(selectedVillagerData.getName()).formatted(Formatting.WHITE)),
            screenX + CONTENT_MARGIN, y, 0xFFFFFF);
        y += lineHeight;
        
        
        context.drawTextWithShadow(textRenderer,
            Text.literal("Age: ").formatted(Formatting.GRAY).append(
                Text.literal(String.valueOf(selectedVillagerData.getAgeInDays())).formatted(Formatting.WHITE)),
            screenX + CONTENT_MARGIN, y, 0xFFFFFF);
        y += lineHeight;
        
        context.drawTextWithShadow(textRenderer,
            Text.literal("Gender: ").formatted(Formatting.GRAY).append(
                Text.literal(selectedVillagerData.getGender()).formatted(Formatting.WHITE)),
            screenX + CONTENT_MARGIN, y, 0xFFFFFF);
        y += lineHeight;
        
        
        context.drawTextWithShadow(textRenderer,
            Text.literal("Personality: ").formatted(Formatting.GRAY).append(
                Text.literal(selectedVillagerData.getPersonality()).formatted(Formatting.YELLOW)),
            screenX + CONTENT_MARGIN, y, 0xFFFFFF);
        y += lineHeight;
        
        context.drawTextWithShadow(textRenderer,
            Text.literal("Mood: ").formatted(Formatting.GRAY).append(
                Text.literal(selectedVillagerData.getHappinessDescription()).formatted(
                    getHappinessFormatting(selectedVillagerData.getHappiness()))),
            screenX + CONTENT_MARGIN, y, 0xFFFFFF);
        y += lineHeight;
        
        
        renderHappinessBar(context, screenX + CONTENT_MARGIN, y, selectedVillagerData.getHappiness());
        y += 20;
        
        
        context.drawTextWithShadow(textRenderer,
            Text.literal("Hobby: ").formatted(Formatting.GRAY).append(
                Text.literal(selectedVillagerData.getHobby()).formatted(Formatting.AQUA)),
            screenX + CONTENT_MARGIN, y, 0xFFFFFF);
        y += lineHeight;
        
        if (!selectedVillagerData.getFavoriteFood().isEmpty()) {
            context.drawTextWithShadow(textRenderer,
                Text.literal("Favorite Food: ").formatted(Formatting.GRAY).append(
                    Text.literal(selectedVillagerData.getFavoriteFood()).formatted(Formatting.GREEN)),
                screenX + CONTENT_MARGIN, y, 0xFFFFFF);
            y += lineHeight;
        }
        
        
        context.drawTextWithShadow(textRenderer,
            Text.literal("Total Trades: ").formatted(Formatting.GRAY).append(
                Text.literal(String.valueOf(selectedVillagerData.getTotalTrades())).formatted(Formatting.GOLD)),
            screenX + CONTENT_MARGIN, y, 0xFFFFFF);
    }
    
    private void renderFamilyTab(DrawContext context, int screenX, int contentY) {
        if (selectedVillager == null || selectedVillagerData == null) {
            context.drawCenteredTextWithShadow(textRenderer, 
                Text.literal("Select a villager from the List tab"),
                screenX + SCREEN_WIDTH / 2, contentY + 50, 0xFFFFFF);
            return;
        }
        
        int y = contentY + 10;
        int lineHeight = 15;
        
        
        if (!selectedVillagerData.getSpouseName().isEmpty()) {
            context.drawTextWithShadow(textRenderer,
                Text.literal("❤ Spouse: ").formatted(Formatting.RED).append(
                    Text.literal(selectedVillagerData.getSpouseName()).formatted(Formatting.WHITE)),
                screenX + CONTENT_MARGIN, y, 0xFFFFFF);
            y += lineHeight + 5;
        }
        
        
        if (!selectedVillagerData.getChildrenNames().isEmpty()) {
            context.drawTextWithShadow(textRenderer,
                Text.literal("👶 Children:").formatted(Formatting.GREEN),
                screenX + CONTENT_MARGIN, y, 0xFFFFFF);
            y += lineHeight;
            
            for (String child : selectedVillagerData.getChildrenNames()) {
                context.drawTextWithShadow(textRenderer,
                    Text.literal("  • " + child).formatted(Formatting.WHITE),
                    screenX + CONTENT_MARGIN + 10, y, 0xFFFFFF);
                y += lineHeight;
            }
            y += 5;
        }
        
        
        if (!selectedVillagerData.getFamilyMembers().isEmpty()) {
            context.drawTextWithShadow(textRenderer,
                Text.literal("👨‍👩‍👧‍👦 Family Members:").formatted(Formatting.AQUA),
                screenX + CONTENT_MARGIN, y, 0xFFFFFF);
            y += lineHeight;
            
            for (String member : selectedVillagerData.getFamilyMembers()) {
                context.drawTextWithShadow(textRenderer,
                    Text.literal("  • " + member).formatted(Formatting.WHITE),
                    screenX + CONTENT_MARGIN + 10, y, 0xFFFFFF);
                y += lineHeight;
            }
        }
        
        if (selectedVillagerData.getSpouseName().isEmpty() && 
            selectedVillagerData.getChildrenNames().isEmpty() && 
            selectedVillagerData.getFamilyMembers().isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("No family connections yet"),
                screenX + SCREEN_WIDTH / 2, contentY + 50, 0xFF808080);
        }
    }
    
    private void renderStatsTab(DrawContext context, int screenX, int contentY) {
        int y = contentY + 10;
        int lineHeight = 20;
        
        
        Map<String, Integer> professionCounts = new HashMap<>();
        int totalHappiness = 0;
        int marriedCount = 0;
        int elderCount = 0;
        int babyCount = 0;
        
        for (VillagerEntity villager : villagers) {
            VillagerData data = villager.getAttached(Villagersreborn.VILLAGER_DATA);
            if (data != null) {
                String profession = villager.getVillagerData().profession().toString();
                professionCounts.put(profession, professionCounts.getOrDefault(profession, 0) + 1);
                
                totalHappiness += data.getHappiness();
                if (!data.getSpouseId().isEmpty()) marriedCount++;
                if (data.getAge() > 300) elderCount++;
                if (data.getAge() < 20) babyCount++;
            }
        }
        
        
        context.drawTextWithShadow(textRenderer,
            Text.literal("📊 Village Statistics").formatted(Formatting.GOLD),
            screenX + CONTENT_MARGIN, y, 0xFFFFFF);
        y += lineHeight + 5;
        
        context.drawTextWithShadow(textRenderer,
            Text.literal("Total Villagers: " + villagers.size()).formatted(Formatting.WHITE),
            screenX + CONTENT_MARGIN, y, 0xFFFFFF);
        y += lineHeight;
        
        context.drawTextWithShadow(textRenderer,
            Text.literal("Average Happiness: " + (totalHappiness / Math.max(1, villagers.size())) + "%")
                .formatted(Formatting.GREEN),
            screenX + CONTENT_MARGIN, y, 0xFFFFFF);
        y += lineHeight;
        
        context.drawTextWithShadow(textRenderer,
            Text.literal("Married: " + marriedCount + " (" + (marriedCount * 100 / Math.max(1, villagers.size())) + "%)")
                .formatted(Formatting.RED),
            screenX + CONTENT_MARGIN, y, 0xFFFFFF);
        y += lineHeight;
        
        context.drawTextWithShadow(textRenderer,
            Text.literal("Babies: " + babyCount + " | Elders: " + elderCount)
                .formatted(Formatting.YELLOW),
            screenX + CONTENT_MARGIN, y, 0xFFFFFF);
        y += lineHeight + 10;
        
        
        context.drawTextWithShadow(textRenderer,
            Text.literal("Professions:").formatted(Formatting.AQUA),
            screenX + CONTENT_MARGIN, y, 0xFFFFFF);
        y += lineHeight;
        
        List<Map.Entry<String, Integer>> sortedProfessions = professionCounts.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(5)
            .collect(Collectors.toList());
        
        for (int i = 0; i < sortedProfessions.size(); i++) {
            Map.Entry<String, Integer> entry = sortedProfessions.get(i);
            String profName = entry.getKey().replace("minecraft:", "");
            context.drawTextWithShadow(textRenderer,
                Text.literal("  " + capitalize(profName) + ": " + entry.getValue())
                    .formatted(Formatting.WHITE),
                screenX + CONTENT_MARGIN + 10, y + (i * lineHeight), 0xFFFFFF);
        }
    }
    
    private void renderNotesTab(DrawContext context, int screenX, int contentY) {
        if (selectedVillager == null) {
            context.drawCenteredTextWithShadow(textRenderer, 
                Text.literal("Select a villager from the List tab"),
                screenX + SCREEN_WIDTH / 2, contentY + 50, 0xFFFFFF);
            return;
        }
        
        context.drawTextWithShadow(textRenderer,
            Text.literal("📝 Notes for " + VillagerNames.getVillagerName(selectedVillager)).formatted(Formatting.YELLOW),
            screenX + CONTENT_MARGIN, contentY - 10, 0xFFFFFF);
    }
    
    private void renderHappinessBar(DrawContext context, int x, int y, int happiness) {
        int barWidth = 200;
        int barHeight = 10;
        
        
        context.fill(x, y, x + barWidth, y + barHeight, 0xFF000000);
        
        
        int fillWidth = (int) (barWidth * (happiness / 100.0f));
        int color = getHappinessColor(happiness);
        context.fill(x + 1, y + 1, x + fillWidth - 1, y + barHeight - 1, color);
        
        
        String happinessText = happiness + "%";
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(happinessText),
            x + barWidth / 2, y + 1, 0xFFFFFF);
    }
    
    private Formatting getHappinessFormatting(int happiness) {
        if (happiness >= 80) return Formatting.GREEN;
        if (happiness >= 60) return Formatting.YELLOW;
        if (happiness >= 40) return Formatting.GOLD;
        if (happiness >= 20) return Formatting.RED;
        return Formatting.DARK_RED;
    }
    
    private int getHappinessColor(int happiness) {
        if (happiness >= 80) return 0xFF55FF55; 
        if (happiness >= 60) return 0xFFFFFF55; 
        if (happiness >= 40) return 0xFFFFAA00; 
        if (happiness >= 20) return 0xFFFF5555; 
        return 0xFFAA0000; 
    }
    
    private void updateListButtons() {
        int endIndex = Math.min(scrollOffset + ENTRIES_PER_PAGE, filteredVillagers.size());
        
        for (int i = 0; i < ENTRIES_PER_PAGE; i++) {
            ButtonWidget button = villagerButtons.get(i);
            if (i + scrollOffset < endIndex) {
                VillagerEntity villager = filteredVillagers.get(i + scrollOffset);
                VillagerData data = villager.getAttached(Villagersreborn.VILLAGER_DATA);
                
                String name = VillagerNames.getVillagerName(villager);
                button.setMessage(Text.literal(name));
                button.visible = true;
            } else {
                button.visible = false;
            }
        }
    }
    
    private void selectVillager(int index) {
        if (index >= 0 && index < filteredVillagers.size()) {
            selectedVillager = filteredVillagers.get(index);
            selectedVillagerData = selectedVillager.getAttached(Villagersreborn.VILLAGER_DATA);
            switchTab(currentTab); 
        }
    }
    
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
    
    private void updateSearch(String query) {
        searchQuery = query.toLowerCase();
        applySearchFilter();
        scrollOffset = 0;
        updateListButtons();
    }
    
    private void applySearchFilter() {
        if (searchQuery.isEmpty()) {
            filteredVillagers = new ArrayList<>(villagers);
        } else {
            filteredVillagers = villagers.stream()
                .filter(villager -> {
                    String name = VillagerNames.getVillagerName(villager).toLowerCase();
                    VillagerData data = villager.getAttached(Villagersreborn.VILLAGER_DATA);
                    if (data != null) {
                        String profession = villager.getVillagerData().profession().toString().toLowerCase();
                        String personality = data.getPersonality().toLowerCase();
                        return name.contains(searchQuery) || 
                               profession.contains(searchQuery) || 
                               personality.contains(searchQuery);
                    }
                    return name.contains(searchQuery);
                })
                .collect(Collectors.toList());
        }
        
        
        filteredVillagers.sort((v1, v2) -> {
            String name1 = VillagerNames.getVillagerName(v1);
            String name2 = VillagerNames.getVillagerName(v2);
            return Comparator.nullsFirst(String::compareTo).compare(name1, name2);
        });
    }
    
    private void scrollList(int direction) {
        int maxOffset = Math.max(0, filteredVillagers.size() - ENTRIES_PER_PAGE);
        scrollOffset = Math.max(0, Math.min(maxOffset, scrollOffset + direction));
        updateListButtons();
    }
    
    private void teleportToVillager() {
        if (selectedVillager != null && client != null && client.player != null) {
            
            ClientPlayNetworking.send(new VillagerTeleportPacket(selectedVillager.getId()));
            
            
            client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_ENDERMAN_TELEPORT, 1.0f));
            
            
            close();
        }
    }
    
    private void refreshVillagerData() {
        if (selectedVillager != null) {
            selectedVillagerData = selectedVillager.getAttached(Villagersreborn.VILLAGER_DATA);
            switchTab(currentTab); 
        }
    }
    
    private void saveVillagerNotes() {
        if (selectedVillager != null && notesField != null && client != null) {
            String notes = notesField.getText();
            
            
            ClientPlayNetworking.send(new UpdateVillagerNotesPacket(selectedVillager.getId(), notes));
            
            
            if (selectedVillagerData != null) {
                selectedVillagerData.setNotes(notes);
            }
            
            
            client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ITEM_BOOK_PAGE_TURN, 1.0f));
        }
    }
}