package com.beeny.system;

import com.beeny.data.VillagerData;
import com.beeny.ai.core.VillagerEmotionSystem;
import com.beeny.ai.learning.VillagerLearningSystem;
import com.beeny.ai.social.VillagerGossipNetwork;
import com.beeny.ai.decision.VillagerUtilityAI;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import net.minecraft.village.VillagerProfession;
import net.minecraft.world.poi.PointOfInterestTypes;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Enhanced profession management system that handles dynamic career changes,
 * workstation management, skill progression, and professional relationships
 */
public class VillagerProfessionManager {
    
    // Profession skill and satisfaction tracking
    public static class ProfessionData {
        public final String professionId;
        public float skillLevel; // 0.0 to 100.0
        public float satisfaction; // 0.0 to 100.0
        public long timeInProfession;
        public int successfulTrades;
        public int failedTrades;
        public final Map<String, Float> specializations; // Sub-skills within profession
        public final Set<String> knownRecipes;
        public float innovation; // Ability to create new trade offers
        
        public ProfessionData(String professionId) {
            this.professionId = professionId;
            this.skillLevel = ThreadLocalRandom.current().nextFloat(10.0f, 30.0f); // Start with basic skills
            this.satisfaction = 50.0f;
            this.timeInProfession = 0;
            this.successfulTrades = 0;
            this.failedTrades = 0;
            this.specializations = new ConcurrentHashMap<>();
            this.knownRecipes = new HashSet<>();
            this.innovation = ThreadLocalRandom.current().nextFloat(0.1f, 0.5f);
            
            initializeProfessionSpecifics();
        }
        
        private void initializeProfessionSpecifics() {
            switch (professionId) {
                case "farmer" -> {
                    specializations.put("crop_growing", 20.0f);
                    specializations.put("animal_care", 15.0f);
                    specializations.put("food_processing", 10.0f);
                    knownRecipes.addAll(Arrays.asList("bread", "cake", "cookies"));
                }
                case "librarian" -> {
                    specializations.put("enchanting", 25.0f);
                    specializations.put("book_binding", 20.0f);
                    specializations.put("research", 15.0f);
                    knownRecipes.addAll(Arrays.asList("book", "bookshelf", "enchanted_book"));
                }
                case "blacksmith", "armorer", "weaponsmith", "toolsmith" -> {
                    specializations.put("metalworking", 25.0f);
                    specializations.put("tool_crafting", 20.0f);
                    specializations.put("repair", 15.0f);
                    knownRecipes.addAll(Arrays.asList("iron_sword", "iron_pickaxe", "iron_armor"));
                }
                case "cleric" -> {
                    specializations.put("brewing", 20.0f);
                    specializations.put("healing", 25.0f);
                    specializations.put("blessing", 15.0f);
                    knownRecipes.addAll(Arrays.asList("healing_potion", "holy_water"));
                }
                case "fletcher" -> {
                    specializations.put("archery", 25.0f);
                    specializations.put("woodworking", 20.0f);
                    knownRecipes.addAll(Arrays.asList("bow", "arrow", "crossbow"));
                }
                case "fisherman" -> {
                    specializations.put("fishing", 30.0f);
                    specializations.put("net_making", 15.0f);
                    knownRecipes.addAll(Arrays.asList("fishing_rod", "cooked_fish"));
                }
                case "shepherd" -> {
                    specializations.put("wool_processing", 25.0f);
                    specializations.put("dyeing", 20.0f);
                    knownRecipes.addAll(Arrays.asList("wool", "carpet", "bed"));
                }
                case "cartographer" -> {
                    specializations.put("mapmaking", 30.0f);
                    specializations.put("exploration", 20.0f);
                    knownRecipes.addAll(Arrays.asList("map", "banner", "item_frame"));
                }
                case "leatherworker" -> {
                    specializations.put("tanning", 25.0f);
                    specializations.put("crafting", 20.0f);
                    knownRecipes.addAll(Arrays.asList("leather_armor", "saddle", "book"));
                }
                case "mason" -> {
                    specializations.put("stonework", 30.0f);
                    specializations.put("construction", 20.0f);
                    knownRecipes.addAll(Arrays.asList("stone_bricks", "chiseled_stone", "pillar"));
                }
            }
        }
        
