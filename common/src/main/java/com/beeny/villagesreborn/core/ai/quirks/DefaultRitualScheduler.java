package com.beeny.villagesreborn.core.ai.quirks;

import com.beeny.villagesreborn.core.common.VillagerEntity;
import com.beeny.villagesreborn.core.config.AIConfig;
import java.util.PriorityQueue;
import java.util.Comparator;
import java.util.function.Supplier;
import java.util.List;
import com.beeny.villagesreborn.core.ai.quirks.QuirkDefinition;
import com.beeny.villagesreborn.core.ai.quirks.QuirkRegistry;

public class DefaultRitualScheduler implements RitualScheduler {
    private final PriorityQueue<ScheduledTask> taskQueue = new PriorityQueue<>(
        Comparator.comparingLong(ScheduledTask::scheduledTick)
    );
    private final Supplier<Long> currentTickSupplier;

    public DefaultRitualScheduler(Supplier<Long> currentTickSupplier) {
        this.currentTickSupplier = currentTickSupplier;
    }

    @Override
    public void scheduleQuirkCheck(VillagerEntity villager) {
        long currentTick = currentTickSupplier.get();
        long interval = AIConfig.getInstance().getQuirkCheckInterval();
        long scheduledTick = currentTick + interval;
        taskQueue.offer(new ScheduledTask(villager, scheduledTick));
    }

    @Override
    public void performScheduledChecks() {
        long currentTick = currentTickSupplier.get();
        while (!taskQueue.isEmpty() && taskQueue.peek().scheduledTick() <= currentTick) {
            ScheduledTask task = taskQueue.poll();
            VillagerEntity villager = task.villager();
            
            if (villager != null) {
                List<QuirkDefinition> quirks = QuirkRegistry.getApplicableQuirks(villager);
                for (QuirkDefinition quirk : quirks) {
                    quirk.apply(villager);
                }
            }
        }
    }

    private record ScheduledTask(VillagerEntity villager, long scheduledTick) {}
}