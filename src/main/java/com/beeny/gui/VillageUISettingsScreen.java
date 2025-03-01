package com.beeny.gui;

import com.beeny.config.VillagesConfig;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VillageUISettingsScreen extends Screen {
    private static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn");
    private final Screen parent;
    private final VillagesConfig.UISettings uiSettings;
    private TextFieldWidget labelFormatField;
    private CyclingButtonWidget<ConversationHud.HudPosition> positionButton;
    private CyclingButtonWidget<Boolean> showCultureButton;
    private CyclingButtonWidget<Boolean> showProfessionButton;
    private TextFieldWidget backgroundColorField;
    private TextFieldWidget borderColorField;
    private TextFieldWidget labelColorField;
    private TextFieldWidget nameColorField;
    
    public VillageUISettingsScreen(Screen parent) {
        super(Text.literal("Villages Reborn UI Settings"));
        this.parent = parent;
        this.uiSettings = VillagesConfig.getInstance().getUISettings();
    }

    @Override
    protected void init() {
        int buttonWidth = 200;
        int buttonHeight = 20;
        int centerX = this.width / 2 - buttonWidth / 2;
        int startY = 40;
        int labelWidth = 100;
        
        // Label format field
        this.labelFormatField = new TextFieldWidget(
            this.textRenderer, centerX, startY, buttonWidth, buttonHeight, Text.literal("Label Format"));
        this.labelFormatField.setText(uiSettings.conversationLabelFormat);
        this.labelFormatField.setMaxLength(50);
        this.addDrawableChild(this.labelFormatField);
        startY += 30;
        
        // Position dropdown
        ConversationHud.HudPosition currentPosition = ConversationHud.HudPosition.valueOf(uiSettings.conversationHudPosition);
        this.positionButton = CyclingButtonWidget.<ConversationHud.HudPosition>builder(
                position -> Text.literal(position.name()))
            .values((Object[])ConversationHud.HudPosition.values())
            .initially(currentPosition)
            .build(centerX, startY, buttonWidth, buttonHeight, Text.literal("Position"));
        this.addDrawableChild(this.positionButton);
        startY += 30;
        
        // Show culture toggle
        this.showCultureButton = CyclingButtonWidget.<Boolean>builder(value -> value ? Text.literal("On") : Text.literal("Off"))
            .values(true, false)
            .initially(uiSettings.showCulture)
            .build(centerX, startY, buttonWidth, buttonHeight, Text.literal("Show Culture"));
        this.addDrawableChild(this.showCultureButton);
        startY += 30;
        
        // Show profession toggle
        this.showProfessionButton = CyclingButtonWidget.<Boolean>builder(value -> value ? Text.literal("On") : Text.literal("Off"))
            .values(true, false)
            .initially(uiSettings.showProfession)
            .build(centerX, startY, buttonWidth, buttonHeight, Text.literal("Show Profession"));
        this.addDrawableChild(this.showProfessionButton);
        startY += 30;
        
        // Background color field (hex format)
        this.backgroundColorField = new TextFieldWidget(
            this.textRenderer, centerX, startY, buttonWidth, buttonHeight, Text.literal("Background Color"));
        this.backgroundColorField.setText(String.format("%08X", uiSettings.backgroundColor));
        this.backgroundColorField.setMaxLength(8);
        this.addDrawableChild(this.backgroundColorField);
        startY += 30;
        
        // Border color field (hex format)
        this.borderColorField = new TextFieldWidget(
            this.textRenderer, centerX, startY, buttonWidth, buttonHeight, Text.literal("Border Color"));
        this.borderColorField.setText(String.format("%08X", uiSettings.borderColor));
        this.borderColorField.setMaxLength(8);
        this.addDrawableChild(this.borderColorField);
        startY += 30;
        
        // Label color field
        this.labelColorField = new TextFieldWidget(
            this.textRenderer, centerX, startY, buttonWidth, buttonHeight, Text.literal("Label Color"));
        this.labelColorField.setText(String.format("%08X", uiSettings.labelColor));
        this.labelColorField.setMaxLength(8);
        this.addDrawableChild(this.labelColorField);
        startY += 30;
        
        // Name color field
        this.nameColorField = new TextFieldWidget(
            this.textRenderer, centerX, startY, buttonWidth, buttonHeight, Text.literal("Name Color"));
        this.nameColorField.setText(String.format("%08X", uiSettings.nameColor));
        this.nameColorField.setMaxLength(8);
        this.addDrawableChild(this.nameColorField);
        startY += 30;
        
        // Save button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Save"), button -> saveSettings())
            .dimensions(centerX - 105, startY, 100, buttonHeight)
            .build());
            
        // Back button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Back"), button -> this.client.setScreen(parent))
            .dimensions(centerX + 5, startY, 100, buttonHeight)
            .build());
    }
    
    @Override
    public void render(net.minecraft.client.gui.DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);
        
        // Draw labels for all fields
        int buttonWidth = 200;
        int labelWidth = 100;
        int centerX = this.width / 2 - buttonWidth / 2;
        int startY = 40;
        
        context.drawTextWithShadow(this.textRenderer, "Label Format:", centerX - labelWidth, startY + 5, 0xFFFFFF);
        startY += 30;
        context.drawTextWithShadow(this.textRenderer, "Position:", centerX - labelWidth, startY + 5, 0xFFFFFF);
        startY += 30;
        context.drawTextWithShadow(this.textRenderer, "Show Culture:", centerX - labelWidth, startY + 5, 0xFFFFFF);
        startY += 30;
        context.drawTextWithShadow(this.textRenderer, "Show Profession:", centerX - labelWidth, startY + 5, 0xFFFFFF);
        startY += 30;
        context.drawTextWithShadow(this.textRenderer, "Background Color:", centerX - labelWidth, startY + 5, 0xFFFFFF);
        startY += 30;
        context.drawTextWithShadow(this.textRenderer, "Border Color:", centerX - labelWidth, startY + 5, 0xFFFFFF);
        startY += 30;
        context.drawTextWithShadow(this.textRenderer, "Label Color:", centerX - labelWidth, startY + 5, 0xFFFFFF);
        startY += 30;
        context.drawTextWithShadow(this.textRenderer, "Name Color:", centerX - labelWidth, startY + 5, 0xFFFFFF);
        
        // Draw help text for label format
        String helpText = "Use {name} to include the villager's name";
        int helpWidth = this.textRenderer.getWidth(helpText);
        context.drawTextWithShadow(this.textRenderer, helpText, this.width / 2 - helpWidth / 2, 330, 0xAAAAAA);
        
        // Draw color preview boxes
        showColorPreview(context, uiSettings.backgroundColor, this.width / 2 + 110, 40 + 30 * 4);
        showColorPreview(context, uiSettings.borderColor, this.width / 2 + 110, 40 + 30 * 5);
        showColorPreview(context, uiSettings.labelColor, this.width / 2 + 110, 40 + 30 * 6);
        showColorPreview(context, uiSettings.nameColor, this.width / 2 + 110, 40 + 30 * 7);
        
        super.render(context, mouseX, mouseY, delta);
    }
    
    /**
     * Renders a small preview box with the given color
     */
    private void showColorPreview(net.minecraft.client.gui.DrawContext context, int color, int x, int y) {
        int previewSize = 16;
        // Draw border
        context.fill(x - 1, y - 1, x + previewSize + 1, y + previewSize + 1, 0xFFFFFFFF);
        // Draw color
        context.fill(x, y, x + previewSize, y + previewSize, color);
    }
    
    private void saveSettings() {
        // Save all settings to config
        try {
            // Save position
            uiSettings.conversationHudPosition = positionButton.getValue().name();
            
            // Save display options
            uiSettings.conversationLabelFormat = labelFormatField.getText();
            if (uiSettings.conversationLabelFormat.isEmpty()) {
                uiSettings.conversationLabelFormat = "Speaking to: {name}";
            }
            
            uiSettings.showCulture = showCultureButton.getValue();
            uiSettings.showProfession = showProfessionButton.getValue();
            
            // Parse color values (with error handling)
            try {
                uiSettings.backgroundColor = Integer.parseUnsignedInt(backgroundColorField.getText(), 16);
                uiSettings.borderColor = Integer.parseUnsignedInt(borderColorField.getText(), 16);
                uiSettings.labelColor = Integer.parseUnsignedInt(labelColorField.getText(), 16);
                uiSettings.nameColor = Integer.parseUnsignedInt(nameColorField.getText(), 16);
            } catch (NumberFormatException e) {
                LOGGER.error("Failed to parse color value", e);
            }
            
            // Save to disk
            VillagesConfig.getInstance().save();
            
            LOGGER.info("UI Settings saved");
        } catch (Exception e) {
            LOGGER.error("Error saving UI settings", e);
        }
        
        // Return to parent screen
        this.client.setScreen(parent);
    }
    
    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}