package com.moulberry.flashback.mixin.replay_server;

import com.moulberry.flashback.Flashback;
import com.moulberry.flashback.playback.ReplayServer;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.EncoderException;
import net.minecraft.network.Connection;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A packet built for a real spectator during replay-server join synchronization can reference state
 * (e.g. modded registry entries) that isn't valid to encode on the connection yet. Vanilla's default
 * exceptionCaught behaviour disconnects the channel on any encoder failure, which for a ReplayServer
 * connection surfaces to the client as an unexpected "invalid packet" disconnect. Dropping the offending
 * packet and keeping the connection alive matches how the rest of replay playback tolerates bad data.
 */
@Mixin(Connection.class)
public abstract class MixinConnectionReplayEncoderFault {

    private static final Set<String> LOGGED_ENCODER_FAILURES = ConcurrentHashMap.newKeySet();

    @Inject(method = "exceptionCaught", at = @At("HEAD"), cancellable = true)
    private void flashback$keepReplayConnectedAfterBadOutboundPacket(ChannelHandlerContext context, Throwable exception, CallbackInfo ci) {
        if (!(exception instanceof EncoderException)) {
            return;
        }

        Object listener = ((Connection) (Object) this).getPacketListener();
        boolean isReplayServerConnection =
            (listener instanceof ServerGamePacketListenerImpl gameListener
                && ((AccessorServerGamePacketListener) gameListener).flashback$getServer() instanceof ReplayServer)
            || (listener instanceof ServerLoginPacketListenerImpl loginListener
                && ((AccessorServerLoginPacketListener) loginListener).flashback$getServer() instanceof ReplayServer);

        if (!isReplayServerConnection) {
            return;
        }

        String message = exception.getMessage();
        String key = message == null ? exception.getClass().getName() : message;
        if (LOGGED_ENCODER_FAILURES.add(key)) {
            Flashback.LOGGER.warn("Dropped an unencodable payload during replay-server join synchronization; keeping the connection alive: {}", key);
        }
        ci.cancel();
    }
}
