package com.beeny.villagesreborn.core.ai;

import com.beeny.villagesreborn.core.common.NBTCompound;
import java.util.HashMap;
import java.util.Map;

/**
 * Minimal PersonalityProfile implementation for TDD
 */
public class PersonalityProfile {
    private final Map<TraitType, Float> traits;

    public PersonalityProfile() {
        this.traits = new HashMap<>();
        // Initialize with default values
        for (TraitType trait : TraitType.values()) {
            traits.put(trait, 0.5f);
        }
    }

    public void adjustTrait(TraitType trait, float value) {
        traits.put(trait, Math.max(0.0f, Math.min(1.0f, value)));
    }

    public float getTraitInfluence(TraitType trait) {
        return traits.getOrDefault(trait, 0.5f);
    }

    public String generateDescription() {
        return "Personality traits: " + traits.toString();
    }

    public NBTCompound toNBT() {
        NBTCompound nbt = new NBTCompound();
        for (Map.Entry<TraitType, Float> entry : traits.entrySet()) {
            nbt.putFloat(entry.getKey().name(), entry.getValue());
        }
        return nbt;
    }

    public static PersonalityProfile fromNBT(NBTCompound nbt) {
        PersonalityProfile profile = new PersonalityProfile();
        for (TraitType trait : TraitType.values()) {
            if (nbt.contains(trait.name())) {
                profile.adjustTrait(trait, nbt.getFloat(trait.name()));
            }
        }
        return profile;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof PersonalityProfile)) return false;
        PersonalityProfile other = (PersonalityProfile) obj;
        return traits.equals(other.traits);
    }
}