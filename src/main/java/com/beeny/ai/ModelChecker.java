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

public class ModelChecker {
    private static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn");
    private static final String LLAMA_MODEL_URL = "https://huggingface.co/meta-llama/Llama-3.2-1B/resolve/main/model.safetensors?download=true";
    private static final String MODELS_FOLDER = "models";
    private static final String LLAMA_MODEL_FILENAME = "llama-3.2-1b.safetensors";

    public static boolean hasLocalModel() {
        Path modelPath = getModelPath();
        return Files.exists(modelPath) && Files.isReadable(modelPath);
    }

    public static Path getModelPath() {
        Path modelsDir = FabricLoader.getInstance().getGameDir().resolve(MODELS_FOLDER);
        return modelsDir.resolve(LLAMA_MODEL_FILENAME);
    }

    public static CompletableFuture<Path> downloadModel(Consumer<Double> progressCallback) {
        return CompletableFuture.supplyAsync(() -> {
            Path modelPath = getModelPath();
            Path modelsDir = modelPath.getParent();
            try {
                if (!Files.exists(modelsDir)) Files.createDirectories(modelsDir);
                LOGGER.info("Starting download of Llama 3.2 model...");
                progressCallback.accept(0.0);
                Path tempFile = Files.createTempFile("llama-download", ".tmp");
                HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
                HttpRequest request = HttpRequest.newBuilder().uri(URI.create(LLAMA_MODEL_URL)).header("User-Agent", "Villages-Reborn-Mod/1.0").GET().build();
                HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFileDownload(tempFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));
                if (response.statusCode() == 200) {
                    Files.move(tempFile, modelPath, StandardCopyOption.REPLACE_EXISTING);
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

    public static CompletableFuture<Boolean> checkIfModelRequiresLogin() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
                HttpRequest request = HttpRequest.newBuilder().uri(URI.create(LLAMA_MODEL_URL)).method("HEAD", HttpRequest.BodyPublishers.noBody()).build();
                HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
                return response.statusCode() == 401 || response.statusCode() == 403 || response.uri().toString().contains("login");
            } catch (Exception e) {
                LOGGER.error("Error checking model access requirements", e);
                return true;
            }
        });
    }
}
