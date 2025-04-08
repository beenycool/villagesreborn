package com.beeny.commands;

import com.beeny.Villagesreborn;
import com.beeny.ai.LLMService;
import com.beeny.ai.ModelType;
import com.beeny.ai.provider.*;
import com.beeny.config.LLMSettings; // Import LLMSettings
import com.beeny.config.VillagesConfig; // Import VillagesConfig
import com.beeny.village.*; // Import village package
import com.beeny.village.VillagerAI; // Specific AI class if needed
// import com.beeny.setup.ModelDownloader; // Unused?
import com.beeny.ai.ModelChecker;
import java.util.List; // Import List for sound count calculation
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files; // Added import
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DebugCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, boolean dedicated) {
        dispatcher.register(CommandManager.literal("villagesreborn_debug")
            .requires(source -> source.hasPermissionLevel(2)) // Require OP level 2
            .executes(DebugCommand::runDebug));
    }

    private static int runDebug(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        source.sendFeedback(() -> Text.literal("Starting Villages Reborn debug checks..."), false);

        StringBuilder report = new StringBuilder();
        report.append("Villages Reborn Debug Report - ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)).append("\n");
        report.append("========================================\n\n");

        // --- Core System Checks ---
        report.append("Section 0: Core System Checks\n");
        report.append("-----------------------------\n");
        testCoreSystems(report);
        report.append("\n");

        // --- Placeholder for Test Sections ---
        report.append("Section 1: AI Provider & LLM Service Checks\n");
        report.append("-----------------------------------------\n");
        testAIProviders(report);
        report.append("\n");
        report.append("Section 2: Cultural Asset Manager Checks\n");
        report.append("--------------------------------------\n");
        testCulturalManagers(report);
        report.append("\n");

        report.append("Section 3: Villager Management System Checks\n");
        report.append("------------------------------------------\n");
        testVillagerSystems(report);
        report.append("\n");
        report.append("Section 4: Local AI Model Checks\n");
        report.append("------------------------------\n");
        testModelChecks(report);
        report.append("\n");

        report.append("========================================\n");
        report.append("Debug checks complete.\n");

        // --- Save Report to File ---
        String fileName = "villagesreborn_debug_report_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".txt";
        Path reportPath = Paths.get("debug_reports", fileName); // Save in a 'debug_reports' subfolder

        try {
            reportPath.getParent().toFile().mkdirs(); // Ensure directory exists
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(reportPath.toFile()))) {
                writer.write(report.toString());
                source.sendFeedback(() -> Text.literal("Debug report saved to: " + reportPath.toAbsolutePath()), false);
            }
        } catch (IOException e) {
            source.sendError(Text.literal("Failed to save debug report: " + e.getMessage()));
            // Log the exception properly in a real scenario
            e.printStackTrace();
        }

        return 1; // Success
    }

    private static void testCoreSystems(StringBuilder report) {
        // Config Check
        try {
            VillagesConfig config = VillagesConfig.getInstance();
            if (config != null) {
                report.append("[ ✓ ] VillagesConfig: Instance loaded successfully.\n");
                // Add checks for specific critical config values if needed
                // e.g., report.append(String.format("    - Default Culture: %s\n", config.getDefaultCulture()));
            } else {
                report.append("[ ✗ ] VillagesConfig: Failed to get instance! Core configuration is missing.\n");
            }
        } catch (Exception e) {
            report.append(String.format("[ ✗ ] VillagesConfig: Error during check - %s\n", e.getMessage()));
        }

        // Debug Report Directory Write Permissions Check
        try {
            Path reportDir = Paths.get("debug_reports");
            reportDir.toFile().mkdirs(); // Ensure directory exists or is created
            Path testFile = reportDir.resolve("permission_test_" + System.currentTimeMillis() + ".tmp");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(testFile.toFile()))) {
                writer.write("test");
            } // try-with-resources ensures the writer is closed
            if (testFile.toFile().delete()) {
                 report.append("[ ✓ ] Debug Directory: Write permissions confirmed for 'debug_reports'.\n");
            } else {
                 report.append("[ ! ] Debug Directory: Wrote test file to 'debug_reports', but failed to delete it.\n");
            }
        } catch (IOException e) {
            report.append(String.format("[ ✗ ] Debug Directory: Failed to write to 'debug_reports' - %s\n", e.getMessage()));
        } catch (SecurityException e) {
             report.append(String.format("[ ✗ ] Debug Directory: Security exception accessing 'debug_reports' - %s\n", e.getMessage()));
        } catch (Exception e) {
            report.append(String.format("[ ✗ ] Debug Directory: Unexpected error during permission check - %s\n", e.getMessage()));
        }
    }

    private static void testAIProviders(StringBuilder report) {
        VillagesConfig config = VillagesConfig.getInstance();
        VillagesConfig.LLMSettings llmSettings = config.getLLMSettings(); // Correct type to inner class
        String activeProvider = llmSettings.getProvider(); // Get the selected provider string from LLMSettings
        String apiKey = llmSettings.getApiKey();
        String endpoint = llmSettings.getEndpoint();
        String model = llmSettings.getModel();
        boolean isLocal = llmSettings.isLocalModel();
        String localPath = llmSettings.getLocalModelPath();

        report.append(String.format("Active AI Provider: %s\n", activeProvider));

        if (isLocal) {
            report.append(String.format("[ ✓ ] Local Model: Enabled\n"));
            report.append(String.format("      - Configured Model Type: %s\n", llmSettings.getModel().isEmpty() ? "[ ! ] (Not Set)" : llmSettings.getModel()));
            report.append(String.format("      - Expected Path: %s\n", localPath.isEmpty() ? "[ ! ] (Not Set)" : localPath));
            ModelType localModelType = ModelType.fromString(llmSettings.getModel()); // Get ModelType from config string
            boolean modelExists = ModelChecker.hasLocalModel(localModelType); // Pass ModelType
            report.append(String.format("      - Model File Status: %s\n", modelExists ? "[ ✓ ] Found" : "[ ✗ ] Missing!"));
        } else {
            report.append(String.format("[ ✓ ] Remote Provider (%s): Enabled\n", activeProvider));
            report.append(String.format("      - API Key: %s\n", apiKey.isEmpty() ? "[ ✗ ] Missing!" : "[ ✓ ] Set"));
            report.append(String.format("      - Endpoint: %s\n", endpoint.isEmpty() ? "[ i ] (Default/Not Set)" : endpoint));
            report.append(String.format("      - Model: %s\n", model.isEmpty() ? "[ i ] (Default/Not Set)" : model));

            // Specific checks based on provider string
            if ("AZURE".equalsIgnoreCase(activeProvider)) {
                if (apiKey.isEmpty() || endpoint.isEmpty()) {
                    report.append("      - Status: [ ✗ ] Config Missing! (Requires API Key and Endpoint)\n");
                } else {
                     report.append("      - Status: [ ✓ ] Configured\n");
                }
            } else { // OpenAI, OpenRouter, Cohere, Anthropic, Deepseek, Gemini, Mistral etc.
                 if (apiKey.isEmpty()) {
                    report.append("      - Status: [ ✗ ] API Key Missing!\n");
                 } else {
                     report.append("      - Status: [ ✓ ] Configured\n");
                 }
            }
        }
        // LLMService Instance Check
        try {
            LLMService service = LLMService.getInstance();
            report.append(String.format("[ %s ] LLMService: %s\n", service != null ? "✓" : "✗", service != null ? "Instance OK" : "Instance is null!"));
            if (service == null) {
                 report.append("      - Note: This usually indicates an issue during initialization or with the selected AI provider config.\n");
            } else {
                 // Could add a check for service.isReady() or similar if available
                 report.append("      - Note: Live connectivity checks are complex and not performed here.\n");
            }
        } catch (Exception e) {
             report.append(String.format("[ ✗ ] LLMService: Error getting instance - %s\n", e.getMessage()));
        }
    }

    private static void testCulturalManagers(StringBuilder report) {
        // Texture Manager Check
        try {
            // Call new public static methods instead of accessing private map
            int cultureCount = CulturalTextureManager.getRegisteredCultureCount();
            int textureCount = CulturalTextureManager.getTotalTextureCount();
            report.append(String.format("[ ✓ ] CulturalTextureManager: Loaded (%d cultures, %d textures)\n", cultureCount, textureCount));
        } catch (Exception e) { // Catch potential errors during static method calls
             report.append(String.format("[ ✗ ] CulturalTextureManager: Error during check - %s\n", e.getMessage()));
        }
        // Removed duplicate/erroneous catch block here

        // Sound Manager Check
        try {
            CulturalSoundManager soundManager = CulturalSoundManager.getInstance();
            if (soundManager != null) {
                 // Call new public methods instead of accessing private maps
                 try {
                     int cultureCount = soundManager.getRegisteredCultureCount();
                     long totalSoundCount = soundManager.getTotalSoundCount(); // Assuming this method exists and works
                     report.append(String.format("[ ✓ ] CulturalSoundManager: Loaded (%d cultures, %d total sounds)\n", cultureCount, totalSoundCount));
                 } catch (NoSuchMethodError nsme) {
                     report.append("[ ! ] CulturalSoundManager: Count methods not found (Update needed?). Instance OK.\n");
                 } catch (Exception e) { // Catch potential errors if methods aren't added yet
                     report.append("[ E ] CulturalSoundManager: Error accessing counts - " + e.getMessage() + "\n");
                 }
            } else {
                report.append("[ ✗ ] CulturalSoundManager: Instance is null!\n");
            }
        } catch (Exception e) {
            report.append(String.format("[ ✗ ] CulturalSoundManager: Error during check - %s\n", e.getMessage()));
        }
    }

    private static void testVillagerSystems(StringBuilder report) {
        try {
            VillagerManager manager = VillagerManager.getInstance();
            if (manager != null) {
                int activeVillagers = manager.getActiveVillagers().size();
                int spawnRegions = manager.getSpawnRegions().size(); // Assuming this method exists
                report.append(String.format("[ ✓ ] VillagerManager: Loaded (Tracking %d active villagers, %d spawn regions)\n", activeVillagers, spawnRegions));

                // Check MoodManager instance (assuming it's a separate singleton)
                try {
                    MoodManager moodManager = MoodManager.getInstance(); // Assuming singleton pattern
                    report.append(String.format("[ %s ] MoodManager: %s\n", moodManager != null ? "✓" : "✗", moodManager != null ? "Instance OK" : "Instance is null!"));
                } catch (Exception e) {
                    report.append(String.format("[ ✗ ] MoodManager: Error checking instance - %s\n", e.getMessage()));
                }

                // Basic VillagerAI Class Check
                 try {
                     Class.forName("com.beeny.village.VillagerAI");
                     report.append("[ ✓ ] VillagerAI: Class loaded successfully.\n");
                 } catch (ClassNotFoundException e) {
                     report.append("[ ✗ ] VillagerAI: Class not found! Critical component missing.\n");
                 } catch (Exception e) {
                     report.append(String.format("[ ✗ ] VillagerAI: Error checking class - %s\n", e.getMessage()));
                 }


                // Relationship checks are complex globally. Reporting active villagers is a good proxy.
                report.append("[ i ] Other Subsystems (Memory, Dialogue, Relationships): Status inferred from VillagerManager & active villagers.\n");
                report.append("      - Detailed checks require specific villager interaction/context.\n");

            } else {
                report.append("[ ✗ ] VillagerManager: Instance is null!\n");
            }
        } catch (Exception e) {
             report.append(String.format("[ ✗ ] VillagerManager: Error during check - %s\n", e.getMessage()));
        }
    }

    private static void testModelChecks(StringBuilder report) {
        VillagesConfig.LLMSettings llmSettings = VillagesConfig.getInstance().getLLMSettings();
        String configuredModelTypeStr = llmSettings.getModel();
        ModelType configuredModelType = ModelType.fromString(configuredModelTypeStr); // Get ModelType from config string

        // Get model path using the configured ModelType
        Path modelPathObj = ModelChecker.getModelPath(configuredModelType);
        String modelPath = (modelPathObj != null) ? modelPathObj.toString() : "(Path Undefined for ModelType)";

        // Local Model Checks (Path, Existence, Readability)
        try {
            boolean localModelEnabled = llmSettings.isLocalModel();

            if (!localModelEnabled) {
                report.append("[ i ] Local model usage is DISABLED in configuration.\n");
            } else {
                // Local model IS enabled, proceed with detailed checks
                report.append("[ i ] Local model usage is ENABLED.\n");
                report.append(String.format("    - Configured Model Type: '%s'\n", configuredModelTypeStr.isEmpty() ? "(Not Set)" : configuredModelTypeStr));
                report.append(String.format("    - Expected Path: '%s'\n", modelPath));

                // Add a check for consistency between localModel flag and actual ModelType
                if (!configuredModelType.isLocalModel() && !configuredModelTypeStr.isEmpty()) {
                     report.append("    - [ ! ] Inconsistency: Local model is enabled, but configured model type '" + configuredModelTypeStr + "' is not a local model type!\n");
                }

                // Check if the specific model file exists and is readable using the configured ModelType
                if (modelPathObj != null) { // Check if path is valid for the model type
                    boolean exists = ModelChecker.hasLocalModel(configuredModelType); // Pass ModelType
                    boolean readable = false;
                    if (exists) {
                        try {
                           readable = Files.isReadable(modelPathObj); // Use Files.isReadable
                        } catch (SecurityException se) {
                            // Ignore, readable remains false
                        }
                    }

                    if (exists && readable) {
                        report.append("    - File Status: [ ✓ ] Model file exists and is readable.\n");
                    } else if (exists && !readable) {
                        report.append("    - File Status: [ ! ] Model file exists but is NOT readable (Permissions issue?).\n");
                    } else {
                        report.append("    - File Status: [ ✗ ] Model file does NOT exist at the expected path!\n");
                    }

                    // Static ModelChecker report (confirms the check above) using the configured ModelType
                    boolean modelAvailable = ModelChecker.hasLocalModel(configuredModelType); // Pass ModelType
                    report.append(String.format("    - ModelChecker Report: [ %s ] Reports local model file for type '%s' as %s.\n",
                         modelAvailable ? "✓" : "✗",
                         configuredModelType.getId(), // Show which model type was checked
                         modelAvailable ? "Available" : "Missing/Unavailable"));

                } else {
                    // Path was null, meaning the configured model type isn't a known local one
                     report.append(String.format("    - Status: [ ! ] Configured model type '%s' is not a known local model or path is undefined.\n", configuredModelTypeStr));
                }
            } // End of localModelEnabled check

        } catch (Exception e) {
             report.append(String.format("[ ✗ ] Local Model Checks: Error during check - %s\n", e.getMessage()));
        }

        // Removed redundant Model Checker Check section as its logic is now integrated above.
    }

}