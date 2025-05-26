package com.beeny.villagesreborn.platform.fabric.test;

/**
 * Test utility class for providing integer values
 */
public class IntProvider {
    private static int counter = 0;
    
    /**
     * Get the next integer value
     */
    public static synchronized int next() {
        return ++counter;
    }
    
    /**
     * Reset the counter
     */
    public static synchronized void reset() {
        counter = 0;
    }
    
    /**
     * Get a specific range integer
     */
    public static synchronized int range(int min, int max) {
        return min + (next() % (max - min + 1));
    }
}