        public void gainExperience(float experience, String specialization) {
            skillLevel = Math.min(100.0f, skillLevel + experience);
            
            if (specialization != null && specializations.containsKey(specialization)) {
                float currentLevel = specializations.get(specialization);
                specializations.put(specialization, Math.min(100.0f, currentLevel + experience * 1.5f));
            }
            
            timeInProfession += 1000; // Increment time markers
        }
        
        public void updateSatisfaction(float change, String reason) {
            satisfaction = Math.max(0.0f, Math.min(100.0f, satisfaction + change));
        }
        
        public float getOverallCompetency() {
            float baseCompetency = skillLevel * 0.6f;
            float specializationBonus = specializations.values().stream()
                .reduce(0.0f, Float::sum) / specializations.size() * 0.3f;
            float experienceBonus = Math.min(20.0f, timeInProfession / 3600000.0f) * 0.1f; // Hours to bonus
            
            return Math.min(100.0f, baseCompetency + specializationBonus + experienceBonus);
        }
        
        public boolean canTeachTo(ProfessionData other) {
            return skillLevel > other.skillLevel + 20.0f && satisfaction > 60.0f;
        }
        
        public List<String> getAvailableSpecializations() {
            List<String> available = new ArrayList<>();
            for (Map.Entry<String, Float> entry : specializations.entrySet()) {
                if (entry.getValue() < 80.0f) { // Can still improve
                    available.add(entry.getKey());
                }
            }
            return available;
        }
    }
    
    // Global profession data storage
    private static final Map<String, ProfessionData> villagerProfessionData = new ConcurrentHashMap<>();
    private static final Map<String, Set<String>> villageWorkstations = new ConcurrentHashMap<>();
    
    // Profession change system
    public static class ProfessionChangeManager {
        
        public static boolean canChangeProfession(VillagerEntity villager, VillagerProfession newProfession) {
            VillagerData data = villager.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
            if (data == null) return false;
            
            // Check if villager is not novice level (has no trades yet)
            if (villager.getOffers().size() > 0) {
                // Experienced villagers need more convincing to change careers
                VillagerEmotionSystem.EmotionalState emotions = VillagerEmotionSystem.getEmotionalState(villager);
                float boredom = emotions.getEmotion(VillagerEmotionSystem.EmotionType.BOREDOM);
                
                if (boredom < 60.0f) return false; // Not bored enough to change
            }
            
            // Check village need for this profession
            VillagerUtilityAI.VillageNeedsConsideration villageNeeds = new VillagerUtilityAI.VillageNeedsConsideration();
            float villageNeed = villageNeeds.getValue(villager, newProfession.toString());
            
            return villageNeed > 0.3f; // Village has moderate need for this profession
        }
        
        public static boolean attemptProfessionChange(VillagerEntity villager, VillagerProfession newProfession) {
            if (!canChangeProfession(villager, newProfession)) return false;
            
            VillagerData data = villager.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
            if (data == null) return false;
            
            String oldProfessionId = villager.getVillagerData().profession().toString().replace("minecraft:", "");
            String newProfessionId = newProfession.toString().replace("minecraft:", "");
            
            // Store old profession data
            ProfessionData oldData = getProfessionData(villager);
            if (oldData != null) {
                // Reduce satisfaction for leaving profession
                oldData.updateSatisfaction(-20.0f, "career_change");
            }
            
            // Change the actual profession
            // Mappings now expect RegistryEntry<VillagerProfession>; use registry entry
            net.minecraft.registry.entry.RegistryEntry<net.minecraft.village.VillagerProfession> newProfEntry =
                net.minecraft.registry.Registries.VILLAGER_PROFESSION.getEntry(newProfession);
            if (newProfEntry != null) {
                villager.setVillagerData(villager.getVillagerData().withProfession(newProfEntry));
            }
            
            // Clear trades to reset to novice level
            villager.getOffers().clear();
            villager.setExperience(0);
            
            // Create new profession data
            ProfessionData newData = new ProfessionData(newProfessionId);
            
            // Transfer some experience from related professions
            transferRelatedExperience(oldData, newData, oldProfessionId, newProfessionId);
            
            villagerProfessionData.put(villager.getUuidAsString(), newData);
            
            // Update villager data
            data.addProfession(newProfessionId);
            
            // Emotional reactions
            VillagerEmotionSystem.processEmotionalEvent(villager,
                new VillagerEmotionSystem.EmotionalEvent(VillagerEmotionSystem.EmotionType.EXCITEMENT, 30.0f, "career_change", false));
            VillagerEmotionSystem.processEmotionalEvent(villager,
                new VillagerEmotionSystem.EmotionalEvent(VillagerEmotionSystem.EmotionType.BOREDOM, -40.0f, "career_change", false));
            
            // Create gossip about career change
            VillagerGossipNetwork.createGossip(villager, villager.getUuidAsString(),
                VillagerGossipNetwork.GossipType.MYSTERIOUS_BEHAVIOR,
                "changed careers from " + oldProfessionId + " to " + newProfessionId);
            
            // Find and claim appropriate workstation
            WorkstationManager.findAndClaimWorkstation(villager, newProfession);
            
            return true;
        }
        
