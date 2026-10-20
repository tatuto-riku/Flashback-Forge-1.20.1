package com.moulberry.flashback.mixin.compat;

import com.moulberry.flashback.compat.ImmersiveVehiclesHandshakeCompat;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies an IV snapshot handshake once its reconstructed wrapper exists on the client. */
@Pseudo
@Mixin(targets = "mcinterface1201.ABuilderEntityBase", remap = false)
public abstract class MixinImmersiveVehiclesBuilderReplay {

    @Inject(method = {"baseTick", "m_6075_"}, at = @At("HEAD"), require = 0)
    private void flashback$applyDeferredReplayHandshake(CallbackInfo ci) {
        Entity builder = (Entity) (Object) this;
        if (builder.level().isClientSide) {
            ImmersiveVehiclesHandshakeCompat.applyPendingHandshake(builder);
        }
    }
}
