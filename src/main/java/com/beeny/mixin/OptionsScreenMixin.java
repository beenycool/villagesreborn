package com.beeny.mixin;

import com.beeny.Villagesreborn;
import com.beeny.gui.VillagesRebornSettingsScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(OptionsScreen.class)
public class OptionsScreenMixin extends Screen {
    protected OptionsScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void addVillagesRebornButton(CallbackInfo ci) {
        OptionsScreen screen = (OptionsScreen)(Object)this;
        this.addDrawableChild(ButtonWidget.builder(
            Text.literal("Villages Reborn"),
            button -> this.client.setScreen(new VillagesRebornSettingsScreen(screen, this.client.options, Villagesreborn.getLLMConfig()))
        ).dimensions(this.width / 2 - 100, this.height - 50, 200, 20).build());
    }
}