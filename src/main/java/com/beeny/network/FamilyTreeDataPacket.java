package com.beeny.network;

import com.beeny.Villagersreborn;
import com.beeny.data.VillagerData;
import com.beeny.system.ServerVillagerManager;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class FamilyTreeDataPacket implements CustomPayload {
    public static final CustomPayload.Id<FamilyTreeDataPacket> ID = new CustomPayload.Id<>(Identifier.of(Villagersreborn.MOD_ID, "family_tree_data"));
    public static final PacketCodec<RegistryByteBuf, FamilyTreeDataPacket> CODEC = PacketCodec.of(
        (value, buf) -> {
            buf.writeLong(value.villagerId);
            buf.writeInt(value.familyMembers.size());
            for (FamilyMemberData member : value.familyMembers) {
                member.toPacket(buf);
            }
        },
        buf -> {
            long villagerId = buf.readLong();
            int size = buf.readInt();
            List<FamilyMemberData> members = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                members.add(FamilyMemberData.fromPacket(buf));
            }
            return new FamilyTreeDataPacket(villagerId, members);
        }
    );

    private final long villagerId;
    private final List<FamilyMemberData> familyMembers;

    public FamilyTreeDataPacket(long villagerId, List<FamilyMemberData> familyMembers) {
        this.villagerId = villagerId;
        this.familyMembers = familyMembers;
    }

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
    
    public long getVillagerId() {
        return villagerId;
    }
    
    public List<FamilyMemberData> getFamilyMembers() {
        return familyMembers;
    }

    public static void register() {
        PayloadTypeRegistry.playC2S().register(RequestPacket.ID, RequestPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(ID, CODEC);
        ServerPlayNetworking.registerGlobalReceiver(RequestPacket.ID, (payload, context) -> {
            // Send family tree data back to client
            context.server().execute(() -> {
                sendFamilyTreeData(context.player(), payload.getVillagerId());
            });
        });
    }

    public static void sendFamilyTreeData(ServerPlayerEntity player, long villagerId) {
        List<FamilyMemberData> familyMembers = new ArrayList<>();
        
        // Get the villager entity
        ServerWorld world = (ServerWorld) player.getWorld();
        Entity entity = world.getEntityById((int) villagerId);
        
        if (entity instanceof VillagerEntity villager) {
            VillagerData villagerData = villager.getAttached(Villagersreborn.VILLAGER_DATA);
            if (villagerData != null) {
                // Add the current villager
                familyMembers.add(createFamilyMemberData(villager, villagerData, "CURRENT"));
                
                // Add spouse if exists
                if (!villagerData.getSpouseId().isEmpty()) {
                    VillagerEntity spouse = ServerVillagerManager.getInstance().getVillager(
                        java.util.UUID.fromString(villagerData.getSpouseId()));
                    if (spouse != null) {
                        VillagerData spouseData = spouse.getAttached(Villagersreborn.VILLAGER_DATA);
                        if (spouseData != null) {
                            familyMembers.add(createFamilyMemberData(spouse, spouseData, "SPOUSE"));
                        }
                    }
                }
                
                // Add children
                for (String childId : villagerData.getChildrenIds()) {
                    VillagerEntity child = ServerVillagerManager.getInstance().getVillager(
                        java.util.UUID.fromString(childId));
                    if (child != null) {
                        VillagerData childData = child.getAttached(Villagersreborn.VILLAGER_DATA);
                        if (childData != null) {
                            familyMembers.add(createFamilyMemberData(child, childData, "CHILD"));
                        }
                    }
                }
                
                // Add parents (find villagers whose children include this villager)
                for (VillagerEntity potentialParent : ServerVillagerManager.getInstance().getAllTrackedVillagers()) {
                    if (potentialParent.getWorld() != world) continue;
                    
                    VillagerData parentData = potentialParent.getAttached(Villagersreborn.VILLAGER_DATA);
                    if (parentData != null && parentData.getChildrenIds().contains(villager.getUuidAsString())) {
                        familyMembers.add(createFamilyMemberData(potentialParent, parentData, "PARENT"));
                    }
                }
                
                // Add siblings (find villagers who share the same parents)
                for (VillagerEntity potentialParent : ServerVillagerManager.getInstance().getAllTrackedVillagers()) {
                    if (potentialParent.getWorld() != world) continue;
                    
                    VillagerData parentData = potentialParent.getAttached(Villagersreborn.VILLAGER_DATA);
                    if (parentData != null &&
                        parentData.getChildrenIds().contains(villager.getUuidAsString())) {
                        // This is a parent, now find other children
                        for (String siblingId : parentData.getChildrenIds()) {
                            if (siblingId.equals(villager.getUuidAsString())) continue; // Skip self
                            
                            VillagerEntity sibling = ServerVillagerManager.getInstance().getVillager(
                                java.util.UUID.fromString(siblingId));
                            if (sibling != null) {
                                VillagerData siblingData = sibling.getAttached(Villagersreborn.VILLAGER_DATA);
                                if (siblingData != null) {
                                    familyMembers.add(createFamilyMemberData(sibling, siblingData, "SIBLING"));
                                }
                            }
                        }
                    }
                }
            }
        }
        
        ServerPlayNetworking.send(player, new FamilyTreeDataPacket(villagerId, familyMembers));
    }
    
    private static FamilyMemberData createFamilyMemberData(VillagerEntity villager, VillagerData data, String relationship) {
        // Derive alive state from entity presence instead of stored flag to avoid desync
        boolean derivedAlive = villager.isAlive();
        return new FamilyMemberData(
            data.getName(),
            villager.getUuidAsString(),
            relationship,
            data.getBirthTime(),
            data.getDeathTime(),
            derivedAlive,
            data.getPersonality().name(),
            data.getHappiness(),
            !data.getProfessionHistory().isEmpty() ? data.getProfessionHistory().get(0) : "",
            data.getSpouseName(),
            data.getChildrenNames().size(),
            data.getBirthPlace(),
            data.getNotes()
        );
    }

    public static class FamilyMemberData {
        private final String name;
        private final String uuid;
        private final String relationship;
        private final long birthTime;
        private final long deathTime;
        private final boolean isAlive;
        private final String personality;
        private final int happiness;
        private final String profession;
        private final String spouseName;
        private final int childrenCount;
        private final String birthPlace;
        private final String notes;

        public FamilyMemberData(String name, String uuid, String relationship, long birthTime, long deathTime,
                               boolean isAlive, String personality, int happiness, String profession,
                               String spouseName, int childrenCount, String birthPlace, String notes) {
            this.name = name;
            this.uuid = uuid;
            this.relationship = relationship;
            this.birthTime = birthTime;
            this.deathTime = deathTime;
            this.isAlive = isAlive;
            this.personality = personality;
            this.happiness = happiness;
            this.profession = profession;
            this.spouseName = spouseName;
            this.childrenCount = childrenCount;
            this.birthPlace = birthPlace;
            this.notes = notes;
        }

        public void toPacket(RegistryByteBuf buf) {
            buf.writeString(name);
            buf.writeString(uuid);
            buf.writeString(relationship);
            buf.writeLong(birthTime);
            buf.writeLong(deathTime);
            buf.writeBoolean(isAlive);
            buf.writeString(personality);
            buf.writeInt(happiness);
            buf.writeString(profession);
            buf.writeString(spouseName);
            buf.writeInt(childrenCount);
            buf.writeString(birthPlace);
            buf.writeString(notes);
        }

        public static FamilyMemberData fromPacket(RegistryByteBuf buf) {
            String name = buf.readString();
            String uuid = buf.readString();
            String relationship = buf.readString();
            long birthTime = buf.readLong();
            long deathTime = buf.readLong();
            boolean isAlive = buf.readBoolean();
            String personality = buf.readString();
            int happiness = buf.readInt();
            String profession = buf.readString();
            String spouseName = buf.readString();
            int childrenCount = buf.readInt();
            String birthPlace = buf.readString();
            String notes = buf.readString();

            return new FamilyMemberData(name, uuid, relationship, birthTime, deathTime, isAlive,
                    personality, happiness, profession, spouseName, childrenCount, birthPlace, notes);
        }

        // Getters
        public String getName() { return name; }
        public String getUuid() { return uuid; }
        public String getRelationship() { return relationship; }
        public long getBirthTime() { return birthTime; }
        public long getDeathTime() { return deathTime; }
        public boolean isAlive() { return isAlive; }
        public String getPersonality() { return personality; }
        public int getHappiness() { return happiness; }
        public String getProfession() { return profession; }
        public String getSpouseName() { return spouseName; }
        public int getChildrenCount() { return childrenCount; }
        public String getBirthPlace() { return birthPlace; }
        public String getNotes() { return notes; }
    }

    public static class RequestPacket implements CustomPayload {
        public static final CustomPayload.Id<RequestPacket> ID = new CustomPayload.Id<>(Identifier.of(Villagersreborn.MOD_ID, "family_tree_request"));
        public static final PacketCodec<RegistryByteBuf, RequestPacket> CODEC = PacketCodec.of(
            (value, buf) -> buf.writeLong(value.villagerId),
            buf -> new RequestPacket(buf.readLong())
        );

        private final long villagerId;

        public RequestPacket(long villagerId) {
            this.villagerId = villagerId;
        }

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }

        public long getVillagerId() {
            return villagerId;
        }
    }
}