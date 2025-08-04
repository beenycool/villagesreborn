package com.beeny.network;

import com.beeny.constants.VillagerConstants;
import com.beeny.constants.VillagerConstants.PersonalityType;
import com.beeny.constants.VillagerConstants.HobbyType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.Uuids;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class VillagerDataPacket {
    public static final PacketCodec<PacketByteBuf, VillagerDataPacket> CODEC = PacketCodec.tuple(
        Uuids.PACKET_CODEC, VillagerDataPacket::getEntityId,
        PacketCodecs.STRING, VillagerDataPacket::getName,
        PacketCodecs.STRING, VillagerDataPacket::getProfession,
        PacketCodecs.INTEGER, VillagerDataPacket::getHappiness,
        PacketCodecs.STRING, VillagerDataPacket::getPersonality,
        PacketCodecs.STRING, VillagerDataPacket::getHobby,
        PacketCodecs.INTEGER, VillagerDataPacket::getAge,
        PacketCodecs.STRING, VillagerDataPacket::getGender,
        PacketCodecs.STRING, VillagerDataPacket::getFavoriteFood,
        PacketCodecs.STRING, VillagerDataPacket::getNotes,
        PacketCodecs.STRING, VillagerDataPacket::getSpouseName,
        PacketCodecs.INTEGER, VillagerDataPacket::getTotalTrades,
        PacketCodecs.STRING, VillagerDataPacket::getBirthPlace,
        PacketCodecs.STRING, VillagerDataPacket::getCurrentGoal,
        PacketCodecs.STRING, VillagerDataPacket::getCurrentAction,
        PacketCodecs.STRING, VillagerDataPacket::getDominantEmotion,
        PacketCodecs.STRING, VillagerDataPacket::getEmotionalDescription,
        VillagerDataPacket::new
    );

    private final UUID entityId;
    private final String name;
    private final String profession;
    private final int happiness;
    private final String personality;
    private final String hobby;
    private final int age;
    private final String gender;
    private final String favoriteFood;
    private final String notes;
    private final String spouseName;
    private final int totalTrades;
    private final String birthPlace;
    private final String currentGoal;
    private final String currentAction;
    private final String dominantEmotion;
    private final String emotionalDescription;

    public VillagerDataPacket(UUID entityId, String name, String profession, int happiness, String personality,
                            String hobby, int age, String gender, String favoriteFood, String notes,
                            String spouseName, int totalTrades, String birthPlace, String currentGoal,
                            String currentAction, String dominantEmotion, String emotionalDescription) {
        this.entityId = entityId;
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

    public UUID getEntityId() {
        return entityId;
    }

    public String getName() {
        return name;
    }

    public String getProfession() {
        return profession;
    }

    public int getHappiness() {
        return happiness;
    }

    public String getPersonality() {
        return personality;
    }

    public String getHobby() {
        return hobby;
    }

    public int getAge() {
        return age;
    }

    public String getGender() {
        return gender;
    }

    public String getFavoriteFood() {
        return favoriteFood;
    }

    public String getNotes() {
        return notes;
    }

    public String getSpouseName() {
        return spouseName;
    }

    public int getTotalTrades() {
        return totalTrades;
    }

    public String getBirthPlace() {
        return birthPlace;
    }

    public String getCurrentGoal() {
        return currentGoal;
    }

    public String getCurrentAction() {
        return currentAction;
    }

    public String getDominantEmotion() {
        return dominantEmotion;
    }

    public String getEmotionalDescription() {
        return emotionalDescription;
    }

    public static VillagerDataPacket fromVillagerData(com.beeny.data.VillagerData data, UUID entityId) {
        return new VillagerDataPacket(
            entityId,
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
            data.getAiState().getCurrentGoal(),
            data.getAiState().getCurrentAction(),
            data.getEmotionalState().getDominantEmotion(),
            data.getEmotionalState().getEmotionalDescription()
        );
    }
}