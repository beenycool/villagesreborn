package com.beeny.ai;

import com.beeny.ai.AIWorldManager;
import com.beeny.ai.VillagerAIManager;
import com.beeny.ai.AIWorldManager.VillagerAIState;
import net.minecraft.entity.passive.VillagerEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.UUID;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class VillagerAIIntegrationTest {

    private AIWorldManager worldManager;
    private VillagerAIManager aiManager;

    @BeforeEach
    public void setup() {
        worldManager = new AIWorldManager();
        aiManager = new VillagerAIManager();
    }

    @Test
    public void testVillagerAIStateInitialization() {
        VillagerAIState state = new VillagerAIState();
        assertTrue(state.isAIActive);
        assertEquals("", state.currentGoal);
        assertEquals("", state.currentAction);
        assertNotNull(state.contextData);
    }

    @Test
    public void testSetAndGetContextData() {
        VillagerAIState state = new VillagerAIState();
        state.setContext("mood", "happy");
        assertEquals("happy", state.getContext("mood"));
    }

    @Test
    public void testStateTransitions() {
        VillagerAIState state = new VillagerAIState();
        state.currentGoal = "FindFood";
        state.currentAction = "WalkToMarket";
        assertEquals("FindFood", state.currentGoal);
        assertEquals("WalkToMarket", state.currentAction);
    }

    @Test
    public void testInactiveAI() {
        VillagerAIState state = new VillagerAIState();
        state.isAIActive = false;
        assertFalse(state.isAIActive);
    }

    @Test
    public void testNullVillagerHandling() {
        // Simulate null villager scenario
        VillagerEntity villager = null;
        boolean result = aiManager.handleVillagerAI(villager);
        assertFalse(result, "AI manager should return false for null villager");
    }

    @Test
    public void testWorldManagerVillagerStateTracking() {
        String uuid = UUID.randomUUID().toString();
        VillagerAIState state = new VillagerAIState();
        worldManager.getVillagerStates().put(uuid, state);
        Map<String, VillagerAIState> states = worldManager.getVillagerStates();
        assertTrue(states.containsKey(uuid));
        assertEquals(state, states.get(uuid));
    }

    // Add more tests as new systems are refactored
}