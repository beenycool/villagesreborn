package com.beeny.system;

import com.beeny.Villagersreborn;
import com.beeny.data.VillagerData;
import com.beeny.dialogue.LLMDialogueManager;
import com.beeny.dialogue.VillagerMemoryManager;
import com.beeny.config.VillagersRebornConfig;
import com.beeny.network.AsyncVillagerChatPacket;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.VillagerProfession;
import net.minecraft.world.World;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.CompletableFuture;

public class VillagerDialogueSystem {
    private static final int DIALOGUE_TIMEOUT_SECONDS = 2;
    
    
    public enum DialogueCategory {
        GREETING,
        WEATHER,
        WORK,
        FAMILY,
        GOSSIP,
        TRADE,
        HOBBY,
        MOOD,
        ADVICE,
        STORY,
        FAREWELL
    }
    
    
    public static class DialogueContext {
        public final VillagerEntity villager;
        public final PlayerEntity player;
        public final VillagerData villagerData;
        public final int playerReputation;
        public final VillagerScheduleManager.TimeOfDay timeOfDay;
        public final String weather;
        public WorldContextInfo worldContextInfo;

        /**
         * Constructs a DialogueContext representing the state of a conversation between a villager and a player.
         * Initializes context information such as villager data, player reputation, time of day, world context, and weather.
         *
         * @param villager the VillagerEntity participating in the dialogue
         * @param player the PlayerEntity interacting with the villager
         */
        public DialogueContext(VillagerEntity villager, PlayerEntity player) {
            this.villager = villager;
            this.player = player;
            this.villagerData = villager.getAttached(Villagersreborn.VILLAGER_DATA);
            this.playerReputation = villagerData != null ?
                villagerData.getPlayerReputation(player.getUuidAsString()) : 0;

            long worldTime = villager.getWorld().getTimeOfDay();
            this.timeOfDay = VillagerScheduleManager.TimeOfDay.fromWorldTime(worldTime);

            this.worldContextInfo = new WorldContextInfo(villager.getWorld(), villager.getBlockPos());
            this.weather = worldContextInfo.weather;
        }

        public String getBiome() {
            return worldContextInfo.biome;
        }

        /**
         * Helper class to encapsulate weather and biome retrieval logic.
         */
        public static class WorldContextInfo {
            public final String weather;
            public final String biome;

            public WorldContextInfo(World world, BlockPos pos) {
                this.weather = getWeatherString(world);
                this.biome = getBiomeString(world, pos);
            }

            private static String getWeatherString(World world) {
                if (world.isThundering()) return "stormy";
                if (world.isRaining()) return "rainy";
                return "clear";
            }

            private static String getBiomeString(World world, BlockPos pos) {
                return world.getBiome(pos).getKey()
                    .map(key -> key.getValue().getPath())
                    .orElse("unknown");
            }
        }
    }
    
    // Removed shared Random; use ThreadLocalRandom for thread safety
    private static final Map<String, Map<DialogueCategory, List<String>>> DIALOGUE_TEMPLATES = new HashMap<>();
    
    static {
        initializeDialogueTemplates();
    }
    
