package com.beeny.villagesreborn.core.hardware;

/**
 * Hardware tier classification for optimizing LLM performance
 */
public enum HardwareTier {
    UNKNOWN("Unknown", "Hardware detection failed"),
    LOW("Low", "Basic hardware - may require smaller models"),
    MEDIUM("Medium", "Moderate hardware - can handle medium models"),
    HIGH("High", "High-end hardware - can handle large models");

    private final String displayName;
    private final String description;

    HardwareTier(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Classify hardware tier based on system specifications
     * 
     * @param ramGB RAM in gigabytes
     * @param cpuCores Number of CPU cores
     * @param hasAvx2 Whether CPU supports AVX2 instructions
     * @return Appropriate hardware tier
     */
    public static HardwareTier classify(int ramGB, int cpuCores, boolean hasAvx2) {
        // Unknown if any value is invalid
        if (ramGB <= 0 || cpuCores <= 0) {
            return UNKNOWN;
        }

        // High tier: 24GB+ RAM, 12+ cores, AVX2 support
        if (ramGB >= 24 && cpuCores >= 12 && hasAvx2) {
            return HIGH;
        }

        // Medium tier: 12GB+ RAM, 6+ cores, AVX2 support
        if (ramGB >= 12 && cpuCores >= 6 && hasAvx2) {
            return MEDIUM;
        }

        // Low tier: anything else that meets minimum requirements
        if (ramGB >= 4 && cpuCores >= 2) {
            return LOW;
        }

        // Below minimum requirements
        return UNKNOWN;
    }
}