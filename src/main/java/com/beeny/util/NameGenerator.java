package com.beeny.util;

import com.beeny.ai.LLMService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class NameGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn");
    private static final int NAME_TIMEOUT_SECONDS = 5;

    public CompletableFuture<String> generateName(String culture, String profession) {
        String prompt = String.format(
            "Generate a name for a %s villager who works as a %s.\n" +
            "The name should reflect their cultural background and profession.\n" +
            "Include a brief title or epithet that reflects their role.\n" +
            "Format: [Name] the [Title]\n" +
            "Keep it under 5 words total.",
            culture, profession
        );

        return LLMService.getInstance()
            .generateResponse(prompt)
            .orTimeout(NAME_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .exceptionally(e -> {
                LOGGER.error("Error generating name for {} {}", culture, profession, e);
                return profession + " Villager"; // Fallback name
            });
    }
}