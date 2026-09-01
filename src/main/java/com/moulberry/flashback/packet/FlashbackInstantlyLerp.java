package com.moulberry.flashback.packet;

import net.minecraft.network.FriendlyByteBuf;

public class FlashbackInstantlyLerp implements FlashbackPacket {
    public static final FlashbackInstantlyLerp INSTANCE = new FlashbackInstantlyLerp();

    private FlashbackInstantlyLerp() {
    }

    @Override
    public void write(FriendlyByteBuf friendlyByteBuf) {
    }

    public static FlashbackInstantlyLerp decode(FriendlyByteBuf ignored) {
        return INSTANCE;
    }
}
