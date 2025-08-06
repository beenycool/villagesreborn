package com.beeny.system.personality;

import com.beeny.Villagersreborn;
import com.beeny.constants.VillagerConstants.PersonalityType;
import com.beeny.data.VillagerData;
import com.beeny.util.BoundingBoxUtils;
import com.beeny.util.VillagerUtils;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Collection of default personality strategy implementations.
 * These classes are package-private and used by the factory.
 */
public class DefaultPersonalityStrategies {
    
    // Prevent instantiation
    private DefaultPersonalityStrategies() {}
}

class ShyPersonality implements PersonalityStrategy {
    @Override
    public void applyPersonalityEffects(VillagerEntity villager, VillagerData data, ServerWorld world) {
        List<PlayerEntity> nearbyPlayers = VillagerUtils.getNearbyPlayers(villager, 8);
        
        if (nearbyPlayers.size() > 2) {
            data.adjustHappiness(-1);
        } else if (nearbyPlayers.size() == 1) {
            data.adjustHappiness(1);
        }
    }
    
    @Override
    public float getHappinessModifier() { return 0.95f; }
    
    @Override
    public int getTradeBonus(int playerReputation) {
        return playerReputation > 40 ? 1 : 0;
    }
    
    @Override
    public String getGreeting(String villagerName, int reputation) {
        return reputation > 40 ? "Oh... h-hello... I'm glad you came back..." : "Um... h-hi there...";
    }
    
    @Override
    public String getPersonalityName() { return "SHY"; }
}

class CheerfulPersonality implements PersonalityStrategy {
    @Override
    public void applyPersonalityEffects(VillagerEntity villager, VillagerData data, ServerWorld world) {
        if (ThreadLocalRandom.current().nextFloat() < 0.08f) {
            List<VillagerEntity> nearbyVillagers = world.getEntitiesByClass(
                VillagerEntity.class,
                BoundingBoxUtils.fromBlocks(villager.getBlockPos(), villager.getBlockPos()).expand(10),
                v -> v != villager && v.isAlive()
            );
            
            // Spread happiness to nearby villagers
            for (VillagerEntity nearby : nearbyVillagers) {
                VillagerData nearbyData = nearby.getAttached(Villagersreborn.VILLAGER_DATA);
                if (nearbyData != null) {
                    nearbyData.adjustHappiness(1);
                }
            }
            
            world.spawnParticles(ParticleTypes.HAPPY_VILLAGER,
                villager.getX(), villager.getY() + 1, villager.getZ(),
                8, 1.0, 0.5, 1.0, 0.1);
            
            data.adjustHappiness(1);
        }
        
        if (data.getHappiness() < 70 && ThreadLocalRandom.current().nextFloat() < 0.05f) {
            data.adjustHappiness(1);
        }
    }
    
    @Override
    public float getHappinessModifier() { return 1.2f; }
    
    @Override
    public int getTradeBonus(int playerReputation) { return 1; }
    
    @Override
    public String getGreeting(String villagerName, int reputation) {
        return "What a beautiful day! Hello there!";
    }
    
    @Override
    public String getPersonalityName() { return "CHEERFUL"; }
}

class MelancholyPersonality implements PersonalityStrategy {
    @Override
    public void applyPersonalityEffects(VillagerEntity villager, VillagerData data, ServerWorld world) {
        if (world.isRaining()) {
            data.adjustHappiness(2);
        }
        
        List<VillagerEntity> nearbyVillagers = VillagerUtils.getNearbyVillagers(villager, 8);
        
        if (nearbyVillagers.isEmpty()) {
            data.adjustHappiness(1);
        } else if (nearbyVillagers.size() > 3) {
            data.adjustHappiness(-1);
        }
        
        // Prefer nighttime
        long timeOfDay = world.getTimeOfDay() % 24000;
        if (timeOfDay >= 13000 && timeOfDay <= 23000) {
            data.adjustHappiness(1);
        }
    }
    
    @Override
    public float getHappinessModifier() { return 0.85f; }
    
    @Override
    public int getTradeBonus(int playerReputation) { return 0; }
    
    @Override
    public String getGreeting(String villagerName, int reputation) {
        return "Hello... *sighs softly*";
    }
    
    @Override
    public String getPersonalityName() { return "MELANCHOLY"; }
}

