package com.beeny.villagesreborn.core.expansion;

public class EntityAttributes {
    private final double maxHealth;
    private final double movementSpeed;
    private final boolean fireResistance;
    private final boolean teleportResistance;
    
    private EntityAttributes(Builder builder) {
        this.maxHealth = builder.maxHealth;
        this.movementSpeed = builder.movementSpeed;
        this.fireResistance = builder.fireResistance;
        this.teleportResistance = builder.teleportResistance;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public double getMaxHealth() {
        return maxHealth;
    }
    
    public double getMovementSpeed() {
        return movementSpeed;
    }
    
    public boolean hasFireResistance() {
        return fireResistance;
    }
    
    public boolean hasTeleportResistance() {
        return teleportResistance;
    }
    
    public static class Builder {
        private double maxHealth = 20.0;
        private double movementSpeed = 0.25;
        private boolean fireResistance = false;
        private boolean teleportResistance = false;
        
        public Builder maxHealth(double maxHealth) {
            this.maxHealth = maxHealth;
            return this;
        }
        
        public Builder movementSpeed(double movementSpeed) {
            this.movementSpeed = movementSpeed;
            return this;
        }
        
        public Builder fireResistance(boolean fireResistance) {
            this.fireResistance = fireResistance;
            return this;
        }
        
        public Builder teleportResistance(boolean teleportResistance) {
            this.teleportResistance = teleportResistance;
            return this;
        }
        
        public EntityAttributes build() {
            return new EntityAttributes(this);
        }
    }
}