package com.moulberry.flashback.packet;

import net.minecraft.network.FriendlyByteBuf;

public interface FlashbackPacket {
    void write(FriendlyByteBuf buffer);
}
