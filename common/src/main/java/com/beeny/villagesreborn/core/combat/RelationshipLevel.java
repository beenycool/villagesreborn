package com.beeny.villagesreborn.core.combat;

/**
 * Enumeration of relationship levels between entities
 */
public enum RelationshipLevel {
    ENEMY(-1.0f),
    STRANGER(0.0f),
    ACQUAINTANCE(0.25f),
    FRIEND(0.5f),
    CLOSE_FRIEND(0.75f),
    FAMILY(1.0f);
    
    private final float value;
    
    RelationshipLevel(float value) {
        this.value = value;
    }
    
    public float getValue() {
        return value;
    }
}