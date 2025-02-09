package com.villagesreborn.beeny;

public class VillagesRebornPlayerData {
    private String playerName;
    private String gender;
    private String originBiome;
    private String backstory;
    private String playerRole; // Store playerRole as String

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getOriginBiome() {
        return originBiome;
    }

    public void setOriginBiome(String originBiome) {
        this.originBiome = originBiome;
    }

    public String getBackstory() {
        return backstory;
    }

    public void setBackstory(String backstory) {
        this.backstory = backstory;
    }

    public String getPlayerRole() {
        return playerRole;
    }

    public void setPlayerRole(String playerRole) { // Setter now accepts String
        this.playerRole = playerRole;
    }
}
