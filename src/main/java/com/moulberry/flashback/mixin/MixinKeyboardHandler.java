package com.moulberry.flashback.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.moulberry.flashback.screen.ScreenInputTracker;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(KeyboardHandler.class)
public class MixinKeyboardHandler {

    @WrapOperation(method = {"keyPress", "charTyped"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;wrapScreenError(Ljava/lang/Runnable;Ljava/lang/String;Ljava/lang/String;)V"))
    public void wrapScreenInput(Runnable runnable, String description, String className, Operation<Void> original) {
        ScreenInputTracker.enter();
        try {
            original.call(runnable, description, className);
        } finally {
            ScreenInputTracker.exit();
        }
    }

}
