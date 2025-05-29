package com.beeny.villagesreborn.core.ai.quirks;

import com.beeny.villagesreborn.core.common.VillagerEntity;

public interface RitualScheduler {
    /**
     * Schedules a quirk check for the specified villager
     * @param villager The villager to schedule a quirk check for
     */
    void scheduleQuirkCheck(VillagerEntity villager);

    /**
     * Performs all scheduled quirk checks
     */
    void performScheduledChecks();
}