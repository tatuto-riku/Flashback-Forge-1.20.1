package com.moulberry.flashback;

import com.moulberry.flashback.packet.FlashbackPacket;
import net.minecraft.server.level.ServerPlayer;

/** Source-compatible send facade used by the replay server after removing Fabric API. */
public final class ServerPlayNetworking {

    private ServerPlayNetworking() {
    }

    public static void send(ServerPlayer player, FlashbackPacket packet) {
        FlashbackNetwork.sendTo(player, packet);
    }
}
