package com.moulberry.flashback.mixin.compat;

import com.moulberry.flashback.compat.ImmersiveVehiclesHandshakeCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Repairs IV snapshot handshakes whose reconstructed wrapper UUID changed. */
@Pseudo
@Mixin(targets = "mcinterface1201.PacketEntityCSHandshakeServer", remap = false)
public abstract class MixinImmersiveVehiclesHandshakeReplay {

    @Inject(method = "handle", at = @At("TAIL"), require = 0)
    private void flashback$finishReplayHandshake(CallbackInfo ci) {
        ImmersiveVehiclesHandshakeCompat.finishReplayHandshake(this);
    }
}
