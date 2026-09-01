package com.moulberry.flashback.packet;

import net.minecraft.network.FriendlyByteBuf;

public class FlashbackClearParticles implements FlashbackPacket {
    public static final FlashbackClearParticles INSTANCE = new FlashbackClearParticles();

    private FlashbackClearParticles() {
    }

    @Override
    public void write(FriendlyByteBuf friendlyByteBuf) {
    }

    public static FlashbackClearParticles decode(FriendlyByteBuf ignored) {
        return INSTANCE;
    }
}
