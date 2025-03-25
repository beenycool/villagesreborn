package com.beeny.gui;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import com.beeny.village.Culture;
import com.beeny.village.Culture.CultureType;
import com.beeny.village.Culture.CulturalTrait;
import com.beeny.village.Culture.Season;

public class CulturesConfigScreen extends Screen {
    private final Screen parent;
    private Culture activeCulture;
    private CultureType selectedPrimary;
    private CultureType selectedSecondary;
    private float hybridRatio = 0.5f;

    public CulturesConfigScreen(Screen parent, Culture activeCulture) {
        super(Text.of("Configure Cultures"));
        this.parent = parent;
        this.activeCulture = activeCulture != null ? activeCulture : new Culture(CultureType.MEDIEVAL);
        this.selectedPrimary = this.activeCulture.getType();
        this.selectedSecondary = this.activeCulture.getSecondaryType();
    }

    @Override
    protected void init() {
        int y = height / 4;
        int buttonWidth = 200;

        addPrimaryCultureButton(y);
        addSecondaryCultureButton(y + 24);
        addHybridRatioButton(y + 48);
        addTraitButtons(y + 72);
        addSeasonalEventButtons(y + 120);

        addDrawableChild(ButtonWidget.builder(Text.of("Done"), button -> close())
            .dimensions(width / 2 - 100, height - 28, buttonWidth, 20)
            .build());
    }

    private void addPrimaryCultureButton(int y) {
        addDrawableChild(ButtonWidget.builder(
            Text.of("Primary Culture: " + selectedPrimary.getDisplayName()),
            button -> cyclePrimaryCulture())
            .dimensions(width / 2 - 100, y, 200, 20)
            .build());
    }

    private void addSecondaryCultureButton(int y) {
        addDrawableChild(ButtonWidget.builder(
            Text.of("Secondary Culture: " + (selectedSecondary != null ? selectedSecondary.getDisplayName() : "None")),
            button -> cycleSecondaryCulture())
            .dimensions(width / 2 - 100, y, 200, 20)
            .build());
    }

    private void addHybridRatioButton(int y) {
        if (selectedSecondary != null) {
            addDrawableChild(ButtonWidget.builder(
                Text.of(String.format("Hybrid Ratio: %.1f", hybridRatio)),
                button -> cycleHybridRatio())
                .dimensions(width / 2 - 100, y, 200, 20)
                .build());
        }
    }

    private void addTraitButtons(int startY) {
        int x = width / 2 - 100;
        int y = startY;
        int traitsPerRow = 2;
        int buttonWidth = 95;
        int spacing = 10;
        
        CulturalTrait[] traits = CulturalTrait.values();
        for (int i = 0; i < traits.length && i < 6; i++) {
            CulturalTrait trait = traits[i];
            boolean hasTrait = activeCulture.getTraits().contains(trait);
            
            int xOffset = (i % traitsPerRow) * (buttonWidth + spacing);
            int yOffset = (i / traitsPerRow) * 24;
            
            addDrawableChild(ButtonWidget.builder(
                Text.of((hasTrait ? "✓ " : "") + trait.getDisplayName()),
                button -> toggleTrait(trait))
                .dimensions(x + xOffset, y + yOffset, buttonWidth, 20)
                .build());
        }
    }

    private void addSeasonalEventButtons(int startY) {
        int x = width / 2 - 100;
        int y = startY;
        
        for (Season season : Season.values()) {
            addDrawableChild(ButtonWidget.builder(
                Text.of(season.getDisplayName() + ": " + activeCulture.getSeasonalEvent(season)),
                button -> cycleSeasonalEvent(season))
                .dimensions(x, y, 200, 20)
                .build());
            y += 24;
        }
    }

    private void cyclePrimaryCulture() {
        CultureType[] types = CultureType.values();
        int currentIndex = java.util.Arrays.asList(types).indexOf(selectedPrimary);
        selectedPrimary = types[(currentIndex + 1) % types.length];
        updateCulture();
    }

    private void cycleSecondaryCulture() {
        if (selectedSecondary == null) {
            selectedSecondary = CultureType.values()[0];
        } else {
            CultureType[] types = CultureType.values();
            int currentIndex = java.util.Arrays.asList(types).indexOf(selectedSecondary);
            currentIndex = (currentIndex + 1) % types.length;
            if (currentIndex == 0) {
                selectedSecondary = null;
            } else {
                selectedSecondary = types[currentIndex];
            }
        }
        updateCulture();
    }

    private void cycleHybridRatio() {
        hybridRatio = (hybridRatio + 0.1f) % 1.1f;
        if (hybridRatio > 1.0f) hybridRatio = 0.0f;
        updateCulture();
    }

    private void toggleTrait(CulturalTrait trait) {
        if (activeCulture.getTraits().contains(trait)) {
            activeCulture.removeTrait(trait);
        } else {
            activeCulture.addTrait(trait);
        }
        init();
    }

    private void cycleSeasonalEvent(Season season) {
        String[] events = {"Festival", "Market", "Tournament", "Ceremony"};
        String current = activeCulture.getSeasonalEvent(season);
        int currentIndex = java.util.Arrays.asList(events).indexOf(current);
        String next = events[(currentIndex + 1) % events.length];
        activeCulture.setSeasonalEvent(season, next);
        init();
    }

    private void updateCulture() {
        activeCulture = new Culture(selectedPrimary, selectedSecondary, hybridRatio);
        init();
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}
