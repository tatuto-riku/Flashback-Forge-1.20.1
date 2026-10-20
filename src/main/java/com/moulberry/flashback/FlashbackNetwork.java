package com.moulberry.flashback;

import com.moulberry.flashback.packet.FinishedServerTick;
import com.moulberry.flashback.packet.FlashbackAccurateEntityPosition;
import com.moulberry.flashback.packet.FlashbackClearEntities;
import com.moulberry.flashback.packet.FlashbackClearParticles;
import com.moulberry.flashback.packet.FlashbackForceClientTick;
import com.moulberry.flashback.packet.FlashbackInstantlyLerp;
import com.moulberry.flashback.packet.FlashbackPacket;
import com.moulberry.flashback.packet.FlashbackRemoteExperience;
import com.moulberry.flashback.packet.FlashbackRemoteFoodData;
import com.moulberry.flashback.packet.FlashbackRemoteSelectHotbarSlot;
import com.moulberry.flashback.packet.FlashbackRemoteSetSlot;
import com.moulberry.flashback.packet.FlashbackRecheckBlocks;
import com.moulberry.flashback.packet.FlashbackVoiceChatSound;
import com.moulberry.flashback.compat.simple_voice_chat.SimpleVoiceChatPlayback;
import com.moulberry.flashback.ext.MinecraftExt;
import com.moulberry.flashback.ext.ClientLevelExt;
import com.moulberry.flashback.playback.ReplayTimer;
import com.moulberry.flashback.playback.ReplayClientTick;
import com.moulberry.flashback.visuals.AccurateEntityPositionHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/** Forge SimpleChannel replacement for Fabric's typed play payloads. */
public final class FlashbackNetwork {

    private static final String PROTOCOL = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
        .named(Flashback.createResourceLocation("main"))
        .networkProtocolVersion(() -> PROTOCOL)
        .clientAcceptedVersions(PROTOCOL::equals)
        .serverAcceptedVersions(PROTOCOL::equals)
        .simpleChannel();

    private static int nextId;

    private FlashbackNetwork() {
    }

