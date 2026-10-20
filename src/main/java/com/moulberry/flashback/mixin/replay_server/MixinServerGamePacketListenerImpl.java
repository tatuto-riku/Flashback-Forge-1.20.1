package com.moulberry.flashback.mixin.replay_server;

import com.moulberry.flashback.playback.ReplayServer;
import net.minecraft.Util;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replay playback uses a local in-memory server and can deliberately pause or spend a long
 * time seeking. Keep-alive timeouts are therefore neither useful nor safe: timeout mods can
 * make the server send a disconnect while the local channel is changing replay state.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class MixinServerGamePacketListenerImpl {

    @Shadow
    @Final
    private MinecraftServer server;

    @Shadow
    private long keepAliveTime;

    @Shadow
    private boolean keepAlivePending;

    @Inject(method = "tick", at = @At("HEAD"))
    private void flashback$disableReplayKeepAliveTimeout(CallbackInfo ci) {
        if (this.server instanceof ReplayServer) {
            this.keepAliveTime = Util.getMillis();
            this.keepAlivePending = false;
        }
    }
}
