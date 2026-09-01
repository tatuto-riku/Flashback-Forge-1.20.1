package com.moulberry.flashback.mixin.playback;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.moulberry.flashback.Flashback;
import com.moulberry.flashback.ext.MinecraftExt;
import com.moulberry.flashback.playback.TickRateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public class MixinClientLevel {

    @Shadow
    @Final
    private Minecraft minecraft;

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

        if (manager.isEntityFrozen(entity)) {
            ci.cancel();
        }
    }

    @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;tickTime()V"))
    public boolean tickTime(ClientLevel instance) {
        return !Flashback.isInReplay() || ((MinecraftExt) minecraft).flashback$getReplayTimer().manager.runsNormally();
    }

}
