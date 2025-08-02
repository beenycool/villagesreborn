package com.beeny.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import java.util.UUID;

public class AsyncVillagerChatPacket {
    public static final Identifier ID = Identifier.of("villagersreborn", "async_villager_chat");
    
    private final UUID conversationId;
    private final UUID villagerUuid;
    private final String villagerName;
    private final UUID playerUuid;
    private final String message;
    private final boolean isFinal;
    private final long timestamp;

    public AsyncVillagerChatPacket(UUID conversationId, UUID villagerUuid, String villagerName, 
                                  UUID playerUuid, String message, boolean isFinal, long timestamp) {
        this.conversationId = conversationId;
        this.villagerUuid = villagerUuid;
        this.villagerName = villagerName;
        this.playerUuid = playerUuid;
        this.message = message;
        this.isFinal = isFinal;
        this.timestamp = timestamp;
    }

    public void write(PacketByteBuf buf) {
        buf.writeUuid(conversationId);
        buf.writeUuid(villagerUuid);
        buf.writeString(villagerName);
        buf.writeUuid(playerUuid);
        buf.writeString(message);
        buf.writeBoolean(isFinal);
        buf.writeLong(timestamp);
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

    // Getters
    public UUID getConversationId() { return conversationId; }
    public UUID getVillagerUuid() { return villagerUuid; }
    public String getVillagerName() { return villagerName; }
    public UUID getPlayerUuid() { return playerUuid; }
    public String getMessage() { return message; }
    public boolean isFinal() { return isFinal; }
    public long getTimestamp() { return timestamp; }
}