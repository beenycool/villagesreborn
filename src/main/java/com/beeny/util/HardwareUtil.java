package com.beeny.util;

import oshi.SystemInfo;
import oshi.hardware.GraphicsCard;
import oshi.hardware.HardwareAbstractionLayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class HardwareUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn");
    private static final long BYTES_PER_GIB = 1024L * 1024L * 1024L;

    /**
     * Attempts to detect the total VRAM across detected graphics cards.
     * Prioritizes non-integrated GPUs if identifiable, otherwise sums all.
     *
     * @return Total detected VRAM in bytes, or 0 if detection fails or no GPUs found.
     */
    public static long getGpuVramBytes() {
        long totalVram = 0;
        long dedicatedVram = 0;
        boolean foundDedicated = false;

        try {
            SystemInfo si = new SystemInfo();
            HardwareAbstractionLayer hal = si.getHardware();
            List<GraphicsCard> gpuList = hal.getGraphicsCards();

            if (gpuList == null || gpuList.isEmpty()) {
                LOGGER.warn("HardwareUtil: No graphics cards detected by OSHI.");
                return 0;
            }

            LOGGER.info("HardwareUtil: Detected {} graphics card(s).", gpuList.size());

            for (GraphicsCard gpu : gpuList) {
                long vram = gpu.getVRam();
                String name = gpu.getName().toLowerCase();
                String vendor = gpu.getVendor().toLowerCase();
                LOGGER.info("HardwareUtil: Found GPU: {} ({}), VRAM: {} bytes ({} GiB)", gpu.getName(), vendor, vram, (double)vram / BYTES_PER_GIB);

                // Simple check for common integrated GPU vendors/names
                boolean isLikelyIntegrated = vendor.contains("intel") || vendor.contains("amd") && (name.contains("graphics") || name.contains("radeon vega"));

                if (!isLikelyIntegrated && vram > 0) {
                    dedicatedVram += vram;
                    foundDedicated = true;
                    LOGGER.info("HardwareUtil: GPU considered dedicated.");
                }
                totalVram += vram; // Always add to total for fallback
            }

            // Prefer sum of dedicated VRAM if any were found, otherwise use total reported VRAM
            long finalVram = foundDedicated ? dedicatedVram : totalVram;

            LOGGER.info("HardwareUtil: Final estimated VRAM: {} bytes ({} GiB). Using {} VRAM.",
                 finalVram, (double)finalVram / BYTES_PER_GIB, foundDedicated ? "dedicated" : "total");

            return finalVram;

        } catch (Throwable e) { // Catch Throwable to include NoClassDefFoundError etc.
            LOGGER.error("HardwareUtil: Failed to detect GPU VRAM using OSHI.", e);
            return 0; // Return 0 on any error
        }
    }

     /**
     * Converts bytes to GiB.
     * @param bytes Value in bytes.
     * @return Value in GiB.
     */
    public static double bytesToGiB(long bytes) {
        return (double) bytes / BYTES_PER_GIB;
    }
}