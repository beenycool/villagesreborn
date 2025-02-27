package com.beeny.village;

import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

/**
 * Utility class for handling visual effects and feedback for villager interactions
 */
public class VillagerFeedbackHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn");
    private static final Random RANDOM = new Random();
    
    /**
     * Show thinking particles beside a villager's head
     */
    public static void showThinkingEffect(VillagerEntity villager) {
        if (villager != null && villager.getWorld() instanceof ServerWorld serverWorld) {
            // Get a position slightly to the side of the villager's head
            double offsetX = (RANDOM.nextBoolean() ? 1 : -1) * 0.5;
            double headY = villager.getY() + villager.getStandingEyeHeight();
            
            // Spawn thought bubble particles beside the villager's head
            for (int i = 0; i < 3; i++) {
                // Randomize slightly for a more natural look
                double particleX = villager.getX() + offsetX + RANDOM.nextDouble() * 0.2 - 0.1;
                double particleY = headY + 0.2 + (0.1 * i) + RANDOM.nextDouble() * 0.1;
                double particleZ = villager.getZ() + RANDOM.nextDouble() * 0.2 - 0.1;
                
                serverWorld.spawnParticles(
                    ParticleTypes.CLOUD, 
                    particleX,
                    particleY,
                    particleZ,
                    1, // Count per spawn
                    0.03, 0.03, 0.03, // Offset
                    0.01 // Speed
                );
            }
        }
    }
    
    /**
     * Show speech bubble particles when a villager is talking
     */
    public static void showSpeakingEffect(VillagerEntity villager) {
        if (villager != null && villager.getWorld() instanceof ServerWorld serverWorld) {
            // Get a position beside the villager's mouth
            double offsetX = (RANDOM.nextBoolean() ? 1 : -1) * 0.4;
            double mouthY = villager.getY() + villager.getStandingEyeHeight() - 0.1;
            
            // Spawn note particles to indicate speaking
            serverWorld.spawnParticles(
                ParticleTypes.NOTE, 
                villager.getX() + offsetX, 
                mouthY,
                villager.getZ(), 
                1, // Count
                0.1, 0.1, 0.1, // Offset
                RANDOM.nextDouble() // Random pitch effect
            );
        }
    }
    
    /**
     * Makes the villager look at the player and gesture
     * @param villager The villager entity
     */
    public static void showTalkingAnimation(VillagerEntity villager) {
        if (villager != null) {
            // Make the villager wave its arms (using the work animation)
            villager.setHeadRollingTimeLeft(20);
            
            if (villager.getWorld() instanceof ServerWorld serverWorld) {
                // Occasionally show emotive particles based on sentiment
                if (Math.random() < 0.3) {
                    // Determine particle type based on a random factor (could be extended to use sentiment analysis)
                    double sentimentRoll = Math.random();
                    if (sentimentRoll > 0.7) {
                        // Happy response
                        serverWorld.spawnParticles(ParticleTypes.HAPPY_VILLAGER, 
                            villager.getX(), villager.getY() + 1.8, villager.getZ(), 
                            3, 0.3, 0.1, 0.3, 0.01);
                    } else if (sentimentRoll < 0.2) {
                        // Surprised or confused
                        serverWorld.spawnParticles(ParticleTypes.ENCHANT, 
                            villager.getX(), villager.getY() + 1.8, villager.getZ(), 
                            3, 0.3, 0.1, 0.3, 0.1);
                    }
                }
            }
        }
    }
    
    /**
     * Show a message to the player about AI model status
     * @param isConfigured Whether the AI is properly configured
     * @return A formatted text message
     */
    public static Text getAIStatusMessage(boolean isConfigured) {
        if (isConfigured) {
            return Text.literal("§aVillager AI is active and working properly.§r");
        } else {
            return Text.literal("§cVillager AI is limited. No AI model or API key is configured.§r\n" +
                               "§7Use §f/vr config§7 to set up the AI system.§r");
        }
    }
    
    /**
     * Create different speech patterns for different types of villagers
     * @param villagerProfession The profession of the villager
     * @param message The original message
     * @return The formatted message with speech patterns
     */
    public static String formatSpeech(String profession, String message) {
        // This could be expanded with more complex formatting based on profession
        if (message == null) return "";
        
        if ("LIBRARIAN".equalsIgnoreCase(profession)) {
            return "\"" + message + "\" *adjusts glasses*";
        } else if ("FARMER".equalsIgnoreCase(profession)) {
            return message + " *nods slowly*";
        } else if ("CLERIC".equalsIgnoreCase(profession)) {
            return "*mumbles* " + message;
        } else if ("WEAPONSMITH".equalsIgnoreCase(profession) || 
                  "ARMORER".equalsIgnoreCase(profession)) {
            return message + "! *grunts*";
        }
        return message;
    }
}