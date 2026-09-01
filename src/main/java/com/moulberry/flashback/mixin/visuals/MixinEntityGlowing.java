package com.moulberry.flashback.mixin.visuals;

import com.moulberry.flashback.combo_options.GlowingOverride;
import com.moulberry.flashback.state.EditorState;
import com.moulberry.flashback.state.EditorStateManager;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class MixinEntityGlowing {

    @Inject(method = "isCurrentlyGlowing", at = @At("HEAD"), cancellable = true)
    public void isCurrentlyGlowing(CallbackInfoReturnable<Boolean> cir) {
        EditorState editorState = EditorStateManager.getCurrent();
        if (editorState == null) {
            return;
        }

        GlowingOverride glowingOverride = editorState.glowingOverride.get(((Entity) (Object) this).getUUID());
        if (glowingOverride == GlowingOverride.FORCE_GLOW) {
            cir.setReturnValue(true);
        } else if (glowingOverride == GlowingOverride.FORCE_NO_GLOW) {
            cir.setReturnValue(false);
        }
    }

}
