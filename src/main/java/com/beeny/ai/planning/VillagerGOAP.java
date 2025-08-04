package com.beeny.ai.planning;

import com.beeny.data.VillagerData;
import com.beeny.ai.core.VillagerEmotionSystem;
import com.beeny.ai.social.VillagerGossipNetwork;
import com.beeny.system.VillagerScheduleManager;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Goal-Oriented Action Planning (GOAP) system for intelligent villager behavior
 * Villagers dynamically plan actions to achieve goals based on their needs, personality, and context
 */
public class VillagerGOAP {
    
    // Core GOAP components
    public interface WorldState {
        boolean getBool(String key);
        int getInt(String key);
        float getFloat(String key);
        String getString(String key);
        void setBool(String key, boolean value);
        void setInt(String key, int value);
        void setFloat(String key, float value);
        void setString(String key, String value);
        boolean hasKey(String key);
        Set<String> getKeys();
        WorldState copy();
    }
    
    public static class SimpleWorldState implements WorldState {
        private final Map<String, Object> state = new ConcurrentHashMap<>();
        
        @Override
        public boolean getBool(String key) {
            Object value = state.get(key);
            return value instanceof Boolean ? (Boolean) value : false;
        }
        
        @Override
        public int getInt(String key) {
            Object value = state.get(key);
            return value instanceof Integer ? (Integer) value : 0;
        }
        
        @Override
        public float getFloat(String key) {
            Object value = state.get(key);
            return value instanceof Float ? (Float) value : 0.0f;
        }
        
        @Override
        public String getString(String key) {
            Object value = state.get(key);
            return value instanceof String ? (String) value : "";
        }
        
        @Override
        public void setBool(String key, boolean value) { state.put(key, value); }
        
        @Override
        public void setInt(String key, int value) { state.put(key, value); }
        
        @Override
        public void setFloat(String key, float value) { state.put(key, value); }
        
        @Override
        public void setString(String key, String value) { state.put(key, value); }
        
        @Override
        public boolean hasKey(String key) { return state.containsKey(key); }
        
        @Override
        public Set<String> getKeys() { return new HashSet<>(state.keySet()); }
        
        @Override
        public WorldState copy() {
            SimpleWorldState copy = new SimpleWorldState();
            copy.state.putAll(this.state);
            return copy;
        }
        
        public boolean satisfies(WorldState goal) {
            Set<String> goalKeys = goal.getKeys();
            if (goalKeys.isEmpty()) return false; // Defensive: empty goal should not be satisfied
        
            for (String key : goalKeys) {
                if (!hasKey(key)) return false;
        
                // Get the actual stored objects to determine type and compare directly
                Object goalValue = ((SimpleWorldState) goal).state.get(key);
                Object currentValue = this.state.get(key);
                
                // Compare objects directly - this handles all types correctly
                if (!Objects.equals(goalValue, currentValue)) {
                    return false;
                }
            }
            return true;
        }
    }
    
    public abstract static class Goal {
        public final String name;
        public final float priority;
        public final WorldState desiredState;
        protected final VillagerEntity villager;
        
        public Goal(String name, float priority, VillagerEntity villager) {
            this.name = name;
            this.priority = priority;
            this.villager = villager;
            this.desiredState = new SimpleWorldState();
            defineGoalState();
        }
        
        protected abstract void defineGoalState();
        public abstract boolean isValid(WorldState currentState);
        public abstract float calculateDynamicPriority(WorldState currentState);
        
        public float getFinalPriority(WorldState currentState) {
            if (!isValid(currentState)) return 0.0f;
            return priority * calculateDynamicPriority(currentState);
        }
    }
    
    public abstract static class Action {
        public final String name;
        public final float baseCost;
        protected final VillagerEntity villager;
        public Action(String name, float baseCost, VillagerEntity villager) {
            this.name = name;
            this.baseCost = baseCost;
            this.villager = villager;
        }
        
        public abstract boolean canPerform(WorldState state);
        public abstract WorldState getEffects(WorldState state);
        public abstract float getCost(WorldState state);
        public abstract boolean execute(WorldState state);
        public abstract WorldState getPreconditions();
    }
    
