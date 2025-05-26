package com.beeny.villagesreborn.core.hardware;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.*;

/**
 * Enhanced unit tests for HardwareInfoManager
 * Tests hardware detection, tier classification, performance scoring, and fallback behavior
 */
@ExtendWith(MockitoExtension.class)
class HardwareInfoManagerEnhancedTest {

    @Mock
    private SystemInfo mockSystemInfo;
    
    @Mock
    private HardwareAbstractionLayer mockHardware;
    
    @Mock
    private GlobalMemory mockMemory;
    
    @Mock
    private CentralProcessor mockProcessor;
    
    @Mock
    private CentralProcessor.ProcessorIdentifier mockProcessorId;
    
    private HardwareInfoManager hardwareManager;
    
    @BeforeEach
    void setUp() {
        // Reset singleton for each test
        HardwareInfoManager.resetInstance();
        hardwareManager = new HardwareInfoManager(mockSystemInfo);
    }
    
    @AfterEach
    void tearDown() {
        HardwareInfoManager.resetInstance();
    }
    
    @Test
    void testHighTierHardwareDetection() {
        // GIVEN: System with high-end hardware
        setupMockSystemInfo(32L * 1024 * 1024 * 1024, 16, "Intel Core i9", true);
        
        // WHEN: Hardware is detected
        HardwareInfo hardwareInfo = hardwareManager.getHardwareInfo();
        
        // THEN: Hardware is classified as HIGH tier
        assertThat(hardwareInfo.getHardwareTier()).isEqualTo(HardwareTier.HIGH);
        assertThat(hardwareInfo.getRamGB()).isEqualTo(32);
        assertThat(hardwareInfo.getCpuCores()).isEqualTo(16);
        assertThat(hardwareInfo.hasAvx2Support()).isTrue();
    }
    
    @Test
    void testMediumTierHardwareDetection() {
        // GIVEN: System with medium-tier hardware
        setupMockSystemInfo(16L * 1024 * 1024 * 1024, 8, "Intel Core i5", true);
        
        // WHEN: Hardware is detected
        HardwareInfo hardwareInfo = hardwareManager.getHardwareInfo();
        
        // THEN: Hardware is classified as MEDIUM tier
        assertThat(hardwareInfo.getHardwareTier()).isEqualTo(HardwareTier.MEDIUM);
        assertThat(hardwareInfo.getRamGB()).isEqualTo(16);
        assertThat(hardwareInfo.getCpuCores()).isEqualTo(8);
        assertThat(hardwareInfo.hasAvx2Support()).isTrue();
    }
    
    @Test
    void testLowTierHardwareDetection() {
        // GIVEN: System with low-tier hardware
        setupMockSystemInfo(4L * 1024 * 1024 * 1024, 2, "Intel Celeron", false);
        
        // WHEN: Hardware is detected
        HardwareInfo hardwareInfo = hardwareManager.getHardwareInfo();
        
        // THEN: Hardware is classified as LOW tier
        assertThat(hardwareInfo.getHardwareTier()).isEqualTo(HardwareTier.LOW);
        assertThat(hardwareInfo.getRamGB()).isEqualTo(4);
        assertThat(hardwareInfo.getCpuCores()).isEqualTo(2);
        assertThat(hardwareInfo.hasAvx2Support()).isFalse();
    }
    
    @Test
    void testFallbackOnDetectionFailure() {
        // GIVEN: SystemInfo that throws exceptions
        when(mockSystemInfo.getHardware()).thenThrow(new RuntimeException("Detection failed"));
        
        // WHEN: Hardware detection is attempted
        HardwareInfo hardwareInfo = hardwareManager.getHardwareInfoWithFallback();
        
        // THEN: Fallback configuration is returned
        assertThat(hardwareInfo).isNotNull();
        assertThat(hardwareInfo.getHardwareTier()).isEqualTo(HardwareTier.UNKNOWN);
        assertThat(hardwareInfo.getRamGB()).isGreaterThan(0);
        assertThat(hardwareInfo.getCpuCores()).isGreaterThan(0);
    }
    
    @Test
    void testPerformanceScoreCalculation() {
        // GIVEN: Hardware with known specifications
        int ramGB = 16;
        int cpuCores = 8;
        boolean hasAvx2 = true;
        
        // WHEN: Performance score is calculated
        double score = HardwareInfoManager.calculatePerformanceScore(ramGB, cpuCores, hasAvx2);
        
        // THEN: Score reflects hardware capabilities
        double expectedScore = (16.0/32.0 * 40) + (8.0/16.0 * 40) + 20; // 20 + 20 + 20 = 60
        assertThat(score).isEqualTo(expectedScore, within(0.1));
    }
    
    @Test
    void testPerformanceScoreCalculationMaxValues() {
        // GIVEN: Maximum hardware specifications
        int ramGB = 64; // Above the 32GB baseline
        int cpuCores = 32; // Above the 16 core baseline
        boolean hasAvx2 = true;
        
        // WHEN: Performance score is calculated
        double score = HardwareInfoManager.calculatePerformanceScore(ramGB, cpuCores, hasAvx2);
        
        // THEN: Score is capped at maximum
        double expectedScore = 40 + 40 + 20; // 100 (maximum possible)
        assertThat(score).isEqualTo(expectedScore, within(0.1));
    }
    
