package com.beeny.ai;

import com.beeny.Villagesreborn;
import com.beeny.config.VillagesConfig;
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

public class ModelChecker {
    private static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn");
    // Model Definitions
    private static final String MODELS_FOLDER = "models";

    // Llama 3.2 1B
    private static final String LLAMA_MODEL_FILENAME = "llama-3.2-1b.safetensors";
    private static final String LLAMA_MODEL_URL = "https://huggingface.co/meta-llama/Llama-3.2-1B/resolve/main/model.safetensors?download=true"; // Requires login?

    // Qwen2 0.5B (Example URL - needs verification)
    private static final String QWEN2_MODEL_FILENAME = "qwen2-0.5b.safetensors";
    private static final String QWEN2_MODEL_URL = "https://huggingface.co/Qwen/Qwen2-0.5B/resolve/main/model.safetensors?download=true"; // Check if login needed

    // Add other models here as needed...

    /**
     * Checks if the specified local model file exists and is readable.
     * @param modelType The type of the local model to check.
     * @return true if the model file exists and is readable, false otherwise.
     */
    public static boolean hasLocalModel(ModelType modelType) {
        if (!modelType.isLocalModel() || modelType == ModelType.LOCAL) { // Exclude generic LOCAL type
             return false; // Only check specific local model types
        }
        Path modelPath = getModelPath(modelType);
        return modelPath != null && Files.exists(modelPath) && Files.isReadable(modelPath);
    }

    /**
     * Gets the expected file path for the specified local model type.
     * @param modelType The type of the local model.
     * @return The Path object for the model file, or null if the modelType is not a known local model.
     */
    public static Path getModelPath(ModelType modelType) {
        String filename = getModelFilename(modelType);
        if (filename == null) {
            return null; // Not a known local model type
        }
        Path modelsDir = FabricLoader.getInstance().getGameDir().resolve(MODELS_FOLDER);
        return modelsDir.resolve(filename);
    }

    /** Helper to get filename based on ModelType */
    private static String getModelFilename(ModelType modelType) {
        return switch (modelType) {
            case QWEN2_0_5B -> QWEN2_MODEL_FILENAME;
            // case LLAMA2 -> LLAMA_MODEL_FILENAME; // If Llama 3.2 is represented by LLAMA2 enum
            // Add other specific local models here
            default -> null; // Not a downloadable local model we know
        };
    }

     /** Helper to get download URL based on ModelType */
    private static String getModelDownloadUrl(ModelType modelType) {
         return switch (modelType) {
            case QWEN2_0_5B -> QWEN2_MODEL_URL;
            // case LLAMA2 -> LLAMA_MODEL_URL;
            // Add other specific local models here
            default -> null; // Not a downloadable local model we know
        };
    }

    /**
     * Downloads the specified local model file.
     * @param modelType The type of the local model to download.
     * @param progressCallback Consumer to report download progress (0.0 to 1.0).
     * @return A CompletableFuture containing the path to the downloaded model, or an exception on failure.
     */
    public static CompletableFuture<Path> downloadModel(ModelType modelType, Consumer<Double> progressCallback) {
        return CompletableFuture.supplyAsync(() -> {
            if (!modelType.isLocalModel() || modelType == ModelType.LOCAL) {
                 throw new IllegalArgumentException("Cannot download non-local or generic local model type: " + modelType);
            }

            Path modelPath = getModelPath(modelType);
            String modelUrl = getModelDownloadUrl(modelType);
            String modelName = modelType.getId(); // e.g., "qwen2-0.5b"

            if (modelPath == null || modelUrl == null) {
                 throw new IllegalArgumentException("Unknown local model type for download: " + modelType);
            }
            Path modelsDir = modelPath.getParent();
            Path tempFile = null; // Declare outside try
            try {
                if (!Files.exists(modelsDir)) Files.createDirectories(modelsDir);
                LOGGER.info("Starting download of {} model from {}...", modelName, modelUrl);
                progressCallback.accept(0.0);
                tempFile = Files.createTempFile(modelName + "-download", ".tmp"); // Assign inside try
                HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
                HttpRequest request = HttpRequest.newBuilder().uri(URI.create(modelUrl)).header("User-Agent", "Villages-Reborn-Mod/1.0").GET().build();
                // TODO: Implement actual progress tracking if BodyHandlers.ofFileDownload doesn't provide it.
                // This might involve reading the stream manually and updating the callback.
                HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFileDownload(tempFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));
                if (response.statusCode() == 200) {
                    Files.move(tempFile, modelPath, StandardCopyOption.REPLACE_EXISTING);
                    VillagesConfig config = VillagesConfig.getInstance();
                    config.getLLMSettings().setProvider("local"); // Use nested settings
                    config.getLLMSettings().setModelType(modelType.getId()); // Set model type to the downloaded one
                    config.save(); // Use correct save method
                    LOGGER.info("{} model downloaded successfully to: {}", modelName, modelPath);
                    progressCallback.accept(1.0);
                    return modelPath;
                } else {
                    throw new IOException("Failed to download " + modelName + " model: HTTP " + response.statusCode());
                }
            } catch (IOException | InterruptedException e) {
                LOGGER.error("Error downloading {} model", modelName, e);
                progressCallback.accept(0.0); // Ensure progress is reset on error
                // Clean up temp file on error
                if (tempFile != null) { // Check if tempFile was assigned
                    try { Files.deleteIfExists(tempFile); } catch (IOException ioex) { LOGGER.error("Failed to delete temp download file: {}", tempFile, ioex); }
                }
                throw new RuntimeException("Failed to download " + modelName + " model", e);
            }
        });
    }

    public static CompletableFuture<Boolean> checkIfModelRequiresLogin() {
        // TODO: This currently only checks Llama 3.2. Needs to be adapted if checking other models like Qwen2.
        // For now, assume only Llama might require login based on original code.
        return CompletableFuture.supplyAsync(() -> {
            String urlToCheck = LLAMA_MODEL_URL; // Hardcoded to Llama for now
            LOGGER.info("Checking access requirements for model URL: {}", urlToCheck);
            try {
                HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
                HttpRequest request = HttpRequest.newBuilder().uri(URI.create(urlToCheck)).method("HEAD", HttpRequest.BodyPublishers.noBody()).build();
                HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
                boolean requiresLogin = response.statusCode() == 401 || response.statusCode() == 403 || response.uri().toString().contains("login");
                LOGGER.info("Model access check for {} completed. Status: {}, Requires Login: {}", urlToCheck, response.statusCode(), requiresLogin);
                return requiresLogin;
            } catch (Exception e) {
                LOGGER.error("Error checking model access requirements for {}", urlToCheck, e);
                // Assume login might be required if check fails? Or return false? Let's return false for now.
                return false;
            }
        });
    }
}
