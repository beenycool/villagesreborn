package com.beeny.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for config JSON round-trip and validation logic.
 * These tests avoid any Minecraft server classes and only touch the config manager/static config.
 */
public class ConfigManagerTest {

    private final Path configDir = Path.of("config");
    private final Path configFile = configDir.resolve("villagersreborn.json");

    @AfterEach
    void cleanup() throws IOException {
        // Remove file created during tests to keep isolation between tests
        if (Files.exists(configFile)) {
            Files.delete(configFile);
        }
    }

    @Test
    @DisplayName("saveConfig() creates file and loadConfig() applies edited values (round-trip)")
    void testRoundTripSaveAndLoad() throws IOException {
        // Arrange: set some in-memory defaults we can later verify were written
        VillagersRebornConfig.VILLAGER_SCAN_CHUNK_RADIUS = 7;
        VillagersRebornConfig.HAPPINESS_NEUTRAL_THRESHOLD = 55;
        VillagersRebornConfig.HAPPINESS_DECAY_RATE = 2;
        VillagersRebornConfig.HAPPINESS_RECOVERY_RATE = 3;

        VillagersRebornConfig.ENABLE_DYNAMIC_DIALOGUE = false;
        VillagersRebornConfig.LLM_PROVIDER = "local";
        VillagersRebornConfig.LLM_API_ENDPOINT = "";
        VillagersRebornConfig.LLM_MODEL = "local-model";
        VillagersRebornConfig.LLM_TEMPERATURE = 1.2;
        VillagersRebornConfig.LLM_MAX_TOKENS = 777;
        VillagersRebornConfig.LLM_REQUEST_TIMEOUT = 4242;
        VillagersRebornConfig.FALLBACK_TO_STATIC = false;
        VillagersRebornConfig.ENABLE_DIALOGUE_CACHE = false;
        VillagersRebornConfig.DIALOGUE_CACHE_SIZE = 123;
        VillagersRebornConfig.CONVERSATION_HISTORY_LIMIT = 8;
        VillagersRebornConfig.LLM_LOCAL_URL = "http://localhost:9090";

        VillagersRebornConfig.AI_EMOTION_UPDATE_INTERVAL = 1111;
        VillagersRebornConfig.AI_MANAGER_UPDATE_INTERVAL = 2222;
        VillagersRebornConfig.AI_GOAP_UPDATE_INTERVAL = 3333;
        VillagersRebornConfig.MARRIAGE_PACKET_COOLDOWN = 4444;

        // Act: write config file using current in-memory values
        ConfigManager.saveConfig();

        assertTrue(Files.exists(configFile), "saveConfig should create the config file");

        // Simulate a user editing the JSON file with different values
        String json = Files.readString(configFile);
        json = json
                .replace("\"villagerScanChunkRadius\": 7", "\"villagerScanChunkRadius\": 9")
                .replace("\"happinessNeutralThreshold\": 55", "\"happinessNeutralThreshold\": 60")
                .replace("\"happinessDecayRate\": 2", "\"happinessDecayRate\": 5")
                .replace("\"happinessRecoveryRate\": 3", "\"happinessRecoveryRate\": 6")
                .replace("\"enableDynamicDialogue\": false", "\"enableDynamicDialogue\": true")
                .replace("\"llmProvider\": \"local\"", "\"llmProvider\": \"gemini\"")
                .replace("\"llmApiEndpoint\": \"\"", "\"llmApiEndpoint\": \"https://api.example.com\"")
                .replace("\"llmModel\": \"local-model\"", "\"llmModel\": \"gemini-1.5\"")
                .replace("\"llmTemperature\": 1.2", "\"llmTemperature\": 0.5")
                .replace("\"llmMaxTokens\": 777", "\"llmMaxTokens\": 888")
                .replace("\"llmRequestTimeout\": 4242", "\"llmRequestTimeout\": 5000")
                .replace("\"fallbackToStatic\": false", "\"fallbackToStatic\": true")
                .replace("\"enableDialogueCache\": false", "\"enableDialogueCache\": true")
                .replace("\"dialogueCacheSize\": 123", "\"dialogueCacheSize\": 456")
                .replace("\"conversationHistoryLimit\": 8", "\"conversationHistoryLimit\": 9")
                .replace("\"llmLocalUrl\": \"http://localhost:9090\"", "\"llmLocalUrl\": \"http://localhost:8081\"")
                .replace("\"aiEmotionUpdateInterval\": 1111", "\"aiEmotionUpdateInterval\": 1500")
                .replace("\"aiManagerUpdateInterval\": 2222", "\"aiManagerUpdateInterval\": 2500")
                .replace("\"aiGoapUpdateInterval\": 3333", "\"aiGoapUpdateInterval\": 3500")
                .replace("\"marriagePacketCooldown\": 4444", "\"marriagePacketCooldown\": 5500");
        Files.writeString(configFile, json);

        // Act: load config and apply validation + assignment to static fields
        ConfigManager.loadConfig();

        // Assert: confirm the values were read and applied
        assertEquals(9, VillagersRebornConfig.VILLAGER_SCAN_CHUNK_RADIUS);
        assertEquals(60, VillagersRebornConfig.HAPPINESS_NEUTRAL_THRESHOLD);
        assertEquals(5, VillagersRebornConfig.HAPPINESS_DECAY_RATE);
        assertEquals(6, VillagersRebornConfig.HAPPINESS_RECOVERY_RATE);

        assertTrue(VillagersRebornConfig.ENABLE_DYNAMIC_DIALOGUE);
        assertEquals("gemini", VillagersRebornConfig.LLM_PROVIDER);
        assertEquals("https://api.example.com", VillagersRebornConfig.LLM_API_ENDPOINT);
        assertEquals("gemini-1.5", VillagersRebornConfig.LLM_MODEL);
        assertEquals(0.5, VillagersRebornConfig.LLM_TEMPERATURE, 1e-9);
        assertEquals(888, VillagersRebornConfig.LLM_MAX_TOKENS);
        assertEquals(5000, VillagersRebornConfig.LLM_REQUEST_TIMEOUT);
        assertTrue(VillagersRebornConfig.FALLBACK_TO_STATIC);
        assertTrue(VillagersRebornConfig.ENABLE_DIALOGUE_CACHE);
        assertEquals(456, VillagersRebornConfig.DIALOGUE_CACHE_SIZE);
        assertEquals(9, VillagersRebornConfig.CONVERSATION_HISTORY_LIMIT);
        assertEquals("http://localhost:8081", VillagersRebornConfig.LLM_LOCAL_URL);

        assertEquals(1500, VillagersRebornConfig.AI_EMOTION_UPDATE_INTERVAL);
        assertEquals(2500, VillagersRebornConfig.AI_MANAGER_UPDATE_INTERVAL);
        assertEquals(3500, VillagersRebornConfig.AI_GOAP_UPDATE_INTERVAL);
        assertEquals(5500, VillagersRebornConfig.MARRIAGE_PACKET_COOLDOWN);
    }

