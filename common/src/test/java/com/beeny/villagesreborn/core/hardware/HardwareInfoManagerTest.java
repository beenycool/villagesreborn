package com.beeny.villagesreborn.core.hardware;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.CentralProcessor.ProcessorIdentifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test-Driven Development tests for HardwareInfoManager
 * Tests hardware detection, tier classification, and caching
 */
@DisplayName("Hardware Info Manager Tests")
class HardwareInfoManagerTest {

    @Mock
    private SystemInfo systemInfo;
    
    @Mock
    private HardwareAbstractionLayer hardware;
    
    @Mock
    private GlobalMemory memory;
    
    @Mock
    private CentralProcessor processor;
    
    @Mock
    private ProcessorIdentifier processorIdentifier;

    private HardwareInfoManager hardwareInfoManager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Reset singleton instance before each test
        HardwareInfoManager.resetInstance();
        
        when(systemInfo.getHardware()).thenReturn(hardware);
        when(hardware.getMemory()).thenReturn(memory);
        when(hardware.getProcessor()).thenReturn(processor);
        when(processor.getProcessorIdentifier()).thenReturn(processorIdentifier);
        
        hardwareInfoManager = new HardwareInfoManager(systemInfo);
        // Set the test instance as the singleton
        HardwareInfoManager.setInstance(hardwareInfoManager);
    }

    @AfterEach
    void tearDown() {
        // Reset singleton instance after each test
        HardwareInfoManager.resetInstance();
    }

    @Test
    @DisplayName("Should get singleton instance")
    void shouldGetSingletonInstance() {
        // Given: Reset singleton
        HardwareInfoManager.resetInstance();
        
        // When: Getting instance
        HardwareInfoManager instance1 = HardwareInfoManager.getInstance();
        HardwareInfoManager instance2 = HardwareInfoManager.getInstance();
        
        // Then: Should return same instance
        assertNotNull(instance1);
        assertSame(instance1, instance2);
    }

    @Test
    @DisplayName("Should detect RAM amount correctly")
    void shouldDetectRamAmount() {
        // Given: 16GB RAM (in bytes)
        long ramInBytes = 16L * 1024 * 1024 * 1024;
        when(memory.getTotal()).thenReturn(ramInBytes);

        // When: Getting hardware info
        HardwareInfo info = hardwareInfoManager.getHardwareInfo();

        // Then: Should return 16GB
        assertEquals(16, info.getRamGB());
    }

    @Test
    @DisplayName("Should detect CPU core count correctly")
    void shouldDetectCpuCoreCount() {
        // Given: 8 logical cores
        when(processor.getLogicalProcessorCount()).thenReturn(8);

        // When: Getting hardware info
        HardwareInfo info = hardwareInfoManager.getHardwareInfo();

        // Then: Should return 8 cores
        assertEquals(8, info.getCpuCores());
    }

    @Test
    @DisplayName("Should detect AVX2 support correctly")
    void shouldDetectAvx2Support() {
        // Given: Intel CPU with AVX2 support
        when(processorIdentifier.getIdentifier()).thenReturn("Intel64 Family 6 Model 60 Stepping 3");
        when(processorIdentifier.getName()).thenReturn("Intel(R) Core(TM) i7-4770K CPU @ 3.50GHz");

        // When: Getting hardware info
        HardwareInfo info = hardwareInfoManager.getHardwareInfo();

        // Then: Should detect AVX2 support
        assertTrue(info.hasAvx2Support());
    }

    @Test
    @DisplayName("Should detect when AVX2 is not supported")
    void shouldDetectNoAvx2Support() {
        // Given: Older CPU without AVX2 support (pre-Haswell)
        when(processorIdentifier.getIdentifier()).thenReturn("Intel64 Family 6 Model 42 Stepping 7");
        when(processorIdentifier.getName()).thenReturn("Intel(R) Pentium(R) CPU G630 @ 2.70GHz");

        // When: Getting hardware info
        HardwareInfo info = hardwareInfoManager.getHardwareInfo();

        // Then: Should not detect AVX2 support
        assertFalse(info.hasAvx2Support());
    }

    @Test
    @DisplayName("Should classify hardware as HIGH tier")
    void shouldClassifyHighTierHardware() {
        // Given: High-end hardware (32GB RAM, 16 cores, AVX2)
        when(memory.getTotal()).thenReturn(32L * 1024 * 1024 * 1024);
        when(processor.getLogicalProcessorCount()).thenReturn(16);
        when(processorIdentifier.getName()).thenReturn("Intel(R) Core(TM) i9-12900K CPU @ 3.20GHz");

        // When: Getting hardware info
        HardwareInfo info = hardwareInfoManager.getHardwareInfo();

        // Then: Should classify as HIGH tier
        assertEquals(HardwareTier.HIGH, info.getHardwareTier());
    }

    @Test
    @DisplayName("Should classify hardware as MEDIUM tier")
    void shouldClassifyMediumTierHardware() {
        // Given: Medium hardware (16GB RAM, 8 cores, AVX2)
        when(memory.getTotal()).thenReturn(16L * 1024 * 1024 * 1024);
        when(processor.getLogicalProcessorCount()).thenReturn(8);
        when(processorIdentifier.getName()).thenReturn("Intel(R) Core(TM) i7-8700K CPU @ 3.70GHz");

        // When: Getting hardware info
        HardwareInfo info = hardwareInfoManager.getHardwareInfo();

        // Then: Should classify as MEDIUM tier
        assertEquals(HardwareTier.MEDIUM, info.getHardwareTier());
    }

    @Test
    @DisplayName("Should classify hardware as LOW tier")
    void shouldClassifyLowTierHardware() {
        // Given: Low-end hardware (4GB RAM, 2 cores, no AVX2)
        when(memory.getTotal()).thenReturn(4L * 1024 * 1024 * 1024);
        when(processor.getLogicalProcessorCount()).thenReturn(2);
        when(processorIdentifier.getName()).thenReturn("Intel(R) Pentium(R) CPU G4560 @ 3.50GHz");

        // When: Getting hardware info
        HardwareInfo info = hardwareInfoManager.getHardwareInfo();

        // Then: Should classify as LOW tier
        assertEquals(HardwareTier.LOW, info.getHardwareTier());
    }

    @Test
    @DisplayName("Should cache hardware info")
    void shouldCacheHardwareInfo() {
        // Given: Hardware info manager
        when(memory.getTotal()).thenReturn(8L * 1024 * 1024 * 1024);
        when(processor.getLogicalProcessorCount()).thenReturn(4);
        when(processorIdentifier.getName()).thenReturn("Intel(R) Core(TM) i5-6500 CPU @ 3.20GHz");

        // When: Getting hardware info twice
        HardwareInfo info1 = hardwareInfoManager.getHardwareInfo();
        HardwareInfo info2 = hardwareInfoManager.getHardwareInfo();

        // Then: Should only call system info once (cached)
        verify(hardware, times(1)).getMemory();
        verify(hardware, times(1)).getProcessor();
        assertSame(info1, info2);
    }

    @Test
    @DisplayName("Should handle hardware detection failure gracefully")
    void shouldHandleDetectionFailureGracefully() {
        // Given: Hardware detection throws exception
        when(hardware.getMemory()).thenThrow(new RuntimeException("Hardware detection failed"));

        // When: Getting hardware info
        HardwareInfo info = hardwareInfoManager.getHardwareInfo();

        // Then: Should return fallback info
        assertNotNull(info);
        assertEquals(HardwareTier.UNKNOWN, info.getHardwareTier());
        assertEquals(0, info.getRamGB());
        assertEquals(0, info.getCpuCores());
        assertFalse(info.hasAvx2Support());
    }

    @Test
    @DisplayName("Should clear cache when requested")
    void shouldClearCacheWhenRequested() {
        // Given: Cached hardware info
        when(memory.getTotal()).thenReturn(8L * 1024 * 1024 * 1024);
        when(processor.getLogicalProcessorCount()).thenReturn(4);
        when(processorIdentifier.getName()).thenReturn("Intel(R) Core(TM) i5-6500 CPU @ 3.20GHz");
        
        HardwareInfo info1 = hardwareInfoManager.getHardwareInfo();

        // When: Clearing cache and getting info again
        hardwareInfoManager.clearCache();
        HardwareInfo info2 = hardwareInfoManager.getHardwareInfo();

        // Then: Should call system info again
        verify(hardware, times(2)).getMemory();
        verify(hardware, times(2)).getProcessor();
        assertNotSame(info1, info2);
    }
}