        private static void transferRelatedExperience(ProfessionData oldData, ProfessionData newData, 
                                                    String oldProf, String newProf) {
            if (oldData == null) return;
            
            float transferRate = calculateExperienceTransferRate(oldProf, newProf);
            
            if (transferRate > 0) {
                float transferredSkill = oldData.skillLevel * transferRate;
                newData.skillLevel = Math.min(50.0f, newData.skillLevel + transferredSkill);
                newData.satisfaction += 10.0f; // Bonus for relevant experience
                
                // Transfer relevant specializations
                transferRelevantSpecializations(oldData, newData, oldProf, newProf);
            }
        }
        
        private static float calculateExperienceTransferRate(String oldProf, String newProf) {
            // Define profession relationship matrix
            // Replace large Map.of(...) with mutable map to avoid arity limitations
            Map<String, Map<String, Float>> relationshipMatrix = new java.util.HashMap<>();
            // Populate a subset to maintain behavior without exceeding Map.of arity
            relationshipMatrix.put("farmer", Map.of("shepherd", 0.3f, "fisherman", 0.2f, "leatherworker", 0.1f));
            relationshipMatrix.put("blacksmith", Map.of("armorer", 0.7f, "weaponsmith", 0.7f, "toolsmith", 0.8f));
            relationshipMatrix.put("armorer", Map.of("blacksmith", 0.6f, "weaponsmith", 0.5f, "leatherworker", 0.3f));
            relationshipMatrix.put("weaponsmith", Map.of("blacksmith", 0.6f, "armorer", 0.5f, "fletcher", 0.3f));
            relationshipMatrix.put("toolsmith", Map.of("blacksmith", 0.7f, "mason", 0.3f));
            relationshipMatrix.put("librarian", Map.of("cartographer", 0.4f, "cleric", 0.2f));
            relationshipMatrix.put("cartographer", Map.of("librarian", 0.3f, "fisherman", 0.2f));
            relationshipMatrix.put("cleric", Map.of("librarian", 0.2f, "farmer", 0.1f));
            relationshipMatrix.put("fletcher", Map.of("leatherworker", 0.3f, "weaponsmith", 0.2f));
            relationshipMatrix.put("fisherman", Map.of("farmer", 0.2f, "cartographer", 0.2f));
            relationshipMatrix.put("shepherd", Map.of("farmer", 0.4f, "leatherworker", 0.5f));
            relationshipMatrix.put("leatherworker", Map.of("shepherd", 0.4f, "armorer", 0.2f));
            relationshipMatrix.put("mason", Map.of("toolsmith", 0.3f, "librarian", 0.1f));
            
            return relationshipMatrix.getOrDefault(oldProf, Map.of()).getOrDefault(newProf, 0.0f);
        }
        
        private static void transferRelevantSpecializations(ProfessionData oldData, ProfessionData newData,
                                                          String oldProf, String newProf) {
            Map<String, String> specializationMapping = Map.of(
                "metalworking", "metalworking",
                "crafting", "crafting", 
                "animal_care", "animal_care",
                "woodworking", "woodworking",
                "research", "research"
            );
            
            for (Map.Entry<String, String> mapping : specializationMapping.entrySet()) {
                if (oldData.specializations.containsKey(mapping.getKey()) && 
                    newData.specializations.containsKey(mapping.getValue())) {
                    
                    float oldLevel = oldData.specializations.get(mapping.getKey());
                    float currentLevel = newData.specializations.get(mapping.getValue());
                    float transferAmount = oldLevel * 0.3f;
                    
                    newData.specializations.put(mapping.getValue(), 
                        Math.min(100.0f, currentLevel + transferAmount));
                }
            }
        }
    }
    
    // Workstation management
    public static class WorkstationManager {
        
