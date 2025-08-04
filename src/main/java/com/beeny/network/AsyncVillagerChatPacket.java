package com.beeny.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import java.util.UUID;

public record AsyncVillagerChatPacket(UUID conversationId, UUID villagerUuid, String villagerName, 
                                     UUID playerUuid, String message, boolean isFinal, long timestamp) implements CustomPayload {
    public static final Identifier ID = Identifier.of("villagersreborn", "async_villager_chat");
    public static final PacketCodec<PacketByteBuf, AsyncVillagerChatPacket> CODEC = PacketCodec.of(
        AsyncVillagerChatPacket::write,
        AsyncVillagerChatPacket::read
    );
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return new Id<>(ID);
    }

    public void write(PacketByteBuf buf) {
        buf.writeUuid(this.conversationId());
        buf.writeUuid(this.villagerUuid());
        buf.writeString(this.villagerName());
        buf.writeUuid(this.playerUuid());
        buf.writeString(this.message());
        buf.writeBoolean(this.isFinal());
        buf.writeLong(this.timestamp());
    }

    public static void write(PacketByteBuf buf, AsyncVillagerChatPacket packet) {
        buf.writeUuid(packet.conversationId());
        buf.writeUuid(packet.villagerUuid());
        buf.writeString(packet.villagerName());
        buf.writeUuid(packet.playerUuid());
        buf.writeString(packet.message());
        buf.writeBoolean(packet.isFinal());
        buf.writeLong(packet.timestamp());
    }

    public static AsyncVillagerChatPacket read(PacketByteBuf buf) {
        return new AsyncVillagerChatPacket(
            buf.readUuid(),
            buf.readUuid(),
            buf.readString(),
            buf.readUuid(),
            buf.readString(),
            buf.readBoolean(),
            buf.readLong()
        );
    }
}