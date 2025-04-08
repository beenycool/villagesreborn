package com.beeny.gui;

import com.beeny.network.VillagesClientNetwork;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Formatting;
import net.minecraft.text.Text;

public class VillageInfoHud {
    private String cultureName = "";
    private int prosperity = 0;
    private int safety = 0;
    private int population = 0;
    private boolean showHud = false;
    private long displayEndTime = 0;
    private static final long DISPLAY_DURATION = 5000;
    private boolean forcedDisplay = false;
    private long lastRequestTime = 0;
    private static final long REQUEST_COOLDOWN = 5000; // 5 seconds between requests
    
    private static VillageInfoHud instance;
    
    public static VillageInfoHud getInstance() {
        if (instance == null) {
            instance = new VillageInfoHud();
        }
        return instance;
    }
    
    private VillageInfoHud() {
    }
    
    public void update(String cultureName, int prosperity, int safety, int population) {
        this.cultureName = cultureName;
        this.prosperity = prosperity;
        this.safety = safety;
        this.population = population;
        
        this.showHud = true;
        this.displayEndTime = System.currentTimeMillis() + DISPLAY_DURATION;
    }
    
    public void forceDisplay(long durationMillis) {
        this.showHud = true;
        this.forcedDisplay = true;
        this.displayEndTime = System.currentTimeMillis() + durationMillis;
    }
    
    public void clear() {
        this.cultureName = "";
        this.prosperity = 0;
        this.safety = 0;
        this.population = 0;
        this.showHud = false;
    }
    
    public void requestUpdate() {
        long currentTime = System.currentTimeMillis();
        
        if (currentTime - lastRequestTime > REQUEST_COOLDOWN) {
            lastRequestTime = currentTime;
            VillagesClientNetwork.requestVillageInfo();
        }
    }
    
    public void render(DrawContext context, MinecraftClient client) {
        long currentTime = System.currentTimeMillis();
        
        if (currentTime > displayEndTime) {
            showHud = false;
            forcedDisplay = false;
        }
        
        if (!showHud || cultureName.isEmpty()) {
            return;
        }
        
        TextRenderer textRenderer = client.textRenderer;
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        
        float animationFactor = calculateAnimationFactor(currentTime);
        int xPos = screenWidth - 150;
        int baseYPos = 10;
        int yPos = baseYPos - (int)((1.0f - animationFactor) * 50);
        int lineHeight = textRenderer.fontHeight + 2;
        int bgAlpha = (int)(128 * animationFactor);
        int backgroundColor = (bgAlpha << 24);
        int borderColor = ((int)(animationFactor * 255) << 24) | (68 << 16) | (68 << 8) | 68;
        
        context.fill(xPos - 6, yPos - 6, xPos + 146, yPos + (lineHeight * 4) + 6, borderColor);
        context.fill(xPos - 5, yPos - 5, xPos + 145, yPos + (lineHeight * 4) + 5, backgroundColor);
        
        Formatting prosperityColor = getColorForValue(prosperity);
        context.drawTextWithShadow(textRenderer,
            Text.literal("Village: ").append(Text.literal(cultureName).formatted(prosperityColor)),
            xPos, yPos,
            0xFF_FFFFFF | ((int)(animationFactor * 255) << 24));
        context.drawTextWithShadow(textRenderer,
            Text.literal("Prosperity: ").append(Text.literal(getBarString(prosperity)).formatted(prosperityColor)),
            xPos, yPos + lineHeight,
            0xFF_FFFFFF | ((int)(animationFactor * 255) << 24));
        Formatting safetyColor = getColorForValue(safety);
        context.drawTextWithShadow(textRenderer,
            Text.literal("Safety: ").append(Text.literal(getBarString(safety)).formatted(safetyColor)),
            xPos, yPos + lineHeight * 2,
            0xFF_FFFFFF | ((int)(animationFactor * 255) << 24));
        context.drawTextWithShadow(textRenderer,
            Text.literal("Population: ").append(Text.literal(String.valueOf(population))),
            xPos, yPos + lineHeight * 3,
            0xFF_FFFFFF | ((int)(animationFactor * 255) << 24));
    }
    
    private float calculateAnimationFactor(long currentTime) {
        long timeRemaining = displayEndTime - currentTime;
        if (forcedDisplay && timeRemaining > 500) {
            return 1.0f;
        }
        
        if (displayEndTime - DISPLAY_DURATION + 500 > currentTime) {
            long elapsed = currentTime - (displayEndTime - DISPLAY_DURATION);
            return Math.min(1.0f, elapsed / 500.0f);
        }
        
        if (timeRemaining < 500) {
            return Math.max(0.0f, timeRemaining / 500.0f);
        }
        
        return 1.0f;
    }
    
    private String getBarString(int value) {
        int filledBars = value / 10;
        StringBuilder bar = new StringBuilder();
        
        for (int i = 0; i < 10; i++) {
            bar.append(i < filledBars ? "■" : "□");
        }
        
        return bar.toString();
    }
    
    private Formatting getColorForValue(int value) {
        if (value < 30) return Formatting.RED;
        if (value < 60) return Formatting.YELLOW;
        return Formatting.GREEN;
    }
}