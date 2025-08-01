package com.beeny.network;

import com.beeny.Villagersreborn;
import com.beeny.data.VillagerData;
import com.beeny.system.ServerVillagerManager;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class RequestVillagerListPacket implements CustomPayload {
    public static final CustomPayload.Id<RequestVillagerListPacket> ID = new CustomPayload.Id<>(Identifier.of(Villagersreborn.MOD_ID, "request_villager_list"));
    public static final PacketCodec<RegistryByteBuf, RequestVillagerListPacket> CODEC = PacketCodec.of((value, buf) -> {}, buf -> new RequestVillagerListPacket());

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }

    public static void register() {
        PayloadTypeRegistry.playC2S().register(ID, CODEC);
        PayloadTypeRegistry.playS2C().register(ResponsePacket.ID, ResponsePacket.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ID, (payload, context) -> {
            // Send villager list back to client
            context.server().execute(() -> {
                sendVillagerList(context.player());
            });
        });
    }

    public static void sendVillagerList(ServerPlayerEntity player) {
        List<VillagerDataPacket> villagerDataList = new ArrayList<>();
        
        // Debug logging
        int totalTrackedVillagers = ServerVillagerManager.getInstance().getTrackedVillagerCount();
        Villagersreborn.LOGGER.info("[RequestVillagerListPacket] Player {} requested villager list. Total tracked villagers: {}", 
            player.getName().getString(), totalTrackedVillagers);
        
        // If we have no tracked villagers, try a manual scan of the world first
        if (totalTrackedVillagers == 0) {
            Villagersreborn.LOGGER.info("[RequestVillagerListPacket] No tracked villagers found, performing manual world scan...");
            ServerWorld world = (ServerWorld) player.getWorld();
            int scannedVillagers = 0;
            
            for (net.minecraft.entity.Entity entity : world.iterateEntities()) {
                if (entity instanceof VillagerEntity villager) {
                    scannedVillagers++;
                    VillagerData data = villager.getAttached(Villagersreborn.VILLAGER_DATA);
                    if (data == null) {
                        // Create VillagerData if it doesn't exist
                        data = new VillagerData();
                        villager.setAttached(Villagersreborn.VILLAGER_DATA, data);
                        Villagersreborn.LOGGER.info("[RequestVillagerListPacket] Created VillagerData for villager at {}", villager.getBlockPos());
                    }
                    // Track the villager
                    ServerVillagerManager.getInstance().trackVillager(villager);
                }
            }
            
            Villagersreborn.LOGGER.info("[RequestVillagerListPacket] Manual scan complete. Found {} villagers in world.", scannedVillagers);
        }
        
        // Get all tracked villagers from the ServerVillagerManager
        int sameWorldCount = 0;
        int withDataCount = 0;
        for (VillagerEntity villager : ServerVillagerManager.getInstance().getAllTrackedVillagers()) {
            // Only include villagers in the same world as the player
            if (villager.getWorld() != player.getWorld()) continue;
            sameWorldCount++;
            
            VillagerData data = villager.getAttached(Villagersreborn.VILLAGER_DATA);
            if (data != null) {
                withDataCount++;
                villagerDataList.add(new VillagerDataPacket(
                    villager.getId(),
                    data.getName(),
                    villager.getBlockPos(),
                    data.getProfessionHistory().isEmpty() ? "None" : data.getProfessionHistory().get(0),
                    data.getHappiness()
                ));
                Villagersreborn.LOGGER.debug("[RequestVillagerListPacket] Including villager: {} at {}", 
                    data.getName(), villager.getBlockPos());
            }
        }
        
        Villagersreborn.LOGGER.info("[RequestVillagerListPacket] Found {} villagers in same world, {} with data. Sending {} villagers to client.", 
            sameWorldCount, withDataCount, villagerDataList.size());
        
        ServerPlayNetworking.send(player, new ResponsePacket(villagerDataList));
    }

    public static class VillagerDataPacket {
        private final int entityId;
        private final String name;
        private final int x, y, z;
        private final String profession;
        private final int happiness;

        public VillagerDataPacket(int entityId, String name, int x, int y, int z, String profession, int happiness) {
            this.entityId = entityId;
            this.name = name;
            this.x = x;
            this.y = y;
            this.z = z;
            this.profession = profession;
            this.happiness = happiness;
        }
        
        public VillagerDataPacket(int entityId, String name, net.minecraft.util.math.BlockPos pos, String profession, int happiness) {
            this(entityId, name, pos.getX(), pos.getY(), pos.getZ(), profession, happiness);
        }

        public void toPacket(RegistryByteBuf buf) {
            buf.writeInt(entityId);
            buf.writeString(name);
            buf.writeInt(x);
            buf.writeInt(y);
            buf.writeInt(z);
            buf.writeString(profession);
            buf.writeInt(happiness);
        }

        public static VillagerDataPacket fromPacket(RegistryByteBuf buf) {
            int entityId = buf.readInt();
            String name = buf.readString();
            int x = buf.readInt();
            int y = buf.readInt();
            int z = buf.readInt();
            String profession = buf.readString();
            int happiness = buf.readInt();
            
            return new VillagerDataPacket(entityId, name, x, y, z, profession, happiness);
        }

        // Getters
        public int getEntityId() { return entityId; }
        public String getName() { return name; }
        public int getX() { return x; }
        public int getY() { return y; }
        public int getZ() { return z; }
        public String getProfession() { return profession; }
        public int getHappiness() { return happiness; }
    }

    public static class ResponsePacket implements CustomPayload {
        public static final CustomPayload.Id<ResponsePacket> ID = new CustomPayload.Id<>(Identifier.of(Villagersreborn.MOD_ID, "villager_list_response"));
        public static final PacketCodec<RegistryByteBuf, ResponsePacket> CODEC = PacketCodec.of(
            (value, buf) -> {
                buf.writeInt(value.villagerDataList.size());
                for (VillagerDataPacket villagerData : value.villagerDataList) {
                    villagerData.toPacket(buf);
                }
            },
            buf -> {
                int size = buf.readInt();
                List<VillagerDataPacket> list = new ArrayList<>();
                for (int i = 0; i < size; i++) {
                    list.add(VillagerDataPacket.fromPacket(buf));
                }
                return new ResponsePacket(list);
            }
        );

        private final List<VillagerDataPacket> villagerDataList;

        public ResponsePacket(List<VillagerDataPacket> villagerDataList) {
            this.villagerDataList = villagerDataList;
        }

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }

        public List<VillagerDataPacket> getVillagerDataList() {
            return villagerDataList;
        }
    }
}