    @Test
    @DisplayName("Validation: invalid/out-of-range values fall back to defaults")
    void testValidationFallbacks() throws IOException {
        // Establish defaults
        VillagersRebornConfig.VILLAGER_SCAN_CHUNK_RADIUS = 8; // valid default in code
        VillagersRebornConfig.LLM_TEMPERATURE = 0.7;
        VillagersRebornConfig.LLM_MAX_TOKENS = 150;
        VillagersRebornConfig.LLM_REQUEST_TIMEOUT = 10000;
        VillagersRebornConfig.LLM_PROVIDER = "gemini";
        VillagersRebornConfig.ENABLE_DYNAMIC_DIALOGUE = true;

        Files.createDirectories(configDir);

        // Invalid and out-of-range JSON to trigger fallbacks
        String invalidJson = "{\n" +
                "  \"villagerScanChunkRadius\": -1,\n" +         // below min (0..64) -> fallback 8
                "  \"llmTemperature\": 5.0,\n" +                // above max (2.0) -> fallback 0.7
                "  \"llmMaxTokens\": 0,\n" +                    // below min (1) -> fallback 150
                "  \"llmRequestTimeout\": 50,\n" +              // below min (100) -> fallback 10000
                "  \"llmProvider\": \"unsupported\",\n" +       // not in allowed list -> fallback gemini
                "  \"enableDynamicDialogue\": \"notABool\"\n" + // invalid type -> fallback true
                "}";
        Files.writeString(configFile, invalidJson);

        // Act: load invalid config
        ConfigManager.loadConfig();

        // Assert: validated fallbacks applied
        assertEquals(8, VillagersRebornConfig.VILLAGER_SCAN_CHUNK_RADIUS);
        assertEquals(0.7, VillagersRebornConfig.LLM_TEMPERATURE, 1e-9);
        assertEquals(150, VillagersRebornConfig.LLM_MAX_TOKENS);
        assertEquals(10000, VillagersRebornConfig.LLM_REQUEST_TIMEOUT);
        assertEquals("gemini", VillagersRebornConfig.LLM_PROVIDER);
        assertTrue(VillagersRebornConfig.ENABLE_DYNAMIC_DIALOGUE);
    }
}