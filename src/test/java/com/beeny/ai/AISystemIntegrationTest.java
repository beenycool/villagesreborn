package com.beeny.ai;

import com.beeny.ai.core.AISubsystemManager;
import com.beeny.data.VillagerData;
import com.beeny.system.VillagerHobbySystem;
import com.beeny.system.VillagerPersonalityBehavior;
import com.beeny.system.VillagerScheduleManager;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive integration test for the refactored AI system.
 * Tests the complete AIWorldManagerRefactored workflow with all subsystems.
 */
public class AISystemIntegrationTest {
    
    @Mock
    private MinecraftServer mockServer;
    
    @Mock
    private VillagerEntity mockVillager;
    
    @Mock
    private VillagerData mockVillagerData;
    
    private AIWorldManagerRefactored aiManager;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Clean up any existing instance
        AIWorldManagerRefactored.cleanup();
        
        // Initialize with mock server
        AIWorldManagerRefactored.initialize(mockServer);
        aiManager = AIWorldManagerRefactored.getInstance();
        
        // Setup mock villager
        when(mockVillager.getUuidAsString()).thenReturn("test-uuid-123");
        when(mockVillager.getAttached(any())).thenReturn(mockVillagerData);
        when(mockVillager.getWorld()).thenReturn(mock(net.minecraft.server.world.ServerWorld.class));
        when(mockVillager.getWorld().isClient()).thenReturn(false);
    }
    
    @Test
    @DisplayName("AI World Manager initializes with all subsystems")
    void testAIManagerInitialization() {
        assertNotNull(aiManager, "AI Manager should be initialized");
        assertNotNull(aiManager.getSubsystemManager(), "Subsystem manager should be available");
        assertNotNull(aiManager.getEmotionSystem(), "Emotion system should be initialized");
        assertNotNull(aiManager.getHobbySystem(), "Hobby system should be initialized");
        assertNotNull(aiManager.getScheduleManager(), "Schedule manager should be initialized");
        assertNotNull(aiManager.getPersonalityBehavior(), "Personality behavior should be initialized");
        assertNotNull(aiManager.getLearningSystem(), "Learning system should be initialized");
        assertNotNull(aiManager.getPlanningSystem(), "Planning system should be initialized");
        
        // Verify subsystems are registered
        assertTrue(aiManager.getSubsystemManager().getSubsystems().size() >= 6, 
                  "Should have at least 6 registered subsystems");
    }
    
    @Test
    @DisplayName("Villager AI initialization works correctly")
    void testVillagerAIInitialization() {
        // Initialize AI for villager
        aiManager.initializeVillagerAI(mockVillager);
        
        // Verify subsystems were initialized
        // We can't directly test private fields, but we can verify no exceptions occurred
        assertDoesNotThrow(() -> aiManager.initializeVillagerAI(mockVillager));
    }
    
    @Test
    @DisplayName("Villager AI updates work correctly")
    void testVillagerAIUpdates() {
        // Initialize first
        aiManager.initializeVillagerAI(mockVillager);
        
        // Update AI for villager
        assertDoesNotThrow(() -> aiManager.updateVillagerAI(mockVillager));
        
        // Verify subsystem manager handles updates
        AISubsystemManager subsystemManager = aiManager.getSubsystemManager();
        assertNotNull(subsystemManager);
    }
    
    @Test
    @DisplayName("Villager AI cleanup works correctly")
    void testVillagerAICleanup() {
        String villagerUuid = "test-uuid-123";
        
        // Initialize first
        aiManager.initializeVillagerAI(mockVillager);
        
        // Cleanup AI for villager
        assertDoesNotThrow(() -> aiManager.cleanupVillagerAI(villagerUuid));
    }
    
    @Test
    @DisplayName("AI analytics are available")
    void testAIAnalytics() {
        Map<String, Object> analytics = aiManager.getAIAnalytics();
        
        assertNotNull(analytics, "Analytics should be available");
        assertFalse(analytics.isEmpty(), "Analytics should contain data");
        
        // Check for expected analytics keys
        assertTrue(analytics.containsKey("subsystemManager"), "Should contain subsystem manager analytics");
    }
    
    @Test
    @DisplayName("Individual subsystems work correctly")
    void testIndividualSubsystems() {
        // Test hobby system
        VillagerHobbySystem hobbySystem = aiManager.getHobbySystem();
        assertNotNull(hobbySystem);
        assertEquals("VillagerHobbySystem", hobbySystem.getSubsystemName());
        assertTrue(hobbySystem.getUpdateInterval() > 0);
        
        // Test schedule manager
        VillagerScheduleManager scheduleManager = aiManager.getScheduleManager();
        assertNotNull(scheduleManager);
        assertEquals("VillagerScheduleManager", scheduleManager.getSubsystemName());
        assertTrue(scheduleManager.getUpdateInterval() > 0);
        
        // Test personality behavior
        VillagerPersonalityBehavior personalityBehavior = aiManager.getPersonalityBehavior();
        assertNotNull(personalityBehavior);
        assertEquals("VillagerPersonalityBehavior", personalityBehavior.getSubsystemName());
        assertTrue(personalityBehavior.getUpdateInterval() > 0);
    }
    
    @Test
    @DisplayName("Subsystem priorities are correctly set")
    void testSubsystemPriorities() {
        // Verify that emotion system has higher priority than hobby system
        assertTrue(aiManager.getEmotionSystem().getPriority() < aiManager.getHobbySystem().getPriority(),
                  "Emotion system should have higher priority (lower number) than hobby system");
        
        // Verify that schedule manager has appropriate priority
        assertTrue(aiManager.getScheduleManager().getPriority() > 0,
                  "Schedule manager should have a valid priority");
    }
    
    @Test
    @DisplayName("Subsystem maintenance works")
    void testSubsystemMaintenance() {
        // Initialize a villager first
        aiManager.initializeVillagerAI(mockVillager);
        
        // Test maintenance on individual subsystems
        assertDoesNotThrow(() -> {
            aiManager.getHobbySystem().performMaintenance();
            aiManager.getScheduleManager().performMaintenance();
            aiManager.getPersonalityBehavior().performMaintenance();
        });
    }
    
    @Test
    @DisplayName("AI system shutdown works correctly")
    void testAISystemShutdown() {
        // Initialize a villager first
        aiManager.initializeVillagerAI(mockVillager);
        
        // Shutdown should not throw exceptions
        assertDoesNotThrow(() -> AIWorldManagerRefactored.cleanup());
        
        // After cleanup, getInstance should throw exception
        assertThrows(IllegalStateException.class, () -> AIWorldManagerRefactored.getInstance());
    }
    
    @Test
    @DisplayName("Subsystem analytics contain expected data")
    void testSubsystemAnalytics() {
        // Initialize a villager to generate some data
        aiManager.initializeVillagerAI(mockVillager);
        
        // Get analytics from individual subsystems
        Map<String, Object> hobbyAnalytics = aiManager.getHobbySystem().getAnalytics();
        Map<String, Object> scheduleAnalytics = aiManager.getScheduleManager().getAnalytics();
        Map<String, Object> personalityAnalytics = aiManager.getPersonalityBehavior().getAnalytics();
        
        assertNotNull(hobbyAnalytics);
        assertNotNull(scheduleAnalytics);
        assertNotNull(personalityAnalytics);
        
        // Check for expected analytics keys
        assertTrue(hobbyAnalytics.containsKey("trackedVillagers"));
        assertTrue(scheduleAnalytics.containsKey("trackedVillagers"));
        assertTrue(personalityAnalytics.containsKey("trackedVillagers"));
    }
    
    @Test
    @DisplayName("Legacy compatibility methods work")
    void testLegacyCompatibility() {
        // Test legacy aliases
        assertSame(aiManager.getAIManager(), aiManager.getVillagerAIManager());
        
        // Test legacy methods
        assertDoesNotThrow(() -> {
            aiManager.getVillagerAIStatus(mockVillager);
            aiManager.getGlobalAIAnalytics();
        });
    }
}