    // Specific Goals
    public static class IncreaseHappinessGoal extends Goal {
        public IncreaseHappinessGoal(VillagerEntity villager) {
            super("increase_happiness", 0.7f, villager);
        }
        
        @Override
        protected void defineGoalState() {
            desiredState.setBool("is_happy", true);
        }
        
        @Override
        public boolean isValid(WorldState currentState) {
            return currentState.getFloat("happiness") < 70.0f;
        }
        
        @Override
        public float calculateDynamicPriority(WorldState currentState) {
            float happiness = currentState.getFloat("happiness");
            return Math.max(0.1f, (100.0f - happiness) / 100.0f);
        }
    }
    
    public static class SocializeGoal extends Goal {
        public SocializeGoal(VillagerEntity villager) {
            super("socialize", 0.5f, villager);
        }
        
        @Override
        protected void defineGoalState() {
            desiredState.setBool("has_socialized", true);
        }
        
        @Override
        public boolean isValid(WorldState currentState) {
            return currentState.getFloat("loneliness") > 20.0f || !currentState.getBool("has_socialized_today");
        }
        
        @Override
        public float calculateDynamicPriority(WorldState currentState) {
            float loneliness = currentState.getFloat("loneliness");
            float personality_modifier = switch (getPersonality()) {
                case "Friendly", "Cheerful" -> 1.5f;
                case "Shy" -> 0.6f;
                case "Grumpy" -> 0.7f;
                default -> 1.0f;
            };
            return Math.max(0.1f, (loneliness / 50.0f)) * personality_modifier;
        }
        
        private String getPersonality() {
            VillagerData data = villager.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
            return data != null ? data.getPersonality() : "Friendly";
        }
    }
    
    public static class WorkGoal extends Goal {
        public WorkGoal(VillagerEntity villager) {
            super("work", 0.6f, villager);
        }
        
        @Override
        protected void defineGoalState() {
            desiredState.setBool("has_worked", true);
        }
        
        @Override
        public boolean isValid(WorldState currentState) {
            VillagerScheduleManager.TimeOfDay timeOfDay = VillagerScheduleManager.TimeOfDay.fromWorldTime(villager.getWorld().getTimeOfDay());
            return (timeOfDay == VillagerScheduleManager.TimeOfDay.MORNING || 
                   timeOfDay == VillagerScheduleManager.TimeOfDay.AFTERNOON) &&
                   !currentState.getBool("has_worked_today");
        }
        
        @Override
        public float calculateDynamicPriority(WorldState currentState) {
            String personality = getPersonality();
            float modifier = switch (personality) {
                case "Energetic", "Serious" -> 1.3f;
                case "Lazy" -> 0.5f;
                default -> 1.0f;
            };
            return modifier;
        }
        
        private String getPersonality() {
            VillagerData data = villager.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
            return data != null ? data.getPersonality() : "Friendly";
        }
    }
    
    public static class FindLoveGoal extends Goal {
        public FindLoveGoal(VillagerEntity villager) {
            super("find_love", 0.3f, villager);
        }
        
        @Override
        protected void defineGoalState() {
            desiredState.setBool("has_spouse", true);
        }
        
        @Override
        public boolean isValid(WorldState currentState) {
            return !currentState.getBool("has_spouse") && 
                   currentState.getInt("age") > 100 &&
                   currentState.getFloat("happiness") > 40.0f;
        }
        
        @Override
        public float calculateDynamicPriority(WorldState currentState) {
            float loneliness = currentState.getFloat("loneliness");
            float love = currentState.getFloat("love");
            int age = currentState.getInt("age");
            
            float ageFactor = Math.max(0.1f, Math.min(1.0f, (age - 100) / 200.0f));
            float emotionalFactor = (loneliness + love) / 100.0f;
            
            return ageFactor * emotionalFactor;
        }
    }
    
    public static class LearnGossipGoal extends Goal {
        public LearnGossipGoal(VillagerEntity villager) {
            super("learn_gossip", 0.4f, villager);
        }
        
        @Override
        protected void defineGoalState() {
            desiredState.setBool("knows_latest_gossip", true);
        }
        
        @Override
        public boolean isValid(WorldState currentState) {
            return currentState.getFloat("curiosity") > 30.0f;
        }
        
