package com.moulberry.flashback.mixin.compat;

import com.moulberry.flashback.Flashback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps IV's rendering anchor attached to the local replay viewer. */
@Pseudo
@Mixin(targets = "mcinterface1201.BuilderEntityRenderForwarder", remap = false)
public abstract class MixinImmersiveVehiclesRenderForwarderReplay {

    @Shadow(remap = false)
    protected Player playerFollowing;

    @Inject(method = {"baseTick", "m_6075_"}, at = @At("HEAD"), require = 0)
    private void flashback$followReplayViewer(CallbackInfo ci) {
        Entity forwarder = (Entity) (Object) this;
        if (!forwarder.level().isClientSide || !Flashback.isInReplay()) {
            return;
        }
        LocalPlayer viewer = Minecraft.getInstance().player;
        if (viewer != null && viewer.level() == forwarder.level()
                && this.playerFollowing != viewer) {
            this.playerFollowing = viewer;
        }
    }
}
