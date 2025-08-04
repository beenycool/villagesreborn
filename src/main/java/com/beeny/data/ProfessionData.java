package com.beeny.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

public class ProfessionData {
    public static final Codec<ProfessionData> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.STRING.fieldOf("currentProfession").orElse("UNEMPLOYED").forGetter(ProfessionData::getCurrentProfession),
            Codec.INT.fieldOf("professionLevel").orElse(1).forGetter(ProfessionData::getProfessionLevel),
            Codec.INT.fieldOf("experience").orElse(0).forGetter(ProfessionData::getExperience),
            Codec.INT.fieldOf("experienceToNextLevel").orElse(100).forGetter(ProfessionData::getExperienceToNextLevel),
            Codec.unboundedMap(Codec.STRING, Codec.INT).fieldOf("professionStats").orElse(Collections.emptyMap()).forGetter(ProfessionData::getProfessionStats),
            Codec.FLOAT.fieldOf("efficiency").orElse(1.0f).forGetter(ProfessionData::getEfficiency),
            Codec.FLOAT.fieldOf("quality").orElse(1.0f).forGetter(ProfessionData::getQuality)
        ).apply(instance, ProfessionData::new)
    );

    private String currentProfession;
    private int professionLevel;
    private int experience;
    private int experienceToNextLevel;
    private final Map<String, Integer> professionStats;
    private float efficiency;
    private float quality;

    public ProfessionData(String currentProfession, int professionLevel, int experience, int experienceToNextLevel, Map<String, Integer> professionStats, float efficiency, float quality) {
        this.currentProfession = currentProfession;
        this.professionLevel = professionLevel;
        this.experience = experience;
        this.experienceToNextLevel = experienceToNextLevel;
        this.professionStats = new HashMap<>(professionStats);
        this.efficiency = efficiency;
        this.quality = quality;
    }

    public ProfessionData() {
        this("UNEMPLOYED", 1, 0, 100, new HashMap<>(), 1.0f, 1.0f);
    }

    public String getCurrentProfession() {
        return currentProfession;
    }

    public int getProfessionLevel() {
        return professionLevel;
    }

    public int getExperience() {
        return experience;
    }

    public int getExperienceToNextLevel() {
        return experienceToNextLevel;
    }

    public Map<String, Integer> getProfessionStats() {
        return Collections.unmodifiableMap(professionStats);
    }

    public float getEfficiency() {
        return efficiency;
    }

    public float getQuality() {
        return quality;
    }

    public void setCurrentProfession(String currentProfession) {
        this.currentProfession = currentProfession;
    }

    public void setProfessionLevel(int professionLevel) {
        this.professionLevel = Math.max(1, professionLevel);
        this.experienceToNextLevel = professionLevel * 100;
    }

    public void addExperience(int amount) {
        experience += amount;
        while (experience >= experienceToNextLevel) {
            experience -= experienceToNextLevel;
            professionLevel++;
            experienceToNextLevel = professionLevel * 100;
        }
    }

    public void setEfficiency(float efficiency) {
        this.efficiency = Math.max(0.1f, Math.min(5.0f, efficiency));
    }

    public void setQuality(float quality) {
        this.quality = Math.max(0.1f, Math.min(5.0f, quality));
    }

    public void incrementStat(String stat) {
        professionStats.put(stat, professionStats.getOrDefault(stat, 0) + 1);
    }

    public int getStat(String stat) {
        return professionStats.getOrDefault(stat, 0);
    }

    public ProfessionData copy() {
        return new ProfessionData(currentProfession, professionLevel, experience, experienceToNextLevel, new HashMap<>(professionStats), efficiency, quality);
    }
}