        @Override
        public float calculateDynamicPriority(WorldState currentState) {
            String personality = getPersonality();
            float modifier = switch (personality) {
                case "Curious" -> 2.0f;
                case "Friendly", "Cheerful" -> 1.3f;
                case "Shy" -> 0.5f;
                default -> 1.0f;
            };
            return (currentState.getFloat("curiosity") / 100.0f) * modifier;
        }
        
        private String getPersonality() {
            VillagerData data = villager.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
            return data != null ? data.getPersonality() : "Friendly";
        }
    }
    
    // Specific Actions
    public static class SocializeAction extends Action {
        public SocializeAction(VillagerEntity villager) {
            super("socialize", 5.0f, villager);
        }
        
        @Override
        public boolean canPerform(WorldState state) {
            return state.getBool("villagers_nearby");
        }
        
        @Override
        public WorldState getEffects(WorldState state) {
            WorldState effects = new SimpleWorldState();
            effects.setBool("has_socialized", true);
            effects.setBool("has_socialized_today", true);
            return effects;
        }
        
        @Override
        public float getCost(WorldState state) {
            String personality = getPersonality();
            float modifier = switch (personality) {
                case "Shy" -> 2.0f;
                case "Friendly", "Cheerful" -> 0.5f;
                default -> 1.0f;
            };
            return baseCost * modifier;
        }
        
        @Override
        public boolean execute(WorldState state) {
            // Find nearby villagers and socialize
            List<VillagerEntity> nearby = villager.getWorld().getEntitiesByClass(
                VillagerEntity.class,
                villager.getBoundingBox().expand(10.0),
                v -> v != villager
            );
            
            if (!nearby.isEmpty()) {
                VillagerEntity target = nearby.get(villager.getWorld().getRandom().nextInt(nearby.size()));
                
                // Emotional effects of socializing
                ServerVillagerManager.getInstance().getAIWorldManager().getEmotionManager().processEmotionalEvent(villager,
                    new VillagerEmotionSystem.EmotionalEvent(VillagerEmotionSystem.EmotionType.HAPPINESS, 10.0f, "socializing", false));
                
                ServerVillagerManager.getInstance().getAIWorldManager().getEmotionManager().processEmotionalEvent(villager,
                    new VillagerEmotionSystem.EmotionalEvent(VillagerEmotionSystem.EmotionType.LONELINESS, -15.0f, "socializing", false));
                
                state.setBool("has_socialized", true);
                state.setBool("has_socialized_today", true);
                return true;
            }
            return false;
        }
        
        @Override
        public WorldState getPreconditions() {
            WorldState preconditions = new SimpleWorldState();
            preconditions.setBool("villagers_nearby", true);
            return preconditions;
        }
        
        private String getPersonality() {
            VillagerData data = villager.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
            return data != null ? data.getPersonality() : "Friendly";
        }
    }
    
    public static class WorkAction extends Action {
        public WorkAction(VillagerEntity villager) {
            super("work", 8.0f, villager);
        }
        
        @Override
        public boolean canPerform(WorldState state) {
            VillagerScheduleManager.TimeOfDay timeOfDay = VillagerScheduleManager.TimeOfDay.fromWorldTime(villager.getWorld().getTimeOfDay());
            return timeOfDay == VillagerScheduleManager.TimeOfDay.MORNING || 
                   timeOfDay == VillagerScheduleManager.TimeOfDay.AFTERNOON;
        }
        
        @Override
        public WorldState getEffects(WorldState state) {
            WorldState effects = new SimpleWorldState();
            effects.setBool("has_worked", true);
            effects.setBool("has_worked_today", true);
            return effects;
        }
        
        @Override
        public float getCost(WorldState state) {
            String personality = getPersonality();
            float modifier = switch (personality) {
                case "Lazy" -> 3.0f;
                case "Energetic", "Serious" -> 0.7f;
                default -> 1.0f;
            };
            return baseCost * modifier;
        }
        
        @Override
        public boolean execute(WorldState state) {
            // Simulate work and its effects
            VillagerEmotionSystem.processEmotionalEvent(villager,
                new VillagerEmotionSystem.EmotionalEvent(VillagerEmotionSystem.EmotionType.CONTENTMENT, 8.0f, "working", false));
            
            // Small chance of boredom from repetitive work
            if (villager.getWorld().getRandom().nextFloat() < 0.2f) {
                VillagerEmotionSystem.processEmotionalEvent(villager,
                    VillagerEmotionSystem.EmotionalEvent.REPETITIVE_TASK);
            }
            
            state.setBool("has_worked", true);
            state.setBool("has_worked_today", true);
            return true;
        }
        
