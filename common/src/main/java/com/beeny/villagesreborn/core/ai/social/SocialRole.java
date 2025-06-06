package com.beeny.villagesreborn.core.ai.social;

/**
 * Defines the social roles villagers can take within their community
 */
public enum SocialRole {
    COMMUNITY_MEMBER(0, "A regular member of the village community"),
    ELDER(3, "Respected elder with wisdom and experience"),
    LEADER(4, "Community leader who guides village decisions"),
    MEDIATOR(2, "Helps resolve conflicts between villagers"),
    MENTOR(2, "Teaches and guides younger villagers"),
    HEALER(2, "Cares for the health and wellbeing of others"),
    CRAFTER(1, "Skilled artisan who creates important items"),
    FARMER(1, "Provides food and sustenance for the village"),
    GUARD(2, "Protects the village from threats"),
    TRADER(1, "Facilitates commerce and trade"),
    STORYTELLER(1, "Preserves and shares village history and culture"),
    OUTCAST(-1, "Rejected or isolated from the community"),
    NEWCOMER(0, "Recently arrived to the village"),
    TROUBLEMAKER(-2, "Causes problems and disrupts village harmony");
    
    private final int influenceModifier;
    private final String description;
    
    SocialRole(int influenceModifier, String description) {
        this.influenceModifier = influenceModifier;
        this.description = description;
    }
    
    public int getInfluenceModifier() { return influenceModifier; }
    public String getDescription() { return description; }
    
    /**
     * Determines if this role can transition to another role
     */
    public boolean canTransitionTo(SocialRole newRole) {
        // Some basic transition rules
        if (this == OUTCAST && newRole == LEADER) return false;
        if (this == TROUBLEMAKER && newRole == ELDER) return false;
        if (this == NEWCOMER && newRole == ELDER) return false;
        
        return true;
    }
}