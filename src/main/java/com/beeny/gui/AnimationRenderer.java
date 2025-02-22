package com.beeny.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import net.minecraft.client.render.RenderLayer;
import java.util.function.Function;

public class AnimationRenderer {
    private int animationTick = 0;
    private static final int ANIMATION_DURATION = 100; // 5 seconds at 20 ticks/second
    private static final Function<Identifier, RenderLayer> TEXTURE_RENDERER = identifier -> {
        // Use the most basic texture render layer
        return RenderLayer.getPositionTexture();
    };
    
    private final Identifier[] frames = {
        Identifier.of("villagesreborn", "textures/gui/anim_frame1.png"),
        Identifier.of("villagesreborn", "textures/gui/anim_frame2.png"),
        Identifier.of("villagesreborn", "textures/gui/anim_frame3.png")
    };

    public void render(DrawContext context, int width, int height) {
        int frameIndex = (animationTick / 20) % frames.length;
        int size = 128;
        int x = width / 2 - size / 2;
        int y = height / 2 - size / 2;
        float u = 0;
        float v = 0;

        // Draw texture with render layer
        context.drawTexture(
            TEXTURE_RENDERER,    // render layer provider
            frames[frameIndex],  // texture
            x, y,               // screen position
            u, v,               // UV coordinates
            size, size,         // width/height
            size, size          // texture dimensions
        );
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