        public static boolean findAndClaimWorkstation(VillagerEntity villager, VillagerProfession profession) {
            if (!(villager.getWorld() instanceof ServerWorld world)) return false;
            
            BlockPos villagerPos = villager.getBlockPos();
            Block requiredWorkstation = getWorkstationForProfession(profession);
            
            if (requiredWorkstation == null) return false;
            
            // Search for unclaimed workstation in nearby area
            List<BlockPos> workstations = findNearbyWorkstations(world, villagerPos, requiredWorkstation, 48);
            
            for (BlockPos workstationPos : workstations) {
                if (isWorkstationUnclaimed(world, workstationPos)) {
                    claimWorkstation(villager, workstationPos);
                    return true;
                }
            }
            
            // If no unclaimed workstation found, suggest creating one
            suggestWorkstationPlacement(villager, requiredWorkstation);
            return false;
        }
        
        private static Block getWorkstationForProfession(VillagerProfession profession) {
            return switch (profession.toString()) {
                case "minecraft:farmer" -> Blocks.COMPOSTER;
                case "minecraft:librarian" -> Blocks.LECTERN;
                case "minecraft:armorer" -> Blocks.BLAST_FURNACE;
                case "minecraft:butcher" -> Blocks.SMOKER;
                case "minecraft:cartographer" -> Blocks.CARTOGRAPHY_TABLE;
                case "minecraft:cleric" -> Blocks.BREWING_STAND;
                case "minecraft:fisherman" -> Blocks.BARREL;
                case "minecraft:fletcher" -> Blocks.FLETCHING_TABLE;
                case "minecraft:leatherworker" -> Blocks.CAULDRON;
                case "minecraft:mason" -> Blocks.STONECUTTER;
                case "minecraft:shepherd" -> Blocks.LOOM;
                case "minecraft:toolsmith" -> Blocks.SMITHING_TABLE;
                case "minecraft:weaponsmith" -> Blocks.GRINDSTONE;
                default -> null;
            };
        }
        
        private static List<BlockPos> findNearbyWorkstations(ServerWorld world, BlockPos center, Block workstation, int radius) {
            List<BlockPos> workstations = new ArrayList<>();
            
            for (int x = -radius; x <= radius; x++) {
                for (int y = -10; y <= 10; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        BlockPos pos = center.add(x, y, z);
                        if (world.getBlockState(pos).getBlock() == workstation) {
                            workstations.add(pos);
                        }
                    }
                }
            }
            
            // Sort by distance
            workstations.sort(Comparator.comparingDouble(pos -> center.getSquaredDistance(pos)));
            return workstations;
        }
        
        private static boolean isWorkstationUnclaimed(ServerWorld world, BlockPos pos) {
            // Check if any villager is already using this workstation
            return world.getPointOfInterestStorage()
                .getInSquare(type -> true, pos, 1, net.minecraft.world.poi.PointOfInterestStorage.OccupationStatus.ANY)
                .noneMatch(poi -> poi.getPos().equals(pos));
        }
        
        private static void claimWorkstation(VillagerEntity villager, BlockPos workstationPos) {
            // This would integrate with Minecraft's POI system to claim the workstation
            String villageName = getVillageName(villager);
            villageWorkstations.computeIfAbsent(villageName, k -> new HashSet<>())
                              .add(workstationPos.toString());
        }
        
