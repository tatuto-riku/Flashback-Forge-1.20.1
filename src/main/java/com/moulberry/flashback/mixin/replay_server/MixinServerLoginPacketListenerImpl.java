package com.moulberry.flashback.mixin.replay_server;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.moulberry.flashback.playback.ReplayServer;
import net.minecraft.network.protocol.login.ClientboundGameProfilePacket;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Mixin(ServerLoginPacketListenerImpl.class)
public abstract class MixinServerLoginPacketListenerImpl {

    @Shadow
    @Final
    MinecraftServer server;

    @Shadow
    ServerLoginPacketListenerImpl.State state;
    @Shadow
    @Nullable
    GameProfile gameProfile;

    @Inject(method = "handleHello", at = @At("HEAD"), cancellable = true)
    public void handleHello(ServerboundHelloPacket serverboundHelloPacket, CallbackInfo ci) {
        if (this.server instanceof ReplayServer) {
//            this.requestedUsername = ReplayServer.REPLAY_VIEWER_NAME;
            UUID replayViewerUUID = UUID.nameUUIDFromBytes(serverboundHelloPacket.name().getBytes(StandardCharsets.UTF_8));
            GameProfile gameProfile = new GameProfile(replayViewerUUID, ReplayServer.REPLAY_VIEWER_NAME);
            gameProfile.getProperties().put("IsReplayViewer", new Property("IsReplayViewer", "True"));
            this.gameProfile = gameProfile;
            // Forge's patched tick() drives ServerboundHelloPacket handling through its own mod-list/channel
            // negotiation (NetworkHooks.tickNegotiation) before advancing NEGOTIATING -> READY_TO_ACCEPT. Jumping
            // straight to READY_TO_ACCEPT here skips that negotiation, leaving the client's FML handshake state
            // out of sync with the server's — it then gets disconnected with a PLAY-protocol packet while still
            // in the LOGIN protocol phase, which crashes with a ClassCastException on the client.
            this.state = ServerLoginPacketListenerImpl.State.NEGOTIATING;
            ci.cancel();
        }
    }

    // Disable strict error handling
    // TODO astavie: is this required for 1.20.1?
//    @WrapOperation(method = "finishLoginAndWaitForClient", at = @At(value = "NEW", target = "net/minecraft/network/protocol/login/ClientboundGameProfilePacket"))
//    public ClientboundGameProfilePacket wrapCreateClientboundGameProfilePacket(GameProfile gameProfile, boolean strict, Operation<ClientboundGameProfilePacket> original) {
//        if (this.server instanceof ReplayServer) {
//            strict = false;
//        }
//        return original.call(gameProfile, strict);
//    }

}
