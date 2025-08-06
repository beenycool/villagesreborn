package com.beeny.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.Uuids;

import java.util.UUID;

public class VillagerDataPacket {
    public static final PacketCodec<PacketByteBuf, VillagerDataPacket> CODEC = PacketCodec.of(
        (value, buf) -> {
            // Write VillagerDataPacket fields into the buffer
            // First write the runtime entity int id, then the stable UUID
            PacketCodecs.INTEGER.encode(buf, value.getEntityIdInt());
            Uuids.PACKET_CODEC.encode(buf, value.getEntityUuid());
            PacketCodecs.STRING.encode(buf, value.getName());
            PacketCodecs.STRING.encode(buf, value.getProfession());
            PacketCodecs.INTEGER.encode(buf, value.getHappiness());
            PacketCodecs.STRING.encode(buf, value.getPersonality());
            PacketCodecs.STRING.encode(buf, value.getHobby());
            PacketCodecs.INTEGER.encode(buf, value.getAge());
            PacketCodecs.STRING.encode(buf, value.getGender());
            PacketCodecs.STRING.encode(buf, value.getFavoriteFood());
            PacketCodecs.STRING.encode(buf, value.getNotes() != null ? value.getNotes() : "");
            PacketCodecs.STRING.encode(buf, value.getSpouseName() != null ? value.getSpouseName() : "");
            PacketCodecs.INTEGER.encode(buf, value.getTotalTrades());
            PacketCodecs.STRING.encode(buf, value.getBirthPlace() != null ? value.getBirthPlace() : "");
            PacketCodecs.STRING.encode(buf, value.getCurrentGoal());
            PacketCodecs.STRING.encode(buf, value.getCurrentAction());
            PacketCodecs.STRING.encode(buf, value.getDominantEmotion());
            PacketCodecs.STRING.encode(buf, value.getEmotionalDescription());
        },
        buf -> new VillagerDataPacket(
            PacketCodecs.INTEGER.decode(buf),
            Uuids.PACKET_CODEC.decode(buf),
            PacketCodecs.STRING.decode(buf),
            PacketCodecs.STRING.decode(buf),
            PacketCodecs.INTEGER.decode(buf),
            PacketCodecs.STRING.decode(buf),
            PacketCodecs.STRING.decode(buf),
            PacketCodecs.INTEGER.decode(buf),
            PacketCodecs.STRING.decode(buf),
            PacketCodecs.STRING.decode(buf),
            PacketCodecs.STRING.decode(buf),
            PacketCodecs.STRING.decode(buf),
            PacketCodecs.INTEGER.decode(buf),
            PacketCodecs.STRING.decode(buf),
            PacketCodecs.STRING.decode(buf),
            PacketCodecs.STRING.decode(buf),
            PacketCodecs.STRING.decode(buf),
            PacketCodecs.STRING.decode(buf)
        )
    );

    // Stable UUID for identity across sessions
    private final UUID entityUuid;
    // Runtime entity ID for server lookups and packets like teleport, notes, etc.
    private final int entityIdInt;
    private final String name;
    private final String profession;
    private final int happiness;
    private final String personality;
    private final String hobby;
    private final int age;
    private final String gender;
    private final String favoriteFood;
    private final String notes; // may be null
    private final String spouseName; // may be null
    private final int totalTrades;
    private final String birthPlace; // may be null
    private final String currentGoal;
    private final String currentAction;
    private final String dominantEmotion;
    private final String emotionalDescription;

    // Back-compat: previous getters
    public UUID getEntityId() { return entityUuid; }
    // Clear getters
    public UUID getEntityUuid() { return entityUuid; }
    public int getEntityIdInt() { return entityIdInt; }
    public String getName() { return name; }
    public String getProfession() { return profession; }
    public int getHappiness() { return happiness; }
    public String getPersonality() { return personality; }
    public String getHobby() { return hobby; }
    public int getAge() { return age; }
    public String getGender() { return gender; }
    public String getFavoriteFood() { return favoriteFood; }
    public String getNotes() { return notes; }
    public String getSpouseName() { return spouseName; }
    public int getTotalTrades() { return totalTrades; }
    public String getBirthPlace() { return birthPlace; }
    public String getCurrentGoal() { return currentGoal; }
    public String getCurrentAction() { return currentAction; }
    public String getDominantEmotion() { return dominantEmotion; }
    public String getEmotionalDescription() { return emotionalDescription; }

    // Additional methods needed by GUI classes
    public UUID getId() { return entityUuid; }
    public String getSpouseId() { return spouseName; }
    public int getAgeInDays() { return age; }
    public String getHappinessDescription() {
        if (happiness >= 80) return "Very Happy";
        if (happiness >= 60) return "Happy";
        if (happiness >= 40) return "Content";
        if (happiness >= 20) return "Unhappy";
        return "Very Unhappy";
    }
    public java.util.List<String> getChildrenNames() { return java.util.Collections.emptyList(); }
    public java.util.List<String> getFamilyMembers() { return java.util.Collections.emptyList(); }
    
    // Mutable note setter for client-side modifications
    private String mutableNotes;
    public void setNotes(String notes) { this.mutableNotes = notes; }
    public String getMutableNotes() { return mutableNotes != null ? mutableNotes : notes; }

    public VillagerDataPacket(int entityIdInt, UUID entityUuid, String name, String profession, int happiness, String personality,
                            String hobby, int age, String gender, String favoriteFood, String notes,
                            String spouseName, int totalTrades, String birthPlace, String currentGoal,
                            String currentAction, String dominantEmotion, String emotionalDescription) {
        this.entityUuid = entityUuid;
        this.entityIdInt = entityIdInt;
        this.name = name;
        this.profession = profession;
        this.happiness = happiness;
        this.personality = personality;
        this.hobby = hobby;
        this.age = age;
        this.gender = gender;
        this.favoriteFood = favoriteFood;
        this.notes = notes;
        this.spouseName = spouseName;
        this.totalTrades = totalTrades;
        this.birthPlace = birthPlace;
        this.currentGoal = currentGoal;
        this.currentAction = currentAction;
        this.dominantEmotion = dominantEmotion;
        this.emotionalDescription = emotionalDescription;
    }


    public static VillagerDataPacket fromVillagerData(com.beeny.data.VillagerData data, int entityIdInt, UUID entityUuid) {
        // Safely handle potential null AIState and EmotionalState to avoid NPEs
        com.beeny.data.AIState aiState = data.getAiState();
        String safeCurrentGoal = (aiState != null && aiState.getCurrentGoal() != null) ? aiState.getCurrentGoal() : "";
        String safeCurrentAction = (aiState != null && aiState.getCurrentAction() != null) ? aiState.getCurrentAction() : "";

        com.beeny.data.EmotionalState emotionalState = data.getEmotionalState();
        String safeDominantEmotion = (emotionalState != null && emotionalState.getDominantEmotion() != null) ? emotionalState.getDominantEmotion() : "";
        String safeEmotionalDescription = (emotionalState != null && emotionalState.getEmotionalDescription() != null) ? emotionalState.getEmotionalDescription() : "";

        return new VillagerDataPacket(
            entityIdInt,
            entityUuid,
            data.getName(),
            data.getProfessionData().getCurrentProfession(),
            data.getHappiness(),
            data.getPersonality().name(),
            data.getHobby().name(),
            data.getAge(),
            data.getGender(),
            data.getFavoriteFood(),
            data.getNotes(),
            data.getSpouseName(),
            data.getTotalTrades(),
            data.getBirthPlace(),
            safeCurrentGoal,
            safeCurrentAction,
            safeDominantEmotion,
            safeEmotionalDescription
        );
    }
}