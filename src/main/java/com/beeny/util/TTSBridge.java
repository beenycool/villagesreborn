package com.beeny.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class TTSBridge {
    private static final String PYTHON_SCRIPT = "kittentts/generate_tts.py";
    private static final long TIMEOUT_SECONDS = 30;

    public static String generateTTS(String text) throws IOException, InterruptedException {
        // Create process builder
        ProcessBuilder processBuilder = new ProcessBuilder("python3", PYTHON_SCRIPT, text);
        processBuilder.redirectErrorStream(true);
        
        // Start process
        Process process = processBuilder.start();
        
        // Wait for process to complete with timeout
        if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            process.destroy();
            throw new IOException("TTS generation timed out after " + TIMEOUT_SECONDS + " seconds");
        }
        
        // Check exit code
        if (process.exitValue() != 0) {
            throw new IOException("TTS generation failed with exit code: " + process.exitValue());
        }
        
        // Read output
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String output = reader.readLine();
            
            // Validate output
            if (output == null || output.isEmpty()) {
                throw new IOException("No output received from TTS script");
            }
            
            File wavFile = new File(output);
            if (!wavFile.exists()) {
                throw new IOException("Generated WAV file not found: " + output);
            }
            
            return output;
        }
    }
}