    private static void initializeDialogueTemplates() {
        
        Map<DialogueCategory, List<String>> friendlyDialogues = new HashMap<>();
        
        friendlyDialogues.put(DialogueCategory.GREETING, Arrays.asList(
            "Hello there, {player}! What a lovely {timeOfDay} it is!",
            "Oh, {player}! So wonderful to see you again!",
            "Welcome, welcome! How are you this fine {weather} day?",
            "{player}! I was just thinking about you!",
            "Ah, my friend! Come, let's chat for a moment!"
        ));
        
        friendlyDialogues.put(DialogueCategory.WEATHER, Arrays.asList(
            "This {weather} weather is just perfect for {activity}, don't you think?",
            "I love {weather} days like this! They remind me of my childhood.",
            "The {weather} weather always puts me in such a good mood!",
            "On days like this, I feel like anything is possible!"
        ));
        
        friendlyDialogues.put(DialogueCategory.WORK, Arrays.asList(
            "I love being a {profession}! Every day brings new joy!",
            "My work as a {profession} keeps me busy, but I wouldn't have it any other way!",
            "Being a {profession} in this village is such a blessing!",
            "I take great pride in my work as a {profession}!"
        ));
        
        friendlyDialogues.put(DialogueCategory.FAMILY, Arrays.asList(
            "My {familyMember} is doing wonderfully, thank you for asking!",
            "Family is everything to me. My {familyMember} brings such joy to my life!",
            "I'm so blessed to have {familyMember} in my life!",
            "Did I tell you about what my {familyMember} did yesterday? So funny!"
        ));
        
        friendlyDialogues.put(DialogueCategory.GOSSIP, Arrays.asList(
            "Have you heard? {villager1} and {villager2} have been spending a lot of time together!",
            "I shouldn't say, but... I think {villager1} has a crush on someone!",
            "Did you know that {villager1} is planning something special?",
            "Between you and me, I heard {villager1} found something interesting near the {location}!"
        ));
        
        friendlyDialogues.put(DialogueCategory.HOBBY, Arrays.asList(
            "I've been really enjoying {hobby} lately! It's so relaxing!",
            "My {hobby} has been going wonderfully! Would you like to hear about it?",
            "I spent all evening {hobby} yesterday. Time just flew by!",
            "{hobby} is my passion! I could talk about it for hours!"
        ));
        
        DIALOGUE_TEMPLATES.put("Friendly", friendlyDialogues);
        
        
        Map<DialogueCategory, List<String>> grumpyDialogues = new HashMap<>();
        
        grumpyDialogues.put(DialogueCategory.GREETING, Arrays.asList(
            "Oh, it's you. What do you want now?",
            "Hmph. I suppose you want something from me?",
            "Can't a villager get some peace around here?",
            "What now, {player}? I'm busy.",
            "Yes, yes, hello. Now what?"
        ));
        
        grumpyDialogues.put(DialogueCategory.WEATHER, Arrays.asList(
            "This {weather} weather is terrible for my joints.",
            "Bah! Another {weather} day. Just my luck.",
            "I hate {weather} weather. Makes everything harder.",
            "Of course it's {weather}. When is it ever nice?"
        ));
        
        grumpyDialogues.put(DialogueCategory.WORK, Arrays.asList(
            "Being a {profession} isn't as easy as it looks, you know.",
            "Another day, another emerald. If I'm lucky.",
            "Work, work, work. That's all there is to life as a {profession}.",
            "Do you know how hard it is being a {profession}? Of course you don't."
        ));
        
        DIALOGUE_TEMPLATES.put("Grumpy", grumpyDialogues);
        
        
        Map<DialogueCategory, List<String>> shyDialogues = new HashMap<>();
        
        shyDialogues.put(DialogueCategory.GREETING, Arrays.asList(
            "Oh! H-hello, {player}... I didn't see you there...",
            "Um... hi... nice to see you again...",
            "*quietly* Hello, {player}...",
            "Oh, you startled me! H-hello...",
            "..."
        ));
        
        shyDialogues.put(DialogueCategory.WEATHER, Arrays.asList(
            "The {weather} weather is... nice, I suppose...",
            "I-I like {weather} days... they're peaceful...",
            "This weather makes me want to stay inside...",
            "It's... it's quite {weather} today, isn't it?"
        ));
        
        DIALOGUE_TEMPLATES.put("Shy", shyDialogues);
        
        
        initializeMoreDialogues();
    }
    
    private static void initializeMoreDialogues() {
        
        Map<DialogueCategory, List<String>> energeticDialogues = new HashMap<>();
        
        energeticDialogues.put(DialogueCategory.GREETING, Arrays.asList(
            "HELLO {player}! ISN'T TODAY AMAZING?!",
            "Oh wow, {player}! I'm SO excited to see you!",
            "HEY HEY HEY! Look who's here! It's {player}!",
            "{player}! Perfect timing! I have SO much energy today!"
        ));
        
        energeticDialogues.put(DialogueCategory.HOBBY, Arrays.asList(
            "I've been {hobby} ALL DAY and I'm still not tired!",
            "Want to join me for some {hobby}? It'll be FUN!",
            "I discovered a new way to do {hobby}! It's AMAZING!",
            "{hobby} gives me SO MUCH ENERGY!"
        ));
        
        DIALOGUE_TEMPLATES.put("Energetic", energeticDialogues);
        
        
        initializeProfessionDialogues();
    }
    
