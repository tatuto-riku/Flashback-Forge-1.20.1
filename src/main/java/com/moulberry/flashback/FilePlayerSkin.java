package com.moulberry.flashback;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * A player skin loaded from a local png file.
 *
 * Only {@code pathToSkin} is serialized, the actual texture is loaded lazily the first time
 * something asks for it.
 */
public class FilePlayerSkin {

    private transient ResourceLocation skinLocation = null;
    private transient Boolean slim = null;

    private final String pathToSkin;

    public FilePlayerSkin(String pathToSkin) {
        this.pathToSkin = pathToSkin;
    }

    @Nullable
    public ResourceLocation getSkinTextureLocation() {
        if (this.skinLocation == null) {
            this.load();
        }
        return this.skinLocation;
    }

    public boolean isSlim() {
        if (this.slim == null) {
            this.load();
        }
        return this.slim != null && this.slim;
    }

    private void load() {
        try (InputStream inputStream = Files.newInputStream(Path.of(this.pathToSkin))) {
            NativeImage nativeImage = NativeImage.read(inputStream);
            int width = nativeImage.getWidth();
            int height = nativeImage.getHeight();

            // The alpha channel of pixel (54, 20) decides whether the skin uses the slim model
            int argb = nativeImage.getPixelRGBA(54 * width / 64, 20 * height / 64);
            this.slim = ((argb >> 24) & 0xFF) < 20;

            DynamicTexture dynamicTexture = new DynamicTexture(nativeImage);
            ResourceLocation resourceLocation = new ResourceLocation("flashback", "skin_from_file/" + UUID.randomUUID());
            Minecraft.getInstance().getTextureManager().register(resourceLocation, dynamicTexture);
            this.skinLocation = resourceLocation;
        } catch (Exception e) {
            Flashback.LOGGER.error("Unable to load skin from file", e);
            this.skinLocation = DefaultPlayerSkin.getDefaultSkin();
            this.slim = false;
        }
    }

}
