package com.moulberry.flashback.screen.select_replay;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import com.moulberry.flashback.Flashback;
import com.moulberry.flashback.RegistryMetaHelper;
import com.moulberry.flashback.screen.EditReplayScreen;
import com.moulberry.flashback.screen.ReplaySummary;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.FaviconTexture;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Date;

public abstract class ReplaySelectionEntry extends ObjectSelectionList.Entry<ReplaySelectionEntry> implements AutoCloseable {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withZone(ZoneId.systemDefault());
    private static final ResourceLocation FOLDER_SPRITE = Flashback.createResourceLocation("folder.png");
    private static final ResourceLocation ICON_OVERLAY_LOCATION = new ResourceLocation("textures/gui/world_selection.png");
    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void close() {
    }

    public static class LoadFromDeviceHeader extends ReplaySelectionEntry {
        private static final Component LOAD_REPLAY_LABEL = Component.translatable("flashback.select_replay.load_replay_from_file");
        private final Minecraft minecraft;

        public LoadFromDeviceHeader(Minecraft minecraft) {
            this.minecraft = minecraft;
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
            int textureY = 46 + (hovered ? 2 : 1) * 20;
            guiGraphics.blitNineSliced(net.minecraft.client.gui.components.AbstractWidget.WIDGETS_LOCATION,
                x + 4, y + 2, width - 8, height - 4, 20, 4, 200, 20, 0, textureY);

            int p = (this.minecraft.screen.width - this.minecraft.font.width(LOAD_REPLAY_LABEL)) / 2;
            int q = y + (height - this.minecraft.font.lineHeight) / 2 + 1;
            guiGraphics.drawString(this.minecraft.font, LOAD_REPLAY_LABEL, p, q, 0xFFFFFF, true);
        }

        @Override
        public Component getNarration() {
            return LOAD_REPLAY_LABEL;
        }
    }

    public static class LoadingHeader extends ReplaySelectionEntry {
        private static final Component LOADING_LABEL = Component.translatable("flashback.select_replay.loading_replays");
        private final Minecraft minecraft;

        public LoadingHeader(Minecraft minecraft) {
            this.minecraft = minecraft;
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
            int p = (this.minecraft.screen.width - this.minecraft.font.width(LOADING_LABEL)) / 2;
            int q = y + (height - this.minecraft.font.lineHeight) / 2;
            guiGraphics.drawString(this.minecraft.font, LOADING_LABEL, p, q, 0xFFFFFF, false);
            String string = net.minecraft.client.gui.screens.LoadingDotsText.get(Util.getMillis());
            int r = (this.minecraft.screen.width - this.minecraft.font.width(string)) / 2;
            int s = q + this.minecraft.font.lineHeight;
            guiGraphics.drawString(this.minecraft.font, string, r, s, 0xFF808080, false);
        }

        @Override
        public Component getNarration() {
            return LOADING_LABEL;
        }
    }

    public static class ReplayFolder extends ReplaySelectionEntry implements AutoCloseable {
        private static final int ICON_WIDTH = 32;
        private static final int ICON_HEIGHT = 32;
        private final Minecraft minecraft;
        private final ReplaySelectionList replaySelectionList;
        private final Path path;
        private final String nameString;
        private final Component nameComponent;
        private final long modifiedTime;
        private final int replayCount;
        private long lastClickTime;

        public ReplayFolder(ReplaySelectionList replaySelectionList, Minecraft minecraft, Path path, long modifiedTime, int replayCount) {
            this.minecraft = minecraft;
            this.replaySelectionList = replaySelectionList;
            this.path = path;
            this.nameString = path.getFileName().toString();
            this.nameComponent = Component.literal(this.nameString).withStyle(net.minecraft.ChatFormatting.YELLOW);
            this.modifiedTime = modifiedTime;
            this.replayCount = replayCount;
        }

