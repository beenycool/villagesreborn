package com.beeny.villagesreborn.platform.fabric.gui.world;

import com.beeny.villagesreborn.core.world.VillageChronicle;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.List;

public class VillageChronicleScreen extends Screen {

    private final VillageChronicle chronicle;

    public VillageChronicleScreen(VillageChronicle chronicle) {
        super(Text.literal("Village Chronicle"));
        this.chronicle = chronicle;
    }

    @Override
    protected void init() {
        super.init();
        // Add a button to close the screen
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Close"), button -> this.close())
                .dimensions(this.width / 2 - 50, this.height - 30, 100, 20)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 15, 0xFFFFFF);

        // Draw chronicle events
        int y = 40;
        for (String event : chronicle.getEvents()) {
            context.drawTextWithShadow(this.textRenderer, event, 10, y, 0xFFFFFF);
            y += 12;
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
} 