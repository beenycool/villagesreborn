package com.beeny.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import net.minecraft.client.render.RenderLayer;
import java.util.function.Function;

public class AnimationRenderer {
    private int animationTick = 0;
    private static final int ANIMATION_DURATION = 100; // 5 seconds at 20 ticks/second
    private static final int TICKS_PER_FRAME = 20;     // Each frame lasts 1 second
    private static final int TEXTURE_SIZE = 128;       // Assumes 128x128 texture dimensions

    private final Identifier[] frames = {
        Identifier.tryParse("villagesreborn:textures/gui/anim_frame1.png"),
        Identifier.tryParse("villagesreborn:textures/gui/anim_frame2.png"),
        Identifier.tryParse("villagesreborn:textures/gui/anim_frame3.png")
    };

    // Defined but unused; kept for potential future use or compatibility
    private static final Function<Identifier, RenderLayer> TEXTURE_LAYER =
        id -> RenderLayer.getGui(); // Renamed from GUI_LAYER

    public void render(DrawContext context, int width, int height) {
        if (frames[0] == null) {
            // Texture loading failed; skip rendering to avoid crashes
            return;
        }

        int frameIndex = (animationTick / TICKS_PER_FRAME) % frames.length;
        int x = width / 2 - TEXTURE_SIZE / 2;
        int y = height / 2 - TEXTURE_SIZE / 2;

        // Updated to use drawTexture with full arguments for 1.21.4
        context.drawTexture(TEXTURE_LAYER, frames[frameIndex], x, y, 0.0F, 0.0F, TEXTURE_SIZE, TEXTURE_SIZE, TEXTURE_SIZE, TEXTURE_SIZE); // Now uses the correctly named variable
    }

    public void render(DrawContext context, int width, int height, float delta) {
        // Overloaded method for compatibility; delta unused as animation is tick-based
        render(context, width, height);
    }

    public boolean isComplete() {
        return animationTick >= ANIMATION_DURATION;
    }

    public void tick() {
        animationTick++;
    }

    public int getCurrentTick() {
        return animationTick;
    }
}
