package com.beeny.village;

import com.beeny.village.event.VillagerEventBehavior;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PotionItem;
import net.minecraft.nbt.NbtCompound;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.ServerCommandSource;

import net.minecraft.entity.effect.StatusEffectInstance;
import java.util.*;
import java.util.function.Consumer;

/**
 * Represents village events such as seasonal festivals, cultural 
 * celebrations, disasters, and political activities.
 */
public class VillageEvent {
    public void giveCompletionRewards() {
        for (PlayerEntity player : participatingPlayers) {
            if (player instanceof ServerPlayerEntity serverPlayer) {
                if (culture.getType().toString().startsWith("infernal_")) {
                    if (Random.create().nextFloat() < 0.1f) { // 10% chance for permanent reward
                        serverPlayer.getServer().getCommandManager().executeWithPrefix(
                            serverPlayer.getCommandSource(),
                            "attribute @s minecraft:generic.nether_fire_resistance base set 1"
                        ); // Hypothetical custom attribute
                        serverPlayer.sendMessage(Text.of("§6You've gained Nether Affinity! Immune to fire in the Nether."), false);
                    } else {
                        // Give fire resistance potion using command
                        serverPlayer.getServer().getCommandManager().executeWithPrefix(
                            serverPlayer.getCommandSource(),
                            "give " + serverPlayer.getName().getString() + " potion{CustomPotionEffects:[{Id:12,Amplifier:0,Duration:3600}]}"
                        );
                    }
                }
            }
        }
    }
    private final String id;
    private final String name;
    private final EventType type;
    private final Culture.Season season;
    private final String description;
    private int duration; // in game ticks
    private int remainingTime;
    private BlockPos eventCenter;
    private final Map<String, Object> eventData;
    private final List<EventPhase> phases;
    private int currentPhase = 0;
    private boolean isComplete = false;
    private final List<EventReward> rewards;
    private final Set<PlayerEntity> participatingPlayers;
    private final Culture culture;
    
    /**
     * Defines the different types of village events
     */
    public enum EventType {
        SEASONAL_FESTIVAL(true, false), // Regular celebrations tied to seasons
        CULTURAL_CELEBRATION(true, false), // Culture-specific celebrations
        DISASTER(false, true), // Events that require assistance (fire, raid, etc.)
        POLITICAL(true, false), // Elections, leadership changes, etc.
        MARKET(true, false), // Special trading opportunities
        CRAFTING(true, false), // Focused on making special items
        CUSTOM(true, false); // Player-initiated events
        
        private final boolean isPositive;
        private final boolean requiresIntervention;
        
        EventType(boolean isPositive, boolean requiresIntervention) {
            this.isPositive = isPositive;
            this.requiresIntervention = requiresIntervention;
        }
        
        public boolean isPositive() { return isPositive; }
        public boolean requiresIntervention() { return requiresIntervention; }
    }
    
    /**
     * Represents a phase of an event with its own duration and actions
     */
    public static class EventPhase {
        private final String name;
        private final String description;
        private final int duration;
        private int remainingTime;
        private final Consumer<VillageEvent> startAction;
        private final Consumer<VillageEvent> tickAction;
        private final Consumer<VillageEvent> completeAction;
        
        public EventPhase(String name, String description, int duration,
                          Consumer<VillageEvent> startAction,
                          Consumer<VillageEvent> tickAction,
                          Consumer<VillageEvent> completeAction) {
            this.name = name;
            this.description = description;
            this.duration = duration;
            this.remainingTime = duration;
            this.startAction = startAction;
            this.tickAction = tickAction;
            this.completeAction = completeAction;
        }
        
        public String getName() { return name; }
        public String getDescription() { return description; }
        public int getDuration() { return duration; }
        public int getRemainingTime() { return remainingTime; }
        
        public void start(VillageEvent event) {
            if (startAction != null) {
                startAction.accept(event);
            }
        }
        
        public void tick(VillageEvent event) {
            remainingTime--;
            if (tickAction != null) {
                tickAction.accept(event);
            }
        }
        
