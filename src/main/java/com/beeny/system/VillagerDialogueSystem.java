package com.beeny.system;

import com.beeny.Villagersreborn;
import com.beeny.data.VillagerData;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.VillagerProfession;
import net.minecraft.world.World;

import java.util.*;

public class VillagerDialogueSystem {
    
    
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
        public final String biome;
        
        public DialogueContext(VillagerEntity villager, PlayerEntity player) {
            this.villager = villager;
            this.player = player;
            this.villagerData = villager.getAttached(Villagersreborn.VILLAGER_DATA);
            this.playerReputation = villagerData != null ? 
                villagerData.getPlayerReputation(player.getUuidAsString()) : 0;
            
            long worldTime = villager.getWorld().getTimeOfDay();
            this.timeOfDay = VillagerScheduleManager.TimeOfDay.fromWorldTime(worldTime);
            
            this.weather = getWeatherString(villager.getWorld());
            this.biome = getBiomeString(villager.getWorld(), villager.getBlockPos());
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
    
    private static final Random RANDOM = new Random();
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
    
    
    public static Text generateDialogue(DialogueContext context, DialogueCategory category) {
        if (context.villagerData == null) {
            return Text.literal("...").formatted(Formatting.GRAY);
        }
        
        
        String personality = context.villagerData.getPersonality();
        Map<DialogueCategory, List<String>> personalityDialogues = 
            DIALOGUE_TEMPLATES.getOrDefault(personality, DIALOGUE_TEMPLATES.get("Friendly"));
        
        
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
        
        
        String selectedDialogue = dialogueOptions.get(RANDOM.nextInt(dialogueOptions.size()));
        String processedDialogue = processDialogueTags(selectedDialogue, context);
        
        
        Formatting formatting = getDialogueFormatting(context);
        
        return Text.literal(processedDialogue).formatted(formatting);
    }
    
    
    private static String processDialogueTags(String dialogue, DialogueContext context) {
        Map<String, String> replacements = new HashMap<>();
        
        
        replacements.put("{player}", context.player.getName().getString());
        replacements.put("{timeOfDay}", context.timeOfDay.name.toLowerCase());
        replacements.put("{weather}", context.weather);
        replacements.put("{biome}", formatBiomeName(context.biome));
        replacements.put("{profession}", "villager");
        
        
        VillagerScheduleManager.Activity currentActivity = 
            VillagerScheduleManager.getCurrentActivity(context.villager);
        replacements.put("{activity}", currentActivity.description.toLowerCase());
        
        
        replacements.put("{hobby}", context.villagerData.getHobby().toLowerCase());
        
        
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
            VillagerEntity randomVillager1 = nearbyVillagers.get(RANDOM.nextInt(nearbyVillagers.size()));
            VillagerData data1 = randomVillager1.getAttached(Villagersreborn.VILLAGER_DATA);
            replacements.put("{villager1}", data1.getName());
            
            if (nearbyVillagers.size() > 1) {
                VillagerEntity randomVillager2;
                do {
                    randomVillager2 = nearbyVillagers.get(RANDOM.nextInt(nearbyVillagers.size()));
                } while (randomVillager2 == randomVillager1);
                VillagerData data2 = randomVillager2.getAttached(Villagersreborn.VILLAGER_DATA);
                replacements.put("{villager2}", data2.getName());
            }
        }
        
        
        String[] locations = {"old oak tree", "village well", "market square", "forest edge", "river bend"};
        replacements.put("{location}", locations[RANDOM.nextInt(locations.length)]);
        
        
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
        
        return Text.literal(message).formatted(Formatting.GRAY);
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
        int random = RANDOM.nextInt(totalWeight);
        int currentWeight = 0;
        
        for (Map.Entry<DialogueCategory, Integer> entry : weights.entrySet()) {
            currentWeight += entry.getValue();
            if (random < currentWeight) {
                return entry.getKey();
            }
        }
        
        return DialogueCategory.GREETING; 
    }
    
    
    public static List<Text> generateConversation(VillagerEntity villager, PlayerEntity player) {
        DialogueContext context = new DialogueContext(villager, player);
        List<Text> conversation = new ArrayList<>();
        
        
        DialogueCategory openingCategory = RANDOM.nextBoolean() ? 
            DialogueCategory.GREETING : DialogueCategory.MOOD;
        conversation.add(generateDialogue(context, openingCategory));
        
        
        DialogueCategory mainCategory = chooseDialogueCategory(context);
        if (mainCategory != openingCategory) {
            conversation.add(generateDialogue(context, mainCategory));
        }
        
        
        if (context.playerReputation > 30 && RANDOM.nextFloat() < 0.5f) {
            DialogueCategory followUp = RANDOM.nextBoolean() ? 
                DialogueCategory.GOSSIP : DialogueCategory.ADVICE;
            conversation.add(generateDialogue(context, followUp));
        }
        
        
        if (context.timeOfDay == VillagerScheduleManager.TimeOfDay.DUSK || 
            context.timeOfDay == VillagerScheduleManager.TimeOfDay.NIGHT) {
            conversation.add(generateDialogue(context, DialogueCategory.FAREWELL));
        }
        
        return conversation;
    }
}