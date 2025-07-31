package com.beeny.system;

import com.beeny.Villagersreborn;
import com.beeny.data.VillagerData;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundCategory;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class VillagerRelationshipManager {
    private static final double MARRIAGE_RANGE = 10.0;
    private static final int MIN_MARRIAGE_AGE = 100; 
    private static final int MARRIAGE_COOLDOWN = 1000; 
    private static final int PROPOSAL_TIME_THRESHOLD = 12000; 
    
    
    private static final Map<String, Set<String>> PERSONALITY_COMPATIBILITIES = Map.of(
        "Friendly", Set.of("Cheerful", "Energetic", "Confident", "Curious"),
        "Grumpy", Set.of("Serious", "Lazy", "Grumpy"),
        "Shy", Set.of("Nervous", "Serious", "Shy"),
        "Energetic", Set.of("Cheerful", "Friendly", "Confident"),
        "Lazy", Set.of("Lazy", "Grumpy", "Serious"),
        "Curious", Set.of("Friendly", "Energetic", "Confident"),
        "Serious", Set.of("Grumpy", "Shy", "Serious"),
        "Cheerful", Set.of("Friendly", "Energetic", "Cheerful"),
        "Nervous", Set.of("Shy", "Nervous"),
        "Confident", Set.of("Friendly", "Energetic", "Curious")
    );
    
    // Track marriage proposals to avoid spam
    private static final Map<String, Long> lastProposalTime = new HashMap<>();
    
    
    public static boolean canMarry(VillagerEntity villager1, VillagerEntity villager2) {
        if (villager1 == null || villager2 == null) return false;
        if (villager1.equals(villager2)) return false;
        
        VillagerData data1 = villager1.getAttached(Villagersreborn.VILLAGER_DATA);
        VillagerData data2 = villager2.getAttached(Villagersreborn.VILLAGER_DATA);
        
        if (data1 == null || data2 == null) return false;
        
        
        if (data1.getAge() < MIN_MARRIAGE_AGE || data2.getAge() < MIN_MARRIAGE_AGE) return false;
        
        
        if (!data1.getSpouseId().isEmpty() || !data2.getSpouseId().isEmpty()) return false;
        
        
        if (data1.getHappiness() < 40 || data2.getHappiness() < 40) return false;
        
        
        if (areRelated(data1, data2, villager1.getUuidAsString(), villager2.getUuidAsString())) return false;
        
        
        double distance = villager1.getPos().distanceTo(villager2.getPos());
        if (distance > MARRIAGE_RANGE) return false;
        
        
        return arePersonalitiesCompatible(data1.getPersonality(), data2.getPersonality());
    }
    
    
    public static boolean attemptMarriage(VillagerEntity villager1, VillagerEntity villager2) {
        if (!canMarry(villager1, villager2)) return false;
        
        
        long currentTime = villager1.getWorld().getTime();
        String uuid1 = villager1.getUuidAsString();
        String uuid2 = villager2.getUuidAsString();
        
        if (lastProposalTime.getOrDefault(uuid1, 0L) + MARRIAGE_COOLDOWN > currentTime ||
            lastProposalTime.getOrDefault(uuid2, 0L) + MARRIAGE_COOLDOWN > currentTime) {
            return false;
        }
        
        
        lastProposalTime.put(uuid1, currentTime);
        lastProposalTime.put(uuid2, currentTime);
        
        
        VillagerData data1 = villager1.getAttached(Villagersreborn.VILLAGER_DATA);
        VillagerData data2 = villager2.getAttached(Villagersreborn.VILLAGER_DATA);
        
        float marriageChance = calculateMarriageChance(data1, data2);
        
        if (ThreadLocalRandom.current().nextFloat() < marriageChance) {
            
            performMarriage(villager1, villager2);
            return true;
        }
        
        return false;
    }
    
    
    private static void performMarriage(VillagerEntity villager1, VillagerEntity villager2) {
        VillagerData data1 = villager1.getAttached(Villagersreborn.VILLAGER_DATA);
        VillagerData data2 = villager2.getAttached(Villagersreborn.VILLAGER_DATA);
        
        
        data1.marry(data2.getName(), villager2.getUuidAsString());
        data2.marry(data1.getName(), villager1.getUuidAsString());
        
        
        data1.addFamilyMember(villager2.getUuidAsString());
        data2.addFamilyMember(villager1.getUuidAsString());
        
        
        if (villager1.getWorld() instanceof ServerWorld serverWorld) {
            
            serverWorld.spawnParticles(ParticleTypes.HEART,
                villager1.getX(), villager1.getY() + 2, villager1.getZ(),
                10, 0.5, 0.5, 0.5, 0.1);
            serverWorld.spawnParticles(ParticleTypes.HEART,
                villager2.getX(), villager2.getY() + 2, villager2.getZ(),
                10, 0.5, 0.5, 0.5, 0.1);
            
            
            serverWorld.playSound(null, villager1.getBlockPos(),
                SoundEvents.BLOCK_BELL_USE, SoundCategory.NEUTRAL, 1.0f, 1.0f);
            
            
            serverWorld.getPlayers().forEach(player -> {
                if (player.getPos().distanceTo(villager1.getPos()) < 50) {
                    player.sendMessage(Text.literal("💒 " + data1.getName() + " and " +
                        data2.getName() + " got married!").formatted(Formatting.LIGHT_PURPLE), false);
                }
            });
        }
        
        
        villager1.setCustomName(Text.literal(data1.getName() + " ❤").formatted(Formatting.RED));
        villager2.setCustomName(Text.literal(data2.getName() + " ❤").formatted(Formatting.RED));
    }
    
    
    public static void onVillagerBreed(VillagerEntity parent1, VillagerEntity parent2, VillagerEntity child) {
        VillagerData parentData1 = parent1.getAttached(Villagersreborn.VILLAGER_DATA);
        VillagerData parentData2 = parent2.getAttached(Villagersreborn.VILLAGER_DATA);
        VillagerData childData = child.getAttached(Villagersreborn.VILLAGER_DATA);
        
        if (parentData1 == null || parentData2 == null || childData == null) return;
        
        String childUuid = child.getUuidAsString();
        String parentUuid1 = parent1.getUuidAsString();
        String parentUuid2 = parent2.getUuidAsString();
        
        
        parentData1.addChild(childUuid);
        parentData2.addChild(childUuid);
        
        
        childData.addFamilyMember(parentUuid1);
        childData.addFamilyMember(parentUuid2);
        
        
        for (String memberUuid : parentData1.getFamilyMembers()) {
            if (!memberUuid.equals(childUuid)) {
                childData.addFamilyMember(memberUuid);
            }
        }
        for (String memberUuid : parentData2.getFamilyMembers()) {
            if (!memberUuid.equals(childUuid)) {
                childData.addFamilyMember(memberUuid);
            }
        }
        
        
        BlockPos pos = child.getBlockPos();
        childData.setBirthPlace(String.format("X:%d Y:%d Z:%d", pos.getX(), pos.getY(), pos.getZ()));
        
        
        inheritTraits(parentData1, parentData2, childData);
    }
    
    
    private static float calculateMarriageChance(VillagerData data1, VillagerData data2) {
        float baseChance = 0.1f;
        
        
        float happinessBonus = ((data1.getHappiness() + data2.getHappiness()) / 200.0f) * 0.3f;
        
        
        int ageDiff = Math.abs(data1.getAge() - data2.getAge());
        float ageBonus = ageDiff < 50 ? 0.1f : (ageDiff < 100 ? 0.05f : 0f);
        
        
        float personalityBonus = arePersonalitiesHighlyCompatible(data1.getPersonality(), data2.getPersonality()) ? 0.2f : 0f;
        
        
        float hobbyBonus = data1.getHobby().equals(data2.getHobby()) ? 0.1f : 0f;
        
        return Math.min(0.8f, baseChance + happinessBonus + ageBonus + personalityBonus + hobbyBonus);
    }
    
    
    private static boolean areRelated(VillagerData data1, VillagerData data2, String uuid1, String uuid2) {
        
        for (String memberUuid : data1.getFamilyMembers()) {
            if (data2.getFamilyMembers().contains(memberUuid)) {
                return true;
            }
        }
        
        
        if (data1.getChildrenIds().contains(uuid2) ||
            data2.getChildrenIds().contains(uuid1)) {
            return true;
        }
        
        
        Set<String> parents1 = new HashSet<>(data1.getFamilyMembers());
        Set<String> parents2 = new HashSet<>(data2.getFamilyMembers());
        parents1.retainAll(parents2);
        if (!parents1.isEmpty()) {
            return true;
        }
        
        return false;
    }
    
    
    private static boolean arePersonalitiesCompatible(String personality1, String personality2) {
        Set<String> compatible = PERSONALITY_COMPATIBILITIES.getOrDefault(personality1, Set.of());
        return compatible.contains(personality2) || personality1.equals(personality2);
    }
    
    private static boolean arePersonalitiesHighlyCompatible(String personality1, String personality2) {
        return (personality1.equals("Friendly") && personality2.equals("Cheerful")) ||
               (personality1.equals("Cheerful") && personality2.equals("Friendly")) ||
               (personality1.equals("Energetic") && personality2.equals("Confident")) ||
               (personality1.equals("Confident") && personality2.equals("Energetic"));
    }
    
    
    private static void inheritTraits(VillagerData parent1, VillagerData parent2, VillagerData child) {
        
        if (ThreadLocalRandom.current().nextBoolean()) {
            child.setPersonality(ThreadLocalRandom.current().nextBoolean() ? parent1.getPersonality() : parent2.getPersonality());
        }
        
        
        int avgHappiness = (parent1.getHappiness() + parent2.getHappiness()) / 2;
        int childHappiness = avgHappiness + ThreadLocalRandom.current().nextInt(21) - 10; 
        
        childHappiness = Math.max(0, Math.min(100, childHappiness));
        child.setHappiness(childHappiness);
        
        
        if (ThreadLocalRandom.current().nextFloat() < 0.3f) {
            child.setHobby(ThreadLocalRandom.current().nextBoolean() ? parent1.getHobby() : parent2.getHobby());
        }
        
        
        if (ThreadLocalRandom.current().nextFloat() < 0.4f && !parent1.getFavoriteFood().isEmpty()) {
            child.setFavoriteFood(ThreadLocalRandom.current().nextBoolean() ? parent1.getFavoriteFood() : parent2.getFavoriteFood());
        }
    }
    
    
    public static void divorce(VillagerEntity villager1, VillagerEntity villager2) {
        VillagerData data1 = villager1.getAttached(Villagersreborn.VILLAGER_DATA);
        VillagerData data2 = villager2.getAttached(Villagersreborn.VILLAGER_DATA);
        
        if (data1 == null || data2 == null) return;
        
        String villager1Uuid = villager1.getUuidAsString();
        String villager2Uuid = villager2.getUuidAsString();
        
        
        if (!data1.getSpouseId().equals(villager2Uuid) || !data2.getSpouseId().equals(villager1Uuid)) {
            return; 
        }
        
        
        data1.marry("", "");
        data2.marry("", "");
        
        
        data1.adjustHappiness(-30);
        data2.adjustHappiness(-30);
        
        
        villager1.setCustomName(Text.literal(data1.getName()).formatted(Formatting.WHITE));
        villager2.setCustomName(Text.literal(data2.getName()).formatted(Formatting.WHITE));
        
        
        if (villager1.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.getPlayers().forEach(player -> {
                if (player.getPos().distanceTo(villager1.getPos()) < 50) {
                    player.sendMessage(Text.literal("💔 " + data1.getName() + " and " +
                        data2.getName() + " got divorced!").formatted(Formatting.DARK_RED), false);
                }
            });
        }
    }
    
    
    public static List<Text> getFamilyTree(VillagerEntity villager) {
        VillagerData data = villager.getAttached(Villagersreborn.VILLAGER_DATA);
        if (data == null) return List.of(Text.literal("No family data available"));
        
        List<Text> tree = new ArrayList<>();
        tree.add(Text.literal("=== Family Tree ===").formatted(Formatting.GOLD));
        
        if (!data.getSpouseName().isEmpty()) {
            tree.add(Text.literal("Spouse: " + data.getSpouseName()).formatted(Formatting.RED));
        }
        
        if (!data.getChildrenNames().isEmpty()) {
            tree.add(Text.literal("Children:").formatted(Formatting.GREEN));
            data.getChildrenNames().forEach(child -> 
                tree.add(Text.literal("  - " + child).formatted(Formatting.GREEN)));
        }
        
        if (!data.getFamilyMembers().isEmpty()) {
            tree.add(Text.literal("Family Members:").formatted(Formatting.BLUE));
            data.getFamilyMembers().forEach(member -> 
                tree.add(Text.literal("  - " + member).formatted(Formatting.BLUE)));
        }
        
        if (!data.getBirthPlace().isEmpty()) {
            tree.add(Text.literal("Birth Place: " + data.getBirthPlace()).formatted(Formatting.YELLOW));
        }
        
        return tree;
    }
    
    
    public static List<VillagerEntity> findPotentialPartners(VillagerEntity villager) {
        if (villager.getWorld() == null) return List.of();
        
        return villager.getWorld().getEntitiesByClass(VillagerEntity.class, 
            villager.getBoundingBox().expand(MARRIAGE_RANGE), 
            potential -> canMarry(villager, potential));
    }
    
    
    public static String getRelationshipStatus(VillagerEntity villager1, VillagerEntity villager2) {
        VillagerData data1 = villager1.getAttached(Villagersreborn.VILLAGER_DATA);
        VillagerData data2 = villager2.getAttached(Villagersreborn.VILLAGER_DATA);
        
        if (data1 == null || data2 == null) return "Unknown";
        
        if (data1.getSpouseId().equals(villager2.getUuidAsString())) {
            return "Married";
        }
        
        if (areRelated(data1, data2, villager1.getUuidAsString(), villager2.getUuidAsString())) {
            return "Family";
        }
        
        return "Strangers";
    }
    
    
    public static int cleanupStaleProposalTimes() {
        long currentTime = System.currentTimeMillis();
        int initialSize = lastProposalTime.size();
        lastProposalTime.entrySet().removeIf(entry ->
            currentTime - entry.getValue() > PROPOSAL_TIME_THRESHOLD * 50
        );
        return initialSize - lastProposalTime.size();
    }
    
    /**
     * Remove proposal time entry for a specific villager
     * @param villagerUuid The UUID of the villager to remove
     */
    public static void removeProposalTime(String villagerUuid) {
        lastProposalTime.remove(villagerUuid);
    }
}