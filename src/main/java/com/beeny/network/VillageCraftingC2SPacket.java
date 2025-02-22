package com.beeny.network;

import com.beeny.village.VillageCraftingManager;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.c2s.play.CustomPayloadC2SPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import io.netty.buffer.Unpooled;
import java.util.UUID;

public class VillageCraftingC2SPacket extends CustomPayloadC2SPacket {
    private static final Identifier CHANNEL = new Identifier("villagesreborn", "craft");
    private final UUID villagerUuid;
    private final String recipeId;

    public VillageCraftingC2SPacket(UUID villagerUuid, String recipeId) {
        super(CHANNEL, createPayload(villagerUuid, recipeId));
        this.villagerUuid = villagerUuid;
        this.recipeId = recipeId;
    }

    private static PacketByteBuf createPayload(UUID villagerUuid, String recipeId) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeUuid(villagerUuid);
        buf.writeString(recipeId);
        return buf;
    }

    public static VillageCraftingC2SPacket read(PacketByteBuf buf) {
        return new VillageCraftingC2SPacket(buf.readUuid(), buf.readString());
    }

    public void apply(ServerPlayerEntity player) {
        VillagerEntity villager = (VillagerEntity) player.getWorld().getEntity(this.villagerUuid);
        if (villager != null) {
            VillageCraftingManager.getInstance().assignTask(villager, this.recipeId, player);
        }
    }

    public static void register() {
        // Registration will happen in the mod initializer
    }
}