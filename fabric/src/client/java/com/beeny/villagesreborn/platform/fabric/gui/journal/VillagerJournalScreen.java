package com.beeny.villagesreborn.platform.fabric.gui.journal;

import com.beeny.villagesreborn.core.ai.VillagerJournal;
import com.beeny.villagesreborn.core.ai.VillagerJournal.JournalEntry;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.text.Style;
import net.minecraft.text.OrderedText;

import java.util.List;

public class VillagerJournalScreen extends Screen {

    private final VillagerJournal journal;
    private List<JournalEntry> entries;
    private JournalEntry selectedEntry;
    private int listWidth = 150;

    public VillagerJournalScreen(VillagerJournal journal) {
        super(Text.literal(journal.getVillagerBrain().getVillagerName() + "'s Journal"));
        this.journal = journal;
    }

    @Override
    protected void init() {
        super.init();
        this.entries = journal.getEntries();
        if (!this.entries.isEmpty()) {
            this.selectedEntry = this.entries.get(0);
        }

        // Add a button to close the screen
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Close"), button -> this.close())
                .dimensions(this.width / 2 - 50, this.height - 30, 100, 20)
                .build());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { // Left click
            int y = 40;
            for (JournalEntry entry : this.entries) {
                if (mouseX >= 10 && mouseX <= 10 + listWidth && mouseY >= y && mouseY <= y + 10) {
                    this.selectedEntry = entry;
                    return true;
                }
                y += 12;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 15, 0xFFFFFF);

        // Draw entry list
        int y = 40;
        for (JournalEntry entry : this.entries) {
            int color = (entry == selectedEntry) ? 0xFFFF00 : 0xFFFFFF;
            context.drawTextWithShadow(this.textRenderer, entry.getTitle(), 10, y, color);
            y += 12;
        }

        // Draw selected entry content
        if (selectedEntry != null) {
            context.drawTextWithShadow(this.textRenderer, selectedEntry.getTitle(), listWidth + 20, 40, 0xFFFF00);
            
            // Wrap and draw content text
            List<OrderedText> lines = this.textRenderer.wrapLines(Text.literal(selectedEntry.getContent()), this.width - listWidth - 30);
            int contentY = 60;
            for (OrderedText line : lines) {
                context.drawTextWithShadow(this.textRenderer, line, listWidth + 20, contentY, 0xFFFFFF);
                contentY += 12;
            }
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
} 