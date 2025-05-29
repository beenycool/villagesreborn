package com.beeny.villagesreborn.core.hardware;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.CentralProcessor.ProcessorIdentifier;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;

/**
 * Manages hardware information detection and caching
 * Uses OSHI library for cross-platform hardware detection
 */
public class HardwareInfoManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(HardwareInfoManager.class);
    
    // Singleton instance
    private static volatile HardwareInfoManager instance;
    private static final Object LOCK = new Object();
    
    private final SystemInfo systemInfo;
    private volatile HardwareInfo cachedHardwareInfo;

    public HardwareInfoManager() {
        this(new SystemInfo());
    }

    // Package-private constructor for testing
    HardwareInfoManager(SystemInfo systemInfo) {
        this.systemInfo = systemInfo;
    }

    /**
     * Get the singleton instance of HardwareInfoManager
     */
    public static HardwareInfoManager getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new HardwareInfoManager();
                }
            }
        }
        return instance;
    }

    /**
     * Set a custom instance (for testing purposes)
     */
    public static void setInstance(HardwareInfoManager customInstance) {
        synchronized (LOCK) {
            instance = customInstance;
        }
    }

    /**
     * Reset the singleton instance (for testing purposes)
     */
    public static void resetInstance() {
        synchronized (LOCK) {
            instance = null;
        }
    }

    /**
     * Get hardware information, using cached result if available
     */
    public HardwareInfo getHardwareInfo() {
        if (cachedHardwareInfo == null) {
            synchronized (this) {
                if (cachedHardwareInfo == null) {
                    cachedHardwareInfo = getHardwareInfoWithFallback();
                }
            }
        }
        return cachedHardwareInfo;
    }

    /**
     * Get hardware information with fallback on detection failure
     */
    public HardwareInfo getHardwareInfoWithFallback() {
        try {
            if (cachedHardwareInfo != null) {
                return cachedHardwareInfo;
            }
            
            HardwareInfo detectedInfo = detectHardwareInfoWithRetry();
            
            // Validate detection results
            if (!isValidHardwareInfo(detectedInfo)) {
                throw new RuntimeException("Invalid hardware detection results");
            }
            
            return detectedInfo;
            
        } catch (Exception e) {
            LOGGER.warn("Hardware detection failed, using fallback configuration: {}", e.getMessage());
            return HardwareInfo.createFallback();
        }
    }

    /**
     * Clear cached hardware information
     */
    public void clearCache() {
        synchronized (this) {
            cachedHardwareInfo = null;
        }
    }

    /**
     * Detect hardware information with retry logic
     */
    private HardwareInfo detectHardwareInfoWithRetry() {
        int maxRetries = 3;
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                HardwareAbstractionLayer hardware = systemInfo.getHardware();
                
                // Detect RAM
                GlobalMemory memory = hardware.getMemory();
                long ramBytes = memory.getTotal();
                int ramGB = (int) (ramBytes / (1024L * 1024L * 1024L));
                
                // Detect CPU cores
                CentralProcessor processor = hardware.getProcessor();
                int cpuCores = processor.getLogicalProcessorCount();
                
                // Detect AVX2 support
                boolean hasAvx2 = detectAvx2Support(processor);
                
                // Enhanced tier classification with performance scoring
                double performanceScore = calculatePerformanceScore(ramGB, cpuCores, hasAvx2);
                HardwareTier tier = classifyHardwareTier(performanceScore);
                
                HardwareInfo info = new HardwareInfo(ramGB, cpuCores, hasAvx2, tier);
                
                // Validate minimum requirements
                validateMinimumRequirements(info);
                
                LOGGER.info("Hardware detected: {} (Performance Score: {})", info, performanceScore);
                return info;
                
            } catch (Exception e) {
                if (attempt == maxRetries) {
                    throw new RuntimeException("Failed after " + maxRetries + " attempts", e);
                }
                
                LOGGER.debug("Hardware detection attempt {} failed, retrying...", attempt);
                try {
                    Thread.sleep(1000L * attempt); // Exponential backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Hardware detection interrupted", ie);
                }
            }
        }
        
        throw new RuntimeException("Hardware detection failed after all retries");
    }

    private HardwareInfo detectHardwareInfo() {
        return detectHardwareInfoWithRetry();
    }

    /**
     * Calculate performance score based on hardware specifications
     */
    public static double calculatePerformanceScore(int ramGB, int cpuCores, boolean hasAvx2) {
        double score = 0;
        
        // RAM scoring (40% weight)
        double ramScore = Math.min(ramGB / 32.0, 1.0) * 40;
        score += ramScore;
        
        // CPU cores scoring (40% weight)
        double coreScore = Math.min(cpuCores / 16.0, 1.0) * 40;
        score += coreScore;
        
        // AVX2 support (20% weight)
        double avxScore = hasAvx2 ? 20 : 0;
        score += avxScore;
        
        return score;
    }

    /**
     * Classify hardware tier based on performance score
     */
    private HardwareTier classifyHardwareTier(double performanceScore) {
        if (performanceScore >= 80) {
            return HardwareTier.HIGH;
        } else if (performanceScore >= 50) {
            return HardwareTier.MEDIUM;
        } else if (performanceScore >= 25) {
            return HardwareTier.LOW;
        } else {
            return HardwareTier.UNKNOWN;
        }
    }

    /**
     * Validate that hardware info contains reasonable values
     */
    private boolean isValidHardwareInfo(HardwareInfo info) {
        return info != null &&
               info.getRamGB() > 0 &&
               info.getCpuCores() > 0 &&
               info.getHardwareTier() != null;
    }

    /**
     * Validate minimum system requirements
     */
    private void validateMinimumRequirements(HardwareInfo info) {
        if (info.getRamGB() < 2) {
            LOGGER.warn("System has less than 2GB RAM, performance may be poor");
        }
        if (info.getCpuCores() < 2) {
            LOGGER.warn("System has less than 2 CPU cores, performance may be poor");
        }
    }

    /**
     * Detect AVX2 support using processor information
     * This is a simplified detection method since OSHI API varies across versions
     */
    private boolean detectAvx2Support(CentralProcessor processor) {
        try {
            // Check processor identifier for modern CPU families that typically support AVX2
            ProcessorIdentifier procId = processor.getProcessorIdentifier();
            if (procId == null) {
                return false;
            }
            
            String name = procId.getName();
            if (name == null || name.trim().isEmpty()) {
                return false;
            }
            
            name = name.toLowerCase();
            
            // Intel processors: Haswell (4th gen) and newer support AVX2
            if (name.contains("intel")) {
                // Look for Core i3/i5/i7/i9 4th gen and newer, or Xeon E5-2600 v3 and newer
                if (name.matches(".*core.*i[3579].*") || name.contains("xeon")) {
                    return true; // Assume modern Intel processors support AVX2
                }
            }
            
            // AMD processors: Excavator architecture and newer support AVX2
            if (name.contains("amd")) {
                // Look for Ryzen, EPYC, or FX-9xxx series
                if (name.contains("ryzen") || name.contains("epyc") || name.matches(".*fx-9[0-9]+.*")) {
                    return true;
                }
            }
            
            // Default to false for unknown processors
            return false;
            
        } catch (Exception e) {
            LOGGER.debug("Could not determine AVX2 support", e);
            return false;
        }
    }
}