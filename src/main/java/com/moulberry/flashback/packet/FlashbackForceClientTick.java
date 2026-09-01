package com.moulberry.flashback.packet;

import net.minecraft.network.FriendlyByteBuf;

public class FlashbackForceClientTick implements FlashbackPacket {
    public static final FlashbackForceClientTick INSTANCE = new FlashbackForceClientTick();

    private FlashbackForceClientTick() {
    }

    @Override
    public void write(FriendlyByteBuf friendlyByteBuf) {
    }

    public static FlashbackForceClientTick decode(FriendlyByteBuf ignored) {
        return INSTANCE;
    }
}
