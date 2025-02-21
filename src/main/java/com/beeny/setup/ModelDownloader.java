package com.beeny.setup;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;

public class ModelDownloader {
    private static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn");
    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_MS = 1000;
    private final Map<String, DownloadStatus> activeDownloads = new ConcurrentHashMap<>();
    private final Path downloadDir;
    
    private record DownloadStatus(CompletableFuture<Path> future, float progress, int retryCount) {}

    public ModelDownloader(Path downloadDir) {
        this.downloadDir = downloadDir;
        downloadDir.toFile().mkdirs();
    }

    public CompletableFuture<Path> downloadModel(String modelName, String url, MinecraftClient client) {
        if (activeDownloads.containsKey(modelName)) {
            return activeDownloads.get(modelName).future();
        }

        CompletableFuture<Path> future = new CompletableFuture<>();
        Path destination = downloadDir.resolve(modelName + ".bin");

        if (Files.exists(destination)) {
            future.complete(destination);
            return future;
        }

        downloadWithRetry(modelName, url, destination, client, 0, future);
        activeDownloads.put(modelName, new DownloadStatus(future, 0f, 0));
        return future;
    }

    private void downloadWithRetry(String modelName, String url, Path destination, 
                                 MinecraftClient client, int retryCount, 
                                 CompletableFuture<Path> future) {
        HttpClient.newHttpClient()
            .sendAsync(
                HttpRequest.newBuilder(URI.create(url)).build(),
                HttpResponse.BodyHandlers.ofInputStream()
            )
            .thenAccept(response -> {
                if (response.statusCode() != 200) {
                    handleDownloadError(modelName, url, destination, client, 
                        retryCount, future, new IOException("HTTP " + response.statusCode()));
                    return;
                }

                try (InputStream in = response.body();
                     OutputStream out = Files.newOutputStream(destination)) {
                    
                    long totalBytes = response.headers()
                        .firstValueAsLong("Content-Length")
                        .orElse(100_000_000L); // Default to 100MB if unknown
                    byte[] buffer = new byte[8192];
                    long downloadedBytes = 0;
                    int bytesRead;

                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                        downloadedBytes += bytesRead;
                        updateProgress(modelName, (float)downloadedBytes / totalBytes);
                        
                        if (client.currentScreen instanceof SetupScreen screen) {
                            screen.setProgress((float)downloadedBytes / totalBytes);
                        }
                    }

                    future.complete(destination);
                    activeDownloads.remove(modelName);
                } catch (IOException e) {
                    handleDownloadError(modelName, url, destination, client, 
                        retryCount, future, e);
                }
            })
            .exceptionally(e -> {
                handleDownloadError(modelName, url, destination, client, 
                    retryCount, future, e);
                return null;
            });
    }

    private void handleDownloadError(String modelName, String url, Path destination,
                                   MinecraftClient client, int retryCount,
                                   CompletableFuture<Path> future, Throwable error) {
        LOGGER.error("Error downloading model {}: {}", modelName, error.getMessage());
        
        if (retryCount < MAX_RETRIES) {
            LOGGER.info("Retrying download for {} (attempt {}/{})", 
                modelName, retryCount + 1, MAX_RETRIES);
            
            CompletableFuture.delayedExecutor(RETRY_DELAY_MS, TimeUnit.MILLISECONDS)
                .execute(() -> downloadWithRetry(modelName, url, destination, 
                    client, retryCount + 1, future));
                    
            activeDownloads.put(modelName, 
                new DownloadStatus(future, 0f, retryCount + 1));
        } else {
            future.completeExceptionally(error);
            activeDownloads.remove(modelName);
            client.execute(() -> {
                if (client.currentScreen instanceof SetupScreen) {
                    client.setScreen(new DownloadErrorScreen(
                        Text.literal("Download Failed"),
                        Text.literal("Failed to download " + modelName + 
                            ". Check your internet connection and try again.")
                    ));
                }
            });
        }
    }

    private void updateProgress(String modelName, float progress) {
        DownloadStatus status = activeDownloads.get(modelName);
        if (status != null) {
            activeDownloads.put(modelName, 
                new DownloadStatus(status.future(), progress, status.retryCount()));
        }
    }

    public float getProgress(String modelName) {
        return activeDownloads.containsKey(modelName) ? 
            activeDownloads.get(modelName).progress() : 1.0f;
    }

    public boolean isDownloading(String modelName) {
        return activeDownloads.containsKey(modelName);
    }
}