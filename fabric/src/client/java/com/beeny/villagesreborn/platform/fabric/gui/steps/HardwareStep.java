package com.beeny.villagesreborn.platform.fabric.gui.steps;

import com.beeny.villagesreborn.core.hardware.HardwareInfo;
import com.beeny.villagesreborn.core.hardware.HardwareTier;
import com.beeny.villagesreborn.platform.fabric.gui.WizardStep;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class HardwareStep implements WizardStep {
    private final HardwareInfo hardwareInfo;
    private WizardStep.StepContext context;

    public HardwareStep(HardwareInfo hardwareInfo) {
        this.hardwareInfo = hardwareInfo;
    }

    @Override
    public void init(WizardStep.StepContext context) {
        this.context = context;
        int centerX = context.getWidth() / 2;
        int currentY = context.getHeight() / 4;
        int spacing = 12;

        addHardwareInfoLine(centerX - 150, currentY, "RAM: " + hardwareInfo.getRamGB() + " GB", Formatting.WHITE);
        currentY += spacing;
        addHardwareInfoLine(centerX - 150, currentY, "CPU Cores: " + hardwareInfo.getCpuCores(), Formatting.WHITE);
        currentY += spacing;
        
        String avxStatus = hardwareInfo.hasAvx2Support() ? "Supported" : "Not Supported";
        Formatting avxColor = hardwareInfo.hasAvx2Support() ? Formatting.GREEN : Formatting.YELLOW;
        addHardwareInfoLine(centerX - 150, currentY, "AVX2: " + avxStatus, avxColor);
        currentY += spacing;
        
        addHardwareInfoLine(centerX - 150, currentY, "Tier: " + hardwareInfo.getHardwareTier(), 
            getTierColor(hardwareInfo.getHardwareTier()));
    }

    private void addHardwareInfoLine(int x, int y, String text, Formatting color) {
        TextWidget widget = new TextWidget(
            x, y, 300, 12,
            Text.literal(text).formatted(color),
            context.getTextRenderer()
        );
        context.addDrawableChild(widget);
    }

    private Formatting getTierColor(HardwareTier tier) {
        return switch (tier) {
            case HIGH -> Formatting.GREEN;
            case MEDIUM -> Formatting.YELLOW;
            case LOW -> Formatting.RED;
            case UNKNOWN -> Formatting.GRAY;
        };
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Additional rendering for hardware detection status
        if (this.context != null) {
            int centerX = this.context.getWidth() / 2;
            int statusY = this.context.getHeight() - 40;
            
            // Show hardware detection status
            String statusText = "Hardware detection complete";
            int textWidth = this.context.getTextRenderer().getWidth(statusText);
            context.drawText(this.context.getTextRenderer(), statusText, 
                centerX - textWidth / 2, statusY, 0x888888, false);
        }
    }

    @Override
    public boolean isValid() {
        return true;
    }
}