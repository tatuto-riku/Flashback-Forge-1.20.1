package com.moulberry.flashback.packet;

import net.minecraft.network.FriendlyByteBuf;

public record FlashbackRemoteSelectHotbarSlot(int entityId, int slot) implements FlashbackPacket {
    public static FlashbackRemoteSelectHotbarSlot decode(FriendlyByteBuf friendlyByteBuf) {
        int entityId = friendlyByteBuf.readVarInt();
        int slot = friendlyByteBuf.readByte();
        return new FlashbackRemoteSelectHotbarSlot(entityId, slot);
    }

    @Override
    public void write(FriendlyByteBuf friendlyByteBuf) {
        friendlyByteBuf.writeVarInt(this.entityId);
        friendlyByteBuf.writeByte(this.slot);
    }

}