        @Override
        public Component getNarration() {
            return Component.translatable("narrator.select", this.nameComponent);
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
            guiGraphics.drawString(this.minecraft.font, this.nameComponent, x + 32 + 3, y + 1, 0xFFFFFF, false);
            int textY = y + this.minecraft.font.lineHeight + 2;
            if (this.modifiedTime > 0L) {
                String time = "Created: " + ReplaySelectionEntry.DATE_FORMAT.format(Instant.ofEpochMilli(this.modifiedTime));
                guiGraphics.drawString(this.minecraft.font, time, x + 32 + 3, textY + 1, 0xFF808080, false);
                textY += this.minecraft.font.lineHeight;
            }

            guiGraphics.drawString(this.minecraft.font, "Replays: " + this.replayCount, x + 32 + 3, textY + 3, 0xFF808080, false);

            RenderSystem.enableBlend();
            guiGraphics.blit(ReplaySelectionEntry.FOLDER_SPRITE, x, y, 0.0f, 0.0f, 32, 32, 32, 32);
            RenderSystem.disableBlend();

            if (this.minecraft.options.touchscreen().get() || hovered) {
                guiGraphics.fill(x, y, x + 32, y + 32, -1601138544);
                int q = mouseX - x;
                boolean hoveredIcon = q < 32;
                guiGraphics.blit(ICON_OVERLAY_LOCATION, x, y, 0.0f, hoveredIcon ? 32.0f : 0.0f, 32, 32, 256, 256);
            }
        }

        @Override
        public boolean mouseClicked(double d, double e, int i) {
            this.replaySelectionList.setSelected(this);
            boolean clickedIcon = d - this.replaySelectionList.getRowLeft() <= 32.0;
            boolean doubleClicked = Util.getMillis() - this.lastClickTime < 250L;
            if (!clickedIcon && !doubleClicked) {
                this.lastClickTime = Util.getMillis();
                return super.mouseClicked(d, e, i);
            }
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
            this.minecraft.setScreen(new SelectReplayScreen(this.replaySelectionList.getScreen(), this.path));
            return true;
        }
    }

    public static class ReplayListEntry extends ReplaySelectionEntry implements AutoCloseable {
        private static final int ICON_WIDTH = 32;
        private static final int ICON_HEIGHT = 32;
        private final Minecraft minecraft;
        private final ReplaySelectionList replaySelectionList;
        final ReplaySummary summary;
        private FaviconTexture icon;
        private long lastClickTime;

        public ReplayListEntry(ReplaySelectionList replaySelectionList, Minecraft minecraft, ReplaySummary summary) {
            this.minecraft = minecraft;
            this.replaySelectionList = replaySelectionList;
            this.summary = summary;
            this.icon = FaviconTexture.forWorld(this.minecraft.getTextureManager(), summary.getReplayId());
            this.loadIcon();
        }

        @Override
        public Component getNarration() {
            MutableComponent component = Component.translatable("narrator.select.world_info", this.summary.getReplayName(),
                new Date(this.summary.getLastModified()), this.summary.getInfo());
            return Component.translatable("narrator.select", component);
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
            String title = this.summary.getReplayName();
            String fileAndTime = this.summary.getReplayId();
            long p = this.summary.getLastModified();
            if (p != -1L) {
                fileAndTime = fileAndTime + " (" + ReplaySelectionEntry.DATE_FORMAT.format(Instant.ofEpochMilli(p)) + ")";
            }

            if (StringUtils.isEmpty(title)) {
                title = I18n.get("flashback.select_replay.replay") + " " + (index + 1);
            }

            Component info = this.summary.getInfo();
            int titleColour = 0xFFFFFF;
            if (!this.summary.canOpen()) {
                titleColour = 0xFF5555;
            } else if (this.summary.hasWarning()) {
                titleColour = 0xFFAA55;
            }

            int titleEnd = guiGraphics.drawString(this.minecraft.font, title, x + ICON_WIDTH + 3, y + 1, titleColour, false);
            String worldName = this.summary.getWorldName();
            if (worldName != null) {
                guiGraphics.drawString(this.minecraft.font, "(" + worldName + ")", titleEnd + 4, y + 1, 0xFF808080, false);
            }

            guiGraphics.drawString(this.minecraft.font, fileAndTime, x + ICON_WIDTH + 3, y + this.minecraft.font.lineHeight + 3, 0xFF808080, false);
            guiGraphics.drawString(this.minecraft.font, info, x + ICON_WIDTH + 3, y + this.minecraft.font.lineHeight * 2 + 3, 0xFF808080, false);

            RenderSystem.enableBlend();
            guiGraphics.blit(this.icon.textureLocation(), x, y, 0.0f, 0.0f, 32, 32, 32, 32);
            RenderSystem.disableBlend();

            if (this.minecraft.options.touchscreen().get() || hovered) {
                guiGraphics.fill(x, y, x + ICON_WIDTH, y + ICON_HEIGHT, -1601138544);
                int q = mouseX - x;
                boolean hoveredIcon = q < 32;

                if (hovered && this.summary.getHoverInfo() != null) {
                    guiGraphics.renderTooltip(this.minecraft.font, this.minecraft.font.split(this.summary.getHoverInfo(), 240), mouseX, mouseY);
                }

                float u;
                if (!this.summary.canOpen()) {
                    u = 96.0f;
                } else if (this.summary.hasWarning()) {
                    u = 32.0f;
                } else {
                    u = 0.0f;
                }
                float v = hoveredIcon ? 32.0f : 0.0f;
                guiGraphics.blit(ICON_OVERLAY_LOCATION, x, y, u, v, ICON_WIDTH, ICON_HEIGHT, 256, 256);
            }
        }

