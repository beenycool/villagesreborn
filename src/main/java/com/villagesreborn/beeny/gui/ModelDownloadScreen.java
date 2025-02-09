package com.villagesreborn.beeny.gui;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ProgressBarWidget;
import net.minecraft.text.Text;
import com.villagesreborn.beeny.ai.LocalLLMManager;

public class ModelDownloadScreen extends Screen {
    private final Screen parent;
    private ProgressBarWidget progressBar;
    private boolean downloadStarted = false;

    public ModelDownloadScreen(Screen parent) {
        super(Text.of("AI Model Download"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        
        progressBar = new ProgressBarWidget(width / 2 - 100, height / 2 - 10, 200, 20);
        addDrawableChild(progressBar);
        
        addDrawableChild(new ButtonWidget(width / 2 - 100, height - 40, 200, 20, 
            Text.of("Download Models (Required)"), button -> {
                downloadStarted = true;
                LocalLLMManager.downloadMissingModels(new LocalLLMManager.DownloadProgressCallback() {
                    @Override
                    public void onProgress(float progress) {
                        progressBar.setProgress(progress);
                    }

                    @Override
                    public void onComplete() {
                        client.setScreen(parent);
                    }
                });
            }));
    }

    @Override
    public void renderBackground() {
        if (downloadStarted) return;
        super.renderBackground();
    }
}
