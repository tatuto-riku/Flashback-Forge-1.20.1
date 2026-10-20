package com.moulberry.flashback.packet;

import net.minecraft.network.FriendlyByteBuf;

/** Clears client-only replay entities which no longer exist after a snapshot rewind. */
public final class FlashbackClearEntities implements FlashbackPacket {

    public static final FlashbackClearEntities INSTANCE = new FlashbackClearEntities();

    private FlashbackClearEntities() {
    }

    @Override
    public void write(FriendlyByteBuf ignored) {
    }

    public static FlashbackClearEntities decode(FriendlyByteBuf ignored) {
        return INSTANCE;
    }
}