        private static void suggestWorkstationPlacement(VillagerEntity villager, Block workstation) {
            // This could integrate with a building/construction system
            // For now, just create gossip about needing a workstation
            VillagerGossipNetwork.createGossip(villager, villager.getUuidAsString(),
                VillagerGossipNetwork.GossipType.MYSTERIOUS_BEHAVIOR,
                "needs a " + workstation.getTranslationKey() + " to work properly");
        }
    }
    
    // Skill progression and training
    public static class SkillSystem {
        
        public static void processTradeExperience(VillagerEntity villager, boolean successful, ItemStack tradedItem) {
            ProfessionData profData = getProfessionData(villager);
            if (profData == null) return;
            
            float baseExperience = successful ? 2.0f : -0.5f;
            String specialization = getRelevantSpecialization(profData.professionId, tradedItem);
            
            profData.gainExperience(baseExperience, specialization);
            
            if (successful) {
                profData.successfulTrades++;
                profData.updateSatisfaction(1.0f, "successful_trade");
            } else {
                profData.failedTrades++;
                profData.updateSatisfaction(-2.0f, "failed_trade");
            }
            
            // Update emotional state based on trade outcome
            VillagerEmotionSystem.EmotionalEvent event = successful ?
                VillagerEmotionSystem.EmotionalEvent.SUCCESSFUL_TRADE :
                new VillagerEmotionSystem.EmotionalEvent(VillagerEmotionSystem.EmotionType.STRESS, 10.0f, "failed_trade", false);
            
            VillagerEmotionSystem.processEmotionalEvent(villager, event);
            
            // Check for skill milestones
            checkSkillMilestones(villager, profData);
        }
        
        private static String getRelevantSpecialization(String profession, ItemStack item) {
            return switch (profession) {
                case "farmer" -> {
                    if (item.getItem() == Items.BREAD || item.getItem() == Items.CAKE) yield "food_processing";
                    if (item.getItem() == Items.WHEAT || item.getItem() == Items.CARROT) yield "crop_growing";
                    yield "general";
                }
                case "librarian" -> {
                    if (item.getItem() == Items.ENCHANTED_BOOK) yield "enchanting";
                    if (item.getItem() == Items.BOOK) yield "book_binding";
                    yield "research";
                }
                case "blacksmith", "armorer", "weaponsmith", "toolsmith" -> {
                    if (item.getItem().toString().contains("sword") || item.getItem().toString().contains("axe")) yield "tool_crafting";
                    yield "metalworking";
                }
                default -> "general";
            };
        }
        
        private static void checkSkillMilestones(VillagerEntity villager, ProfessionData profData) {
            float competency = profData.getOverallCompetency();
            
            if (competency >= 25.0f && competency < 30.0f) {
                unlockAdvancedTrades(villager, profData);
                VillagerEmotionSystem.processEmotionalEvent(villager,
                    new VillagerEmotionSystem.EmotionalEvent(VillagerEmotionSystem.EmotionType.HAPPINESS, 15.0f, "skill_milestone", false));
            } else if (competency >= 50.0f && competency < 55.0f) {
                becomeMaster(villager, profData);
                VillagerEmotionSystem.processEmotionalEvent(villager,
                    new VillagerEmotionSystem.EmotionalEvent(VillagerEmotionSystem.EmotionType.HAPPINESS, 25.0f, "master_level", false));
            } else if (competency >= 75.0f && competency < 80.0f) {
                becomeInnovator(villager, profData);
                VillagerEmotionSystem.processEmotionalEvent(villager,
                    new VillagerEmotionSystem.EmotionalEvent(VillagerEmotionSystem.EmotionType.EXCITEMENT, 30.0f, "innovation_unlock", false));
            }
        }
        
        private static void unlockAdvancedTrades(VillagerEntity villager, ProfessionData profData) {
            // This would add more sophisticated trade offers
            profData.innovation += 0.1f;
            profData.updateSatisfaction(10.0f, "advanced_trades_unlocked");
            
            VillagerGossipNetwork.createGossip(villager, villager.getUuidAsString(),
                VillagerGossipNetwork.GossipType.ACHIEVEMENT,
                "has become more skilled in their profession");
        }
        
        private static void becomeMaster(VillagerEntity villager, ProfessionData profData) {
            profData.innovation += 0.2f;
            profData.updateSatisfaction(20.0f, "master_status");
            
            // Masters can teach other villagers
            enableTeachingAbility(villager, profData);
            
            VillagerGossipNetwork.createGossip(villager, villager.getUuidAsString(),
                VillagerGossipNetwork.GossipType.ACHIEVEMENT,
                "has achieved master level in their profession");
        }
        
        private static void becomeInnovator(VillagerEntity villager, ProfessionData profData) {
            profData.innovation += 0.3f;
            profData.updateSatisfaction(30.0f, "innovator_status");
            
            // Innovators can create unique trade offers
            enableInnovation(villager, profData);
            
            VillagerGossipNetwork.createGossip(villager, villager.getUuidAsString(),
                VillagerGossipNetwork.GossipType.ACHIEVEMENT,
                "has become an innovator in their field");
        }
        
        private static void enableTeachingAbility(VillagerEntity villager, ProfessionData profData) {
            // Find apprentice candidates
            List<VillagerEntity> nearbyVillagers = villager.getWorld().getEntitiesByClass(
                VillagerEntity.class,
                villager.getBoundingBox().expand(20),
                v -> v != villager && hasSameProfession(v, villager)
            );
            
            for (VillagerEntity apprentice : nearbyVillagers) {
                ProfessionData apprenticeData = getProfessionData(apprentice);
                if (apprenticeData != null && profData.canTeachTo(apprenticeData)) {
                    // Create mentor-apprentice relationship
                    createMentorshipRelation(villager, apprentice, profData, apprenticeData);
                }
            }
        }
        
        private static boolean hasSameProfession(VillagerEntity v1, VillagerEntity v2) {
            return v1.getVillagerData().profession().equals(v2.getVillagerData().profession());
        }
        
        private static void createMentorshipRelation(VillagerEntity mentor, VillagerEntity apprentice,
                                                   ProfessionData mentorData, ProfessionData apprenticeData) {
            // Transfer knowledge gradually
            float knowledgeTransfer = mentorData.skillLevel * 0.05f;
            apprenticeData.gainExperience(knowledgeTransfer, null);
            
            // Both gain satisfaction from the teaching relationship
            mentorData.updateSatisfaction(5.0f, "teaching");
            apprenticeData.updateSatisfaction(8.0f, "learning");
            
            VillagerGossipNetwork.createGossip(mentor, apprentice.getUuidAsString(),
                VillagerGossipNetwork.GossipType.POSITIVE_INTERACTION,
                "is teaching " + apprentice.getName().getString());
        }
        
        private static void enableInnovation(VillagerEntity villager, ProfessionData profData) {
            // This would allow creation of unique, innovative trade offers
            generateInnovativeTrades(villager, profData);
        }
        
        private static void generateInnovativeTrades(VillagerEntity villager, ProfessionData profData) {
            // Create custom trade offers based on specializations and innovation level
            TradeOfferList offers = villager.getOffers();
            
            // Example: Innovative librarian might offer books with rare enchantments
            if (profData.professionId.equals("librarian") && profData.innovation > 0.8f) {
                // Would create special enchanted book trade
                profData.knownRecipes.add("innovative_enchantment");
            }
            
            // Innovation increases trade variety and value
            float innovationBonus = profData.innovation * 10.0f;
            profData.updateSatisfaction(innovationBonus, "innovation_success");
        }
    }
    
    // Integration with AI systems
    public static void updateProfessionSatisfaction(VillagerEntity villager) {
        ProfessionData profData = getProfessionData(villager);
        if (profData == null) return;
        
        VillagerEmotionSystem.EmotionalState emotions = VillagerEmotionSystem.getEmotionalState(villager);
        
        // Professional satisfaction affects emotional state
        float satisfactionDelta = (profData.satisfaction - 50.0f) / 50.0f; // -1 to 1
        
        VillagerEmotionSystem.processEmotionalEvent(villager,
            new VillagerEmotionSystem.EmotionalEvent(VillagerEmotionSystem.EmotionType.CONTENTMENT, 
                satisfactionDelta * 5.0f, "professional_satisfaction", false));
        
        // High boredom suggests career change
        float boredom = emotions.getEmotion(VillagerEmotionSystem.EmotionType.BOREDOM);
        if (boredom > 70.0f && profData.satisfaction < 30.0f) {
            profData.updateSatisfaction(-5.0f, "boredom_affecting_work");
        }
    }
    
    public static ProfessionData getProfessionData(VillagerEntity villager) {
        return villagerProfessionData.computeIfAbsent(villager.getUuidAsString(), 
            k -> new ProfessionData(villager.getVillagerData().profession().toString().replace("minecraft:", "")));
    }
    
    public static void initializeVillagerProfession(VillagerEntity villager) {
        if (!villagerProfessionData.containsKey(villager.getUuidAsString())) {
            String professionId = villager.getVillagerData().profession().toString().replace("minecraft:", "");
            ProfessionData profData = new ProfessionData(professionId);
            villagerProfessionData.put(villager.getUuidAsString(), profData);
        }
    }
    
    public static void cleanupProfessionData(String villagerUuid) {
        villagerProfessionData.remove(villagerUuid);
    }
    
    private static String getVillageName(VillagerEntity villager) {
        // Simple village identification - could be enhanced with proper village tracking
        BlockPos pos = villager.getBlockPos();
        return "village_" + (pos.getX() / 100) + "_" + (pos.getZ() / 100);
    }
    
    // Career counseling system
    public static class CareerCounselor {
        
        public static List<VillagerProfession> recommendProfessions(VillagerEntity villager) {
            VillagerData data = villager.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
            VillagerEmotionSystem.EmotionalState emotions = VillagerEmotionSystem.getEmotionalState(villager);
            
            if (data == null) return new ArrayList<>();
            
            List<VillagerProfession> recommendations = new ArrayList<>();
            
            // Analyze personality and emotions for career fit
            String personality = data.getPersonality();
            float curiosity = emotions.getEmotion(VillagerEmotionSystem.EmotionType.CURIOSITY);
            float contentment = emotions.getEmotion(VillagerEmotionSystem.EmotionType.CONTENTMENT);
            
            // Personality-based recommendations
            switch (personality) {
                case "Curious" -> {
                    if (curiosity > 60.0f) {
                        // These constants are RegistryKey; skip adding if wrong type
                        // TODO: adapt to correct API for recommendations type if needed
                    }
                }
                case "Energetic" -> {
                    // TODO: adapt to correct type for recommendations; constants are RegistryKey, skip to compile
                }
                case "Friendly" -> {
                    // TODO: adjust to proper type (RegistryEntry vs RegistryKey vs VillagerProfession)
                    // recommendations.add(VillagerProfession.FARMER);
                    // recommendations.add(VillagerProfession.SHEPHERD);
                }
                case "Serious" -> {
                    // recommendations.add(VillagerProfession.CLERIC);
                    // recommendations.add(VillagerProfession.ARMORER);
                }
                case "Confident" -> {
                    // recommendations.add(VillagerProfession.WEAPONSMITH);
                    // recommendations.add(VillagerProfession.TOOLSMITH);
                }
            }
            
            // Filter based on village needs
            return recommendations.stream()
                .filter(prof -> {
                    VillagerUtilityAI.VillageNeedsConsideration villageNeeds = new VillagerUtilityAI.VillageNeedsConsideration();
                    return villageNeeds.getValue(villager, prof.toString()) > 0.2f;
                })
                .toList();
        }
        
        public static String generateCareerAdvice(VillagerEntity villager) {
            ProfessionData profData = getProfessionData(villager);
            VillagerData data = villager.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
            
            if (profData == null || data == null) return "Keep working hard!";
            
            StringBuilder advice = new StringBuilder();
            
            if (profData.satisfaction < 30.0f) {
                advice.append("You seem unhappy with your current work. ");
                List<VillagerProfession> recommendations = recommendProfessions(villager);
                if (!recommendations.isEmpty()) {
                    advice.append("Have you considered becoming a ")
                          .append(recommendations.get(0).toString().replace("minecraft:", ""))
                          .append("? ");
                }
            } else if (profData.skillLevel < 50.0f) {
                List<String> availableSpecs = profData.getAvailableSpecializations();
                if (!availableSpecs.isEmpty()) {
                    advice.append("You could improve your skills in ")
                          .append(availableSpecs.get(0))
                          .append(". ");
                }
            } else {
                advice.append("You're doing excellent work! ");
                if (profData.innovation < 0.5f) {
                    advice.append("Consider trying new approaches to your craft. ");
                }
            }
            
            return advice.toString();
        }
    }
    
    // Integration helpers
    public static void onVillagerTrade(VillagerEntity villager, ItemStack tradedItem, boolean successful) {
        SkillSystem.processTradeExperience(villager, successful, tradedItem);
        updateProfessionSatisfaction(villager);
    }
    
    public static void onVillagerWork(VillagerEntity villager) {
        ProfessionData profData = getProfessionData(villager);
        if (profData != null) {
            profData.gainExperience(0.5f, null); // Small daily work experience
            profData.timeInProfession += 60000; // 1 minute of work time
        }
    }
    
    public static void evaluateCareerChange(VillagerEntity villager) {
        VillagerUtilityAI.CareerDecisionMaker careerMaker = VillagerUtilityAI.UtilityAIManager.getCareerDecisionMaker();
        
        if (careerMaker.shouldChangeCareer(villager)) {
            List<VillagerProfession> recommendations = CareerCounselor.recommendProfessions(villager);
            if (!recommendations.isEmpty()) {
                VillagerProfession newProfession = recommendations.get(0);
                if (ProfessionChangeManager.attemptProfessionChange(villager, newProfession)) {
                    // Successfully changed career
                    VillagerLearningSystem.processPlayerInteraction(villager, null, "career_change", 0.8f);
                }
            }
        }
    }
}