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
                villagerDataList.add(VillagerDataPacket.fromVillagerData(data, villager.getUuid()));
                Villagersreborn.LOGGER.debug("[RequestVillagerListPacket] Including villager: {} at {}", 
                    data.getName(), villager.getBlockPos());
            }
        }
        
        Villagersreborn.LOGGER.info("[RequestVillagerListPacket] Found {} villagers in same world, {} with data. Sending {} villagers to client.", 
            sameWorldCount, withDataCount, villagerDataList.size());
        
        ServerPlayNetworking.send(player, new ResponsePacket(villagerDataList));
    }

    public static class ResponsePacket implements CustomPayload {
        public static final CustomPayload.Id<ResponsePacket> ID = new CustomPayload.Id<>(Identifier.of(Villagersreborn.MOD_ID, "villager_list_response"));
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