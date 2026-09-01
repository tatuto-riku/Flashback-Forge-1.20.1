package com.moulberry.flashback.visuals;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.moulberry.flashback.Flashback;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.IOException;

@Mod.EventBusSubscriber(modid = "flashback", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ShaderManager {

    public static final ShaderManager INSTANCE = new ShaderManager();

    public static ShaderInstance blitScreenRoundAlpha;

    @SubscribeEvent
    public static void register(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(),
            new ResourceLocation("flashback:blit_screen_round_alpha"), DefaultVertexFormat.BLIT_SCREEN),
            shader -> blitScreenRoundAlpha = shader);
    }

}
