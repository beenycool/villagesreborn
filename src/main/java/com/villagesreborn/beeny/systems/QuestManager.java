package com.villagesreborn.beeny.systems;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Collection;
import java.util.HashMap;

public class QuestManager {
    private final Map<Integer, Quest> activeQuests = new ConcurrentHashMap<>();
    private final Map<Integer, Quest> availableQuests = new HashMap<>();

    public QuestManager() {
        initializeQuests();
    }

    private void initializeQuests() {
        // Example quest: Simple Delivery
        QuestOption option1 = new QuestOption("Accept", 102, null); // Example next quest ID
        QuestOption option2 = new QuestOption("Decline", null, null);
        Map<String, QuestOption> deliveryOptions = Map.of("option_accept", option1, "option_decline", option2);
        Quest deliveryQuest = new Quest(101, "Delivery to Neighbor", "Deliver a package to the villager in the next village.", deliveryOptions);
        availableQuests.put(deliveryQuest.getId(), deliveryQuest);

        // Example quest: Clear out Goblins
        QuestOption goblinOption1 = new QuestOption("Attack goblins", null, null);
        QuestOption goblinOption2 = new QuestOption("Negotiate", null, null);
        Map<String, QuestOption> goblinOptions = Map.of("option_attack", goblinOption1, "option_negotiate", goblinOption2);
        Quest goblinQuest = new Quest(102, "Goblin Menace", "Clear out the goblin camp near the village.", goblinOptions);
        availableQuests.put(goblinQuest.getId(), goblinQuest);
    }

    public void startQuest(Quest quest) {
        activeQuests.put(quest.getId(), quest);
        quest.setStatus(Quest.QuestStatus.ACTIVE);
        // Emit event: QuestStartedEvent
    }

    public void updateQuestProgress(int questId, Object progressData) {
        Quest quest = activeQuests.get(questId);
        if (quest != null) {
            // Update progress and evaluate branching logic based on progressData and conditions
            // Emit event: QuestUpdatedEvent
        }
    }

    public void completeQuest(int questId) {
        Quest quest = activeQuests.remove(questId);
        if (quest != null) {
            quest.setStatus(Quest.QuestStatus.COMPLETED);
            // Process rewards and update village state accordingly
            // Emit event: QuestCompletedEvent
        }
    }

    public Collection<Quest> getActiveQuests() {
        return activeQuests.values();
    }

    public Collection<Quest> getAvailableQuests() {
        return availableQuests.values();
    }
}
