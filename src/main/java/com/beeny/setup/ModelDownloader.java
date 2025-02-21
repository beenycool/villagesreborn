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

public class ModelDownloader {
    private static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn");

    public static CompletableFuture<Path> downloadModel(String modelName, String expectedHash, Consumer<Float> progressCallback) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String modelUrl = "https://example.com/models/" + modelName; // Replace with actual model hosting URL
                Path modelsDir = FabricLoader.getInstance().getConfigDir().resolve("villagesreborn/models");
                Path modelPath = modelsDir.resolve(modelName);
                Files.createDirectories(modelsDir);

                // Download the model
                HttpURLConnection conn = (HttpURLConnection) new URL(modelUrl).openConnection();
                long totalSize = conn.getContentLengthLong();

                try (InputStream in = conn.getInputStream();
                     OutputStream out = Files.newOutputStream(modelPath)) {
                    byte[] buffer = new byte[8192];
                    MessageDigest digest = MessageDigest.getInstance("SHA-256");
                    long downloaded = 0;
                    int bytesRead;

                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                        digest.update(buffer, 0, bytesRead);
                        downloaded += bytesRead;
                        progressCallback.accept((float) downloaded / totalSize);
                    }

                    // Verify hash
                    String downloadedHash = bytesToHex(digest.digest());
                    if (!downloadedHash.equals(expectedHash)) {
                        Files.delete(modelPath);
                        throw new IOException("Model hash verification failed");
                    }
                }

                return modelPath;
            } catch (IOException | NoSuchAlgorithmException e) {
                LOGGER.error("Failed to download model: " + modelName, e);
                throw new RuntimeException("Model download failed", e);
            }
        });
    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}