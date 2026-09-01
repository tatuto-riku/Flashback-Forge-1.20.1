package com.moulberry.flashback.mixin.norandom;

import com.moulberry.flashback.Flashback;
import net.minecraft.client.particle.Particle;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Random;

@Mixin(Particle.class)
public class MixinParticle {

    @Redirect(method = "<init>(Lnet/minecraft/client/multiplayer/ClientLevel;DDD)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;create()Lnet/minecraft/util/RandomSource;"))
    private RandomSource createParticleRandom() {
        RandomSource randomSource = RandomSource.create();
        if (Flashback.isExporting()) {
            Random random = Flashback.EXPORT_JOB.getParticleRandom();
            if (random != null) {
                randomSource.setSeed(random.nextLong());
            }
        }
        return randomSource;
    }

}
