package com.beeny.network;

import com.beeny.Villagersreborn;
import com.beeny.constants.StringConstants;
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
    public static final CustomPayload.Id<RequestVillagerListPacket> ID = new CustomPayload.Id<>(Identifier.of(Villagersreborn.MOD_ID, StringConstants.CH_REQUEST_VILLAGER_LIST));
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
        // Cap the number of villagers sent to the client to avoid network/perf issues.
        // 200 is a sensible upper bound for UI lists and dense villages while preventing large payloads.
        final int MAX_VILLAGERS_TO_SEND = 200;

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
        
        // Track UUIDs to detect duplicates
        java.util.Set<java.util.UUID> seenUuids = new java.util.HashSet<>();
        int duplicateCount = 0;
        
        for (VillagerEntity villager : ServerVillagerManager.getInstance().getAllTrackedVillagers()) {
            // Only include villagers in the same world as the player
            if (villager.getWorld() != player.getWorld()) continue;
            sameWorldCount++;
            
            // Check for duplicate UUIDs
            java.util.UUID villagerUuid = villager.getUuid();
            if (seenUuids.contains(villagerUuid)) {
                Villagersreborn.LOGGER.warn("[RequestVillagerListPacket] Duplicate UUID detected: {} for villager at {}",
                    villagerUuid, villager.getBlockPos());
                duplicateCount++;
            } else {
                seenUuids.add(villagerUuid);
            }
            
            VillagerData data = villager.getAttached(Villagersreborn.VILLAGER_DATA);
            if (data != null) {
                withDataCount++;
                villagerDataList.add(VillagerDataPacket.fromVillagerData(data, villager.getId(), villager.getUuid()));
                Villagersreborn.LOGGER.debug("[RequestVillagerListPacket] Including villager: {} at {}",
                    data.getName(), villager.getBlockPos());
            }
        }
        
        // Log total duplicate count
        if (duplicateCount > 0) {
            Villagersreborn.LOGGER.warn("[RequestVillagerListPacket] Found {} duplicate UUIDs in tracked villager list", duplicateCount);
        }
        
        // Truncate the list to the maximum allowed size before sending
        List<VillagerDataPacket> toSend = villagerDataList.size() > MAX_VILLAGERS_TO_SEND
                ? villagerDataList.subList(0, MAX_VILLAGERS_TO_SEND)
                : villagerDataList;

        Villagersreborn.LOGGER.info("[RequestVillagerListPacket] Found {} villagers in same world, {} with data. Sending {} (capped at {}) villagers to client.",
                sameWorldCount, withDataCount, toSend.size(), MAX_VILLAGERS_TO_SEND);

        ServerPlayNetworking.send(player, new ResponsePacket(toSend));
    }

    public static class ResponsePacket implements CustomPayload {
        public static final CustomPayload.Id<ResponsePacket> ID = new CustomPayload.Id<>(Identifier.of(Villagersreborn.MOD_ID, StringConstants.CH_VILLAGER_LIST_RESPONSE));
        public static final PacketCodec<RegistryByteBuf, ResponsePacket> CODEC = PacketCodec.of(
            (value, buf) -> {
                buf.writeInt(value.villagerDataList.size());
                for (VillagerDataPacket villagerData : value.villagerDataList) {
                    VillagerDataPacket.CODEC.encode(buf, villagerData);
                }
            },
            buf -> {
                int size = buf.readInt();
                List<VillagerDataPacket> list = new ArrayList<>();
                for (int i = 0; i < size; i++) {
                    list.add(VillagerDataPacket.CODEC.decode(buf));
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