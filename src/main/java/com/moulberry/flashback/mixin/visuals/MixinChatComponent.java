package com.moulberry.flashback.mixin.visuals;

import com.moulberry.flashback.Flashback;
import com.moulberry.flashback.playback.ReplayServer;
import com.moulberry.flashback.state.EditorState;
import com.moulberry.flashback.state.EditorStateManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies the replay chat visibility setting at the component that actually
 * draws chat. Targeting ChatComponent itself also covers calls introduced by
 * other GUI mods instead of depending on one particular call inside Gui.
 */
@Mixin(ChatComponent.class)
public abstract class MixinChatComponent {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void flashback$hideReplayChat(GuiGraphics guiGraphics, int tickCount,
                                          int mouseX, int mouseY, CallbackInfo ci) {
        if (!Flashback.isInReplay()) {
            return;
        }

        EditorState editorState = EditorStateManager.getCurrent();
        if (editorState == null) {
            // A newly opened replay can still be paused before another editor window has
            // caused its state file to load. Resolve it here so the saved/default chat
            // setting is effective from the very first rendered frame.
            ReplayServer replayServer = Flashback.getReplayServer();
            if (replayServer != null) {
                editorState = EditorStateManager.get(replayServer.getMetadata().replayIdentifier);
            }
        }
        if (editorState != null && !editorState.replayVisuals.showChat) {
            ci.cancel();
        }
    }
}
