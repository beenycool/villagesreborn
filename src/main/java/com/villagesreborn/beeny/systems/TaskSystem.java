package com.villagesreborn.beeny.systems;

import com.villagesreborn.beeny.ai.VillagerProfession;
import java.util.concurrent.*;
import java.util.UUID;

public class TaskSystem {
    private final ScheduledExecutorService taskScheduler = Executors.newScheduledThreadPool(4);
    private final ConcurrentHashMap<String, VillageTask> activeTasks = new ConcurrentHashMap<>();

    public record VillageTask(
        String taskId,
        VillagerProfession profession,
        String description,
        long deadline,
        Runnable onSuccess,
        Runnable onFailure
    ) {}

    public void createTask(VillagerProfession profession, String description, 
                          long durationMinutes, Runnable success, Runnable failure) {
        VillageTask task = new VillageTask(
            UUID.randomUUID().toString(),
            profession,
            description,
            System.currentTimeMillis() + durationMinutes * 60 * 1000,
            success,
            failure
        );
        
        activeTasks.put(task.taskId(), task);
        scheduleTaskExpiry(task);
    }

    private void scheduleTaskExpiry(VillageTask task) {
        long delay = task.deadline() - System.currentTimeMillis();
        taskScheduler.schedule(() -> {
            if (activeTasks.containsKey(task.taskId())) {
                task.onFailure().run();
                activeTasks.remove(task.taskId());
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    public void completeTask(String taskId) {
        VillageTask task = activeTasks.remove(taskId);
        if (task != null) {
            task.onSuccess().run();
            GovernanceSystem governance = ServiceLocator.getService(GovernanceSystem.class);
            if (governance != null) {
                governance.handleTaskCompletion(task);
            }
        }
    }

    public void failTask(String taskId) {
        VillageTask task = activeTasks.remove(taskId);
        if (task != null) {
            task.onFailure().run();
            GovernanceSystem governance = ServiceLocator.getService(GovernanceSystem.class);
            if (governance != null) {
                governance.handleTaskFailure(task);
            }
        }
    }
}
