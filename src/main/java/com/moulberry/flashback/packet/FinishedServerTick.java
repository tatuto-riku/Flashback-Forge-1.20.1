package com.moulberry.flashback.packet;

import net.minecraft.network.FriendlyByteBuf;

public class FinishedServerTick implements FlashbackPacket {
    public static final FinishedServerTick INSTANCE = new FinishedServerTick();

    private FinishedServerTick() {
    }

    @Override
    public void write(FriendlyByteBuf friendlyByteBuf) {
    }

    public static FinishedServerTick decode(FriendlyByteBuf ignored) {
        return INSTANCE;
    }
}
