package com.beeny.system;

import com.beeny.Villagersreborn;
import com.beeny.data.VillagerData;
import com.beeny.data.EmotionalState;
import com.beeny.constants.VillagerConstants.PersonalityType;
import com.beeny.util.VillagerUtils;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VillagerEmotionalBehavior {
    
    public static void updateEmotionalState(@Nullable VillagerEntity villager, @Nullable VillagerData data) {
        if (villager == null || villager.getWorld().isClient || data == null) return;
        
        ServerWorld world = (ServerWorld) villager.getWorld();
        
        // Simplified emotional effects based on circumstances
        // Focus on happiness changes rather than complex emotion tracking
        
        // Social environment effects
        List<VillagerEntity> nearbyVillagers = VillagerUtils.getNearbyVillagers(villager, 10);
        
        // Apply social and environmental happiness effects
        applyEnvironmentalEffects(villager, data, world, nearbyVillagers);
    }
    
    private static void applyEnvironmentalEffects(@NotNull VillagerEntity villager, @NotNull VillagerData data, 
                                                 @NotNull ServerWorld world, @NotNull List<VillagerEntity> nearbyVillagers) {
        
        // Happy villagers spread happiness
        if (data.getHappiness() > 80 && VillagerUtils.shouldPerformRandomAction(0.05f)) {
            spreadJoy(villager, world);
        }
        
        // Sad villagers need comfort
        if (data.getHappiness() < 30 && VillagerUtils.shouldPerformRandomAction(0.08f)) {
            seekComfort(villager, data, world);
        }
        
        // Lonely villagers seek social interaction
        if (nearbyVillagers.isEmpty() && VillagerUtils.shouldPerformRandomAction(0.1f)) {
            seekSocialInteraction(villager, data, world);
        }
        
        // Environmental effects
        if (world.isThundering()) {
            data.adjustHappiness(-2); // Thunderstorms are stressful
        } else if (world.isRaining() && data.getPersonality() == PersonalityType.MELANCHOLY) {
            data.adjustHappiness(1); // Melancholy villagers like rain
        }
        
        // Darkness effects
        if (world.getLightLevel(villager.getBlockPos()) < 8) {
            data.adjustHappiness(-1); // Fear of darkness
        }
    }
    
    private static void spreadJoy(@NotNull VillagerEntity villager, @NotNull ServerWorld world) {
        List<VillagerEntity> nearbyVillagers = VillagerUtils.getNearbyVillagers(villager, 8);
        
        // Create happy particles
        VillagerUtils.spawnHappinessParticles(world, villager.getPos(), 8);
        
        // Spread joy to nearby villagers
        for (VillagerEntity nearby : nearbyVillagers) {
            VillagerData nearbyData = VillagerUtils.getVillagerData(nearby);
            if (nearbyData != null) {
                nearbyData.adjustHappiness(2);
                nearbyData.getEmotionalState().adjustEmotion("joy", 0.1f);
                
                VillagerUtils.spawnParticles(world, nearby.getPos().add(0, 1, 0), 
                    ParticleTypes.HEART, 3, 0.5);
            }
        }
        
        // Notify nearby players
        List<PlayerEntity> nearbyPlayers = VillagerUtils.getNearbyPlayers(villager, 15);
        
        for (PlayerEntity player : nearbyPlayers) {
            VillagerData villagerData = VillagerUtils.getVillagerData(villager);
            if (villagerData != null) {
                player.sendMessage(Text.literal("✨ " + villagerData.getName() + 
                    " seems especially joyful today, spreading happiness to others!")
                    .formatted(Formatting.GOLD), true);
            }
        }
    }
    
    private static void seekComfort(@NotNull VillagerEntity villager, @NotNull VillagerData data, @NotNull ServerWorld world) {
        // Look for married partner among nearby villagers
        if (!data.getSpouseId().isEmpty()) {
            List<VillagerEntity> nearbyVillagers = VillagerUtils.getNearbyVillagers(villager, 20);
            
            for (VillagerEntity nearby : nearbyVillagers) {
                VillagerData nearbyData = VillagerUtils.getVillagerData(nearby);
                if (nearbyData != null && nearby.getUuidAsString().equals(data.getSpouseId())) {
                    // Found comfort with spouse
                    data.adjustHappiness(5);
                    
                    VillagerUtils.spawnParticles(world, villager.getPos().add(0, 1, 0), 
                        ParticleTypes.HEART, 5, 0.5);
                    return;
                }
            }
        }
        
        // Look for any friendly villager
        List<VillagerEntity> nearbyVillagers = VillagerUtils.getNearbyVillagers(villager, 10);
        
        if (!nearbyVillagers.isEmpty()) {
            VillagerEntity comforter = VillagerUtils.getRandomElement(nearbyVillagers.toArray(new VillagerEntity[0]));
            VillagerData comforterData = VillagerUtils.getVillagerData(comforter);
            
            if (comforterData != null && comforterData.getPersonality() == PersonalityType.FRIENDLY) {
                // Receive comfort
                data.adjustHappiness(3);
                comforterData.adjustHappiness(1);
                
                VillagerUtils.spawnParticles(world, 
                    villager.getPos().add(comforter.getPos()).multiply(0.5).add(0, 1, 0),
                    ParticleTypes.COMPOSTER, 5, 0.5);
            }
        }
    }
    
    // Removed avoidDangers method - integrated into applyEnvironmentalEffects
    
    private static void seekSocialInteraction(@NotNull VillagerEntity villager, @NotNull VillagerData data, @NotNull ServerWorld world) {
        List<VillagerEntity> nearbyVillagers = VillagerUtils.getNearbyVillagers(villager, 8);
        
        if (!nearbyVillagers.isEmpty()) {
            VillagerEntity target = VillagerUtils.getRandomElement(nearbyVillagers.toArray(new VillagerEntity[0]));
            VillagerData targetData = VillagerUtils.getVillagerData(target);
            
            if (targetData != null) {
                // Social interaction improves mood
                data.adjustHappiness(2);
                targetData.adjustHappiness(1);
                
                VillagerUtils.spawnParticles(world,
                    villager.getPos().add(target.getPos()).multiply(0.5).add(0, 1.5, 0),
                    ParticleTypes.BUBBLE_POP, 4, 0.5);
                
                // Add to recent events
                data.addRecentEvent("Had a nice chat with " + targetData.getName());
            }
        } else {
            // No one around, feel lonely
            data.adjustHappiness(-1);
        }
    }
    
    public static boolean shouldRefuseInteraction(@Nullable VillagerData data) {
        if (data == null) return false;
        
        // Very unhappy villagers might refuse interactions
        if (data.getHappiness() < 20) {
            return VillagerUtils.shouldPerformRandomAction(0.3f);
        }
        
        // Anxious personalities are more likely to refuse
        if (data.getPersonality() == PersonalityType.ANXIOUS || data.getPersonality() == PersonalityType.SHY) {
            return VillagerUtils.shouldPerformRandomAction(0.1f);
        }
        
        return false;
    }
    
    public static float getEmotionalTradeModifier(@Nullable VillagerData data) {
        if (data == null) return 0.0f;
        
        // Happiness-based trade modifier
        float happinessFactor = (data.getHappiness() - 50) / 50.0f; // -1 to 1
        
        // Personality affects trade willingness
        float personalityModifier = switch (data.getPersonality()) {
            case FRIENDLY, CHEERFUL -> 0.1f;
            case GRUMPY, ANXIOUS -> -0.1f;
            default -> 0.0f;
        };
        
        return (happinessFactor * 0.15f) + personalityModifier; // -0.25 to +0.25 modifier
    }
    
    @NotNull
    public static String getEmotionalGreeting(@Nullable VillagerData data, @Nullable String baseName, @Nullable String personality) {
        if (data == null || baseName == null) return "Someone says hello.";
        
        // Happiness-based greetings
        if (data.getHappiness() > 80) {
            return "✨ " + baseName + " beams with joy as they greet you!";
        } else if (data.getHappiness() < 30) {
            return "😔 " + baseName + " gives you a melancholy nod...";
        } else if (data.getPersonality() == PersonalityType.ANXIOUS) {
            return "😟 " + baseName + " fidgets while greeting you...";
        } else if (data.getPersonality() == PersonalityType.SHY) {
            return "😰 " + baseName + " nervously acknowledges your presence...";
        }
        
        return baseName + " says hello.";
    }
}