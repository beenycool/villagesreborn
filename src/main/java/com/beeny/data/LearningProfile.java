package com.beeny.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

public class LearningProfile {
    public static final Codec<LearningProfile> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.unboundedMap(Codec.STRING, Codec.FLOAT).fieldOf("skillLevels").orElse(Collections.emptyMap()).forGetter(LearningProfile::getSkillLevels),
            Codec.unboundedMap(Codec.STRING, Codec.FLOAT).fieldOf("learningRates").orElse(Collections.emptyMap()).forGetter(LearningProfile::getLearningRates),
            Codec.unboundedMap(Codec.STRING, Codec.FLOAT).fieldOf("experience").orElse(Collections.emptyMap()).forGetter(LearningProfile::getExperience),
            Codec.INT.fieldOf("totalExperience").orElse(0).forGetter(LearningProfile::getTotalExperience),
            Codec.FLOAT.fieldOf("adaptability").orElse(0.5f).forGetter(LearningProfile::getAdaptability)
        ).apply(instance, LearningProfile::new)
    );

    private final Map<String, Float> skillLevels;
    private final Map<String, Float> learningRates;
    private final Map<String, Float> experience;
    private int totalExperience;
    private float adaptability;

    public LearningProfile(Map<String, Float> skillLevels, Map<String, Float> learningRates, Map<String, Float> experience, int totalExperience, float adaptability) {
        this.skillLevels = new HashMap<>(skillLevels);
        this.learningRates = new HashMap<>(learningRates);
        this.experience = new HashMap<>(experience);
        this.totalExperience = totalExperience;
        this.adaptability = adaptability;
    }

    public LearningProfile() {
        this(new HashMap<>(), new HashMap<>(), new HashMap<>(), 0, 0.5f);
    }

    public Map<String, Float> getSkillLevels() {
        return Collections.unmodifiableMap(skillLevels);
    }

    public Map<String, Float> getLearningRates() {
        return Collections.unmodifiableMap(learningRates);
    }

    public Map<String, Float> getExperience() {
        return Collections.unmodifiableMap(experience);
    }

    public int getTotalExperience() {
        return totalExperience;
    }

    public float getAdaptability() {
        return adaptability;
    }

    public float getSkillLevel(String skill) {
        return skillLevels.getOrDefault(skill, 0.0f);
    }

    public float getLearningRate(String skill) {
        return learningRates.getOrDefault(skill, 1.0f);
    }

    public float getExperience(String skill) {
        return experience.getOrDefault(skill, 0.0f);
    }

    public void setSkillLevel(String skill, float level) {
        skillLevels.put(skill, Math.max(0.0f, Math.min(100.0f, level)));
    }

    public void setLearningRate(String skill, float rate) {
        learningRates.put(skill, Math.max(0.1f, Math.min(5.0f, rate)));
    }

    public void addExperience(String skill, float amount) {
        float currentExp = experience.getOrDefault(skill, 0.0f);
        experience.put(skill, currentExp + amount);
        totalExperience += (int) amount;
        
        // Level up based on experience
        float currentLevel = skillLevels.getOrDefault(skill, 0.0f);
        float newLevel = currentLevel + (amount * getLearningRate(skill) * 0.01f);
        setSkillLevel(skill, newLevel);
    }

    public void setAdaptability(float adaptability) {
        this.adaptability = Math.max(0.0f, Math.min(1.0f, adaptability));
    }

    public LearningProfile copy() {
        return new LearningProfile(new HashMap<>(skillLevels), new HashMap<>(learningRates), new HashMap<>(experience), totalExperience, adaptability);
    }
}