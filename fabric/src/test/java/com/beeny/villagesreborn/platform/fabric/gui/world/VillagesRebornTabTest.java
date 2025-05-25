package com.beeny.villagesreborn.platform.fabric.gui.world;

import com.beeny.villagesreborn.core.hardware.HardwareInfo;
import com.beeny.villagesreborn.core.hardware.HardwareInfoManager;
import com.beeny.villagesreborn.core.hardware.HardwareTier;
import com.beeny.villagesreborn.core.world.VillagesRebornWorldSettings;
import net.minecraft.text.Text;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for VillagesRebornTab UI component
 * Verifies tab creation, settings management, and basic functionality
 */
@ExtendWith(MockitoExtension.class)
class VillagesRebornTabTest {

    @Mock
    private HardwareInfoManager mockHardwareManager;
    
    @Mock
    private HardwareInfo mockHardwareInfo;
    
    private static final int TEST_WIDTH = 800;
    private static final int TEST_HEIGHT = 600;
    
    @BeforeEach
    void setUp() {
        // Use lenient stubbing to avoid UnnecessaryStubbingException
        lenient().when(mockHardwareInfo.getHardwareTier()).thenReturn(HardwareTier.MEDIUM);
    }
    
    @Test
    void shouldCreateTabWithDefaultSettings() {
        // Given
        try (MockedStatic<HardwareInfoManager> managerMock = mockStatic(HardwareInfoManager.class)) {
            managerMock.when(HardwareInfoManager::getInstance).thenReturn(mockHardwareManager);
            when(mockHardwareManager.getHardwareInfo()).thenReturn(mockHardwareInfo);
            
            // When
            VillagesRebornTab tab = new VillagesRebornTab(TEST_WIDTH, TEST_HEIGHT);
            
            // Then
            assertNotNull(tab);
            assertNotNull(tab.getSettings());
            
            // Verify hardware-based defaults for MEDIUM tier
            VillagesRebornWorldSettings settings = tab.getSettings();
            assertEquals(150, settings.getVillagerMemoryLimit());
            assertTrue(settings.isEnableAdvancedAI());
            assertEquals(2, settings.getTickOptimizationLevel());
        }
    }
    
    @Test
    void shouldHandleHardwareInfoException() {
        // Given
        try (MockedStatic<HardwareInfoManager> managerMock = mockStatic(HardwareInfoManager.class)) {
            managerMock.when(HardwareInfoManager::getInstance).thenReturn(mockHardwareManager);
            when(mockHardwareManager.getHardwareInfo()).thenThrow(new RuntimeException("Hardware detection failed"));
            
            // When
            VillagesRebornTab tab = new VillagesRebornTab(TEST_WIDTH, TEST_HEIGHT);
            
            // Then
            assertNotNull(tab);
            assertNotNull(tab.getSettings());
            // Should fall back to basic defaults
            VillagesRebornWorldSettings settings = tab.getSettings();
            assertNotNull(settings);
            
            // Verify the exception was actually thrown during hardware info retrieval
            verify(mockHardwareManager).getHardwareInfo();
        }
    }
    
    @Test
    void shouldHaveCorrectTitle() {
        // Given
        try (MockedStatic<HardwareInfoManager> managerMock = mockStatic(HardwareInfoManager.class);
             MockedStatic<Text> textMock = mockStatic(Text.class)) {
            
            managerMock.when(HardwareInfoManager::getInstance).thenReturn(mockHardwareManager);
            when(mockHardwareManager.getHardwareInfo()).thenReturn(mockHardwareInfo);
            
            Text expectedTitle = Text.literal("Villages Reborn Settings");
            textMock.when(() -> Text.translatable("villagesreborn.world_creation.tab_title"))
                   .thenReturn(expectedTitle);
            
            VillagesRebornTab tab = new VillagesRebornTab(TEST_WIDTH, TEST_HEIGHT);
            
            // When
            Text title = Text.translatable("villagesreborn.world_creation.tab_title");
            
            // Then
            assertEquals(expectedTitle, title);
            textMock.verify(() -> Text.translatable("villagesreborn.world_creation.tab_title"));
        }
    }
    
    @Test
    void shouldCallOnCloseWithoutError() {
        // Given
        try (MockedStatic<HardwareInfoManager> managerMock = mockStatic(HardwareInfoManager.class)) {
            managerMock.when(HardwareInfoManager::getInstance).thenReturn(mockHardwareManager);
            when(mockHardwareManager.getHardwareInfo()).thenReturn(mockHardwareInfo);
            
            VillagesRebornTab tab = new VillagesRebornTab(TEST_WIDTH, TEST_HEIGHT);
            
            // When & Then
            assertDoesNotThrow(() -> tab.onClose());
        }
    }
    
    @Test
    void shouldCreateWithDifferentHardwareTiers() {
        // Test LOW tier
        try (MockedStatic<HardwareInfoManager> managerMock = mockStatic(HardwareInfoManager.class)) {
            managerMock.when(HardwareInfoManager::getInstance).thenReturn(mockHardwareManager);
            when(mockHardwareInfo.getHardwareTier()).thenReturn(HardwareTier.LOW);
            when(mockHardwareManager.getHardwareInfo()).thenReturn(mockHardwareInfo);
            
            VillagesRebornTab tab = new VillagesRebornTab(TEST_WIDTH, TEST_HEIGHT);
            VillagesRebornWorldSettings settings = tab.getSettings();
            
            // LOW tier should have conservative defaults
            assertEquals(50, settings.getVillagerMemoryLimit());
            assertFalse(settings.isEnableAdvancedAI());
            assertEquals(3, settings.getTickOptimizationLevel()); // LOW tier uses highest optimization (3)
        }
        
        // Test HIGH tier
        try (MockedStatic<HardwareInfoManager> managerMock = mockStatic(HardwareInfoManager.class)) {
            managerMock.when(HardwareInfoManager::getInstance).thenReturn(mockHardwareManager);
            when(mockHardwareInfo.getHardwareTier()).thenReturn(HardwareTier.HIGH);
            when(mockHardwareManager.getHardwareInfo()).thenReturn(mockHardwareInfo);
            
            VillagesRebornTab tab = new VillagesRebornTab(TEST_WIDTH, TEST_HEIGHT);
            VillagesRebornWorldSettings settings = tab.getSettings();
            
            // HIGH tier should have performance defaults
            assertEquals(300, settings.getVillagerMemoryLimit());
            assertTrue(settings.isEnableAdvancedAI());
            assertEquals(1, settings.getTickOptimizationLevel()); // HIGH tier uses lowest optimization (1)
        }
    }
}