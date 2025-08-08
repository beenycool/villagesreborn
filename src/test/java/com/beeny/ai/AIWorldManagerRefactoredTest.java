package com.beeny.ai;

import com.beeny.system.VillagerHobbySystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import net.minecraft.server.MinecraftServer;
import net.minecraft.entity.passive.VillagerEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AIWorldManagerRefactored subsystem coordination
 */
public class AIWorldManagerRefactoredTest {
    
    @Mock
    private MinecraftServer mockServer;
    
    @Mock 
    private VillagerEntity mockVillager;
    
    private AIWorldManagerRefactored aiManager;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Reset singleton for testing using reflection
        try {
            java.lang.reflect.Field instanceField = AIWorldManagerRefactored.class.getDeclaredField("instance");
            instanceField.setAccessible(true);
            instanceField.set(null, null);
        } catch (Exception e) {
            // Ignore reflection errors in test environment
        }
    }
    
    @Test
    void testSingletonInitialization() {
        AIWorldManagerRefactored.initialize(mockServer);
        AIWorldManagerRefactored instance1 = AIWorldManagerRefactored.getInstance();
        AIWorldManagerRefactored instance2 = AIWorldManagerRefactored.getInstance();
        
        assertSame(instance1, instance2);
        assertNotNull(instance1);
    }
    
    @Test
    void testGetInstanceWithoutInitialization() {
        assertThrows(IllegalStateException.class, () -> {
            AIWorldManagerRefactored.getInstance();
        });
    }
    
    @Test
    void testSubsystemAccess() {
        AIWorldManagerRefactored.initialize(mockServer);
        AIWorldManagerRefactored manager = AIWorldManagerRefactored.getInstance();
        
        VillagerHobbySystem hobbySystem = manager.getHobbySystem();
        assertNotNull(hobbySystem);
        assertEquals("VillagerHobbySystem", hobbySystem.getSubsystemName());
    }
    
    @Test 
    void testVillagerLifecycleMethods() {
        AIWorldManagerRefactored.initialize(mockServer);
        AIWorldManagerRefactored manager = AIWorldManagerRefactored.getInstance();
        
        // Should not throw exceptions
        assertDoesNotThrow(() -> {
            manager.initializeVillagerAI(mockVillager);
            manager.updateVillagerAI(mockVillager);
            manager.cleanupVillagerAI("test-uuid");
        });
    }
    
    @Test
    void testShutdown() {
        AIWorldManagerRefactored.initialize(mockServer);
        AIWorldManagerRefactored manager = AIWorldManagerRefactored.getInstance();
        
        assertDoesNotThrow(() -> {
            manager.shutdown();
        });
    }
}