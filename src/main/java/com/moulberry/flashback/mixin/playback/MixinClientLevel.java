package com.moulberry.flashback.mixin.playback;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.moulberry.flashback.Flashback;
import com.moulberry.flashback.ext.ClientLevelExt;
import com.moulberry.flashback.ext.MinecraftExt;
import com.moulberry.flashback.playback.TickRateManager;
import com.moulberry.flashback.playback.ReplayClientTick;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Deque;

@Mixin(ClientLevel.class)
public class MixinClientLevel implements ClientLevelExt {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    @Final
    private Deque<Runnable> lightUpdateQueue;

    @Unique
    private final LongSet flashback$pendingLightRechecks = new LongOpenHashSet();

    @Override
    public int flashback$clearQueuedLightUpdates() {
        int queuedUpdates = this.lightUpdateQueue.size();
        this.lightUpdateQueue.clear();
        this.flashback$pendingLightRechecks.clear();
        return queuedUpdates;
    }

    @Override
    public int flashback$drainQueuedLightUpdates() {
        int appliedUpdates = 0;
        Runnable update;
        while ((update = this.lightUpdateQueue.poll()) != null) {
            update.run();
            appliedUpdates++;
        }
        return appliedUpdates;
    }

    @Override
    public void flashback$queueLightRechecks(long[] positions) {
        for (long position : positions) {
            this.flashback$pendingLightRechecks.add(position);
        }
    }

    @Override
    public int flashback$applyQueuedLightRechecks() {
        ClientLevel level = (ClientLevel) (Object) this;

        // LightEngine#runLightUpdates evaluates checkBlock nodes *before* it installs section data
        // queued by queueSectionData (LayerLightSectionStorage#markNewInconsistencies). Flush the
        // recorded snapshot light first, otherwise the rechecks below are computed against the old
        // arrays and then overwritten by snapshot light, leaving dark spots at changed blocks.
        this.flashback$runAllLightUpdates();

        int recheckedBlocks = 0;
        for (long packedPosition : this.flashback$pendingLightRechecks) {
            BlockPos position = BlockPos.of(packedPosition);
            if (level.getChunkSource().getChunk(
                    position.getX() >> 4, position.getZ() >> 4, false) == null) {
                continue;
            }
            level.getChunkSource().getLightEngine().checkBlock(position);
            level.setSectionDirtyWithNeighbors(
                    position.getX() >> 4, position.getY() >> 4, position.getZ() >> 4);
            recheckedBlocks++;
        }
        this.flashback$pendingLightRechecks.clear();

        // Client lighting is synchronous in 1.20.1. Finish propagation now, after the recorded
        // light packets have been applied, so rendering cannot observe an intermediate graph.
        this.flashback$runAllLightUpdates();
        return recheckedBlocks;
    }

    @Unique
    private void flashback$runAllLightUpdates() {
        ClientLevel level = (ClientLevel) (Object) this;
        int passes = 0;
        while (level.getChunkSource().getLightEngine().hasLightWork() && passes++ < 1024) {
            level.getChunkSource().getLightEngine().runLightUpdates();
        }
    }

    @Inject(method = "tickNonPassenger", at = @At("HEAD"), cancellable = true)
    private void tickEntity(Entity entity, CallbackInfo ci) {
        if (!Flashback.isInReplay()) {
            return;
        }

        TickRateManager manager = ((MinecraftExt) minecraft).flashback$getReplayTimer().manager;

        // The local player is ticked manually by MixinMinecraft, outside of ClientLevel#tickEntities.
        // That call must always go through, otherwise the replay camera can never move.
        if (manager.isManualLocalPlayerTick()) {
            return;
        }

        if (!ReplayClientTick.runsNormally(minecraft) || manager.isEntityFrozen(entity)) {
            ci.cancel();
        }
    }

    @Inject(method = "tickPassenger", at = @At("HEAD"), cancellable = true)
    private void flashback$freezePassenger(Entity vehicle, Entity passenger, CallbackInfo ci) {
        if (Flashback.isInReplay() && !ReplayClientTick.runsNormally(minecraft)) {
            // Passenger ticking is a separate recursive path and bypasses tickNonPassenger.
            // Cancelling it prevents seats from continuing to reposition riders while paused.
            ci.cancel();
        }
    }

    @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;tickTime()V"))
    public boolean tickTime(ClientLevel instance) {
        return ReplayClientTick.runsNormally(minecraft);
    }

}
