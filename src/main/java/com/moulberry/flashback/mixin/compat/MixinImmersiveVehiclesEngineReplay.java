package com.moulberry.flashback.mixin.compat;

import com.moulberry.flashback.Flashback;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Prevents IV from treating a reconstructed replay input as a live engine-start request. */
@Pseudo
@Mixin(targets = "minecrafttransportsimulator.entities.instances.PartEngine", remap = false)
public abstract class MixinImmersiveVehiclesEngineReplay {

    @Inject(
            method = "autoStartEngine",
            at = @At(
                    value = "FIELD",
                    target = "Lminecrafttransportsimulator/systems/LanguageSystem;"
                            + "INTERACT_VEHICLE_NOFUEL:"
                            + "Lminecrafttransportsimulator/systems/LanguageSystem$LanguageEntry;",
                    opcode = Opcodes.GETSTATIC),
            cancellable = true,
            require = 0)
    private void flashback$suppressReplayNoFuelMessage(CallbackInfo ci) {
        if (Flashback.isInReplay()) {
            ci.cancel();
        }
    }
}
