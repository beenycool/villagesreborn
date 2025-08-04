package com.beeny.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

public class AIState {
    public static final Codec<AIState> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.STRING.fieldOf("currentGoal").orElse("IDLE").forGetter(AIState::getCurrentGoal),
            Codec.STRING.fieldOf("currentAction").orElse("NONE").forGetter(AIState::getCurrentAction),
            Codec.list(Codec.STRING).fieldOf("goalQueue").orElse(Collections.emptyList()).forGetter(AIState::getGoalQueue),
            Codec.INT.fieldOf("priority").orElse(0).forGetter(AIState::getPriority),
            Codec.BOOL.fieldOf("isActive").orElse(true).forGetter(AIState::isActive),
            Codec.LONG.fieldOf("lastDecisionTime").orElse(0L).forGetter(AIState::getLastDecisionTime)
        ).apply(instance, AIState::new)
    );

    private String currentGoal;
    private String currentAction;
    private final List<String> goalQueue;
    private int priority;
    private boolean isActive;
    private long lastDecisionTime;

    public AIState(String currentGoal, String currentAction, List<String> goalQueue, int priority, boolean isActive, long lastDecisionTime) {
        this.currentGoal = currentGoal;
        this.currentAction = currentAction;
        this.goalQueue = new ArrayList<>(goalQueue);
        this.priority = priority;
        this.isActive = isActive;
        this.lastDecisionTime = lastDecisionTime;
    }

    public AIState() {
        this("IDLE", "NONE", new ArrayList<>(), 0, true, System.currentTimeMillis());
    }

    public String getCurrentGoal() {
        return currentGoal;
    }

    public String getCurrentAction() {
        return currentAction;
    }

    public List<String> getGoalQueue() {
        return Collections.unmodifiableList(goalQueue);
    }

    public int getPriority() {
        return priority;
    }

    public boolean isActive() {
        return isActive;
    }

    public long getLastDecisionTime() {
        return lastDecisionTime;
    }

    public void setCurrentGoal(String currentGoal) {
        this.currentGoal = currentGoal;
    }

    public void setCurrentAction(String currentAction) {
        this.currentAction = currentAction;
    }

    public void addGoal(String goal) {
        goalQueue.add(goal);
    }

    public void removeGoal(String goal) {
        goalQueue.remove(goal);
    }

    public void clearGoals() {
        goalQueue.clear();
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public void setLastDecisionTime(long lastDecisionTime) {
        this.lastDecisionTime = lastDecisionTime;
    }

    public AIState copy() {
        return new AIState(currentGoal, currentAction, new ArrayList<>(goalQueue), priority, isActive, lastDecisionTime);
    }
}