        @Override
        public boolean mouseClicked(double d, double e, int i) {
            if (!this.summary.canOpen()) {
                return true;
            }
            this.replaySelectionList.setSelected(this);
            boolean clickedIcon = d - this.replaySelectionList.getRowLeft() <= 32.0;
            boolean doubleClicked = Util.getMillis() - this.lastClickTime < 250L;
            if (!clickedIcon && !doubleClicked) {
                this.lastClickTime = Util.getMillis();
                return super.mouseClicked(d, e, i);
            }
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
            this.openReplay();
            return true;
        }

        public boolean canOpen() {
            return this.summary.canOpen();
        }

        public void openReplay() {
            if (!this.summary.canOpen()) {
                return;
            }
            if (this.summary.hasNamespaceMismatch()) {
                Screen previousScreen = this.minecraft.screen;
                BooleanConsumer afterWarning = doLoad -> {
                    if (doLoad) {
                        this.minecraft.forceSetScreen(new GenericDirtMessageScreen(Component.translatable("flashback.select_replay.data_read")));
                        Flashback.openReplayWorld(this.summary.getPath());
                    } else {
                        this.minecraft.setScreen(previousScreen);
                    }
                };
                Component message = RegistryMetaHelper.createMismatchWarning(
                    this.replaySelectionList.currentNamespacesForRegistries, this.summary.getReplayMetadata().namespacesForRegistries);
                this.minecraft.setScreen(new ConfirmScreen(afterWarning,
                    Component.translatable("flashback.screen_registry_mismatch"), message,
                    Component.translatable("selectWorld.backupJoinSkipButton"), CommonComponents.GUI_CANCEL));
            } else {
                this.minecraft.forceSetScreen(new GenericDirtMessageScreen(Component.translatable("flashback.select_replay.data_read")));
                Flashback.openReplayWorld(this.summary.getPath());
            }
        }

        public void deleteReplay() {
            this.minecraft.setScreen(new ConfirmScreen(bl -> {
                if (bl) {
                    this.minecraft.setScreen(new ProgressScreen(true));
                    this.doDeleteReplay();
                }
                this.minecraft.setScreen(this.replaySelectionList.getScreen());
            }, Component.translatable("flashback.select_replay.delete_question"),
                Component.translatable("selectWorld.deleteWarning", this.summary.getReplayName()),
                Component.translatable("selectWorld.deleteButton"), CommonComponents.GUI_CANCEL));
        }

        private void doDeleteReplay() {
            String id = this.summary.getReplayId();
            try {
                Files.delete(this.summary.getPath());
            } catch (IOException e) {
                SystemToast.onWorldDeleteFailure(this.minecraft, id);
                ReplaySelectionEntry.LOGGER.error("Failed to delete replay {}", id, e);
            }
            this.replaySelectionList.reloadReplayList();
        }

        public void editReplay() {
            Minecraft.getInstance().setScreen(new EditReplayScreen(Minecraft.getInstance().screen, this.summary));
        }

        private void loadIcon() {
            byte[] iconBytes = this.summary.getIconBytes();
            if (iconBytes != null) {
                try {
                    this.icon.upload(NativeImage.read(iconBytes));
                    return;
                } catch (Throwable t) {
                    ReplaySelectionEntry.LOGGER.error("Invalid icon for replay {}", this.summary.getReplayId(), t);
                }
            }
            this.icon.clear();
        }

        @Override
        public void close() {
            if (this.icon != null) {
                this.icon.close();
                this.icon = null;
            }
        }
    }
}
