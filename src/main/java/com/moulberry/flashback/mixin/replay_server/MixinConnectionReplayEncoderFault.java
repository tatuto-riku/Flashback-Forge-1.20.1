package com.moulberry.flashback.mixin.replay_server;

import com.moulberry.flashback.Flashback;
import com.moulberry.flashback.playback.ReplayServer;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.CodecException;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundDisconnectPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A modded packet sent while a replay is being reconstructed can reference registry state that is not
 * valid on one end of the local connection. Vanilla 1.20.1 handles a codec failure by sending a clientbound
 * disconnect packet regardless of which end failed. On the client end that packet travels in the wrong
 * direction and the replay server rejects it as an invalid packet. Replay playback already tolerates
 * unsupported recorded packets, so discard only codec failures belonging to the local replay connection.
 */
@Mixin(Connection.class)
public abstract class MixinConnectionReplayEncoderFault {

    private static final Set<String> LOGGED_CODEC_FAILURES = ConcurrentHashMap.newKeySet();
    private static final Set<String> LOGGED_CLIENT_FAILURES = ConcurrentHashMap.newKeySet();

    @Shadow
    @Final
    private PacketFlow receiving;

    @Inject(method = "exceptionCaught", at = @At("HEAD"), cancellable = true)
    private void flashback$keepReplayConnectedAfterBadOutboundPacket(ChannelHandlerContext context, Throwable exception, CallbackInfo ci) {
        Object listener = ((Connection) (Object) this).getPacketListener();
        boolean isReplayServerConnection =
            (listener instanceof ServerGamePacketListenerImpl gameListener
                && ((AccessorServerGamePacketListener) gameListener).flashback$getServer() instanceof ReplayServer)
            || (listener instanceof ServerLoginPacketListenerImpl loginListener
                && ((AccessorServerLoginPacketListener) loginListener).flashback$getServer() instanceof ReplayServer);

        // The peer of a CLIENTBOUND-receiving connection is the replay server. Codec failures
        // on this side must not invoke vanilla's server-only disconnect-packet response.
        boolean isReplayClientConnection = this.receiving == PacketFlow.CLIENTBOUND && Flashback.isInReplay();

        if (isReplayClientConnection && !(exception instanceof Error)) {
            String message = exception.getMessage();
            String key = exception.getClass().getName() + ": " + (message == null ? "(no message)" : message);
            if (LOGGED_CLIENT_FAILURES.add(key)) {
                Flashback.LOGGER.warn("Ignored a local replay-client connection failure to prevent an invalid reverse-direction disconnect: {}", key, exception);
            }
            ci.cancel();
            return;
        }

        if (!isReplayServerConnection || !flashback$isCodecFailure(exception)) {
            return;
        }

        String message = exception.getMessage();
        String key = exception.getClass().getName() + ": " + (message == null ? "(no message)" : message);
        if (LOGGED_CODEC_FAILURES.add(key)) {
            Flashback.LOGGER.warn("Dropped an incompatible payload on the local replay connection; keeping playback connected: {}", key);
        }
        ci.cancel();
    }

    @Inject(
        method = "send(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void flashback$blockReverseDirectionDisconnect(Packet<?> packet, PacketSendListener sendListener, CallbackInfo ci) {
        if (packet instanceof ClientboundDisconnectPacket
            && this.receiving == PacketFlow.CLIENTBOUND
            && Flashback.isInReplay()) {
            if (LOGGED_CLIENT_FAILURES.add("reverse-direction disconnect packet")) {
                Flashback.LOGGER.warn("Blocked an invalid reverse-direction disconnect packet on the local replay-client connection");
            }
            ci.cancel();
        }
    }

    private static boolean flashback$isCodecFailure(Throwable throwable) {
        for (Throwable cause = throwable; cause != null; cause = cause.getCause()) {
            if (cause instanceof CodecException) {
                return true;
            }
        }
        return false;
    }
}
