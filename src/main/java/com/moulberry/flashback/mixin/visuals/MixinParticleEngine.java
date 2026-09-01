package com.moulberry.flashback.mixin.visuals;

import com.moulberry.flashback.Flashback;
import com.moulberry.flashback.state.EditorState;
import com.moulberry.flashback.state.EditorStateManager;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleEngine.class)
public abstract class MixinParticleEngine {

    @Inject(method = "createParticle", at = @At("HEAD"), cancellable = true)
    private void flashback$filterParticle(ParticleOptions particleOptions, double x, double y, double z, double dx, double dy, double dz,
                                           CallbackInfoReturnable<Particle> cir) {
        if (!Flashback.isInReplay()) {
            return;
        }

        EditorState editorState = EditorStateManager.getCurrent();
        if (editorState == null || editorState.filteredParticles.isEmpty()) {
            return;
        }

        ResourceLocation key = BuiltInRegistries.PARTICLE_TYPE.getKey(particleOptions.getType());
        if (editorState.filteredParticles.contains(key.toString())) {
            cir.setReturnValue(null);
        }
    }
}