    private static void initializeProfessionDialogues() {
        
        Map<DialogueCategory, List<String>> farmerDialogues = new HashMap<>();
        
        farmerDialogues.put(DialogueCategory.ADVICE, Arrays.asList(
            "Plant your crops with the moon phases for better yields!",
            "A little bone meal goes a long way, trust me!",
            "The secret to good farming? Patience and water!",
            "Always rotate your crops to keep the soil healthy!"
        ));
        
        farmerDialogues.put(DialogueCategory.STORY, Arrays.asList(
            "Last season, I grew a pumpkin so big, it took three of us to move it!",
            "My grandfather taught me everything about farming. He had golden hands!",
            "Once, during a drought, we all worked together to save the crops. Those were hard times.",
            "I remember when this field was just wilderness. Look at it now!"
        ));
        
        
        DIALOGUE_TEMPLATES.put("profession_farmer", farmerDialogues);
        
        
        Map<DialogueCategory, List<String>> librarianDialogues = new HashMap<>();
        
        librarianDialogues.put(DialogueCategory.ADVICE, Arrays.asList(
            "Knowledge is power, but wisdom is knowing how to use it!",
            "I've read that enchanting during a full moon yields better results!",
            "Always keep your books dry. Moisture is their greatest enemy!",
            "The ancient texts speak of great treasures hidden in {biome} biomes!"
        ));
        
        librarianDialogues.put(DialogueCategory.STORY, Arrays.asList(
            "I once found a book written in an ancient language. Still trying to decode it!",
            "The library's oldest book dates back 500 years. It's about {biome} exploration!",
            "A traveling scholar once told me about a lost library. I dream of finding it!",
            "Books have taken me on more adventures than my feet ever could!"
        ));
        
        DIALOGUE_TEMPLATES.put("profession_librarian", librarianDialogues);
    }
    
    
    // Add SLF4J logger
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(VillagerDialogueSystem.class);

    /**
     * Asynchronous dialogue generation. Returns a placeholder immediately,
     * and invokes the provided callback with the generated dialogue when ready.
     */
    public static Text generateDialogue(DialogueContext context, DialogueCategory category, java.util.function.Consumer<Text> onDialogueReady) {
        if (context.villagerData == null) {
            onDialogueReady.accept(Text.literal("...").formatted(Formatting.GRAY));
            return Text.literal("...").formatted(Formatting.GRAY);
        }

        UUID conversationId = UUID.randomUUID();

        if (VillagersRebornConfig.ENABLE_DYNAMIC_DIALOGUE) {
            // Send interim packet if the player is a server player
            if (context.player instanceof ServerPlayerEntity) {
                ServerPlayerEntity serverPlayer = (ServerPlayerEntity) context.player;
                AsyncVillagerChatPacket interimPacket = new AsyncVillagerChatPacket(
                    conversationId,
                    context.villager.getUuid(),
                    context.villagerData.getName(),
                    context.player.getUuid(),
                    Text.translatable("villagersreborn.dialogue.generating").getString(),
                    false,
                    System.currentTimeMillis()
                );
                // Send interim packet to client using CustomPayload
                net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(
                    serverPlayer,
                    interimPacket
                );
            }

            LLMDialogueManager.generateDialogueAsync(context, category)
                .orTimeout(DIALOGUE_TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS)
                .whenComplete((llmDialogue, throwable) -> {
                    if (throwable != null) {
                        LOGGER.warn("LLM dialogue generation failed or timed out, falling back to static", throwable);
                        onDialogueReady.accept(net.minecraft.text.Text.literal("...").formatted(net.minecraft.util.Formatting.GRAY));
                    } else if (llmDialogue != null) {
                        // Send final packet on the server thread
                        if (context.player instanceof ServerPlayerEntity) {
                            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) context.player;
                            serverPlayer.getServer().execute(() -> {
                                ServerPlayerEntity onlinePlayer = serverPlayer.getServer().getPlayerManager().getPlayer(serverPlayer.getUuid());
                                if (onlinePlayer != null) {
                                    AsyncVillagerChatPacket finalPacket = new AsyncVillagerChatPacket(
                                        conversationId,
                                        context.villager.getUuid(),
                                        context.villagerData.getName(),
                                        context.player.getUuid(),
                                        llmDialogue.getString(),
                                        true,
                                        System.currentTimeMillis()
                                    );
                                    net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(
                                        onlinePlayer,
                                        finalPacket
                                    );
                                }
                            });
                        }
                        onDialogueReady.accept(llmDialogue);
                    } else {
                        onDialogueReady.accept(null);
                    }
                });
            // Return placeholder immediately; actual dialogue will be provided via callback
            return Text.translatable("villagersreborn.dialogue.generating");
        }

