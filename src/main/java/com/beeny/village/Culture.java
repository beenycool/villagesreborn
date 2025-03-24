package com.beeny.village;

public enum Culture {
    ROMAN("roman", "Roman"),
    EGYPTIAN("egyptian", "Egyptian"),
    VICTORIAN("victorian", "Victorian"),
    NYC("nyc", "NYC"),
    NETHER("nether", "Nether"),
    END("end", "End");

    private final String id;
    private final String displayName;

    Culture(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }
}
