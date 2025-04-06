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

        // --- Placeholder for Test Sections ---
        report.append("Section 1: AI Provider Checks\n");
        report.append("-----------------------------\n");
        testAIProviders(report);
        report.append("\n");
        report.append("Section 2: Cultural Manager Checks\n");
        report.append("----------------------------------\n");
        testCulturalManagers(report);
        report.append("\n");

        report.append("Section 3: Villager System Checks\n");
        report.append("---------------------------------\n");
        testVillagerSystems(report);
        report.append("\n");
        report.append("Section 4: Model Checks\n");
        report.append("-----------------------\n");
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

    private static void testAIProviders(StringBuilder report) {
        VillagesConfig config = VillagesConfig.getInstance();
        VillagesConfig.LLMSettings llmSettings = config.getLLMSettings(); // Correct type to inner class
        String activeProvider = config.getAIProvider(); // Get the selected provider string
        String apiKey = llmSettings.getApiKey();
        String endpoint = llmSettings.getEndpoint();
        String model = llmSettings.getModel();
        boolean isLocal = llmSettings.isLocalModel();
        String localPath = llmSettings.getLocalModelPath();

        report.append(String.format("Active AI Provider: %s\n", activeProvider));

        if (isLocal) {
            report.append(String.format("[ X ] Local Model: Enabled\n"));
            report.append(String.format("      - Path: %s\n", localPath.isEmpty() ? "(Not Set)" : localPath));
            report.append(String.format("      - Model File Status: %s\n", ModelChecker.hasLocalModel() ? "Found" : "Missing!"));
        } else {
            report.append(String.format("[ X ] Remote Provider (%s): Enabled\n", activeProvider));
            report.append(String.format("      - API Key: %s\n", apiKey.isEmpty() ? "Missing!" : "Set"));
            report.append(String.format("      - Endpoint: %s\n", endpoint.isEmpty() ? "(Default/Not Set)" : endpoint));
            report.append(String.format("      - Model: %s\n", model.isEmpty() ? "(Default/Not Set)" : model));

            // Specific checks based on provider string
            if ("AZURE".equalsIgnoreCase(activeProvider)) {
                if (apiKey.isEmpty() || endpoint.isEmpty()) {
                    report.append("      - Status: Config Missing! (Requires API Key and Endpoint)\n");
                } else {
                     report.append("      - Status: Configured\n");
                }
            } else { // OpenAI, OpenRouter, Cohere, Anthropic, Deepseek, Gemini, Mistral etc.
                 if (apiKey.isEmpty()) {
                    report.append("      - Status: API Key Missing!\n");
                 } else {
                     report.append("      - Status: Configured\n");
                 }
            }
        }
        // Note: LLMService instance check might be useful too, but not directly related to config errors.
        LLMService service = LLMService.getInstance();
        report.append(String.format("[ %s ] LLMService: %s\n", service != null ? "X" : "!", service != null ? "Instance OK" : "Instance is null!"));
    }

    private static void testCulturalManagers(StringBuilder report) {
        // Texture Manager Check
        try {
            // Call new public static methods instead of accessing private map
            int cultureCount = CulturalTextureManager.getRegisteredCultureCount();
            int textureCount = CulturalTextureManager.getTotalTextureCount();
            report.append(String.format("[ X ] CulturalTextureManager: Loaded (%d cultures, %d textures)\n", cultureCount, textureCount));
        } catch (Exception e) { // Catch potential errors during static method calls
             report.append(String.format("[ E ] CulturalTextureManager: Error during check - %s\n", e.getMessage()));
        }
        // Removed duplicate/erroneous catch block here

        // Sound Manager Check
        try {
            CulturalSoundManager soundManager = CulturalSoundManager.getInstance();
            if (soundManager != null) {
                 // Call new public methods instead of accessing private maps
                 try {
                     int cultureCount = soundManager.getRegisteredCultureCount();
                     long totalSoundCount = soundManager.getTotalSoundCount();
                     report.append(String.format("[ X ] CulturalSoundManager: Loaded (%d cultures, %d total sounds)\n", cultureCount, totalSoundCount));
                 } catch (Exception e) { // Catch potential errors if methods aren't added yet
                     report.append("[ E ] CulturalSoundManager: Error accessing counts - " + e.getMessage() + "\n");
                 }
            } else {
                report.append("[   ] CulturalSoundManager: Instance is null!\n");
            }
        } catch (Exception e) {
            report.append(String.format("[ E ] CulturalSoundManager: Error during check - %s\n", e.getMessage()));
        }
    }

    private static void testVillagerSystems(StringBuilder report) {
        try {
            VillagerManager manager = VillagerManager.getInstance();
            if (manager != null) {
                int activeVillagers = manager.getActiveVillagers().size();
                int spawnRegions = manager.getSpawnRegions().size();
                report.append(String.format("[ X ] VillagerManager: Loaded (Tracking %d active villagers, %d spawn regions)\n", activeVillagers, spawnRegions));

                // Check MoodManager instance (often tied to VillagerManager or global)
                // Assuming MoodManager might be accessible or part of VillagerManager logic
                // If MoodManager is a separate singleton:
                // MoodManager moodManager = MoodManager.getInstance();
                // report.append(String.format("[ %s ] MoodManager: %s\n", moodManager != null ? "X" : " ", moodManager != null ? "Instance OK" : "Instance is null!"));

                // Relationship checks are complex globally. Reporting active villagers is a good proxy.
                report.append("[ i ] Villager Subsystems (Memory, Dialogue, Mood, Relationships): Status inferred from active villager count.\n");
                report.append("      - Detailed checks require specific villager context.\n");

            } else {
                report.append("[   ] VillagerManager: Instance is null!\n");
            }
        } catch (Exception e) {
             report.append(String.format("[ E ] VillagerManager: Error during check - %s\n", e.getMessage()));
        }
    }

    private static void testModelChecks(StringBuilder report) {
        // Get model path directly from ModelChecker
        Path modelPathObj = ModelChecker.getModelPath();
        String modelPath = modelPathObj.toString();

        // Model Downloader Check
        try {
            // ModelDownloader might not be a singleton, depends on usage.
            // If it's used transiently, check config/path instead.
            report.append(String.format("[ i ] Model Path: Configured to '%s'\n", modelPath));
            // Check if the specific model file exists
             if (ModelChecker.hasLocalModel()) {
                 report.append("      - Status: Model file exists.\n");
             } else {
                 report.append("      - Status: Model file does NOT exist!\n");
             }
             // TODO: Add check if ModelDownloader instance is accessible/relevant globally
        } catch (Exception e) {
             report.append(String.format("[ E ] ModelDownloader: Error during check - %s\n", e.getMessage()));
        }

        // Model Checker Check
        try {
            // ModelChecker might also be transient or part of initialization.
            // Check if models specified in config actually exist at the path.
            // Use static ModelChecker methods
            boolean modelAvailable = ModelChecker.hasLocalModel();
            report.append(String.format("[ %s ] ModelChecker: Local model file %s at '%s'\n",
                 modelAvailable ? "X" : "!",
                 modelAvailable ? "Available" : "Missing/Unavailable",
                 modelPath));
        } catch (Exception e) {
             report.append(String.format("[ E ] ModelChecker: Error during check - %s\n", e.getMessage()));
        }
    }

}