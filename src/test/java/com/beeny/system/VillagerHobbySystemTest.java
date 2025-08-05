package com.beeny.system;

import com.beeny.data.VillagerData;
import com.beeny.constants.VillagerConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for VillagerHobbySystem AISubsystem implementation
 */
public class VillagerHobbySystemTest {
    
    private VillagerHobbySystem hobbySystem;
    
    @Mock
    private VillagerEntity mockVillager;
    
    @Mock
    private ServerWorld mockWorld;
    
    private VillagerData testData;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        hobbySystem = new VillagerHobbySystem();
        testData = new VillagerData();
        testData.setHobby(VillagerConstants.HobbyType.GARDENING);
        
        when(mockVillager.getWorld()).thenReturn(mockWorld);
        when(mockWorld.isClient()).thenReturn(false);
    }
    
    @Test
    void testSubsystemInterface() {
        assertEquals("VillagerHobbySystem", hobbySystem.getSubsystemName());
        assertEquals(10000L, hobbySystem.getUpdateInterval());
        assertTrue(hobbySystem.isEnabled());
        assertEquals(100, hobbySystem.getPriority());
    }
    
    @Test
    void testInitializeVillager() {
        // Should not throw exception
        assertDoesNotThrow(() -> {
            hobbySystem.initializeVillager(mockVillager, testData);
        });
    }
    
    @Test
    void testCleanupVillager() {
        // Should not throw exception
        assertDoesNotThrow(() -> {
            hobbySystem.cleanupVillager("test-uuid");
        });
    }
    
    @Test
    void testAnalytics() {
        Map<String, Object> analytics = hobbySystem.getAnalytics();
        assertNotNull(analytics);
        assertEquals("hobby", analytics.get("subsystem"));
    }
    
    @Test
    void testNeedsUpdateClientSide() {
        when(mockVillager.getWorld()).thenReturn(mockWorld);
        when(mockWorld.isClient()).thenReturn(true);
        
        assertFalse(hobbySystem.needsUpdate(mockVillager));
    }
    
    @Test
    void testMaintenanceAndShutdown() {
        // Should not throw exceptions
        assertDoesNotThrow(() -> {
            hobbySystem.performMaintenance();
            hobbySystem.shutdown();
        });
    }
    
    @Test
    void testPerformHobbyActivityNullData() {
        // Should handle null data gracefully
        assertDoesNotThrow(() -> {
            hobbySystem.performHobbyActivity(mockVillager, null);
        });
    }
    
    @Test
    void testPerformHobbyActivityClientSide() {
        when(mockWorld.isClient()).thenReturn(true);
        
        // Should return early for client side
        assertDoesNotThrow(() -> {
            hobbySystem.performHobbyActivity(mockVillager, testData);
        });
    }
    
    @Test
    void testHobbyActivityDescription() {
        assertEquals("tending to plants", 
            VillagerHobbySystem.getHobbyActivityDescription(VillagerConstants.HobbyType.GARDENING));
        assertEquals("fishing by the water", 
            VillagerHobbySystem.getHobbyActivityDescription(VillagerConstants.HobbyType.FISHING));
        assertEquals("chatting with neighbors", 
            VillagerHobbySystem.getHobbyActivityDescription(VillagerConstants.HobbyType.GOSSIPING));
    }
}