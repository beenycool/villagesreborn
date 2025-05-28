package com.beeny.villagesreborn.platform.fabric.testing;

/**
 * Test utility class for providing integer values in tests
 * This resolves the NoClassDefFoundError issue in test initialization
 */
public class IntProvider {
    private static int value = 0;
    
    public static int getValue() {
        return value;
    }
    
    public static void setValue(int newValue) {
        value = newValue;
    }
    
    public static void reset() {
        value = 0;
    }
}