class EnergeticPersonality implements PersonalityStrategy {
    @Override
    public void applyPersonalityEffects(VillagerEntity villager, VillagerData data, ServerWorld world) {
        if (ThreadLocalRandom.current().nextFloat() < 0.1f) {
            world.spawnParticles(ParticleTypes.CRIT,
                villager.getX(), villager.getY() + 0.5, villager.getZ(),
                3, 0.5, 0.5, 0.5, 0.3);
            
            data.adjustHappiness(1);
        }
        
        List<VillagerEntity> nearbyVillagers = VillagerUtils.getNearbyVillagers(villager, 6);
        
        if (nearbyVillagers.isEmpty()) {
            data.adjustHappiness(-1);
        }
        
        // Prefer daytime
        long timeOfDay = world.getTimeOfDay() % 24000;
        if (timeOfDay >= 1000 && timeOfDay <= 11000) {
            data.adjustHappiness(1);
        }
    }
    
    @Override
    public float getHappinessModifier() { return 1.0f; }
    
    @Override
    public int getTradeBonus(int playerReputation) { return 0; }
    
    @Override
    public String getGreeting(String villagerName, int reputation) {
        return "Hey there! I'm having such a great day!";
    }
    
    @Override
    public String getPersonalityName() { return "ENERGETIC"; }
}

class CalmPersonality implements PersonalityStrategy {
    @Override
    public void applyPersonalityEffects(VillagerEntity villager, VillagerData data, ServerWorld world) {
        if (ThreadLocalRandom.current().nextFloat() < 0.04f) {
            List<VillagerEntity> nearbyVillagers = VillagerUtils.getNearbyVillagers(villager, 8);
            
            // Calm villagers have a soothing effect on others
            for (VillagerEntity nearby : nearbyVillagers) {
                VillagerData nearbyData = nearby.getAttached(Villagersreborn.VILLAGER_DATA);
                if (nearbyData != null) {
                    nearbyData.adjustHappiness(1);
                }
            }
        }
    }
    
    @Override
    public float getHappinessModifier() { return 1.0f; }
    
    @Override
    public int getTradeBonus(int playerReputation) { return 0; }
    
    @Override
    public String getGreeting(String villagerName, int reputation) {
        return "Good day to you. How may I help?";
    }
    
    @Override
    public String getPersonalityName() { return "CALM"; }
}

class ConfidentPersonality implements PersonalityStrategy {
    @Override
    public void applyPersonalityEffects(VillagerEntity villager, VillagerData data, ServerWorld world) {
        if (data.getHappiness() < 60 && ThreadLocalRandom.current().nextFloat() < 0.03f) {
            data.adjustHappiness(1);
        }
        
        if (ThreadLocalRandom.current().nextFloat() < 0.05f) {
            List<VillagerEntity> nearbyVillagers = VillagerUtils.getNearbyVillagers(villager, 8);
            
            // Confident villagers boost shy villagers
            for (VillagerEntity nearby : nearbyVillagers) {
                VillagerData nearbyData = nearby.getAttached(Villagersreborn.VILLAGER_DATA);
                if (nearbyData != null && nearbyData.getPersonality() == PersonalityType.SHY) {
                    nearbyData.adjustHappiness(2);
                }
            }
        }
    }
    
    @Override
    public float getHappinessModifier() { return 1.1f; }
    
    @Override
    public int getTradeBonus(int playerReputation) { return 1; }
    
    @Override
    public String getGreeting(String villagerName, int reputation) {
        return "Greetings! I hope you're ready for some excellent trades!";
    }
    
    @Override
    public String getPersonalityName() { return "CONFIDENT"; }
}

class AnxiousPersonality implements PersonalityStrategy {
    @Override
    public void applyPersonalityEffects(VillagerEntity villager, VillagerData data, ServerWorld world) {
        if (world.isThundering()) {
            data.adjustHappiness(-3);
        }
        
        List<VillagerEntity> nearbyVillagers = VillagerUtils.getNearbyVillagers(villager, 8);
        
        if (nearbyVillagers.size() > 4) {
            data.adjustHappiness(-2);
        } else if (nearbyVillagers.size() == 1 || nearbyVillagers.size() == 2) {
            data.adjustHappiness(1);
        }
    }
    
    @Override
    public float getHappinessModifier() { return 0.9f; }
    
    @Override
    public int getTradeBonus(int playerReputation) {
        return playerReputation > 60 ? 1 : -1;
    }
    
    @Override
    public String getGreeting(String villagerName, int reputation) {
        return "Oh! You startled me... hello...";
    }
    
    @Override
    public String getPersonalityName() { return "ANXIOUS"; }
}

