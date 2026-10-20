package com.moulberry.flashback.mixin.ui;

import com.moulberry.flashback.Flashback;
import com.moulberry.flashback.screen.FlashbackButton;
import com.moulberry.flashback.screen.select_replay.SelectReplayScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = TitleScreen.class, priority = 1300)
public class MixinTitleScreen extends Screen {

    private MixinTitleScreen() {
        super(null);
    }

    @Unique
    private AbstractWidget openSelectReplayScreenButton = null;

    @Inject(method = "init", at = @At("RETURN"))
    public void init(CallbackInfo ci) {
        if (this.openSelectReplayScreenButton != null) {
            this.removeWidget(this.openSelectReplayScreenButton);
            this.openSelectReplayScreenButton = null;
        }
        this.createOpenSelectReplayScreenButton();
    }

    // We do this on tick to ensure that our button is added after ever other mod's
    // Our button logic has automatic repositioning, whereas other mods (i.e. ModMenu)
    // might not implement repositioning logic, causing our button to render under theirs
    @Inject(method = "tick", at = @At("RETURN"))
    public void tick(CallbackInfo ci) {
        if (this.openSelectReplayScreenButton == null ||
            !this.renderables.contains(this.openSelectReplayScreenButton) ||
            this.overlapsExistingWidget(this.openSelectReplayScreenButton.getX(), this.openSelectReplayScreenButton.getY(),
                this.openSelectReplayScreenButton.getWidth(), this.openSelectReplayScreenButton.getHeight(),
                this.openSelectReplayScreenButton)) {
            this.createOpenSelectReplayScreenButton();
        }
    }

    @Unique
    private void createOpenSelectReplayScreenButton() {
        if (this.openSelectReplayScreenButton != null) {
            this.removeWidget(this.openSelectReplayScreenButton);
            this.openSelectReplayScreenButton = null;
        }

        // Prefer a square icon immediately to the right of one of the main
        // title-screen buttons. Try multiple rows and columns because other
        // mods commonly add controls in the same area.
        for (int column = 0; column < 5; column++) {
            for (int index = this.renderables.size() - 1; index >= 0; index--) {
                Renderable renderable = this.renderables.get(index);
                if (!(renderable instanceof AbstractWidget anchor) || anchor.getWidth() < 150) {
                    continue;
                }

                int size = Math.max(16, Math.min(24, anchor.getHeight()));
                int x = anchor.getRectangle().right() + 4 + column * (size + 4);
                int y = anchor.getY();
                if (x + size <= this.width - 4 && y >= 4 && y + size <= this.height - 4 &&
                    !this.overlapsExistingWidget(x, y, size, size)) {
                    this.addReplayButton(x, y, size);
                    return;
                }
            }
        }

        // If a heavily modified title screen has no suitable main button,
        // scan the right edge for a free square instead of hiding the control.
        int size = 20;
        for (int column = 0; column < 5; column++) {
            int x = this.width - 4 - size - column * (size + 4);
            for (int y = 4; y + size <= this.height - 4; y += size + 4) {
                if (x >= 4 && !this.overlapsExistingWidget(x, y, size, size)) {
                    this.addReplayButton(x, y, size);
                    return;
                }
            }
        }
    }

    @Unique
    private void addReplayButton(int x, int y, int size) {
        this.openSelectReplayScreenButton = new FlashbackButton(x, y, size, size,
            Component.literal("Open Replays"), this::openReplayScreen).flashbackWithTooltip();
        this.addRenderableWidget(this.openSelectReplayScreenButton);
    }

    @Unique
    private boolean overlapsExistingWidget(int x, int y, int width, int height) {
        return this.overlapsExistingWidget(x, y, width, height, null);
    }

    @Unique
    private boolean overlapsExistingWidget(int x, int y, int width, int height, AbstractWidget ignored) {
        for (Renderable renderable : this.renderables) {
            if (renderable instanceof AbstractWidget widget && widget != ignored &&
                x < widget.getRectangle().right() && x + width > widget.getX() &&
                y < widget.getRectangle().bottom() && y + height > widget.getY()) {
                return true;
            }
        }
        return false;
    }

    @Unique
    private void openReplayScreen(Button button) {
        List<String> incompatibleMods = Screen.hasShiftDown() ? List.of() : Flashback.getReplayIncompatibleMods();

        if (incompatibleMods.isEmpty()) {
            this.minecraft.setScreen(new SelectReplayScreen(this));
        } else {
            String mods = StringUtils.join(incompatibleMods, ", ");
            String description = """
                You have mods which are known to cause crashes when loading replays
                Please remove the following mods in order to be able to load replays:

                """;
            this.minecraft.setScreen(new AlertScreen(() -> Minecraft.getInstance().setScreen(this),
                Component.literal("Incompatible Mods"), Component.literal(description).append(Component.literal(mods).withStyle(ChatFormatting.RED))));
        }
    }

}