        @Override
        public WorldState getPreconditions() {
            return new SimpleWorldState(); // No specific preconditions
        }
        
        private String getPersonality() {
            VillagerData data = villager.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
            return data != null ? data.getPersonality() : "Friendly";
        }
    }
    
    public static class GossipAction extends Action {
        public GossipAction(VillagerEntity villager) {
            super("gossip", 3.0f, villager);
        }
        
        @Override
        public boolean canPerform(WorldState state) {
            return state.getBool("villagers_nearby");
        }
        
        @Override
        public WorldState getEffects(WorldState state) {
            WorldState effects = new SimpleWorldState();
            effects.setBool("knows_latest_gossip", true);
            effects.setBool("has_socialized", true);
            return effects;
        }
        
        @Override
        public float getCost(WorldState state) {
            String personality = getPersonality();
            float modifier = switch (personality) {
                case "Shy" -> 2.5f;
                case "Curious", "Friendly" -> 0.5f;
                default -> 1.0f;
            };
            return baseCost * modifier;
        }
        
        @Override
        public boolean execute(WorldState state) {
            // Find recent gossip and "learn" it
            List<VillagerGossipNetwork.GossipPiece> availableGossip = new ArrayList<>();
            // In a real implementation, this would query the gossip network
            
            VillagerEmotionSystem.processEmotionalEvent(villager,
                new VillagerEmotionSystem.EmotionalEvent(VillagerEmotionSystem.EmotionType.CURIOSITY, 10.0f, "learning_gossip", false));
            
            state.setBool("knows_latest_gossip", true);
            state.setBool("has_socialized", true);
            return true;
        }
        
        @Override
        public WorldState getPreconditions() {
            WorldState preconditions = new SimpleWorldState();
            preconditions.setBool("villagers_nearby", true);
            return preconditions;
        }
        
        private String getPersonality() {
            VillagerData data = villager.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
            return data != null ? data.getPersonality() : "Friendly";
        }
    }
    
    // GOAP Planner
    public static class GOAPPlanner {
        public static List<Action> plan(WorldState currentState, Goal goal, List<Action> availableActions) {
            // A* search for action sequence
            PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparing(n -> n.fCost));
            Set<String> closedSet = new HashSet<>();
            
            Node startNode = new Node(currentState, null, null, 0);
            openSet.add(startNode);

            // Safeguard: iteration and time limits
            final int MAX_ITERATIONS = 10000;
            final long MAX_TIME_MS = 100; // 100 milliseconds
            int iterations = 0;
            long startTime = System.currentTimeMillis();
            
            while (!openSet.isEmpty()) {
                // Check iteration and time limits
                iterations++;
                if (iterations > MAX_ITERATIONS || (System.currentTimeMillis() - startTime) > MAX_TIME_MS) {
                    // Exceeded safe limits, abort search
                    return new ArrayList<>();
                }

                Node current = openSet.poll();
                
                String stateKey = generateStateKey(current.state);
                if (closedSet.contains(stateKey)) continue;
                closedSet.add(stateKey);
                
                // Check if goal is satisfied
                if (((SimpleWorldState) current.state).satisfies(goal.desiredState)) {
                    return reconstructPath(current);
                }
                
                // Explore possible actions
                for (Action action : availableActions) {
                    if (action.canPerform(current.state)) {
                        WorldState newState = applyAction(current.state, action);
                        float newCost = current.gCost + action.getCost(current.state);
                        float heuristic = calculateHeuristic(newState, goal.desiredState);
                        
                        Node neighbor = new Node(newState, current, action, newCost);
                        neighbor.fCost = newCost + heuristic;
                        
                        String neighborKey = generateStateKey(newState);
                        if (!closedSet.contains(neighborKey)) {
                            openSet.add(neighbor);
                        }
                    }
                }
            }
            
            return new ArrayList<>(); // No plan found
        }
        