    public static void register() {
        CHANNEL.messageBuilder(FlashbackForceClientTick.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(FlashbackForceClientTick::write)
            .decoder(FlashbackForceClientTick::decode)
            .consumerMainThread((packet, context) -> {
                if (Flashback.isInReplay()) {
                    tickClientOnceUnfrozen();
                }
                context.get().setPacketHandled(true);
            }).add();

        CHANNEL.messageBuilder(FlashbackClearParticles.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(FlashbackClearParticles::write)
            .decoder(FlashbackClearParticles::decode)
            .consumerMainThread((packet, context) -> {
                if (Flashback.isInReplay()) {
                    Minecraft.getInstance().particleEngine.clearParticles();
                }
                context.get().setPacketHandled(true);
            }).add();

        CHANNEL.messageBuilder(FlashbackClearEntities.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(FlashbackClearEntities::write)
            .decoder(FlashbackClearEntities::decode)
            .consumerMainThread((packet, context) -> {
                Minecraft minecraft = Minecraft.getInstance();
                if (Flashback.isInReplay() && minecraft.level != null) {
                    ((ClientLevelExt) minecraft.level).flashback$clearQueuedLightUpdates();
                    // Snapshot replay replaces all non-player entities. The vanilla tracker can
                    // leave entities such as dropped items alive on the physical client until a
                    // later tracking pass, so remove them before the snapshot spawns arrive.
                    java.util.List<Entity> entities = new java.util.ArrayList<>();
                    minecraft.level.getEntities().getAll().forEach(entities::add);
                    for (Entity entity : entities) {
                        if (!(entity instanceof net.minecraft.world.entity.player.Player)) {
                            entity.discard();
                        }
                    }
                }
                context.get().setPacketHandled(true);
            }).add();

        CHANNEL.messageBuilder(FlashbackRecheckBlocks.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(FlashbackRecheckBlocks::write)
            .decoder(FlashbackRecheckBlocks::decode)
            .consumerMainThread((packet, context) -> {
                Minecraft minecraft = Minecraft.getInstance();
                if (Flashback.isInReplay() && minecraft.level != null) {
                    ((ClientLevelExt) minecraft.level)
                            .flashback$queueLightRechecks(packet.positions());
                }
                context.get().setPacketHandled(true);
            }).add();

        CHANNEL.messageBuilder(FlashbackInstantlyLerp.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(FlashbackInstantlyLerp::write)
            .decoder(FlashbackInstantlyLerp::decode)
            .consumerMainThread((packet, context) -> {
                Minecraft minecraft = Minecraft.getInstance();
                if (Flashback.isInReplay() && minecraft.level != null) {
                    // Full chunk/light packets queue their renderer invalidation in ClientLevel.
                    // Drain only after every packet for this snapshot has arrived, so paused
                    // replays cannot render halfway through the queue and a later seek cannot
                    // apply stale light over the new block state.
                    ((ClientLevelExt) minecraft.level).flashback$drainQueuedLightUpdates();
                    ((ClientLevelExt) minecraft.level).flashback$applyQueuedLightRechecks();
                    for (Entity entity : minecraft.level.entitiesForRendering()) {
                        if (entity instanceof LivingEntity livingEntity
                                && !entity.isRemoved() && !(entity instanceof LocalPlayer)) {
                            // 1.20.1 stores the network interpolation target on LivingEntity.
                            // Snap to that target and consume the interpolation instead of merely
                            // resetting the previous render position while the entity keeps sliding.
                            if (livingEntity.lerpSteps > 0) {
                                entity.moveTo(livingEntity.lerpX, livingEntity.lerpY,
                                        livingEntity.lerpZ, (float) livingEntity.lerpYRot,
                                        (float) livingEntity.lerpXRot);
                            }
                            livingEntity.lerpSteps = 0;
                            livingEntity.lerpHeadSteps = 0;
                        }
                        entity.setOldPosAndRot();
                    }
                }
                context.get().setPacketHandled(true);
            }).add();

        CHANNEL.messageBuilder(FlashbackRemoteSelectHotbarSlot.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(FlashbackRemoteSelectHotbarSlot::write)
            .decoder(FlashbackRemoteSelectHotbarSlot::decode)
            .consumerMainThread((packet, context) -> {
                Entity entity = replayEntity(packet.entityId());
                if (entity instanceof Player player) {
                    player.getInventory().selected = packet.slot();
                }
                context.get().setPacketHandled(true);
            }).add();

        CHANNEL.messageBuilder(FlashbackRemoteExperience.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(FlashbackRemoteExperience::write)
            .decoder(FlashbackRemoteExperience::decode)
            .consumerMainThread((packet, context) -> {
                Entity entity = replayEntity(packet.entityId());
                if (entity instanceof Player player) {
                    player.experienceProgress = packet.experienceProgress();
                    player.totalExperience = packet.totalExperience();
                    player.experienceLevel = packet.experienceLevel();
                }
                context.get().setPacketHandled(true);
            }).add();

        CHANNEL.messageBuilder(FlashbackRemoteFoodData.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(FlashbackRemoteFoodData::write)
            .decoder(FlashbackRemoteFoodData::decode)
            .consumerMainThread((packet, context) -> {
                Entity entity = replayEntity(packet.entityId());
                if (entity instanceof Player player) {
                    player.getFoodData().setFoodLevel(packet.foodLevel());
                    player.getFoodData().setSaturation(packet.saturationLevel());
                }
                context.get().setPacketHandled(true);
            }).add();

        CHANNEL.messageBuilder(FlashbackRemoteSetSlot.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(FlashbackRemoteSetSlot::write)
            .decoder(FlashbackRemoteSetSlot::decode)
            .consumerMainThread((packet, context) -> {
                Entity entity = replayEntity(packet.entityId());
                if (entity instanceof Player player) {
                    player.getInventory().setItem(packet.slot(), packet.itemStack());
                }
                context.get().setPacketHandled(true);
            }).add();

        // The payload itself has no hard dependency on Simple Voice Chat, so keeping its
        // discriminator stable is safe even when the optional mod is absent.
        CHANNEL.messageBuilder(FlashbackVoiceChatSound.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(FlashbackVoiceChatSound::write)
            .decoder(FlashbackVoiceChatSound::decode)
            .consumerMainThread((packet, context) -> {
                if (Flashback.isInReplay() && LoaderCompat.isModLoaded("voicechat")) {
                    SimpleVoiceChatPlayback.play(packet);
                }
                context.get().setPacketHandled(true);
            }).add();

        CHANNEL.messageBuilder(FlashbackAccurateEntityPosition.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(FlashbackAccurateEntityPosition::write)
            .decoder(FlashbackAccurateEntityPosition::decode)
            .consumerMainThread((packet, context) -> {
                if (Flashback.isInReplay()) {
                    AccurateEntityPositionHandler.update(packet);
                }
                context.get().setPacketHandled(true);
            }).add();

        CHANNEL.messageBuilder(FinishedServerTick.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(FinishedServerTick::write)
            .decoder(FinishedServerTick::decode)
            // ExportJob runs synchronously on the render thread and waits for
            // this acknowledgement. Scheduling it back onto that same thread
            // deadlocks before the first frame can be rendered.
            .consumerNetworkThread((packet, context) -> {
                if (Flashback.EXPORT_JOB != null) {
                    Flashback.EXPORT_JOB.onFinishedServerTick();
                }
                context.get().setPacketHandled(true);
            }).add();
    }

    /**
     * Runs a single client tick with the tick rate manager temporarily unfrozen.
     *
     * The replay server unfreezes the manager, sends FlashbackForceClientTick and immediately
     * re-freezes it again, all within the same server tick. On 1.21.1 those three steps are
     * separate ordered packets, so the client is still unfrozen when it handles the force tick.
     * Here the frozen flag is a shared static, so by the time this runs it is already back to
     * "frozen" and the tick would be a no-op (every entity is frozen), leaving the replay
     * showing the pre-jump state until the user unpauses.
     */
    private static void tickClientOnceUnfrozen() {
        Minecraft minecraft = Minecraft.getInstance();
        ReplayTimer replayTimer = ((MinecraftExt) minecraft).flashback$getReplayTimer();

        boolean wasFrozen = replayTimer.manager.isFrozen();
        replayTimer.manager.setFrozen(false);
        try {
            ReplayClientTick.runForced(minecraft::tick);
        } finally {
            replayTimer.manager.setFrozen(wasFrozen);
        }
    }

    private static Entity replayEntity(int entityId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!Flashback.isInReplay() || minecraft.level == null) {
            return null;
        }
        return minecraft.level.getEntity(entityId);
    }

    public static void sendTo(ServerPlayer player, FlashbackPacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
