// File: src/main/java/com/beeny/mixin/CreateWorldScreenMixin.java
package com.beeny.mixin;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.beeny.gui.VillagesRebornSettingsScreen; // Ensure this import exists

@Mixin(CreateWorldScreen.class)
public abstract class CreateWorldScreenMixin extends Screen { // Make abstract as it extends Screen indirectly
    protected CreateWorldScreenMixin(Text title) {
        super(title);
    }

    @Inject(at = @At("RETURN"), method = "init")
    private void addVillagesRebornButton(CallbackInfo info) {
        // Standard button width is typically 150 or 200. Let's use 150 like vanilla buttons.
        int buttonWidth = 150;
        // Calculate centered X position
        int buttonX = this.width / 2 - buttonWidth / 2;
        // Position it appropriately relative to other buttons (might need adjustment based on vanilla layout changes)
        // Assuming it replaces the "More World Options..." or sits near it.
        // Let's try placing it below "Allow Commands"
        int buttonY = this.height / 4 + 48 + 24 * 2; // Adjusted Y position estimate

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Villages Reborn"), button -> {
            // Ensure 'this' refers to the CreateWorldScreen instance
            this.client.setScreen(new VillagesRebornSettingsScreen((Screen)(Object)this));
        })
        // Use calculated centered position and standard width
        .dimensions(buttonX, buttonY, buttonWidth, 20)
        .build());
    }
}