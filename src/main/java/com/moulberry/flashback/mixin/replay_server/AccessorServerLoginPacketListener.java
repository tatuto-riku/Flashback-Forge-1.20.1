package com.moulberry.flashback.mixin.replay_server;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerLoginPacketListenerImpl.class)
public interface AccessorServerLoginPacketListener {
    @Accessor("server")
    MinecraftServer flashback$getServer();
}