        public void complete(VillageEvent event) {
            if (completeAction != null) {
                completeAction.accept(event);
            }
        }
        
        public boolean isComplete() {
            return remainingTime <= 0;
        }
    }
    
    /**
     * Represents rewards given to players for participating in events
     */
    public static class EventReward {
        private final String rewardType; // e.g., "item", "recipe", "reputation"
        private final String rewardId;   // e.g., item ID, recipe name
        private final int amount;
        private final Map<String, Object> rewardData;
        
        public EventReward(String rewardType, String rewardId, int amount) {
            this.rewardType = rewardType;
            this.rewardId = rewardId;
            this.amount = amount;
            this.rewardData = new HashMap<>();
        }
        
        public EventReward withData(String key, Object value) {
            rewardData.put(key, value);
            return this;
        }
        
        public String getRewardType() { return rewardType; }
        public String getRewardId() { return rewardId; }
        public int getAmount() { return amount; }
        public Map<String, Object> getRewardData() { return rewardData; }
        
        // Apply the reward to a player
        public void grantTo(PlayerEntity player) {
            // Implementation would depend on the reward type
            // For example, giving items, teaching recipes, etc.
        }
    }
    
    /**
     * Creates a new village event
     */
    private VillageEvent(String id, String name, EventType type, Culture.Season season, 
                        String description, int duration, BlockPos eventCenter, Culture culture) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.season = season;
        this.description = description;
        this.duration = duration;
        this.remainingTime = duration;
        this.eventCenter = eventCenter;
        this.eventData = new HashMap<>();
        this.phases = new ArrayList<>();
        this.rewards = new ArrayList<>();
        this.participatingPlayers = new HashSet<>();
        this.culture = culture;
    }
    
    /**
     * Builder for creating village events
     */
    public static class Builder {
        private final String id;
        private String name;
        private EventType type = EventType.CULTURAL_CELEBRATION;
        private Culture.Season season = null;
        private String description = "";
        private int duration = 24000; // Default: 1 Minecraft day
        private BlockPos eventCenter = null;
        private final List<EventPhase> phases = new ArrayList<>();
        private final List<EventReward> rewards = new ArrayList<>();
        private Culture culture;
        
        public Builder(String id) {
            this.id = id;
            this.name = id;
        }
        
        public Builder withName(String name) {
            this.name = name;
            return this;
        }
        
        public Builder withType(EventType type) {
            this.type = type;
            return this;
        }
        
        public Builder withSeason(Culture.Season season) {
            this.season = season;
            return this;
        }
        
        public Builder withDescription(String description) {
            this.description = description;
            return this;
        }
        
        public Builder withDuration(int duration) {
            this.duration = duration;
            return this;
        }
        
        public Builder withEventCenter(BlockPos eventCenter) {
            this.eventCenter = eventCenter;
            return this;
        }
        
        public Builder withPhase(EventPhase phase) {
            this.phases.add(phase);
            return this;
        }
        
        public Builder withReward(EventReward reward) {
            this.rewards.add(reward);
            return this;
        }
        
        public Builder withCulture(Culture culture) {
            this.culture = culture;
            return this;
        }
        
        public VillageEvent build() {
            VillageEvent event = new VillageEvent(id, name, type, season, description, duration, eventCenter, culture);
            
            // Add all phases
            for (EventPhase phase : phases) {
                event.addPhase(phase);
            }
            
            // Add all rewards
            for (EventReward reward : rewards) {
                event.addReward(reward);
            }
            
            return event;
        }
    }
    
    /**
     * Add a phase to this event
     */
    public void addPhase(EventPhase phase) {
        phases.add(phase);
    }
    
    /**
     * Add a reward to this event
     */
    public void addReward(EventReward reward) {
        rewards.add(reward);
    }
    
    /**
     * Start the event
     */
    public void start() {
        if (!phases.isEmpty()) {
            currentPhase = 0;
            phases.get(0).start(this);
        }
    }
    
    /**
     * Update the event state (called each tick)
     */
    public void tick() {
        if (isComplete) return;
        
        remainingTime--;
        
        // If we have phases, handle phase-specific logic
        if (!phases.isEmpty() && currentPhase < phases.size()) {
            EventPhase phase = phases.get(currentPhase);
            phase.tick(this);
            
            // Check if current phase is complete
            if (phase.isComplete()) {
                phase.complete(this);
                currentPhase++;
                
                // If there's another phase, start it
                if (currentPhase < phases.size()) {
                    phases.get(currentPhase).start(this);
                } else {
                    // All phases complete
                    complete();
                }
            }
        } else if (remainingTime <= 0) {
            // No phases or time expired
            complete();
        }
    }
    
    /**
     * Complete the event, awarding rewards to participants
     */
    public void complete() {
        if (isComplete) return;
        
        isComplete = true;
        
        // Grant rewards to all participating players
        for (PlayerEntity player : participatingPlayers) {
            for (EventReward reward : rewards) {
                reward.grantTo(player);
            }
        }
        
        // Notify players that the event has ended
        for (PlayerEntity player : participatingPlayers) {
            player.sendMessage(Text.of("The " + name + " event has concluded!"), false);
        }
    }
    
    /**
     * Add a player as a participant in this event
     */
    public void addParticipant(PlayerEntity player) {
        participatingPlayers.add(player);
        player.sendMessage(Text.of("You are now participating in the " + name + " event!"), false);
    }
    
    /**
     * Check if a player is participating in this event
     */
    public boolean isParticipating(PlayerEntity player) {
        return participatingPlayers.contains(player);
    }
    
    /**
     * Get current event status as a text description
     */
    public String getStatus() {
        if (isComplete) {
            return "Concluded";
        }
        
        if (!phases.isEmpty() && currentPhase < phases.size()) {
            EventPhase phase = phases.get(currentPhase);
            return "Phase " + (currentPhase + 1) + ": " + phase.getName();
        }
        
        return "In progress";
    }
    
    /**
     * Create a seasonal festival based on the current season and culture
     */
    public static VillageEvent createSeasonalFestival(Culture culture, Culture.Season season, BlockPos center) {
        // Get the seasonal event name from the culture
        String eventName = culture.getSeasonalEvent(season);
        
        // Base duration: 2 Minecraft days
        int duration = 48000;
        
        // Create appropriate builders based on culture and season
        Builder builder = new Builder("festival_" + culture.getType().getId() + "_" + season.name().toLowerCase())
            .withName(eventName)
            .withType(EventType.SEASONAL_FESTIVAL)
            .withSeason(season)
            .withDuration(duration)
            .withEventCenter(center)
            .withCulture(culture);
            
        String description = "";
        
        // Customize event based on culture and season
        switch(culture.getType()) {
            case ROMAN:
                if (season == Culture.Season.SUMMER) {
                    description = "A grand celebration featuring gladiatorial contests and chariot races.";
                    builder.withPhase(createFestivalPhase("Preparation", "Setting up the arena", 6000, eventName))
                           .withPhase(createFestivalPhase("Games", "Gladiatorial contests", 24000, eventName))
                           .withPhase(createFestivalPhase("Feast", "Celebratory feast", 18000, eventName));
                } else if (season == Culture.Season.WINTER) {
                    description = "A festival of gift-giving and role reversal, honoring Saturn.";
                }
                break;
                
            case MEDIEVAL:
                if (season == Culture.Season.SPRING) {
                    description = "A tournament of knights showcasing their skills in jousting and swordplay.";
                    builder.withPhase(createFestivalPhase("Herald's Call", "Announcing the tournament", 6000, eventName))
                           .withPhase(createFestivalPhase("Jousting", "Knights compete in jousting", 18000, eventName))
                           .withPhase(createFestivalPhase("Melee", "Free-for-all combat tournament", 12000, eventName))
                           .withPhase(createFestivalPhase("Awards", "Honoring the champions", 12000, eventName));
                } else if (season == Culture.Season.AUTUMN) {
                    description = "Celebrating the harvest with feasting, dancing, and games.";
                }
                break;
                
            case GREEK:
                if (season == Culture.Season.SUMMER) {
                    description = "Athletic competitions honoring Zeus, featuring races, wrestling, and discus throwing.";
                    builder.withPhase(createFestivalPhase("Opening Ceremony", "Lighting the sacred flame", 6000, eventName))
                           .withPhase(createFestivalPhase("Athletic Competitions", "Various sporting events", 24000, eventName))
                           .withPhase(createFestivalPhase("Awards", "Crowning the victors with olive wreaths", 18000, eventName));
                }
                break;
                
            case JAPANESE:
                if (season == Culture.Season.SPRING) {
                    description = "Viewing the beautiful cherry blossoms while enjoying poetry and sake.";
                    builder.withPhase(createFestivalPhase("Viewing", "Cherry blossom appreciation", 24000, eventName))
                           .withPhase(createFestivalPhase("Poetry", "Writing haiku under the trees", 12000, eventName))
                           .withPhase(createFestivalPhase("Feast", "A traditional meal", 12000, eventName));
                }
                break;
                
            case MAYAN:
                if (season == Culture.Season.SPRING) {
                    description = "Ceremonies to call for rain in preparation for the planting season.";
                } else if (season == Culture.Season.WINTER) {
                    description = "Celebrating the beginning of a new year in the calendar.";
                    builder.withPhase(createFestivalPhase("Preparations", "Setting up altars and offerings", 12000, eventName))
                           .withPhase(createFestivalPhase("Rituals", "Priests performing sacred ceremonies", 18000, eventName))
                           .withPhase(createFestivalPhase("Celebration", "Community feasting and dancing", 18000, eventName));
                }
                break;
                
            case EGYPTIAN:
                if (season == Culture.Season.SUMMER) {
                    description = "A festival honoring the sun god Ra at the height of his power.";
                }
                break;
                
            case VICTORIAN:
                if (season == Culture.Season.WINTER) {
                    description = "An elegant ball celebrating the holiday season with dancing and fine food.";
                    builder.withPhase(createFestivalPhase("Arrivals", "Guests arriving in carriages", 6000, eventName))
                           .withPhase(createFestivalPhase("Dining", "Formal dinner service", 12000, eventName))
                           .withPhase(createFestivalPhase("Dancing", "Ballroom dancing until midnight", 18000, eventName))
                           .withPhase(createFestivalPhase("Farewell", "Departure and gift exchange", 12000, eventName));
                }
                break;
                
            default:
                description = "A seasonal celebration reflecting local customs and traditions.";
                break;
        }
        
        // Add description
        builder.withDescription(description);
        
        // Add standard rewards
        builder.withReward(new EventReward("item", "festival_token", 1)
                            .withData("cultureid", culture.getType().getId()));
                            
        if (culture.isHybrid()) {
            // For hybrid cultures, add an extra reward reflecting both cultures
            builder.withReward(new EventReward("item", "cultural_exchange_token", 1)
                               .withData("primary", culture.getType().getId())
                               .withData("secondary", culture.getSecondaryType().getId()));
        }
        
        // Create the event
        return builder.build();
    }
    
    /**
     * Create a disaster event that requires player intervention
     */
    public static VillageEvent createDisasterEvent(String disasterType, BlockPos center, int severity) {
        String id = "disaster_" + disasterType.toLowerCase();
        String name;
        String description;
        int duration = 12000; // Half a Minecraft day by default
        
        Builder builder = new Builder(id)
            .withType(EventType.DISASTER)
            .withEventCenter(center);
        
        switch (disasterType.toLowerCase()) {
            case "fire":
                name = "Village Fire";
                description = "A fire has broken out in the village! Help put it out before buildings are destroyed.";
                builder.withPhase(createDisasterPhase("Initial Spread", "The fire begins to spread", 3000, name))
                       .withPhase(createDisasterPhase("Growing Blaze", "The fire intensifies", 6000, name))
                       .withPhase(createDisasterPhase("Critical Point", "Structures are at risk of collapse", 3000, name));
                break;
                
            case "raid":
                name = "Hostile Raid";
                description = "The village is under attack! Defend the villagers from hostile mobs.";
                builder.withPhase(createDisasterPhase("Warning", "Scouts spot approaching enemies", 3000, name))
                       .withPhase(createDisasterPhase("Attack", "Enemies raid the village", 6000, name))
                       .withPhase(createDisasterPhase("Last Stand", "The final wave of attackers", 3000, name));
                break;
                
            case "disease":
                name = "Village Illness";
                description = "A mysterious illness is spreading among villagers. Help them find a cure!";
                duration = 24000; // Full Minecraft day - diseases take longer to resolve
                builder.withPhase(createDisasterPhase("Outbreak", "Initial cases appear", 6000, name))
                       .withPhase(createDisasterPhase("Spreading", "The illness spreads to more villagers", 12000, name))
                       .withPhase(createDisasterPhase("Treatment", "Administering the cure", 6000, name));
                break;
                
            case "drought":
                name = "Severe Drought";
                description = "Crops are failing due to lack of water. Help the village survive!";
                duration = 36000; // 1.5 Minecraft days
                builder.withPhase(createDisasterPhase("Early Signs", "Crops begin to wilt", 12000, name))
                       .withPhase(createDisasterPhase("Crisis", "Food shortages begin", 18000, name))
                       .withPhase(createDisasterPhase("Recovery", "Implementing irrigation solutions", 6000, name));
                break;
                
            default:
                name = "Village Crisis";
                description = "The village needs your help with an urgent matter!";
                break;
        }
        
        // Adjust duration and description based on severity
        duration = (int)(duration * (0.5 + (severity * 0.25)));
        if (severity > 2) {
            description = "SEVERE: " + description;
        }
        
        // Finalize builder
        builder.withName(name)
               .withDescription(description)
               .withDuration(duration);
        
        // Add rewards for resolving the disaster
        builder.withReward(new EventReward("reputation", "village_hero", 10))
               .withReward(new EventReward("item", "village_thanks_token", 1));
        
        return builder.build();
    }
    
    /**
     * Create a political event like an election
     */
    public static VillageEvent createPoliticalEvent(String eventType, Culture culture, BlockPos center) {
        String id = "political_" + eventType.toLowerCase();
        String name;
        String description;
        int duration = 24000; // Default: 1 Minecraft day
        
        Builder builder = new Builder(id)
            .withType(EventType.POLITICAL)
            .withEventCenter(center)
            .withCulture(culture);
        
        switch (eventType.toLowerCase()) {
            case "election":
                name = "Village Leadership Election";
                
                // Customize based on culture
                if (culture.getType() == Culture.CultureType.ROMAN) {
                    name = "Senate Election";
                    description = "Citizens are voting for new senators. Your influence could sway the outcome!";
                } else if (culture.getType() == Culture.CultureType.MEDIEVAL) {
                    name = "Council Selection";
                    description = "A new village council is being selected. Help choose who will lead!";
                } else if (culture.getType() == Culture.CultureType.GREEK) {
                    name = "Democratic Assembly";
                    description = "Citizens gather to vote on new leaders and important issues.";
                } else {
                    description = "The village is selecting new leadership. Your participation matters!";
                }
                
                builder.withPhase(createPoliticalPhase("Nominations", "Candidates are being nominated", 6000, name))
                       .withPhase(createPoliticalPhase("Campaigning", "Candidates present their platforms", 12000, name))
                       .withPhase(createPoliticalPhase("Voting", "Citizens cast their votes", 6000, name))
                       .withPhase(createPoliticalPhase("Results", "Announcing the winners", 2000, name));
                break;
                
            case "treaty":
                name = "Diplomatic Treaty";
                description = "Representatives from another village have arrived to negotiate a treaty.";
                
                builder.withPhase(createPoliticalPhase("Arrival", "Diplomats arrive for negotiations", 6000, name))
                       .withPhase(createPoliticalPhase("Negotiations", "Terms are being discussed", 12000, name))
                       .withPhase(createPoliticalPhase("Signing", "The treaty is signed", 6000, name));
                break;
                
            case "coronation":
                if (culture.getType() == Culture.CultureType.MEDIEVAL) {
                    name = "Lord's Investiture";
                    description = "A new lord is being formally recognized as the village leader.";
                } else if (culture.getType() == Culture.CultureType.JAPANESE) {
                    name = "Daimyo Succession";
                    description = "A ceremony to install the new leader of the village.";
                } else {
                    name = "Leadership Ceremony";
                    description = "A formal ceremony to recognize the new village leader.";
                }
                
                builder.withPhase(createPoliticalPhase("Preparations", "Setting up for the ceremony", 6000, name))
                       .withPhase(createPoliticalPhase("Procession", "The formal procession of dignitaries", 6000, name))
                       .withPhase(createPoliticalPhase("Ceremony", "The investiture ceremony", 6000, name))
                       .withPhase(createPoliticalPhase("Celebration", "Feasting and celebration", 12000, name));
                break;
                
            default:
                name = "Political Gathering";
                description = "A political event is taking place in the village.";
                break;
        }
        
        // Finalize builder
        builder.withName(name)
               .withDescription(description)
               .withDuration(duration);
        
        // Add rewards for participation
        builder.withReward(new EventReward("reputation", "political_influence", 5))
               .withReward(new EventReward("item", "political_favor", 1));
        
        return builder.build();
    }
    
    /**
     * Helper method to create a festival event phase with proper behavior handlers
     */
    private static EventPhase createFestivalPhase(String name, String description, int duration, String eventName) {
        // Create behavior handlers for this phase
        Consumer<VillageEvent> startAction = VillagerEventBehavior.createFestivalBehavior(eventName, name);
        Consumer<VillageEvent> tickAction = VillagerEventBehavior.createPhaseTicker("FESTIVAL");
        
        // Determine the next phase for completion messaging
        Consumer<VillageEvent> completeAction = VillagerEventBehavior.createPhaseCompleter(name);
        
        return new EventPhase(name, description, duration, startAction, tickAction, completeAction);
    }
    
    /**
     * Helper method to create a disaster event phase with proper behavior handlers
     */
    private static EventPhase createDisasterPhase(String name, String description, int duration, String eventName) {
        // Create behavior handlers for this phase
        Consumer<VillageEvent> startAction = VillagerEventBehavior.createDisasterBehavior(eventName, name);
        Consumer<VillageEvent> tickAction = VillagerEventBehavior.createPhaseTicker("DISASTER");
        Consumer<VillageEvent> completeAction = VillagerEventBehavior.createPhaseCompleter(name);
        
        return new EventPhase(name, description, duration, startAction, tickAction, completeAction);
    }
    
    /**
     * Helper method to create a political event phase with proper behavior handlers
     */
    private static EventPhase createPoliticalPhase(String name, String description, int duration, String eventName) {
        // Create behavior handlers for this phase
        Consumer<VillageEvent> startAction = VillagerEventBehavior.createPoliticalBehavior(eventName, name);
        Consumer<VillageEvent> tickAction = VillagerEventBehavior.createPhaseTicker("POLITICAL");
        Consumer<VillageEvent> completeAction = VillagerEventBehavior.createPhaseCompleter(name);
        
        return new EventPhase(name, description, duration, startAction, tickAction, completeAction);
    }
    
    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public EventType getType() { return type; }
    public Culture.Season getSeason() { return season; }
    public String getDescription() { return description; }
    public int getDuration() { return duration; }
    public int getRemainingTime() { return remainingTime; }
    public BlockPos getEventCenter() { return eventCenter; }
    public boolean isComplete() { return isComplete; }
    public Culture getCulture() { return culture; }
    public Set<PlayerEntity> getParticipatingPlayers() { return participatingPlayers; }
    
    /**
     * Get the current phase if one exists
     */
    public EventPhase getCurrentPhase() {
        if (!phases.isEmpty() && currentPhase < phases.size()) {
            return phases.get(currentPhase);
        }
        return null;
    }
}