    @Test
    void testPerformanceScoreCalculationMinValues() {
        // GIVEN: Minimal hardware specifications
        int ramGB = 2;
        int cpuCores = 1;
        boolean hasAvx2 = false;
        
        // WHEN: Performance score is calculated
        double score = HardwareInfoManager.calculatePerformanceScore(ramGB, cpuCores, hasAvx2);
        
        // THEN: Score reflects low capabilities
        double expectedScore = (2.0/32.0 * 40) + (1.0/16.0 * 40) + 0; // 2.5 + 2.5 + 0 = 5
        assertThat(score).isEqualTo(5.0, within(0.1));
    }
    
    @Test
    void testCachingBehavior() {
        // GIVEN: System with hardware info
        setupMockSystemInfo(16L * 1024 * 1024 * 1024, 8, "Intel Core i5", true);
        
        // WHEN: Hardware is detected multiple times
        HardwareInfo first = hardwareManager.getHardwareInfo();
        HardwareInfo second = hardwareManager.getHardwareInfo();
        
        // THEN: Same instance is returned (cached)
        assertThat(first).isSameAs(second);
        
        // AND: SystemInfo is only called once
        verify(mockSystemInfo, times(1)).getHardware();
    }
    
    @Test
    void testCacheClear() {
        // GIVEN: System with cached hardware info
        setupMockSystemInfo(16L * 1024 * 1024 * 1024, 8, "Intel Core i5", true);
        hardwareManager.getHardwareInfo(); // Cache the result
        
        // WHEN: Cache is cleared
        hardwareManager.clearCache();
        
        // AND: Hardware is detected again
        hardwareManager.getHardwareInfo();
        
        // THEN: SystemInfo is called again
        verify(mockSystemInfo, times(2)).getHardware();
    }
    
    @Test
    void testRetryLogicOnTransientFailure() {
        // GIVEN: SystemInfo that fails first call but succeeds on retry
        when(mockSystemInfo.getHardware())
            .thenThrow(new RuntimeException("Transient failure"))
            .thenReturn(mockHardware);
        
        setupMockHardware(16L * 1024 * 1024 * 1024, 8, "Intel Core i5", true);
        
        // WHEN: Hardware detection is attempted
        HardwareInfo hardwareInfo = hardwareManager.getHardwareInfoWithFallback();
        
        // THEN: Detection succeeds after retry
        assertThat(hardwareInfo.getHardwareTier()).isEqualTo(HardwareTier.MEDIUM);
        
        // AND: SystemInfo was called multiple times
        verify(mockSystemInfo, atLeast(2)).getHardware();
    }
    
    @Test
    void testAVX2DetectionIntel() {
        // GIVEN: Intel processor
        setupMockSystemInfo(16L * 1024 * 1024 * 1024, 8, "Intel Core i7-4770K", false);
        
        // WHEN: Hardware is detected
        HardwareInfo hardwareInfo = hardwareManager.getHardwareInfo();
        
        // THEN: AVX2 is detected for Intel Core processors
        assertThat(hardwareInfo.hasAvx2Support()).isTrue();
    }
    
    @Test
    void testAVX2DetectionAMD() {
        // GIVEN: AMD Ryzen processor
        setupMockSystemInfo(16L * 1024 * 1024 * 1024, 8, "AMD Ryzen 5 3600", false);
        
        // WHEN: Hardware is detected
        HardwareInfo hardwareInfo = hardwareManager.getHardwareInfo();
        
        // THEN: AVX2 is detected for Ryzen processors
        assertThat(hardwareInfo.hasAvx2Support()).isTrue();
    }
    
    @Test
    void testSingletonBehavior() {
        // GIVEN: Multiple getInstance calls
        HardwareInfoManager instance1 = HardwareInfoManager.getInstance();
        HardwareInfoManager instance2 = HardwareInfoManager.getInstance();
        
        // THEN: Same instance is returned
        assertThat(instance1).isSameAs(instance2);
    }
    
    private void setupMockSystemInfo(long memoryBytes, int cpuCores, String processorName, boolean forceAvx2) {
        when(mockSystemInfo.getHardware()).thenReturn(mockHardware);
        setupMockHardware(memoryBytes, cpuCores, processorName, forceAvx2);
    }
    
    private void setupMockHardware(long memoryBytes, int cpuCores, String processorName, boolean forceAvx2) {
        when(mockHardware.getMemory()).thenReturn(mockMemory);
        when(mockHardware.getProcessor()).thenReturn(mockProcessor);
        
        when(mockMemory.getTotal()).thenReturn(memoryBytes);
        when(mockProcessor.getLogicalProcessorCount()).thenReturn(cpuCores);
        when(mockProcessor.getProcessorIdentifier()).thenReturn(mockProcessorId);
        when(mockProcessorId.getName()).thenReturn(processorName);
    }
}