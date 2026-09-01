package com.moulberry.flashback.screen;

import com.moulberry.flashback.Flashback;
import com.moulberry.flashback.configuration.FlashbackConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.SimpleOptionsSubScreen;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;

public class ConfigScreen extends SimpleOptionsSubScreen {

    public ConfigScreen(Screen previous) {
        super(previous, Minecraft.getInstance().options, Component.translatable("flashback.flashback_options"), Flashback.getConfig().createOptionInstances());
    }

    public void removed() {
        Flashback.getConfig().saveToDefaultFolder();
    }

}
