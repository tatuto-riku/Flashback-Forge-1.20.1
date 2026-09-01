package com.moulberry.flashback.mixin.replay_server;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerGamePacketListenerImpl.class)
public interface AccessorServerGamePacketListener {
    @Accessor("server")
    MinecraftServer flashback$getServer();
}
