package com.beeny.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import net.minecraft.client.render.RenderLayer;

public class AnimationRenderer {
    private int animationTick = 0;
    private static final int ANIMATION_DURATION = 100; // 5 seconds at 20 ticks/second
    private final Identifier[] frames = {
        Identifier.of("villagesreborn", "textures/gui/anim_frame1.png"),
        Identifier.of("villagesreborn", "textures/gui/anim_frame2.png"),
        Identifier.of("villagesreborn", "textures/gui/anim_frame3.png")
    };

    public void render(DrawContext context, int width, int height) {
        int frameIndex = (animationTick / 20) % frames.length;
        context.drawTexture(RenderLayer::getEntitySolid, frames[frameIndex], width / 2 - 64, height / 2 - 64, 0, 0, 128, 128, 128, 128);
    }

    public void tick() {
        animationTick++;
    }

    public boolean isComplete() {
        return animationTick >= ANIMATION_DURATION;
    }

    public int getCurrentTick() {
        return animationTick;
    }
}