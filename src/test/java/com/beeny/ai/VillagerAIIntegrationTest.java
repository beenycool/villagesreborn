package com.beeny.ai;

import com.beeny.ai.AIWorldManager;
import com.beeny.ai.VillagerAIManager;
import com.beeny.ai.AIWorldManager.VillagerAIState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Timeout;

import java.util.UUID;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class VillagerAIIntegrationTest {

    private AIWorldManager worldManager;

    @BeforeEach
    public void setup() {
        // Construct minimal AIWorldManager for tests that don't touch real MC server
        // If the real AIWorldManager requires a server, these tests focus purely on the state POJO behaviors.
        worldManager = new AIWorldManager();
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
    public void testWorldManagerVillagerStateTracking() {
        String uuid = UUID.randomUUID().toString();
        VillagerAIState state = new VillagerAIState();
        worldManager.getVillagerStates().put(uuid, state);
        Map<String, VillagerAIState> states = worldManager.getVillagerStates();
        assertTrue(states.containsKey(uuid));
        assertEquals(state, states.get(uuid));
    }

    // 1) Concurrent access to villager states by multiple threads
    @Test
    @Timeout(5)
    public void testConcurrentContextUpdates() throws InterruptedException {
        VillagerAIState state = new VillagerAIState();

        int threads = 8;
        int iterationsPerThread = 1000;
        ExecutorService exec = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);
        AtomicInteger totalWrites = new AtomicInteger(0);

        for (int t = 0; t < threads; t++) {
            final int threadId = t;
            exec.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < iterationsPerThread; i++) {
                        state.setContext("k_" + threadId + "_" + i, i);
                        totalWrites.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    fail("Thread interrupted");
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await();
        exec.shutdownNow();

        // Verify all writes are visible (ConcurrentHashMap ensures thread-safe updates)
        assertEquals(threads * iterationsPerThread, totalWrites.get());
        assertEquals(threads * iterationsPerThread, state.contextData.size());
        // spot check a few keys
        assertEquals(0, state.getContext("k_0_0"));
        assertEquals(iterationsPerThread - 1, state.getContext("k_0_" + (iterationsPerThread - 1)));
    }

    // 2) State "persistence" and restoration (simulate save/load by copying context)
    @Test
    public void testStatePersistenceAndRestoration() {
        VillagerAIState original = new VillagerAIState();
        original.isAIActive = true;
        original.currentGoal = "Gather";
        original.currentAction = "Pathfind";
        original.setContext("mood", "focused");
        original.setContext("energy", 75);

        // Simulate persistence by shallow-copying the state into a new instance
        VillagerAIState restored = new VillagerAIState();
        restored.isAIActive = original.isAIActive;
        restored.currentGoal = original.currentGoal;
        restored.currentAction = original.currentAction;
        original.contextData.forEach(restored::setContext);

        assertEquals(original.isAIActive, restored.isAIActive);
        assertEquals(original.currentGoal, restored.currentGoal);
        assertEquals(original.currentAction, restored.currentAction);
        assertEquals(original.getContext("mood"), restored.getContext("mood"));
        assertEquals(original.getContext("energy"), restored.getContext("energy"));
    }

    // 3) Interaction between subsystems: emotions, learning, and planning via context flags
    @Test
    public void testSubsystemInteractionViaContext() {
        VillagerAIState state = new VillagerAIState();

        // Emotions update sets a derived flag
        state.setContext("emotion_happiness", 80.0f);
        // Learning updates influence preferences
        state.setContext("learn_pref_socialize", 0.6f);
        // Planning reads these to decide current goal
        boolean shouldSocialize = ((Float) state.getContext("emotion_happiness")) > 70.0f
                && ((Float) state.getContext("learn_pref_socialize")) > 0.5f;

        state.currentGoal = shouldSocialize ? "Socialize" : "Work";
        assertEquals("Socialize", state.currentGoal);
    }

    // 4) Error handling scenarios: ensure context lookups/updates tolerate unexpected inputs
    @Test
    public void testErrorHandlingForAIContextOperations() {
        VillagerAIState state = new VillagerAIState();

        // Setting null key should not throw; map will accept null key? ConcurrentHashMap forbids null keys/values.
        // So validate we defensively avoid nulls by checking behavior.
        assertThrows(NullPointerException.class, () -> state.setContext(null, "x"));
        assertThrows(NullPointerException.class, () -> state.setContext("key", null));

        // Missing key should return null gracefully
        assertNull(state.getContext("does_not_exist"));

        // Type safety: insert int and read as Integer
        state.setContext("counter", 5);
        Object v = state.getContext("counter");
        assertTrue(v instanceof Integer);
        assertEquals(5, v);
    }

    // 5) Performance under load: bulk put/read within time budget
    @Test
    @Timeout(5)
    public void testPerformanceUnderLoad() {
        VillagerAIState state = new VillagerAIState();
        int total = 50_000;

        // Bulk writes
        for (int i = 0; i < total; i++) {
            state.setContext("p_" + i, i);
        }
        assertEquals(total, state.contextData.size());

        // Bulk reads
        long sum = 0;
        for (int i = 0; i < total; i++) {
            sum += (int) state.getContext("p_" + i);
        }
        // simple assertion to ensure loop executed and values were present
        assertTrue(sum > 0);
    }
}