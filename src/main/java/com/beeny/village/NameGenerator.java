package com.beeny.village;

import com.beeny.ai.LLMService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NameGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn");

    public String generateName(String culture, String profession) {
        try {
            String prompt = String.format(
                "Generate a lore-friendly name for a Minecraft villager from %s culture who works as a %s. " +
                "Provide ONLY the name without any additional text or explanation.", 
                culture, profession
            );

            String response = LLMService.getInstance()
                .generateResponse(prompt)
                .get(10, TimeUnit.SECONDS)
                .trim();

            // Validate the name
            if (isValidName(response)) {
                return response;
            } else {
                LOGGER.warn("Generated name was invalid: {}", response);
                return getRandomDefaultName(culture);
            }
        } catch (Exception e) {
            LOGGER.error("Error generating villager name", e);
            return getRandomDefaultName(culture);
        }
    }

    private boolean isValidName(String name) {
        // Check if name is not empty and contains only letters and spaces
        return name != null && 
               !name.isEmpty() && 
               name.length() <= 32 &&
               name.matches("^[\\p{L}\\s'-]+$");
    }

    private String getRandomDefaultName(String culture) {
        return "Villager#" + (int)(Math.random() * 1000);
    }
}