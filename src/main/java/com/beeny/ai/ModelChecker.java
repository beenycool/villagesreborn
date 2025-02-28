package com.beeny.ai;

import com.beeny.Villagesreborn;
import com.beeny.setup.LLMConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Handles checking for and downloading AI models for the Villages Reborn mod.
 */
public class ModelChecker {
    private static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn");
    private static final String LLAMA_MODEL_URL = "https://huggingface.co/meta-llama/Llama-3.2-1B/resolve/main/model.safetensors?download=true";
    private static final String MODELS_FOLDER = "models";
    private static final String LLAMA_MODEL_FILENAME = "llama-3.2-1b.safetensors";

    /**
     * Check if a local AI model is available
     * @return true if a local model exists, false otherwise
     */
    public static boolean hasLocalModel() {
        Path modelPath = getModelPath();
        return Files.exists(modelPath) && Files.isReadable(modelPath);
    }

    /**
     * Get the path where the model should be stored
     * @return Path to the model file
     */
    public static Path getModelPath() {
        Path modelsDir = FabricLoader.getInstance().getGameDir().resolve(MODELS_FOLDER);
        return modelsDir.resolve(LLAMA_MODEL_FILENAME);
    }

    /**
     * Download the Llama 3.2 model
     * @param progressCallback Callback to report download progress (0.0 to 1.0)
     * @return CompletableFuture that completes when download is finished
     */
    public static CompletableFuture<Path> downloadModel(Consumer<Double> progressCallback) {
        return CompletableFuture.supplyAsync(() -> {
            Path modelPath = getModelPath();
            Path modelsDir = modelPath.getParent();
            
            try {
                // Create models directory if it doesn't exist
                if (!Files.exists(modelsDir)) {
                    Files.createDirectories(modelsDir);
                }
                
                LOGGER.info("Starting download of Llama 3.2 model...");
                progressCallback.accept(0.0);
                
                // Create a temporary file for downloading
                Path tempFile = Files.createTempFile("llama-download", ".tmp");
                
                // Set up HTTP client with redirect following
                HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
                
                // Start the download
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(LLAMA_MODEL_URL))
                    .header("User-Agent", "Villages-Reborn-Mod/1.0")
                    .GET()
                    .build();
                
                // Send request and handle response
                HttpResponse<Path> response = client.send(request, 
                    HttpResponse.BodyHandlers.ofFileDownload(tempFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));
                
                if (response.statusCode() == 200) {
                    // Move the temp file to the final destination
                    Files.move(tempFile, modelPath, StandardCopyOption.REPLACE_EXISTING);
                    
                    // Update configuration
                    LLMConfig config = Villagesreborn.getLLMConfig();
                    config.setProvider("local");
                    config.setModelType("llama-3.2-1b");
                    config.saveConfig();
                    
                    LOGGER.info("Llama 3.2 model downloaded successfully to: {}", modelPath);
                    progressCallback.accept(1.0);
                    return modelPath;
                } else {
                    throw new IOException("Failed to download model: HTTP " + response.statusCode());
                }
            } catch (IOException | InterruptedException e) {
                LOGGER.error("Error downloading Llama model", e);
                progressCallback.accept(0.0);
                throw new RuntimeException("Failed to download model", e);
            }
        });
    }
    
    /**
     * Check if HuggingFace requires login for the model
     * @return CompletableFuture that completes with true if login is required, false otherwise
     */
    public static CompletableFuture<Boolean> checkIfModelRequiresLogin() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
                
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(LLAMA_MODEL_URL))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .build();
                
                HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
                
                // Check if we got redirected to a login page
                boolean requiresLogin = 
                    response.statusCode() == 401 || 
                    response.statusCode() == 403 ||
                    response.uri().toString().contains("login");
                
                return requiresLogin;
            } catch (Exception e) {
                LOGGER.error("Error checking model access requirements", e);
                // Assume it requires login if we can't check
                return true;
            }
        });
    }
}