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
                    cachedHardwareInfo = detectHardwareInfo();
                }
            }
        }
        return cachedHardwareInfo;
    }

    /**
     * Clear cached hardware information
     */
    public void clearCache() {
        synchronized (this) {
            cachedHardwareInfo = null;
        }
    }

    private HardwareInfo detectHardwareInfo() {
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
            // Note: OSHI doesn't provide direct feature flag access in all versions
            // We'll use a fallback approach checking processor identifiers
            boolean hasAvx2 = detectAvx2Support(processor);
            
            // Classify hardware tier
            HardwareTier tier = HardwareTier.classify(ramGB, cpuCores, hasAvx2);
            
            HardwareInfo info = new HardwareInfo(ramGB, cpuCores, hasAvx2, tier);
            LOGGER.info("Hardware detected: {}", info);
            
            return info;
            
        } catch (Exception e) {
            LOGGER.warn("Failed to detect hardware information", e);
            return HardwareInfo.createFallback();
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