        // Fall back to static dialogue system
        return generateStaticDialogue(context, category);
    }
    
    public static CompletableFuture<Text> generateDialogueAsync(DialogueContext context, DialogueCategory category) {
        if (context.villagerData == null) {
            return CompletableFuture.completedFuture(Text.literal("...").formatted(Formatting.GRAY));
        }
        
        // Try LLM generation first if enabled
        if (VillagersRebornConfig.ENABLE_DYNAMIC_DIALOGUE) {
            return LLMDialogueManager.generateDialogueAsync(context, category)
                .thenCompose(llmDialogue -> {
                    if (llmDialogue != null) {
                        return CompletableFuture.completedFuture(llmDialogue);
                    } else {
                        // Fall back to static dialogue
                        return CompletableFuture.completedFuture(generateStaticDialogue(context, category));
                    }
                })
                .exceptionally(throwable -> {
                    LOGGER.error("Async LLM dialogue generation failed, falling back to static", throwable);
                    return generateStaticDialogue(context, category);
                });
        } else {
            return CompletableFuture.completedFuture(generateStaticDialogue(context, category));
        }
    }
    
    private static Text generateStaticDialogue(DialogueContext context, DialogueCategory category) {
        
        String personality = context.villagerData.getPersonality().name();
        String personalityKey = personality.substring(0, 1).toUpperCase() + personality.substring(1).toLowerCase();
        Map<DialogueCategory, List<String>> personalityDialogues =
            DIALOGUE_TEMPLATES.getOrDefault(personalityKey, DIALOGUE_TEMPLATES.get("Friendly"));
        
        
        String professionKey = "profession_" + context.villager.getVillagerData()
            .profession().toString().toLowerCase().replace("minecraft:", "");
        Map<DialogueCategory, List<String>> professionDialogues = 
            DIALOGUE_TEMPLATES.get(professionKey);
        
        
        List<String> dialogueOptions = new ArrayList<>();
        
        
        if (personalityDialogues.containsKey(category)) {
            dialogueOptions.addAll(personalityDialogues.get(category));
        }
        
        
        if (professionDialogues != null && 
            (category == DialogueCategory.ADVICE || category == DialogueCategory.STORY || 
             category == DialogueCategory.WORK)) {
            if (professionDialogues.containsKey(category)) {
                dialogueOptions.addAll(professionDialogues.get(category));
            }
        }
        
        
        if (dialogueOptions.isEmpty()) {
            return generateFallbackDialogue(context, category);
        }
        
        
        String selectedDialogue = dialogueOptions.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(dialogueOptions.size()));
        String processedDialogue = processDialogueTags(selectedDialogue, context);
        
        
        Formatting formatting = getDialogueFormatting(context);
        
        // Update memory for static dialogue too
        VillagerMemoryManager.addVillagerResponse(
            context.villager, context.player, processedDialogue, category.name()
        );
        
        return (Text) Text.literal(processedDialogue).formatted(formatting);
    }
    
    
    private static String processDialogueTags(String dialogue, DialogueContext context) {
        Map<String, String> replacements = new HashMap<>();
        
        
        replacements.put("{player}", context.player.getName().getString());
        replacements.put("{timeOfDay}", context.timeOfDay.name.toLowerCase());
        replacements.put("{weather}", context.weather);
        replacements.put("{biome}", formatBiomeName(context.worldContextInfo.biome));
        replacements.put("{profession}", "villager");
        
        
        VillagerScheduleManager.Activity currentActivity = 
            VillagerScheduleManager.getCurrentActivity(context.villager);
        replacements.put("{activity}", currentActivity.description.toLowerCase());
        
        
        replacements.put("{hobby}", context.villagerData.getHobby().name().toLowerCase());
        
        
        if (!context.villagerData.getSpouseName().isEmpty()) {
            replacements.put("{spouse}", context.villagerData.getSpouseName());
            replacements.put("{familyMember}", context.villagerData.getSpouseName());
        } else if (!context.villagerData.getChildrenNames().isEmpty()) {
            String childName = context.villagerData.getChildrenNames().get(0);
            replacements.put("{child}", childName);
            replacements.put("{familyMember}", "child " + childName);
        } else {
            replacements.put("{familyMember}", "family");
        }
        
        
        List<VillagerEntity> nearbyVillagers = context.villager.getWorld().getEntitiesByClass(
            VillagerEntity.class,
            context.villager.getBoundingBox().expand(30),
            v -> v != context.villager && v.getAttached(Villagersreborn.VILLAGER_DATA) != null
        );
        
        if (!nearbyVillagers.isEmpty()) {
            VillagerEntity randomVillager1 = nearbyVillagers.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(nearbyVillagers.size()));
            VillagerData data1 = randomVillager1.getAttached(Villagersreborn.VILLAGER_DATA);
            replacements.put("{villager1}", data1.getName());
            
            if (nearbyVillagers.size() > 1) {
                VillagerEntity randomVillager2;
                do {
                    randomVillager2 = nearbyVillagers.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(nearbyVillagers.size()));
                } while (randomVillager2 == randomVillager1);
                VillagerData data2 = randomVillager2.getAttached(Villagersreborn.VILLAGER_DATA);
                replacements.put("{villager2}", data2.getName());
            }
        }
        
        
        String[] locations = {"old oak tree", "village well", "market square", "forest edge", "river bend"};
        replacements.put("{location}", locations[ThreadLocalRandom.current().nextInt(locations.length)]);
        
        
        String processed = dialogue;
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            processed = processed.replace(entry.getKey(), entry.getValue());
        }
        
        return processed;
    }
    
    
    private static Text generateFallbackDialogue(DialogueContext context, DialogueCategory category) {
        String message = switch (category) {
            case GREETING -> "Hello there!";
            case WEATHER -> "Interesting weather today.";
            case WORK -> "Work keeps me busy.";
            case FAMILY -> "Family is important.";
            case GOSSIP -> "I don't have much news.";
            case TRADE -> "Looking to trade?";
            case HOBBY -> "I enjoy my free time.";
            case MOOD -> "I'm doing alright.";
            case ADVICE -> "Stay safe out there.";
            case STORY -> "I don't have any stories right now.";
            case FAREWELL -> "Goodbye!";
        };
        
        return (Text) Text.literal(message).formatted(Formatting.GRAY);
    }
    
    
    private static Formatting getDialogueFormatting(DialogueContext context) {
        if (context.playerReputation > 50) {
            return Formatting.GREEN;
        } else if (context.playerReputation < -20) {
            return Formatting.RED;
        } else if (context.villagerData.getHappiness() > 70) {
            return Formatting.AQUA;
        } else if (context.villagerData.getHappiness() < 30) {
            return Formatting.GRAY;
        }
        return Formatting.WHITE;
    }
    
    
    private static String formatBiomeName(String biome) {
        return biome.replace("_", " ").toLowerCase();
    }
    
    
    private static String formatProfessionName(VillagerProfession profession) {
        String name = profession.toString().replace("minecraft:", "");
        return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
    }
    
    
    public static DialogueCategory chooseDialogueCategory(DialogueContext context) {
        
        Map<DialogueCategory, Integer> weights = new HashMap<>();
        
        
        weights.put(DialogueCategory.GREETING, 20);
        weights.put(DialogueCategory.WEATHER, 15);
        weights.put(DialogueCategory.MOOD, 15);
        
        
        if (context.timeOfDay == VillagerScheduleManager.TimeOfDay.MORNING || 
            context.timeOfDay == VillagerScheduleManager.TimeOfDay.AFTERNOON) {
            weights.put(DialogueCategory.WORK, 20);
        }
        
        
        if (context.playerReputation > 20) {
            weights.put(DialogueCategory.GOSSIP, 15);
            weights.put(DialogueCategory.ADVICE, 10);
            weights.put(DialogueCategory.STORY, 10);
        }
        
        
        if (!context.villagerData.getSpouseName().isEmpty() || 
            !context.villagerData.getChildrenNames().isEmpty()) {
            weights.put(DialogueCategory.FAMILY, 20);
        }
        
        
        weights.put(DialogueCategory.HOBBY, 10);
        
        
        if (context.timeOfDay == VillagerScheduleManager.TimeOfDay.DUSK || 
            context.timeOfDay == VillagerScheduleManager.TimeOfDay.NIGHT) {
            weights.put(DialogueCategory.FAREWELL, 25);
        }
        
        
        int totalWeight = weights.values().stream().mapToInt(Integer::intValue).sum();
        int random = java.util.concurrent.ThreadLocalRandom.current().nextInt(totalWeight);
        int currentWeight = 0;
        
        for (Map.Entry<DialogueCategory, Integer> entry : weights.entrySet()) {
            currentWeight += entry.getValue();
            if (random < currentWeight) {
                return entry.getKey();
            }
        }
        
        return DialogueCategory.GREETING; 
    }

    // Convenience overload to obtain immediate text while allowing async updates via callback
    public static Text generateDialogue(DialogueContext context, DialogueCategory category) {
        return generateDialogue(context, category, t -> {});
    }

    // Synchronous conversation wrapper with timeout
    public static List<Text> generateConversation(VillagerEntity villager, PlayerEntity player) {
        try {
            return generateConversationAsync(villager, player)
                .get(5, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            LOGGER.error("Failed to generate conversation", e);
            return java.util.List.of(net.minecraft.text.Text.literal("...").formatted(net.minecraft.util.Formatting.GRAY));
        }
    }

    // Asynchronously build a short conversation consisting of multiple lines
    public static CompletableFuture<List<Text>> generateConversationAsync(VillagerEntity villager, PlayerEntity player) {
        DialogueContext context = new DialogueContext(villager, player);
        List<CompletableFuture<Text>> futureDialogues = new ArrayList<>();

        // Use current activity to influence dialogue selection
        VillagerScheduleManager.Activity currentActivity = 
            VillagerScheduleManager.getCurrentActivity(villager);

        DialogueCategory openingCategory = java.util.concurrent.ThreadLocalRandom.current().nextBoolean() ?
            DialogueCategory.GREETING : DialogueCategory.MOOD;
        futureDialogues.add(generateDialogueAsync(context, openingCategory));

        DialogueCategory mainCategory = chooseDialogueCategory(context);
        if (mainCategory != openingCategory) {
            futureDialogues.add(generateDialogueAsync(context, mainCategory));
        }

        if (context.playerReputation > 30 && java.util.concurrent.ThreadLocalRandom.current().nextFloat() < 0.5f) {
            DialogueCategory followUp = java.util.concurrent.ThreadLocalRandom.current().nextBoolean() ?
                DialogueCategory.GOSSIP : DialogueCategory.ADVICE;
            futureDialogues.add(generateDialogueAsync(context, followUp));
        }

        if ((context.timeOfDay == VillagerScheduleManager.TimeOfDay.DUSK ||
             context.timeOfDay == VillagerScheduleManager.TimeOfDay.NIGHT) &&
            mainCategory != DialogueCategory.FAREWELL) {
            futureDialogues.add(generateDialogueAsync(context, DialogueCategory.FAREWELL));
        }

        // Combine all futures
        return CompletableFuture.allOf(futureDialogues.toArray(new CompletableFuture[0]))
            .thenApply(v -> futureDialogues.stream()
                .map(CompletableFuture::join)
                .collect(java.util.stream.Collectors.toList()));
    }

    // Single-line contextual dialogue reflective of current activity
    public static Text generateContextualDialogue(VillagerEntity villager, PlayerEntity player) {
        DialogueContext context = new DialogueContext(villager, player);

        VillagerScheduleManager.Activity currentActivity =
            VillagerScheduleManager.getCurrentActivity(villager);

        DialogueCategory category = chooseDialogueCategoryForActivity(context, currentActivity);
        Text dialogue = generateDialogue(context, category);

        // Add activity-specific context to the dialogue
        if (currentActivity != VillagerScheduleManager.Activity.WANDER) {
            String activityPrefix = getActivityPrefix(currentActivity);
            if (activityPrefix != null) {
                return Text.literal(activityPrefix + " ")
                    .formatted(Formatting.ITALIC, Formatting.GRAY)
                    .append(dialogue);
            }
        }

        return dialogue;
    }

    private static DialogueCategory chooseDialogueCategoryForActivity(DialogueContext context, VillagerScheduleManager.Activity activity) {
        // Activity-specific dialogue preferences
        return switch (activity) {
            case WORK -> DialogueCategory.WORK;
            case SOCIALIZE -> DialogueCategory.GOSSIP;
            case EAT -> context.villagerData.getHappiness() > 60 ? DialogueCategory.MOOD : DialogueCategory.GREETING;
            case HOBBY -> DialogueCategory.HOBBY;
            case STUDY -> DialogueCategory.ADVICE;
            case PRAY -> DialogueCategory.MOOD;
            case EXERCISE -> DialogueCategory.MOOD;
            case SLEEP -> DialogueCategory.FAREWELL;
            case WAKE_UP -> DialogueCategory.GREETING;
            default -> chooseDialogueCategory(context);
        };
    }

    private static String getActivityPrefix(VillagerScheduleManager.Activity activity) {
        return switch (activity) {
            case WORK -> "*pauses from work*";
            case EAT -> "*looks up from eating*";
            case STUDY -> "*closes book briefly*";
            case EXERCISE -> "*wipes sweat*";
            case HOBBY -> "*sets down hobby materials*";
            case PRAY -> "*finishes prayer*";
            case SOCIALIZE -> "*turns from conversation*";
            default -> null;
        };
    }
}