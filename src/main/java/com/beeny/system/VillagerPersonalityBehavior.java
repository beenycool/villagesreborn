package com.beeny.system;

import com.beeny.Villagersreborn;
import com.beeny.data.VillagerData;
import com.beeny.constants.VillagerConstants.PersonalityType;
import com.beeny.util.BoundingBoxUtils;
import com.beeny.util.VillagerUtils;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class VillagerPersonalityBehavior {
    
    public static void applyPersonalityEffects(VillagerEntity villager, VillagerData data) {
        if (villager.getWorld().isClient || data == null) return;
        
        PersonalityType personality = data.getPersonality();
        ServerWorld world = (ServerWorld) villager.getWorld();
        
        switch (personality) {
            case FRIENDLY -> applyFriendlyBehavior(villager, data, world);
            case SHY -> applyShyBehavior(villager, data, world);
            case GRUMPY -> applyGrumpyBehavior(villager, data, world);
            case CHEERFUL -> applyCheerfulBehavior(villager, data, world);
            case MELANCHOLY -> applyMelancholyBehavior(villager, data, world);
            case ENERGETIC -> applyEnergeticBehavior(villager, data, world);
            case CALM -> applyCalmBehavior(villager, data, world);
            case CONFIDENT -> applyConfidentBehavior(villager, data, world);
            case ANXIOUS, NERVOUS -> applyAnxiousBehavior(villager, data, world);
            case CURIOUS -> applyCuriousBehavior(villager, data, world);
            case LAZY -> applyLazyBehavior(villager, data, world);
            case SERIOUS -> applySeriousBehavior(villager, data, world);
        }
    }
    
    private static void applyFriendlyBehavior(VillagerEntity villager, VillagerData data, ServerWorld world) {
        // Friendly villagers seek out social interactions more often
        if (VillagerUtils.shouldPerformRandomAction(0.05f)) {
            List<VillagerEntity> nearbyVillagers = VillagerUtils.getNearbyVillagers(villager, 12);
            
            if (!nearbyVillagers.isEmpty()) {
                VillagerEntity target = VillagerUtils.getRandomElement(nearbyVillagers.toArray(new VillagerEntity[0]));
                VillagerData targetData = VillagerUtils.getVillagerData(target);
                
                if (targetData != null) {
                    data.adjustHappiness(2);
                    targetData.adjustHappiness(1);
                    
                    VillagerUtils.spawnHeartParticles(world, villager.getPos(), target.getPos(), 3);
                }
            }
        }
    }
    
    private static void applyShyBehavior(VillagerEntity villager, VillagerData data, ServerWorld world) {
        List<PlayerEntity> nearbyPlayers = VillagerUtils.getNearbyPlayers(villager, 8);
        
        if (nearbyPlayers.size() > 2) {
            data.adjustHappiness(-1);
        } else if (nearbyPlayers.size() == 1) {
            data.adjustHappiness(1);
        }
    }
    
    private static void applyGrumpyBehavior(VillagerEntity villager, VillagerData data, ServerWorld world) {
        List<VillagerEntity> nearbyVillagers = VillagerUtils.getNearbyVillagers(villager, 6);
        
        if (nearbyVillagers.size() > 2) {
            data.adjustHappiness(-1);
            
            if (VillagerUtils.shouldPerformRandomAction(0.02f)) {
                VillagerUtils.playVillagerSound(world, villager.getBlockPos(), 
                    SoundEvents.ENTITY_VILLAGER_NO, 0.8f, 0.8f);
            }
        }
        
        if (data.getHappiness() > 60 && VillagerUtils.shouldPerformRandomAction(0.1f)) {
            data.adjustHappiness(-1);
        }
        
        if (nearbyVillagers.isEmpty()) {
            data.adjustHappiness(1);
        }
    }
    
    private static void applyCheerfulBehavior(VillagerEntity villager, VillagerData data, ServerWorld world) {
        if (ThreadLocalRandom.current().nextFloat() < 0.08f) {
            List<VillagerEntity> nearbyVillagers = world.getEntitiesByClass(
                VillagerEntity.class,
                BoundingBoxUtils.fromBlocks(villager.getBlockPos(), villager.getBlockPos()).expand(10),
                v -> v != villager && v.isAlive()
            );
            
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
    
    private static void applyMelancholyBehavior(VillagerEntity villager, VillagerData data, ServerWorld world) {
        if (world.isRaining()) {
            data.adjustHappiness(2);
        }
        
        List<VillagerEntity> nearbyVillagers = world.getEntitiesByClass(
            VillagerEntity.class,
            BoundingBoxUtils.fromBlocks(villager.getBlockPos(), villager.getBlockPos()).expand(8),
            v -> v != villager && v.isAlive()
        );
        
        if (nearbyVillagers.isEmpty()) {
            data.adjustHappiness(1);
        } else if (nearbyVillagers.size() > 3) {
            data.adjustHappiness(-1);
        }
        
        long timeOfDay = world.getTimeOfDay() % 24000;
        if (timeOfDay >= 13000 && timeOfDay <= 23000) {
            data.adjustHappiness(1);
        }
    }
    
    private static void applyEnergeticBehavior(VillagerEntity villager, VillagerData data, ServerWorld world) {
        if (ThreadLocalRandom.current().nextFloat() < 0.1f) {
            world.spawnParticles(ParticleTypes.CRIT,
                villager.getX(), villager.getY() + 0.5, villager.getZ(),
                3, 0.5, 0.5, 0.5, 0.3);
            
            data.adjustHappiness(1);
        }
        
        List<VillagerEntity> nearbyVillagers = world.getEntitiesByClass(
            VillagerEntity.class,
            BoundingBoxUtils.fromBlocks(villager.getBlockPos(), villager.getBlockPos()).expand(6),
            v -> v != villager && v.isAlive()
        );
        
        if (nearbyVillagers.isEmpty()) {
            data.adjustHappiness(-1);
        }
        
        long timeOfDay = world.getTimeOfDay() % 24000;
        if (timeOfDay >= 1000 && timeOfDay <= 11000) {
            data.adjustHappiness(1);
        }
    }
    
    private static void applyCalmBehavior(VillagerEntity villager, VillagerData data, ServerWorld world) {
        if (ThreadLocalRandom.current().nextFloat() < 0.04f) {
            List<VillagerEntity> nearbyVillagers = world.getEntitiesByClass(
                VillagerEntity.class,
                BoundingBoxUtils.fromBlocks(villager.getBlockPos(), villager.getBlockPos()).expand(8),
                v -> v != villager && v.isAlive()
            );
            
            for (VillagerEntity nearby : nearbyVillagers) {
                VillagerData nearbyData = nearby.getAttached(Villagersreborn.VILLAGER_DATA);
                if (nearbyData != null) {
                    nearbyData.adjustHappiness(1);
                }
            }
        }
    }
    
    private static void applyConfidentBehavior(VillagerEntity villager, VillagerData data, ServerWorld world) {
        if (data.getHappiness() < 60 && ThreadLocalRandom.current().nextFloat() < 0.03f) {
            data.adjustHappiness(1);
        }
        
        if (ThreadLocalRandom.current().nextFloat() < 0.05f) {
            List<VillagerEntity> nearbyVillagers = world.getEntitiesByClass(
                VillagerEntity.class,
                BoundingBoxUtils.fromBlocks(villager.getBlockPos(), villager.getBlockPos()).expand(8),
                v -> v != villager && v.isAlive()
            );
            
            for (VillagerEntity nearby : nearbyVillagers) {
                VillagerData nearbyData = nearby.getAttached(Villagersreborn.VILLAGER_DATA);
                if (nearbyData != null && nearbyData.getPersonality() == PersonalityType.SHY) {
                    nearbyData.adjustHappiness(2);
                }
            }
        }
    }
    
    private static void applyAnxiousBehavior(VillagerEntity villager, VillagerData data, ServerWorld world) {
        if (world.isThundering()) {
            data.adjustHappiness(-3);
        }
        
        List<VillagerEntity> nearbyVillagers = world.getEntitiesByClass(
            VillagerEntity.class,
            BoundingBoxUtils.fromBlocks(villager.getBlockPos(), villager.getBlockPos()).expand(8),
            v -> v != villager && v.isAlive()
        );
        
        if (nearbyVillagers.size() > 4) {
            data.adjustHappiness(-2);
        } else if (nearbyVillagers.size() == 1 || nearbyVillagers.size() == 2) {
            data.adjustHappiness(1);
        }
    }
    
    private static void applyCuriousBehavior(VillagerEntity villager, VillagerData data, ServerWorld world) {
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
        
        List<PlayerEntity> nearbyPlayers = world.getEntitiesByClass(
            PlayerEntity.class,
            BoundingBoxUtils.fromBlocks(villager.getBlockPos(), villager.getBlockPos()).expand(10),
            player -> player.isAlive()
        );
        
        if (!nearbyPlayers.isEmpty()) {
            data.adjustHappiness(1);
        }
    }
    
    private static void applyLazyBehavior(VillagerEntity villager, VillagerData data, ServerWorld world) {
        if (data.getHappiness() > 40 && ThreadLocalRandom.current().nextFloat() < 0.08f) {
            data.adjustHappiness(-1);
        }
        
        long timeOfDay = world.getTimeOfDay() % 24000;
        if (timeOfDay >= 12000 && timeOfDay <= 14000) {
            data.adjustHappiness(-1);
        }
        
        if (timeOfDay >= 2000 && timeOfDay <= 4000) {
            data.adjustHappiness(1);
        }
        
        List<VillagerEntity> nearbyVillagers = world.getEntitiesByClass(
            VillagerEntity.class,
            BoundingBoxUtils.fromBlocks(villager.getBlockPos(), villager.getBlockPos()).expand(5),
            v -> v != villager && v.isAlive()
        );
        
        if (nearbyVillagers.isEmpty()) {
            data.adjustHappiness(1);
        }
    }
    
    private static void applySeriousBehavior(VillagerEntity villager, VillagerData data, ServerWorld world) {
        long timeOfDay = world.getTimeOfDay() % 24000;
        if (timeOfDay >= 6000 && timeOfDay <= 12000) {
            data.adjustHappiness(1);
        }
        
        List<VillagerEntity> nearbyVillagers = world.getEntitiesByClass(
            VillagerEntity.class,
            BoundingBoxUtils.fromBlocks(villager.getBlockPos(), villager.getBlockPos()).expand(8),
            v -> v != villager && v.isAlive()
        );
        
        for (VillagerEntity nearby : nearbyVillagers) {
            VillagerData nearbyData = nearby.getAttached(Villagersreborn.VILLAGER_DATA);
            if (nearbyData != null && nearbyData.getPersonality() == PersonalityType.CHEERFUL) {
                if (ThreadLocalRandom.current().nextFloat() < 0.02f) {
                    data.adjustHappiness(-1);
                }
            }
        }
        
        long seriousNeighbors = nearbyVillagers.stream()
            .map(v -> v.getAttached(Villagersreborn.VILLAGER_DATA))
            .filter(d -> d != null && d.getPersonality() == PersonalityType.SERIOUS)
            .count();
        
        if (seriousNeighbors > 0) {
            data.adjustHappiness(1);
        }
    }
    
    public static float getPersonalityHappinessModifier(PersonalityType personality) {
        return switch (personality) {
            case CHEERFUL -> 1.2f;
            case CONFIDENT -> 1.1f;
            case FRIENDLY -> 1.05f;
            case ENERGETIC, CALM, CURIOUS -> 1.0f;
            case SHY, NERVOUS -> 0.95f;
            case ANXIOUS, LAZY -> 0.9f;
            case MELANCHOLY -> 0.85f;
            case GRUMPY -> 0.8f;
            case SERIOUS -> 0.9f;
        };
    }
    
    public static int getPersonalityTradeBonus(PersonalityType personality, int playerReputation) {
        return switch (personality) {
            case FRIENDLY -> playerReputation > 30 ? 2 : 1;
            case CONFIDENT, CHEERFUL -> 1;
            case GRUMPY -> playerReputation > 50 ? 1 : -1;
            case SHY, NERVOUS -> playerReputation > 40 ? 1 : 0;
            case ANXIOUS -> playerReputation > 60 ? 1 : -1;
            case SERIOUS -> playerReputation > 35 ? 1 : 0;
            default -> 0;
        };
    }
    
    public static String getPersonalitySpecificGreeting(PersonalityType personality, String villagerName, int reputation) {
        return switch (personality) {
            case FRIENDLY -> reputation > 20 ? 
                "Hello there, friend! Wonderful to see you again!" :
                "Hello! Nice to meet you!";
            case SHY, NERVOUS -> reputation > 40 ?
                "Oh... h-hello... I'm glad you came back..." :
                "Um... h-hi there...";
            case GRUMPY -> reputation > 60 ?
                "Oh, it's you. I suppose you're not too bad." :
                "What do you want now?";
            case CHEERFUL -> "What a beautiful day! Hello there!";
            case CONFIDENT -> "Greetings! I hope you're ready for some excellent trades!";
            case ENERGETIC -> "Hey there! I'm having such a great day!";
            case MELANCHOLY -> "Hello... *sighs softly*";
            case CURIOUS -> "Oh, hello! I was just wondering about something...";
            case ANXIOUS -> "Oh! You startled me... hello...";
            case CALM -> "Good day to you. How may I help?";
            case LAZY -> "Oh... hello there... *yawns*";
            case SERIOUS -> "Good day. How may I assist you?";
        };
    }
}