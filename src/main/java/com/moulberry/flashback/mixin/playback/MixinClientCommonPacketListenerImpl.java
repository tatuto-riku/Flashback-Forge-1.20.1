package com.moulberry.flashback.mixin.playback;

import com.moulberry.flashback.Flashback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.game.ClientboundResourcePackPacket;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.URL;
import java.util.UUID;

@Mixin(ClientPacketListener.class)
public abstract class MixinClientCommonPacketListenerImpl {

    @Shadow
    @Final
    protected Minecraft minecraft;

    @Shadow
    @Nullable
    private static URL parseResourcePackUrl(String string) {
        return null;
    }

    /**
     * Removes the resource pack prompt screen in replays
     */
    @Inject(method = "handleResourcePack", at = @At("HEAD"), cancellable = true)
    public void handleResourcePack(ClientboundResourcePackPacket clientboundResourcePackPacket, CallbackInfo ci) {
        if (Flashback.isInReplay()) {
            PacketUtils.ensureRunningOnSameThread(clientboundResourcePackPacket, (ClientPacketListener)(Object)this, this.minecraft);

            URL uRL = parseResourcePackUrl(clientboundResourcePackPacket.getUrl());
            if (uRL != null) {
                String string = clientboundResourcePackPacket.getHash();
                this.minecraft.getDownloadedPackSource().downloadAndSelectResourcePack(uRL, string, true);
            }

            ci.cancel();
        }
    }

    @Inject(method = "handleSetEntityPassengersPacket", at = @At("HEAD"), cancellable = true)
    private void flashback$ignoreDuplicatePassengerRelationship(
            ClientboundSetPassengersPacket packet, CallbackInfo ci) {
        if (!Flashback.isInReplay() || this.minecraft.level == null) {
            return;
        }

        PacketUtils.ensureRunningOnSameThread(
                packet, (ClientPacketListener) (Object) this, this.minecraft);
        Entity vehicle = this.minecraft.level.getEntity(packet.getVehicle());
        if (vehicle == null) {
            return;
        }

        int[] passengerIds = packet.getPassengers();
        if (vehicle.getPassengers().size() != passengerIds.length) {
            return;
        }
        for (int index = 0; index < passengerIds.length; index++) {
            if (vehicle.getPassengers().get(index).getId() != passengerIds[index]) {
                return;
            }
        }

        // ClientPacketListener normally ejects and re-adds every passenger for every packet.
        // A no-op packet therefore calls IV's stopRiding hook and permanently marks its linked
        // seat as dismounted. Cancelling only an already-identical relationship is generic and
        // preserves every actual mount, dismount, and seat change.
        ci.cancel();
    }

}
