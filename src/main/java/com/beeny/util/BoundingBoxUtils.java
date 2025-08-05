package com.beeny.util;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * Utility class providing null-safe operations on {@link Box} for villager/entity management.
 * <p>
 * All methods are static and null-safe:
 * - Predicate methods return false when any input is null.
 * - Transformer/creator methods return the original box (if applicable) or null when inputs are insufficient.
 * </p>
 */
public final class BoundingBoxUtils {

    private BoundingBoxUtils() {
        // No instances
    }

    /**
     * Checks whether two boxes intersect (volumetric intersection).
     *
     * @param a first box (nullable)
     * @param b second box (nullable)
     * @return true if both non-null and they intersect, otherwise false
     */
    public static boolean intersects(Box a, Box b) {
        if (a == null || b == null) return false;
        return a.intersects(b);
    }

    /**
     * Checks if {@code outer} fully contains {@code inner}.
     *
     * @param outer the potential containing box (nullable)
     * @param inner the potential contained box (nullable)
     * @return true if both non-null and all inner corners lie within outer, otherwise false
     */
    public static boolean contains(Box outer, Box inner) {
        if (outer == null || inner == null) return false;
        return outer.minX <= inner.minX && outer.minY <= inner.minY && outer.minZ <= inner.minZ
            && outer.maxX >= inner.maxX && outer.maxY >= inner.maxY && outer.maxZ >= inner.maxZ;
    }

    /**
     * Checks if a point lies within a box (inclusive of faces).
     *
     * @param box the box (nullable)
     * @param x point x
     * @param y point y
     * @param z point z
     * @return true if box non-null and point is inside or on the faces, otherwise false
     */
    public static boolean containsPoint(Box box, double x, double y, double z) {
        if (box == null) return false;
        return x >= box.minX && x <= box.maxX
            && y >= box.minY && y <= box.maxY
            && z >= box.minZ && z <= box.maxZ;
    }

    /**
     * Expands the box by the given deltas on the positive side (max) if deltas are positive,
     * or on the negative side (min) if deltas are negative, following {@link Box#expand(double, double, double)} semantics.
     *
     * @param box the original box (nullable)
     * @param dx expansion along X
     * @param dy expansion along Y
     * @param dz expansion along Z
     * @return a new expanded box; returns {@code box} if box is null
     */
    public static Box expand(Box box, double dx, double dy, double dz) {
        if (box == null) return null;
        return box.expand(dx, dy, dz);
    }

    /**
     * Inflates the box uniformly in all directions by {@code amount}.
     * Effectively expands by (+/-amount) on each axis around both min and max.
     *
     * @param box the original box (nullable)
     * @param amount non-negative amount to inflate (negative amounts will be clamped to 0)
     * @return a new inflated box; returns {@code box} if box is null
     */
    public static Box inflate(Box box, double amount) {
        if (box == null) return null;
        double a = Math.max(0.0, amount);
        return new Box(
            box.minX - a, box.minY - a, box.minZ - a,
            box.maxX + a, box.maxY + a, box.maxZ + a
        );
    }

    /**
     * Translates (moves) the box by the given deltas.
     *
     * @param box the original box (nullable)
     * @param dx delta x
     * @param dy delta y
     * @param dz delta z
     * @return a new translated box; returns {@code box} if box is null
     */
    public static Box translate(Box box, double dx, double dy, double dz) {
        if (box == null) return null;
        return box.offset(dx, dy, dz);
    }