        private static WorldState applyAction(WorldState currentState, Action action) {
            WorldState newState = currentState.copy();
            WorldState effects = action.getEffects(currentState);
            
            for (String key : effects.getKeys()) {
                if (effects.hasKey(key)) {
                    Object value = ((SimpleWorldState) effects).state.get(key);
                    ((SimpleWorldState) newState).state.put(key, value);
                }
            }
            
            return newState;
        }
        
        private static float calculateHeuristic(WorldState current, WorldState goal) {
            int unsatisfiedGoals = 0;
            for (String key : goal.getKeys()) {
                if (!current.hasKey(key) || !Objects.equals(
                    ((SimpleWorldState) current).state.get(key),
                    ((SimpleWorldState) goal).state.get(key))) {
                    unsatisfiedGoals++;
                }
            }
            return unsatisfiedGoals;
        }
        
        private static String generateStateKey(WorldState state) {
            // Simple state key generation - could be optimized
            return state.getKeys().stream()
                .sorted()
                .map(key -> key + "=" + ((SimpleWorldState) state).state.get(key))
                .reduce("", (a, b) -> a + ";" + b);
        }
        
        private static List<Action> reconstructPath(Node endNode) {
            List<Action> path = new ArrayList<>();
            Node current = endNode;
            
            while (current.parent != null) {
                path.add(0, current.action);
                current = current.parent;
            }
            
            return path;
        }
    }
    
    private static class Node {
        WorldState state;
        Node parent;
        Action action;
        float gCost;
        float fCost;
        
        Node(WorldState state, Node parent, Action action, float gCost) {
            this.state = state;
            this.parent = parent;
            this.action = action;
            this.gCost = gCost;
        }
    }
    
    // Manager for villager GOAP systems
    public static class VillagerGOAPManager {
        private static final Map<String, List<Goal>> villagerGoals = new ConcurrentHashMap<>();
        private static final Map<String, List<Action>> villagerActions = new ConcurrentHashMap<>();
        private static final Map<String, List<Action>> currentPlans = new ConcurrentHashMap<>();
        private static final Map<String, Integer> planExecutionIndex = new ConcurrentHashMap<>();
        
        public static void initializeVillager(VillagerEntity villager) {
            String uuid = villager.getUuidAsString();
            
            // Initialize goals based on villager data and personality
            List<Goal> goals = new ArrayList<>();
            goals.add(new IncreaseHappinessGoal(villager));
            goals.add(new SocializeGoal(villager));
            goals.add(new WorkGoal(villager));
            goals.add(new FindLoveGoal(villager));
            goals.add(new LearnGossipGoal(villager));
            
            villagerGoals.put(uuid, goals);
            
            // Initialize available actions
            List<Action> actions = new ArrayList<>();
            actions.add(new SocializeAction(villager));
            actions.add(new WorkAction(villager));
            actions.add(new GossipAction(villager));
            
            villagerActions.put(uuid, actions);
        }
        
        public static void updateVillager(VillagerEntity villager) {
            String uuid = villager.getUuidAsString();
            WorldState currentState = buildCurrentWorldState(villager);
            
            // Get current plan or create new one
            List<Action> currentPlan = currentPlans.get(uuid);
            int executionIndex = planExecutionIndex.getOrDefault(uuid, 0);
            
            boolean needNewPlan = false;
            
            if (currentPlan == null || executionIndex >= currentPlan.size()) {
                needNewPlan = true;
            } else {
                // Check if current action can still be performed
                Action currentAction = currentPlan.get(executionIndex);
                if (!currentAction.canPerform(currentState)) {
                    needNewPlan = true;
                }
            }
            
            if (needNewPlan) {
                Goal bestGoal = selectBestGoal(villager, currentState);
                if (bestGoal != null) {
                    List<Action> newPlan = GOAPPlanner.plan(currentState, bestGoal, villagerActions.get(uuid));
                    currentPlans.put(uuid, newPlan);
                    planExecutionIndex.put(uuid, 0);
                    executionIndex = 0;
                    currentPlan = newPlan;
                }
            }
            
            // Execute current action
            if (currentPlan != null && executionIndex < currentPlan.size()) {
                Action currentAction = currentPlan.get(executionIndex);
                if (currentAction.execute(currentState)) {
                    planExecutionIndex.put(uuid, executionIndex + 1);
                }
            }
        }
        
        private static Goal selectBestGoal(VillagerEntity villager, WorldState currentState) {
            String uuid = villager.getUuidAsString();
            List<Goal> goals = villagerGoals.get(uuid);
            if (goals == null) return null;
            
            return goals.stream()
                .filter(goal -> goal.isValid(currentState))
                .max(Comparator.comparing(goal -> goal.getFinalPriority(currentState)))
                .orElse(null);
        }
        
        private static WorldState buildCurrentWorldState(VillagerEntity villager) {
            SimpleWorldState state = new SimpleWorldState();
            VillagerData data = villager.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
            VillagerEmotionSystem.EmotionalState emotions = VillagerEmotionSystem.getEmotionalState(villager);
            
            if (data != null) {
                state.setInt("age", data.getAge());
                state.setFloat("happiness", data.getHappiness());
                state.setBool("has_spouse", !data.getSpouseId().isEmpty());
                state.setString("personality", data.getPersonality());
                
                // Emotional state
                state.setFloat("loneliness", emotions.getEmotion(VillagerEmotionSystem.EmotionType.LONELINESS));
                state.setFloat("curiosity", emotions.getEmotion(VillagerEmotionSystem.EmotionType.CURIOSITY));
                state.setFloat("love", emotions.getEmotion(VillagerEmotionSystem.EmotionType.LOVE));
                state.setFloat("boredom", emotions.getEmotion(VillagerEmotionSystem.EmotionType.BOREDOM));
            }
            
            // Environmental state
            List<VillagerEntity> nearbyVillagers = villager.getWorld().getEntitiesByClass(
                VillagerEntity.class,
                villager.getBoundingBox().expand(10.0),
                v -> v != villager
            );
            state.setBool("villagers_nearby", !nearbyVillagers.isEmpty());
            
            // Time-based state
            VillagerScheduleManager.TimeOfDay timeOfDay = VillagerScheduleManager.TimeOfDay.fromWorldTime(villager.getWorld().getTimeOfDay());
            state.setString("time_of_day", timeOfDay.name());
            
            // Daily activity tracking with time-based reset
            long worldTime = villager.getWorld().getTimeOfDay();
            int currentDay = (int) (worldTime / 24000);

            // Use villager's lastConversationTime as a lightweight persisted "last day touched"
            // If day changed since last touch, reset daily flags to false; otherwise infer from logs
            long lastTouch = (data != null) ? data.getLastConversationTime() : 0L;
            int lastDay = (int) (lastTouch / 24000);
            boolean newDay = currentDay != lastDay;

            boolean hasWorkedToday = false, hasSocializedToday = false;

            if (!newDay) {
                // Infer from DailyActivityTracker if available for current day
                com.beeny.system.DailyActivityTracker.DailyLog todayLog =
                    com.beeny.system.DailyActivityTracker.getDailyLog(villager.getUuidAsString(), currentDay);
                if (todayLog != null) {
                    java.util.Map<String, Long> durations = todayLog.getActivityDurations();
                    long workTime = durations.getOrDefault("Working", 0L);
                    long socializeTime = durations.getOrDefault("Socializing", 0L);
                    // Some places might label socialize activity differently; be defensive
                    if (socializeTime == 0L) {
                        socializeTime = durations.getOrDefault("Socialize", 0L);
                    }
                    hasWorkedToday = workTime > 0;
                    hasSocializedToday = socializeTime > 0;
                }
            } else {
                // Day changed: reset daily flags
                hasWorkedToday = false;
                hasSocializedToday = false;
                // Update last touch to the start of current day to avoid repeat resets within the day
                if (data != null) {
                    // Store as a time within current day so (time/24000) == currentDay
                    data.setLastConversationTime(currentDay * 24000L);
                }
            }

            state.setBool("has_worked_today", hasWorkedToday);
            state.setBool("has_socialized_today", hasSocializedToday);
            
            return state;
        }
        
        public static void cleanup(String villagerUuid) {
            villagerGoals.remove(villagerUuid);
            villagerActions.remove(villagerUuid);
            currentPlans.remove(villagerUuid);
            planExecutionIndex.remove(villagerUuid);
        }
    }
}