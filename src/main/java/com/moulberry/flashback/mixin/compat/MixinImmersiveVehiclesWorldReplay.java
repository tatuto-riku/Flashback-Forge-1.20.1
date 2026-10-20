package com.moulberry.flashback.mixin.compat;

import com.moulberry.flashback.Flashback;
import com.moulberry.flashback.playback.ReplayClientTick;
import com.moulberry.flashback.playback.ReplayServer;
import net.minecraft.client.Minecraft;
import net.minecraftforge.event.TickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Prevents IV from running a second live vehicle simulation inside a replay server. */
@Pseudo
@Mixin(targets = "mcinterface1201.WrapperWorld", remap = false)
public abstract class MixinImmersiveVehiclesWorldReplay {

    @Inject(method = "onIVWorldTick", at = @At("HEAD"), cancellable = true, require = 0)
    private void flashback$skipReplayWorldSimulation(
            TickEvent.LevelTickEvent event, CallbackInfo ci) {
        if (!event.level.isClientSide() && event.level.getServer() instanceof ReplayServer) {
            ci.cancel();
        } else if (event.level.isClientSide() && Flashback.isInReplay()
                && !ReplayClientTick.runsNormally(Minecraft.getInstance())) {
            // IV advances its own entity manager from Forge's level-tick event rather than from
            // vanilla Entity#tick. Stop that independent physics clock while the replay is paused.
            ci.cancel();
        }
    }
}