class CuriousPersonality implements PersonalityStrategy {
    @Override
    public void applyPersonalityEffects(VillagerEntity villager, VillagerData data, ServerWorld world) {
        if (ThreadLocalRandom.current().nextFloat() < 0.06f) {
            String[] observations = {
                "the way the light hits the ground",
                "an interesting cloud formation",
                "a peculiar sound in the distance", 
                "the texture of a nearby block",
                "the behavior of other villagers"
            };
            
            String observation = observations[ThreadLocalRandom.current().nextInt(observations.length)];
            data.addRecentEvent("Noticed " + observation);
            data.adjustHappiness(2);
            
            world.spawnParticles(ParticleTypes.END_ROD,
                villager.getX(), villager.getY() + 1.5, villager.getZ(),
                3, 0.8, 0.5, 0.8, 0.1);
        }
        
        List<PlayerEntity> nearbyPlayers = VillagerUtils.getNearbyPlayers(villager, 10);
        if (!nearbyPlayers.isEmpty()) {
            data.adjustHappiness(1);
        }
    }
    
    @Override
    public float getHappinessModifier() { return 1.0f; }
    
    @Override
    public int getTradeBonus(int playerReputation) { return 0; }
    
    @Override
    public String getGreeting(String villagerName, int reputation) {
        return "Oh, hello! I was just wondering about something...";
    }
    
    @Override
    public String getPersonalityName() { return "CURIOUS"; }
}

class LazyPersonality implements PersonalityStrategy {
    @Override
    public void applyPersonalityEffects(VillagerEntity villager, VillagerData data, ServerWorld world) {
        if (data.getHappiness() > 40 && ThreadLocalRandom.current().nextFloat() < 0.08f) {
            data.adjustHappiness(-1);
        }
        
        long timeOfDay = world.getTimeOfDay() % 24000;
        if (timeOfDay >= 12000 && timeOfDay <= 14000) { // Noon - don't like working
            data.adjustHappiness(-1);
        }
        
        if (timeOfDay >= 2000 && timeOfDay <= 4000) { // Late morning - prefer this time
            data.adjustHappiness(1);
        }
        
        List<VillagerEntity> nearbyVillagers = VillagerUtils.getNearbyVillagers(villager, 5);
        if (nearbyVillagers.isEmpty()) {
            data.adjustHappiness(1);
        }
    }
    
    @Override
    public float getHappinessModifier() { return 0.9f; }
    
    @Override
    public int getTradeBonus(int playerReputation) { return 0; }
    
    @Override
    public String getGreeting(String villagerName, int reputation) {
        return "Oh... hello there... *yawns*";
    }
    
    @Override
    public String getPersonalityName() { return "LAZY"; }
}

class SeriousPersonality implements PersonalityStrategy {
    @Override
    public void applyPersonalityEffects(VillagerEntity villager, VillagerData data, ServerWorld world) {
        long timeOfDay = world.getTimeOfDay() % 24000;
        if (timeOfDay >= 6000 && timeOfDay <= 12000) { // Working hours
            data.adjustHappiness(1);
        }
        
        List<VillagerEntity> nearbyVillagers = VillagerUtils.getNearbyVillagers(villager, 8);
        
        // Don't like cheerful villagers
        for (VillagerEntity nearby : nearbyVillagers) {
            VillagerData nearbyData = nearby.getAttached(Villagersreborn.VILLAGER_DATA);
            if (nearbyData != null && nearbyData.getPersonality() == PersonalityType.CHEERFUL) {
                if (ThreadLocalRandom.current().nextFloat() < 0.02f) {
                    data.adjustHappiness(-1);
                }
            }
        }
        
        // Like other serious villagers
        long seriousNeighbors = nearbyVillagers.stream()
            .map(v -> v.getAttached(Villagersreborn.VILLAGER_DATA))
            .filter(d -> d != null && d.getPersonality() == PersonalityType.SERIOUS)
            .count();
        
        if (seriousNeighbors > 0) {
            data.adjustHappiness(1);
        }
    }
    
    @Override
    public float getHappinessModifier() { return 0.9f; }
    
    @Override
    public int getTradeBonus(int playerReputation) {
        return playerReputation > 35 ? 1 : 0;
    }
    
    @Override
    public String getGreeting(String villagerName, int reputation) {
        return "Good day. How may I assist you?";
    }
    
    @Override
    public String getPersonalityName() { return "SERIOUS"; }
}