package com.villagesreborn.beeny.systems;

import java.util.Map;

public class Quest {
    private final int id;
    private final String title;
    private final String description;
    private final Map<String, QuestOption> options; // branching decisions mapped by option key
    private QuestStatus status;
    // Additional fields for objectives and rewards

    public Quest(int id, String title, String description, Map<String, QuestOption> options) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.options = options;
        this.status = QuestStatus.NOT_STARTED;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Map<String, QuestOption> getOptions() {
        return options;
    }

    public QuestStatus getStatus() {
        return status;
    }

    public void setStatus(QuestStatus status) {
        this.status = status;
    }

    public enum QuestStatus {
        NOT_STARTED,
        ACTIVE,
        COMPLETED,
        FAILED
    }
}
