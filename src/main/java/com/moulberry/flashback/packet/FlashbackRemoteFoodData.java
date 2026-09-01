package com.moulberry.flashback.packet;

import net.minecraft.network.FriendlyByteBuf;

public record FlashbackRemoteFoodData(int entityId, int foodLevel, float saturationLevel) implements FlashbackPacket {
    public static FlashbackRemoteFoodData decode(FriendlyByteBuf friendlyByteBuf) {
        int entityId = friendlyByteBuf.readVarInt();
        int foodLevel = friendlyByteBuf.readVarInt();
        float saturationLevel = friendlyByteBuf.readFloat();
        return new FlashbackRemoteFoodData(entityId, foodLevel, saturationLevel);
    }

    @Override
    public void write(FriendlyByteBuf friendlyByteBuf) {
        friendlyByteBuf.writeVarInt(this.entityId);
        friendlyByteBuf.writeVarInt(this.foodLevel);
        friendlyByteBuf.writeFloat(this.saturationLevel);
    }

}
