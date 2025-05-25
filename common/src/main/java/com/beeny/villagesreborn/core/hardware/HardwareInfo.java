package com.beeny.villagesreborn.core.hardware;

/**
 * Immutable data class representing system hardware information
 * Used for hardware tier classification and LLM optimization
 */
public class HardwareInfo {
    private final int ramGB;
    private final int cpuCores;
    private final boolean hasAvx2Support;
    private final HardwareTier hardwareTier;

    public HardwareInfo(int ramGB, int cpuCores, boolean hasAvx2Support, HardwareTier hardwareTier) {
        this.ramGB = ramGB;
        this.cpuCores = cpuCores;
        this.hasAvx2Support = hasAvx2Support;
        this.hardwareTier = hardwareTier;
    }

    /**
     * Create fallback hardware info for when detection fails
     */
    public static HardwareInfo createFallback() {
        return new HardwareInfo(0, 0, false, HardwareTier.UNKNOWN);
    }

    public int getRamGB() {
        return ramGB;
    }

    public int getCpuCores() {
        return cpuCores;
    }

    public boolean hasAvx2Support() {
        return hasAvx2Support;
    }

    public HardwareTier getHardwareTier() {
        return hardwareTier;
    }

    @Override
    public String toString() {
        return String.format("HardwareInfo{RAM=%dGB, CPU=%d cores, AVX2=%s, tier=%s}", 
            ramGB, cpuCores, hasAvx2Support, hardwareTier);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        HardwareInfo that = (HardwareInfo) obj;
        return ramGB == that.ramGB &&
               cpuCores == that.cpuCores &&
               hasAvx2Support == that.hasAvx2Support &&
               hardwareTier == that.hardwareTier;
    }

    @Override
    public int hashCode() {
        int result = ramGB;
        result = 31 * result + cpuCores;
        result = 31 * result + (hasAvx2Support ? 1 : 0);
        result = 31 * result + (hardwareTier != null ? hardwareTier.hashCode() : 0);
        return result;
    }
}