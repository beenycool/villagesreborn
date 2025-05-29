package com.beeny.villagesreborn.core.ai.quirks;

import com.beeny.villagesreborn.core.common.VillagerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuirkExecutor {
    private static final Logger LOGGER = LoggerFactory.getLogger(QuirkExecutor.class);

    public static void execute(VillagerEntity villager, QuirkDefinition quirk) {
        if (villager == null || quirk == null) return;
        
        try {
            LOGGER.info("Executing quirk '{}' for villager {}", quirk.getName(), villager.getName());
            quirk.apply(villager);
        } catch (Exception e) {
            LOGGER.error("Error executing quirk '{}' for villager {}: {}", 
                         quirk.getName(), villager.getName(), e.getMessage());
        }
    }
}