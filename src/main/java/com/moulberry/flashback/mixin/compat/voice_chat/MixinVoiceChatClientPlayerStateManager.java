package com.moulberry.flashback.mixin.compat.voice_chat;

import com.moulberry.flashback.Flashback;
import de.maxhenkel.voicechat.voice.client.ClientPlayerStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keeps replay actors visible to Simple Voice Chat's client-side state checks. */
@Pseudo
@Mixin(value = ClientPlayerStateManager.class, remap = false)
public abstract class MixinVoiceChatClientPlayerStateManager {

    @Inject(method = "isPlayerDisconnected", at = @At("HEAD"), cancellable = true, require = 0)
    private void flashback$replayPlayersAreConnected(CallbackInfoReturnable<Boolean> cir) {
        if (Flashback.isInReplay()) {
            cir.setReturnValue(false);
        }
    }
}
