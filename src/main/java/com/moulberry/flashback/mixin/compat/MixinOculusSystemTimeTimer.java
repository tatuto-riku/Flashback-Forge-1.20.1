package com.moulberry.flashback.mixin.compat;

import com.moulberry.flashback.Flashback;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Uses deterministic shader time during video export, matching upstream Flashback. */
@Pseudo
@Mixin(targets = "net.irisshaders.iris.uniforms.SystemTimeUniforms$Timer", remap = false)
public abstract class MixinOculusSystemTimeTimer {

    @Inject(method = "getFrameTimeCounter", at = @At("HEAD"), cancellable = true, require = 0)
    private void flashback$getReplayFrameTimeCounter(CallbackInfoReturnable<Float> cir) {
        if (Flashback.isExporting()) {
            double replayTick = Flashback.EXPORT_JOB.getCurrentTickDouble();
            cir.setReturnValue((float) ((replayTick / 20.0) % 3600.0));
        }
    }

    @Inject(method = "getLastFrameTime", at = @At("HEAD"), cancellable = true, require = 0)
    private void flashback$getReplayLastFrameTime(CallbackInfoReturnable<Float> cir) {
        if (Flashback.isExporting()) {
            cir.setReturnValue((float) (1.0 / Flashback.EXPORT_JOB.getSettings().framerate()));
        }
    }
}
