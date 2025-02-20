package com.beeny.village;

import net.minecraft.util.math.BlockPos;
import net.minecraft.server.world.ServerWorld;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MoodManager {
    private static final MoodManager INSTANCE = new MoodManager();
    private final Map<UUID, Integer> moodModifiers = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastDangerTime = new ConcurrentHashMap<>();
    
    private MoodManager() {}
    
    public static MoodManager getInstance() {
        return INSTANCE;
    }

    public void updateVillagerMood(VillagerAI villagerAI, ServerWorld world) {
        UUID villagerUUID = villagerAI.getVillager().getUuid();
        int currentHappiness = villagerAI.getHappiness();
        int moodModifier = calculateMoodModifier(villagerAI, world);
        
        moodModifiers.put(villagerUUID, moodModifier);
        villagerAI.setHappiness(currentHappiness + moodModifier);
    }

    private int calculateMoodModifier(VillagerAI villagerAI, ServerWorld world) {
        int modifier = 0;

        // Check village prosperity
        modifier += calculateProsperityModifier(villagerAI, world);

        // Check safety (recent dangers)
        modifier += calculateSafetyModifier(villagerAI);

        // Check social bonds
        modifier += calculateSocialModifier(villagerAI);

        return modifier;
    }

    private int calculateProsperityModifier(VillagerAI villagerAI, ServerWorld world) {
        int modifier = 0;
        BlockPos pos = villagerAI.getHomePos();
        SpawnRegion region = VillagerManager.getInstance().getNearestSpawnRegion(pos);

        if (region != null) {
            // More points of interest = higher prosperity
            int poiCount = region.getPointsOfInterest().size();
            modifier += Math.min(5, poiCount / 2);
        }

        return modifier;
    }

    private int calculateSafetyModifier(VillagerAI villagerAI) {
        int modifier = 0;
        UUID villagerUUID = villagerAI.getVillager().getUuid();
        
        // Check if there was recent danger
        Long lastDanger = lastDangerTime.get(villagerUUID);
        if (lastDanger != null) {
            long timeSinceDanger = System.currentTimeMillis() - lastDanger;
            if (timeSinceDanger < 300000) { // Last 5 minutes
                modifier -= 5; // Negative impact from recent danger
            } else {
                lastDangerTime.remove(villagerUUID); // Danger has passed
            }
        }

        return modifier;
    }

    private int calculateSocialModifier(VillagerAI villagerAI) {
        int modifier = 0;
        UUID villagerUUID = villagerAI.getVillager().getUuid();
        VillagerManager villagerManager = VillagerManager.getInstance();

        // Count relationships
        int relationshipCount = villagerManager.getRelationshipCount(villagerUUID);
        modifier += Math.min(5, relationshipCount);

        return modifier;
    }

    public void reportDanger(UUID villagerUUID) {
        lastDangerTime.put(villagerUUID, System.currentTimeMillis());
    }

    public int getMoodModifier(UUID villagerUUID) {
        return moodModifiers.getOrDefault(villagerUUID, 0);
    }

    public void clearMoodModifiers() {
        moodModifiers.clear();
        lastDangerTime.clear();
    }
}