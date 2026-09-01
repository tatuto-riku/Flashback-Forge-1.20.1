package com.moulberry.flashback.mixin.ui;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.moulberry.flashback.Flashback;
import com.moulberry.flashback.combo_options.RecordingControlsLocation;
import com.moulberry.flashback.screen.BottomTextWidget;
import com.moulberry.flashback.screen.FlashbackButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractStringWidget;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.IntSupplier;

@Mixin(PauseScreen.class)
public abstract class MixinPauseScreen extends Screen {

    protected MixinPauseScreen(Component title) {
        super(title);
    }

    @Inject(method = "createPauseMenu", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/layouts/GridLayout;arrangeElements()V", shift = At.Shift.BEFORE))
    public void createPauseMenuBelow(CallbackInfo ci, @Local GridLayout.RowHelper rowHelper) {
        if (Flashback.isInReplay() || Flashback.getConfig().recordingControlsLocation != RecordingControlsLocation.BELOW) {
            return;
        }

        rowHelper.addChild(new BottomTextWidget(204, 30, Component.literal("Flashback"), Minecraft.getInstance().font), 2);
        if (Flashback.RECORDER == null) {
            rowHelper.addChild(Button.builder(Component.translatable("flashback.recording_controls.start"), button -> {
                Flashback.startRecordingReplay();
                Minecraft.getInstance().setScreen(null);
            }).width(204).build(), 2);
        } else {
            rowHelper.addChild(Button.builder(Component.translatable("flashback.recording_controls.finish"), button -> {
                Flashback.finishRecordingReplay();
                Minecraft.getInstance().setScreen(null);
            }).width(204).build(), 2);

            Component pauseLabel = Component.translatable(Flashback.RECORDER.isPaused()
                ? "flashback.recording_controls.unpause" : "flashback.recording_controls.pause");
            rowHelper.addChild(Button.builder(pauseLabel, button -> {
                Flashback.pauseRecordingReplay(!Flashback.RECORDER.isPaused());
                Minecraft.getInstance().setScreen(null);
            }).width(98).build());
            rowHelper.addChild(createCancelButton(98));
        }
    }

    @Inject(method = "createPauseMenu", at = @At("RETURN"))
    public void createPauseMenuSide(CallbackInfo ci) {
        RecordingControlsLocation location = Flashback.getConfig().recordingControlsLocation;
        if (Flashback.isInReplay() || (location != RecordingControlsLocation.RIGHT && location != RecordingControlsLocation.LEFT)) {
            return;
        }

        boolean right = location == RecordingControlsLocation.RIGHT;
        int x = this.width / 2;
        SortedSet<Integer> heights = new TreeSet<>();
        for (Renderable renderable : this.renderables) {
            if (renderable instanceof AbstractStringWidget) {
                continue;
            }
            if (renderable instanceof AbstractWidget widget) {
                int candidate = right ? widget.getX() + widget.getWidth() + 4 : widget.getX() - 24;
                if ((right && candidate > x) || (!right && candidate < x)) {
                    x = candidate;
                    heights.clear();
                }
                heights.add(widget.getY());
            }
        }

        IntSupplier nextHeight = new IntSupplier() {
            private final Integer[] values = heights.toArray(Integer[]::new);
            private int index = values.length >= 4 ? 1 : 0;
            private int last = Integer.MIN_VALUE;

            @Override
            public int getAsInt() {
                int next = index < values.length ? values[index++] : (last == Integer.MIN_VALUE ? height / 2 : last + 24);
                if (last != Integer.MIN_VALUE) {
                    next = Math.max(next, last + 24);
                }
                last = next;
                return next;
            }
        };

        if (Flashback.RECORDER == null) {
            addControl(x, nextHeight.getAsInt(), "flashback.recording_controls.start", ICON_START, button -> {
                Flashback.startRecordingReplay();
                Minecraft.getInstance().setScreen(null);
            });
            return;
        }

        addControl(x, nextHeight.getAsInt(), "flashback.recording_controls.finish", ICON_FINISH, button -> {
            Flashback.finishRecordingReplay();
            Minecraft.getInstance().setScreen(null);
        });
        boolean paused = Flashback.RECORDER.isPaused();
        addControl(x, nextHeight.getAsInt(), paused ? "flashback.recording_controls.unpause" : "flashback.recording_controls.pause",
            paused ? ICON_START : ICON_PAUSE, button -> {
                Flashback.pauseRecordingReplay(!paused);
                Minecraft.getInstance().setScreen(null);
            });
        addControl(x, nextHeight.getAsInt(), "flashback.recording_controls.cancel", ICON_CANCEL, button -> confirmCancel());
    }

    private static final ResourceLocation ICON_START = new ResourceLocation("flashback", "icon_pixelated_start.png");
    private static final ResourceLocation ICON_FINISH = new ResourceLocation("flashback", "icon_pixelated_finish.png");
    private static final ResourceLocation ICON_PAUSE = new ResourceLocation("flashback", "icon_pixelated_pause.png");
    private static final ResourceLocation ICON_CANCEL = new ResourceLocation("flashback", "icon_pixelated_cancel.png");

    private void addControl(int x, int y, String tooltip, ResourceLocation icon, Button.OnPress onPress) {
        this.addRenderableWidget(new FlashbackButton(x, y, 20, 20, Component.translatable(tooltip),
            onPress, icon).flashbackWithTooltip());
    }

    private Button createCancelButton(int width) {
        return Button.builder(Component.translatable("flashback.recording_controls.cancel"), button -> confirmCancel()).width(width).build();
    }

    private void confirmCancel() {
        Minecraft.getInstance().setScreen(new ConfirmScreen(value -> {
            if (value) {
                Flashback.cancelRecordingReplay();
                Minecraft.getInstance().setScreen(null);
            } else {
                Minecraft.getInstance().setScreen(new PauseScreen(true));
            }
        }, Component.translatable("flashback.confirm_cancel_recording"),
            Component.translatable("flashback.confirm_cancel_recording_description")));
    }

    @WrapOperation(method = "createPauseMenu", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/layouts/FrameLayout;alignInRectangle(Lnet/minecraft/client/gui/layouts/LayoutElement;IIIIFF)V"))
    public void alignPauseMenu(LayoutElement element, int x, int y, int width, int height, float alignX, float alignY, Operation<Void> original) {
        if (!Flashback.isInReplay() && Flashback.getConfig().recordingControlsLocation == RecordingControlsLocation.BELOW) {
            alignY += 0.05f;
        }
        original.call(element, x, y, width, height, alignX, alignY);
    }
}
