package com.moulberry.flashback.packet;

import net.minecraft.network.FriendlyByteBuf;

public record FlashbackRemoteExperience(int entityId, float experienceProgress, int totalExperience, int experienceLevel) implements FlashbackPacket {
    public static FlashbackRemoteExperience decode(FriendlyByteBuf friendlyByteBuf) {
        int entityId = friendlyByteBuf.readVarInt();
        float experienceProgress = friendlyByteBuf.readFloat();
        int totalExperience = friendlyByteBuf.readVarInt();
        int experienceLevel = friendlyByteBuf.readVarInt();
        return new FlashbackRemoteExperience(entityId, experienceProgress, totalExperience, experienceLevel);
    }

    @Override
    public void write(FriendlyByteBuf friendlyByteBuf) {
        friendlyByteBuf.writeVarInt(this.entityId);
        friendlyByteBuf.writeFloat(this.experienceProgress);
        friendlyByteBuf.writeVarInt(this.totalExperience);
        friendlyByteBuf.writeVarInt(this.experienceLevel);
    }

}
