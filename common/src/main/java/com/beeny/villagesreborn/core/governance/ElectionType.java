package com.beeny.villagesreborn.core.governance;

/**
 * Types of elections that can be held in villages
 */
public enum ElectionType {
    MAYOR("Village Mayor Election"),
    COUNCIL("Village Council Election"),
    SHERIFF("Village Sheriff Election"),
    TRADE_LEADER("Trade Guild Leader Election"),
    EMERGENCY_LEADER("Emergency Leadership Election");
    
    private final String displayName;
    
    ElectionType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}