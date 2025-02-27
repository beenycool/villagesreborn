package com.beeny.gui;

import com.beeny.ai.ModelChecker;
import com.beeny.setup.LLMConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Screen displayed when no AI model is detected, offering to download one.
 */
public class ModelDownloadScreen extends Screen {
    private static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn");
    private final Screen parent;
    private final LLMConfig config;
    private double downloadProgress = 0.0;
    private boolean isDownloading = false;
    private boolean downloadComplete = false;
    private boolean requiresLogin = false;
    private boolean loginChecked = false;
    private final AtomicBoolean cancelRequested = new AtomicBoolean(false);
    
    // UI elements
    private ButtonWidget downloadButton;
    private ButtonWidget cancelButton;
    private ButtonWidget continueButton;
    private ButtonWidget loginButton;
    private ButtonWidget configureApiButton;

    public ModelDownloadScreen(Screen parent, LLMConfig config) {
        super(Text.literal("Villages Reborn - AI Model Setup"));
        this.parent = parent;
        this.config = config;
    }

    @Override
    protected void init() {
        super.init();
        
        int centerX = this.width / 2;
        int buttonWidth = Math.min(200, this.width / 2);
        
        // Check if model requires login (only once)
        if (!loginChecked) {
            loginChecked = true;
            ModelChecker.checkIfModelRequiresLogin().thenAccept(loginRequired -> {
                requiresLogin = loginRequired;
                // Re-init buttons on main thread when we know if login is required
                MinecraftClient.getInstance().execute(this::clearAndInitButtons);
            });
        }
        
        clearAndInitButtons();
    }
    
    private void clearAndInitButtons() {
        clearChildren();
        
        int centerX = this.width / 2;
        int buttonWidth = Math.min(200, this.width / 2);
        
        if (downloadComplete) {
            // Show continue button when download is complete
            continueButton = ButtonWidget.builder(Text.literal("Continue"), button -> {
                MinecraftClient.getInstance().setScreen(parent);
            })
            .dimensions(centerX - buttonWidth / 2, height / 2 + 80, buttonWidth, 20)
            .build();
            addDrawableChild(continueButton);
            return;
        }
        
        if (isDownloading) {
            // Show cancel button during download
            cancelButton = ButtonWidget.builder(Text.literal("Cancel"), button -> {
                cancelRequested.set(true);
                isDownloading = false;
                clearAndInitButtons();
            })
            .dimensions(centerX - buttonWidth / 2, height / 2 + 80, buttonWidth, 20)
            .build();
            addDrawableChild(cancelButton);
            return;
        }
        
        if (requiresLogin) {
            // If login is required, show login button and configure API button
            loginButton = ButtonWidget.builder(Text.literal("Login to HuggingFace"), button -> {
                try {
                    Desktop.getDesktop().browse(new URI("https://huggingface.co/login"));
                } catch (Exception e) {
                    LOGGER.error("Failed to open browser", e);
                }
            })
            .dimensions(centerX - buttonWidth / 2, height / 2, buttonWidth, 20)
            .build();
            addDrawableChild(loginButton);
            
            configureApiButton = ButtonWidget.builder(Text.literal("Configure API Setup"), button -> {
                MinecraftClient.getInstance().setScreen(new SetupScreen(config));
            })
            .dimensions(centerX - buttonWidth / 2, height / 2 + 30, buttonWidth, 20)
            .build();
            addDrawableChild(configureApiButton);
        } else {
            // Standard download button when no login is required
            downloadButton = ButtonWidget.builder(Text.literal("Download Llama 3.2 Model"), button -> {
                startDownload();
            })
            .dimensions(centerX - buttonWidth / 2, height / 2, buttonWidth, 20)
            .build();
            addDrawableChild(downloadButton);
        }
        
        // Always add skip button
        ButtonWidget skipButton = ButtonWidget.builder(Text.literal("Skip (Use External API)"), button -> {
            MinecraftClient.getInstance().setScreen(new SetupScreen(config));
        })
        .dimensions(centerX - buttonWidth / 2, height / 2 + 60, buttonWidth, 20)
        .build();
        addDrawableChild(skipButton);
        
        // Always add back button
        ButtonWidget backButton = ButtonWidget.builder(Text.literal("Back"), button -> {
            MinecraftClient.getInstance().setScreen(parent);
        })
        .dimensions(centerX - buttonWidth / 2, height - 30, buttonWidth, 20)
        .build();
        addDrawableChild(backButton);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        
        int centerX = width / 2;
        
        // Title
        context.drawCenteredTextWithShadow(textRenderer, title, centerX, 20, 0xFFFFFF);
        
        // Main description text
        int textY = 50;
        String[] lines;
        
        if (downloadComplete) {
            lines = new String[] {
                "Download Complete!",
                "The Llama 3.2 model has been successfully downloaded.",
                "Villages Reborn is now configured to use the local model."
            };
        } else if (isDownloading) {
            lines = new String[] {
                "Downloading Llama 3.2 Model...",
                String.format("Progress: %.1f%%", downloadProgress * 100),
                "This may take several minutes depending on your internet connection."
            };
            
            // Draw progress bar
            int barWidth = Math.min(300, width - 100);
            int barHeight = 10;
            int barX = centerX - barWidth / 2;
            int barY = height / 2 + 30;
            
            // Background
            context.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF555555);
            // Progress
            int progressWidth = (int)(barWidth * downloadProgress);
            context.fill(barX, barY, barX + progressWidth, barY + barHeight, 0xFF00AA00);
        } else if (requiresLogin) {
            lines = new String[] {
                "AI Model Not Detected",
                "The Llama 3.2 model requires a HuggingFace account to download.",
                "Please login to HuggingFace first, then try downloading again.",
                "",
                "Alternatively, you can configure an external API."
            };
        } else {
            lines = new String[] {
                "AI Model Not Detected",
                "Villages Reborn uses AI to make villagers intelligent and interactive.",
                "Would you like to download the Llama 3.2 model? (1.0 GB)",
                "",
                "The model will be stored locally and provides better performance."
            };
        }
        
        for (String line : lines) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(line), centerX, textY, 0xFFFFFF);
            textY += 12;
        }
        
        super.render(context, mouseX, mouseY, delta);
    }
    
    private void startDownload() {
        isDownloading = true;
        downloadProgress = 0.0;
        clearAndInitButtons();
        
        ModelChecker.downloadModel(progress -> {
            if (cancelRequested.get()) {
                return;
            }
            downloadProgress = progress;
            if (progress >= 1.0) {
                downloadComplete = true;
                isDownloading = false;
                MinecraftClient.getInstance().execute(this::clearAndInitButtons);
            }
        }).exceptionally(e -> {
            LOGGER.error("Download failed", e);
            isDownloading = false;
            MinecraftClient.getInstance().execute(this::clearAndInitButtons);
            return null;
        });
    }
    
    @Override
    public boolean shouldCloseOnEsc() {
        return !isDownloading;
    }
}