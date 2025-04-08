package com.beeny.gui;

import com.beeny.ai.ModelChecker;
import com.beeny.ai.ModelType; // Need ModelType enum
import com.beeny.util.HardwareUtil; // Need HardwareUtil
import com.beeny.config.VillagesConfig; // Use main config
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.tooltip.Tooltip; // Added import
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.net.URI;
import java.util.concurrent.CompletableFuture; // Added import
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Screen displayed when no AI model is detected, offering to download one.
 */
public class ModelDownloadScreen extends Screen {
    private static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn");
    private final Screen parent;
    private final VillagesConfig config; // Use main config instance
    private double downloadProgress = 0.0;
    private boolean isDownloading = false;
    private boolean downloadComplete = false;
    private boolean requiresLogin = false;
    private boolean loginChecked = false;
    private final AtomicBoolean cancelRequested = new AtomicBoolean(false);
    private long detectedVramBytes = -1; // -1 indicates not checked yet
    private ModelType recommendedModel = null; // Store the recommended model
    private String hardwareInfoText = "Checking hardware..."; // Text to display hardware info/recommendation

    // UI elements
    private ButtonWidget downloadLlamaButton; // Specific button for Llama
    private ButtonWidget downloadQwenButton; // Specific button for Qwen
    private ButtonWidget cancelButton;
    private ButtonWidget continueButton;
    private ButtonWidget loginButton;
    private ButtonWidget configureApiButton;

    public ModelDownloadScreen(Screen parent, VillagesConfig config) { // Accept main config
        super(Text.literal("Villages Reborn - AI Model Setup"));
        this.parent = parent;
        this.config = config;
    }

