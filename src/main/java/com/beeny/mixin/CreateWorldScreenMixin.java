package com.beeny.mixin;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.beeny.gui.VillagesRebornSettingsScreen;

@Mixin(CreateWorldScreen.class)
public class CreateWorldScreenMixin extends Screen {
    protected CreateWorldScreenMixin(Text title) {
        super(title);
    }

    @Inject(at = @At("RETURN"), method = "init")
    private void addVillagesRebornButton(CallbackInfo info) {
        this.addDrawableChild(ButtonWidget.builder(Text.of("Villages Reborn"), button -> {
            this.client.setScreen(new VillagesRebornSettingsScreen(this));
        }).dimensions(this.width / 2 - 85, this.height / 4 + 48 + 24, 150, 20).build());
    }
}