    /**
     * Returns the union (minimal enclosing box) of two boxes.
     *
     * @param a first box (nullable)
     * @param b second box (nullable)
     * @return a new box enclosing both; returns the non-null input if one is null; returns null if both are null
     */
    public static Box union(Box a, Box b) {
        if (a == null && b == null) return null;
        if (a == null) return b;
        if (b == null) return a;
        double minX = Math.min(a.minX, b.minX);
        double minY = Math.min(a.minY, b.minY);
        double minZ = Math.min(a.minZ, b.minZ);
        double maxX = Math.max(a.maxX, b.maxX);
        double maxY = Math.max(a.maxY, b.maxY);
        double maxZ = Math.max(a.maxZ, b.maxZ);
        return new Box(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /**
     * Computes the volume of a box.
     *
     * @param box the box (nullable)
     * @return the volume if box non-null; otherwise 0.0
     */
    public static double volume(Box box) {
        if (box == null) return 0.0;
        double dx = Math.max(0.0, box.maxX - box.minX);
        double dy = Math.max(0.0, box.maxY - box.minY);
        double dz = Math.max(0.0, box.maxZ - box.minZ);
        return dx * dy * dz;
    }

    /**
     * Creates an axis-aligned box from center and half extents.
     *
     * @param cx center x
     * @param cy center y
     * @param cz center z
     * @param hx half extent x (clamped to >= 0)
     * @param hy half extent y (clamped to >= 0)
     * @param hz half extent z (clamped to >= 0)
     * @return a new box
     */
    public static Box fromCenterAndHalfExtents(double cx, double cy, double cz, double hx, double hy, double hz) {
        double x = Math.max(0.0, hx);
        double y = Math.max(0.0, hy);
        double z = Math.max(0.0, hz);
        return new Box(cx - x, cy - y, cz - z, cx + x, cy + y, cz + z);
    }

    /**
     * Creates an axis-aligned box from two corners, null-safe.
     *
     * @param a first corner (nullable)
     * @param b second corner (nullable)
     * @return a new box or null if either corner is null
     */
    public static Box fromCorners(Vec3d a, Vec3d b) {
        if (a == null || b == null) return null;
        return new Box(a, b);
    }

    /**
     * Creates a box from block positions (inclusive min to inclusive max corners).
     *
     * @param a first block position (nullable)
     * @param b second block position (nullable)
     * @return a new box or null if either position is null
     */
    public static Box fromBlocks(BlockPos a, BlockPos b) {
        if (a == null || b == null) return null;
        int minX = Math.min(a.getX(), b.getX());
        int minY = Math.min(a.getY(), b.getY());
        int minZ = Math.min(a.getZ(), b.getZ());
        int maxX = Math.max(a.getX(), b.getX());
        int maxY = Math.max(a.getY(), b.getY());
        int maxZ = Math.max(a.getZ(), b.getZ());
        // Box constructor uses [min, max] with doubles; for block-aligned, add 1 to max to cover full blocks if needed by caller.
        return new Box(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
    }

    /**
     * Clamps a box to world-like bounds (e.g., Y: 0..255/319 depending on version).
     *
     * @param box the original box (nullable)
     * @param minX world min X
     * @param minY world min Y
     * @param minZ world min Z
     * @param maxX world max X
     * @param maxY world max Y
     * @param maxZ world max Z
     * @return a clamped box; returns {@code box} if box is null
     */
    public static Box clampToWorld(Box box, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        if (box == null) return null;
        double cMinX = clamp(box.minX, minX, maxX);
        double cMinY = clamp(box.minY, minY, maxY);
        double cMinZ = clamp(box.minZ, minZ, maxZ);
        double cMaxX = clamp(box.maxX, minX, maxX);
        double cMaxY = clamp(box.maxY, minY, maxY);
        double cMaxZ = clamp(box.maxZ, minZ, maxZ);
        // Ensure not inverted
        double nMinX = Math.min(cMinX, cMaxX);
        double nMinY = Math.min(cMinY, cMaxY);
        double nMinZ = Math.min(cMinZ, cMaxZ);
        double nMaxX = Math.max(cMinX, cMaxX);
        double nMaxY = Math.max(cMinY, cMaxY);
        double nMaxZ = Math.max(cMinZ, cMaxZ);
        return new Box(nMinX, nMinY, nMinZ, nMaxX, nMaxY, nMaxZ);
    }

    /**
     * Shrinks the box to ensure it fits entirely within {@code bounds}. If the box lies completely
     * outside, returns an empty intersection box with zero or near-zero volume along the outside edge.
     *
     * @param box the original box (nullable)
     * @param bounds the containing bounds (nullable)
     * @return a new box clamped within bounds; null if either input is null
     */
    public static Box clampToBounds(Box box, Box bounds) {
        if (box == null || bounds == null) return null;
        double minX = Math.max(box.minX, bounds.minX);
        double minY = Math.max(box.minY, bounds.minY);
        double minZ = Math.max(box.minZ, bounds.minZ);
        double maxX = Math.min(box.maxX, bounds.maxX);
        double maxY = Math.min(box.maxY, bounds.maxY);
        double maxZ = Math.min(box.maxZ, bounds.maxZ);
        // Not forcing non-inversion; if inverted, Box will still be constructed, representing empty area.
        return new Box(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /**
     * Returns the center of the box as a {@link Vec3d}.
     *
     * @param box the box (nullable)
     * @return center vector, or null if box is null
     */
    public static Vec3d center(Box box) {
        if (box == null) return null;
        return new Vec3d(
            (box.minX + box.maxX) * 0.5,
            (box.minY + box.maxY) * 0.5,
            (box.minZ + box.maxZ) * 0.5
        );
    }

    /**
     * Returns true if the box has a non-zero volume (all edge lengths > 0).
     *
     * @param box the box (nullable)
     * @return true if non-null and strictly positive volume
     */
    public static boolean hasVolume(Box box) {
        if (box == null) return false;
        return (box.maxX - box.minX) > 0 && (box.maxY - box.minY) > 0 && (box.maxZ - box.minZ) > 0;
    }

    private static double clamp(double v, double min, double max) {
        if (v < min) return min;
        if (v > max) return max;
        return v;
    }
}