    @Override
    protected void init() {
        super.init();
        
        int centerX = this.width / 2;
        int buttonWidth = Math.min(200, this.width / 2);
        
        // Check hardware and login requirements (only once)
        if (detectedVramBytes == -1) { // Only check if not already checked
            detectedVramBytes = 0; // Mark as checking
            CompletableFuture.runAsync(() -> {
                detectedVramBytes = HardwareUtil.getGpuVramBytes();
                double detectedVramGiB = HardwareUtil.bytesToGiB(detectedVramBytes);

                // Determine recommendation based on VRAM (adjust thresholds as needed)
                if (detectedVramBytes <= 0) { // Error or no detection
                    recommendedModel = null; // Cannot recommend
                    hardwareInfoText = "§eCould not detect VRAM. Using API is recommended.§r";
                } else if (detectedVramGiB < 3.0) { // Example: Less than 3 GiB VRAM
                    recommendedModel = null; // Recommend API
                    hardwareInfoText = String.format("Detected VRAM: %.1f GiB. §eUsing API is recommended.§r", detectedVramGiB);
                } else if (detectedVramGiB < 5.0) { // Example: 3-5 GiB VRAM
                    recommendedModel = ModelType.QWEN2_0_5B;
                    hardwareInfoText = String.format("Detected VRAM: %.1f GiB. Recommended: %s (Faster, ~3GB RAM)", detectedVramGiB, recommendedModel.getId());
                } else { // Example: 5+ GiB VRAM
                    // Assuming LLAMA2 enum represents Llama 3.2 1B based on ModelChecker logic
                    recommendedModel = ModelType.LLAMA2; // Need to ensure LLAMA2 is defined and linked in ModelChecker
                    hardwareInfoText = String.format("Detected VRAM: %.1f GiB. Recommended: %s (Higher Quality, ~6GB RAM)", detectedVramGiB, recommendedModel.getId());
                }
                // Refresh UI after hardware check
                MinecraftClient.getInstance().execute(this::clearAndInitButtons);
            });
        }

        if (!loginChecked) {
            loginChecked = true;
            // Assuming checkIfModelRequiresLogin checks the potentially larger model (Llama)
            ModelChecker.checkIfModelRequiresLogin().thenAccept(loginRequired -> {
                requiresLogin = loginRequired;
                // Refresh UI after login check
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
            cancelButton = ButtonWidget.builder(Text.literal("Cancel Download"), button -> {
                cancelRequested.set(true); // Signal cancellation
                // Actual download cancellation might need more logic in ModelChecker/Downloader if possible
                isDownloading = false;
                downloadProgress = 0.0; // Reset progress
                clearAndInitButtons(); // Rebuild UI
            })
            .dimensions(centerX - buttonWidth / 2, height / 2 + 80, buttonWidth, 20)
            .build();
            addDrawableChild(cancelButton);
            return; // Don't add other buttons while downloading
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
                MinecraftClient.getInstance().setScreen(new SetupScreen(config)); // Pass main config
            })
            .dimensions(centerX - buttonWidth / 2, height / 2 + 30, buttonWidth, 20)
            .build();
            addDrawableChild(configureApiButton);
        } else {
            // --- Download Buttons (when no login required or after login) ---
            int downloadButtonY = height / 2;

            // Qwen2 0.5B Button
            boolean isQwenRecommended = recommendedModel == ModelType.QWEN2_0_5B;
            String qwenButtonText = "Download Qwen2 0.5B (~0.6GB)" + (isQwenRecommended ? " [Recommended]" : "");
            downloadQwenButton = ButtonWidget.builder(Text.literal(qwenButtonText), button -> {
                startDownload(ModelType.QWEN2_0_5B);
            })
            .dimensions(centerX - buttonWidth / 2, downloadButtonY, buttonWidth, 20)
            .build();
            addDrawableChild(downloadQwenButton);
            downloadButtonY += 24; // Move next button down

            // Llama 3.2 1B Button (Assuming LLAMA2 enum represents this)
            // Ensure LLAMA2 is defined in ModelType and linked in ModelChecker
            boolean isLlamaRecommended = recommendedModel == ModelType.LLAMA2;
            String llamaButtonText = "Download Llama 3.2 1B (~1.0GB)" + (isLlamaRecommended ? " [Recommended]" : "");
            downloadLlamaButton = ButtonWidget.builder(Text.literal(llamaButtonText), button -> {
                 // Check login requirement specifically for Llama if needed here
                 if (requiresLogin) {
                     // Maybe show a message or disable button if login is still needed for this specific model
                     LOGGER.warn("Llama download attempted but login required.");
                     // Optionally re-trigger login flow or show message
                 } else {
                     startDownload(ModelType.LLAMA2); // Use the correct enum
                 }
            })
            .dimensions(centerX - buttonWidth / 2, downloadButtonY, buttonWidth, 20)
            .build();
            // Disable Llama button if login is required for it
            if (requiresLogin) {
                downloadLlamaButton.active = false;
                downloadLlamaButton.setTooltip(Tooltip.of(Text.literal("Requires HuggingFace login (see above)")));
            }
            addDrawableChild(downloadLlamaButton);
            downloadButtonY += 24; // Move next button down

            // Adjust position of Skip button based on download buttons
            int skipButtonY = downloadButtonY; // Position below last download button

            // Always add skip button
            ButtonWidget skipButton = ButtonWidget.builder(Text.literal("Skip (Use External API)"), button -> {
                MinecraftClient.getInstance().setScreen(new SetupScreen(config));
            })
            .dimensions(centerX - buttonWidth / 2, skipButtonY, buttonWidth, 20)
            .build();
            addDrawableChild(skipButton);

        }
        
        // Removed redundant Skip button logic, handled within the requiresLogin check above
        
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
                "Would you like to download the Llama 3.2 model? (~1.0 GB)", // Added ~ for approximate size
                "",
                "§eWarning: This is a large download and may take several minutes.§r", // Added warning line
                "§ePlease do not close the game during the download.§r", // Added advice line
                "",
                "The model will be stored locally and provides better performance.",
                "",
                hardwareInfoText // Display hardware info and recommendation
            };
        }
        
        for (String line : lines) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(line), centerX, textY, 0xFFFFFF);
            textY += 12;
        }
        
        super.render(context, mouseX, mouseY, delta);
    }
    
    /**
     * Starts the download process for the specified model type.
     * @param modelToDownload The ModelType enum constant for the model to download.
     */
    private void startDownload(ModelType modelToDownload) {
        isDownloading = true;
        downloadProgress = 0.0;
        cancelRequested.set(false); // Reset cancellation flag
        clearAndInitButtons(); // Update UI to show progress bar/cancel button

        ModelChecker.downloadModel(modelToDownload, progress -> {
            // Check cancellation flag *before* updating UI or state
            if (cancelRequested.get()) {
                // If download was cancelled, we might need to ensure the download task actually stops.
                // OSHI's download handler might not support cancellation directly.
                // For now, we just stop updating the UI and state here.
                LOGGER.info("Download cancelled by user for {}.", modelToDownload.getId());
                // No further UI updates or state changes if cancelled.
                return;
            }

            // Update progress on the main thread
            MinecraftClient.getInstance().execute(() -> {
                downloadProgress = progress;
                if (progress >= 1.0) {
                    downloadComplete = true;
                    isDownloading = false;
                    clearAndInitButtons(); // Update UI to show "Continue"
                }
                // No need for else, progress bar updates in render()
            });

        }).exceptionally(e -> {
            // Handle download failure on the main thread
            MinecraftClient.getInstance().execute(() -> {
                LOGGER.error("Download failed for {}", modelToDownload.getId(), e);
                isDownloading = false;
                // Optionally display an error message to the user here
                // e.g., add a field like `downloadErrorText` and display it in render()
                clearAndInitButtons(); // Rebuild UI to show download buttons again
            });
            return null;
        });
    }
    
    @Override
    public boolean shouldCloseOnEsc() {
        return !isDownloading;
    }
}