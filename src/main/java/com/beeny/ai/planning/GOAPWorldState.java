package com.beeny.ai.planning;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * World state representation for GOAP planning.
 * Handles state management and comparison operations.
 */
public interface GOAPWorldState {
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
    GOAPWorldState copy();
    
    /**
     * Simple implementation of world state with thread-safe operations.
     */
    class Simple implements GOAPWorldState {
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
        public void setBool(String key, boolean value) { 
            state.put(key, value); 
        }
        
        @Override
        public void setInt(String key, int value) { 
            state.put(key, value); 
        }
        
        @Override
        public void setFloat(String key, float value) { 
            state.put(key, value); 
        }
        
        @Override
        public void setString(String key, String value) { 
            state.put(key, value); 
        }
        
        @Override
        public boolean hasKey(String key) { 
            return state.containsKey(key); 
        }
        
        @Override
        public Set<String> getKeys() { 
            return new HashSet<>(state.keySet()); 
        }
        
        @Override
        public GOAPWorldState copy() {
            Simple copy = new Simple();
            copy.state.putAll(this.state);
            return copy;
        }
        
        /**
         * Check if this state satisfies the given goal state.
         */
        public boolean satisfies(GOAPWorldState goal) {
            Set<String> goalKeys = goal.getKeys();
            if (goalKeys.isEmpty()) return false;
        
            for (String key : goalKeys) {
                if (!hasKey(key)) return false;
        
                Object goalValue = ((Simple) goal).state.get(key);
                Object currentValue = this.state.get(key);
                
                if (!Objects.equals(goalValue, currentValue)) {
                    return false;
                }
            }
            return true;
        }
        
        /**
         * Generate a unique key for this state for caching/comparison.
         */
        public String generateStateKey() {
            return state.keySet().stream()
                .sorted()
                .map(key -> key + "=" + state.get(key))
                .reduce("", (a, b) -> a + ";" + b);
        }
        
        /**
         * Apply effects from another state to this state.
         */
        public void applyEffects(GOAPWorldState effects) {
            for (String key : effects.getKeys()) {
                if (effects.hasKey(key)) {
                    Object value = ((Simple) effects).state.get(key);
                    this.state.put(key, value);
                }
            }
        }
    }
}