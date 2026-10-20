package com.moulberry.flashback.mixin.compat;

import com.moulberry.flashback.Flashback;
import com.moulberry.flashback.compat.ImmersiveVehiclesRiderCompat;
import com.moulberry.flashback.playback.ReplayClientTick;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.TickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Avoids IV repeating its Entity Culling setup warning inside the replay chat. */
@Pseudo
@Mixin(targets = "mcinterface1201.InterfaceClient", remap = false)
public abstract class MixinImmersiveVehiclesClientWarning {

    @Shadow(remap = false)
    private static int ticksToCullingWarning;

    @Inject(method = "onIVClientTick", at = @At("HEAD"), cancellable = true, require = 0)
    private static void flashback$suppressReplayCullingWarning(
            TickEvent.ClientTickEvent event, CallbackInfo ci) {
        if (Flashback.isInReplay()) {
            ticksToCullingWarning = 0;

            // IV owns a second client-side simulation clock and calls tickAll directly from this
            // event.  It therefore bypasses ClientLevel's entity tick guard.  Let it run only on
            // ticks where the replay timeline advances (including Flashback's one forced seek
            // tick); otherwise engines, exhaust/fire particles and rider suspension keep moving
            // while the replay is paused.
            if (!ReplayClientTick.runsNormally(Minecraft.getInstance())) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "onIVClientTick", at = @At("RETURN"), require = 0)
    private static void flashback$positionReplayRidersAfterVehicleUpdate(
            TickEvent.ClientTickEvent event, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!Flashback.isInReplay() || event.phase != TickEvent.Phase.END
                || minecraft.level == null || !ReplayClientTick.runsNormally(minecraft)) {
            return;
        }

        // IV completes the second half of its vehicle simulation from ClientTickEvent.END, which
        // may run after Minecraft#tick has already returned.  Position riders again at IV's true
        // update boundary so a newly mounted player follows on the very first replay tick.
        for (Entity passenger : minecraft.level.entitiesForRendering()) {
            Entity vehicle = passenger.getVehicle();
            if (vehicle != null && !vehicle.isRemoved() && vehicle.hasPassenger(passenger)) {
                ImmersiveVehiclesRiderCompat.reconcile(vehicle, passenger);
                vehicle.positionRider(passenger);
            }
        }
    }
}
